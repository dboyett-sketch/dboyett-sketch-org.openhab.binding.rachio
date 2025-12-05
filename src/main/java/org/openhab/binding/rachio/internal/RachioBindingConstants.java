package org.openhab.binding.rachio.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * The {@link RachioBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Brian G. - Initial contribution (from 2.5 binding)
 * @author Daniel B. - Major rewrite for OpenHAB 5.x with professional features
 */
@NonNullByDefault
public class RachioBindingConstants {

    // =========================================================================
    // BINDING ID
    // =========================================================================
    public static final String BINDING_ID = "rachio";

    // =========================================================================
    // THING TYPE UIDs
    // =========================================================================
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "device");
    public static final ThingTypeUID THING_TYPE_ZONE = new ThingTypeUID(BINDING_ID, "zone");

    // =========================================================================
    // CONFIGURATION PROPERTIES
    // =========================================================================
    
    // Bridge Configuration
    public static final String CONFIG_API_KEY = "apiKey";
    public static final String CONFIG_WEBHOOK_ID = "webhookId";
    public static final String CONFIG_WEBHOOK_SECRET = "webhookSecret";
    public static final String CONFIG_WEBHOOK_URL = "webhookUrl";
    public static final String CONFIG_REFRESH_INTERVAL = "refreshInterval";
    public static final String CONFIG_ALLOWED_IPS = "allowedIPs";
    public static final String CONFIG_USE_AWS_IPS = "useAWSIPs";
    public static final String CONFIG_POLLING_ENABLED = "pollingEnabled";
    public static final String CONFIG_RATE_LIMIT_WARNING = "rateLimitWarningThreshold";
    public static final String CONFIG_RATE_LIMIT_CRITICAL = "rateLimitCriticalThreshold";
    public static final String CONFIG_ADAPTIVE_POLLING = "adaptivePolling";
    public static final String CONFIG_ENABLE_FORECAST = "enableForecast";
    public static final String CONFIG_ENABLE_WATER_ANALYTICS = "enableWaterAnalytics";
    public static final String CONFIG_ENABLE_SAVINGS = "enableSavings";
    public static final String CONFIG_ENABLE_ALERTS = "enableAlerts";
    public static final String CONFIG_ENABLE_SCHEDULE_MONITORING = "enableScheduleMonitoring";
    public static final String CONFIG_DEBUG_LOGGING = "debugLogging";
    public static final String CONFIG_WEBHOOK_LOGGING = "webhookLogging";
    public static final String CONFIG_RATE_LIMIT_LOGGING = "rateLimitLogging";
    public static final String CONFIG_HTTP_TIMEOUT = "httpTimeout";
    public static final String CONFIG_MAX_RETRIES = "maxRetries";
    public static final String CONFIG_RETRY_DELAY = "retryDelay";
    public static final String CONFIG_VALIDATE_SSL = "validateSSL";

    // Device Configuration
    public static final String CONFIG_DEVICE_ID = "deviceId";
    public static final String CONFIG_DEVICE_NAME = "deviceName";
    public static final String CONFIG_DEVICE_POLLING_INTERVAL = "pollingInterval";
    public static final String CONFIG_DEVICE_AUTO_REFRESH = "autoRefresh";
    public static final String CONFIG_DEVICE_MONITOR_FORECAST = "monitorForecast";
    public static final String CONFIG_DEVICE_FORECAST_INTERVAL = "forecastUpdateInterval";
    public static final String CONFIG_DEVICE_MONITOR_WATER_USAGE = "monitorWaterUsage";
    public static final String CONFIG_DEVICE_WATER_USAGE_INTERVAL = "waterUsageUpdateInterval";
    public static final String CONFIG_DEVICE_MONITOR_SAVINGS = "monitorSavings";
    public static final String CONFIG_DEVICE_MONITOR_ALERTS = "monitorAlerts";
    public static final String CONFIG_DEVICE_ALERTS_INTERVAL = "alertsUpdateInterval";
    public static final String CONFIG_DEVICE_MONITOR_SCHEDULES = "monitorSchedules";
    public static final String CONFIG_DEVICE_DEFAULT_RUNTIME = "defaultZoneRuntime";
    public static final String CONFIG_DEVICE_MAX_RUNTIME = "maxZoneRuntime";
    public static final String CONFIG_DEVICE_MIN_RUNTIME = "minZoneRuntime";
    public static final String CONFIG_DEVICE_CONFIRM_STOP = "confirmStopWatering";
    public static final String CONFIG_DEVICE_CONFIRM_RAIN_DELAY = "confirmRainDelay";
    public static final String CONFIG_DEVICE_DISPLAY_UNITS = "displayUnits";
    public static final String CONFIG_DEVICE_SHOW_ADVANCED = "showAdvancedChannels";
    public static final String CONFIG_DEVICE_GROUP_BY_CATEGORY = "groupByCategory";

    // Zone Configuration
    public static final String CONFIG_ZONE_ID = "zoneId";
    public static final String CONFIG_ZONE_NUMBER = "zoneNumber";
    public static final String CONFIG_ZONE_NAME = "zoneName";
    public static final String CONFIG_ZONE_DEFAULT_RUNTIME = "defaultRuntime";
    public static final String CONFIG_ZONE_MAX_RUNTIME = "maxRuntime";
    public static final String CONFIG_ZONE_MIN_RUNTIME = "minRuntime";
    public static final String CONFIG_ZONE_ENABLED = "enabled";
    public static final String CONFIG_ZONE_SOIL_TYPE = "soilType";
    public static final String CONFIG_ZONE_CROP_TYPE = "cropType";
    public static final String CONFIG_ZONE_CROP_COEFFICIENT = "cropCoefficient";
    public static final String CONFIG_ZONE_NOZZLE_TYPE = "nozzleType";
    public static final String CONFIG_ZONE_NOZZLE_RATE = "nozzleRate";
    public static final String CONFIG_ZONE_SLOPE_TYPE = "slopeType";
    public static final String CONFIG_ZONE_SHADE_TYPE = "shadeType";
    public static final String CONFIG_ZONE_ROOT_DEPTH = "rootDepth";
    public static final String CONFIG_ZONE_EFFICIENCY = "efficiency";
    public static final String CONFIG_ZONE_AREA = "area";
    public static final String CONFIG_ZONE_WATER_ADJUSTMENT_1 = "waterAdjustment1";
    public static final String CONFIG_ZONE_WATER_ADJUSTMENT_2 = "waterAdjustment2";
    public static final String CONFIG_ZONE_WATER_ADJUSTMENT_3 = "waterAdjustment3";
    public static final String CONFIG_ZONE_WATER_ADJUSTMENT_4 = "waterAdjustment4";
    public static final String CONFIG_ZONE_WATER_ADJUSTMENT_5 = "waterAdjustment5";
    public static final String CONFIG_ZONE_MONITOR_STATUS = "monitorStatus";
    public static final String CONFIG_ZONE_SHOW_PROFESSIONAL_DATA = "showProfessionalData";
    public static final String CONFIG_ZONE_TRACK_HISTORY = "trackWateringHistory";
    public static final String CONFIG_ZONE_HISTORY_DAYS = "historyDays";
    public static final String CONFIG_ZONE_ALLOW_MANUAL_START = "allowManualStart";
    public static final String CONFIG_ZONE_ALLOW_MANUAL_STOP = "allowManualStop";
    public static final String CONFIG_ZONE_CONFIRM_START = "confirmStart";
    public static final String CONFIG_ZONE_CONFIRM_STOP = "confirmStop";
    public static final String CONFIG_ZONE_DISPLAY_NAME = "displayName";
    public static final String CONFIG_ZONE_DISPLAY_ICON = "displayIcon";
    public static final String CONFIG_ZONE_SHOW_IN_DASHBOARD = "showInDashboard";
    public static final String CONFIG_ZONE_DASHBOARD_ORDER = "dashboardOrder";
    public static final String CONFIG_ZONE_ENABLE_AUTOMATION = "enableAutomation";
    public static final String CONFIG_ZONE_AUTOMATION_RULES = "automationRules";
    public static final String CONFIG_ZONE_LINK_TO_WEATHER = "linkToWeather";
    public static final String CONFIG_ZONE_LINK_TO_SOIL_MOISTURE = "linkToSoilMoisture";

    // =========================================================================
    // BRIDGE CHANNELS
    // =========================================================================
    
    // Status Channels
    public static final String CHANNEL_BRIDGE_STATUS = "bridgeStatus";
    public static final String CHANNEL_BRIDGE_WEBHOOK_STATUS = "webhookStatus";
    public static final String CHANNEL_BRIDGE_API_STATUS = "apiStatus";
    public static final String CHANNEL_BRIDGE_POLLING_STATUS = "pollingStatus";
    
    // Rate Limiting Channels
    public static final String CHANNEL_BRIDGE_RATE_LIMIT_TOTAL = "rateLimitTotal";
    public static final String CHANNEL_BRIDGE_RATE_LIMIT_REMAINING = "rateLimitRemaining";
    public static final String CHANNEL_BRIDGE_RATE_LIMIT_PERCENT = "rateLimitPercent";
    public static final String CHANNEL_BRIDGE_RATE_LIMIT_RESET = "rateLimitReset";
    
    // Statistics Channels
    public static final String CHANNEL_BRIDGE_LAST_UPDATE = "lastUpdate";
    public static final String CHANNEL_BRIDGE_WEBHOOK_COUNT = "webhookCount";
    public static final String CHANNEL_BRIDGE_DEVICE_COUNT = "deviceCount";
    public static final String CHANNEL_BRIDGE_ZONE_COUNT = "zoneCount";
    
    // Control Channels
    public static final String CHANNEL_BRIDGE_REFRESH_DEVICES = "refreshDevices";
    public static final String CHANNEL_BRIDGE_REGISTER_WEBHOOK = "registerWebhook";
    public static final String CHANNEL_BRIDGE_UNREGISTER_WEBHOOK = "unregisterWebhook";

    // =========================================================================
    // DEVICE CHANNELS
    // =========================================================================
    
    // Basic Information Channels
    public static final String CHANNEL_DEVICE_NAME = "deviceName";
    public static final String CHANNEL_DEVICE_STATUS = "deviceStatus";
    public static final String CHANNEL_DEVICE_ONLINE = "deviceOnline";
    public static final String CHANNEL_DEVICE_PAUSED = "devicePaused";
    public static final String CHANNEL_DEVICE_ZONE_COUNT = "zoneCount";
    
    // Rain Delay Channels
    public static final String CHANNEL_DEVICE_RAIN_DELAY = "rainDelay";
    public static final String CHANNEL_DEVICE_RAIN_DELAY_HOURS = "rainDelayHours";
    public static final String CHANNEL_DEVICE_SET_RAIN_DELAY = "setRainDelay";
    
    // Device Control Channels
    public static final String CHANNEL_DEVICE_STOP_WATER = "stopWater";
    public static final String CHANNEL_DEVICE_RUN_ALL_ZONES = "runAllZones";
    public static final String CHANNEL_DEVICE_RUN_NEXT_ZONE = "runNextZone";
    public static final String CHANNEL_DEVICE_PAUSE = "pauseDevice";
    
    // Device Information Channels
    public static final String CHANNEL_DEVICE_SERIAL_NUMBER = "serialNumber";
    public static final String CHANNEL_DEVICE_MODEL = "model";
    public static final String CHANNEL_DEVICE_MAC_ADDRESS = "macAddress";
    public static final String CHANNEL_DEVICE_FIRMWARE_VERSION = "firmwareVersion";
    public static final String CHANNEL_DEVICE_CREATED_DATE = "createdDate";
    public static final String CHANNEL_DEVICE_UPDATED_DATE = "updatedDate";
    public static final String CHANNEL_DEVICE_LAST_HEARD_FROM = "lastHeardFrom";
    
    // Professional Feature Channels
    public static final String CHANNEL_DEVICE_FORECAST = "forecast";
    public static final String CHANNEL_DEVICE_WEATHER_INTEL = "weatherIntel";
    public static final String CHANNEL_DEVICE_WATER_USAGE = "waterUsage";
    public static final String CHANNEL_DEVICE_WATER_SAVINGS = "waterSavings";
    public static final String CHANNEL_DEVICE_ALERTS = "alerts";
    public static final String CHANNEL_DEVICE_SCHEDULE_STATUS = "scheduleStatus";

    // =========================================================================
    // ZONE CHANNELS
    // =========================================================================
    
    // Basic Zone Channels
    public static final String CHANNEL_ZONE_NAME = "zoneName";
    public static final String CHANNEL_ZONE_ENABLED = "zoneEnabled";
    public static final String CHANNEL_ZONE_STATUS = "zoneStatus";
    public static final String CHANNEL_ZONE_RUNTIME = "zoneRuntime";
    public static final String CHANNEL_ZONE_LAST_RUN = "zoneLastRun";
    
    // Zone Control Channels
    public static final String CHANNEL_ZONE_START = "zoneStart";
    public static final String CHANNEL_ZONE_STOP = "zoneStop";
    
    // Water Management Channels
    public static final String CHANNEL_ZONE_WATER_BUDGET = "waterBudget";
    public static final String CHANNEL_ZONE_AVAILABLE_WATER = "availableWater";
    
    // Professional Irrigation Data Channels
    public static final String CHANNEL_ZONE_SOIL_TYPE = "soilType";
    public static final String CHANNEL_ZONE_CROP_TYPE = "cropType";
    public static final String CHANNEL_ZONE_CROP_COEFFICIENT = "cropCoefficient";
    public static final String CHANNEL_ZONE_NOZZLE_TYPE = "nozzleType";
    public static final String CHANNEL_ZONE_NOZZLE_RATE = "nozzleRate";
    public static final String CHANNEL_ZONE_SLOPE_TYPE = "slopeType";
    public static final String CHANNEL_ZONE_SHADE_TYPE = "shadeType";
    public static final String CHANNEL_ZONE_ROOT_DEPTH = "rootDepth";
    public static final String CHANNEL_ZONE_EFFICIENCY = "efficiency";
    public static final String CHANNEL_ZONE_AREA = "area";
    
    // Water Adjustment Runtimes (levels 1-5)
    public static final String CHANNEL_ZONE_WATER_ADJUSTMENT_1 = "wateringAdjustment1";
    public static final String CHANNEL_ZONE_WATER_ADJUSTMENT_2 = "wateringAdjustment2";
    public static final String CHANNEL_ZONE_WATER_ADJUSTMENT_3 = "wateringAdjustment3";
    public static final String CHANNEL_ZONE_WATER_ADJUSTMENT_4 = "wateringAdjustment4";
    public static final String CHANNEL_ZONE_WATER_ADJUSTMENT_5 = "wateringAdjustment5";
    
    // Helper method to get water adjustment channel ID
    public static String getWaterAdjustmentChannelId(int level) {
        switch (level) {
            case 1: return CHANNEL_ZONE_WATER_ADJUSTMENT_1;
            case 2: return CHANNEL_ZONE_WATER_ADJUSTMENT_2;
            case 3: return CHANNEL_ZONE_WATER_ADJUSTMENT_3;
            case 4: return CHANNEL_ZONE_WATER_ADJUSTMENT_4;
            case 5: return CHANNEL_ZONE_WATER_ADJUSTMENT_5;
            default: return CHANNEL_ZONE_WATER_ADJUSTMENT_1;
        }
    }

    // =========================================================================
    // CHANNEL TYPE UIDs
    // =========================================================================
    public static final ChannelTypeUID CHANNEL_TYPE_UID_STRING = new ChannelTypeUID(BINDING_ID, "string");
    public static final ChannelTypeUID CHANNEL_TYPE_UID_NUMBER = new ChannelTypeUID(BINDING_ID, "number");
    public static final ChannelTypeUID CHANNEL_TYPE_UID_SWITCH = new ChannelTypeUID(BINDING_ID, "switch");
    public static final ChannelTypeUID CHANNEL_TYPE_UID_DATETIME = new ChannelTypeUID(BINDING_ID, "datetime");
    public static final ChannelTypeUID CHANNEL_TYPE_UID_CONTACT = new ChannelTypeUID(BINDING_ID, "contact");
    public static final ChannelTypeUID CHANNEL_TYPE_UID_DIMMER = new ChannelTypeUID(BINDING_ID, "dimmer");

    // =========================================================================
    // CHANNEL GROUP IDs
    // =========================================================================
    public static final String CHANNEL_GROUP_BASIC = "basic";
    public static final String CHANNEL_GROUP_CONTROL = "control";
    public static final String CHANNEL_GROUP_STATUS = "status";
    public static final String CHANNEL_GROUP_PROFESSIONAL = "professional";
    public static final String CHANNEL_GROUP_MONITORING = "monitoring";
    public static final String CHANNEL_GROUP_WEBHOOK = "webhook";
    public static final String CHANNEL_GROUP_WATER_MANAGEMENT = "waterManagement";
    public static final String CHANNEL_GROUP_IRRIGATION_DATA = "irrigationData";
    public static final String CHANNEL_GROUP_SITE_CONDITIONS = "siteConditions";
    public static final String CHANNEL_GROUP_WATER_ADJUSTMENTS = "waterAdjustments";

    // =========================================================================
    // SUPPORTED THING TYPES
    // =========================================================================
    public static final ThingTypeUID[] SUPPORTED_THING_TYPES = {
        THING_TYPE_BRIDGE,
        THING_TYPE_DEVICE,
        THING_TYPE_ZONE
    };

    // =========================================================================
    // DISCOVERY PROPERTIES
    // =========================================================================
    public static final String DISCOVERY_DEVICE_ID = "deviceId";
    public static final String DISCOVERY_DEVICE_NAME = "deviceName";
    public static final String DISCOVERY_ZONE_ID = "zoneId";
    public static final String DISCOVERY_ZONE_NAME = "zoneName";
    public static final String DISCOVERY_ZONE_NUMBER = "zoneNumber";

    // =========================================================================
    // WEBHOOK CONSTANTS
    // =========================================================================
    public static final String WEBHOOK_PATH = "/rachio/webhook";
    public static final int WEBHOOK_DEFAULT_PORT = 8080;
    public static final String WEBHOOK_EVENT_TYPE = "eventType";
    public static final String WEBHOOK_DEVICE_ID = "deviceId";
    public static final String WEBHOOK_ZONE_ID = "zoneId";
    public static final String WEBHOOK_SIGNATURE = "x-signature";

    // Webhook Event Types
    public static final String EVENT_ZONE_STATUS = "ZONE_STATUS";
    public static final String EVENT_DEVICE_STATUS = "DEVICE_STATUS";
    public static final String EVENT_RAIN_DELAY = "RAIN_DELAY";
    public static final String EVENT_WEATHER_INTEL = "WEATHER_INTEL";
    public static final String EVENT_WATER_BUDGET = "WATER_BUDGET";
    public static final String EVENT_SCHEDULE_STATUS = "SCHEDULE_STATUS";
    public static final String EVENT_RAIN_SENSOR = "RAIN_SENSOR";
    public static final String EVENT_DEVICE_ALERT = "DEVICE_ALERT";
    public static final String EVENT_SCHEDULE_COMPLETE = "SCHEDULE_COMPLETE";

    // =========================================================================
    // API CONSTANTS
    // =========================================================================
    public static final String API_BASE_URL = "https://api.rach.io/1/public";
    public static final int API_RATE_LIMIT_DAILY = 1700;
    public static final String API_HEADER_RATE_LIMIT_TOTAL = "X-RateLimit-Limit";
    public static final String API_HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String API_HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset";

    // =========================================================================
    // THING PROPERTIES
    // =========================================================================
    public static final String PROPERTY_DEVICE_ID = "deviceId";
    public static final String PROPERTY_DEVICE_NAME = "deviceName";
    public static final String PROPERTY_ZONE_ID = "zoneId";
    public static final String PROPERTY_ZONE_NAME = "zoneName";
    public static final String PROPERTY_ZONE_NUMBER = "zoneNumber";
    public static final String PROPERTY_SERIAL_NUMBER = "serialNumber";
    public static final String PROPERTY_MODEL = "model";
    public static final String PROPERTY_MAC_ADDRESS = "macAddress";
    public static final String PROPERTY_FIRMWARE_VERSION = "firmwareVersion";
    public static final String PROPERTY_CREATED_DATE = "createdDate";
    public static final String PROPERTY_UPDATED_DATE = "updatedDate";
    
    // Professional Data Properties
    public static final String PROPERTY_SOIL_TYPE = "soilType";
    public static final String PROPERTY_CROP_TYPE = "cropType";
    public static final String PROPERTY_CROP_COEFFICIENT = "cropCoefficient";
    public static final String PROPERTY_NOZZLE_TYPE = "nozzleType";
    public static final String PROPERTY_NOZZLE_RATE = "nozzleRate";
    public static final String PROPERTY_SLOPE_TYPE = "slopeType";
    public static final String PROPERTY_SHADE_TYPE = "shadeType";
    public static final String PROPERTY_ROOT_DEPTH = "rootDepth";
    public static final String PROPERTY_EFFICIENCY = "efficiency";
    public static final String PROPERTY_AREA = "area";
    public static final String PROPERTY_WATER_ADJUSTMENTS = "wateringAdjustments";

    // =========================================================================
    // DEFAULT VALUES
    // =========================================================================
    public static final int DEFAULT_ZONE_RUNTIME = 600; // 10 minutes in seconds
    public static final int DEFAULT_REFRESH_INTERVAL = 60; // 1 minute in seconds
    public static final int DEFAULT_RAIN_DELAY_HOURS = 24;
    public static final int MIN_ZONE_RUNTIME = 60; // 1 minute in seconds
    public static final int MAX_ZONE_RUNTIME = 10800; // 3 hours in seconds
    public static final int MIN_REFRESH_INTERVAL = 30; // 30 seconds
    public static final int MAX_REFRESH_INTERVAL = 3600; // 1 hour

    // =========================================================================
    // STATUS STRINGS
    // =========================================================================
    public static final String STATUS_ONLINE = "ONLINE";
    public static final String STATUS_OFFLINE = "OFFLINE";
    public static final String STATUS_SLEEP = "SLEEP";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_IDLE = "IDLE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_STOPPED = "STOPPED";
    public static final String STATUS_PAUSED = "PAUSED";

    // =========================================================================
    // LOGGING CONSTANTS
    // =========================================================================
    public static final String LOGGER_API = "org.openhab.binding.rachio.api";
    public static final String LOGGER_WEBHOOK = "org.openhab.binding.rachio.webhook";
    public static final String LOGGER_HANDLER = "org.openhab.binding.rachio.handler";
    public static final String LOGGER_DISCOVERY = "org.openhab.binding.rachio.discovery";

    // =========================================================================
    // UNITS
    // =========================================================================
    public static final String UNIT_SECONDS = "s";
    public static final String UNIT_MINUTES = "min";
    public static final String UNIT_HOURS = "h";
    public static final String UNIT_INCHES = "in";
    public static final String UNIT_SQUARE_FEET = "ft²";
    public static final String UNIT_PERCENT = "%";
    public static final String UNIT_INCHES_PER_HOUR = "in/h";

    // =========================================================================
    // CHANNEL CATEGORIES
    // =========================================================================
    public static final String CATEGORY_IRRIGATION = "Irrigation";
    public static final String CATEGORY_WATER = "Water";
    public static final String CATEGORY_WEATHER = "Weather";
    public static final String CATEGORY_SYSTEM = "System";
    public static final String CATEGORY_MONITORING = "Monitoring";
    public static final String CATEGORY_PROFESSIONAL = "Professional";
    public static final String CATEGORY_SOIL = "Soil";
    public static final String CATEGORY_CROP = "Crop";
    public static final String CATEGORY_EQUIPMENT = "Equipment";
    public static final String CATEGORY_SITE = "Site";
    public static final String CATEGORY_PLANT = "Plant";
    public static final String CATEGORY_PERFORMANCE = "Performance";
    public static final String CATEGORY_MEASUREMENTS = "Measurements";

    // =========================================================================
    // EVENT CATEGORIES
    // =========================================================================
    public static final String CATEGORY_ZONE_EVENT = "ZoneEvent";
    public static final String CATEGORY_DEVICE_EVENT = "DeviceEvent";
    public static final String CATEGORY_WEATHER_EVENT = "WeatherEvent";
    public static final String CATEGORY_SCHEDULE_EVENT = "ScheduleEvent";
    public static final String CATEGORY_ALERT_EVENT = "AlertEvent";

    // =========================================================================
    // CONFIGURATION DESCRIPTIONS
    // =========================================================================
    public static final String CONFIG_DESCRIPTION_URI_BRIDGE = "thing-type:rachio:bridge";
    public static final String CONFIG_DESCRIPTION_URI_DEVICE = "thing-type:rachio:device";
    public static final String CONFIG_DESCRIPTION_URI_ZONE = "thing-type:rachio:zone";

    // =========================================================================
    // PARAMETER OPTIONS
    // =========================================================================
    public static final String[] SOIL_TYPES = {
        "Clay", "Clay Loam", "Loam", "Loamy Sand", "Sand", "Sandy Clay", "Sandy Clay Loam",
        "Sandy Loam", "Silt", "Silt Clay", "Silt Clay Loam", "Silt Loam", "Custom"
    };

    public static final String[] CROP_TYPES = {
        "Cool Season Grass", "Warm Season Grass", "Fruit Trees", "Flowers", "Garden",
        "Ground Cover", "Shrubs", "Trees", "Vegetables", "Custom"
    };

    public static final String[] NOZZLE_TYPES = {
        "Fixed Spray Head", "Rotating Stream Nozzle", "Rotary Nozzle", "Rotor", "Drip Irrigation",
        "Bubbler", "Soaker Hose", "Impact Sprinkler", "Custom"
    };

    public static final String[] SLOPE_TYPES = {
        "Flat (0-2%)", "Gentle (2-6%)", "Moderate (6-12%)", "Steep (12+%)", "Custom"
    };

    public static final String[] SHADE_TYPES = {
        "Full Sun", "Partial Sun", "Partial Shade", "Full Shade", "Custom"
    };

    // =========================================================================
    // DEFAULT PROFESSIONAL VALUES
    // =========================================================================
    public static final double DEFAULT_CROP_COEFFICIENT = 0.8;
    public static final double DEFAULT_NOZZLE_RATE = 1.5; // inches per hour
    public static final double DEFAULT_ROOT_DEPTH = 6.0; // inches
    public static final double DEFAULT_EFFICIENCY = 0.75; // 75%
    public static final double DEFAULT_AREA = 500.0; // square feet
}
