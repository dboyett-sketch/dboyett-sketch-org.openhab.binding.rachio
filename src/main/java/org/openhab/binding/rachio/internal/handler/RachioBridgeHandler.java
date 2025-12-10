package org.openhab.binding.rachio.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiException;
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
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZonedDateTime;
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
public class RachioBridgeHandler extends RachioHandler {

    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);
    private final HttpClientFactory httpClientFactory;
    
    private final Map<String, RachioDevice> deviceCache = new ConcurrentHashMap<>();
    private final Set<RachioStatusListener> statusListeners = ConcurrentHashMap.newKeySet();

    private @Nullable RachioDiscoveryService discoveryService;
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

    // ===== Listener Management Methods =====
    
    /**
     * Register a status listener with this bridge.
     *
     * @param listener the listener to register
     */
    public void registerStatusListener(RachioStatusListener listener) {
        if (listener != null) {
            statusListeners.add(listener);
            logger.debug("Registered status listener: {}", listener.getListenerDescription());
        }
    }

    /**
     * Unregister a status listener from this bridge.
     *
     * @param listener the listener to unregister
     */
    public void unregisterStatusListener(RachioStatusListener listener) {
        if (listener != null) {
            statusListeners.remove(listener);
            logger.debug("Unregistered status listener: {}", listener.getListenerDescription());
        }
    }

    /**
     * Notify all registered listeners of a status change.
     *
     * @param status the new thing status
     * @param detail the status detail
     * @param message optional description message
     */
    protected void notifyStatusListeners(ThingStatus status, ThingStatusDetail detail, @Nullable String message) {
        for (RachioStatusListener listener : statusListeners) {
            if (listener.isActive()) {
                try {
                    listener.onStatusChanged(status, detail, message);
                } catch (Exception e) {
                    logger.warn("Error notifying status listener {}", listener.getListenerDescription(), e);
                }
            }
        }
    }

    /**
     * Notify all registered listeners of a device update.
     *
     * @param deviceId the ID of the device that was updated
     */
    protected void notifyDeviceUpdated(String deviceId) {
        for (RachioStatusListener listener : statusListeners) {
            if (listener.isActive() && listener.isForDevice(deviceId)) {
                try {
                    listener.onDeviceUpdated(deviceId);
                } catch (Exception e) {
                    logger.warn("Error notifying device update to listener {}", listener.getListenerDescription(), e);
                }
            }
        }
    }

    /**
     * Notify all registered listeners of a zone update.
     *
     * @param zoneId the ID of the zone that was updated
     */
    protected void notifyZoneUpdated(String zoneId) {
        for (RachioStatusListener listener : statusListeners) {
            if (listener.isActive() && listener.isForZone(zoneId)) {
                try {
                    listener.onZoneUpdated(zoneId);
                } catch (Exception e) {
                    logger.warn("Error notifying zone update to listener {}", listener.getListenerDescription(), e);
                }
            }
        }
    }

    /**
     * Notify device updates to listeners (used by pollDevices)
     */
    private void notifyDeviceUpdates(List<RachioDevice> devices) {
        // Simple notification - could be enhanced
        for (RachioDevice device : devices) {
            if (device.getId() != null) {
                notifyDeviceUpdated(device.getId());
            }
        }
        logger.debug("Notified listeners of {} device updates", devices.size());
    }

    /**
     * Notify rate limit changes to listeners
     */
    private void notifyRateLimitChanged(int remaining, int total, long reset) {
        for (RachioStatusListener listener : statusListeners) {
            if (listener.isActive()) {
                try {
                    listener.onRateLimitChanged(remaining, total, reset);
                } catch (Exception e) {
                    logger.warn("Error notifying rate limit change to listener {}", listener.getListenerDescription(), e);
                }
            }
        }
    }

    /**
     * Notify connection changes to listeners
     */
    private void notifyConnectionChanged(boolean connected, @Nullable String message) {
        for (RachioStatusListener listener : statusListeners) {
            if (listener.isActive()) {
                try {
                    listener.onConnectionChanged(connected, message);
                } catch (Exception e) {
                    logger.warn("Error notifying connection change to listener {}", listener.getListenerDescription(), e);
                }
            }
        }
    }

    /**
     * Notify errors to listeners
     */
    private void notifyError(String errorMessage, @Nullable Throwable exception) {
        for (RachioStatusListener listener : statusListeners) {
            if (listener.isActive()) {
                try {
                    listener.onError(errorMessage, exception);
                } catch (Exception e) {
                    logger.warn("Error notifying error to listener {}", listener.getListenerDescription(), e);
                }
            }
        }
    }

    // ===== End Listener Management Methods =====

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
            rachioHttp = new RachioHttp(config.apiKey, httpClientFactory);
            rachioSecurity = new RachioSecurity();
            
            // Set initial rate limit values from HTTP client
            if (rachioHttp != null) {
                rateLimitRemaining = rachioHttp.getRemainingRequests();
                rateLimitTotal = rachioHttp.getRateLimit();
                rateLimitReset = rachioHttp.getResetTime();
            }
            
            updateStatus(ThingStatus.UNKNOWN);
            
            // Start initial poll
            scheduler.execute(() -> {
                try {
                    // Get person info to retrieve devices
                    if (rachioHttp != null) {
                        RachioPerson person = rachioHttp.getPersonInfo();
                        if (person != null && person.getId() != null) {
                            personId = person.getId();
                            logger.debug("Retrieved person ID: {}", personId);
                            
                            // Update rate limits after successful API call
                            updateRateLimitChannels();
                            
                            updateStatus(ThingStatus.ONLINE);
                            startDiscovery();
                            
                            // Notify listeners
                            notifyStatusListeners(ThingStatus.ONLINE, ThingStatusDetail.NONE, 
                                    "Bridge initialized successfully");
                            notifyConnectionChanged(true, "Bridge initialized successfully");
                            
                            // Start polling
                            startPolling();
                        } else {
                            String errorMsg = "Failed to retrieve person information from Rachio API";
                            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMsg);
                            notifyStatusListeners(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMsg);
                            notifyConnectionChanged(false, errorMsg);
                        }
                    }
                } catch (Exception e) {
                    String errorMsg = "Error during bridge initialization: " + e.getMessage();
                    logger.error(errorMsg, e);
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMsg);
                    notifyStatusListeners(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMsg);
                    notifyError(errorMsg, e);
                }
            });
            
        } catch (Exception e) {
            String errorMsg = "Failed to initialize HTTP client: " + e.getMessage();
            logger.error(errorMsg, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMsg);
            notifyStatusListeners(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMsg);
            notifyError(errorMsg, e);
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio bridge handler");
        
        stopPolling();
        
        // Clear caches and listeners
        deviceCache.clear();
        statusListeners.clear();
        
        // Clear services
        discoveryService = null;
        config = null;
        personId = null;
        
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
            notifyError(e.getMessage(), e);
        }
    }

    /**
     * Poll devices from Rachio API
     * Changed from private to package-private to allow access from device/zone handlers
     */
    void pollDevices() {
        RachioHttp localHttpClient = rachioHttp;
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
                
                // Update rate limits from HTTP client
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
                
                // Notify connection is good
                notifyConnectionChanged(true, "Successfully polled devices");
            } else {
                logger.warn("No devices found in person info");
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, 
                        "No devices found in API response");
                notifyStatusListeners(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, 
                        "No devices found in API response");
                notifyConnectionChanged(false, "No devices found in API response");
            }
        } catch (Exception e) {
            logger.error("Error polling devices", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, 
                    "Polling error: " + e.getMessage());
            notifyStatusListeners(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, 
                    "Polling error: " + e.getMessage());
            notifyError("Polling error: " + e.getMessage(), e);
        }
    }

    /**
     * Implementation of abstract method from RachioHandler
     */
    @Override
    protected void pollStatus() throws RachioApiException {
        pollDevices();
    }

    /**
     * Get polling interval from configuration
     */
    @Override
    protected int getPollingInterval() {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig != null && localConfig.refresh > 0) {
            return localConfig.refresh;
        }
        return super.getPollingInterval(); // Default from RachioHandler
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
                            ZonedDateTime.ofInstant(Instant.ofEpochMilli(rateLimitReset), 
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
        RachioHttp localHttpClient = rachioHttp;
        if (localHttpClient != null) {
            try {
                RachioDevice device = localHttpClient.getDevice(deviceId);
                if (device != null) {
                    deviceCache.put(deviceId, device);
                    return device;
                }
            } catch (Exception e) {
                logger.debug("Error fetching device from API: {}", e.getMessage());
            }
        }
        return null;
    }

    /**
     * Get zone by ID
     */
    public @Nullable RachioZone getZone(String zoneId) {
        // Search through cached devices for the zone
        for (RachioDevice device : deviceCache.values()) {
            if (device.getZones() != null) {
                for (RachioZone zone : device.getZones()) {
                    if (zoneId.equals(zone.getId())) {
                        return zone;
                    }
                }
            }
        }
        
        // If not found in cache, try to find via API
        RachioHttp localHttpClient = rachioHttp;
        if (localHttpClient != null) {
            try {
                // We need to find which device this zone belongs to first
                for (RachioDevice device : deviceCache.values()) {
                    if (device.getZones() != null) {
                        for (RachioZone zone : device.getZones()) {
                            if (zoneId.equals(zone.getId())) {
                                return zone;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Error fetching zone from cache: {}", e.getMessage());
            }
        }
        return null;
    }

    /**
     * Get the RachioHttp instance
     */
    public @Nullable RachioHttp getRachioHttp() {
        return rachioHttp;
    }

    /**
     * Get the RachioSecurity instance
     */
    public @Nullable RachioSecurity getRachioSecurity() {
        return rachioSecurity;
    }

    /**
     * Get person ID
     */
    public @Nullable String getPersonId() {
        return personId;
    }

    // ===== RachioStatusListener interface implementations =====
    // Note: RachioBridgeHandler extends RachioHandler which implements RachioStatusListener
    // These are the bridge-specific implementations

    @Override
    public void onDeviceUpdated(String deviceId) {
        // Bridge doesn't need to react to device updates from other listeners
        logger.debug("Device {} update received at bridge", deviceId);
    }

    @Override
    public void onZoneUpdated(String zoneId) {
        // Bridge doesn't need to react to zone updates from other listeners
        logger.debug("Zone {} update received at bridge", zoneId);
    }

    @Override
    public void onWebhookEvent(String deviceId, String eventType, 
                               @Nullable String subType, 
                               @Nullable Map<String, Object> eventData) {
        // Handle webhook events at bridge level
        logger.debug("Webhook event {} received for device {} at bridge", eventType, deviceId);
        
        // Forward to appropriate device/zone handlers via notifications
        notifyDeviceUpdated(deviceId);
        
        // If event contains zone information, also notify zone handlers
        if (eventData != null && eventData.containsKey("zoneId")) {
            Object zoneIdObj = eventData.get("zoneId");
            if (zoneIdObj instanceof String) {
                notifyZoneUpdated((String) zoneIdObj);
            }
        }
    }

    @Override
    public void onRateLimitChanged(int remainingRequests, int limit, long resetTime) {
        // Update bridge's rate limit state
        this.rateLimitRemaining = remainingRequests;
        this.rateLimitTotal = limit;
        this.rateLimitReset = resetTime;
        
        updateRateLimitChannels();
    }

    @Override
    public void onConnectionChanged(boolean connected, @Nullable String message) {
        // Bridge handles its own connection status
        if (!connected) {
            logger.warn("Connection change notified to bridge: {}", message);
        }
    }

    @Override
    public void onError(String errorMessage, @Nullable Throwable exception) {
        // Bridge handles its own errors
        logger.error("Error notified to bridge: {}", errorMessage, exception);
    }

    @Override
    public @Nullable String getThingId() {
        return getThing().getUID().getId();
    }

    @Override
    public boolean isForDevice(String deviceId) {
        // Bridge handles all devices
        return true;
    }
    
    @Override
    public boolean isActive() {
        return getThing().getStatus() != ThingStatus.UNINITIALIZED;
    }
    
    @Override
    public String getListenerDescription() {
        return "RachioBridgeHandler[" + getThing().getUID() + "]";
    }
    
    @Override
    public boolean isForZone(String zoneId) {
        // Bridge handles all zones
        return true;
    }
}
