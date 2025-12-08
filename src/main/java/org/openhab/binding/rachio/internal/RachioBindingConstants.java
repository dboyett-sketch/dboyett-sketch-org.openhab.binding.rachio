package org.openhab.binding.rachio.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link RachioBindingConstants} class defines common constants
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioBindingConstants {
    public static final String BINDING_ID = "rachio";
    
    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "device");
    public static final ThingTypeUID THING_TYPE_ZONE = new ThingTypeUID(BINDING_ID, "zone");
    
    // Bridge configuration parameters
    public static final String PARAM_API_KEY = "apiKey";
    public static final String PARAM_WEBHOOK_PORT = "webhookPort";
    public static final String PARAM_WEBHOOK_PATH = "webhookPath";
    public static final String PARAM_WEBHOOK_ENABLED = "webhookEnabled";
    public static final String PARAM_REFRESH_INTERVAL = "refreshInterval";
    public static final String PARAM_IP_FILTER_ENABLED = "ipFilterEnabled";
    public static final String PARAM_IP_FILTER_LIST = "ipFilterList";
    public static final String PARAM_AWS_IP_RANGES = "awsIpRanges";
    public static final String PARAM_IMAGE_PORT = "imagePort";
    
    // Device configuration parameters
    public static final String PARAM_DEVICE_ID = "deviceId";
    
    // Zone configuration parameters
    public static final String PARAM_ZONE_ID = "zoneId";
    
    // Bridge channels
    public static final String CHANNEL_RATE_LIMIT_REMAINING = "rateLimitRemaining";
    public static final String CHANNEL_RATE_LIMIT_LIMIT = "rateLimitLimit";
    public static final String CHANNEL_RATE_LIMIT_PERCENT = "rateLimitPercent";
    public static final String CHANNEL_RATE_LIMIT_STATUS = "rateLimitStatus";
    public static final String CHANNEL_PERSON_NAME = "personName";
    public static final String CHANNEL_DEVICE_COUNT = "deviceCount";
    public static final String CHANNEL_ZONE_COUNT = "zoneCount";
    public static final String CHANNEL_WEBHOOK_STATUS = "webhookStatus";
    public static final String CHANNEL_API_STATUS = "apiStatus";
    public static final String CHANNEL_WEBHOOK_LAST_EVENT = "webhookLastEvent";
    public static final String CHANNEL_WEBHOOK_LAST_TIME = "webhookLastTime";
    
    // Device channels
    public static final String CHANNEL_DEVICE_NAME = "deviceName";
    public static final String CHANNEL_DEVICE_MODEL = "deviceModel";
    public static final String CHANNEL_DEVICE_SERIAL = "deviceSerial";
    public static final String CHANNEL_DEVICE_MAC = "deviceMac";
    public static final String CHANNEL_DEVICE_STATUS = "deviceStatus";
    public static final String CHANNEL_DEVICE_ON = "deviceOn";
    public static final String CHANNEL_DEVICE_ZONE_COUNT = "deviceZoneCount";
    public static final String CHANNEL_DEVICE_PAUSE = "devicePause";
    
    public static final String CHANNEL_RAIN_DELAY = "rainDelay";
    public static final String CHANNEL_RAIN_DELAY_ACTIVE = "rainDelayActive";
    public static final String CHANNEL_RAIN_DELAY_END = "rainDelayEnd";
    public static final String CHANNEL_RAIN_DELAY_REMAINING = "rainDelayRemaining";
    
    public static final String CHANNEL_STOP_WATERING = "stopWatering";
    public static final String CHANNEL_RUN_ALL_ZONES = "runAllZones";
    public static final String CHANNEL_RUN_NEXT_ZONE = "runNextZone";
    
    public static final String CHANNEL_SCHEDULE_MODE = "scheduleMode";
    public static final String CHANNEL_FLEX_SCHEDULE = "flexSchedule";
    
    public static final String CHANNEL_LATITUDE = "latitude";
    public static final String CHANNEL_LONGITUDE = "longitude";
    public static final String CHANNEL_ELEVATION = "elevation";
    public static final String CHANNEL_TIMEZONE = "timezone";
    
    public static final String CHANNEL_WATERING_STATUS = "wateringStatus";
    public static final String CHANNEL_ACTIVE_ZONE_COUNT = "activeZoneCount";
    public static final String CHANNEL_TOTAL_ZONE_AREA = "totalZoneArea";
    
    // Forecast channels
    public static final String CHANNEL_FORECAST_TEMP = "forecastTemperature";
    public static final String CHANNEL_FORECAST_PRECIP = "forecastPrecipitation";
    public static final String CHANNEL_FORECAST_HUMIDITY = "forecastHumidity";
    public static final String CHANNEL_FORECAST_ET = "forecastET";
    public static final String CHANNEL_FORECAST_WIND = "forecastWind";
    public static final String CHANNEL_FORECAST_SUMMARY = "forecastSummary";
    public static final String CHANNEL_FORECAST_HIGH = "forecastHigh";
    public static final String CHANNEL_FORECAST_LOW = "forecastLow";
    public static final String CHANNEL_FORECAST_PRECIP_PROB = "forecastPrecipProbability";
    public static final String CHANNEL_FORECAST_PRECIP_AMOUNT = "forecastPrecipAmount";
    public static final String CHANNEL_FORECAST_DAY_ET = "forecastDayET";
    
    // Usage channels
    public static final String CHANNEL_USAGE_TOTAL = "usageTotal";
    public static final String CHANNEL_USAGE_SAVINGS = "usageSavings";
    public static final String CHANNEL_USAGE_EFFICIENCY = "usageEfficiency";
    public static final String CHANNEL_USAGE_PERIOD_START = "usagePeriodStart";
    public static final String CHANNEL_USAGE_PERIOD_END = "usagePeriodEnd";
    
    // Savings channels
    public static final String CHANNEL_SAVINGS_MONEY = "savingsMoney";
    public static final String CHANNEL_SAVINGS_CO2 = "savingsCO2";
    
    // Alert channels
    public static final String CHANNEL_ALERT_STATUS = "alertStatus";
    public static final String CHANNEL_ALERT_LAST = "alertLast";
    public static final String CHANNEL_ALERT_TIME = "alertTime";
    
    // Zone channels
    public static final String CHANNEL_ZONE_NAME = "zoneName";
    public static final String CHANNEL_ZONE_NUMBER = "zoneNumber";
    public static final String CHANNEL_ZONE_ENABLED = "zoneEnabled";
    public static final String CHANNEL_ZONE_RUNTIME = "zoneRuntime";
    public static final String CHANNEL_ZONE_MAX_RUNTIME = "zoneMaxRuntime";
    public static final String CHANNEL_ZONE_STATUS = "zoneStatus";
    public static final String CHANNEL_ZONE_WATERING = "zoneWatering";
    public static final String CHANNEL_ZONE_REMAINING = "zoneRemaining";
    public static final String CHANNEL_ZONE_PERCENT = "zonePercent";
    public static final String CHANNEL_ZONE_START = "zoneStart";
    public static final String CHANNEL_ZONE_STOP = "zoneStop";
    
    // Professional irrigation channels
    public static final String CHANNEL_SOIL_TYPE = "soilType";
    public static final String CHANNEL_CROP_TYPE = "cropType";
    public static final String CHANNEL_NOZZLE_TYPE = "nozzleType";
    public static final String CHANNEL_SLOPE_TYPE = "slopeType";
    public static final String CHANNEL_SHADE_TYPE = "shadeType";
    public static final String CHANNEL_ROOT_DEPTH = "rootDepth";
    public static final String CHANNEL_IRRIGATION_EFFICIENCY = "irrigationEfficiency";
    public static final String CHANNEL_AVAILABLE_WATER = "availableWater";
    public static final String CHANNEL_ADJUSTMENT_LEVEL_PREFIX = "adjustmentLevel";
    public static final String CHANNEL_ZONE_AREA = "zoneArea";
    public static final String CHANNEL_NOZZLE_RATE = "nozzleRate";
    public static final String CHANNEL_CROP_COEFFICIENT = "cropCoefficient";
    public static final String CHANNEL_WATER_VOLUME = "waterVolume";
    public static final String CHANNEL_LAST_WATERED = "lastWatered";
    public static final String CHANNEL_LAST_RUN_DURATION = "lastRunDuration";
    public static final String CHANNEL_TOTAL_WATER_USED = "totalWaterUsed";
    
    // Thing properties
    public static final String PROPERTY_DEVICE_NAME = "deviceName";
    public static final String PROPERTY_DEVICE_MODEL = "deviceModel";
    public static final String PROPERTY_DEVICE_SERIAL = "deviceSerial";
    public static final String PROPERTY_DEVICE_MAC = "deviceMac";
    public static final String PROPERTY_ZONE_COUNT = "zoneCount";
    public static final String PROPERTY_LATITUDE = "latitude";
    public static final String PROPERTY_LONGITUDE = "longitude";
    public static final String PROPERTY_ELEVATION = "elevation";
    public static final String PROPERTY_TIMEZONE = "timezone";
    public static final String PROPERTY_FLEX_SCHEDULE = "flexSchedule";
    
    public static final String PROPERTY_ZONE_NAME = "zoneName";
    public static final String PROPERTY_ZONE_NUMBER = "zoneNumber";
    public static final String PROPERTY_ZONE_ENABLED = "zoneEnabled";
    public static final String PROPERTY_ZONE_RUNTIME = "zoneRuntime";
    public static final String PROPERTY_ZONE_MAX_RUNTIME = "zoneMaxRuntime";
    public static final String PROPERTY_ZONE_AREA = "zoneArea";
    public static final String PROPERTY_SOIL_TYPE = "soilType";
    public static final String PROPERTY_CROP_TYPE = "cropType";
    public static final String PROPERTY_NOZZLE_TYPE = "nozzleType";
    public static final String PROPERTY_SLOPE_TYPE = "slopeType";
    public static final String PROPERTY_SHADE_TYPE = "shadeType";
    public static final String PROPERTY_ROOT_DEPTH = "rootDepth";
    public static final String PROPERTY_IRRIGATION_EFFICIENCY = "irrigationEfficiency";
    public static final String PROPERTY_AVAILABLE_WATER = "availableWater";
}
