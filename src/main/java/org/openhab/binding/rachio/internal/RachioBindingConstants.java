package org.openhab.binding.rachio.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link RachioBindingConstants} class defines common constants, which are
 * used across the whole binding.
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

    // Bridge properties
    public static final String PROPERTY_PERSON_ID = "personId";
    public static final String PROPERTY_PERSON_NAME = "personName";
    public static final String PROPERTY_PERSON_EMAIL = "personEmail";
    public static final String PROPERTY_PERSON_USERNAME = "personUsername";

    // Device properties
    public static final String PROPERTY_DEVICE_ID = "deviceId";
    public static final String PROPERTY_DEVICE_NAME = "deviceName";
    public static final String PROPERTY_DEVICE_MODEL = "deviceModel";
    public static final String PROPERTY_DEVICE_SERIAL = "serialNumber";
    public static final String PROPERTY_DEVICE_STATUS = "status";
    public static final String PROPERTY_DEVICE_LATITUDE = "latitude";
    public static final String PROPERTY_DEVICE_LONGITUDE = "longitude";
    public static final String PROPERTY_DEVICE_TIMEZONE = "timeZone";
    public static final String PROPERTY_DEVICE_MAC_ADDRESS = "macAddress";
    public static final String PROPERTY_DEVICE_ELEVATION = "elevation";
    public static final String PROPERTY_DEVICE_FLEX_SCHEDULES = "flexScheduleRules";

    // Zone properties
    public static final String PROPERTY_ZONE_ID = "zoneId";
    public static final String PROPERTY_ZONE_NUMBER = "zoneNumber";
    public static final String PROPERTY_ZONE_NAME = "zoneName";
    public static final String PROPERTY_ZONE_ENABLED = "enabled";
    public static final String PROPERTY_ZONE_RUNTIME = "runtime";
    public static final String PROPERTY_ZONE_MAX_RUNTIME = "maxRuntime";
    public static final String PROPERTY_ZONE_AREA = "area";
    public static final String PROPERTY_ZONE_SOIL_TYPE = "soilType";
    public static final String PROPERTY_ZONE_CROP_TYPE = "cropType";
    public static final String PROPERTY_ZONE_NOZZLE_TYPE = "nozzleType";
    public static final String PROPERTY_ZONE_SLOPE_TYPE = "slopeType";
    public static final String PROPERTY_ZONE_SHADE_TYPE = "shadeType";
    public static final String PROPERTY_ZONE_ROOT_DEPTH = "rootZoneDepth";
    public static final String PROPERTY_ZONE_EFFICIENCY = "efficiency";
    public static final String PROPERTY_ZONE_AVAILABLE_WATER = "availableWater";
    public static final String PROPERTY_ADJUSTMENT_LEVEL_PREFIX = "adjustmentLevel";

    // Bridge channels
    public static final String CHANNEL_RATE_LIMIT_REMAINING = "rateLimitRemaining";
    public static final String CHANNEL_RATE_LIMIT_PERCENT = "rateLimitPercent";
    public static final String CHANNEL_RATE_LIMIT_STATUS = "rateLimitStatus";
    public static final String CHANNEL_RATE_LIMIT_RESET = "rateLimitReset";

    // Device channels
    public static final String CHANNEL_DEVICE_ONLINE = "online";
    public static final String CHANNEL_DEVICE_STATUS = "status";
    public static final String CHANNEL_WATERING = "watering";
    public static final String CHANNEL_RAIN_DELAY = "rainDelay";
    public static final String CHANNEL_LAST_EVENT_TIME = "lastEventTime";
    public static final String CHANNEL_DEVICE_TOTAL_AREA = "totalArea";
    public static final String CHANNEL_DEVICE_ELEVATION = "elevation";
    public static final String CHANNEL_DEVICE_FLEX_SCHEDULES = "flexSchedules";

    // Zone channels
    public static final String CHANNEL_START_ZONE = "startZone";
    public static final String CHANNEL_STOP_ZONE = "stopZone";
    public static final String CHANNEL_ZONE_ENABLED = "enabled";
    public static final String CHANNEL_ZONE_RUNTIME = "runtime";
    public static final String CHANNEL_ZONE_MAX_RUNTIME = "maxRuntime";
    public static final String CHANNEL_ZONE_AREA = "area";
    public static final String CHANNEL_SOIL_TYPE = "soilType";
    public static final String CHANNEL_CROP_TYPE = "cropType";
    public static final String CHANNEL_NOZZLE_TYPE = "nozzleType";
    public static final String CHANNEL_SLOPE_TYPE = "slopeType";
    public static final String CHANNEL_SHADE_TYPE = "shadeType";
    public static final String CHANNEL_ROOT_DEPTH = "rootDepth";
    public static final String CHANNEL_IRRIGATION_EFFICIENCY = "irrigationEfficiency";
    public static final String CHANNEL_SOIL_AVAILABLE_WATER = "soilAvailableWater";
    public static final String CHANNEL_CROP_COEFFICIENT = "cropCoefficient";
    public static final String CHANNEL_NOZZLE_RATE = "nozzleRate";

    // Weather forecast channels
    public static final String CHANNEL_FORECAST_TEMPERATURE = "forecastTemperature";
    public static final String CHANNEL_FORECAST_PRECIPITATION = "forecastPrecipitation";
    public static final String CHANNEL_FORECAST_HUMIDITY = "forecastHumidity";
    public static final String CHANNEL_FORECAST_WIND_SPEED = "forecastWindSpeed";
    public static final String CHANNEL_FORECAST_ICON = "forecastIcon";

    // Configuration parameters
    public static final String CONFIG_API_KEY = "apiKey";
    public static final String CONFIG_REFRESH_INTERVAL = "refreshInterval";
    public static final String CONFIG_WEBHOOK_ENABLED = "webhookEnabled";
    public static final String CONFIG_WEBHOOK_URL = "webhookUrl";
    public static final String CONFIG_WEBHOOK_SECRET = "webhookSecret";
    public static final String CONFIG_ALLOWED_IP_RANGES = "allowedIpRanges";
    public static final String CONFIG_WEATHER_ENABLED = "weatherEnabled";
    public static final String CONFIG_WEBHOOK_CHECK_INTERVAL = "webhookCheckInterval";

    // API constants
    public static final String API_BASE_URL = "https://api.rach.io/1/public";
    public static final int DEFAULT_REFRESH_INTERVAL = 300; // 5 minutes
    public static final int DEFAULT_WEBHOOK_CHECK_INTERVAL = 60; // 1 hour
    public static final int DEFAULT_RATE_LIMIT = 14400; // 14400 calls per day

    // Webhook event types
    public static final String EVENT_TYPE_ZONE_STATUS = "ZONE_STATUS";
    public static final String EVENT_TYPE_DEVICE_STATUS = "DEVICE_STATUS";
    public static final String EVENT_TYPE_RAIN_DELAY = "RAIN_DELAY";
    public static final String EVENT_TYPE_WEATHER_INTEL = "WEATHER_INTEL";
    public static final String EVENT_TYPE_WATER_BUDGET = "WATER_BUDGET";
    public static final String EVENT_TYPE_SCHEDULE_STATUS = "SCHEDULE_STATUS";
    public static final String EVENT_TYPE_RAIN_SENSOR = "RAIN_SENSOR";

    // Event subtypes
    public static final String SUBTYPE_STARTED = "STARTED";
    public static final String SUBTYPE_STOPPED = "STOPPED";
    public static final String SUBTYPE_COMPLETED = "COMPLETED";
    public static final String SUBTYPE_ONLINE = "ONLINE";
    public static final String SUBTYPE_OFFLINE = "OFFLINE";
    public static final String SUBTYPE_SLEEP = "SLEEP_MODE";

    // HTTP headers
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset";

    // Units
    public static final String UNIT_SECONDS = "s";
    public static final String UNIT_MINUTES = "min";
    public static final String UNIT_PERCENT = "%";
    public static final String UNIT_SQUARE_FEET = "ft²";
    public static final String UNIT_INCHES = "in";
    public static final String UNIT_INCHES_PER_HOUR = "in/h";
}
