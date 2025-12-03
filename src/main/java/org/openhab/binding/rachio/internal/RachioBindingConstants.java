package org.openhab.binding.rachio.internal;

import java.util.Set;

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

    // List of all Channel ids - UPDATED FOR DYNAMIC CHANNELS
    public static final String CHANNEL_START_ALL_ZONES = "startAllZones";
    public static final String CHANNEL_START_NEXT_ZONE = "startNextZone";
    public static final String CHANNEL_STOP_WATERING = "stopWatering";
    public static final String CHANNEL_RAIN_DELAY = "rainDelay";
    public static final String CHANNEL_START_ZONE = "startZone";
    
    // Device-specific channel IDs
    public static final String CHANNEL_DEVICE_STATUS = "device#status";
    
    // Zone-specific channel IDs  
    public static final String CHANNEL_ZONE_NAME = "zone#zoneName";
    public static final String CHANNEL_ZONE_ENABLED = "zone#zoneEnabled";
    public static final String CHANNEL_ZONE_RUNTIME = "zone#zoneRuntime";

    // Channel group and property identifiers - KEPT FOR BACKWARD COMPATIBILITY
    public static final String DEVICE = "device";
    public static final String STATUS = "status";
    public static final String ZONE = "zone";
    public static final String ZONE_NAME = "zoneName";
    public static final String ZONE_ENABLED = "zoneEnabled";
    public static final String ZONE_RUNTIME = "zoneRuntime";
    public static final String ZONE_STATUS = "zoneStatus";
    public static final String RUN_ZONE = "runZone";
    public static final String STOP_WATERING = "stopWatering";

    // Bridge config properties - ALL PARAMETERS ADDED
    public static final String API_KEY = "apiKey";
    public static final String WEBHOOK_ID = "webhookId";
    public static final String POLLING_INTERVAL = "pollingInterval";
    public static final String DEFAULT_RUNTIME = "defaultRuntime";
    public static final String CALLBACK_URL = "callbackUrl";
    public static final String CLEAR_ALL_CALLBACKS = "clearAllCallbacks";
    public static final String IP_FILTER = "ipFilter";

    // Device config properties
    public static final String DEVICE_ID = "deviceId";

    // Zone config properties
    public static final String ZONE_ID = "zoneId";

    // Default duration for watering in seconds
    public static final int DEFAULT_DURATION = 300;

    // Supported Thing Types
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS =
            Set.of(THING_TYPE_BRIDGE, THING_TYPE_DEVICE, THING_TYPE_ZONE);

    // Webhook event types - USING STRING NAMES (your current approach)
    public static final String WEBHOOK_EVENT_ZONE_STARTED = "ZONE_STARTED";
    public static final String WEBHOOK_EVENT_ZONE_STOPPED = "ZONE_STOPPED";
    public static final String WEBHOOK_EVENT_ZONE_COMPLETED = "ZONE_COMPLETED";
    public static final String WEBHOOK_EVENT_RAIN_DELAY = "RAIN_DELAY";
    public static final String WEBHOOK_EVENT_DEVICE_STATUS = "DEVICE_STATUS";
    
    // ===== WEBHOOK EVENT TYPE IDs (from Rachio API) =====
    // These are the actual IDs that Rachio API expects
    public static final String WEBHOOK_EVENT_ID_DEVICE_STATUS = "5";
    public static final String WEBHOOK_EVENT_ID_RAIN_DELAY = "6";
    public static final String WEBHOOK_EVENT_ID_WEATHER_INTELLIGENCE = "7";
    public static final String WEBHOOK_EVENT_ID_WATER_BUDGET = "8";
    public static final String WEBHOOK_EVENT_ID_SCHEDULE_STATUS = "9";
    public static final String WEBHOOK_EVENT_ID_ZONE_STATUS = "10";
    public static final String WEBHOOK_EVENT_ID_RAIN_SENSOR_DETECTION = "11";
    public static final String WEBHOOK_EVENT_ID_ZONE_DELTA = "12";
    public static final String WEBHOOK_EVENT_ID_DELTA = "14";

    // API endpoints - FIXED TO PREVENT DOUBLE "public" IN PATH
    public static final String RACHIO_API_BASE = "https://api.rach.io/1";
    
    // Person endpoints
    public static final String API_PERSON_INFO = "/public/person/info";
    public static final String API_PERSON_BY_ID = "/public/person/";
    
    // Device endpoints
    public static final String API_DEVICE = "/public/device/";
    public static final String API_DEVICE_STOP = "/public/device/stop";
    public static final String API_RUN_ALL_ZONES = "/public/device/runAllZones";
    public static final String API_RUN_NEXT_ZONE = "/public/device/runNextZone";
    public static final String API_RAIN_DELAY = "/public/device/rainDelay";
    
    // Zone endpoints
    public static final String API_ZONE = "/public/zone/";
    public static final String API_ZONE_START = "/public/zone/start";
    public static final String API_ZONE_ENABLE = "/public/zone/enable";
    
    // ===== WEBHOOK API ENDPOINTS (CORRECTED) =====
    // Based on current Rachio API documentation
    public static final String API_WEBHOOK_BASE = "/public/notification";
    public static final String API_WEBHOOK_LIST = "/public/notification/{deviceId}/webhook";
    public static final String API_WEBHOOK_REGISTER = "/public/notification/webhook";
    public static final String API_WEBHOOK_DELETE = "/public/notification/webhook/{webhookId}";
    
    // HTTP Methods - ADDED FOR COMPLETENESS
    public static final String HTTP_METHOD_GET = "GET";
    public static final String HTTP_METHOD_PUT = "PUT";
    public static final String HTTP_METHOD_POST = "POST";
    public static final String HTTP_METHOD_DELETE = "DELETE";

    // HTTP Timeout - ADDED FOR COMPLETENESS
    public static final int HTTP_TIMEOUT = 10000; // 10 seconds

    // Rate limiting constants
    public static final int RATE_LIMIT_REQUESTS_PER_HOUR = 1500;
    public static final int MIN_POLLING_INTERVAL = 90; // seconds
    public static final int DEFAULT_POLLING_INTERVAL = 120; // seconds
    public static final int MAX_RUNTIME_SECONDS = 10800; // 3 hours
    public static final int MAX_RAIN_DELAY_HOURS = 168; // 1 week

    // AWS IP range constants
    public static final String AWS_IPADDR_DOWNLOAD_URL = "https://ip-ranges.amazonaws.com/ip-ranges.json";
    public static final String AWS_IPADDR_REGION_FILTER = "us-";

    // Rate limit related constants
    public static final String CHANNEL_RATE_LIMIT_REMAINING = "bridge#rateLimitRemaining";
    public static final String CHANNEL_RATE_LIMIT_PERCENT = "bridge#rateLimitPercent";
    public static final String CHANNEL_RATE_LIMIT_STATUS = "bridge#rateLimitStatus";
    public static final String CHANNEL_RATE_LIMIT_RESET = "bridge#rateLimitReset";

    // Rate limiting thresholds (percentages)
    public static final int RATE_LIMIT_WARNING_THRESHOLD = 10; // 10%
    public static final int RATE_LIMIT_CRITICAL_THRESHOLD = 2; // 2%

    // Adaptive polling multipliers
    public static final double ADAPTIVE_POLLING_NORMAL = 1.0;
    public static final double ADAPTIVE_POLLING_WARNING = 2.0;
    public static final double ADAPTIVE_POLLING_CRITICAL = 3.0;

    // Rate limit defaults
    public static final int RATE_LIMIT_DEFAULT_LIMIT = 1700; // Rachio hourly limit
}