package org.openhab.binding.rachio.internal.handler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.RachioActions;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.RachioSecurity;
import org.openhab.binding.rachio.internal.api.RachioWebHookServletService;
import org.openhab.binding.rachio.internal.api.dto.RachioAlert;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioEventSummary;
import org.openhab.binding.rachio.internal.api.dto.RachioForecast;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioSchedule;
import org.openhab.binding.rachio.internal.api.dto.RachioUsage;
import org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.api.dto.ZoneRunStatus;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for the Rachio bridge (API connection)
 *
 * @author Dave Boyett - Initial contribution
 */
@Component(service = RachioHandler.class)
@NonNullByDefault
public class RachioBridgeHandler extends BaseBridgeHandler implements RachioHandler, RachioActions {

    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);

    private @Nullable RachioBridgeConfiguration config;
    private @Nullable RachioHttp rachioHttp;
    private @Nullable RachioSecurity security;
    private @Nullable RachioWebHookServletService webhookService;
    private @Nullable ScheduledFuture<?> pollingJob;
    private @Nullable ScheduledFuture<?> webhookHealthJob;

    private final List<RachioStatusListener> statusListeners = new CopyOnWriteArrayList<>();
    private final Map<String, RachioDevice> deviceCache = new HashMap<>();
    private @Nullable Instant lastUpdate;

    private int adaptivePollingInterval = 60; // seconds
    private boolean webhookRegistered = false;
    private @Nullable Instant webhookLastChecked;

    @Activate
    public RachioBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        config = getConfigAs(RachioBridgeConfiguration.class);
        
        if (config.apiKey == null || config.apiKey.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "API key must be configured");
            return;
        }
        
        // Initialize HTTP client
        rachioHttp = new RachioHttp(this);
        rachioHttp.initialize(config);
        
        // Initialize security
        security = new RachioSecurity();
        
        // Initialize webhook service if enabled
        if (config.webhookEnabled) {
            initializeWebhookService();
        }
        
        // Start polling
        startPolling();
        
        // Initial refresh
        scheduler.schedule(this::refreshBridge, 2, TimeUnit.SECONDS);
        
        logger.debug("Rachio bridge handler initialized");
    }

    @Override
    public void dispose() {
        stopPolling();
        stopWebhookHealthCheck();
        
        if (config != null && config.webhookEnabled && webhookService != null) {
            try {
                webhookService.unregisterWebhook();
                logger.debug("Webhook unregistered during disposal");
            } catch (Exception e) {
                logger.warn("Error unregistering webhook: {}", e.getMessage());
            }
        }
        
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String channelId = channelUID.getIdWithoutGroup();
        
        if (command instanceof RefreshType) {
            refreshBridge();
            return;
        }
        
        switch (channelId) {
            case RachioBindingConstants.CHANNEL_REFRESH:
                if (command instanceof OnOffType && command == OnOffType.ON) {
                    refreshBridge();
                    updateState(channelUID, OnOffType.OFF);
                }
                break;
                
            case RachioBindingConstants.CHANNEL_WEBHOOK_REGISTER:
                if (command instanceof OnOffType) {
                    boolean enable = command == OnOffType.ON;
                    if (enable) {
                        registerWebhook();
                    } else {
                        unregisterWebhook();
                    }
                }
                break;
                
            case RachioBindingConstants.CHANNEL_CLEAR_CACHE:
                if (command instanceof OnOffType && command == OnOffType.ON) {
                    clearCache();
                    updateState(channelUID, OnOffType.OFF);
                }
                break;
                
            default:
                logger.debug("Unhandled command for channel {}: {}", channelId, command);
        }
    }

    /**
     * Initialize webhook service
     */
    private void initializeWebhookService() {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig == null || !localConfig.webhookEnabled) {
            return;
        }
        
        try {
            String externalUrl = determineExternalUrl(localConfig);
            webhookService = new RachioWebHookServletService(this, localConfig.apiKey, 
                    localConfig.webhookSecret, externalUrl);
            
            // Register webhook
            registerWebhook();
            
            // Start webhook health check
            startWebhookHealthCheck();
            
            logger.info("Webhook service initialized with URL: {}", externalUrl);
            
        } catch (Exception e) {
            logger.error("Failed to initialize webhook service: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Webhook configuration error: " + e.getMessage());
        }
    }

    /**
     * Determine external URL for webhooks
     */
    private String determineExternalUrl(RachioBridgeConfiguration config) {
        if (config.webhookExternalUrl != null && !config.webhookExternalUrl.isEmpty()) {
            return config.webhookExternalUrl;
        }
        
        try {
            String localAddress = InetAddress.getLocalHost().getHostAddress();
            return "http://" + localAddress + ":" + (config.webhookPort > 0 ? config.webhookPort : 8080);
        } catch (UnknownHostException e) {
            logger.warn("Could not determine local address: {}", e.getMessage());
            return "http://localhost:8080";
        }
    }

    /**
     * Register webhook with Rachio API
     */
    private void registerWebhook() {
        RachioWebHookServletService localService = webhookService;
        if (localService == null) {
            logger.warn("Webhook service not initialized");
            return;
        }
        
        try {
            boolean success = localService.registerWebhook();
            webhookRegistered = success;
            
            if (success) {
                logger.info("Webhook registered successfully");
                updateState(RachioBindingConstants.CHANNEL_WEBHOOK_STATUS, 
                        new StringType("REGISTERED"));
            } else {
                logger.error("Failed to register webhook");
                updateState(RachioBindingConstants.CHANNEL_WEBHOOK_STATUS, 
                        new StringType("FAILED"));
            }
            
            updateState(RachioBindingConstants.CHANNEL_WEBHOOK_REGISTERED, 
                    OnOffType.from(webhookRegistered));
            
        } catch (Exception e) {
            logger.error("Error registering webhook: {}", e.getMessage(), e);
            webhookRegistered = false;
            updateState(RachioBindingConstants.CHANNEL_WEBHOOK_STATUS, 
                    new StringType("ERROR"));
        }
    }

    /**
     * Unregister webhook
     */
    private void unregisterWebhook() {
        RachioWebHookServletService localService = webhookService;
        if (localService == null) {
            return;
        }
        
        try {
            boolean success = localService.unregisterWebhook();
            if (success) {
                logger.info("Webhook unregistered successfully");
                webhookRegistered = false;
                updateState(RachioBindingConstants.CHANNEL_WEBHOOK_STATUS, 
                        new StringType("UNREGISTERED"));
                updateState(RachioBindingConstants.CHANNEL_WEBHOOK_REGISTERED, OnOffType.OFF);
            }
        } catch (Exception e) {
            logger.error("Error unregistering webhook: {}", e.getMessage(), e);
        }
    }

    /**
     * Start webhook health check
     */
    private void startWebhookHealthCheck() {
        stopWebhookHealthCheck();
        
        webhookHealthJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                checkWebhookHealth();
            } catch (Exception e) {
                logger.error("Error in webhook health check: {}", e.getMessage(), e);
            }
        }, 5, 300, TimeUnit.SECONDS); // Check every 5 minutes after 5-second delay
    }

    /**
     * Stop webhook health check
     */
    private void stopWebhookHealthCheck() {
        ScheduledFuture<?> localJob = webhookHealthJob;
        if (localJob != null) {
            localJob.cancel(true);
            webhookHealthJob = null;
        }
    }

    /**
     * Check webhook health and re-register if needed
     */
    private void checkWebhookHealth() {
        RachioWebHookServletService localService = webhookService;
        if (localService == null || !webhookRegistered) {
            return;
        }
        
        try {
            boolean isRegistered = localService.isWebhookRegistered();
            if (!isRegistered) {
                logger.warn("Webhook appears to be unregistered, attempting to re-register");
                registerWebhook();
            } else {
                logger.debug("Webhook health check passed");
                updateState(RachioBindingConstants.CHANNEL_WEBHOOK_STATUS, 
                        new StringType("HEALTHY"));
            }
            
            webhookLastChecked = Instant.now();
            
        } catch (Exception e) {
            logger.error("Error checking webhook health: {}", e.getMessage());
            updateState(RachioBindingConstants.CHANNEL_WEBHOOK_STATUS, 
                    new StringType("CHECK_FAILED"));
        }
    }

    /**
     * Start polling for updates
     */
    private void startPolling() {
        stopPolling(); // Ensure no existing polling job
        
        pollingJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                refreshBridge();
            } catch (Exception e) {
                logger.error("Error during bridge polling: {}", e.getMessage(), e);
            }
        }, 0, adaptivePollingInterval, TimeUnit.SECONDS);
        
        logger.debug("Started polling with interval {} seconds", adaptivePollingInterval);
    }

    /**
     * Stop polling
     */
    private void stopPolling() {
        ScheduledFuture<?> localPollingJob = pollingJob;
        if (localPollingJob != null) {
            localPollingJob.cancel(true);
            pollingJob = null;
        }
    }

    /**
     * Refresh bridge data
     */
    private void refreshBridge() {
        RachioHttp localHttp = rachioHttp;
        if (localHttp == null) {
            return;
        }
        
        try {
            // Get person info to verify API connection
            RachioPerson person = localHttp.getPerson();
            if (person != null) {
                updateStatus(ThingStatus.ONLINE);
                
                // Update channels
                updateBridgeChannels();
                
                // Update adaptive polling based on rate limits
                updateAdaptivePolling(localHttp);
                
                // Update device cache
                refreshDevices();
                
                lastUpdate = Instant.now();
                updateState(RachioBindingConstants.CHANNEL_LAST_UPDATE, 
                        new DateTimeType(java.time.ZonedDateTime.now()));
                
                logger.debug("Bridge refresh completed successfully");
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Could not retrieve person info");
            }
            
        } catch (RachioApiException e) {
            logger.error("Error refreshing bridge: {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    e.getMessage());
        }
    }

    /**
     * Update bridge channels
     */
    private void updateBridgeChannels() {
        RachioHttp localHttp = rachioHttp;
        if (localHttp == null) {
            return;
        }
        
        // Update rate limit channels
        Map<String, Object> rateLimitInfo = localHttp.getRateLimitInfo();
        updateState(RachioBindingConstants.CHANNEL_RATE_LIMIT_REMAINING, 
                new DecimalType((Integer) rateLimitInfo.getOrDefault("remaining", 0)));
        updateState(RachioBindingConstants.CHANNEL_RATE_LIMIT_TOTAL, 
                new DecimalType((Integer) rateLimitInfo.getOrDefault("total", 100)));
        updateState(RachioBindingConstants.CHANNEL_RATE_LIMIT_PERCENT, 
                new DecimalType(calculateRateLimitPercent(rateLimitInfo)));
        updateState(RachioBindingConstants.CHANNEL_RATE_LIMIT_STATE, 
                new StringType((String) rateLimitInfo.getOrDefault("state", "UNKNOWN")));
        
        // Update cache size
        updateState(RachioBindingConstants.CHANNEL_CACHE_SIZE, 
                new DecimalType(localHttp.getCacheSize()));
        
        // Update webhook status
        updateState(RachioBindingConstants.CHANNEL_WEBHOOK_REGISTERED, 
                OnOffType.from(webhookRegistered));
    }

    /**
     * Calculate rate limit percentage
     */
    private int calculateRateLimitPercent(Map<String, Object> rateLimitInfo) {
        int remaining = (Integer) rateLimitInfo.getOrDefault("remaining", 0);
        int total = (Integer) rateLimitInfo.getOrDefault("total", 100);
        
        if (total == 0) {
            return 0;
        }
        
        return (int) ((remaining * 100.0) / total);
    }

    /**
     * Update adaptive polling based on rate limits
     */
    private void updateAdaptivePolling(RachioHttp http) {
        int multiplier = http.getAdaptivePollingMultiplier();
        int newInterval = 60 * multiplier; // Base 60 seconds times multiplier
        
        if (newInterval != adaptivePollingInterval) {
            adaptivePollingInterval = newInterval;
            logger.info("Adaptive polling interval changed to {} seconds (multiplier: {})", 
                    adaptivePollingInterval, multiplier);
            
            // Restart polling with new interval
            stopPolling();
            startPolling();
        }
    }

    /**
     * Refresh devices and notify listeners
     */
    private void refreshDevices() {
        RachioHttp localHttp = rachioHttp;
        if (localHttp == null) {
            return;
        }
        
        try {
            List<RachioDevice> devices = localHttp.getDevices();
            deviceCache.clear();
            
            for (RachioDevice device : devices) {
                if (device.id != null) {
                    deviceCache.put(device.id, device);
                }
            }
            
            // Notify status listeners
            for (RachioStatusListener listener : statusListeners) {
                listener.deviceListUpdated(devices);
            }
            
            logger.debug("Refreshed {} devices", devices.size());
            
        } catch (RachioApiException e) {
            logger.error("Error refreshing devices: {}", e.getMessage());
        }
    }

    /**
     * Clear cache
     */
    private void clearCache() {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            localHttp.clearCache();
            deviceCache.clear();
            logger.info("Cache cleared");
        }
    }

    /**
     * Handle webhook event
     */
    public void handleWebhookEvent(RachioWebhookEvent event) {
        logger.debug("Handling webhook event: {} for device {}", event.type, event.deviceId);
        
        // Update device cache
        if (event.deviceId != null) {
            deviceCache.remove(event.deviceId);
        }
        
        // Notify status listeners
        for (RachioStatusListener listener : statusListeners) {
            listener.webhookEventReceived(event);
        }
        
        // Update channels based on event type
        updateChannelsFromWebhook(event);
    }

    /**
     * Update channels from webhook event
     */
    private void updateChannelsFromWebhook(RachioWebhookEvent event) {
        // Update last webhook time
        updateState(RachioBindingConstants.CHANNEL_WEBHOOK_LAST_EVENT, 
                new DateTimeType(java.time.ZonedDateTime.now()));
        
        // Update event type channel
        if (event.type != null) {
            updateState(RachioBindingConstants.CHANNEL_WEBHOOK_EVENT_TYPE, 
                    new StringType(event.type));
        }
        
        // Update device status if applicable
        if (event.deviceId != null && event.type != null) {
            if (event.type.contains("DEVICE_STATUS")) {
                String status = extractStatusFromEvent(event);
                if (status != null) {
                    updateDeviceStatus(event.deviceId, status);
                }
            } else if (event.type.contains("ZONE_STATUS")) {
                handleZoneStatusEvent(event);
            } else if (event.type.contains("RAIN_DELAY")) {
                handleRainDelayEvent(event);
            }
        }
    }

    /**
     * Extract status from webhook event
     */
    private @Nullable String extractStatusFromEvent(RachioWebhookEvent event) {
        if (event.type == null) {
            return null;
        }
        
        if (event.type.endsWith("_ONLINE")) {
            return "ONLINE";
        } else if (event.type.endsWith("_OFFLINE")) {
            return "OFFLINE";
        } else if (event.type.endsWith("_SLEEP")) {
            return "SLEEP";
        }
        
        return null;
    }

    /**
     * Handle zone status event
     */
    private void handleZoneStatusEvent(RachioWebhookEvent event) {
        // This is handled by zone handlers
        // Bridge just logs it
        logger.debug("Zone status event: {} for zone {}", event.type, event.zoneId);
    }

    /**
     * Handle rain delay event
     */
    private void handleRainDelayEvent(RachioWebhookEvent event) {
        if (event.deviceId != null && event.summary != null) {
            try {
                int delayHours = Integer.parseInt(event.summary);
                updateRainDelay(event.deviceId, delayHours);
                logger.debug("Rain delay set to {} hours for device {}", delayHours, event.deviceId);
            } catch (NumberFormatException e) {
                logger.warn("Could not parse rain delay from summary: {}", event.summary);
            }
        }
    }

    /**
     * Update device status
     */
    public void updateDeviceStatus(String deviceId, String status) {
        updateState(RachioBindingConstants.CHANNEL_DEVICE_STATUS, new StringType(status));
        logger.debug("Device {} status updated to {}", deviceId, status);
    }

    /**
     * Update zone status
     */
    public void updateZoneStatus(String zoneId, ZoneRunStatus status, int duration) {
        logger.debug("Zone {} status updated to {} with duration {}", zoneId, status, duration);
        // Zone handlers will update their own channels
    }

    /**
     * Update rain delay
     */
    public void updateRainDelay(String deviceId, int hours) {
        updateState(RachioBindingConstants.CHANNEL_RAIN_DELAY, new DecimalType(hours));
        logger.debug("Rain delay updated to {} hours for device {}", hours, deviceId);
    }

    /**
     * Update rate limit status
     */
    public void updateRateLimitStatus(String state, int remaining, int total, @Nullable Instant reset) {
        // Channels are updated in updateBridgeChannels()
        logger.debug("Rate limit status: {} - {}/{}", state, remaining, total);
    }

    /**
     * Get actions interface
     */
    public @Nullable RachioActions getActions() {
        return this;
    }

    /**
     * Add status listener
     */
    public void addStatusListener(RachioStatusListener listener) {
        statusListeners.add(listener);
    }

    /**
     * Remove status listener
     */
    public void removeStatusListener(RachioStatusListener listener) {
        statusListeners.remove(listener);
    }

    // ========== RachioActions Interface Implementation ==========

    @Override
    public void startZone(String thingId, String zoneId, int duration, String deviceId) {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            localHttp.startZone(thingId, zoneId, duration, deviceId);
        }
    }

    @Override
    public void stopWatering(String thingId, String deviceId) {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            localHttp.stopWatering(thingId, deviceId);
        }
    }

    @Override
    public void runAllZones(String thingId, int duration, String deviceId) {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            localHttp.runAllZones(thingId, duration, deviceId);
        }
    }

    @Override
    public void rainDelay(String thingId, int hours, String deviceId) {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            localHttp.rainDelay(thingId, hours, deviceId);
        }
    }

    @Override
    public void runNextZone(String thingId, int duration, String deviceId) {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            localHttp.runNextZone(thingId, duration, deviceId);
        }
    }

    @Override
    public void setZoneEnabled(String thingId, String zoneId, boolean enabled, String deviceId) {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            localHttp.setZoneEnabled(thingId, zoneId, enabled, deviceId);
        }
    }

    @Override
    public @Nullable RachioPerson getPerson() throws RachioApiException {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            return localHttp.getPerson();
        }
        throw new RachioApiException("Rachio HTTP client not initialized");
    }

    @Override
    public @Nullable RachioDevice getDevice(String deviceId) throws RachioApiException {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            return localHttp.getDevice(deviceId);
        }
        throw new RachioApiException("Rachio HTTP client not initialized");
    }

    @Override
    public List<RachioDevice> getDevices() throws RachioApiException {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            return localHttp.getDevices();
        }
        throw new RachioApiException("Rachio HTTP client not initialized");
    }

    @Override
    public List<RachioEventSummary> getEvents(String deviceId, int count) throws RachioApiException {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            return localHttp.getEvents(deviceId, count);
        }
        throw new RachioApiException("Rachio HTTP client not initialized");
    }

    @Override
    public @Nullable RachioForecast getForecast(String deviceId) throws RachioApiException {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            return localHttp.getForecast(deviceId);
        }
        throw new RachioApiException("Rachio HTTP client not initialized");
    }

    @Override
    public @Nullable RachioUsage getUsage(String deviceId, int year) throws RachioApiException {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            return localHttp.getUsage(deviceId, year);
        }
        throw new RachioApiException("Rachio HTTP client not initialized");
    }

    @Override
    public @Nullable RachioUsage getSavings(String deviceId, int year) throws RachioApiException {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            return localHttp.getSavings(deviceId, year);
        }
        throw new RachioApiException("Rachio HTTP client not initialized");
    }

    @Override
    public void pauseDevice(String deviceId, boolean pause) throws RachioApiException {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            localHttp.pauseDevice(deviceId, pause);
        } else {
            throw new RachioApiException("Rachio HTTP client not initialized");
        }
    }

    @Override
    public List<RachioAlert> getAlerts(String deviceId) throws RachioApiException {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            return localHttp.getAlerts(deviceId);
        }
        throw new RachioApiException("Rachio HTTP client not initialized");
    }

    @Override
    public List<RachioSchedule> getSchedules(String deviceId) throws RachioApiException {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            return localHttp.getSchedules(deviceId);
        }
        throw new RachioApiException("Rachio HTTP client not initialized");
    }

    @Override
    public List<java.util.Map<String, Object>> getWateringHistory(String zoneId, int count) 
            throws RachioApiException {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            return localHttp.getWateringHistory(zoneId, count);
        }
        throw new RachioApiException("Rachio HTTP client not initialized");
    }

    @Override
    public void processWebhookEvent(RachioWebhookEvent event) {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            localHttp.processWebhookEvent(event);
        }
    }

    @Override
    public void clearCache() {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            localHttp.clearCache();
        }
    }

    @Override
    public int getCacheSize() {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            return localHttp.getCacheSize();
        }
        return 0;
    }

    @Override
    public Map<String, Object> getRateLimitInfo() {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            return localHttp.getRateLimitInfo();
        }
        return Collections.emptyMap();
    }

    @Override
    public int getAdaptivePollingMultiplier() {
        RachioHttp localHttp = rachioHttp;
        if (localHttp != null) {
            return localHttp.getAdaptivePollingMultiplier();
        }
        return 1;
    }

    @Override
    public boolean validateWebhookSignature(String payload, String signature, String secret) {
        RachioSecurity localSecurity = security;
        if (localSecurity != null) {
            return localSecurity.validateWebhookSignature(payload, signature, secret);
        }
        return false;
    }

    /**
     * Get bridge configuration
     */
    public @Nullable RachioBridgeConfiguration getBridgeConfig() {
        return config;
    }

    /**
     * Get webhook service
     */
    public @Nullable RachioWebHookServletService getWebhookService() {
        return webhookService;
    }
}
