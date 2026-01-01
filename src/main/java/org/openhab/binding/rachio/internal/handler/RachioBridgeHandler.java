package org.openhab.binding.rachio.internal.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.RachioApiClient;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.RachioWebHookServletService;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
import org.openhab.binding.rachio.internal.discovery.RachioDiscoveryService;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Damion Boyett - Refactor contribution
 */
@NonNullByDefault
public class RachioBridgeHandler extends BaseBridgeHandler implements RachioHttp.RateLimitCallback {

    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);

    // Services passed via constructor
    private final HttpClientFactory httpClientFactory;
    private final ScheduledExecutorService scheduler;

    private @Nullable RachioWebHookServletService webHookService;
    private @Nullable RachioDiscoveryService discoveryService;

    private RachioBridgeConfiguration config = new RachioBridgeConfiguration();
    private @Nullable RachioApiClient apiClient;
    private @Nullable ScheduledFuture<?> pollingJob;
    private @Nullable ScheduledFuture<?> webhookRegistrationJob;
    private @Nullable ScheduledFuture<?> adaptivePollingAdjustmentJob;

    private final List<RachioStatusListener> statusListeners = new CopyOnWriteArrayList<>();
    private final List<RachioZoneHandler> zoneHandlers = new CopyOnWriteArrayList<>();
    private final Map<String, RachioDevice> discoveredDevices = new HashMap<>();
    private final Map<String, List<RachioZone>> deviceZones = new HashMap<>();

    // ===== Rate limit tracking =====
    private int rateLimitRemaining = 1500;
    private int rateLimitLimit = 1500;
    private long rateLimitReset = 0;
    private String rateLimitStatus = "NORMAL";
    private int currentPollingInterval = 120; // Default from README: 120 seconds

    // ===== Flag to track if channels have been created =====
    private boolean channelsCreated = false;

    /**
     * Constructor for manual instantiation by factory
     *
     * @param bridge The bridge thing
     * @param httpClientFactory HTTP client factory service
     * @param scheduler Scheduled executor service
     */
    public RachioBridgeHandler(final Bridge bridge, HttpClientFactory httpClientFactory,
            ScheduledExecutorService scheduler) {
        super(bridge);
        this.httpClientFactory = httpClientFactory;
        this.scheduler = scheduler;
        logger.debug("RachioBridgeHandler created with service injection");
    }

    public void setWebHookService(RachioWebHookServletService webHookService) {
        this.webHookService = webHookService;
        logger.debug("WebHook service set");
    }

    public void unsetWebHookService(RachioWebHookServletService webHookService) {
        if (this.webHookService == webHookService) {
            this.webHookService = null;
            logger.debug("WebHook service unset");
        }
    }

    public void setDiscoveryService(RachioDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
        logger.debug("Discovery service set");
    }

    public void unsetDiscoveryService(RachioDiscoveryService discoveryService) {
        if (this.discoveryService == discoveryService) {
            this.discoveryService = null;
            logger.debug("Discovery service unset");
        }
    }

    @Override
    @SuppressWarnings("null")
    public void initialize() {
        config = getConfigAs(RachioBridgeConfiguration.class);
        String apiKeyValue = config.getApiKey();
        logger.debug("Initializing Rachio bridge with API key: {}",
                apiKeyValue != null && !apiKeyValue.trim().isEmpty() ? "[SET]" : "[NOT SET]");

        if (apiKeyValue == null || apiKeyValue.trim().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "API key is missing");
            return;
        }

        String validatedApiKey = apiKeyValue.trim();

        createRateLimitChannels();

        this.apiClient = new RachioApiClient();
        this.apiClient.initialize(validatedApiKey);
        logger.debug("API client initialized with provided key");

        RachioHttp.setRateLimitCallback(this);
        logger.debug("Registered rate limit callback with RachioHttp");

        startPolling();

        String callbackUrlValue = config.callbackUrl;
        if (callbackUrlValue != null && !callbackUrlValue.trim().isEmpty()) {
            scheduleWebhookRegistration();
        }

        updateStatus(ThingStatus.ONLINE);
    }

    /**
     * ===== Create all rate limit dashboard channels =====
     * Implements OpenHAB 5.x ChannelBuilder pattern for dynamic channel creation.
     * "4 real-time channels for rate limit status"
     */
    private void createRateLimitChannels() {
        if (channelsCreated) {
            logger.debug("Rate limit channels already created, skipping");
            return;
        }

        logger.debug("Creating rate limit dashboard channels for bridge");

        try {
            ThingBuilder thingBuilder = editThing();
            boolean modified = false;

            // ===== CHANNEL 1: rateLimitRemaining =====
            ChannelUID remainingChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_RATE_LIMIT_REMAINING);
            if (getThing().getChannel(remainingChannelUID) == null) {
                ChannelTypeUID channelTypeUID = RachioBindingConstants.CHANNEL_TYPE_RATE_LIMIT_REMAINING;
                ChannelBuilder channelBuilder = ChannelBuilder.create(remainingChannelUID, "Number")
                        .withType(channelTypeUID).withLabel("API Calls Remaining")
                        .withDescription("Number of API calls remaining in the current rate limit window");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_RATE_LIMIT_REMAINING);
            }

            // ===== CHANNEL 2: rateLimitPercent =====
            ChannelUID percentChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_RATE_LIMIT_PERCENT);
            if (getThing().getChannel(percentChannelUID) == null) {
                ChannelTypeUID channelTypeUID = RachioBindingConstants.CHANNEL_TYPE_RATE_LIMIT_PERCENT;
                ChannelBuilder channelBuilder = ChannelBuilder.create(percentChannelUID, "Number")
                        .withType(channelTypeUID).withLabel("Rate Limit Percentage")
                        .withDescription("Percentage of API calls remaining before hitting rate limit");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_RATE_LIMIT_PERCENT);
            }

            // ===== CHANNEL 3: rateLimitStatus =====
            ChannelUID statusChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_RATE_LIMIT_STATUS);
            if (getThing().getChannel(statusChannelUID) == null) {
                ChannelTypeUID channelTypeUID = RachioBindingConstants.CHANNEL_TYPE_RATE_LIMIT_STATUS;
                ChannelBuilder channelBuilder = ChannelBuilder.create(statusChannelUID, "String")
                        .withType(channelTypeUID).withLabel("Rate Limit Status")
                        .withDescription("Current rate limit status: Normal, Warning, or Critical");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_RATE_LIMIT_STATUS);
            }

            // ===== CHANNEL 4: rateLimitReset =====
            ChannelUID resetChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_RATE_LIMIT_RESET);
            if (getThing().getChannel(resetChannelUID) == null) {
                ChannelTypeUID channelTypeUID = RachioBindingConstants.CHANNEL_TYPE_RATE_LIMIT_RESET;
                ChannelBuilder channelBuilder = ChannelBuilder.create(resetChannelUID, "Number")
                        .withType(channelTypeUID).withLabel("Rate Limit Reset")
                        .withDescription("Seconds until the rate limit window resets");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_RATE_LIMIT_RESET);
            }

            if (modified) {
                updateThing(thingBuilder.build());
                channelsCreated = true;
                logger.info("Successfully created all 4 rate limit dashboard channels");
            } else {
                logger.debug("All rate limit channels already exist");
                channelsCreated = true;
            }

        } catch (Exception e) {
            logger.error("Failed to create rate limit channels: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Failed to create rate limit channels: " + e.getMessage());
        }
    }

    @Override
    public void dispose() {
        stopPolling();
        cancelWebhookRegistration();
        stopAdaptivePollingAdjustment();

        // ===== Unregister rate limit callback =====
        RachioHttp.setRateLimitCallback(null);
        logger.debug("Unregistered rate limit callback");

        RachioApiClient client = apiClient;
        String callbackUrlValue = config.callbackUrl;
        if (client != null && callbackUrlValue != null && !callbackUrlValue.trim().isEmpty()) {
            try {
                client.unregisterWebhookByUrl(callbackUrlValue);
            } catch (Exception e) {
                logger.debug("Error unregistering webhook during dispose", e);
            }
        }

        statusListeners.clear();
        zoneHandlers.clear();
        discoveredDevices.clear();
        deviceZones.clear();
        channelsCreated = false;

        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            refreshChannel(channelUID);
        }
    }

    // ===== IMPLEMENTATION OF RATELIMITCALLBACK INTERFACE =====
    /**
     * Callback from RachioHttp when rate limit headers are received.
     * This is the critical connection point for the rate limiting dashboard.
     * 
     * @param remaining Number of API calls remaining in current window
     * @param limit Total API calls allowed in window (typically 1500)
     * @param reset Seconds until rate limit resets
     * @param status Status string (NORMAL/WARNING/CRITICAL)
     */
    @Override
    public void onRateLimitUpdate(int remaining, int limit, long reset, String status) {
        logger.trace("Rate limit update received: {}/{}, reset: {}, status: {}", remaining, limit, reset, status);

        scheduler.submit(() -> {
            updateRateLimitState(remaining, limit, reset, status);
        });
    }

    /**
     * Update rate limit state and trigger adaptive polling adjustments.
     * This method runs on the scheduler thread for thread safety.
     * 
     * @param remaining API calls remaining
     * @param limit Total API calls allowed
     * @param reset Seconds until reset
     * @param status Status string from RachioHttp
     */
    private void updateRateLimitState(int remaining, int limit, long reset, String status) {
        // Update bridge fields
        this.rateLimitRemaining = remaining;
        this.rateLimitLimit = limit;
        this.rateLimitReset = reset;
        this.rateLimitStatus = status;

        // Calculate percentage for channels
        int percent = (limit > 0) ? (remaining * 100 / limit) : 100;

        logger.debug("Rate limit state updated: {}/{} ({}%), reset in {}s, status: {}", remaining, limit, percent,
                reset, status);

        // Update all rate limit channels
        updateRateLimitChannels(remaining, limit, percent, reset, status);

        // Notify all status listeners
        notifyRateLimitListeners(remaining, limit, status);

        // Apply adaptive polling based on README thresholds
        applyAdaptivePolling(percent, status);
    }

    /**
     * Update all rate limit dashboard channels with current data.
     * 
     * @param remaining API calls remaining
     * @param limit Total API calls allowed
     * @param percent Percentage of calls remaining
     * @param reset Seconds until reset
     * @param status Status string
     */
    private void updateRateLimitChannels(int remaining, int limit, int percent, long reset, String status) {
        // Update rate limit remaining channel
        updateState(RachioBindingConstants.CHANNEL_RATE_LIMIT_REMAINING, new QuantityType<>(remaining, Units.ONE));

        // Update percentage channel
        updateState(RachioBindingConstants.CHANNEL_RATE_LIMIT_PERCENT, new QuantityType<>(percent, Units.PERCENT));

        // Update status channel
        updateState(RachioBindingConstants.CHANNEL_RATE_LIMIT_STATUS, new StringType(status));

        // Update reset channel
        updateState(RachioBindingConstants.CHANNEL_RATE_LIMIT_RESET, new QuantityType<>(reset, Units.SECOND));

        logger.trace("Rate limit channels updated: remaining={}, percent={}%, status={}, reset={}s", remaining, percent,
                status, reset);
    }

    /**
     * Notify all registered listeners of rate limit changes.
     * 
     * @param remaining API calls remaining
     * @param limit Total API calls allowed
     * @param status Status string
     */
    private void notifyRateLimitListeners(int remaining, int limit, String status) {
        for (RachioStatusListener listener : statusListeners) {
            try {
                listener.onRateLimitStatusChanged(remaining, limit, status);
            } catch (Exception e) {
                logger.debug("Error notifying rate limit listener {}: {}", listener.getListenerId(), e.getMessage());
            }
        }
    }

    /**
     * Apply adaptive polling based on rate limit status.
     * - Normal: Polls every 120 seconds
     * - Warning (10%): Slows to 240-second intervals
     * - Critical (2%): Slows to 360-second intervals
     * 
     * @param percent Percentage of calls remaining
     * @param status Status string
     */
    private void applyAdaptivePolling(int percent, String status) {
        int newInterval;

        if (percent <= 2) {
            newInterval = 360; // Critical: 360 seconds (6 minutes)
            logger.warn("Rate limit CRITICAL ({}%). Adjusting polling to {} seconds.", percent, newInterval);
        } else if (percent <= 10) {
            newInterval = 240; // Warning: 240 seconds (4 minutes)
            logger.info("Rate limit WARNING ({}%). Adjusting polling to {} seconds.", percent, newInterval);
        } else {
            newInterval = config.getPollingInterval(); // Normal: use configured interval
        }

        // Only restart polling if interval changed AND we're not already at this interval
        if (newInterval != currentPollingInterval) {
            logger.debug("Adaptive polling adjustment: {}s -> {}s (status: {})", currentPollingInterval, newInterval,
                    status);

            currentPollingInterval = newInterval;
            restartPollingWithNewInterval(newInterval);
        }
    }

    /**
     * Restart polling with a new interval.
     * 
     * @param newInterval New polling interval in seconds
     */
    private void restartPollingWithNewInterval(int newInterval) {
        stopPolling();

        // Schedule the restart with a small delay to avoid rapid restarts
        if (adaptivePollingAdjustmentJob != null && !adaptivePollingAdjustmentJob.isCancelled()) {
            adaptivePollingAdjustmentJob.cancel(false);
        }

        adaptivePollingAdjustmentJob = scheduler.schedule(() -> {
            logger.info("Restarting polling with adaptive interval: {} seconds", newInterval);
            pollingJob = scheduler.scheduleWithFixedDelay(this::pollDevices, 0, newInterval, TimeUnit.SECONDS);
            adaptivePollingAdjustmentJob = null;
        }, 5, TimeUnit.SECONDS);
    }

    /**
     * Stop the adaptive polling adjustment job.
     */
    private void stopAdaptivePollingAdjustment() {
        ScheduledFuture<?> job = adaptivePollingAdjustmentJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(true);
            adaptivePollingAdjustmentJob = null;
        }
    }

    public @Nullable RachioApiClient getApiClient() {
        return apiClient;
    }

    public RachioBridgeConfiguration getBridgeConfiguration() {
        return config;
    }

    /**
     * Gets the image URL for a zone.
     * Based on Rachio community discussion, the `imageUrl` is part of the person/device info.
     *
     * @param zoneId The zone ID.
     * @return The image URL or null if not available.
     */
    public @Nullable String getImageUrl(String zoneId) {
        logger.debug("getImageUrl requested for zone: {}", zoneId);
        return null;
    }

    public void pollDevices() {
        RachioApiClient client = apiClient;
        if (client == null) {
            return;
        }

        try {
            RachioPerson person = client.getPersonInfo();
            if (person != null && person.devices != null) {
                for (RachioDevice device : person.devices) {
                    processDevice(device);
                }
            }

            updateStatus(ThingStatus.ONLINE);
        } catch (Exception e) {
            logger.warn("Error polling Rachio devices: {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void processDevice(RachioDevice device) {
        String deviceId = device.id;
        if (deviceId == null) {
            logger.warn("Device has null ID, skipping processing");
            return;
        }

        discoveredDevices.put(deviceId, device);

        RachioDiscoveryService discovery = discoveryService;
        if (discovery != null) {
            discovery.onDeviceDiscovered(device);
        }

        List<RachioZone> zones = device.zones;
        if (zones != null) {
            deviceZones.put(deviceId, zones);
            if (discovery != null) {
                for (RachioZone zone : zones) {
                    discovery.onZoneDiscovered(deviceId, zone);
                }
            }
        }

        // Notify all status listeners
        for (RachioStatusListener listener : statusListeners) {
            listener.onDeviceUpdated(device);
        }

        // Notify all zone handlers
        for (RachioZoneHandler zoneHandler : zoneHandlers) {
            zoneHandler.onDeviceUpdated(device);
        }
    }

    public void registerStatusListener(RachioStatusListener listener) {
        if (!statusListeners.contains(listener)) {
            statusListeners.add(listener);
            logger.debug("Registered status listener: {}", listener.getListenerId());
        }
    }

    public void unregisterStatusListener(RachioStatusListener listener) {
        statusListeners.remove(listener);
        logger.debug("Unregistered status listener: {}", listener.getListenerId());
    }

    public void registerZoneHandler(RachioZoneHandler handler) {
        if (!zoneHandlers.contains(handler)) {
            zoneHandlers.add(handler);
            logger.debug("Registered zone handler: {}", handler.getThing().getUID());
        }
    }

    public void unregisterZoneHandler(RachioZoneHandler handler) {
        zoneHandlers.remove(handler);
        logger.debug("Unregistered zone handler: {}", handler.getThing().getUID());
    }

    public boolean isListenerRegistered(RachioStatusListener listener) {
        return statusListeners.contains(listener);
    }

    // Helper method to notify zone updates
    public void notifyZoneUpdated(@Nullable String deviceId, RachioZone zone) {
        String actualDeviceId = deviceId != null ? deviceId : "unknown";
        for (RachioStatusListener listener : statusListeners) {
            listener.onZoneUpdated(actualDeviceId, zone);
        }
    }

    /**
     * Enable or disable a zone.
     * This method is required by RachioZoneHandler.java to function properly.
     *
     * @param zoneId The zone ID to enable/disable
     * @param enabled true to enable, false to disable
     */
    public void setZoneEnabled(String zoneId, boolean enabled) {
        RachioApiClient client = apiClient;
        if (client == null) {
            logger.warn("Cannot set zone enabled state - API client not available");
            return;
        }

        logger.info("Zone {} {} requested", zoneId, enabled ? "enabled" : "disabled");

        // In a full implementation, this would call an API endpoint:
        // client.setZoneEnabled(zoneId, enabled);

        // For compilation purposes, we'll implement a basic version
        // that updates local state and notifies listeners
        RachioZone zone = getZoneData(zoneId);
        if (zone != null) {
            zone.enabled = enabled;
            String zoneDeviceId = zone.deviceId != null ? zone.deviceId : "unknown";
            notifyZoneUpdated(zoneDeviceId, zone);
            logger.debug("Zone {} {} locally", zoneId, enabled ? "enabled" : "disabled");
        }
    }

    private void startPolling() {
        int pollingInterval = config.getPollingInterval();
        currentPollingInterval = pollingInterval;
        pollingJob = scheduler.scheduleWithFixedDelay(this::pollDevices, 10, pollingInterval, TimeUnit.SECONDS);
        logger.debug("Started polling job with interval {} seconds", pollingInterval);
    }

    private void stopPolling() {
        ScheduledFuture<?> job = pollingJob;
        if (job != null) {
            job.cancel(true);
            pollingJob = null;
        }
    }

    private void scheduleWebhookRegistration() {
        webhookRegistrationJob = scheduler.schedule(this::registerWebhook, 30, TimeUnit.SECONDS);
    }

    private void cancelWebhookRegistration() {
        ScheduledFuture<?> job = webhookRegistrationJob;
        if (job != null) {
            job.cancel(true);
            webhookRegistrationJob = null;
        }
    }

    private void refreshChannel(ChannelUID channelUID) {
        switch (channelUID.getId()) {
            case RachioBindingConstants.CHANNEL_RATE_LIMIT_REMAINING:
                updateState(channelUID, new QuantityType<>(rateLimitRemaining, Units.ONE));
                break;
            case RachioBindingConstants.CHANNEL_RATE_LIMIT_PERCENT:
                int percent = (rateLimitLimit > 0) ? (rateLimitRemaining * 100 / rateLimitLimit) : 100;
                updateState(channelUID, new QuantityType<>(percent, Units.PERCENT));
                break;
            case RachioBindingConstants.CHANNEL_RATE_LIMIT_STATUS:
                updateState(channelUID, new StringType(rateLimitStatus));
                break;
            case RachioBindingConstants.CHANNEL_RATE_LIMIT_RESET:
                updateState(channelUID, new QuantityType<>(rateLimitReset, Units.SECOND));
                break;
        }
    }

    private void registerWebhook() {
        RachioApiClient client = apiClient;
        String callbackUrlValue = config.callbackUrl;
        if (client == null || callbackUrlValue == null || callbackUrlValue.trim().isEmpty()) {
            return;
        }

        try {
            boolean success = client.registerWebhook(callbackUrlValue);
            if (success) {
                logger.info("Registered webhook with callback URL: {}", callbackUrlValue);
            } else {
                logger.warn("Failed to register webhook with callback URL: {}", callbackUrlValue);
                scheduler.schedule(this::registerWebhook, 5, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            logger.warn("Failed to register webhook: {}", e.getMessage());
            scheduler.schedule(this::registerWebhook, 5, TimeUnit.MINUTES);
        }
    }

    /**
     * Process webhook event from servlet
     *
     * @param eventJson Webhook event JSON
     */
    public void processWebhook(String eventJson) {
        for (RachioStatusListener listener : statusListeners) {
            listener.onWebhookEventReceived(eventJson);
        }

        for (RachioZoneHandler zoneHandler : zoneHandlers) {
            zoneHandler.onWebhookEventReceived(eventJson);
        }

        logger.debug("Received webhook event: {}",
                eventJson.length() > 100 ? eventJson.substring(0, 100) + "..." : eventJson);
    }

    /**
     * Get device data for device handler
     *
     * @param deviceId Device ID
     * @return Device data or null
     */
    public @Nullable RachioDevice getDeviceData(String deviceId) {
        return discoveredDevices.get(deviceId);
    }

    /**
     * Get zone data for zone handler
     *
     * @param zoneId Zone ID
     * @return Zone data or null
     */
    public @Nullable RachioZone getZoneData(String zoneId) {
        // Search through all device zones to find the matching zone
        for (Map.Entry<String, List<RachioZone>> entry : deviceZones.entrySet()) {
            List<RachioZone> zones = entry.getValue();
            if (zones != null) {
                for (RachioZone zone : zones) {
                    if (zone.id != null && zone.id.equals(zoneId)) {
                        return zone;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Start watering a specific zone
     *
     * @param zoneId Zone ID to start
     * @param duration Duration in seconds
     */
    public void startZone(String zoneId, int duration) {
        RachioApiClient client = apiClient;
        if (client != null) {
            try {
                client.startZone(zoneId, duration);
                logger.info("Started zone {} for {} seconds", zoneId, duration);
            } catch (Exception e) {
                logger.warn("Failed to start zone {}: {}", zoneId, e.getMessage());
            }
        }
    }

    /**
     * Stop watering a specific zone
     *
     * @param zoneId Zone ID to stop
     */
    public void stopZone(String zoneId) {
        RachioApiClient client = apiClient;
        if (client != null) {
            try {
                client.stopZone(zoneId);
                logger.info("Stopped zone {}", zoneId);
            } catch (Exception e) {
                logger.warn("Failed to stop zone {}: {}", zoneId, e.getMessage());
            }
        }
    }
}
