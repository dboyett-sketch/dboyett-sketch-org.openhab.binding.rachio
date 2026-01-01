package org.openhab.binding.rachio.internal.api;

import java.time.Instant;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

/**
 * DTO representing a Rachio event from webhooks or API
 * Base class for all Rachio event types
 * 
 * @author dboyett-sketch
 */
public class RachioEvent {

    @SerializedName("event")
    public String event;

    @SerializedName("deviceId")
    public String deviceId;

    @SerializedName("timestamp")
    public long timestamp;

    @SerializedName("data")
    public Map<String, Object> data;

    @SerializedName("zoneId")
    public String zoneId;

    @SerializedName("userId")
    public String userId;

    @SerializedName("subscriptionId")
    public String subscriptionId;

    @SerializedName("eventId")
    public String eventId;

    /**
     * Default constructor for Gson
     */
    public RachioEvent() {
        // Default constructor
    }

    /**
     * Constructor with required fields
     */
    public RachioEvent(String event, String deviceId, long timestamp) {
        this.event = event;
        this.deviceId = deviceId;
        this.timestamp = timestamp;
    }

    /**
     * Get the event type
     */
    public String getEvent() {
        return event;
    }

    /**
     * Get the device ID
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Get the timestamp as Instant
     */
    public Instant getTimestampAsInstant() {
        return Instant.ofEpochSecond(timestamp);
    }

    /**
     * Get the timestamp as milliseconds
     */
    public long getTimestampMillis() {
        return timestamp * 1000L;
    }

    /**
     * Check if this is a zone-related event
     */
    public boolean isZoneEvent() {
        return zoneId != null && !zoneId.isEmpty();
    }

    /**
     * Check if this is a device-related event
     */
    public boolean isDeviceEvent() {
        return deviceId != null && !deviceId.isEmpty() && !isZoneEvent();
    }

    /**
     * Check if this is a system event
     */
    public boolean isSystemEvent() {
        return deviceId == null || deviceId.isEmpty();
    }

    /**
     * Get the zone ID if this is a zone event
     */
    public String getZoneId() {
        return zoneId;
    }

    /**
     * Get event data as a map
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * Get a specific value from event data
     */
    public Object getDataValue(String key) {
        return data != null ? data.get(key) : null;
    }

    /**
     * Get a string value from event data
     */
    public String getDataString(String key) {
        Object value = getDataValue(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Get an integer value from event data
     */
    public Integer getDataInt(String key) {
        Object value = getDataValue(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Get a long value from event data
     */
    public Long getDataLong(String key) {
        Object value = getDataValue(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Get a boolean value from event data
     */
    public Boolean getDataBoolean(String key) {
        Object value = getDataValue(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return null;
    }

    /**
     * Check if event has data
     */
    public boolean hasData() {
        return data != null && !data.isEmpty();
    }

    /**
     * Check if event is valid
     */
    public boolean isValid() {
        return event != null && !event.isEmpty() && timestamp > 0;
    }

    /**
     * Get event age in seconds
     */
    public long getAgeInSeconds() {
        long currentTime = Instant.now().getEpochSecond();
        return currentTime - timestamp;
    }

    /**
     * Check if event is recent (within specified seconds)
     */
    public boolean isRecent(int seconds) {
        return getAgeInSeconds() <= seconds;
    }

    /**
     * Get event category based on event type
     */
    public EventCategory getCategory() {
        if (event == null) {
            return EventCategory.UNKNOWN;
        }

        if (event.contains("ZONE_")) {
            return EventCategory.ZONE;
        } else if (event.contains("DEVICE_")) {
            return EventCategory.DEVICE;
        } else if (event.contains("SCHEDULE_")) {
            return EventCategory.SCHEDULE;
        } else if (event.contains("RAIN_")) {
            return EventCategory.WEATHER;
        } else if (event.contains("ALERT_")) {
            return EventCategory.ALERT;
        } else {
            return EventCategory.SYSTEM;
        }
    }

    /**
     * Get event priority (higher = more important)
     */
    public int getPriority() {
        EventCategory category = getCategory();

        switch (category) {
            case ALERT:
                return 100;
            case DEVICE:
                return 80;
            case ZONE:
                return 60;
            case SCHEDULE:
                return 40;
            case WEATHER:
                return 30;
            case SYSTEM:
                return 20;
            default:
                return 10;
        }
    }

    /**
     * Check if this is a watering-related event
     */
    public boolean isWateringEvent() {
        return event != null && (event.contains("START") || event.contains("STOP") || event.contains("WATER"));
    }

    /**
     * Check if this is an error/alert event
     */
    public boolean isAlertEvent() {
        return event != null && (event.contains("ALERT") || event.contains("ERROR") || event.contains("FAILURE"));
    }

    /**
     * Get human-readable event description
     */
    public String getDescription() {
        if (event == null) {
            return "Unknown Event";
        }

        switch (event) {
            case "ZONE_STARTED":
                return "Zone Watering Started";
            case "ZONE_STOPPED":
                return "Zone Watering Stopped";
            case "ZONE_COMPLETED":
                return "Zone Watering Completed";
            case "ZONE_PAUSED":
                return "Zone Watering Paused";
            case "DEVICE_STATUS":
                return "Device Status Changed";
            case "DEVICE_CONNECTED":
                return "Device Connected";
            case "DEVICE_DISCONNECTED":
                return "Device Disconnected";
            case "RAIN_DELAY":
                return "Rain Delay Activated";
            case "RAIN_DELAY_ENDED":
                return "Rain Delay Ended";
            case "SCHEDULE_STARTED":
                return "Schedule Started";
            case "SCHEDULE_COMPLETED":
                return "Schedule Completed";
            case "ALERT_LEAK":
                return "Leak Alert";
            case "ALERT_OFFLINE":
                return "Device Offline Alert";
            default:
                return event.replace("_", " ");
        }
    }

    /**
     * Create a summary string for logging
     */
    public String toSummaryString() {
        StringBuilder summary = new StringBuilder();
        summary.append("Event[type=").append(event);

        if (deviceId != null) {
            summary.append(", device=").append(deviceId);
        }

        if (zoneId != null) {
            summary.append(", zone=").append(zoneId);
        }

        summary.append(", time=").append(getTimestampAsInstant());
        summary.append("]");

        return summary.toString();
    }

    @Override
    public String toString() {
        return "RachioEvent{" + "event='" + event + '\'' + ", deviceId='" + deviceId + '\'' + ", timestamp=" + timestamp
                + ", zoneId='" + zoneId + '\'' + ", data=" + (data != null ? data.size() : 0) + " entries" + '}';
    }

    /**
     * Event categories
     */
    public enum EventCategory {
        ZONE,
        DEVICE,
        SCHEDULE,
        WEATHER,
        ALERT,
        SYSTEM,
        UNKNOWN
    }
}
