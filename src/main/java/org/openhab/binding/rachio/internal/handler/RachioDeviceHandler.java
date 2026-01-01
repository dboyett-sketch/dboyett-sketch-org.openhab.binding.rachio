package org.openhab.binding.rachio.internal.handler;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.RachioApiClient;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioForecast;
import org.openhab.binding.rachio.internal.api.dto.RachioSavings;
import org.openhab.binding.rachio.internal.api.dto.RachioUsage;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioDeviceConfiguration;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.MetricPrefix;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class RachioDeviceHandler extends RachioHandler {

    private final Logger logger = LoggerFactory.getLogger(RachioDeviceHandler.class);

    private @Nullable RachioDeviceConfiguration config;
    private @Nullable RachioDevice lastDeviceData;
    private @Nullable RachioUsage lastUsageData;
    private @Nullable RachioForecast lastForecastData;

    private boolean channelsCreated = false;

    /**
     * Constructor for manual instantiation by factory
     *
     * @param thing The device thing
     * @param scheduler Scheduled executor service (injected by factory)
     */
    public RachioDeviceHandler(Thing thing, ScheduledExecutorService scheduler) {
        super(thing);
        // Scheduler is already available via inheritance from BaseThingHandler
        logger.debug("RachioDeviceHandler created with scheduler injection");
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio device handler for thing {}", getThing().getUID());

        config = getConfigAs(RachioDeviceConfiguration.class);
        updateStatus(ThingStatus.UNKNOWN);

        createDeviceChannels();

        RachioBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null) {
            bridgeHandler.registerStatusListener(this);
            logger.debug("Registered with bridge handler");
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge not available");
            return;
        }

        scheduler.schedule(this::refreshDeviceData, 2, TimeUnit.SECONDS);
        logger.debug("Rachio device handler initialized for thing {}", getThing().getUID());
    }

    private void createDeviceChannels() {
        if (channelsCreated) {
            logger.debug("Device channels already created, skipping");
            return;
        }

        logger.debug("Creating device channels for thing {}", getThing().getUID());

        try {
            ThingBuilder thingBuilder = editThing();
            boolean modified = false;

            // ===== DEVICE STATUS CHANNELS =====

            // 1. Device Status (String)
            ChannelUID statusChannelUID = new ChannelUID(getThing().getUID(), RachioBindingConstants.CHANNEL_STATUS);
            if (getThing().getChannel(statusChannelUID) == null) {
                ChannelTypeUID channelTypeUID = RachioBindingConstants.CHANNEL_TYPE_DEVICE_STATUS;
                ChannelBuilder channelBuilder = ChannelBuilder.create(statusChannelUID, "String")
                        .withType(channelTypeUID).withLabel("Device Status")
                        .withDescription("Current status of the Rachio controller (ONLINE/OFFLINE/SLEEP)");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_STATUS);
            }

            // 2. Device Paused (Switch)
            ChannelUID pausedChannelUID = new ChannelUID(getThing().getUID(), RachioBindingConstants.CHANNEL_PAUSED);
            if (getThing().getChannel(pausedChannelUID) == null) {
                ChannelTypeUID channelTypeUID = RachioBindingConstants.CHANNEL_TYPE_DEVICE_PAUSED;
                ChannelBuilder channelBuilder = ChannelBuilder.create(pausedChannelUID, "Switch")
                        .withType(channelTypeUID).withLabel("Device Paused")
                        .withDescription("Manual pause status of the Rachio controller");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_PAUSED);
            }

            // 3. Device Enabled (Switch)
            ChannelUID enabledChannelUID = new ChannelUID(getThing().getUID(), RachioBindingConstants.CHANNEL_ENABLED);
            if (getThing().getChannel(enabledChannelUID) == null) {
                ChannelBuilder channelBuilder = ChannelBuilder.create(enabledChannelUID, "Switch")
                        .withLabel("Device Enabled").withDescription("Whether the device is enabled for watering");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ENABLED);
            }

            // 4. Device Deleted (Switch)
            ChannelUID deletedChannelUID = new ChannelUID(getThing().getUID(), RachioBindingConstants.CHANNEL_DELETED);
            if (getThing().getChannel(deletedChannelUID) == null) {
                ChannelBuilder channelBuilder = ChannelBuilder.create(deletedChannelUID, "Switch")
                        .withLabel("Device Deleted")
                        .withDescription("Whether the device has been deleted in the Rachio system");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_DELETED);
            }

            // 5. Zones Count (Number)
            ChannelUID zonesCountChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONES_COUNT);
            if (getThing().getChannel(zonesCountChannelUID) == null) {
                ChannelBuilder channelBuilder = ChannelBuilder.create(zonesCountChannelUID, "Number")
                        .withLabel("Zones Count").withDescription("Number of irrigation zones on this device");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONES_COUNT);
            }

            // 6. Rain Delay (Number:Time)
            ChannelUID rainDelayChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_RAIN_DELAY);
            if (getThing().getChannel(rainDelayChannelUID) == null) {
                ChannelTypeUID channelTypeUID = RachioBindingConstants.CHANNEL_TYPE_DEVICE_RAIN_DELAY;
                Configuration config = new Configuration();
                config.put("unit", "s");
                ChannelBuilder channelBuilder = ChannelBuilder.create(rainDelayChannelUID, "Number")
                        .withType(channelTypeUID).withLabel("Rain Delay")
                        .withDescription("Rain delay remaining in seconds").withConfiguration(config);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_RAIN_DELAY);
            }

            // ===== WEATHER INTELLIGENCE CHANNELS =====

            // 7. Forecast Temperature (Number:Temperature)
            ChannelUID forecastTempChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_DEVICE_FORECAST_TEMP);
            if (getThing().getChannel(forecastTempChannelUID) == null) {
                Configuration config = new Configuration();
                config.put("unit", "°C");
                ChannelBuilder channelBuilder = ChannelBuilder.create(forecastTempChannelUID, "Number")
                        .withLabel("Current Temperature").withDescription("Current temperature from weather forecast")
                        .withConfiguration(config);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_DEVICE_FORECAST_TEMP);
            }

            // 8. Forecast Precipitation (Number:Dimensionless)
            ChannelUID forecastPrecipChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_DEVICE_FORECAST_PRECIP);
            if (getThing().getChannel(forecastPrecipChannelUID) == null) {
                Configuration config = new Configuration();
                config.put("unit", "%");
                ChannelBuilder channelBuilder = ChannelBuilder.create(forecastPrecipChannelUID, "Number")
                        .withLabel("Precipitation Probability").withDescription("Probability of precipitation (0-100%)")
                        .withConfiguration(config);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_DEVICE_FORECAST_PRECIP);
            }

            // 9. Forecast Precipitation Accumulation (Number:Length)
            ChannelUID forecastPrecipAccumChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_DEVICE_FORECAST_PRECIP_ACCUM);
            if (getThing().getChannel(forecastPrecipAccumChannelUID) == null) {
                ChannelTypeUID channelTypeUID = RachioBindingConstants.CHANNEL_TYPE_DEVICE_FORECAST_PRECIP_ACCUM;
                ChannelBuilder channelBuilder = ChannelBuilder.create(forecastPrecipAccumChannelUID, "Number")
                        .withType(channelTypeUID).withLabel("Precipitation Accumulation")
                        .withDescription("Expected precipitation accumulation in millimeters");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_DEVICE_FORECAST_PRECIP_ACCUM);
            }

            // 10. Forecast Humidity (Number:Dimensionless)
            ChannelUID forecastHumidityChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_DEVICE_FORECAST_HUMIDITY);
            if (getThing().getChannel(forecastHumidityChannelUID) == null) {
                Configuration config = new Configuration();
                config.put("unit", "%");
                ChannelBuilder channelBuilder = ChannelBuilder.create(forecastHumidityChannelUID, "Number")
                        .withLabel("Humidity").withDescription("Current humidity percentage").withConfiguration(config);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_DEVICE_FORECAST_HUMIDITY);
            }

            // 11. Forecast Wind Speed (Number:Speed)
            ChannelUID forecastWindChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_DEVICE_FORECAST_WIND);
            if (getThing().getChannel(forecastWindChannelUID) == null) {
                Configuration config = new Configuration();
                config.put("unit", "km/h");
                ChannelBuilder channelBuilder = ChannelBuilder.create(forecastWindChannelUID, "Number")
                        .withLabel("Wind Speed").withDescription("Current wind speed in km/h")
                        .withConfiguration(config);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_DEVICE_FORECAST_WIND);
            }

            // 12. Forecast Evapotranspiration (Number:Length)
            ChannelUID forecastEtChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_DEVICE_FORECAST_ET);
            if (getThing().getChannel(forecastEtChannelUID) == null) {
                ChannelBuilder channelBuilder = ChannelBuilder.create(forecastEtChannelUID, "Number")
                        .withLabel("Evapotranspiration")
                        .withDescription("Daily evapotranspiration rate in millimeters");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_DEVICE_FORECAST_ET);
            }

            // 13. Forecast Temperature Tomorrow (Number:Temperature)
            ChannelUID forecastTempTomorrowChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_FORECAST_TEMP_TOMORROW);
            if (getThing().getChannel(forecastTempTomorrowChannelUID) == null) {
                Configuration config = new Configuration();
                config.put("unit", "°C");
                ChannelBuilder channelBuilder = ChannelBuilder.create(forecastTempTomorrowChannelUID, "Number")
                        .withLabel("Tomorrow's Temperature").withDescription("Forecast temperature for tomorrow")
                        .withConfiguration(config);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_FORECAST_TEMP_TOMORROW);
            }

            // 14. Forecast Precipitation Tomorrow (Number:Dimensionless)
            ChannelUID forecastPrecipTomorrowChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_FORECAST_PRECIP_TOMORROW);
            if (getThing().getChannel(forecastPrecipTomorrowChannelUID) == null) {
                Configuration config = new Configuration();
                config.put("unit", "%");
                ChannelBuilder channelBuilder = ChannelBuilder.create(forecastPrecipTomorrowChannelUID, "Number")
                        .withLabel("Tomorrow's Precipitation")
                        .withDescription("Probability of precipitation tomorrow (0-100%)").withConfiguration(config);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_FORECAST_PRECIP_TOMORROW);
            }

            // 15. Forecast Humidity Tomorrow (Number:Dimensionless)
            ChannelUID forecastHumidityTomorrowChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_FORECAST_HUMIDITY_TOMORROW);
            if (getThing().getChannel(forecastHumidityTomorrowChannelUID) == null) {
                Configuration config = new Configuration();
                config.put("unit", "%");
                ChannelBuilder channelBuilder = ChannelBuilder.create(forecastHumidityTomorrowChannelUID, "Number")
                        .withLabel("Tomorrow's Humidity").withDescription("Forecast humidity percentage for tomorrow")
                        .withConfiguration(config);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_FORECAST_HUMIDITY_TOMORROW);
            }

            // ===== WATER ANALYTICS CHANNELS =====

            // 16. Usage Total (Number)
            ChannelUID usageTotalChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_DEVICE_USAGE_TOTAL);
            if (getThing().getChannel(usageTotalChannelUID) == null) {
                ChannelBuilder channelBuilder = ChannelBuilder.create(usageTotalChannelUID, "Number")
                        .withLabel("Total Usage").withDescription("Total water usage metric");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_DEVICE_USAGE_TOTAL);
            }

            // 17. Usage Water (Number:Volume)
            ChannelUID usageWaterChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_DEVICE_USAGE_WATER);
            if (getThing().getChannel(usageWaterChannelUID) == null) {
                Configuration config = new Configuration();
                config.put("unit", "L");
                ChannelBuilder channelBuilder = ChannelBuilder.create(usageWaterChannelUID, "Number")
                        .withLabel("Water Usage").withDescription("Total water used in liters")
                        .withConfiguration(config);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_DEVICE_USAGE_WATER);
            }

            // 18. Usage Start (DateTime)
            ChannelUID usageStartChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_DEVICE_USAGE_START);
            if (getThing().getChannel(usageStartChannelUID) == null) {
                ChannelBuilder channelBuilder = ChannelBuilder.create(usageStartChannelUID, "DateTime")
                        .withLabel("Usage Start").withDescription("Start date/time of water usage period");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_DEVICE_USAGE_START);
            }

            // 19. Usage End (DateTime)
            ChannelUID usageEndChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_DEVICE_USAGE_END);
            if (getThing().getChannel(usageEndChannelUID) == null) {
                ChannelBuilder channelBuilder = ChannelBuilder.create(usageEndChannelUID, "DateTime")
                        .withLabel("Usage End").withDescription("End date/time of water usage period");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_DEVICE_USAGE_END);
            }

            // 20. Device Alert (String)
            ChannelUID alertChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_DEVICE_ALERT);
            if (getThing().getChannel(alertChannelUID) == null) {
                ChannelBuilder channelBuilder = ChannelBuilder.create(alertChannelUID, "String")
                        .withLabel("Device Alert")
                        .withDescription("Current alert status (SMART_SKIP_ACTIVE, NORMAL, etc.)");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_DEVICE_ALERT);
            }

            if (modified) {
                updateThing(thingBuilder.build());
                channelsCreated = true;
                logger.info("Successfully created {} device channels for thing {}", getThing().getChannels().size(),
                        getThing().getUID());
            } else {
                logger.debug("All device channels already exist");
                channelsCreated = true;
            }

        } catch (Exception e) {
            logger.error("Failed to create device channels: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Failed to create device channels: " + e.getMessage());
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio device handler for thing {}", getThing().getUID());

        RachioBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null) {
            bridgeHandler.unregisterStatusListener(this);
        }

        stopRefresh();

        // Reset channels created flag
        channelsCreated = false;

        super.dispose();
    }

    @Override
    protected void performRefresh(ChannelUID channelUID) {
        // Implementation of abstract method from RachioHandler
        logger.debug("Performing refresh for channel {} in device handler", channelUID);
        refreshDeviceData();
    }

    // ===================================================================
    // RachioStatusListener Interface Implementation
    // ===================================================================

    @Override
    public String getListenerId() {
        // Return the Thing's UID as a unique identifier for this listener
        return getThing().getUID().toString();
    }

    @Override
    public ListenerType getListenerType() {
        // This is a device-level handler
        return ListenerType.DEVICE;
    }

    @Override
    public void onNewDevice(@Nullable String deviceId) {
        logger.debug("Listener {} notified of new device: {}", getListenerId(), deviceId);
        // Device discovery is primarily handled by RachioDiscoveryService
    }

    @Override
    public void onNewZone(@Nullable String deviceId, @Nullable String zoneId) {
        logger.debug("Listener {} notified of new zone {} on device {}", getListenerId(), zoneId, deviceId);
        // Zone discovery is primarily handled by RachioDiscoveryService
    }

    @Override
    public void onDeviceStatusChanged(@Nullable RachioDevice device) {
        if (device != null) {
            logger.debug("Listener {} received full device update for: {}", getListenerId(), device.id);
            updateDeviceData(device);
        }
    }

    @Override
    public void onDeviceStatusChanged(@Nullable String deviceId, @Nullable String status) {
        logger.debug("Listener {} received status update for device {}: {}", getListenerId(), deviceId, status);
        // Status updates via this method may be triggered by webhooks.
        // Trigger a refresh to get full device data.
        if (deviceId != null && deviceId.equals(getDeviceIdFromConfig())) {
            scheduler.submit(this::refreshDeviceData);
        }
    }

    @Override
    public void onWebhookEventReceived(@Nullable String eventJson) {
        logger.trace("Listener {} received raw webhook event: {}", getListenerId(), eventJson);
        // Raw JSON processing is handled by RachioWebHookServlet
    }

    @Override
    public void onDeviceUpdated(@Nullable RachioDevice device) {
        // Alias for onDeviceStatusChanged(RachioDevice) for clarity in bridge logic
        onDeviceStatusChanged(device);
    }

    @Override
    public void onZoneUpdated(@Nullable String zoneId, @Nullable RachioZone zone) {
        logger.debug("Listener {} received update for zone {}: {}", getListenerId(), zoneId,
                zone != null ? zone.name : "null");
        // Zone-specific updates are primarily handled by RachioZoneHandler
    }

    @Override
    public void onZoneStatusChanged(@Nullable String deviceId, @Nullable String zoneId, @Nullable String status) {
        logger.debug("Listener {} received status for zone {} on device {}: {}", getListenerId(), zoneId, deviceId,
                status);
        // Zone run status is primarily handled by RachioZoneHandler
    }

    @Override
    public void onRateLimitStatusChanged(int remaining, int limit, @Nullable String status) {
        logger.debug("Listener {} notified of rate limit change: {}/{}, status: {}", getListenerId(), remaining, limit,
                status);
        // Rate limit monitoring is a bridge-level concern, logged here for awareness.
    }

    // Helper to get device ID from config
    private @Nullable String getDeviceIdFromConfig() {
        RachioDeviceConfiguration localConfig = config;
        return (localConfig != null) ? localConfig.deviceId : null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String channelId = channelUID.getId();

        if (command instanceof RefreshType) {
            handleRefreshCommand(channelUID, () -> performRefresh(channelUID));
            return;
        }

        logger.debug("Received command {} for channel {}", command, channelId);

        if (RachioBindingConstants.CHANNEL_PAUSED.equals(channelId) && command instanceof OnOffType) {
            handlePauseCommand((OnOffType) command);
        }
    }

    private void handlePauseCommand(OnOffType command) {
        RachioBridgeHandler bridgeHandler = getBridgeHandler();
        RachioDeviceConfiguration localConfig = config;

        if (bridgeHandler == null || localConfig == null || localConfig.deviceId == null) {
            logger.warn("Cannot handle pause command - missing bridge or config");
            return;
        }

        scheduler.submit(() -> {
            try {
                boolean shouldPause = (command == OnOffType.ON);
                RachioApiClient apiClient = bridgeHandler.getApiClient();
                if (apiClient != null) {
                    // Note: setDevicePaused method may need to be added to RachioApiClient
                    // For now, we'll log and implement a placeholder
                    logger.info("Device {} pause {} requested", localConfig.deviceId, shouldPause ? "ON" : "OFF");
                    // In full implementation: apiClient.setDevicePaused(localConfig.deviceId, shouldPause);
                }
                updateState(RachioBindingConstants.CHANNEL_PAUSED, command);
            } catch (Exception e) {
                logger.error("Failed to {} device: {}", command == OnOffType.ON ? "pause" : "unpause", e.getMessage());
            }
        });
    }

    public void refreshDeviceData() {
        logger.debug("Refreshing device data for thing {}", getThing().getUID());

        RachioBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge not available");
            return;
        }

        RachioDeviceConfiguration localConfig = config;
        if (localConfig == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Configuration missing");
            return;
        }

        String deviceId = localConfig.deviceId;
        if (deviceId == null || deviceId.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device ID not configured");
            return;
        }

        try {
            RachioApiClient apiClient = bridgeHandler.getApiClient();
            if (apiClient == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "API client not available");
                return;
            }

            RachioDevice deviceData = apiClient.getDevice(deviceId);
            if (deviceData != null) {
                lastDeviceData = deviceData;
                updateDeviceData(deviceData);

                scheduler.submit(() -> {
                    try {
                        RachioSavings savingsData = apiClient.getSavings(deviceId); // Changed from getDeviceSavings()
                        if (savingsData != null) {
                            updateSavingsData(savingsData);
                        }
                    } catch (Exception e) {
                        logger.debug("Failed to fetch savings data: {}", e.getMessage());
                    }
                });

                scheduler.submit(() -> {
                    try {
                        RachioForecast forecastData = apiClient.getForecast(deviceId);
                        if (forecastData != null) {
                            lastForecastData = forecastData;
                            updateForecastData(forecastData);
                        }
                    } catch (Exception e) {
                        logger.debug("Failed to fetch forecast data: {}", e.getMessage());
                    }
                });

                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "No device data received");
            }
        } catch (Exception e) {
            logger.error("Error refreshing device {}: {}", deviceId, e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private @Nullable RachioBridgeHandler getBridgeHandler() {
        var bridge = getBridge();
        return bridge != null ? (RachioBridgeHandler) bridge.getHandler() : null;
    }

    public void updateDeviceData(@Nullable RachioDevice device) {
        if (device == null)
            return;

        lastDeviceData = device;
        logger.debug("Updating device data for {}: {}", device.id, device.name);

        updateProperty(RachioBindingConstants.PROPERTY_ID, device.id);
        updateProperty(RachioBindingConstants.PROPERTY_STATUS, device.getStatusNonNull());
        updateProperty(RachioBindingConstants.PROPERTY_PAUSED, String.valueOf(device.paused));
        updateProperty(RachioBindingConstants.PROPERTY_ENABLED, String.valueOf(device.isEnabled()));

        String deviceModel = device.getModelNonNull();
        if (!deviceModel.isEmpty()) {
            updateProperty(RachioBindingConstants.PROPERTY_MODEL, deviceModel);
        }

        String deviceSerial = device.getSerialNumberSafe();
        if (!deviceSerial.isEmpty()) {
            updateProperty(RachioBindingConstants.PROPERTY_SERIAL, deviceSerial);
        }

        String deviceName = device.getName();
        if (!deviceName.isEmpty()) {
            updateProperty(RachioBindingConstants.PROPERTY_DEVICE_NAME, deviceName);
        }

        Instant createdDate = device.createdDate;
        if (createdDate != null) {
            updateProperty(RachioBindingConstants.PROPERTY_CREATED_DATE, createdDate.toString());
        }

        updateState(RachioBindingConstants.CHANNEL_STATUS, new StringType(device.getStatusNonNull()));
        updateState(RachioBindingConstants.CHANNEL_PAUSED, device.paused ? OnOffType.ON : OnOffType.OFF);
        updateState(RachioBindingConstants.CHANNEL_ENABLED, device.isEnabled() ? OnOffType.ON : OnOffType.OFF);
        updateState(RachioBindingConstants.CHANNEL_DELETED, device.deleted ? OnOffType.ON : OnOffType.OFF);

        List<RachioZone> deviceZones = device.zones;
        if (deviceZones != null) {
            updateState(RachioBindingConstants.CHANNEL_ZONES_COUNT, new DecimalType(deviceZones.size()));
        }

        Instant rainDelayExp = device.rainDelayExpiration;
        if (rainDelayExp != null) {
            long nowSeconds = Instant.now().getEpochSecond();
            long expirationSeconds = rainDelayExp.getEpochSecond();
            long rainDelaySeconds = Math.max(0, expirationSeconds - nowSeconds);
            updateState(RachioBindingConstants.CHANNEL_RAIN_DELAY, new DecimalType(rainDelaySeconds));
        } else {
            updateState(RachioBindingConstants.CHANNEL_RAIN_DELAY, new DecimalType(0));
        }

        logger.debug("Device paused field: {}", device.paused);
    }

    private void updateSavingsData(@Nullable RachioSavings savings) {
        if (savings == null)
            return;

        logger.debug("Updating savings data for device");

        String startDateStr = savings.getStartDate();
        String endDateStr = savings.getEndDate();
        Double totalSavings = savings.getTotalSavings();
        Double waterSaved = savings.getWaterSaved();

        if (startDateStr != null && !startDateStr.isEmpty()) {
            try {
                // Parse ISO date string to Instant
                Instant startInstant = Instant.parse(startDateStr);
                updateState(RachioBindingConstants.CHANNEL_DEVICE_USAGE_START,
                        new DateTimeType(ZonedDateTime.ofInstant(startInstant, ZoneId.systemDefault())));
            } catch (Exception e) {
                logger.debug("Failed to parse start date: {}", startDateStr);
            }
        }

        if (endDateStr != null && !endDateStr.isEmpty()) {
            try {
                Instant endInstant = Instant.parse(endDateStr);
                updateState(RachioBindingConstants.CHANNEL_DEVICE_USAGE_END,
                        new DateTimeType(ZonedDateTime.ofInstant(endInstant, ZoneId.systemDefault())));
            } catch (Exception e) {
                logger.debug("Failed to parse end date: {}", endDateStr);
            }
        }

        if (totalSavings != null) {
            updateState(RachioBindingConstants.CHANNEL_DEVICE_USAGE_TOTAL, new QuantityType<>(totalSavings, Units.ONE));
        }

        if (waterSaved != null) {
            // waterSaved is likely in gallons, convert to litres for OpenHAB metric
            double waterSavedLitres = waterSaved * 3.78541;
            updateState(RachioBindingConstants.CHANNEL_DEVICE_USAGE_WATER,
                    new QuantityType<>(waterSavedLitres, Units.LITRE));
        }
    }

    private void updateForecastData(@Nullable RachioForecast forecast) {
        if (forecast == null)
            return;

        logger.debug("Updating forecast data for device");

        RachioForecast.DailyForecast todayForecast = forecast.getToday();
        RachioForecast.DailyForecast tomorrowForecast = forecast.getTomorrow();
        RachioForecast.CurrentConditions current = forecast.getCurrent();

        if (todayForecast != null) {
            Double tempHigh = todayForecast.getTemperatureHigh();
            if (tempHigh != null) {
                updateState(RachioBindingConstants.CHANNEL_DEVICE_FORECAST_TEMP,
                        new QuantityType<>(tempHigh, SIUnits.CELSIUS));
            }

            Double tempLow = todayForecast.getTemperatureLow();
            if (tempLow != null) {
                updateState(RachioBindingConstants.CHANNEL_FORECAST_TEMP_TOMORROW,
                        new QuantityType<>(tempLow, SIUnits.CELSIUS));
            }

            Double precipProb = todayForecast.precipProbability;
            if (precipProb != null) {
                updateState(RachioBindingConstants.CHANNEL_DEVICE_FORECAST_PRECIP,
                        new QuantityType<>(precipProb * 100, Units.PERCENT));
            }

            Double precipAccum = todayForecast.precipAccumulation;
            if (precipAccum != null) {
                updateState(RachioBindingConstants.CHANNEL_DEVICE_FORECAST_PRECIP_ACCUM,
                        new QuantityType<>(precipAccum, MetricPrefix.MILLI(SIUnits.METRE)));
            }

            Double et = todayForecast.getEvapotranspiration();
            if (et != null) {
                updateState(RachioBindingConstants.CHANNEL_DEVICE_FORECAST_ET,
                        new QuantityType<>(et, MetricPrefix.MILLI(SIUnits.METRE)));
            }

            Boolean smartSkip = todayForecast.getSmartSkip();
            if (smartSkip != null) {
                updateState(RachioBindingConstants.CHANNEL_DEVICE_ALERT,
                        new StringType(smartSkip ? "SMART_SKIP_ACTIVE" : "NORMAL"));
            }
        }

        if (tomorrowForecast != null) {
            Double tempHighTomorrow = tomorrowForecast.getTemperatureHigh();
            if (tempHighTomorrow != null) {
                updateState(RachioBindingConstants.CHANNEL_FORECAST_TEMP_TOMORROW,
                        new QuantityType<>(tempHighTomorrow, SIUnits.CELSIUS));
            }

            Double tomorrowPrecipProb = tomorrowForecast.precipProbability;
            if (tomorrowPrecipProb != null) {
                updateState(RachioBindingConstants.CHANNEL_FORECAST_PRECIP_TOMORROW,
                        new QuantityType<>(tomorrowPrecipProb * 100, Units.PERCENT));
            }
        }

        if (current != null) {
            Double currentTemp = current.temperature;
            if (currentTemp != null) {
                updateState(RachioBindingConstants.CHANNEL_DEVICE_FORECAST_TEMP,
                        new QuantityType<>(currentTemp, SIUnits.CELSIUS));
            }

            Double currentHumidity = current.humidity;
            if (currentHumidity != null) {
                updateState(RachioBindingConstants.CHANNEL_DEVICE_FORECAST_HUMIDITY,
                        new QuantityType<>(currentHumidity * 100, Units.PERCENT));
            }

            Double currentWindSpeed = current.windSpeed;
            if (currentWindSpeed != null) {
                updateState(RachioBindingConstants.CHANNEL_DEVICE_FORECAST_WIND,
                        new QuantityType<>(currentWindSpeed, SIUnits.KILOMETRE_PER_HOUR));
            }
        }
    }
}
