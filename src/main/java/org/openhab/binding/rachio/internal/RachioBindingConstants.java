package org.openhab.binding.rachio.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link RachioBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioBindingConstants {

    public static final String BINDING_ID = "rachio";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "rachio-bridge");
    public static final ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "rachio-device");
    public static final ThingTypeUID THING_TYPE_ZONE = new ThingTypeUID(BINDING_ID, "rachio-zone");

    // List of all Channel IDs
    // Bridge channels
    public static final String CHANNEL_REFRESH = "refresh";
    public static final String CHANNEL_CLEAR_CACHE = "clearCache";
    public static final String CHANNEL_CACHE_SIZE = "cacheSize";
    public static final String CHANNEL_LAST_UPDATE = "lastUpdate";
    
    // Rate limiting channels
    public static final String CHANNEL_RATE_LIMIT_REMAINING = "rateLimitRemaining";
    public static final String CHANNEL_RATE_LIMIT_TOTAL = "rateLimitTotal";
    public static final String CHANNEL_RATE_LIMIT_PERCENT = "rateLimitPercent";
    public static final String CHANNEL_RATE_LIMIT_STATE = "rateLimitState";
    public static final String CHANNEL_RATE_LIMIT_RESET = "rateLimitReset";
    
    // Webhook channels
    public static final String CHANNEL_WEBHOOK_STATUS = "webhookStatus";
    public static final String CHANNEL_WEBHOOK_REGISTER = "webhookRegister";
    public static final String CHANNEL_WEBHOOK_REGISTERED = "webhookRegistered";
    public static final String CHANNEL_WEBHOOK_LAST_EVENT = "webhookLastEvent";
    public static final String CHANNEL_WEBHOOK_EVENT_TYPE = "webhookEventType";
    
    // Device channels
    public static final String CHANNEL_DEVICE_STATUS = "deviceStatus";
    public static final String CHANNEL_DEVICE_NAME = "deviceName";
    public static final String CHANNEL_DEVICE_MODEL = "deviceModel";
    public static final String CHANNEL_DEVICE_SERIAL = "deviceSerial";
    public static final String CHANNEL_DEVICE_ONLINE = "deviceOnline";
    public static final String CHANNEL_DEVICE_PAUSED = "devicePaused";
    public static final String CHANNEL_DEVICE_ZONE_COUNT = "zoneCount";
    public static final String CHANNEL_DEVICE_RAIN_DELAY = "rainDelay";
    public static final String CHANNEL_DEVICE_RAIN_DELAY_HOURS = "rainDelayHours";
    public static final String CHANNEL_DEVICE_LAST_HEARD = "lastHeard";
    
    // Zone channels
    public static final String CHANNEL_ZONE_NAME = "zoneName";
    public static final String CHANNEL_ZONE_NUMBER = "zoneNumber";
    public static final String CHANNEL_ZONE_ENABLED = "zoneEnabled";
    public static final String CHANNEL_ZONE_START = "zoneStart";
    public static final String CHANNEL_ZONE_STOP = "zoneStop";
    public static final String CHANNEL_ZONE_DURATION = "zoneDuration";
    public static final String CHANNEL_ZONE_STATUS = "zoneStatus";
    public static final String CHANNEL_ZONE_REMAINING = "zoneRemaining";
    public static final String CHANNEL_ZONE_END_TIME = "zoneEndTime";
    
    // Professional Irrigation Data Channels - SOIL
    public static final String CHANNEL_SOIL_TYPE = "soilType";
    public static final String CHANNEL_SOIL_AVAILABLE_WATER = "soilAvailableWater";
    
    // Professional Irrigation Data Channels - CROP
    public static final String CHANNEL_CROP_TYPE = "cropType";
    public static final String CHANNEL_CROP_COEFFICIENT = "cropCoefficient";
    
    // Professional Irrigation Data Channels - NOZZLE
    public static final String CHANNEL_NOZZLE_TYPE = "nozzleType";
    public static final String CHANNEL_NOZZLE_RATE = "nozzleRate";
    
    // Professional Irrigation Data Channels - SLOPE & SHADE
    public static final String CHANNEL_SLOPE_TYPE = "slopeType";
    public static final String CHANNEL_SHADE_TYPE = "shadeType";
    
    // Professional Irrigation Data Channels - EFFICIENCY & DEPTH
    public static final String CHANNEL_IRRIGATION_EFFICIENCY = "irrigationEfficiency";
    public static final String CHANNEL_ROOT_DEPTH = "rootDepth";
    
    // Professional Irrigation Data Channels - AREA
    public static final String CHANNEL_ZONE_AREA = "zoneArea";
    
    // Professional Irrigation Data Channels - WATERING ADJUSTMENTS
    public static final String CHANNEL_ADJUSTMENT_LEVEL1 = "adjustmentLevel1";
    public static final String CHANNEL_ADJUSTMENT_LEVEL2 = "adjustmentLevel2";
    public static final String CHANNEL_ADJUSTMENT_LEVEL3 = "adjustmentLevel3";
    public static final String CHANNEL_ADJUSTMENT_LEVEL4 = "adjustmentLevel4";
    public static final String CHANNEL_ADJUSTMENT_LEVEL5 = "adjustmentLevel5";
    
    // Status and Monitoring Channels
    public static final String CHANNEL_LAST_UPDATED = "lastUpdated";
    public static final String CHANNEL_HEALTH_STATUS = "healthStatus";
    
    // Command channels
    public static final String CHANNEL_RUN_ALL_ZONES = "runAllZones";
    public static final String CHANNEL_RUN_NEXT_ZONE = "runNextZone";
    public static final String CHANNEL_SET_RAIN_DELAY = "setRainDelay";
    public static final String CHANNEL_STOP_WATERING = "stopWatering";
    
    // Configuration parameters
    public static final String PARAM_API_KEY = "apiKey";
    public static final String PARAM_PERSON_ID = "personId";
    public static final String PARAM_DEVICE_ID = "deviceId";
    public static final String PARAM_ZONE_ID = "zoneId";
    public static final String PARAM_WEBHOOK_ENABLED = "webhookEnabled";
    public static final String PARAM_WEBHOOK_SECRET = "webhookSecret";
    public static final String PARAM_WEBHOOK_EXTERNAL_URL = "webhookExternalUrl";
    public static final String PARAM_WEBHOOK_PORT = "webhookPort";
    
    // Default values
    public static final int DEFAULT_POLLING_INTERVAL = 60; // seconds
    public static final int DEFAULT_WEBHOOK_PORT = 8080;
    public static final int DEFAULT_ZONE_DURATION = 300; // 5 minutes
    
    // Webhook constants
    public static final String WEBHOOK_PATH = "/rachio/webhook";
    public static final String WEBHOOK_CALLBACK_PATH = "/rachio/callback";
    
    // API constants
    public static final String API_BASE_URL = "https://api.rach.io/1/public";
    public static final int API_TIMEOUT = 30; // seconds
    
    // Rate limiting constants
    public static final int RATE_LIMIT_WARNING_THRESHOLD = 10;
    public static final int RATE_LIMIT_CRITICAL_THRESHOLD = 3;
    
    // Thing properties
    public static final String PROPERTY_DEVICE_ID = "deviceId";
    public static final String PROPERTY_ZONE_ID = "zoneId";
    public static final String PROPERTY_DEVICE_NAME = "deviceName";
    public static final String PROPERTY_ZONE_NAME = "zoneName";
    public static final String PROPERTY_DEVICE_MODEL = "deviceModel";
    public static final String PROPERTY_DEVICE_SERIAL = "deviceSerial";
    
    // Status constants
    public static final String STATUS_ONLINE = "ONLINE";
    public static final String STATUS_OFFLINE = "OFFLINE";
    public static final String STATUS_SLEEP = "SLEEP";
    public static final String STATUS_PAUSED = "PAUSED";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_IDLE = "IDLE";
    
    // Zone status constants
    public static final String ZONE_STATUS_STARTED = "STARTED";
    public static final String ZONE_STATUS_STOPPED = "STOPPED";
    public static final String ZONE_STATUS_COMPLETED = "COMPLETED";
    
    // Event types
    public static final String EVENT_ZONE_STATUS = "ZONE_STATUS";
    public static final String EVENT_DEVICE_STATUS = "DEVICE_STATUS";
    public static final String EVENT_RAIN_DELAY = "RAIN_DELAY";
    public static final String EVENT_WEATHER_INTEL = "WEATHER_INTEL";
    public static final String EVENT_WATER_BUDGET = "WATER_BUDGET";
    public static final String EVENT_SCHEDULE_STATUS = "SCHEDULE_STATUS";
    public static final String EVENT_RAIN_SENSOR = "RAIN_SENSOR";
}
