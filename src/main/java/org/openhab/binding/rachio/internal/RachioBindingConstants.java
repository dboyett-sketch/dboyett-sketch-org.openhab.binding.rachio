package org.openhab.binding.rachio.internal;

import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link RachioBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author David Boyett - Initial contribution
 */
public class RachioBindingConstants {

    public static final String BINDING_ID = "rachio";
    
    // Thing Type UIDs - MUST match thing-types.xml IDs exactly
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "rachio-bridge");
    public static final ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "rachio-device");
    public static final ThingTypeUID THING_TYPE_ZONE = new ThingTypeUID(BINDING_ID, "rachio-zone");
    
    // Channel IDs for Bridge
    public static final String CHANNEL_BRIDGE_STATUS = "bridgeStatus";
    public static final String CHANNEL_BRIDGE_HEARTBEAT = "heartbeat";
    
    // Channel IDs for Device
    public static final String CHANNEL_DEVICE_STATUS = "status";
    public static final String CHANNEL_DEVICE_ONLINE = "online";
    public static final String CHANNEL_DEVICE_RAIN_DELAY = "rainDelay";
    public static final String CHANNEL_DEVICE_RAIN_DELAY_END_TIME = "rainDelayEndTime";
    public static final String CHANNEL_DEVICE_SCHEDULE_MODE = "scheduleMode";
    
    // Rate limiting channels for Device
    public static final String CHANNEL_RATE_LIMIT_REMAINING = "rateLimitRemaining";
    public static final String CHANNEL_RATE_LIMIT_PERCENT = "rateLimitPercent";
    public static final String CHANNEL_RATE_LIMIT_STATUS = "rateLimitStatus";
    public static final String CHANNEL_RATE_LIMIT_RESET = "rateLimitReset";
    
    // Channel IDs for Zone
    public static final String CHANNEL_ZONE_RUN = "run";
    public static final String CHANNEL_ZONE_RUN_TIME = "runTime";
    public static final String CHANNEL_ZONE_STATUS = "status";
    public static final String CHANNEL_ZONE_ENABLED = "enabled";
    
    // Professional zone data channels
    public static final String CHANNEL_ZONE_SOIL_TYPE = "soilType";
    public static final String CHANNEL_ZONE_SOIL_AVAILABLE_WATER = "soilAvailableWater";
    public static final String CHANNEL_ZONE_CROP_TYPE = "cropType";
    public static final String CHANNEL_ZONE_CROP_COEFFICIENT = "cropCoefficient";
    public static final String CHANNEL_ZONE_NOZZLE_TYPE = "nozzleType";
    public static final String CHANNEL_ZONE_NOZZLE_RATE = "nozzleRate";
    public static final String CHANNEL_ZONE_SLOPE_TYPE = "slopeType";
    public static final String CHANNEL_ZONE_SHADE_TYPE = "shadeType";
    public static final String CHANNEL_ZONE_ROOT_DEPTH = "rootDepth";
    public static final String CHANNEL_ZONE_IRRIGATION_EFFICIENCY = "irrigationEfficiency";
    public static final String CHANNEL_ZONE_ADJUSTMENT_LEVEL_1 = "adjustmentLevel1";
    public static final String CHANNEL_ZONE_ADJUSTMENT_LEVEL_2 = "adjustmentLevel2";
    public static final String CHANNEL_ZONE_ADJUSTMENT_LEVEL_3 = "adjustmentLevel3";
    public static final String CHANNEL_ZONE_ADJUSTMENT_LEVEL_4 = "adjustmentLevel4";
    public static final String CHANNEL_ZONE_ADJUSTMENT_LEVEL_5 = "adjustmentLevel5";
    public static final String CHANNEL_ZONE_AREA = "zoneArea";
    
    // Bridge Configuration Properties
    public static final String CONFIG_API_KEY = "apiKey";
    public static final String CONFIG_REFRESH_INTERVAL = "refresh";
    public static final String CONFIG_WEBHOOK_PORT = "webhookPort";
    public static final String CONFIG_WEBHOOK_ENABLED = "webhookEnabled";
    public static final String CONFIG_HOST = "host";
    public static final String CONFIG_PORT = "port";
    
    // Device Configuration Properties
    public static final String CONFIG_DEVICE_ID = "deviceId";
    
    // Zone Configuration Properties
    public static final String CONFIG_ZONE_ID = "zoneId";
    public static final String CONFIG_ZONE_DEVICE_ID = "deviceId";
    
    // Listener Properties
    public static final String PROPERTY_LISTENER_ID = "listenerId";
    
    // API Constants
    public static final String RACHIO_API_BASE_URL = "https://api.rach.io";
    public static final int DEFAULT_REFRESH_INTERVAL = 30;
    public static final int DEFAULT_WEBHOOK_PORT = 8088;
    public static final String DEFAULT_HOST = "api.rach.io";
    public static final int DEFAULT_PORT = 443;
    
    // Webhook Constants
    public static final String WEBHOOK_PATH = "/rachio/webhook";
    public static final String WEBHOOK_CALLBACK_URL = "callbackUrl";
    public static final String WEBHOOK_EXTERNAL_PORT = "externalPort";
    
    // Event Types
    public static final String EVENT_ZONE_STATUS = "ZONE_STATUS";
    public static final String EVENT_DEVICE_STATUS = "DEVICE_STATUS";
    public static final String EVENT_RAIN_DELAY = "RAIN_DELAY";
    public static final String EVENT_WEATHER_INTEL = "WEATHER_INTEL";
    public static final String EVENT_WATER_BUDGET = "WATER_BUDGET";
    public static final String EVENT_SCHEDULE_STATUS = "SCHEDULE_STATUS";
    public static final String EVENT_RAIN_SENSOR = "RAIN_SENSOR";
    
    // Zone Status Values
    public static final String ZONE_STATUS_STARTED = "STARTED";
    public static final String ZONE_STATUS_STOPPED = "STOPPED";
    public static final String ZONE_STATUS_COMPLETED = "COMPLETED";
    
    // Device Status Values
    public static final String DEVICE_STATUS_ONLINE = "ONLINE";
    public static final String DEVICE_STATUS_OFFLINE = "OFFLINE";
    public static final String DEVICE_STATUS_SLEEP = "SLEEP";
    
    // Misc Constants
    public static final String UNKNOWN = "UNKNOWN";
    public static final String PROPERTY_ID = "id";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_MODEL = "model";
    public static final String PROPERTY_ZONE_NUMBER = "zoneNumber";
    public static final String PROPERTY_LOCATION = "location";
    
    // Rate Limiting
    public static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset";
    public static final String HEADER_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";
    public static final int RATE_LIMIT_CRITICAL_THRESHOLD = 10; // When to slow down polling
    public static final int RATE_LIMIT_WARNING_THRESHOLD = 50; // When to warn
}
