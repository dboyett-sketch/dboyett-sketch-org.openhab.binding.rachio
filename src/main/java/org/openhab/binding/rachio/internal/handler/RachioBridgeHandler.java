package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.RachioSecurity;
import org.openhab.binding.rachio.internal.api.RachioWebHookServletService;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioWebHookEvent;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
import org.openhab.core.common.NamedThreadFactory;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);

    private final HttpClientFactory httpClientFactory;
    private final RachioSecurity security;
    private final RachioWebHookServletService webhookService;
    private final List<RachioStatusListener> statusListeners = new CopyOnWriteArrayList<>();

    private @Nullable RachioHttp http;
    private RachioBridgeConfiguration config = new RachioBridgeConfiguration();
    private @Nullable ScheduledFuture<?> pollingJob;
    private @Nullable ScheduledFuture<?> webhookCheckJob;
    private @Nullable Future<?> initializationJob;

    private @Nullable RachioPerson personInfo;
    private final Map<String, RachioDevice> devices = new HashMap<>();
    private final Map<String, Thing> deviceThings = new HashMap<>();
    private final Map<String, Thing> zoneThings = new HashMap<>();

    public RachioBridgeHandler(Bridge bridge, HttpClientFactory httpClientFactory, RachioSecurity security,
            RachioWebHookServletService webhookService) {
        super(bridge);
        this.httpClientFactory = httpClientFactory;
        this.security = security;
        this.webhookService = webhookService;
    }

    @Override
    public void initialize() {
        config = getConfigAs(RachioBridgeConfiguration.class);

        if (config.apiKey.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "API key is required");
            return;
        }

        updateStatus(ThingStatus.UNKNOWN);

        // Initialize HTTP client
        http = new RachioHttp(config.apiKey, httpClientFactory);

        // Start initialization in background
        initializationJob = scheduler.submit(this::initializeBridge);
    }

    private void initializeBridge() {
        try {
            // Test API connection
            personInfo = http.getPersonInfo();
            if (personInfo == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Failed to get person info");
                return;
            }

            updateStatus(ThingStatus.ONLINE);

            // Load devices
            refreshDevices();

            // Start polling
            if (config.refreshInterval > 0) {
                startPolling();
            }

            // Setup webhooks if enabled
            if (config.webhookEnabled) {
                setupWebhooks();
            }

            // Update bridge properties
            updateBridgeProperties();

        } catch (Exception e) {
            logger.error("Failed to initialize bridge: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void updateBridgeProperties() {
        Map<String, String> properties = new HashMap<>();
        
        if (personInfo != null) {
            properties.put(PROPERTY_PERSON_ID, personInfo.getId());
            properties.put(PROPERTY_PERSON_NAME, personInfo.getFirstName() + " " + personInfo.getLastName());
            properties.put(PROPERTY_PERSON_EMAIL, personInfo.getEmail());
            
            String username = personInfo.getUsername();
            if (username != null) {
                properties.put(PROPERTY_PERSON_USERNAME, username);
            }
        }
        
        updateProperties(properties);
    }

    private void refreshDevices() {
        if (personInfo == null || http == null) {
            return;
        }

        try {
            List<RachioDevice> deviceList = personInfo.getDevices();
            devices.clear();

            for (RachioDevice device : deviceList) {
                String deviceId = device.getId();
                devices.put(deviceId, device);

                // Update existing device thing or create discovery
                Thing existingDevice = deviceThings.get(deviceId);
                if (existingDevice != null && existingDevice.getHandler() instanceof RachioDeviceHandler) {
                    ((RachioDeviceHandler) existingDevice.getHandler()).refresh();
                }

                // Refresh zones for this device
                refreshZones(device);
            }

            // Notify status listeners
            for (RachioStatusListener listener : statusListeners) {
                listener.deviceListUpdated(devices.values());
            }

        } catch (Exception e) {
            logger.error("Failed to refresh devices: {}", e.getMessage(), e);
        }
    }

    private void refreshZones(RachioDevice device) {
        if (http == null) {
            return;
        }

        try {
            List<RachioZone> zoneList = device.getZones();
            for (RachioZone zone : zoneList) {
                String zoneId = zone.getId();
                
                // Update existing zone thing
                Thing existingZone = zoneThings.get(zoneId);
                if (existingZone != null && existingZone.getHandler() instanceof RachioZoneHandler) {
                    ((RachioZoneHandler) existingZone.getHandler()).refresh();
                }
            }

        } catch (Exception e) {
            logger.error("Failed to refresh zones for device {}: {}", device.getId(), e.getMessage(), e);
        }
    }

    private void startPolling() {
        stopPolling();

        pollingJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                refreshDevices();
            } catch (Exception e) {
                logger.debug("Error during polling: {}", e.getMessage(), e);
            }
        }, 0, config.refreshInterval, TimeUnit.SECONDS);

        logger.debug("Started polling with interval {} seconds", config.refreshInterval);
    }

    private void stopPolling() {
        ScheduledFuture<?> job = pollingJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(true);
            pollingJob = null;
        }
    }

    private void setupWebhooks() {
        if (config.webhookEnabled && http != null) {
            try {
                String externalId = getThing().getUID().getId();
                String callbackUrl = config.webhookUrl;
                
                if (callbackUrl.isEmpty()) {
                    logger.warn("Webhook URL not configured, webhooks disabled");
                    return;
                }

                webhookService.registerWebhook(http, externalId, callbackUrl, config.webhookSecret);

                // Start periodic webhook health check
                if (config.webhookCheckInterval > 0) {
                    startWebhookHealthCheck();
                }

            } catch (Exception e) {
                logger.error("Failed to setup webhooks: {}", e.getMessage(), e);
            }
        }
    }

    private void startWebhookHealthCheck() {
        stopWebhookHealthCheck();

        webhookCheckJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                if (http != null) {
                    webhookService.checkWebhookHealth(http);
                }
            } catch (Exception e) {
                logger.debug("Error during webhook health check: {}", e.getMessage(), e);
            }
        }, config.webhookCheckInterval, config.webhookCheckInterval, TimeUnit.MINUTES);
    }

    private void stopWebhookHealthCheck() {
        ScheduledFuture<?> job = webhookCheckJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(true);
            webhookCheckJob = null;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            refreshChannel(channelUID);
        }
    }

    private void refreshChannel(ChannelUID channelUID) {
        String channelId = channelUID.getIdWithoutGroup();
        
        switch (channelId) {
            case CHANNEL_RATE_LIMIT_REMAINING:
                if (http != null) {
                    updateState(channelUID.getId(), new org.openhab.core.library.types.DecimalType(http.getRemainingCalls()));
                }
                break;
            case CHANNEL_RATE_LIMIT_PERCENT:
                if (http != null) {
                    int remaining = http.getRemainingCalls();
                    int limit = http.getCurrentRateLimit();
                    if (limit > 0) {
                        double percent = (remaining * 100.0) / limit;
                        updateState(channelUID.getId(), new org.openhab.core.library.types.DecimalType(percent));
                    }
                }
                break;
            case CHANNEL_RATE_LIMIT_STATUS:
                if (http != null) {
                    int remaining = http.getRemainingCalls();
                    String status = remaining > 1000 ? "OK" : remaining > 100 ? "WARNING" : "CRITICAL";
                    updateState(channelUID.getId(), new org.openhab.core.library.types.StringType(status));
                }
                break;
            case CHANNEL_RATE_LIMIT_RESET:
                if (http != null) {
                    Instant resetTime = http.getResetTime();
                    updateState(channelUID.getId(), new org.openhab.core.library.types.DateTimeType(resetTime));
                }
                break;
        }
    }

    /**
     * Handle incoming webhook event
     */
    public void handleWebhookEvent(RachioWebHookEvent event) {
        if (event == null) {
            return;
        }

        String eventType = event.getType();
        String deviceId = event.getDeviceId();
        String subType = event.getSubType();

        logger.debug("Processing webhook event: type={}, deviceId={}, subType={}", eventType, deviceId, subType);

        // Update device status if applicable
        Thing deviceThing = deviceThings.get(deviceId);
        if (deviceThing != null && deviceThing.getHandler() instanceof RachioDeviceHandler) {
            ((RachioDeviceHandler) deviceThing.getHandler()).handleWebhookEvent(event);
        }

        // Update zone status if applicable
        String zoneId = event.getZoneId();
        if (zoneId != null) {
            Thing zoneThing = zoneThings.get(zoneId);
            if (zoneThing != null && zoneThing.getHandler() instanceof RachioZoneHandler) {
                ((RachioZoneHandler) zoneThing.getHandler()).handleWebhookEvent(event);
            }
        }

        // Notify status listeners
        for (RachioStatusListener listener : statusListeners) {
            listener.webhookEventReceived(event);
        }
    }

    /**
     * Check if an IP address is allowed based on configuration
     */
    public boolean isIpAllowed(String ipAddress) {
        return security.isIpAllowed(ipAddress, config.allowedIpRanges);
    }

    /**
     * Get webhook secret
     */
    public String getWebhookSecret() {
        return config.webhookSecret;
    }

    /**
     * Check if webhook is enabled
     */
    public boolean isWebhookEnabled() {
        return config.webhookEnabled;
    }

    /**
     * Get HTTP client
     */
    public @Nullable RachioHttp getHttp() {
        return http;
    }

    /**
     * Get refresh interval
     */
    public int getRefreshInterval() {
        return config.refreshInterval;
    }

    /**
     * Check if weather is enabled
     */
    public boolean isWeatherEnabled() {
        return config.weatherEnabled;
    }

    /**
     * Register a device thing
     */
    public void registerDeviceThing(String deviceId, Thing thing) {
        deviceThings.put(deviceId, thing);
    }

    /**
     * Register a zone thing
     */
    public void registerZoneThing(String zoneId, Thing thing) {
        zoneThings.put(zoneId, thing);
    }

    /**
     * Unregister a device thing
     */
    public void unregisterDeviceThing(String deviceId) {
        deviceThings.remove(deviceId);
    }

    /**
     * Unregister a zone thing
     */
    public void unregisterZoneThing(String zoneId) {
        zoneThings.remove(zoneId);
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

    @Override
    public void dispose() {
        stopPolling();
        stopWebhookHealthCheck();

        Future<?> initJob = initializationJob;
        if (initJob != null && !initJob.isCancelled()) {
            initJob.cancel(true);
        }

        RachioHttp localHttp = http;
        if (localHttp != null) {
            localHttp.shutdown();
        }

        webhookService.unregisterWebhook(getThing().getUID().getId());

        statusListeners.clear();
        devices.clear();
        deviceThings.clear();
        zoneThings.clear();

        super.dispose();
    }

    /**
     * Get scheduler for scheduling tasks
     */
    public org.openhab.core.scheduler.ScheduledExecutorService getScheduler() {
        return scheduler;
    }
}
