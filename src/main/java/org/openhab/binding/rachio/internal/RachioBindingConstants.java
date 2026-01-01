package org.openhab.binding.rachio.internal;

import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * The {@link RachioBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Damion Boyett - Initial contribution
 */
public class RachioBindingConstants {

    private static final String BINDING_ID = "rachio";

    // ===================================================================
    // API ENDPOINT CONSTANTS - ADDED TO FIX COMPILATION ERRORS
    // ===================================================================

    // API Base URL and Endpoints (CRITICAL - referenced in RachioApiClient.java)
    public static final String API_BASE_URL = "https://api.rach.io/1/public";
    public static final String API_PERSON_ENDPOINT = "/person/info";
    public static final String API_DEVICE_ENDPOINT = "/device";
    public static final String API_ZONE_ENDPOINT = "/zone";
    public static final String API_WEBHOOK_ENDPOINT = "/webhook";

    // Authentication (CRITICAL - referenced in RachioHttp.java)
    public static final String RACHIO_AUTH_BEARER_PREFIX = "Bearer ";

    // Rate Limiting (CRITICAL - referenced in RachioHttp.java)
    public static final int MAX_RATE_LIMIT = 1500;
    public static final String STATUS_CRITICAL = "Critical";
    public static final String STATUS_WARNING = "Warning";
    public static final String STATUS_NORMAL = "Normal";

    // ===================================================================
    // Thing Type UIDs
    // ===================================================================
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "rachio-bridge");
    public static final ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "rachio-device");
    public static final ThingTypeUID THING_TYPE_ZONE = new ThingTypeUID(BINDING_ID, "rachio-zone");

    // ===================================================================
    // Channel IDs - COMPLETE SET
    // ===================================================================

    // Bridge channels
    public static final String CHANNEL_RATE_LIMIT_REMAINING = "rateLimitRemaining";
    public static final String CHANNEL_RATE_LIMIT_PERCENT = "rateLimitPercent";
    public static final String CHANNEL_RATE_LIMIT_STATUS = "rateLimitStatus";
    public static final String CHANNEL_RATE_LIMIT_RESET = "rateLimitReset";

    // Device channels - COMPLETE SET (referenced in RachioDeviceHandler.java)
    public static final String CHANNEL_STATUS = "status";
    public static final String CHANNEL_PAUSED = "paused";
    public static final String CHANNEL_ENABLED = "enabled";
    public static final String CHANNEL_DELETED = "deleted";
    public static final String CHANNEL_ZONES_COUNT = "zonesCount";
    public static final String CHANNEL_LAST_RUN_START = "lastRunStart";
    public static final String CHANNEL_LAST_RUN_END = "lastRunEnd";
    public static final String CHANNEL_LAST_RUN_DURATION = "lastRunDuration";
    public static final String CHANNEL_LAST_RUN_WATER = "lastRunWater";
    public static final String CHANNEL_FORECAST_TEMP = "forecastTemp";
    public static final String CHANNEL_FORECAST_PRECIP = "forecastPrecip";
    public static final String CHANNEL_FORECAST_HUMIDITY = "forecastHumidity";
    public static final String CHANNEL_FORECAST_TEMP_TOMORROW = "forecastTempTomorrow";
    public static final String CHANNEL_FORECAST_PRECIP_TOMORROW = "forecastPrecipTomorrow";
    public static final String CHANNEL_FORECAST_HUMIDITY_TOMORROW = "forecastHumidityTomorrow";
    public static final String CHANNEL_RAIN_DELAY = "rainDelay";

    // Additional device channels from original constants
    public static final String CHANNEL_DEVICE_STATUS = "status";
    public static final String CHANNEL_DEVICE_PAUSED = "paused";
    public static final String CHANNEL_DEVICE_RAIN_DELAY = "rainDelay";
    public static final String CHANNEL_DEVICE_RAIN_DELAY_EXPIRATION = "rainDelayExpiration";
    public static final String CHANNEL_DEVICE_USAGE_TOTAL = "usageTotal";
    public static final String CHANNEL_DEVICE_USAGE_START = "usageStart";
    public static final String CHANNEL_DEVICE_USAGE_END = "usageEnd";
    public static final String CHANNEL_DEVICE_USAGE_DURATION = "usageDuration";
    public static final String CHANNEL_DEVICE_USAGE_WATER = "usageWater";
    public static final String CHANNEL_DEVICE_FORECAST_TEMP = "forecastTemp";
    public static final String CHANNEL_DEVICE_FORECAST_PRECIP = "forecastPrecip";
    public static final String CHANNEL_DEVICE_FORECAST_HUMIDITY = "forecastHumidity";
    public static final String CHANNEL_DEVICE_FORECAST_WIND = "forecastWind";
    public static final String CHANNEL_DEVICE_FORECAST_SOLAR = "forecastSolar";
    public static final String CHANNEL_DEVICE_FORECAST_ET = "forecastEt";
    public static final String CHANNEL_DEVICE_SCHEDULE = "schedule";
    public static final String CHANNEL_DEVICE_ALERT = "alert";

    // PATCHED: Added missing channel constant for precipitation accumulation
    public static final String CHANNEL_DEVICE_FORECAST_PRECIP_ACCUM = "forecastPrecipAccum";

    // Zone channels - COMPLETE SET
    public static final String CHANNEL_ZONE_RUN = "zoneRun";
    public static final String CHANNEL_ZONE_ENABLED = "zoneEnabled";
    public static final String CHANNEL_ZONE_RUNTIME = "zoneRuntime";
    public static final String CHANNEL_ZONE_SOIL = "soilType";
    public static final String CHANNEL_ZONE_CROP = "cropType";
    public static final String CHANNEL_ZONE_NOZZLE = "nozzleType";
    public static final String CHANNEL_ZONE_SHADE = "shadeType";
    public static final String CHANNEL_ZONE_SLOPE = "slopeType";
    public static final String CHANNEL_ZONE_IRRIGATION_EFFICIENCY = "irrigationEfficiency";
    public static final String CHANNEL_ZONE_WATER_REQ = "waterRequirement";
    public static final String CHANNEL_ZONE_WATER_AVAILABLE = "waterAvailable";
    public static final String CHANNEL_ZONE_WATER_DEPTH = "waterDepth";
    public static final String CHANNEL_ZONE_ROOT_DEPTH = "rootDepth";
    public static final String CHANNEL_ZONE_MOISTURE = "moisture";
    public static final String CHANNEL_ZONE_USAGE_TOTAL = "zoneUsageTotal";
    public static final String CHANNEL_ZONE_USAGE_START = "zoneUsageStart";
    public static final String CHANNEL_ZONE_USAGE_END = "zoneUsageEnd";
    public static final String CHANNEL_ZONE_USAGE_DURATION = "zoneUsageDuration";
    public static final String CHANNEL_ZONE_USAGE_WATER = "zoneUsageWater";

    // Additional zone channels referenced in errors
    public static final String CHANNEL_ZONE_DURATION = "zoneDuration";
    public static final String CHANNEL_ZONE_IMAGE = "zoneImage";
    public static final String CHANNEL_ZONE_DEPLETION = "zoneDepletion";
    public static final String CHANNEL_ZONE_EFFICIENCY = "zoneEfficiency";
    public static final String CHANNEL_ZONE_AREA = "zoneArea";
    public static final String CHANNEL_ZONE_MAX_RUNTIME = "zoneMaxRuntime";
    public static final String CHANNEL_ZONE_RUNTIME_NO_MULTIPLIER = "zoneRuntimeNoMultiplier";
    public static final String CHANNEL_ZONE_SATURATED_DEPTH = "zoneSaturatedDepth";
    public static final String CHANNEL_ZONE_AVAILABLE_WATER = "zoneAvailableWater";
    public static final String CHANNEL_ZONE_LAST_RUN_START = "zoneLastRunStart";
    public static final String CHANNEL_ZONE_LAST_RUN_END = "zoneLastRunEnd";
    public static final String CHANNEL_ZONE_LAST_RUN_DURATION = "zoneLastRunDuration";
    public static final String CHANNEL_ZONE_LAST_RUN_WATER = "zoneLastRunWater";

    // ===================================================================
    // Channel Type UIDs
    // ===================================================================
    public static final ChannelTypeUID CHANNEL_TYPE_RATE_LIMIT_REMAINING = new ChannelTypeUID(BINDING_ID,
            CHANNEL_RATE_LIMIT_REMAINING);
    public static final ChannelTypeUID CHANNEL_TYPE_RATE_LIMIT_PERCENT = new ChannelTypeUID(BINDING_ID,
            CHANNEL_RATE_LIMIT_PERCENT);
    public static final ChannelTypeUID CHANNEL_TYPE_RATE_LIMIT_STATUS = new ChannelTypeUID(BINDING_ID,
            CHANNEL_RATE_LIMIT_STATUS);
    public static final ChannelTypeUID CHANNEL_TYPE_RATE_LIMIT_RESET = new ChannelTypeUID(BINDING_ID,
            CHANNEL_RATE_LIMIT_RESET);
    public static final ChannelTypeUID CHANNEL_TYPE_DEVICE_STATUS = new ChannelTypeUID(BINDING_ID,
            CHANNEL_DEVICE_STATUS);
    public static final ChannelTypeUID CHANNEL_TYPE_DEVICE_PAUSED = new ChannelTypeUID(BINDING_ID,
            CHANNEL_DEVICE_PAUSED);
    public static final ChannelTypeUID CHANNEL_TYPE_DEVICE_RAIN_DELAY = new ChannelTypeUID(BINDING_ID,
            CHANNEL_DEVICE_RAIN_DELAY);
    public static final ChannelTypeUID CHANNEL_TYPE_ZONE_RUN = new ChannelTypeUID(BINDING_ID, CHANNEL_ZONE_RUN);
    public static final ChannelTypeUID CHANNEL_TYPE_ZONE_ENABLED = new ChannelTypeUID(BINDING_ID, CHANNEL_ZONE_ENABLED);
    public static final ChannelTypeUID CHANNEL_TYPE_ZONE_RUNTIME = new ChannelTypeUID(BINDING_ID, CHANNEL_ZONE_RUNTIME);
    public static final ChannelTypeUID CHANNEL_TYPE_ZONE_SOIL = new ChannelTypeUID(BINDING_ID, CHANNEL_ZONE_SOIL);
    public static final ChannelTypeUID CHANNEL_TYPE_ZONE_CROP = new ChannelTypeUID(BINDING_ID, CHANNEL_ZONE_CROP);
    public static final ChannelTypeUID CHANNEL_TYPE_ZONE_NOZZLE = new ChannelTypeUID(BINDING_ID, CHANNEL_ZONE_NOZZLE);
    public static final ChannelTypeUID CHANNEL_TYPE_ZONE_IRRIGATION_EFFICIENCY = new ChannelTypeUID(BINDING_ID,
            CHANNEL_ZONE_IRRIGATION_EFFICIENCY);

    // PATCHED: Added channel type for precipitation accumulation
    public static final ChannelTypeUID CHANNEL_TYPE_DEVICE_FORECAST_PRECIP_ACCUM = new ChannelTypeUID(BINDING_ID,
            CHANNEL_DEVICE_FORECAST_PRECIP_ACCUM);

    // ===================================================================
    // Discovery Property Constants
    // ===================================================================
    public static final String DEVICE_PROP_ID = "deviceId";
    public static final String DEVICE_PROP_NAME = "deviceName";
    public static final String DEVICE_PROP_STATUS = "deviceStatus";
    public static final String DEVICE_PROP_SERIAL = "deviceSerial";
    public static final String DEVICE_PROP_MODEL = "deviceModel";
    public static final String DEVICE_PROP_MAC = "deviceMac";
    public static final String DEVICE_PROP_ZONECOUNT = "zoneCount";

    public static final String ZONE_PROP_ID = "zoneId";
    public static final String ZONE_PROP_NAME = "zoneName";
    public static final String ZONE_PROP_NUMBER = "zoneNumber";
    public static final String ZONE_PROP_ENABLED = "zoneEnabled";
    public static final String ZONE_PROP_SOIL = "zoneSoil";
    public static final String ZONE_PROP_CROP = "zoneCrop";
    public static final String ZONE_PROP_NOZZLE = "zoneNozzle";
    public static final String ZONE_PROP_EFFICIENCY = "zoneEfficiency";

    // ===================================================================
    // Thing Property Constants - COMPLETE SET
    // ===================================================================
    public static final String PROPERTY_ID = "id";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_DEVICE_ID = "deviceId";
    public static final String PROPERTY_ZONE_ID = "zoneId";
    public static final String PROPERTY_ZONE_NUMBER = "zoneNumber";
    public static final String PROPERTY_SOIL_TYPE = "soilType";
    public static final String PROPERTY_CROP_TYPE = "cropType";
    public static final String PROPERTY_NOZZLE_TYPE = "nozzleType";
    public static final String PROPERTY_IRRIGATION_EFFICIENCY = "irrigationEfficiency";

    // Additional property constants referenced in errors
    public static final String PROPERTY_STATUS = "status";
    public static final String PROPERTY_PAUSED = "paused";
    public static final String PROPERTY_ENABLED = "enabled";
    public static final String PROPERTY_MODEL = "model";
    public static final String PROPERTY_SERIAL = "serial";
    public static final String PROPERTY_DEVICE_NAME = "deviceName";
    public static final String PROPERTY_CREATED_DATE = "createdDate";

    // ===================================================================
    // Configuration Parameters
    // ===================================================================
    public static final String CONFIG_API_KEY = "apiKey";
    public static final String CONFIG_POLLING_INTERVAL = "pollingInterval";
    public static final String CONFIG_IP_FILTER = "ipFilter";
    public static final String CONFIG_USE_AWS_RANGES = "useAwsRanges";
    public static final String CONFIG_DEVICE_ID = "deviceId";
    public static final String CONFIG_ZONE_ID = "zoneId";

    // ===================================================================
    // Webhook Constants
    // ===================================================================
    public static final String WEBHOOK_URL = "/rachio/webhook";
    public static final String IMAGE_URL = "/rachio/image";
    public static final String WEBHOOK_CALLBACK_PATH = "callback";
    public static final String WEBHOOK_EXTERNAL_PORT = "externalPort";
    public static final String WEBHOOK_ID = "webhookId";

    // ===================================================================
    // Security Constants
    // ===================================================================
    public static final String HEADER_SIGNATURE = "X-Rachio-Signature";
    public static final String HEADER_EVENT_TYPE = "X-Rachio-Event";

    // ===================================================================
    // Rate Limiting Headers
    // ===================================================================
    public static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset";

    // ===================================================================
    // Status Constants
    // ===================================================================
    public static final String STATUS_ONLINE = "ONLINE";
    public static final String STATUS_OFFLINE = "OFFLINE";
    public static final String STATUS_SLEEP = "SLEEP";

    // ===================================================================
    // Rate Limit Status Levels
    // ===================================================================
    public static final String RATE_LIMIT_NORMAL = "Normal";
    public static final String RATE_LIMIT_WARNING = "Warning";
    public static final String RATE_LIMIT_CRITICAL = "Critical";

    // ===================================================================
    // SI Units
    // ===================================================================
    public static final String MILLIMETRE = "mm";
    public static final String METRE = "m";
    public static final String LITRE = "L";
    public static final String CELSIUS = "°C";
    public static final String PERCENT = "%";
    public static final String SECOND = "s";
    public static final String MINUTE = "min";
    public static final String HOUR = "h";

    // ===================================================================
    // Unit Instances for Type-Safe Usage
    // ===================================================================
    public static final javax.measure.Unit<?> MILLIMETRE_UNIT = SIUnits.METRE.divide(1000);
    public static final javax.measure.Unit<?> METRE_UNIT = SIUnits.METRE;
    public static final javax.measure.Unit<?> LITRE_UNIT = Units.LITRE;
    public static final javax.measure.Unit<?> CELSIUS_UNIT = SIUnits.CELSIUS;

    // ===================================================================
    // Event Types
    // ===================================================================
    public static final String EVENT_ZONE_STARTED = "ZONE_STARTED";
    public static final String EVENT_ZONE_STOPPED = "ZONE_STOPPED";
    public static final String EVENT_ZONE_COMPLETED = "ZONE_COMPLETED";
    public static final String EVENT_ZONE_CYCLED = "ZONE_CYCLED";
    public static final String EVENT_ZONE_PAUSED = "ZONE_PAUSED";
    public static final String EVENT_RAIN_DELAY = "RAIN_DELAY";
    public static final String EVENT_RAIN_SENSOR_DETECTION = "RAIN_SENSOR_DETECTION";
    public static final String EVENT_DEVICE_CONNECTED = "DEVICE_CONNECTED";
    public static final String EVENT_DEVICE_DISCONNECTED = "DEVICE_DISCONNECTED";

    // ===================================================================
    // Default Values
    // ===================================================================
    public static final int DEFAULT_POLLING_INTERVAL = 120; // seconds
    public static final int DEFAULT_WEBHOOK_PORT = 8080;
    public static final int DEFAULT_RATE_LIMIT_WARNING = 150; // 10% of 1500
    public static final int DEFAULT_RATE_LIMIT_CRITICAL = 30; // 2% of 1500

    // ===================================================================
    // API URLs (Legacy - keeping for compatibility)
    // ===================================================================
    public static final String RACHIO_API_URL = "https://api.rach.io/1/public";
    public static final String RACHIO_PERSON_URL = RACHIO_API_URL + "/person/info";
    public static final String RACHIO_DEVICE_URL = RACHIO_API_URL + "/device";
    public static final String RACHIO_ZONE_URL = RACHIO_API_URL + "/zone";
    public static final String RACHIO_WEBHOOK_URL = RACHIO_API_URL + "/webhook";
    public static final String RACHIO_SCHEDULE_URL = RACHIO_API_URL + "/schedulerule";

    // ===================================================================
    // Listener Types for RachioStatusListener
    // ===================================================================
    public static final String LISTENER_TYPE_BRIDGE = "bridge";
    public static final String LISTENER_TYPE_DEVICE = "device";
    public static final String LISTENER_TYPE_ZONE = "zone";
    public static final String LISTENER_TYPE_DISCOVERY = "discovery";
}
