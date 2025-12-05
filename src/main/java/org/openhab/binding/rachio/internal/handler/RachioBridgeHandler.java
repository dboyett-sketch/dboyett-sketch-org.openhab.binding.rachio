package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioBridgeHandler} is responsible for handling commands and status
 * updates for the Rachio Bridge.
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);
    private final List<RachioStatusListener> listeners = new CopyOnWriteArrayList<>();
    
    private @Nullable RachioBridgeConfiguration config;
    private @Nullable RachioHttp httpHandler;
    private @Nullable ScheduledFuture<?> pollingJob;
    private @Nullable ScheduledFuture<?> webhookCheckJob;
    
    private @Nullable RachioPerson personData;
    private final Map<String, RachioDevice> deviceCache = new HashMap<>();
    private final Map<String, RachioZone> zoneCache = new HashMap<>();
    
    private int pollInterval = 300; // 5 minutes default
    private ZonedDateTime lastUpdated;
    private boolean webhookRegistered = false;
    private @Nullable String webhookId;
    
    private final HttpClientFactory httpClientFactory;

    public RachioBridgeHandler(Bridge bridge, HttpClientFactory httpClientFactory) {
        super(bridge);
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            refreshBridgeData();
            return;
        }

        try {
            RachioHttp http = httpHandler;
            RachioBridgeConfiguration config = this.config;
            
            if (http == null || config == null) {
                logger.warn("Bridge handler not properly initialized");
                return;
            }

            String channelId = channelUID.getIdWithoutGroup();
            
            switch (channelId) {
                case CHANNEL_RUN_ALL_ZONES:
                    if (command instanceof DecimalType || command instanceof QuantityType) {
                        int duration = ((DecimalType) command).intValue();
                        if (duration > 0 && config.deviceId != null) {
                            http.runAllZones(getThing().getUID().getId(), duration, config.deviceId);
                            updateState(CHANNEL_STATUS, new StringType("Running all zones"));
                            updateState(CHANNEL_LAST_COMMAND, new StringType("runAllZones: " + duration + "s"));
                            updateState(CHANNEL_LAST_COMMAND_TIME, new DateTimeType(ZonedDateTime.now()));
                            logger.info("Started running all zones for {} seconds", duration);
                        }
                    }
                    break;
                    
                case CHANNEL_RAIN_DELAY:
                    if (command instanceof DecimalType || command instanceof QuantityType) {
                        int hours = ((DecimalType) command).intValue();
                        if (hours >= 0 && hours <= 168 && config.deviceId != null) {
                            http.rainDelay(getThing().getUID().getId(), hours, config.deviceId);
                            updateState(CHANNEL_STATUS, new StringType("Rain delay: " + hours + "h"));
                            updateState(CHANNEL_LAST_COMMAND, new StringType("rainDelay: " + hours + "h"));
                            updateState(CHANNEL_LAST_COMMAND_TIME, new DateTimeType(ZonedDateTime.now()));
                            logger.info("Set rain delay to {} hours", hours);
                        }
                    }
                    break;
                    
                case CHANNEL_RUN_NEXT_ZONE:
                    if (command instanceof DecimalType || command instanceof QuantityType) {
                        int duration = ((DecimalType) command).intValue();
                        if (duration > 0 && config.deviceId != null) {
                            http.runNextZone(getThing().getUID().getId(), duration, config.deviceId);
                            updateState(CHANNEL_STATUS, new StringType("Running next zone"));
                            updateState(CHANNEL_LAST_COMMAND, new StringType("runNextZone: " + duration + "s"));
                            updateState(CHANNEL_LAST_COMMAND_TIME, new DateTimeType(ZonedDateTime.now()));
                            logger.info("Started running next zone for {} seconds", duration);
                        }
                    }
                    break;
                    
                case CHANNEL_STOP_WATERING:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        if (config.deviceId != null) {
                            http.stopWatering(config.deviceId);
                            updateState(CHANNEL_STATUS, new StringType("Stopped watering"));
                            updateState(CHANNEL_LAST_COMMAND, new StringType("stopWatering"));
                            updateState(CHANNEL_LAST_COMMAND_TIME, new DateTimeType(ZonedDateTime.now()));
                            updateState(CHANNEL_STOP_WATERING, OnOffType.OFF); // Reset switch
                            logger.info("Stopped all watering");
                        }
                    }
                    break;
                    
                case CHANNEL_WEBHOOK_STATUS:
                    if (command instanceof OnOffType) {
                        boolean enable = command == OnOffType.ON;
                        if (enable) {
                            registerWebhook();
                        } else {
                            unregisterWebhook();
                        }
                    }
                    break;
            }
        } catch (RachioApiException e) {
            logger.error("Failed to execute command {}: {}", command, e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio bridge handler");
        
        config = getConfigAs(RachioBridgeConfiguration.class);
        updateStatus(ThingStatus.UNKNOWN);

        // Validate configuration
        if (config.apiKey == null || config.apiKey.trim().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "API key is required");
            return;
        }

        // Initialize HTTP handler
        try {
            httpHandler = new RachioHttp(httpClientFactory, config.apiKey);
        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, 
                    "Failed to initialize HTTP client: " + e.getMessage());
            return;
        }

        // Create dynamic channels
        createBridgeChannels();

        // Start polling
        startPolling();

        // Initial refresh
        refreshBridgeData();

        // Register webhook if configured
        if (config.webhookEnabled) {
            scheduler.schedule(this::registerWebhook, 10, TimeUnit.SECONDS);
        }
    }

    private void createBridgeChannels() {
        logger.debug("Creating bridge channels");
        
        List<Channel> channels = new ArrayList<>();
        ChannelUID thingUID = getThing().getUID();

        // Status Channels
        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_STATUS), "String")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_STATUS))
                .withLabel("Bridge Status")
                .withDescription("Current status of the bridge")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_LAST_UPDATED), "DateTime")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_LAST_UPDATED))
                .withLabel("Last Updated")
                .withDescription("When bridge data was last updated")
                .build());

        // Command Channels
        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_RUN_ALL_ZONES), "Number")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_RUN_ALL_ZONES))
                .withLabel("Run All Zones")
                .withDescription("Run all zones for specified duration in seconds")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_RUN_NEXT_ZONE), "Number")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_RUN_NEXT_ZONE))
                .withLabel("Run Next Zone")
                .withDescription("Run next available zone for specified duration in seconds")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_RAIN_DELAY), "Number")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_RAIN_DELAY))
                .withLabel("Rain Delay")
                .withDescription("Set rain delay in hours (0-168)")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_STOP_WATERING), "Switch")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_STOP_WATERING))
                .withLabel("Stop Watering")
                .withDescription("Stop all watering immediately")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_LAST_COMMAND), "String")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_LAST_COMMAND))
                .withLabel("Last Command")
                .withDescription("Last command executed")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_LAST_COMMAND_TIME), "DateTime")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_LAST_COMMAND_TIME))
                .withLabel("Last Command Time")
                .withDescription("When last command was executed")
                .build());

        // Rate Limiting Monitoring Channels
        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_RATE_LIMIT_REMAINING), "Number")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_RATE_LIMIT_REMAINING))
                .withLabel("API Calls Remaining")
                .withDescription("Remaining API calls before rate limit")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_RATE_LIMIT_USAGE), "Number:Dimensionless")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_RATE_LIMIT_USAGE))
                .withLabel("API Usage Percentage")
                .withDescription("Percentage of API limit used")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_RATE_LIMIT_STATUS), "String")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_RATE_LIMIT_STATUS))
                .withLabel("Rate Limit Status")
                .withDescription("Current rate limit status")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_RATE_LIMIT_RESET), "DateTime")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_RATE_LIMIT_RESET))
                .withLabel("Rate Limit Reset")
                .withDescription("When rate limit resets")
                .build());

        // Webhook Status Channels
        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_WEBHOOK_STATUS), "Switch")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_WEBHOOK_STATUS))
                .withLabel("Webhook Status")
                .withDescription("Enable/disable webhook registration")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_WEBHOOK_LAST_EVENT), "DateTime")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_WEBHOOK_LAST_EVENT))
                .withLabel("Last Webhook Event")
                .withDescription("When last webhook event was received")
                .build());

        // Update the thing with all channels
        updateThing(editThing().withChannels(channels).build());
        
        logger.debug("Created {} bridge channels", channels.size());
    }

    private void startPolling() {
        ScheduledFuture<?> pollingJob = this.pollingJob;
        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
        }

        this.pollingJob = scheduler.scheduleWithFixedDelay(this::refreshBridgeData, 10, pollInterval, TimeUnit.SECONDS);
        logger.debug("Started bridge polling every {} seconds", pollInterval);
        
        // Start webhook health check
        this.webhookCheckJob = scheduler.scheduleWithFixedDelay(this::checkWebhookHealth, 60, 300, TimeUnit.SECONDS);
    }

    private void refreshBridgeData() {
        try {
            RachioHttp http = httpHandler;
            RachioBridgeConfiguration config = this.config;
            
            if (http == null || config == null) {
                logger.debug("Cannot refresh bridge data - handler not initialized");
                return;
            }

            // Check if rate limited
            if (http.isRateLimited(config.deviceId)) {
                logger.warn("API rate limited - skipping refresh");
                updateState(CHANNEL_RATE_LIMIT_STATUS, new StringType("RATE_LIMITED"));
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "API rate limited");
                return;
            }

            // Get person info
            RachioPerson person = http.getPerson();
            if (person != null) {
                this.personData = person;
                logger.debug("Retrieved person info for: {}", person.fullName);
            }

            // Get devices if deviceId is not configured
            if (config.deviceId == null || config.deviceId.isEmpty()) {
                if (person != null && person.devices != null && !person.devices.isEmpty()) {
                    // Use first device
                    config.deviceId = person.devices.get(0).id;
                    logger.info("Auto-selected device ID: {}", config.deviceId);
                }
            }

            // Get device details if we have a device ID
            if (config.deviceId != null && !config.deviceId.isEmpty()) {
                RachioDevice device = http.getDevice(config.deviceId);
                if (device != null) {
                    deviceCache.put(device.id, device);
                    
                    // Cache zones
                    if (device.zones != null) {
                        for (RachioZone zone : device.zones) {
                            zoneCache.put(zone.id, zone);
                        }
                    }
                    
                    logger.debug("Retrieved device: {} with {} zones", device.name, 
                            device.zones != null ? device.zones.size() : 0);
                }
            }

            // Update rate limiting info
            updateRateLimitInfo(http, config.deviceId);

            lastUpdated = ZonedDateTime.now();
            updateState(CHANNEL_LAST_UPDATED, new DateTimeType(lastUpdated));
            updateState(CHANNEL_STATUS, new StringType("ONLINE"));
            updateStatus(ThingStatus.ONLINE);

            // Notify listeners
            notifyDeviceStatusChanged(config.deviceId, true);

        } catch (RachioApiException e) {
            logger.error("Failed to refresh bridge data: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            updateState(CHANNEL_STATUS, new StringType("ERROR: " + e.getMessage()));
        }
    }

    private void updateRateLimitInfo(RachioHttp http, @Nullable String deviceId) {
        Map<String, Object> rateInfo = http.getRateLimitInfo(deviceId);
        
        int remaining = (int) rateInfo.getOrDefault("remaining", 60);
        int limit = (int) rateInfo.getOrDefault("limit", 60);
        double usagePercentage = (double) rateInfo.getOrDefault("usagePercentage", 0.0);
        boolean isLimited = (boolean) rateInfo.getOrDefault("isLimited", false);
        String resetTime = (String) rateInfo.getOrDefault("resetTime", "");
        
        updateState(CHANNEL_RATE_LIMIT_REMAINING, new DecimalType(remaining));
        updateState(CHANNEL_RATE_LIMIT_USAGE, new QuantityType<>(usagePercentage, Units.PERCENT));
        updateState(CHANNEL_RATE_LIMIT_STATUS, new StringType(isLimited ? "LIMITED" : "NORMAL"));
        
        try {
            if (!resetTime.isEmpty()) {
                ZonedDateTime resetDateTime = ZonedDateTime.parse(resetTime);
                updateState(CHANNEL_RATE_LIMIT_RESET, new DateTimeType(resetDateTime));
            }
        } catch (Exception e) {
            logger.debug("Could not parse rate limit reset time: {}", resetTime);
        }
        
        // Adjust polling based on rate limit
        if (isLimited || remaining < 10) {
            // Slow down polling when rate limited
            if (pollInterval < 600) { // Don't go below 10 minutes
                pollInterval = 600;
                startPolling(); // Restart with new interval
                logger.warn("Rate limited detected - increased polling interval to {} seconds", pollInterval);
            }
        } else if (remaining > 30 && pollInterval > 300) {
            // Speed up polling when we have plenty of calls
            pollInterval = 300;
            startPolling(); // Restart with new interval
            logger.debug("Good rate limit - decreased polling interval to {} seconds", pollInterval);
        }
    }

    private void registerWebhook() {
        try {
            RachioHttp http = httpHandler;
            RachioBridgeConfiguration config = this.config;
            
            if (http == null || config == null || config.deviceId == null || config.deviceId.isEmpty()) {
                logger.warn("Cannot register webhook - missing configuration");
                return;
            }

            // Get external URL from configuration or generate one
            String webhookUrl = config.webhookUrl;
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                logger.warn("Webhook URL not configured - cannot register webhook");
                return;
            }

            String externalId = "openhab-rachio-" + config.deviceId;
            
            // Check if webhook already exists
            List<org.openhab.binding.rachio.internal.api.RachioApiWebHookEntry> existingWebhooks = http.listWebhooks(config.deviceId);
            if (existingWebhooks != null) {
                for (org.openhab.binding.rachio.internal.api.RachioApiWebHookEntry webhook : existingWebhooks) {
                    if (webhook.externalId.equals(externalId)) {
                        this.webhookId = webhook.id;
                        webhookRegistered = true;
                        logger.info("Webhook already registered: {}", webhookId);
                        updateState(CHANNEL_WEBHOOK_STATUS, OnOffType.ON);
                        return;
                    }
                }
            }

            // Register new webhook
            http.registerWebhook(config.deviceId, webhookUrl, externalId);
            webhookRegistered = true;
            updateState(CHANNEL_WEBHOOK_STATUS, OnOffType.ON);
            logger.info("Registered webhook for device {}", config.deviceId);

        } catch (RachioApiException e) {
            logger.error("Failed to register webhook: {}", e.getMessage(), e);
            webhookRegistered = false;
            updateState(CHANNEL_WEBHOOK_STATUS, OnOffType.OFF);
        }
    }

    private void unregisterWebhook() {
        try {
            if (webhookId != null) {
                RachioHttp http = httpHandler;
                if (http != null) {
                    http.deleteWebhook(webhookId);
                    logger.info("Unregistered webhook: {}", webhookId);
                }
            }
            webhookRegistered = false;
            webhookId = null;
            updateState(CHANNEL_WEBHOOK_STATUS, OnOffType.OFF);
            
        } catch (RachioApiException e) {
            logger.error("Failed to unregister webhook: {}", e.getMessage(), e);
        }
    }

    private void checkWebhookHealth() {
        if (!webhookRegistered && config != null && config.webhookEnabled) {
            logger.info("Webhook not registered but enabled - attempting to register");
            registerWebhook();
        }
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> pollingJob = this.pollingJob;
        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
        }
        this.pollingJob = null;
        
        ScheduledFuture<?> webhookCheckJob = this.webhookCheckJob;
        if (webhookCheckJob != null && !webhookCheckJob.isCancelled()) {
            webhookCheckJob.cancel(true);
        }
        this.webhookCheckJob = null;

        // Unregister webhook on disposal
        if (config != null && config.webhookEnabled) {
            unregisterWebhook();
        }

        // Clear caches
        deviceCache.clear();
        zoneCache.clear();
        listeners.clear();

        super.dispose();
        logger.debug("Disposed bridge handler");
    }

    // Listener management
    public void registerListener(RachioStatusListener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(RachioStatusListener listener) {
        listeners.remove(listener);
    }

    private void notifyDeviceStatusChanged(String deviceId, boolean online) {
        for (RachioStatusListener listener : listeners) {
            listener.deviceStatusChanged(deviceId, online);
        }
    }

    public void notifyZoneStatusChanged(RachioZone zone) {
        for (RachioStatusListener listener : listeners) {
            listener.zoneStatusChanged(zone);
        }
    }

    public void notifyWebhookEventReceived(String deviceId, String eventType, String eventData) {
        updateState(CHANNEL_WEBHOOK_LAST_EVENT, new DateTimeType(ZonedDateTime.now()));
        
        for (RachioStatusListener listener : listeners) {
            listener.webhookEventReceived(deviceId, eventType, eventData);
        }
    }

    // Getters for other handlers
    public @Nullable RachioHttp getHttpHandler() {
        return httpHandler;
    }

    public @Nullable RachioDevice getDeviceData(String deviceId) {
        return deviceCache.get(deviceId);
    }

    public @Nullable RachioZone getZoneData(String zoneId) {
        return zoneCache.get(zoneId);
    }

    public @Nullable RachioBridgeConfiguration getBridgeConfiguration() {
        return config;
    }

    public boolean isWebhookRegistered() {
        return webhookRegistered;
    }

    public void setPollInterval(int interval) {
        this.pollInterval = interval;
        startPolling();
    }
}
