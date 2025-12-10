package org.openhab.binding.rachio.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

import java.util.Set;

/**
 * The {@link RachioBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioBindingConstants {

    public static final String BINDING_ID = "rachio";

    // Bridge
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");

    // Device
    public static final ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "device");

    // Zone
    public static final ThingTypeUID THING_TYPE_ZONE = new ThingTypeUID(BINDING_ID, "zone");

    // Channel groups
    public static final String CHANNEL_GROUP_ZONE_PROPERTIES = "zoneProperties";
    public static final String CHANNEL_GROUP_DEVICE_STATUS = "deviceStatus";
    public static final String CHANNEL_GROUP_ZONE_CONTROL = "zoneControl";
    public static final String CHANNEL_GROUP_WEBHOOK = "webhook";
    public static final String CHANNEL_GROUP_RATE_LIMIT = "rateLimit";

    // Bridge configuration properties
    public static final String CONFIG_API_KEY = "apiKey";
    public static final String CONFIG_WEBHOOK_ID = "webhookId";
    public static final String CONFIG_WEBHOOK_SECRET = "webhookSecret";
    public static final String CONFIG_WEBHOOK_URL = "webhookUrl";
    public static final String CONFIG_REFRESH = "refresh";
    public static final String CONFIG_PERSON_ID = "personId";

    // Device configuration properties
    public static final String CONFIG_DEVICE_ID = "deviceId";
    public static final String CONFIG_DEVICE_NAME = "deviceName";

    // Zone configuration properties
    public static final String CONFIG_ZONE_ID = "zoneId";
    public static final String CONFIG_ZONE_NAME = "zoneName";
    public static final String CONFIG_ZONE_NUMBER = "zoneNumber";
    public static final String CONFIG_DEFAULT_DURATION = "defaultDuration";

    // Thing properties
    public static final String PROPERTY_SERIAL_NUMBER = "serialNumber";
    public static final String PROPERTY_MAC_ADDRESS = "macAddress";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_ID = "id";
    public static final String PROPERTY_MODEL = "model";
    public static final String PROPERTY_STATUS = "status";
    public static final String PROPERTY_ZONE_COUNT = "zoneCount";
    public static final String PROPERTY_SOIL_TYPE = "soilType";
    public static final String PROPERTY_CROP_TYPE = "cropType";
    public static final String PROPERTY_NOZZLE_TYPE = "nozzleType";
    public static final String PROPERTY_SLOPE_TYPE = "slopeType";
    public static final String PROPERTY_SHADE_TYPE = "shadeType";

    // Device channels
    public static final String CHANNEL_DEVICE_STATUS = "device-status";
    public static final String CHANNEL_RAIN_DELAY = "rain-delay";
    public static final String CHANNEL_ONLINE = "online";
    public static final String CHANNEL_PAUSE = "pause";
    public static final String CHANNEL_WATER_BUDGET = "water-budget";
    public static final String CHANNEL_WEATHER_SKIP = "weather-skip";
    public static final String CHANNEL_SCHEDULE = "schedule";
    public static final String CHANNEL_RAIN_SENSOR = "rain-sensor";

    // Zone channels
    public static final String CHANNEL_ZONE_STATUS = "zone-status";
    public static final String CHANNEL_ZONE_ENABLED = "zone-enabled";
    public static final String CHANNEL_ZONE_RUNTIME = "zone-runtime";
    public static final String CHANNEL_ZONE_START = "zone-start";
    public static final String CHANNEL_ZONE_STOP = "zone-stop";
    public static final String CHANNEL_ZONE_DURATION = "zone-duration";
    public static final String CHANNEL_ZONE_NEXT = "zone-next";
    public static final String CHANNEL_ZONE_ALL = "zone-all";

    // Zone property channels
    public static final String CHANNEL_SOIL_TYPE = "soil-type";
    public static final String CHANNEL_SOIL_AVAILABLE_WATER = "soil-available-water";
    public static final String CHANNEL_CROP_TYPE = "crop-type";
    public static final String CHANNEL_CROP_COEFFICIENT = "crop-coefficient";
    public static final String CHANNEL_NOZZLE_TYPE = "nozzle-type";
    public static final String CHANNEL_NOZZLE_RATE = "nozzle-rate";
    public static final String CHANNEL_SLOPE_TYPE = "slope-type";
    public static final String CHANNEL_SHADE_TYPE = "shade-type";
    public static final String CHANNEL_ROOT_DEPTH = "root-depth";
    public static final String CHANNEL_IRRIGATION_EFFICIENCY = "irrigation-efficiency";
    public static final String CHANNEL_WATER_ADJUSTMENT_1 = "water-adjustment-1";
    public static final String CHANNEL_WATER_ADJUSTMENT_2 = "water-adjustment-2";
    public static final String CHANNEL_WATER_ADJUSTMENT_3 = "water-adjustment-3";
    public static final String CHANNEL_WATER_ADJUSTMENT_4 = "water-adjustment-4";
    public static final String CHANNEL_WATER_ADJUSTMENT_5 = "water-adjustment-5";
    public static final String CHANNEL_ZONE_AREA = "zone-area";

    // Professional irrigation data channels
    public static final String CHANNEL_WATERING_HISTORY = "watering-history";
    public static final String CHANNEL_SCHEDULE_MANAGEMENT = "schedule-management";
    public static final String CHANNEL_WEATHER_FORECAST = "weather-forecast";
    public static final String CHANNEL_USAGE_ANALYTICS = "usage-analytics";
    public static final String CHANNEL_SAVINGS_DATA = "savings-data";
    public static final String CHANNEL_DEVICE_PAUSE = "device-pause";
    public static final String CHANNEL_ALERTS = "alerts";

    // Rate limiting channels
    public static final String CHANNEL_RATE_LIMIT_REMAINING = "rate-limit-remaining";
    public static final String CHANNEL_RATE_LIMIT_PERCENTAGE = "rate-limit-percentage";
    public static final String CHANNEL_RATE_LIMIT_PERCENT = "rate-limit-percent";  // ADDED - Missing constant
    public static final String CHANNEL_RATE_LIMIT_STATUS = "rate-limit-status";
    public static final String CHANNEL_RATE_LIMIT_RESET = "rate-limit-reset";

    // Webhook channels
    public static final String CHANNEL_WEBHOOK_STATUS = "webhook-status";
    public static final String CHANNEL_WEBHOOK_EVENTS = "webhook-events";
    public static final String CHANNEL_WEBHOOK_HEALTH = "webhook-health";

    // List of all channel IDs
    public static final String[] ZONE_CHANNEL_IDS = {
            CHANNEL_ZONE_STATUS,
            CHANNEL_ZONE_ENABLED,
            CHANNEL_ZONE_RUNTIME,
            CHANNEL_ZONE_START,
            CHANNEL_ZONE_STOP,
            CHANNEL_ZONE_DURATION,
            CHANNEL_ZONE_NEXT,
            CHANNEL_ZONE_ALL,
            CHANNEL_SOIL_TYPE,
            CHANNEL_SOIL_AVAILABLE_WATER,
            CHANNEL_CROP_TYPE,
            CHANNEL_CROP_COEFFICIENT,
            CHANNEL_NOZZLE_TYPE,
            CHANNEL_NOZZLE_RATE,
            CHANNEL_SLOPE_TYPE,
            CHANNEL_SHADE_TYPE,
            CHANNEL_ROOT_DEPTH,
            CHANNEL_IRRIGATION_EFFICIENCY,
            CHANNEL_WATER_ADJUSTMENT_1,
            CHANNEL_WATER_ADJUSTMENT_2,
            CHANNEL_WATER_ADJUSTMENT_3,
            CHANNEL_WATER_ADJUSTMENT_4,
            CHANNEL_WATER_ADJUSTMENT_5,
            CHANNEL_ZONE_AREA
    };

    public static final String[] DEVICE_CHANNEL_IDS = {
            CHANNEL_DEVICE_STATUS,
            CHANNEL_RAIN_DELAY,
            CHANNEL_ONLINE,
            CHANNEL_PAUSE,
            CHANNEL_WATER_BUDGET,
            CHANNEL_WEATHER_SKIP,
            CHANNEL_SCHEDULE,
            CHANNEL_RAIN_SENSOR,
            CHANNEL_WATERING_HISTORY,
            CHANNEL_SCHEDULE_MANAGEMENT,
            CHANNEL_WEATHER_FORECAST,
            CHANNEL_USAGE_ANALYTICS,
            CHANNEL_SAVINGS_DATA,
            CHANNEL_DEVICE_PAUSE,
            CHANNEL_ALERTS,
            CHANNEL_RATE_LIMIT_REMAINING,
            CHANNEL_RATE_LIMIT_PERCENTAGE,
            CHANNEL_RATE_LIMIT_STATUS,
            CHANNEL_RATE_LIMIT_RESET,
            CHANNEL_WEBHOOK_STATUS,
            CHANNEL_WEBHOOK_EVENTS,
            CHANNEL_WEBHOOK_HEALTH
    };

    // Event types
    public static final String EVENT_ZONE_STATUS = "ZONE_STATUS";
    public static final String EVENT_DEVICE_STATUS = "DEVICE_STATUS";
    public static final String EVENT_RAIN_DELAY = "RAIN_DELAY";
    public static final String EVENT_WEATHER_INTEL = "WEATHER_INTEL";
    public static final String EVENT_WATER_BUDGET = "WATER_BUDGET";
    public static final String EVENT_SCHEDULE_STATUS = "SCHEDULE_STATUS";
    public static final String EVENT_RAIN_SENSOR = "RAIN_SENSOR";

    // API endpoints
    public static final String API_BASE_URL = "https://api.rach.io/1/public";
    public static final String API_PERSON = "/person/info";
    public static final String API_DEVICE = "/device";
    public static final String API_DEVICE_ON = "/device/on";
    public static final String API_DEVICE_OFF = "/device/off";
    public static final String API_DEVICE_PAUSE = "/device/pause";
    public static final String API_DEVICE_STOP_WATER = "/device/stop_water";
    public static final String API_ZONE_START = "/zone/start";
    public static final String API_ZONE_START_MULTIPLE = "/zone/start_multiple";
    public static final String API_ZONE_STOP = "/zone/stop";
    public static final String API_ZONE_RUN = "/zone/run";
    public static final String API_ZONE_SET = "/zone/set";
    public static final String API_SCHEDULE = "/schedule";
    public static final String API_FORECAST = "/forecast";
    public static final String API_USAGE = "/usage";
    public static final String API_SAVINGS = "/savings";
    public static final String API_ALERTS = "/alerts";
    public static final String API_WEBHOOK = "/webhook";
    public static final String API_WEBHOOK_EVENT = "/webhook/event";

    // HTTP headers
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset";

    // Content types
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";

    // Webhook constants
    public static final String WEBHOOK_SIGNATURE_HEADER = "X-Rachio-Signature";
    public static final String WEBHOOK_EVENT_HEADER = "X-Rachio-Event";
    public static final String WEBHOOK_DELIVERY_HEADER = "X-Rachio-Delivery";
    public static final String WEBHOOK_HMAC_ALGORITHM = "HmacSHA256";

    // Rate limiting constants
    public static final int RATE_LIMIT_THRESHOLD_LOW = 20;
    public static final int RATE_LIMIT_THRESHOLD_CRITICAL = 5;
    public static final long RATE_LIMIT_REFRESH_INTERVAL_NORMAL = 30000; // 30 seconds
    public static final long RATE_LIMIT_REFRESH_INTERVAL_SLOW = 60000; // 60 seconds
    public static final long RATE_LIMIT_REFRESH_INTERVAL_CRITICAL = 120000; // 120 seconds

    // Cache constants
    public static final long CACHE_DURATION_DEVICES = 300000; // 5 minutes
    public static final long CACHE_DURATION_ZONES = 300000; // 5 minutes
    public static final long CACHE_DURATION_FORECAST = 3600000; // 1 hour

    // Default values
    public static final int DEFAULT_ZONE_DURATION = 10; // 10 minutes
    public static final int DEFAULT_REFRESH_INTERVAL = 60; // 60 seconds
    public static final int DEFAULT_WEBHOOK_PORT = 8080;

    // Thing property keys (for Thing.getProperties())
    public static final String PROPERTY_KEY_SERIAL = "serialNumber";
    public static final String PROPERTY_KEY_MAC = "macAddress";
    public static final String PROPERTY_KEY_MODEL = "model";
    public static final String PROPERTY_KEY_ZONES = "zoneCount";
    public static final String PROPERTY_KEY_STATUS = "status";
    public static final String PROPERTY_KEY_SOIL = "soilType";
    public static final String PROPERTY_KEY_CROP = "cropType";
    public static final String PROPERTY_KEY_NOZZLE = "nozzleType";
    public static final String PROPERTY_KEY_SLOPE = "slopeType";
    public static final String PROPERTY_KEY_SHADE = "shadeType";
    public static final String PROPERTY_KEY_ROOT_DEPTH = "rootDepth";
    public static final String PROPERTY_KEY_EFFICIENCY = "efficiency";
    public static final String PROPERTY_KEY_AREA = "area";

    // Configuration property keys
    public static final String CONFIG_KEY_API_KEY = "apiKey";
    public static final String CONFIG_KEY_WEBHOOK_ID = "webhookId";
    public static final String CONFIG_KEY_WEBHOOK_SECRET = "webhookSecret";
    public static final String CONFIG_KEY_WEBHOOK_URL = "webhookUrl";
    public static final String CONFIG_KEY_REFRESH = "refresh";
    public static final String CONFIG_KEY_PERSON_ID = "personId";
    public static final String CONFIG_KEY_DEVICE_ID = "deviceId";
    public static final String CONFIG_KEY_ZONE_ID = "zoneId";

    // ===== CRITICAL ADDITIONS - MISSING CONSTANTS =====
    
    // Supported thing types set - THIS WAS MISSING
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(
        THING_TYPE_BRIDGE, THING_TYPE_DEVICE, THING_TYPE_ZONE
    );

    // Bridge configuration getter methods - referenced in code
    public static final String BRIDGE_CONFIG_WEBHOOK_URL = "webhookUrl";
    public static final String BRIDGE_CONFIG_WEBHOOK_SECRET = "webhookSecret";
    
    // Additional missing configuration keys
    public static final String CONFIG_KEY_DEFAULT_DURATION = "defaultDuration";
    public static final String CONFIG_KEY_DEVICE_NAME = "deviceName";
    public static final String CONFIG_KEY_ZONE_NAME = "zoneName";
    public static final String CONFIG_KEY_ZONE_NUMBER = "zoneNumber";
    
    // Additional event types that might be needed
    public static final String EVENT_TYPE_ZONE_STARTED = "ZONE_STARTED";
    public static final String EVENT_TYPE_ZONE_STOPPED = "ZONE_STOPPED";
    public static final String EVENT_TYPE_ZONE_COMPLETED = "ZONE_COMPLETED";
    public static final String EVENT_TYPE_DEVICE_ONLINE = "DEVICE_ONLINE";
    public static final String EVENT_TYPE_DEVICE_OFFLINE = "DEVICE_OFFLINE";
    
    // Rate limit method names for RachioHttp
    public static final String METHOD_GET_RATE_LIMITS = "getRateLimits";
    public static final String METHOD_GET_RATE_LIMIT_RESET = "getRateLimitReset";
    
    // Handler method names for logging/debugging
    public static final String METHOD_GET_BRIDGE_CONFIG = "getBridgeConfig";
    public static final String METHOD_GET_HTTP_CLIENT = "getHttpClient";
    public static final String METHOD_IS_WEBHOOK_CONFIGURED = "isWebhookConfigured";
    
    // Discovery service constants
    public static final String DISCOVERY_SERVICE_BACKGROUND = "startBackgroundDiscovery";
    public static final String DISCOVERY_SERVICE_SCAN = "startScan";
    
    // Webhook event parsing method
    public static final String METHOD_PARSE_WEBHOOK_EVENT = "parseWebhookEvent";
    
    // Security-related methods
    public static final String METHOD_IS_IP_ALLOWED = "isIpAllowed";
    public static final String METHOD_GET_GSON = "getGson";
    public static final String METHOD_HANDLE_WEBHOOK_EVENT = "handleWebhookEvent";
    
    // Scheduler reference for RachioSecurity
    public static final String SCHEDULER_REFERENCE = "scheduler";
}
