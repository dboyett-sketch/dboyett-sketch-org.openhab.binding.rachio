package org.openhab.binding.rachio.internal.handler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.RachioActions;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.RachioImageServletService;
import org.openhab.binding.rachio.internal.api.RachioWebHookServletService;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
import org.openhab.binding.rachio.internal.discovery.RachioDiscoveryService;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Michael Lobstein - Initial contribution
 * @author Your Name - Enhanced for OpenHAB 5.x
 */
@NonNullByDefault
public class RachioBridgeHandler extends RachioHandler implements RachioActions {

    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);

    // Configuration
    private @Nullable RachioBridgeConfiguration config;

    // Services
    private @Nullable RachioHttp rachioHttp;
    private @Nullable RachioWebHookServletService webhookService;
    private @Nullable RachioImageServletService imageService;

    // Data cache
    private @Nullable RachioPerson person;
    private final Map<String, RachioDevice> devices = new ConcurrentHashMap<>();
    private final Map<String, List<RachioZone>> deviceZones = new ConcurrentHashMap<>();

    // Listeners
    private final List<RachioStatusListener> listeners = Collections.synchronizedList(new ArrayList<>());

    // Scheduling
    private @Nullable ScheduledFuture<?> pollingJob;
    private @Nullable ScheduledFuture<?> webhookCheckJob;
    private long lastPollTime = 0;
    private static final int POLLING_INTERVAL = 60; // seconds
    private static final int WEBHOOK_CHECK_INTERVAL = 300; // seconds

    // Rate limiting
    private int remainingRequests = 300;
    private int limitRequests = 300;
    private long resetTime = 0;

    /**
     * Constructor
     *
     * @param bridge the bridge
     */
    public RachioBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio bridge handler");

        // Load configuration
        config = getConfigAs(RachioBridgeConfiguration.class);
        if (config == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Configuration is null");
            return;
        }

        // Validate API key
        if (config.apiKey == null || config.apiKey.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatus.CONFIGURATION_ERROR, "API key not configured");
            return;
        }

        // Initialize common handler functionality
        initializeCommon();

        // Create HTTP client
        rachioHttp = new RachioHttp(this, config);

        // Initialize services
        try {
            if (config.webhookEnabled) {
                webhookService = new RachioWebHookServletService(this, config);
                webhookService.activate();
            }
            
            imageService = new RachioImageServletService(this);
            imageService.activate();
        } catch (Exception e) {
            logger.error("Error initializing services: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Service initialization failed: " + e.getMessage());
            return;
        }

        // Start polling
        startPolling();

        // Initial status
        updateStatus(ThingStatus.UNKNOWN);
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio bridge handler");

        // Stop polling
        stopPolling();

        // Dispose services
        if (webhookService != null) {
            webhookService.deactivate();
        }
        if (imageService != null) {
            imageService.deactivate();
        }

        // Clear data
        devices.clear();
        deviceZones.clear();
        listeners.clear();

        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            if (command instanceof RefreshType) {
                logger.debug("Refresh command received for channel: {}", channelUID.getId());
                refreshChannel(channelUID);
            } else {
                logger.debug("Command {} received for channel: {}", command, channelUID.getId());
                handleChannelCommand(channelUID, command);
            }
        } catch (Exception e) {
            logger.error("Error handling command: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void refreshChannel(ChannelUID channelUID) {
        String channelId = channelUID.getId();
        
        switch (channelId) {
            case RachioBindingConstants.CHANNEL_RATE_LIMIT_REMAINING:
                updateState(channelUID, new org.openhab.core.library.types.QuantityType<>(remainingRequests, org.openhab.core.library.unit.Units.ONE));
                break;
            case RachioBindingConstants.CHANNEL_RATE_LIMIT_PERCENT:
                double percent = limitRequests > 0 ? (remainingRequests * 100.0) / limitRequests : 0;
                updateState(channelUID, new org.openhab.core.library.types.QuantityType<>(percent, org.openhab.core.library.unit.Units.PERCENT));
                break;
            case RachioBindingConstants.CHANNEL_RATE_LIMIT_RESET:
                if (resetTime > 0) {
                    long secondsUntilReset = Math.max(0, resetTime - System.currentTimeMillis() / 1000);
                    updateState(channelUID, new org.openhab.core.library.types.QuantityType<>(secondsUntilReset, org.openhab.core.library.unit.Units.SECOND));
                }
                break;
            default:
                logger.debug("Unhandled refresh for channel: {}", channelId);
        }
    }

    @Override
    protected void handleChannelCommand(ChannelUID channelUID, Command command) {
        // Bridge-specific commands can be handled here
        logger.debug("Bridge command {} for channel {} not implemented", command, channelUID.getId());
    }

    @Override
    public void refreshAllChannels() {
        // Refresh all bridge channels
        getThing().getChannels().forEach(channel -> {
            refreshChannel(channel.getUID());
        });
    }

    /**
     * Start polling for device updates
     */
    private void startPolling() {
        stopPolling(); // Ensure any existing job is stopped
        
        pollingJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                pollDevices();
            } catch (Exception e) {
                logger.error("Error polling devices: {}", e.getMessage(), e);
            }
        }, 10, POLLING_INTERVAL, TimeUnit.SECONDS);

        // Start webhook health check if enabled
        if (config != null && config.webhookEnabled) {
            webhookCheckJob = scheduler.scheduleWithFixedDelay(() -> {
                try {
                    checkWebhookHealth();
                } catch (Exception e) {
                    logger.error("Error checking webhook health: {}", e.getMessage(), e);
                }
            }, 30, WEBHOOK_CHECK_INTERVAL, TimeUnit.SECONDS);
        }
    }

    /**
     * Stop polling
     */
    private void stopPolling() {
        if (pollingJob != null) {
            pollingJob.cancel(true);
            pollingJob = null;
        }
        if (webhookCheckJob != null) {
            webhookCheckJob.cancel(true);
            webhookCheckJob = null;
        }
    }

    /**
     * Poll devices from Rachio API
     */
    private void pollDevices() {
        if (rachioHttp == null) {
            logger.debug("RachioHttp not initialized, skipping poll");
            return;
        }

        try {
            logger.debug("Polling Rachio devices");
            
            // Get person info
            RachioPerson currentPerson = rachioHttp.getPersonInfo();
            if (currentPerson != null) {
                person = currentPerson;
                logger.debug("Retrieved person: {}", person.username);
            }

            // Get devices
            List<RachioDevice> deviceList = rachioHttp.getDeviceList();
            if (deviceList != null && !deviceList.isEmpty()) {
                devices.clear();
                deviceZones.clear();
                
                for (RachioDevice device : deviceList) {
                    devices.put(device.id, device);
                    
                    // Get zones for this device
                    List<RachioZone> zones = rachioHttp.getZoneList(device.id);
                    if (zones != null) {
                        deviceZones.put(device.id, zones);
                        logger.debug("Device {} has {} zones", device.name, zones.size());
                    }
                }
                
                logger.debug("Retrieved {} devices", devices.size());
                
                // Notify listeners
                notifyDeviceListUpdated(deviceList);
                
                // Update status
                updateStatus(ThingStatus.ONLINE);
            } else {
                logger.debug("No devices found");
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "No devices found");
            }

            // Update rate limiting info
            updateRateLimitInfo();

            lastPollTime = System.currentTimeMillis();

        } catch (IOException e) {
            logger.error("Error polling devices: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error polling devices: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, e.getMessage());
        }
    }

    /**
     * Check webhook health and re-register if needed
     */
    private void checkWebhookHealth() {
        if (webhookService == null) {
            return;
        }

        try {
            boolean healthy = webhookService.checkWebhookHealth();
            if (!healthy) {
                logger.warn("Webhook health check failed, attempting to re-register");
                webhookService.registerWebhook();
            }
        } catch (Exception e) {
            logger.error("Error checking webhook health: {}", e.getMessage(), e);
        }
    }

    /**
     * Update rate limiting information
     */
    private void updateRateLimitInfo() {
        if (rachioHttp != null) {
            remainingRequests = rachioHttp.getRemainingRequests();
            limitRequests = rachioHttp.getLimitRequests();
            resetTime = rachioHttp.getResetTime();
            
            // Update channels
            refreshAllChannels();
            
            // Log if running low
            if (remainingRequests < 50) {
                logger.warn("Rate limit low: {}/{} remaining, resets in {} seconds", 
                    remainingRequests, limitRequests, 
                    Math.max(0, resetTime - System.currentTimeMillis() / 1000));
            }
        }
    }

    /**
     * Register a status listener
     */
    public void registerListener(RachioStatusListener listener) {
        listeners.add(listener);
        logger.debug("Registered listener: {}", listener.getClass().getSimpleName());
    }

    /**
     * Unregister a status listener
     */
    public void unregisterListener(RachioStatusListener listener) {
        listeners.remove(listener);
        logger.debug("Unregistered listener: {}", listener.getClass().getSimpleName());
    }

    /**
     * Notify listeners that device list was updated
     */
    private void notifyDeviceListUpdated(List<RachioDevice> deviceList) {
        synchronized (listeners) {
            for (RachioStatusListener listener : listeners) {
                try {
                    listener.onDeviceDataUpdated(deviceList.get(0)); // Notify with first device
                } catch (Exception e) {
                    logger.error("Error notifying listener: {}", e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Notify listeners of a webhook event
     */
    public void notifyWebhookEvent(RachioWebhookEvent event) {
        if (event == null) {
            return;
        }

        logger.debug("Notifying listeners of webhook event");
        
        synchronized (listeners) {
            for (RachioStatusListener listener : listeners) {
                try {
                    // Call appropriate method based on event type
                    listener.onThingStateChanged(null, null); // Pass appropriate device/zone
                } catch (Exception e) {
                    logger.error("Error notifying listener of webhook event: {}", e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Process a webhook event
     */
    public void processWebhookEvent(RachioWebhookEvent event) {
        if (event == null) {
            logger.warn("Received null webhook event");
            return;
        }

        logger.debug("Processing webhook event");
        
        try {
            // Update rate limiting info from event headers if available
            updateRateLimitInfo();
            
            // Notify listeners
            notifyWebhookEvent(event);
            
            // Update last poll time since we got fresh data
            lastPollTime = System.currentTimeMillis();
            
        } catch (Exception e) {
            logger.error("Error processing webhook event: {}", e.getMessage(), e);
        }
    }

    // RachioActions implementation
    
    @Override
    public void startZone(String deviceId, String zoneId, int duration) throws IOException {
        if (rachioHttp != null) {
            rachioHttp.startZone(deviceId, zoneId, duration);
        }
    }

    @Override
    public void stopWatering(String deviceId) throws IOException {
        if (rachioHttp != null) {
            rachioHttp.stopWatering(deviceId);
        }
    }

    @Override
    public void runAllZones(String deviceId, int duration) throws IOException {
        if (rachioHttp != null) {
            rachioHttp.runAllZones(deviceId, duration);
        }
    }

    @Override
    public void runNextZone(String deviceId) throws IOException {
        if (rachioHttp != null) {
            rachioHttp.runNextZone(deviceId);
        }
    }

    @Override
    public void setRainDelay(String deviceId, int hours) throws IOException {
        if (rachioHttp != null) {
            rachioHttp.setRainDelay(deviceId, hours);
        }
    }

    @Override
    public void setZoneEnabled(String deviceId, String zoneId, boolean enabled) throws IOException {
        if (rachioHttp != null) {
            rachioHttp.setZoneEnabled(deviceId, zoneId, enabled);
        }
    }

    @Override
    public void clearCache() {
        devices.clear();
        deviceZones.clear();
        person = null;
        logger.debug("Cache cleared");
    }

    // Getters
    
    public @Nullable RachioHttp getRachioHttp() {
        return rachioHttp;
    }

    public @Nullable RachioPerson getPerson() {
        return person;
    }

    public Map<String, RachioDevice> getDevices() {
        return devices;
    }

    public @Nullable List<RachioZone> getZones(String deviceId) {
        return deviceZones.get(deviceId);
    }

    public @Nullable RachioBridgeConfiguration getBridgeConfiguration() {
        return config;
    }

    public long getLastPollTime() {
        return lastPollTime;
    }

    public int getRemainingRequests() {
        return remainingRequests;
    }

    public int getLimitRequests() {
        return limitRequests;
    }

    public long getResetTime() {
        return resetTime;
    }

    // ThingHandlerService support
    
    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return List.of(RachioDiscoveryService.class);
    }

    // Helper methods for webhook and image services
    
    public @Nullable String getExternalUrl() {
        return config != null ? config.webhookExternalUrl : null;
    }

    public int getWebhookPort() {
        return config != null ? config.webhookPort : 8080;
    }

    public boolean isWebhookEnabled() {
        return config != null && config.webhookEnabled;
    }
}
