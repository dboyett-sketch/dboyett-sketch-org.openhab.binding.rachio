package org.openhab.binding.rachio.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.RachioSecurity;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
import org.openhab.binding.rachio.internal.discovery.RachioDiscoveryService;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

/**
 * The {@link RachioBridgeHandler} is responsible for handling commands for the bridge
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);
    private final HttpClientFactory httpClientFactory;
    
    private final Map<String, RachioDevice> deviceCache = new ConcurrentHashMap<>();
    private final Set<RachioStatusListener> statusListeners = ConcurrentHashMap.newKeySet();

    private @Nullable RachioHttp httpClient;
    private @Nullable RachioSecurity security;
    private @Nullable RachioDiscoveryService discoveryService;
    private @Nullable ScheduledFuture<?> pollingHandler;
    private @Nullable RachioBridgeConfiguration config;
    private @Nullable String personId;
    
    // Rate limiting channels state
    private int rateLimitRemaining = 60;
    private int rateLimitTotal = 60;
    private long rateLimitReset = 0;

    public RachioBridgeHandler(Bridge bridge, HttpClientFactory httpClientFactory) {
        super(bridge);
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio bridge handler");

        config = getConfigAs(RachioBridgeConfiguration.class);
        if (config.apiKey == null || config.apiKey.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "API key is not configured");
            return;
        }

        // Initialize HTTP client with HttpClientFactory
        try {
            httpClient = new RachioHttp(config.apiKey, httpClientFactory);
            security = new RachioSecurity();
            
            // Set initial rate limit values
            rateLimitRemaining = httpClient.getRemainingRequests();
            rateLimitTotal = httpClient.getRateLimit();
            rateLimitReset = httpClient.getResetTime();
            
            updateStatus(ThingStatus.UNKNOWN);
            
            // Start initial poll
            scheduler.execute(() -> {
                try {
                    // Get person info to retrieve devices
                    RachioPerson person = httpClient.getPersonInfo();
                    if (person != null && person.getId() != null) {
                        personId = person.getId();
                        logger.debug("Retrieved person ID: {}", personId);
                        
                        // Update rate limits after successful API call
                        updateRateLimitChannels();
                        
                        // Initialize polling handler correctly
                        pollingHandler = scheduler.scheduleWithFixedDelay(this::pollDevices, 
                                0, config.refresh, TimeUnit.SECONDS);
                        
                        updateStatus(ThingStatus.ONLINE);
                        startDiscovery();
                        
                        // Notify listeners
                        notifyStatusListeners(ThingStatus.ONLINE, ThingStatusDetail.NONE, 
                                "Bridge initialized successfully");
                    } else {
                        String errorMsg = "Failed to retrieve person information from Rachio API";
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMsg);
                        notifyStatusListeners(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMsg);
                    }
                } catch (Exception e) {
                    String errorMsg = "Error during bridge initialization: " + e.getMessage();
                    logger.error(errorMsg, e);
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMsg);
                    notifyStatusListeners(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMsg);
                }
            });
            
        } catch (Exception e) {
            String errorMsg = "Failed to initialize HTTP client: " + e.getMessage();
            logger.error(errorMsg, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMsg);
            notifyStatusListeners(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMsg);
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio bridge handler");
        
        // Cancel polling job
        ScheduledFuture<?> localPollingHandler = pollingHandler;
        if (localPollingHandler != null && !localPollingHandler.isCancelled()) {
            localPollingHandler.cancel(true);
            pollingHandler = null;
        }
        
        // Clear caches and listeners
        deviceCache.clear();
        statusListeners.clear();
        
        // Clear HTTP client
        httpClient = null;
        security = null;
        
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Handling command {} for channel {}", command, channelUID);

        if (command instanceof RefreshType) {
            // Refresh bridge status and rate limits
            scheduler.execute(() -> {
                pollDevices();
                updateRateLimitChannels();
            });
            return;
        }

        String channelId = channelUID.getIdWithoutGroup();
        
        try {
            switch (channelId) {
                case CHANNEL_RATE_LIMIT_STATUS:
                    // Force a poll to update rate limits
                    if (command instanceof RefreshType) {
                        pollDevices();
                    }
                    break;
                    
                case CHANNEL_WEBHOOK_STATUS:
                    // Handle webhook registration commands
                    if (command instanceof RefreshType) {
                        updateWebhookStatus();
                    }
                    break;
                    
                default:
                    logger.debug("Unhandled command for channel {}", channelId);
            }
        } catch (Exception e) {
            logger.error("Error handling command {} for channel {}", command, channelId, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            notifyStatusListeners(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    /**
     * Poll devices from Rachio API
     * Changed from private to package-private to allow access from device/zone handlers
     */
    void pollDevices() {
        RachioHttp localHttpClient = httpClient;
        if (localHttpClient == null || getThing().getStatus() != ThingStatus.ONLINE) {
            return;
        }

        try {
            logger.debug("Polling Rachio devices");
            
            // Get person info to retrieve devices
            RachioPerson person = localHttpClient.getPersonInfo();
            if (person != null && person.getDevices() != null) {
                // Update device cache
                for (RachioDevice device : person.getDevices()) {
                    deviceCache.put(device.getId(), device);
                }
                
                // Update rate limits
                rateLimitRemaining = localHttpClient.getRemainingRequests();
                rateLimitTotal = localHttpClient.getRateLimit();
                rateLimitReset = localHttpClient.getResetTime();
                
                // Update rate limit channels
                updateRateLimitChannels();
                
                // Notify status listeners of updates
                notifyDeviceUpdates(person.getDevices());
                
                logger.debug("Successfully polled {} devices", person.getDevices().size());
                
                // Update webhook status
                updateWebhookStatus();
            } else {
                logger.warn("No devices found in person info");
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, 
                        "No devices found in API response");
            }
        } catch (Exception e) {
            logger.error("Error polling devices", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, 
                    "Polling error: " + e.getMessage());
            notifyStatusListeners(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, 
                    "Polling error: " + e.getMessage());
        }
    }

    /**
     * Update rate limit channels
     */
    private void updateRateLimitChannels() {
        updateState(CHANNEL_RATE_LIMIT_REMAINING, 
                new org.openhab.core.library.types.DecimalType(rateLimitRemaining));
        
        if (rateLimitTotal > 0) {
            int percentage = (int) ((rateLimitRemaining / (double) rateLimitTotal) * 100);
            updateState(CHANNEL_RATE_LIMIT_PERCENTAGE, 
                    new org.openhab.core.library.types.DecimalType(percentage));
            
            // Update rate limit status channel
            String status;
            if (rateLimitRemaining <= RATE_LIMIT_THRESHOLD_CRITICAL) {
                status = "CRITICAL";
            } else if (rateLimitRemaining <= RATE_LIMIT_THRESHOLD_LOW) {
                status = "LOW";
            } else {
                status = "NORMAL";
            }
            updateState(CHANNEL_RATE_LIMIT_STATUS, 
                    new org.openhab.core.library.types.StringType(status));
        }
        
        if (rateLimitReset > 0) {
            updateState(CHANNEL_RATE_LIMIT_RESET, 
                    new org.openhab.core.library.types.DateTimeType(
                            new java.time.ZonedDateTime(java.time.Instant.ofEpochMilli(rateLimitReset), 
                            java.time.ZoneId.systemDefault())));
        }
        
        // Notify listeners of rate limit change
        notifyRateLimitChanged(rateLimitRemaining, rateLimitTotal, rateLimitReset);
    }

    /**
     * Update webhook status channel
     */
    private void updateWebhookStatus() {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig != null && localConfig.webhookUrl != null && !localConfig.webhookUrl.isEmpty()) {
            updateState(CHANNEL_WEBHOOK_STATUS, 
                    new org.openhab.core.library.types.StringType("CONFIGURED"));
        } else {
            updateState(CHANNEL_WEBHOOK_STATUS, 
                    new org.openhab.core.library.types.StringType("NOT_CONFIGURED"));
        }
    }

    /**
     * Start device discovery
     */
    private void startDiscovery() {
        // Use startBackgroundDiscovery() instead of protected startScan()
        RachioDiscoveryService localDiscoveryService = discoveryService;
        if (localDiscoveryService != null) {
            localDiscoveryService.startBackgroundDiscovery();
            logger.debug("Started background discovery");
        } else {
            logger.debug("Discovery service not available");
        }
    }

    /**
     * Set discovery service
     */
    public void setDiscoveryService(RachioDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
        discoveryService.setBridgeHandler(this);
        logger.debug("Discovery service set");
    }

    /**
     * Get device by ID
     */
    public @Nullable RachioDevice getDevice(String deviceId) {
        // Try cache first
        RachioDevice cachedDevice = deviceCache.get(deviceId);
        if (cachedDevice != null) {
            return cachedDevice;
        }
        
        // If not in cache, try to fetch from API
        RachioHttp localHttpClient = httpClient;
        if (localHttpClient != null) {
            try {
                RachioDevice device = localHttpClient.getDevice(deviceId);
                if (device != null) {
                    deviceCache.put(deviceId, device);
                    return device;
                }
            } catch (Exception e) {
                logger.debug("Error fetching device {} from API: {}", deviceId, e.getMessage());
            }
        }
        
        return null;
    }

    /**
     * Get zone by ID (searches through all devices)
     */
    public @Nullable RachioZone getZone(String zoneId) {
        // First check cache
        for (RachioDevice device : deviceCache.values()) {
            if (device.getZones() != null) {
                for (RachioZone zone : device.getZones()) {
                    if (zoneId.equals(zone.getId())) {
                        return zone;
                    }
                }
            }
        }
        
        // If not in cache, poll devices to refresh cache
        pollDevices();
        
        // Check again after poll
        for (RachioDevice device : deviceCache.values()) {
            if (device.getZones() != null) {
                for (RachioZone zone : device.getZones()) {
                    if (zoneId.equals(zone.getId())) {
                        return zone;
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Get all devices for discovery
     */
    public List<RachioDevice> discoverDevices() {
        // Ensure cache is populated
        if (deviceCache.isEmpty()) {
            pollDevices();
        }
        return new ArrayList<>(deviceCache.values());
    }

    /**
     * Get HTTP client
     */
    public @Nullable RachioHttp getHttpClient() {
        return httpClient;
    }

    /**
     * Get security utility
     */
    public @Nullable RachioSecurity getSecurity() {
        return security;
    }

    /**
     * Get person ID
     */
    public @Nullable String getPersonId() {
        return personId;
    }

    /**
     * Get bridge configuration
     */
    public @Nullable RachioBridgeConfiguration getBridgeConfig() {
        return config;
    }

    /**
     * Status listener management
     */
    public void registerStatusListener(RachioStatusListener listener) {
        if (listener != null) {
            statusListeners.add(listener);
            logger.debug("Registered status listener: {}", listener.getClass().getSimpleName());
        }
    }

    public void unregisterStatusListener(RachioStatusListener listener) {
        if (listener != null) {
            statusListeners.remove(listener);
            logger.debug("Unregistered status listener: {}", listener.getClass().getSimpleName());
        }
    }

    /**
     * Notify all status listeners of device updates
     */
    private void notifyDeviceUpdates(List<RachioDevice> devices) {
        for (RachioStatusListener listener : statusListeners) {
            try {
                for (RachioDevice device : devices) {
                    listener.onDeviceUpdated(device.getId());
                }
            } catch (Exception e) {
                logger.warn("Error notifying status listener {}", listener.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Notify all status listeners of rate limit changes
     */
    private void notifyRateLimitChanged(int remainingRequests, int limit, long resetTime) {
        for (RachioStatusListener listener : statusListeners) {
            try {
                listener.onRateLimitChanged(remainingRequests, limit, resetTime);
            } catch (Exception e) {
                logger.warn("Error notifying rate limit change to listener {}", 
                        listener.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Notify all status listeners of a status change
     */
    protected void notifyStatusListeners(ThingStatus status, ThingStatusDetail detail, @Nullable String message) {
        for (RachioStatusListener listener : statusListeners) {
            try {
                listener.onStatusChanged(status, detail, message);
            } catch (Exception e) {
                logger.warn("Error notifying status listener {}", listener.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Get all status listeners (for testing/debugging)
     */
    protected Set<RachioStatusListener> getStatusListeners() {
        return new HashSet<>(statusListeners);
    }

    /**
     * Clear device cache (force refresh on next poll)
     */
    public void clearDeviceCache() {
        deviceCache.clear();
        logger.debug("Device cache cleared");
        
        // Also clear HTTP client cache if available
        RachioHttp localHttpClient = httpClient;
        if (localHttpClient != null) {
            localHttpClient.clearCache();
        }
    }

    /**
     * Check if webhook is configured
     */
    public boolean isWebhookConfigured() {
        RachioBridgeConfiguration localConfig = config;
        return localConfig != null && localConfig.webhookUrl != null && !localConfig.webhookUrl.isEmpty();
    }

    /**
     * Get webhook URL
     */
    public @Nullable String getWebhookUrl() {
        RachioBridgeConfiguration localConfig = config;
        return localConfig != null ? localConfig.webhookUrl : null;
    }

    /**
     * Get webhook secret
     */
    public @Nullable String getWebhookSecret() {
        RachioBridgeConfiguration localConfig = config;
        return localConfig != null ? localConfig.webhookSecret : null;
    }

    /**
     * Get current rate limit info
     */
    public Map<String, Object> getRateLimitInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("remaining", rateLimitRemaining);
        info.put("total", rateLimitTotal);
        info.put("reset", rateLimitReset);
        info.put("percentage", rateLimitTotal > 0 ? 
                (int) ((rateLimitRemaining / (double) rateLimitTotal) * 100) : 0);
        info.put("status", rateLimitRemaining <= RATE_LIMIT_THRESHOLD_CRITICAL ? "CRITICAL" :
                         rateLimitRemaining <= RATE_LIMIT_THRESHOLD_LOW ? "LOW" : "NORMAL");
        return info;
    }

    /**
     * Update bridge status and notify listeners
     */
    @Override
    protected void updateStatus(ThingStatus status, ThingStatusDetail statusDetail, @Nullable String description) {
        super.updateStatus(status, statusDetail, description);
        notifyStatusListeners(status, statusDetail, description);
    }
}
