package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioAlert;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioForecast;
import org.openhab.binding.rachio.internal.api.dto.RachioForecast.ForecastDay;
import org.openhab.binding.rachio.internal.api.dto.RachioUsage;
import org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioDeviceConfiguration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for Rachio devices with professional irrigation features
 *
 * @author Damion Boyett - Enhanced with professional monitoring and analytics
 */
@NonNullByDefault
public class RachioDeviceHandler extends BaseThingHandler implements RachioHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioDeviceHandler.class);
    
    private static final int REFRESH_INTERVAL = 60; // seconds
    private static final int FORECAST_CACHE_MINUTES = 30;
    private static final int USAGE_CACHE_MINUTES = 60;
    
    private @Nullable RachioDeviceConfiguration config;
    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable RachioDevice device;
    private @Nullable ScheduledFuture<?> refreshJob;
    
    private @Nullable RachioForecast cachedForecast;
    private @Nullable Instant forecastCacheTime;
    private @Nullable RachioUsage cachedUsage;
    private @Nullable Instant usageCacheTime;
    
    private boolean rainDelayActive = false;
    private @Nullable Instant rainDelayEndTime;
    private boolean schedulePaused = false;
    
    public RachioDeviceHandler(Thing thing) {
        super(thing);
    }
    
    @Override
    public void initialize() {
        logger.debug("Initializing Rachio device handler");
        
        config = getConfigAs(RachioDeviceConfiguration.class);
        if (config == null || config.deviceId.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, 
                        "Device ID not configured");
            return;
        }
        
        bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, 
                        "Bridge not found or offline");
            return;
        }
        
        // Create dynamic channels for professional features
        createProfessionalChannels();
        
        // Schedule refresh
        scheduleRefresh();
        
        // Initial refresh
        scheduler.schedule(this::refresh, 2, TimeUnit.SECONDS);
        
        updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.NONE, "Initializing...");
    }
    
    @Override
    public void dispose() {
        logger.debug("Disposing Rachio device handler");
        
        stopRefresh();
        
        super.dispose();
    }
    
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            refreshChannel(channelUID);
            return;
        }
        
        String channelId = channelUID.getId();
        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        
        if (localBridgeHandler == null) {
            logger.error("Bridge handler not available");
            return;
        }
        
        String deviceId = getDeviceId();
        if (deviceId == null) {
            logger.error("Device ID not available");
            return;
        }
        
        try {
            switch (channelId) {
                case CHANNEL_DEVICE_ON:
                    if (command instanceof OnOffType) {
                        // Note: Rachio API doesn't have a direct on/off toggle
                        // This would need to be implemented through schedule modes
                        logger.info("Device on/off control not directly supported via API");
                    }
                    break;
                    
                case CHANNEL_DEVICE_PAUSE:
                    if (command instanceof OnOffType) {
                        boolean pause = command == OnOffType.ON;
                        // Pause/resume schedule
                        // This would require additional API implementation
                        logger.info("Schedule pause/resume not yet implemented");
                    }
                    break;
                    
                case CHANNEL_RAIN_DELAY:
                    if (command instanceof DecimalType) {
                        int duration = ((DecimalType) command).intValue();
                        if (duration > 0) {
                            localBridgeHandler.setRainDelay(deviceId, duration);
                            logger.info("Set rain delay on device {} for {} seconds", deviceId, duration);
                        } else {
                            // Cancel rain delay
                            localBridgeHandler.setRainDelay(deviceId, 0);
                            logger.info("Canceled rain delay on device {}", deviceId);
                        }
                    }
                    break;
                    
                case CHANNEL_STOP_WATERING:
                    if (command == OnOffType.ON) {
                        localBridgeHandler.stopWatering(deviceId, "OPENHAB");
                        logger.info("Stopped all watering on device {}", deviceId);
                    }
                    break;
                    
                case CHANNEL_RUN_ALL_ZONES:
                    if (command instanceof DecimalType) {
                        int duration = ((DecimalType) command).intValue();
                        if (duration > 0) {
                            localBridgeHandler.runAllZones(deviceId, duration, "OPENHAB");
                            logger.info("Running all zones on device {} for {} seconds", deviceId, duration);
                        }
                    }
                    break;
                    
                case CHANNEL_RUN_NEXT_ZONE:
                    if (command == OnOffType.ON) {
                        localBridgeHandler.runNextZone(deviceId, "OPENHAB");
                        logger.info("Running next zone on device {}", deviceId);
                    }
                    break;
                    
                default:
                    logger.debug("Unhandled command {} for channel {}", command, channelId);
            }
            
        } catch (Exception e) {
            logger.error("Error handling command {} for channel {}: {}", command, channelId, e.getMessage(), e);
        }
    }
    
    private void createProfessionalChannels() {
        ThingBuilder thingBuilder = editThing();
        boolean channelsModified = false;
        
        // Weather forecast channels
        if (getThing().getChannel(CHANNEL_FORECAST_TEMP) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_FORECAST_TEMP), "Number:Temperature")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_FORECAST_TEMP))
                    .withLabel("Forecast Temperature")
                    .withDescription("Current temperature from forecast")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        if (getThing().getChannel(CHANNEL_FORECAST_PRECIP) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_FORECAST_PRECIP), "Number:Length")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_FORECAST_PRECIP))
                    .withLabel("Forecast Precipitation")
                    .withDescription("Current precipitation from forecast")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        if (getThing().getChannel(CHANNEL_FORECAST_HUMIDITY) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_FORECAST_HUMIDITY), "Number:Dimensionless")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_FORECAST_HUMIDITY))
                    .withLabel("Forecast Humidity")
                    .withDescription("Current humidity from forecast")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        if (getThing().getChannel(CHANNEL_FORECAST_ET) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_FORECAST_ET), "Number:Length")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_FORECAST_ET))
                    .withLabel("Forecast ET")
                    .withDescription("Evapotranspiration from forecast")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        if (getThing().getChannel(CHANNEL_FORECAST_WIND) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_FORECAST_WIND), "Number:Speed")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_FORECAST_WIND))
                    .withLabel("Forecast Wind Speed")
                    .withDescription("Wind speed from forecast")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        // Water usage analytics channels
        if (getThing().getChannel(CHANNEL_USAGE_TOTAL) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_USAGE_TOTAL), "Number:Volume")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_USAGE_TOTAL))
                    .withLabel("Total Water Usage")
                    .withDescription("Total water used by device")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        if (getThing().getChannel(CHANNEL_USAGE_SAVINGS) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_USAGE_SAVINGS), "Number:Volume")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_USAGE_SAVINGS))
                    .withLabel("Water Savings")
                    .withDescription("Water saved compared to traditional irrigation")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        if (getThing().getChannel(CHANNEL_USAGE_EFFICIENCY) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_USAGE_EFFICIENCY), "Number:Dimensionless")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_USAGE_EFFICIENCY))
                    .withLabel("Water Efficiency")
                    .withDescription("Water efficiency percentage")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        if (getThing().getChannel(CHANNEL_SAVINGS_MONEY) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_SAVINGS_MONEY), "Number:Currency")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_SAVINGS_MONEY))
                    .withLabel("Money Savings")
                    .withDescription("Money saved on water bills")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        if (getThing().getChannel(CHANNEL_SAVINGS_CO2) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_SAVINGS_CO2), "Number:Mass")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_SAVINGS_CO2))
                    .withLabel("CO2 Savings")
                    .withDescription("CO2 emissions saved")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        // Alert channels
        if (getThing().getChannel(CHANNEL_ALERT_STATUS) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_ALERT_STATUS), "String")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_ALERT_STATUS))
                    .withLabel("Alert Status")
                    .withDescription("Current alert status")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        if (getThing().getChannel(CHANNEL_ALERT_LAST) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_ALERT_LAST), "String")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_ALERT_LAST))
                    .withLabel("Last Alert")
                    .withDescription("Most recent alert")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        // Update thing if channels were added
        if (channelsModified) {
            updateThing(thingBuilder.build());
            logger.debug("Added professional channels to device {}", getDeviceId());
        }
    }
    
    private void scheduleRefresh() {
        if (refreshJob == null || refreshJob.isCancelled()) {
            refreshJob = scheduler.scheduleWithFixedDelay(this::refresh, 
                    REFRESH_INTERVAL, REFRESH_INTERVAL, TimeUnit.SECONDS);
            logger.debug("Scheduled device refresh every {} seconds", REFRESH_INTERVAL);
        }
    }
    
    private void stopRefresh() {
        if (refreshJob != null && !refreshJob.isCancelled()) {
            refreshJob.cancel(true);
            refreshJob = null;
            logger.debug("Stopped device refresh job");
        }
    }
    
    public void refresh() {
        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }
        
        String deviceId = getDeviceId();
        if (deviceId == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
            return;
        }
        
        try {
            // Get device info
            RachioDevice updatedDevice = localBridgeHandler.getDevice(deviceId);
            if (updatedDevice == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, 
                            "Device not found");
                return;
            }
            
            // Update device object
            device = updatedDevice;
            
            // Update channels
            updateChannels(updatedDevice);
            
            // Update professional data (forecast, usage, etc.)
            updateProfessionalData(deviceId);
            
            // Update thing status
            updateDeviceStatus(updatedDevice);
            
            logger.trace("Device {} refresh completed", deviceId);
            
        } catch (Exception e) {
            logger.debug("Error refreshing device {}: {}", deviceId, e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }
    
    private void updateChannels(RachioDevice device) {
        // Basic device info
        updateState(CHANNEL_DEVICE_NAME, new StringType(device.getName()));
        updateState(CHANNEL_DEVICE_MODEL, new StringType(device.getModel()));
        updateState(CHANNEL_DEVICE_SERIAL, new StringType(device.getSerialNumber()));
        updateState(CHANNEL_DEVICE_MAC, new StringType(device.getMacAddress()));
        
        // Device status
        updateState(CHANNEL_DEVICE_STATUS, new StringType(device.getStatus()));
        updateState(CHANNEL_DEVICE_ON, device.isOn() ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_DEVICE_ZONE_COUNT, new DecimalType(device.getZoneCount()));
        
        // Rain delay
        rainDelayActive = device.isRainDelayState();
        updateState(CHANNEL_RAIN_DELAY_ACTIVE, rainDelayActive ? OnOffType.ON : OnOffType.OFF);
        
        if (rainDelayActive && device.getRainDelayExpirationDate() != null) {
            rainDelayEndTime = device.getRainDelayExpirationDate();
            updateState(CHANNEL_RAIN_DELAY_END, new DateTimeType(ZonedDateTime.ofInstant(rainDelayEndTime, java.time.ZoneId.systemDefault())));
            
            long remainingSeconds = device.getRainDelayRemainingSeconds();
            updateState(CHANNEL_RAIN_DELAY_REMAINING, new QuantityType<>(remainingSeconds, Units.SECOND));
        } else {
            rainDelayEndTime = null;
            updateState(CHANNEL_RAIN_DELAY_END, new DateTimeType()); // Empty date
            updateState(CHANNEL_RAIN_DELAY_REMAINING, new QuantityType<>(0, Units.SECOND));
        }
        
        // Schedule mode
        String scheduleMode = device.getScheduleMode();
        schedulePaused = "PAUSED".equals(scheduleMode);
        updateState(CHANNEL_SCHEDULE_MODE, new StringType(scheduleMode != null ? scheduleMode : "UNKNOWN"));
        updateState(CHANNEL_DEVICE_PAUSE, schedulePaused ? OnOffType.ON : OnOffType.OFF);
        
        // Flex schedule
        updateState(CHANNEL_FLEX_SCHEDULE, device.isFlexScheduleRules() ? OnOffType.ON : OnOffType.OFF);
        
        // Location
        updateState(CHANNEL_LATITUDE, new DecimalType(device.getLatitude()));
        updateState(CHANNEL_LONGITUDE, new DecimalType(device.getLongitude()));
        updateState(CHANNEL_ELEVATION, new QuantityType<>(device.getElevation(), SIUnits.METRE));
        
        // Timezone
        if (device.getTimeZone() != null) {
            updateState(CHANNEL_TIMEZONE, new StringType(device.getTimeZone()));
        }
        
        // Watering status
        updateState(CHANNEL_WATERING_STATUS, device.isWatering() ? OnOffType.ON : OnOffType.OFF);
        
        // Active zone count
        updateState(CHANNEL_ACTIVE_ZONE_COUNT, new DecimalType(device.getActiveZoneCount()));
        
        // Total zone area
        updateState(CHANNEL_TOTAL_ZONE_AREA, new QuantityType<>(device.getTotalZoneArea(), Units.SQUARE_METRE));
    }
    
    private void updateProfessionalData(String deviceId) {
        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler == null) {
            return;
        }
        
        RachioHttp http = localBridgeHandler.getHttp();
        if (http == null) {
            return;
        }
        
        try {
            // Update forecast data (with caching)
            updateForecastData(deviceId, http);
            
            // Update usage data (with caching)
            updateUsageData(deviceId, http);
            
        } catch (Exception e) {
            logger.debug("Error updating professional data for device {}: {}", deviceId, e.getMessage());
        }
    }
    
    private void updateForecastData(String deviceId, RachioHttp http) throws Exception {
        Instant now = Instant.now();
        
        // Check cache
        if (cachedForecast != null && forecastCacheTime != null &&
            Duration.between(forecastCacheTime, now).toMinutes() < FORECAST_CACHE_MINUTES) {
            updateForecastChannels(cachedForecast);
            return;
        }
        
        // Fetch fresh forecast data
        // Fixed: Call getDeviceForecast method with deviceId
        RachioForecast forecast = http.getDeviceForecast(deviceId);
        if (forecast != null) {
            cachedForecast = forecast;
            forecastCacheTime = now;
            updateForecastChannels(forecast);
        }
    }
    
    private void updateForecastChannels(RachioForecast forecast) {
        if (forecast == null) {
            return;
        }
        
        // Current conditions
        if (forecast.currentTemp != null) {
            // Fixed: Use Units.CELSIUS constant
            updateState(CHANNEL_FORECAST_TEMP, new QuantityType<>(forecast.currentTemp, Units.CELSIUS));
        }
        
        if (forecast.currentPrecip != null) {
            // Fixed: Use SIUnits.MILLIMETER constant
            updateState(CHANNEL_FORECAST_PRECIP, new QuantityType<>(forecast.currentPrecip, SIUnits.MILLIMETER));
        }
        
        if (forecast.currentHumidity != null) {
            updateState(CHANNEL_FORECAST_HUMIDITY, new PercentType(forecast.currentHumidity.intValue()));
        }
        
        if (forecast.currentEt != null) {
            // Fixed: Use SIUnits.MILLIMETER constant
            updateState(CHANNEL_FORECAST_ET, new QuantityType<>(forecast.currentEt, SIUnits.MILLIMETER));
        }
        
        if (forecast.currentWindSpeed != null) {
            updateState(CHANNEL_FORECAST_WIND, new QuantityType<>(forecast.currentWindSpeed, Units.KILOMETRE_PER_HOUR));
        }
        
        // Forecast summary
        if (forecast.summary != null) {
            updateState(CHANNEL_FORECAST_SUMMARY, new StringType(forecast.summary));
        }
        
        // Daily forecast
        if (forecast.daily != null && !forecast.daily.isEmpty()) {
            ForecastDay today = forecast.daily.get(0);
            
            if (today.highTemp != null) {
                updateState(CHANNEL_FORECAST_HIGH, new QuantityType<>(today.highTemp, Units.CELSIUS));
            }
            
            if (today.lowTemp != null) {
                updateState(CHANNEL_FORECAST_LOW, new QuantityType<>(today.lowTemp, Units.CELSIUS));
            }
            
            if (today.precipProbability != null) {
                updateState(CHANNEL_FORECAST_PRECIP_PROB, new PercentType(today.precipProbability.intValue()));
            }
            
            if (today.precipAmount != null) {
                // Fixed: Use SIUnits.MILLIMETER constant
                updateState(CHANNEL_FORECAST_PRECIP_AMOUNT, new QuantityType<>(today.precipAmount, SIUnits.MILLIMETER));
            }
            
            if (today.et != null) {
                // Fixed: Use SIUnits.MILLIMETER constant
                updateState(CHANNEL_FORECAST_DAY_ET, new QuantityType<>(today.et, SIUnits.MILLIMETER));
            }
        }
    }
    
    private void updateUsageData(String deviceId, RachioHttp http) throws Exception {
        Instant now = Instant.now();
        
        // Check cache
        if (cachedUsage != null && usageCacheTime != null &&
            Duration.between(usageCacheTime, now).toMinutes() < USAGE_CACHE_MINUTES) {
            updateUsageChannels(cachedUsage);
            return;
        }
        
        // Fetch fresh usage data
        // Fixed: Call getDeviceUsage method with deviceId
        RachioUsage usage = http.getDeviceUsage(deviceId);
        if (usage != null) {
            cachedUsage = usage;
            usageCacheTime = now;
            updateUsageChannels(usage);
        }
    }
    
    private void updateUsageChannels(RachioUsage usage) {
        if (usage == null) {
            return;
        }
        
        // Total water usage
        if (usage.totalWaterUsed != null) {
            updateState(CHANNEL_USAGE_TOTAL, new QuantityType<>(usage.totalWaterUsed, Units.LITRE));
        }
        
        // Water savings
        if (usage.waterSavings != null) {
            updateState(CHANNEL_USAGE_SAVINGS, new QuantityType<>(usage.waterSavings, Units.LITRE));
        }
        
        // Water efficiency
        if (usage.efficiency != null) {
            updateState(CHANNEL_USAGE_EFFICIENCY, new PercentType(usage.efficiency.intValue()));
        }
        
        // Money savings
        if (usage.moneySavings != null) {
            updateState(CHANNEL_SAVINGS_MONEY, new QuantityType<>(usage.moneySavings, Units.US_DOLLAR));
        }
        
        // CO2 savings
        if (usage.co2Savings != null) {
            updateState(CHANNEL_SAVINGS_CO2, new QuantityType<>(usage.co2Savings, Units.KILOGRAM));
        }
        
        // Usage period
        if (usage.periodStart != null && usage.periodEnd != null) {
            updateState(CHANNEL_USAGE_PERIOD_START, new DateTimeType(ZonedDateTime.parse(usage.periodStart)));
            updateState(CHANNEL_USAGE_PERIOD_END, new DateTimeType(ZonedDateTime.parse(usage.periodEnd)));
        }
    }
    
    private void updateDeviceStatus(RachioDevice device) {
        ThingStatus status = device.getThingStatus();
        ThingStatusDetail detail = device.getThingStatusDetail();
        String description = device.getThingStatusDescription();
        
        updateStatus(status, detail, description);
    }
    
    private void refreshChannel(ChannelUID channelUID) {
        String channelId = channelUID.getId();
        RachioDevice localDevice = device;
        
        if (localDevice == null) {
            return;
        }
        
        // Refresh individual channel based on device data
        switch (channelId) {
            case CHANNEL_DEVICE_STATUS:
                updateState(channelUID, new StringType(localDevice.getStatus()));
                break;
            case CHANNEL_DEVICE_ON:
                updateState(channelUID, localDevice.isOn() ? OnOffType.ON : OnOffType.OFF);
                break;
            case CHANNEL_RAIN_DELAY_ACTIVE:
                updateState(channelUID, localDevice.isRainDelayState() ? OnOffType.ON : OnOffType.OFF);
                break;
            case CHANNEL_SCHEDULE_MODE:
                String scheduleMode = localDevice.getScheduleMode();
                updateState(channelUID, new StringType(scheduleMode != null ? scheduleMode : "UNKNOWN"));
                break;
            case CHANNEL_WATERING_STATUS:
                updateState(channelUID, localDevice.isWatering() ? OnOffType.ON : OnOffType.OFF);
                break;
            default:
                logger.debug("Unhandled refresh for channel: {}", channelId);
        }
    }
    
    // Event handlers
    
    public void onAllZonesStarted(int duration) {
        updateState(CHANNEL_WATERING_STATUS, OnOffType.ON);
        logger.info("All zones started on device {} for {} seconds", getDeviceId(), duration);
    }
    
    public void onNextZoneStarted() {
        updateState(CHANNEL_WATERING_STATUS, OnOffType.ON);
        logger.info("Next zone started on device {}", getDeviceId());
    }
    
    public void onWateringStopped() {
        updateState(CHANNEL_WATERING_STATUS, OnOffType.OFF);
        logger.info("All watering stopped on device {}", getDeviceId());
    }
    
    public void onRainDelaySet(int duration) {
        rainDelayActive = duration > 0;
        updateState(CHANNEL_RAIN_DELAY_ACTIVE, rainDelayActive ? OnOffType.ON : OnOffType.OFF);
        
        if (rainDelayActive) {
            rainDelayEndTime = Instant.now().plusSeconds(duration);
            updateState(CHANNEL_RAIN_DELAY_END, new DateTimeType(ZonedDateTime.ofInstant(rainDelayEndTime, java.time.ZoneId.systemDefault())));
            updateState(CHANNEL_RAIN_DELAY_REMAINING, new QuantityType<>(duration, Units.SECOND));
            logger.info("Rain delay set on device {} for {} seconds", getDeviceId(), duration);
        } else {
            rainDelayEndTime = null;
            updateState(CHANNEL_RAIN_DELAY_END, new DateTimeType());
            updateState(CHANNEL_RAIN_DELAY_REMAINING, new QuantityType<>(0, Units.SECOND));
            logger.info("Rain delay cleared on device {}", getDeviceId());
        }
    }
    
    public void updateDevice(RachioDevice updatedDevice) {
        this.device = updatedDevice;
        updateChannels(updatedDevice);
        updateDeviceStatus(updatedDevice);
    }
    
    // Utility methods
    
    private @Nullable RachioBridgeHandler getBridgeHandler() {
        return getBridge() != null ? (RachioBridgeHandler) getBridge().getHandler() : null;
    }
    
    public @Nullable String getDeviceId() {
        RachioDeviceConfiguration localConfig = config;
        return localConfig != null ? localConfig.deviceId : null;
    }
    
    public @Nullable RachioDevice getDevice() {
        return device;
    }
    
    public @Nullable RachioZone getZone(String zoneId) {
        RachioDevice localDevice = device;
        if (localDevice != null) {
            return localDevice.getZoneById(zoneId);
        }
        return null;
    }
    
    public boolean hasZone(String zoneId) {
        RachioDevice localDevice = device;
        if (localDevice != null) {
            return localDevice.getZoneById(zoneId) != null;
        }
        return false;
    }
    
    public List<RachioZone> getZones() {
        RachioDevice localDevice = device;
        if (localDevice != null) {
            List<RachioZone> zones = localDevice.getZones();
            return zones != null ? zones : List.of();
        }
        return List.of();
    }
    
    public boolean isRainDelayActive() {
        return rainDelayActive;
    }
    
    public @Nullable Instant getRainDelayEndTime() {
        return rainDelayEndTime;
    }
    
    public boolean isSchedulePaused() {
        return schedulePaused;
    }
    
    @Override
    public void handleWebhookEvent(RachioWebhookEvent event) {
        // Process webhook events for this device
        if (event != null && getDeviceId() != null) {
            String eventType = event.getType();
            String deviceId = event.getDeviceId();
            
            if (getDeviceId().equals(deviceId)) {
                logger.info("Device {} received webhook event: {}", deviceId, eventType);
                
                // Refresh device data when webhook event is received
                scheduler.schedule(this::refresh, 1, TimeUnit.SECONDS);
                
                // Update alert channels if this is an alert event
                if (eventType.contains("ALERT")) {
                    updateAlertChannels(event);
                }
            }
        }
    }
    
    private void updateAlertChannels(RachioWebhookEvent event) {
        // Parse alert information from event
        String alertMessage = event.getSummary();
        if (alertMessage != null) {
            updateState(CHANNEL_ALERT_STATUS, new StringType("ACTIVE"));
            updateState(CHANNEL_ALERT_LAST, new StringType(alertMessage));
            updateState(CHANNEL_ALERT_TIME, new DateTimeType(ZonedDateTime.now()));
            
            logger.warn("Device alert: {}", alertMessage);
        }
    }
    
    @Override
    public void handleImageCall(javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse resp) {
        // Image handling would be implemented here
        logger.debug("Image call received for device {}", getDeviceId());
    }
}
