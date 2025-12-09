package org.openhab.binding.rachio.internal;

import java.util.Set;

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

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(
        THING_TYPE_BRIDGE, THING_TYPE_DEVICE, THING_TYPE_ZONE
    );

    // Bridge configuration
    public static final String CONFIG_ACCESS_TOKEN = "accessToken";
    public static final String CONFIG_SECRET_KEY = "secretKey";
    public static final String CONFIG_DEVICE_ID = "deviceId";
    public static final String CONFIG_WEBHOOK_URL = "webhookUrl";
    public static final String CONFIG_WEBHOOK_ENABLED = "webhookEnabled";
    public static final String CONFIG_WEBHOOK_CHECK_INTERVAL = "webhookCheckInterval";
    public static final String CONFIG_ALLOWED_IPS = "allowedIps";
    public static final String CONFIG_ALLOW_AWS_IPS = "allowAwsIps";
    public static final String CONFIG_WEATHER_ENABLED = "weatherEnabled";

    // Device configuration
    public static final String CONFIG_DEVICE_ID = "deviceId";

    // Zone configuration
    public static final String CONFIG_ZONE_ID = "zoneId";
    public static final String CONFIG_ZONE_DEVICE_ID = "deviceId";

    // Thing properties
    public static final String PROPERTY_ID = "id";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_MODEL = "model";
    public static final String PROPERTY_SERIAL_NUMBER = "serialNumber";
    public static final String PROPERTY_MAC_ADDRESS = "macAddress";
    public static final String PROPERTY_STATUS = "status";
    public static final String PROPERTY_ZONE_COUNT = "zoneCount";
    public static final String PROPERTY_ELEVATION = "elevation";
    public static final String PROPERTY_FLEX_SCHEDULE = "flexSchedule";
    public static final String PROPERTY_CREATED_DATE = "createdDate";
    public static final String PROPERTY_UPDATED_DATE = "updatedDate";
    public static final String PROPERTY_ZONE_NUMBER = "zoneNumber";
    public static final String PROPERTY_ENABLED = "enabled";
    public static final String PROPERTY_RUNTIME = "runtime";
    public static final String PROPERTY_AREA = "area";
    public static final String PROPERTY_SOIL_TYPE = "soilType";
    public static final String PROPERTY_CROP_TYPE = "cropType";
    public static final String PROPERTY_NOZZLE_TYPE = "nozzleType";
    public static final String PROPERTY_SLOPE_TYPE = "slopeType";
    public static final String PROPERTY_SHADE_TYPE = "shadeType";
    public static final String PROPERTY_ROOT_ZONE_DEPTH = "rootZoneDepth";
    public static final String PROPERTY_EFFICIENCY = "efficiency";
    public static final String PROPERTY_AVAILABLE_WATER = "availableWater";
    public static final String PROPERTY_ADJUSTMENT_LEVEL_PREFIX = "adjustmentLevel";
    public static final String PROPERTY_DEVICE_ID = "deviceId";

    // Bridge channels
    public static final String CHANNEL_RATE_LIMIT_REMAINING = "rateLimitRemaining";
    public static final String CHANNEL_RATE_LIMIT_PERCENT = "rateLimitPercent";
    public static final String CHANNEL_RATE_LIMIT_STATUS = "rateLimitStatus";
    public static final String CHANNEL_RATE_LIMIT_RESET = "rateLimitReset";

    // Device channels
    public static final String CHANNEL_STATUS = "status";
    public static final String CHANNEL_ONLINE = "online";
    public static final String CHANNEL_RAIN_DELAY = "rainDelay";
    public static final String CHANNEL_RAIN_DELAY_REMAINING = "rainDelayRemaining";
    public static final String CHANNEL_RUN_ALL_ZONES = "runAllZones";
    public static final String CHANNEL_STOP_WATERING = "stopWatering";
    public static final String CHANNEL_RUN_NEXT_ZONE = "runNextZone";
    public static final String CHANNEL_PAUSE_DEVICE = "pauseDevice";
    public static final String CHANNEL_SERIAL_NUMBER = "serialNumber";
    public static final String CHANNEL_MAC_ADDRESS = "macAddress";
    public static final String CHANNEL_ELEVATION = "elevation";
    public static final String CHANNEL_FLEX_SCHEDULE = "flexSchedule";
    public static final String CHANNEL_ZONE_COUNT = "zoneCount";
    public static final String CHANNEL_TOTAL_AREA = "totalArea";
    
    // Weather channels
    public static final String CHANNEL_FORECAST_TEMP = "forecastTemp";
    public static final String CHANNEL_FORECAST_PRECIP = "forecastPrecip";
    public static final String CHANNEL_FORECAST_HUMIDITY = "forecastHumidity";
    public static final String CHANNEL_FORECAST_WIND = "forecastWind";
    public static final String CHANNEL_FORECAST_SOLAR = "forecastSolar";
    public static final String CHANNEL_FORECAST_ET = "forecastEt";
    
    // Usage channels
    public static final String CHANNEL_USAGE_TOTAL = "usageTotal";
    public static final String CHANNEL_USAGE_SAVINGS = "usageSavings";
    public static final String CHANNEL_USAGE_CURRENT = "usageCurrent";
    
    // Alert channels
    public static final String CHANNEL_ALERTS = "alerts";

    // Zone channels
    public static final String CHANNEL_ZONE_NAME = "zoneName";
    public static final String CHANNEL_ZONE_NUMBER = "zoneNumber";
    public static final String CHANNEL_ZONE_STATUS = "zoneStatus";
    public static final String CHANNEL_ZONE_ENABLED = "zoneEnabled";
    public static final String CHANNEL_ZONE_RUNTIME = "zoneRuntime";
    public static final String CHANNEL_ZONE_START = "zoneStart";
    public static final String CHANNEL_ZONE_STOP = "zoneStop";
    
    // Professional zone data channels
    public static final String CHANNEL_ZONE_AREA = "zoneArea";
    public static final String CHANNEL_ZONE_SOIL_TYPE = "zoneSoilType";
    public static final String CHANNEL_ZONE_CROP_TYPE = "zoneCropType";
    public static final String CHANNEL_ZONE_NOZZLE_TYPE = "zoneNozzleType";
    public static final String CHANNEL_ZONE_SLOPE_TYPE = "zoneSlopeType";
    public static final String CHANNEL_ZONE_SHADE_TYPE = "zoneShadeType";
    public static final String CHANNEL_ZONE_ROOT_DEPTH = "zoneRootDepth";
    public static final String CHANNEL_ZONE_EFFICIENCY = "zoneEfficiency";
    public static final String CHANNEL_ZONE_AVAILABLE_WATER = "zoneAvailableWater";
    public static final String CHANNEL_ADJUSTMENT_LEVEL_PREFIX = "adjustmentLevel";
    
    // Webhook event types
    public static final String EVENT_DEVICE_STATUS = "DEVICE_STATUS_EVENT";
    public static final String EVENT_ZONE_STATUS = "ZONE_STATUS_EVENT";
    public static final String EVENT_RAIN_DELAY = "RAIN_DELAY_EVENT";
    public static final String EVENT_WEATHER_INTELLIGENCE = "WEATHER_INTELLIGENCE_EVENT";
    public static final String EVENT_WATER_BUDGET = "WATER_BUDGET_EVENT";
    public static final String EVENT_SCHEDULE_STATUS = "SCHEDULE_STATUS_EVENT";
    public static final String EVENT_RAIN_SENSOR = "RAIN_SENSOR_DETECTION_EVENT";
    public static final String EVENT_ALERT = "ALERT_EVENT";
}
