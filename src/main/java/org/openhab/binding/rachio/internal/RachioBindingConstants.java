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

    // Binding ID
    public static final String BINDING_ID = "rachio";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "device");
    public static final ThingTypeUID THING_TYPE_ZONE = new ThingTypeUID(BINDING_ID, "zone");

    // String constants for Thing Types
    public static final String BRIDGE_TYPE = "rachioBridge";
    public static final String DEVICE_TYPE = "rachioDevice";
    public static final String ZONE_TYPE = "rachioZone";

    // List of all Channel ids
    // Bridge Channels
    public static final String CHANNEL_RATE_LIMIT_REMAINING = "rateLimitRemaining";
    public static final String CHANNEL_RATE_LIMIT_PERCENT = "rateLimitPercent";
    public static final String CHANNEL_RATE_LIMIT_STATUS = "rateLimitStatus";
    public static final String CHANNEL_RATE_LIMIT_RESET = "rateLimitReset";
    public static final String CHANNEL_WEBHOOK_HEALTH = "webhookHealth";
    public static final String CHANNEL_CACHE_STATUS = "cacheStatus";
    
    // Device Channels
    public static final String CHANNEL_DEVICE_STATUS = "deviceStatus";
    public static final String CHANNEL_DEVICE_ONLINE = "online";
    public static final String CHANNEL_DEVICE_LAST_HEARD = "lastHeardFrom";
    public static final String CHANNEL_DEVICE_ZONES = "deviceZones";
    public static final String CHANNEL_RAIN_DELAY = "rainDelay";
    public static final String CHANNEL_RAIN_DELAY_SET = "rainDelaySet";
    public static final String CHANNEL_DEVICE_PAUSE = "devicePause";
    
    // Weather Channels
    public static final String CHANNEL_TEMPERATURE = "temperature";
    public static final String CHANNEL_PRECIPITATION = "precipitation";
    public static final String CHANNEL_EVAPOTRANSPIRATION = "evapotranspiration";
    public static final String CHANNEL_WIND_SPEED = "windSpeed";
    public static final String CHANNEL_HUMIDITY = "humidity";
    public static final String CHANNEL_SOLAR_RADIATION = "solarRadiation";
    
    // Water Usage Channels
    public static final String CHANNEL_WATER_USAGE = "waterUsage";
    public static final String CHANNEL_WATER_SAVINGS = "waterSavings";
    public static final String CHANNEL_CARBON_SAVINGS = "carbonSavings";
    public static final String CHANNEL_MONEY_SAVINGS = "moneySavings";
    
    // Zone Control Channels
    public static final String CHANNEL_ZONE_RUN = "run";
    public static final String CHANNEL_ZONE_STOP = "stop";
    public static final String CHANNEL_ZONE_ENABLED = "enabled";
    public static final String CHANNEL_ZONE_STATUS = "zoneStatus";
    public static final String CHANNEL_ZONE_DURATION = "duration";
    public static final String CHANNEL_ZONE_NEXT_RUN = "nextRun";
    
    // Professional Irrigation Data Channels
    public static final String CHANNEL_SOIL_TYPE = "soilType";
    public static final String CHANNEL_SOIL_AVAILABLE_WATER = "soilAvailableWater";
    public static final String CHANNEL_CROP_TYPE = "cropType";
    public static final String CHANNEL_CROP_COEFFICIENT = "cropCoefficient";
    public static final String CHANNEL_NOZZLE_TYPE = "nozzleType";
    public static final String CHANNEL_NOZZLE_RATE = "nozzleRate";
    public static final String CHANNEL_SLOPE_TYPE = "slopeType";
    public static final String CHANNEL_SHADE_TYPE = "shadeType";
    public static final String CHANNEL_ROOT_DEPTH = "rootDepth";
    public static final String CHANNEL_IRRIGATION_EFFICIENCY = "irrigationEfficiency";
    public static final String CHANNEL_ADJUSTMENT_LEVEL1 = "adjustmentLevel1";
    public static final String CHANNEL_ADJUSTMENT_LEVEL2 = "adjustmentLevel2";
    public static final String CHANNEL_ADJUSTMENT_LEVEL3 = "adjustmentLevel3";
    public static final String CHANNEL_ADJUSTMENT_LEVEL4 = "adjustmentLevel4";
    public static final String CHANNEL_ADJUSTMENT_LEVEL5 = "adjustmentLevel5";
    public static final String CHANNEL_ZONE_AREA = "zoneArea";
    
    // Schedule Channels
    public static final String CHANNEL_SCHEDULE_STATUS = "scheduleStatus";
    public static final String CHANNEL_SCHEDULE_NEXT_RUN = "scheduleNextRun";
    public static final String CHANNEL_SCHEDULE_TOTAL_DURATION = "scheduleTotalDuration";
    
    // Alert Channels
    public static final String CHANNEL_ALERT_STATUS = "alertStatus";
    public static final String CHANNEL_ALERT_MESSAGE = "alertMessage";
    public static final String CHANNEL_ALERT_TYPE = "alertType";
    public static final String CHANNEL_ALERT_TIMESTAMP = "alertTimestamp";

    // Configuration properties
    public static final String CONFIG_API_KEY = "apiKey";
    public static final String CONFIG_REFRESH_INTERVAL = "refreshInterval";
    public static final String CONFIG_RATE_LIMIT_WARNING = "rateLimitWarning";
    public static final String CONFIG_WEBHOOK_ENABLED = "webhookEnabled";
    public static final String CONFIG_WEBHOOK_SECRET = "webhookSecret";
    public static final String CONFIG_IP_FILTER_ENABLED = "ipFilterEnabled";
    public static final String CONFIG_IP_FILTER_LIST = "ipFilterList";
    public static final String CONFIG_AWS_IP_RANGES = "awsIpRanges";
    public static final String CONFIG_DEVICE_ID = "deviceId";
    public static final String CONFIG_ZONE_ID = "zoneId";
    public static final String CONFIG_ZONE_RUNTIME = "zoneRuntime";
    
    // Webhook properties
    public static final String PROPERTY_WEBHOOK_ID = "webhookId";
    public static final String PROPERTY_WEBHOOK_URL = "webhookUrl";
    public static final String PROPERTY_WEBHOOK_EXTERNAL_URL = "webhookExternalUrl";
    
    // Event types
    public static final String EVENT_ZONE_STATUS = "ZONE_STATUS";
    public static final String EVENT_DEVICE_STATUS = "DEVICE_STATUS";
    public static final String EVENT_RAIN_DELAY = "RAIN_DELAY";
    public static final String EVENT_WEATHER_INTEL = "WEATHER_INTEL";
    public static final String EVENT_WATER_BUDGET = "WATER_BUDGET";
    public static final String EVENT_SCHEDULE_STATUS = "SCHEDULE_STATUS";
    public static final String EVENT_RAIN_SENSOR = "RAIN_SENSOR";
    
    // Rate limit constants
    public static final int RATE_LIMIT_MAX_REQUESTS = 1700;
    public static final String RATE_LIMIT_HEADER_REMAINING = "X-RateLimit-Remaining";
    public static final String RATE_LIMIT_HEADER_LIMIT = "X-RateLimit-Limit";
    public static final String RATE_LIMIT_HEADER_RESET = "X-RateLimit-Reset";
    
    // API constants
    public static final String API_BASE_URL = "https://api.rach.io/1/public";
    public static final String API_PERSON_INFO = "/person/info";
    public static final String API_DEVICE_LIST = "/device";
    public static final String API_DEVICE = "/device/{id}";
    public static final String API_ZONE_START = "/zone/start";
    public static final String API_ZONE_STOP = "/zone/stop";
    public static final String API_ZONE_RUN_ALL = "/zone/start_all";
    public static final String API_ZONE_RUN_NEXT = "/zone/start_next";
    public static final String API_RAIN_DELAY = "/device/rain_delay";
    public static final String API_PAUSE_DEVICE = "/device/pause";
    public static final String API_ZONE = "/zone/{id}";
    public static final String API_ZONE_WATERING = "/zone/{id}/watering";
    public static final String API_DEVICE_FORECAST = "/device/{id}/forecast";
    public static final String API_DEVICE_USAGE = "/device/{id}/usage";
    public static final String API_DEVICE_SAVINGS = "/device/{id}/savings";
    public static final String API_DEVICE_ALERTS = "/device/{id}/alerts";
    public static final String API_SCHEDULE = "/schedule";
    public static final String API_WEBHOOK = "/webhook";
    
    // Servlet constants
    public static final String SERVLET_WEBHOOK_PATH = "/rachio/webhook";
    public static final String SERVLET_IMAGE_PATH = "/rachio/image";
    
    // Thing properties
    public static final String PROPERTY_DEVICE_ID = "deviceId";
    public static final String PROPERTY_ZONE_ID = "zoneId";
    public static final String PROPERTY_ZONE_NAME = "zoneName";
    public static final String PROPERTY_DEVICE_NAME = "deviceName";
    public static final String PROPERTY_DEVICE_MODEL = "deviceModel";
    public static final String PROPERTY_DEVICE_SERIAL = "deviceSerial";
    public static final String PROPERTY_ZONE_NUMBER = "zoneNumber";
    public static final String PROPERTY_ZONE_RUNTIME = "zoneRuntime";
    public static final String PROPERTY_ZONE_ENABLED = "zoneEnabled";
    
    // Default values
    public static final int DEFAULT_REFRESH_INTERVAL = 60; // seconds
    public static final int DEFAULT_RATE_LIMIT_WARNING = 100; // requests
    public static final boolean DEFAULT_WEBHOOK_ENABLED = true;
    public static final boolean DEFAULT_IP_FILTER_ENABLED = true;
    public static final boolean DEFAULT_AWS_IP_RANGES = true;
    
    // Status constants
    public static final String STATUS_OK = "OK";
    public static final String STATUS_WARNING = "WARNING";
    public static final String STATUS_CRITICAL = "CRITICAL";
    public static final String STATUS_OFFLINE = "OFFLINE";
    public static final String STATUS_ONLINE = "ONLINE";
    public static final String STATUS_SLEEP = "SLEEP";
    
    // Unit constants
    public static final String UNIT_SECONDS = "s";
    public static final String UNIT_MINUTES = "min";
    public static final String UNIT_HOURS = "h";
    public static final String UNIT_DAYS = "d";
    public static final String UNIT_PERCENT = "%";
    public static final String UNIT_INCHES = "in";
    public static final String UNIT_FAHRENHEIT = "°F";
    public static final String UNIT_CELSIUS = "°C";
    public static final String UNIT_CUBIC_METERS = "m³";
    public static final String UNIT_GALLONS = "gal";
    public static final String UNIT_SQUARE_FEET = "ft²";
    public static final String UNIT_SQUARE_METERS = "m²";
    
    // Logging constants
    public static final String LOGGER_API = "org.openhab.binding.rachio.api";
    public static final String LOGGER_WEBHOOK = "org.openhab.binding.rachio.webhook";
    public static final String LOGGER_HANDLER = "org.openhab.binding.rachio.handler";
    public static final String LOGGER_DISCOVERY = "org.openhab.binding.rachio.discovery";
    
    // Cache constants
    public static final long CACHE_DURATION_DEVICES = 300000; // 5 minutes in milliseconds
    public static final long CACHE_DURATION_ZONES = 300000; // 5 minutes
    public static final long CACHE_DURATION_FORECAST = 3600000; // 1 hour
    public static final long CACHE_DURATION_USAGE = 86400000; // 24 hours
    
    // Security constants
    public static final String HMAC_ALGORITHM = "HmacSHA256";
    public static final String HEADER_SIGNATURE = "X-Rachio-Signature";
    public static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    
    // Error messages
    public static final String ERROR_API_KEY_MISSING = "API key is missing";
    public static final String ERROR_DEVICE_NOT_FOUND = "Device not found";
    public static final String ERROR_ZONE_NOT_FOUND = "Zone not found";
    public static final String ERROR_RATE_LIMIT_EXCEEDED = "Rate limit exceeded";
    public static final String ERROR_WEBHOOK_VALIDATION = "Webhook signature validation failed";
    public static final String ERROR_IP_NOT_ALLOWED = "IP address not allowed";
}
