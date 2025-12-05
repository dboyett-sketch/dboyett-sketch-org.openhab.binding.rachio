package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

import java.time.Instant;

/**
 * DTO for Rachio Webhook Event
 * Based on: Rachio Webhook Event Format
 * https://rachio.readme.io/docs/webhooks
 * 
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioWebhookEvent {
    // Event metadata
    @SerializedName("eventType")
    public String eventType;
    
    @SerializedName("deviceId")
    public String deviceId;
    
    @SerializedName("timestamp")
    public Instant timestamp;
    
    @SerializedName("externalId")
    public @Nullable String externalId;
    
    @SerializedName("webhookId")
    public @Nullable String webhookId;
    
    // Event-specific data (varies by eventType)
    @SerializedName("zone")
    public @Nullable ZoneEventData zone;
    
    @SerializedName("device")
    public @Nullable DeviceEventData device;
    
    @SerializedName("schedule")
    public @Nullable ScheduleEventData schedule;
    
    @SerializedName("rainDelay")
    public @Nullable RainDelayEventData rainDelay;
    
    @SerializedName("weatherIntelligence")
    public @Nullable WeatherIntelEventData weatherIntelligence;
    
    @SerializedName("waterBudget")
    public @Nullable WaterBudgetEventData waterBudget;
    
    @SerializedName("rainSensor")
    public @Nullable RainSensorEventData rainSensor;
    
    // Nested event data classes
    
    public static class ZoneEventData {
        @SerializedName("id")
        public String id;
        
        @SerializedName("zoneNumber")
        public int zoneNumber;
        
        @SerializedName("name")
        public String name;
        
        @SerializedName("state")
        public String state; // STARTED, STOPPED, COMPLETED, PAUSED, RESUMED
        
        @SerializedName("duration")
        public int duration; // seconds
        
        @SerializedName("startDate")
        public @Nullable Instant startDate;
        
        @SerializedName("endDate")
        public @Nullable Instant endDate;
        
        @SerializedName("totalDuration")
        public int totalDuration; // seconds
        
        @SerializedName("totalWaterUsage")
        public double totalWaterUsage; // gallons
    }
    
    public static class DeviceEventData {
        @SerializedName("id")
        public String id;
        
        @SerializedName("name")
        public String name;
        
        @SerializedName("status")
        public String status; // ONLINE, OFFLINE, SLEEP_MODE
        
        @SerializedName("previousStatus")
        public @Nullable String previousStatus;
        
        @SerializedName("reason")
        public @Nullable String reason;
        
        @SerializedName("lastConnectivity")
        public @Nullable Instant lastConnectivity;
    }
    
    public static class ScheduleEventData {
        @SerializedName("id")
        public String id;
        
        @SerializedName("name")
        public String name;
        
        @SerializedName("scheduleType")
        public String scheduleType; // FIXED, FLEX, FLEX_DAILY
        
        @SerializedName("state")
        public String state; // STARTED, STOPPED, SKIPPED, COMPLETED
        
        @SerializedName("startDate")
        public @Nullable Instant startDate;
        
        @SerializedName("endDate")
        public @Nullable Instant endDate;
        
        @SerializedName("duration")
        public int duration; // seconds
        
        @SerializedName("totalDuration")
        public int totalDuration; // seconds
        
        @SerializedName("totalWaterUsage")
        public double totalWaterUsage; // gallons
        
        @SerializedName("skipReason")
        public @Nullable String skipReason;
        
        @SerializedName("skipDate")
        public @Nullable Instant skipDate;
    }
    
    public static class RainDelayEventData {
        @SerializedName("id")
        public String deviceId;
        
        @SerializedName("duration")
        public int duration; // seconds
        
        @SerializedName("startDate")
        public Instant startDate;
        
        @SerializedName("endDate")
        public Instant endDate;
        
        @SerializedName("cancelled")
        public boolean cancelled;
        
        @SerializedName("cancelledDate")
        public @Nullable Instant cancelledDate;
    }
    
    public static class WeatherIntelEventData {
        @SerializedName("deviceId")
        public String deviceId;
        
        @SerializedName("type")
        public String type; // PRECIPITATION, FREEZE, HIGH_WIND, ET
        
        @SerializedName("value")
        public double value;
        
        @SerializedName("unit")
        public String unit;
        
        @SerializedName("threshold")
        public double threshold;
        
        @SerializedName("action")
        public String action; // SKIP, DELAY, REDUCE
        
        @SerializedName("actionValue")
        public double actionValue;
        
        @SerializedName("forecastDate")
        public Instant forecastDate;
        
        @SerializedName("effectiveDate")
        public Instant effectiveDate;
    }
    
    public static class WaterBudgetEventData {
        @SerializedName("deviceId")
        public String deviceId;
        
        @SerializedName("budget")
        public double budget; // percentage 0-200
        
        @SerializedName("previousBudget")
        public double previousBudget;
        
        @SerializedName("effectiveDate")
        public Instant effectiveDate;
        
        @SerializedName("reason")
        public @Nullable String reason;
    }
    
    public static class RainSensorEventData {
        @SerializedName("deviceId")
        public String deviceId;
        
        @SerializedName("state")
        public String state; // WET, DRY
        
        @SerializedName("previousState")
        public @Nullable String previousState;
        
        @SerializedName("detectionDate")
        public Instant detectionDate;
        
        @SerializedName("duration")
        public int duration; // seconds sensor has been in current state
    }
    
    // Helper methods
    
    /**
     * Get event type as enum
     */
    public EventType getEventTypeEnum() {
        try {
            return EventType.valueOf(eventType);
        } catch (IllegalArgumentException e) {
            return EventType.UNKNOWN;
        }
    }
    
    /**
     * Check if this is a zone status event
     */
    public boolean isZoneStatusEvent() {
        return EventType.ZONE_STATUS_EVENT.name().equals(eventType);
    }
    
    /**
     * Check if this is a device status event
     */
    public boolean isDeviceStatusEvent() {
        return EventType.DEVICE_STATUS_EVENT.name().equals(eventType);
    }
    
    /**
     * Check if this is a schedule status event
     */
    public boolean isScheduleStatusEvent() {
        return EventType.SCHEDULE_STATUS_EVENT.name().equals(eventType);
    }
    
    /**
     * Check if this is a rain delay event
     */
    public boolean isRainDelayEvent() {
        return EventType.RAIN_DELAY_EVENT.name().equals(eventType);
    }
    
    /**
     * Check if this is a weather intelligence event
     */
    public boolean isWeatherIntelEvent() {
        return EventType.WEATHER_INTELLIGENCE_EVENT.name().equals(eventType);
    }
    
    /**
     * Check if this is a water budget event
     */
    public boolean isWaterBudgetEvent() {
        return EventType.WATER_BUDGET_EVENT.name().equals(eventType);
    }
    
    /**
     * Check if this is a rain sensor event
     */
    public boolean isRainSensorEvent() {
        return EventType.RAIN_SENSOR_DETECTION_EVENT.name().equals(eventType);
    }
    
    /**
     * Get zone ID from event (if applicable)
     */
    public @Nullable String getZoneId() {
        if (zone != null) {
            return zone.id;
        }
        return null;
    }
    
    /**
     * Get zone number from event (if applicable)
     */
    public @Nullable Integer getZoneNumber() {
        if (zone != null) {
            return zone.zoneNumber;
        }
        return null;
    }
    
    /**
     * Get zone state from event (if applicable)
     */
    public @Nullable String getZoneState() {
        if (zone != null) {
            return zone.state;
        }
        return null;
    }
    
    /**
     * Get device status from event (if applicable)
     */
    public @Nullable String getDeviceStatus() {
        if (device != null) {
            return device.status;
        }
        return null;
    }
    
    /**
     * Get schedule ID from event (if applicable)
     */
    public @Nullable String getScheduleId() {
        if (schedule != null) {
            return schedule.id;
        }
        return null;
    }
    
    /**
     * Get schedule state from event (if applicable)
     */
    public @Nullable String getScheduleState() {
        if (schedule != null) {
            return schedule.state;
        }
        return null;
    }
    
    /**
     * Get rain delay duration in hours (if applicable)
     */
    public @Nullable Double getRainDelayHours() {
        if (rainDelay != null) {
            return rainDelay.duration / 3600.0;
        }
        return null;
    }
    
    /**
     * Get water budget percentage (if applicable)
     */
    public @Nullable Double getWaterBudget() {
        if (waterBudget != null) {
            return waterBudget.budget;
        }
        return null;
    }
    
    /**
     * Get rain sensor state (if applicable)
     */
    public @Nullable String getRainSensorState() {
        if (rainSensor != null) {
            return rainSensor.state;
        }
        return null;
    }
    
    /**
     * Get event summary
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Event: ").append(eventType).append(" on device ").append(deviceId);
        
        if (zone != null) {
            sb.append(", Zone: ").append(zone.name).append(" (").append(zone.state).append(")");
        }
        
        if (device != null) {
            sb.append(", Device: ").append(device.status);
        }
        
        if (schedule != null) {
            sb.append(", Schedule: ").append(schedule.name).append(" (").append(schedule.state).append(")");
        }
        
        if (rainDelay != null) {
            sb.append(", Rain Delay: ").append(rainDelay.duration).append("s");
            if (rainDelay.cancelled) {
                sb.append(" (cancelled)");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Get event details for logging
     */
    public java.util.Map<String, Object> getDetails() {
        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("eventType", eventType);
        details.put("deviceId", deviceId);
        details.put("timestamp", timestamp.toString());
        
        if (zone != null) {
            java.util.Map<String, Object> zoneDetails = new java.util.HashMap<>();
            zoneDetails.put("id", zone.id);
            zoneDetails.put("name", zone.name);
            zoneDetails.put("zoneNumber", zone.zoneNumber);
            zoneDetails.put("state", zone.state);
            zoneDetails.put("duration", zone.duration);
            details.put("zone", zoneDetails);
        }
        
        if (device != null) {
            java.util.Map<String, Object> deviceDetails = new java.util.HashMap<>();
            deviceDetails.put("id", device.id);
            deviceDetails.put("name", device.name);
            deviceDetails.put("status", device.status);
            deviceDetails.put("previousStatus", device.previousStatus);
            details.put("device", deviceDetails);
        }
        
        if (schedule != null) {
            java.util.Map<String, Object> scheduleDetails = new java.util.HashMap<>();
            scheduleDetails.put("id", schedule.id);
            scheduleDetails.put("name", schedule.name);
            scheduleDetails.put("state", schedule.state);
            scheduleDetails.put("duration", schedule.duration);
            details.put("schedule", scheduleDetails);
        }
        
        if (rainDelay != null) {
            java.util.Map<String, Object> rainDelayDetails = new java.util.HashMap<>();
            rainDelayDetails.put("duration", rainDelay.duration);
            rainDelayDetails.put("startDate", rainDelay.startDate.toString());
            rainDelayDetails.put("endDate", rainDelay.endDate.toString());
            rainDelayDetails.put("cancelled", rainDelay.cancelled);
            details.put("rainDelay", rainDelayDetails);
        }
        
        return details;
    }
    
    /**
     * Check if zone started event
     */
    public boolean isZoneStarted() {
        return isZoneStatusEvent() && zone != null && "STARTED".equals(zone.state);
    }
    
    /**
     * Check if zone stopped event
     */
    public boolean isZoneStopped() {
        return isZoneStatusEvent() && zone != null && "STOPPED".equals(zone.state);
    }
    
    /**
     * Check if zone completed event
     */
    public boolean isZoneCompleted() {
        return isZoneStatusEvent() && zone != null && "COMPLETED".equals(zone.state);
    }
    
    /**
     * Check if device came online
     */
    public boolean isDeviceOnline() {
        return isDeviceStatusEvent() && device != null && "ONLINE".equals(device.status);
    }
    
    /**
     * Check if device went offline
     */
    public boolean isDeviceOffline() {
        return isDeviceStatusEvent() && device != null && "OFFLINE".equals(device.status);
    }
    
    /**
     * Check if rain delay started
     */
    public boolean isRainDelayStarted() {
        return isRainDelayEvent() && rainDelay != null && !rainDelay.cancelled;
    }
    
    /**
     * Check if rain delay cancelled
     */
    public boolean isRainDelayCancelled() {
        return isRainDelayEvent() && rainDelay != null && rainDelay.cancelled;
    }
    
    /**
     * Check if schedule started
     */
    public boolean isScheduleStarted() {
        return isScheduleStatusEvent() && schedule != null && "STARTED".equals(schedule.state);
    }
    
    /**
     * Check if schedule skipped
     */
    public boolean isScheduleSkipped() {
        return isScheduleStatusEvent() && schedule != null && "SKIPPED".equals(schedule.state);
    }
    
    /**
     * Event type enum
     */
    public enum EventType {
        ZONE_STATUS_EVENT,
        DEVICE_STATUS_EVENT,
        SCHEDULE_STATUS_EVENT,
        RAIN_DELAY_EVENT,
        WEATHER_INTELLIGENCE_EVENT,
        WATER_BUDGET_EVENT,
        RAIN_SENSOR_DETECTION_EVENT,
        UNKNOWN
    }
}
