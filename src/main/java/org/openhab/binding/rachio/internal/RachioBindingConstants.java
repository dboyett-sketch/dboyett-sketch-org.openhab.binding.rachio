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

    // Channel groups
    public static final String CHANNEL_GROUP_STATUS = "status";
    public static final String CHANNEL_GROUP_CONTROL = "control";
    public static final String CHANNEL_GROUP_MONITORING = "monitoring";
    public static final String CHANNEL_GROUP_PROFESSIONAL = "professional";

    // Bridge channels
    public static final String CHANNEL_STATUS = "status";
    public static final String CHANNEL_LAST_UPDATED = "last-updated";
    
    // Bridge command channels
    public static final String CHANNEL_RUN_ALL_ZONES = "run-all-zones";
    public static final String CHANNEL_RUN_NEXT_ZONE = "run-next-zone";
    public static final String CHANNEL_RAIN_DELAY = "rain-delay";
    public static final String CHANNEL_STOP_WATERING = "stop-watering";
    public static final String CHANNEL_LAST_COMMAND = "last-command";
    public static final String CHANNEL_LAST_COMMAND_TIME = "last-command-time";
    
    // Bridge monitoring channels
    public static final String CHANNEL_RATE_LIMIT_REMAINING = "rate-limit-remaining";
    public static final String CHANNEL_RATE_LIMIT_USAGE = "rate-limit-usage";
    public static final String CHANNEL_RATE_LIMIT_STATUS = "rate-limit-status";
    public static final String CHANNEL_RATE_LIMIT_RESET = "rate-limit-reset";
    
    // Bridge webhook channels
    public static final String CHANNEL_WEBHOOK_STATUS = "webhook-status";
    public static final String CHANNEL_WEBHOOK_LAST_EVENT = "webhook-last-event";

    // Device channels
    public static final String CHANNEL_DEVICE_NAME = "device-name";
    public static final String CHANNEL_DEVICE_STATUS = "device-status";
    public static final String CHANNEL_DEVICE_ONLINE = "device-online";
    public static final String CHANNEL_DEVICE_SERIAL = "device-serial";
    public static final String CHANNEL_DEVICE_MODEL = "device-model";
    public static final String CHANNEL_DEVICE_ZONE_COUNT = "device-zone-count";
    public static final String CHANNEL_DEVICE_LAST_HEARTBEAT = "device-last-heartbeat";
    
    // Zone channels
    public static final String CHANNEL_ZONE_NAME = "zone-name";
    public static final String CHANNEL_ZONE_ENABLED = "zone-enabled";
    public static final String CHANNEL_ZONE_RUN = "zone-run";
    public static final String CHANNEL_ZONE_STATUS = "zone-status";
    public static final String CHANNEL_ZONE_LAST_RUN = "zone-last-run";
    public static final String CHANNEL_ZONE_RUN_TIME = "zone-run-time";
    public static final String CHANNEL_ZONE_NUMBER = "zone-number";
    
    // Zone professional irrigation data channels
    public static final String CHANNEL_SOIL_TYPE = "soil-type";
    public static final String CHANNEL_CROP_TYPE = "crop-type";
    public static final String CHANNEL_CROP_COEFFICIENT = "crop-coefficient";
    public static final String CHANNEL_NOZZLE_TYPE = "nozzle-type";
    public static final String CHANNEL_NOZZLE_RATE = "nozzle-rate";
    public static final String CHANNEL_EFFICIENCY = "efficiency";
    public static final String CHANNEL_ROOT_DEPTH = "root-depth";
    public static final String CHANNEL_ZONE_AREA = "zone-area";
    public static final String CHANNEL_SLOPE_TYPE = "slope-type";
    public static final String CHANNEL_SHADE_TYPE = "shade-type";
    public static final String CHANNEL_WATER_ADJUSTMENT = "water-adjustment";
    public static final String CHANNEL_AVAILABLE_WATER = "available-water";
    public static final String CHANNEL_MOISTURE_PER_INCH = "moisture-per-inch";
    
    // Command constants
    public static final String COMMAND_START = "START";
    public static final String COMMAND_STOP = "STOP";
    public static final String COMMAND_COMPLETE = "COMPLETE";
    
    // Event types
    public static final String EVENT_ZONE_STATUS = "ZONE_STATUS_EVENT";
    public static final String EVENT_DEVICE_STATUS = "DEVICE_STATUS_EVENT";
    public static final String EVENT_SCHEDULE_STATUS = "SCHEDULE_STATUS_EVENT";
    public static final String EVENT_RAIN_DELAY = "RAIN_DELAY_EVENT";
    public static final String EVENT_WEATHER_INTEL = "WEATHER_INTELLIGENCE_EVENT";
    public static final String EVENT_WATER_BUDGET = "WATER_BUDGET_EVENT";
    public static final String EVENT_RAIN_SENSOR = "RAIN_SENSOR_DETECTION_EVENT";
    
    // Status constants
    public static final String STATUS_ONLINE = "ONLINE";
    public static final String STATUS_OFFLINE = "OFFLINE";
    public static final String STATUS_SLEEP = "SLEEP_MODE";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_IDLE = "IDLE";
    public static final String STATUS_STOPPED = "STOPPED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    
    // Configuration properties
    public static final String CONFIG_API_KEY = "apiKey";
    public static final String CONFIG_DEVICE_ID = "deviceId";
    public static final String CONFIG_ZONE_ID = "zoneId";
    public static final String CONFIG_WEBHOOK_URL = "webhookUrl";
    public static final String CONFIG_WEBHOOK_ENABLED = "webhookEnabled";
    public static final String CONFIG_POLL_INTERVAL = "pollInterval";
    public static final String CONFIG_ZONE_NAME = "name";
    public static final String CONFIG_ZONE_ENABLED = "enabled";
    
    // Property constants
    public static final String PROPERTY_DEVICE_ID = "deviceId";
    public static final String PROPERTY_DEVICE_NAME = "deviceName";
    public static final String PROPERTY_DEVICE_SERIAL = "serialNumber";
    public static final String PROPERTY_DEVICE_MODEL = "model";
    public static final String PROPERTY_ZONE_ID = "zoneId";
    public static final String PROPERTY_ZONE_NUMBER = "zoneNumber";
    public static final String PROPERTY_ZONE_NAME = "zoneName";
    public static final String PROPERTY_PERSON_ID = "personId";
    public static final String PROPERTY_PERSON_NAME = "personName";
    
    // Webhook constants
    public static final String WEBHOOK_PATH = "/rachio/webhook";
    public static final int WEBHOOK_DEFAULT_PORT = 8080;
    public static final String WEBHOOK_SIGNATURE_HEADER = "X-Rachio-Signature";
    
    // API constants
    public static final String API_BASE_URL = "https://api.rach.io/1/public";
    public static final String API_AUTH_HEADER = "Authorization";
    public static final String API_AUTH_PREFIX = "Bearer ";
    public static final int API_RATE_LIMIT_DEFAULT = 60;
    public static final int API_RATE_LIMIT_RESET_HOURS = 1;
    
    // Thing properties
    public static final String THING_PROPERTY_ID = "id";
    public static final String THING_PROPERTY_NAME = "name";
    public static final String THING_PROPERTY_MODEL = "model";
    public static final String THING_PROPERTY_SERIAL = "serial";
    public static final String THING_PROPERTY_MAC = "macAddress";
    public static final String THING_PROPERTY_ZONES = "zones";
    public static final String THING_PROPERTY_ONLINE = "online";
    
    // Discovery constants
    public static final int DISCOVERY_TIMEOUT_SECONDS = 10;
    public static final String DISCOVERY_THING_TYPE_HINT = "thingType";
    public static final String DISCOVERY_DEVICE = "device";
    public static final String DISCOVERY_ZONE = "zone";
    
    // Unit constants
    public static final String UNIT_SECONDS = "s";
    public static final String UNIT_MINUTES = "min";
    public static final String UNIT_HOURS = "h";
    public static final String UNIT_INCHES = "in";
    public static final String UNIT_SQUARE_FEET = "ft²";
    public static final String UNIT_PERCENT = "%";
    public static final String UNIT_INCHES_PER_HOUR = "in/h";
    
    // Soil type constants
    public static final String SOIL_CLAY = "Clay";
    public static final String SOIL_LOAM = "Loam";
    public static final String SOIL_SAND = "Sand";
    public static final String SOIL_SILT = "Silt";
    public static final String SOIL_CLAY_LOAM = "Clay Loam";
    public static final String SOIL_SILT_LOAM = "Silt Loam";
    public static final String SOIL_SANDY_LOAM = "Sandy Loam";
    public static final String SOIL_LOAMY_SAND = "Loamy Sand";
    public static final String SOIL_SANDY_CLAY = "Sandy Clay";
    public static final String SOIL_SILTY_CLAY = "Silty Clay";
    public static final String SOIL_SANDY_CLAY_LOAM = "Sandy Clay Loam";
    public static final String SOIL_SILTY_CLAY_LOAM = "Silty Clay Loam";
    
    // Crop type constants
    public static final String CROP_TURFGRASS = "Turfgrass";
    public static final String CROP_FLOWERS = "Flowers";
    public static final String CROP_VEGETABLES = "Vegetables";
    public static final String CROP_TREES = "Trees";
    public static final String CROP_SHRUBS = "Shrubs";
    public static final String CROP_GROUNDCOVER = "Groundcover";
    public static final String CROP_VINES = "Vines";
    public static final String CROP_FRUIT_TREES = "Fruit Trees";
    public static final String CROP_BERRIES = "Berries";
    public static final String CROP_HERBS = "Herbs";
    
    // Nozzle type constants
    public static final String NOZZLE_ROTARY = "Rotary";
    public static final String NOZZLE_SPRAY = "Spray";
    public static final String NOZZLE_DRIP = "Drip";
    public static final String NOZZLE_BUBBLER = "Bubbler";
    public static final String NOZZLE_MICRO_SPRAY = "Micro Spray";
    public static final String NOZZLE_ROTOR = "Rotor";
    public static final String NOZZLE_IMPACT = "Impact";
    public static final String NOZZLE_STREAM = "Stream";
    public static final String NOZZLE_MISTER = "Mister";
    
    // Slope type constants
    public static final String SLOPE_FLAT = "Flat";
    public static final String SLOPE_GENTLE = "Gentle";
    public static final String SLOPE_MODERATE = "Moderate";
    public static final String SLOPE_STEEP = "Steep";
    public static final String SLOPE_VERY_STEEP = "Very Steep";
    
    // Shade type constants
    public static final String SHADE_FULL_SUN = "Full Sun";
    public static final String SHADE_PART_SUN = "Part Sun";
    public static final String SHADE_PART_SHADE = "Part Shade";
    public static final String SHADE_FULL_SHADE = "Full Shade";
    public static final String SHADE_DENSE_SHADE = "Dense Shade";
    
    // Channel type UIDs (for dynamic channel creation)
    public static final String CHANNEL_TYPE_STRING = "String";
    public static final String CHANNEL_TYPE_NUMBER = "Number";
    public static final String CHANNEL_TYPE_SWITCH = "Switch";
    public static final String CHANNEL_TYPE_DATETIME = "DateTime";
    public static final String CHANNEL_TYPE_DIMMER = "Dimmer";
    public static final String CHANNEL_TYPE_CONTACT = "Contact";
    public static final String CHANNEL_TYPE_ROLLERSHUTTER = "Rollershutter";
    
    // Channel type suffixes for unit channels
    public static final String CHANNEL_TYPE_NUMBER_DIMENSIONLESS = "Number:Dimensionless";
    public static final String CHANNEL_TYPE_NUMBER_LENGTH = "Number:Length";
    public static final String CHANNEL_TYPE_NUMBER_AREA = "Number:Area";
    public static final String CHANNEL_TYPE_NUMBER_TIME = "Number:Time";
    public static final String CHANNEL_TYPE_NUMBER_TEMPERATURE = "Number:Temperature";
    public static final String CHANNEL_TYPE_NUMBER_PRESSURE = "Number:Pressure";
    public static final String CHANNEL_TYPE_NUMBER_SPEED = "Number:Speed";
    public static final String CHANNEL_TYPE_NUMBER_INTENSITY = "Number:Intensity";
    
    // Default values
    public static final int DEFAULT_RUN_TIME = 300; // 5 minutes in seconds
    public static final int DEFAULT_POLL_INTERVAL = 300; // 5 minutes in seconds
    public static final int DEFAULT_RAIN_DELAY = 0; // No rain delay
    public static final double DEFAULT_CROP_COEFFICIENT = 0.8;
    public static final double DEFAULT_EFFICIENCY = 75.0;
    public static final double DEFAULT_ROOT_DEPTH = 6.0; // inches
    public static final double DEFAULT_NOZZLE_RATE = 1.0; // inches per hour
    public static final int DEFAULT_WATER_ADJUSTMENT = 3; // Medium adjustment
    
    // Validation constants
    public static final int MIN_RUN_TIME = 1; // 1 second minimum
    public static final int MAX_RUN_TIME = 10800; // 3 hours maximum
    public static final int MIN_RAIN_DELAY = 0; // 0 hours minimum
    public static final int MAX_RAIN_DELAY = 168; // 7 days maximum
    public static final int MIN_POLL_INTERVAL = 30; // 30 seconds minimum
    public static final int MAX_POLL_INTERVAL = 3600; // 1 hour maximum
    
    // Logging constants
    public static final String LOGGER_API = "org.openhab.binding.rachio.api";
    public static final String LOGGER_HANDLER = "org.openhab.binding.rachio.handler";
    public static final String LOGGER_DISCOVERY = "org.openhab.binding.rachio.discovery";
    public static final String LOGGER_WEBHOOK = "org.openhab.binding.rachio.webhook";
    
    // List of all config parameters
    public static final String[] BRIDGE_CONFIG_PARAMETERS = {
        CONFIG_API_KEY,
        CONFIG_WEBHOOK_URL,
        CONFIG_WEBHOOK_ENABLED,
        CONFIG_POLL_INTERVAL
    };
    
    public static final String[] DEVICE_CONFIG_PARAMETERS = {
        CONFIG_DEVICE_ID
    };
    
    public static final String[] ZONE_CONFIG_PARAMETERS = {
        CONFIG_ZONE_ID,
        CONFIG_ZONE_NAME,
        CONFIG_ZONE_ENABLED,
        CONFIG_DEVICE_ID
    };
}
