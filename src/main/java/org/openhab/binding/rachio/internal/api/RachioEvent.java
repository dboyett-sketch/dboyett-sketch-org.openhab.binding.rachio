package org.openhab.binding.rachio.internal.api;

import java.time.Instant;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for Rachio events
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioEvent {

    // Event type constants
    public static final String TYPE_DEVICE_STATUS_ONLINE = "DEVICE_STATUS_EVENT_ONLINE";
    public static final String TYPE_DEVICE_STATUS_OFFLINE = "DEVICE_STATUS_EVENT_OFFLINE";
    public static final String TYPE_DEVICE_STATUS_SLEEP = "DEVICE_STATUS_EVENT_SLEEP";
    
    public static final String TYPE_ZONE_STATUS_STARTED = "ZONE_STATUS_EVENT_STARTED";
    public static final String TYPE_ZONE_STATUS_STOPPED = "ZONE_STATUS_EVENT_STOPPED";
    public static final String TYPE_ZONE_STATUS_COMPLETED = "ZONE_STATUS_EVENT_COMPLETED";
    
    public static final String TYPE_SCHEDULE_STATUS_STARTED = "SCHEDULE_STATUS_EVENT_STARTED";
    public static final String TYPE_SCHEDULE_STATUS_COMPLETED = "SCHEDULE_STATUS_EVENT_COMPLETED";
    public static final String TYPE_SCHEDULE_STATUS_SKIPPED = "SCHEDULE_STATUS_EVENT_SKIPPED";
    
    public static final String TYPE_RAIN_DELAY = "RAIN_DELAY_EVENT";
    public static final String TYPE_WEATHER_INTEL_SKIP = "WEATHER_INTELLIGENCE_SKIP_EVENT";
    public static final String TYPE_RAIN_SENSOR_DETECTION = "RAIN_SENSOR_DETECTION_EVENT";
    public static final String TYPE_WATER_BUDGET = "WATER_BUDGET_EVENT";
    
    // Event fields
    @SerializedName("id")
    public @Nullable String id;
    
    @SerializedName("type")
    public @Nullable String type;
    
    @SerializedName("deviceId")
    public @Nullable String deviceId;
    
    @SerializedName("zoneId")
    public @Nullable String zoneId;
    
    @SerializedName("scheduleId")
    public @Nullable String scheduleId;
    
    @SerializedName("userId")
    public @Nullable String userId;
    
    @SerializedName("createDate")
    public @Nullable Instant createDate;
    
    @SerializedName("eventDate")
    public @Nullable Instant eventDate;
    
    @SerializedName("summary")
    public @Nullable String summary;
    
    @SerializedName("category")
    public @Nullable String category;
    
    @SerializedName("subCategory")
    public @Nullable String subCategory;
    
    @SerializedName("deviceName")
    public @Nullable String deviceName;
    
    @SerializedName("zoneName")
    public @Nullable String zoneName;
    
    @SerializedName("scheduleName")
    public @Nullable String scheduleName;
    
    @SerializedName("duration")
    public @Nullable Integer duration;
    
    @SerializedName("totalDuration")
    public @Nullable Integer totalDuration;
    
    @SerializedName("cycleSoak")
    public @Nullable Boolean cycleSoak;
    
    @SerializedName("cycleSoakStatus")
    public @Nullable String cycleSoakStatus;
    
    @SerializedName("smartCycle")
    public @Nullable Boolean smartCycle;
    
    @SerializedName("smartCycleStatus")
    public @Nullable String smartCycleStatus;
    
    @SerializedName("flexSchedule")
    public @Nullable Boolean flexSchedule;
    
    @SerializedName("flexScheduleStatus")
    public @Nullable String flexScheduleStatus;
    
    // Weather intelligence fields
    @SerializedName("weatherIntelType")
    public @Nullable String weatherIntelType;
    
    @SerializedName("weatherIntelSkipReason")
    public @Nullable String weatherIntelSkipReason;
    
    @SerializedName("forecastPrecipitation")
    public @Nullable Double forecastPrecipitation;
    
    @SerializedName("forecastTemperature")
    public @Nullable Double forecastTemperature;
    
    @SerializedName("forecastWindSpeed")
    public @Nullable Double forecastWindSpeed;
    
    @SerializedName("forecastHumidity")
    public @Nullable Double forecastHumidity;
    
    @SerializedName("forecastSolarRadiation")
    public @Nullable Double forecastSolarRadiation;
    
    @SerializedName("forecastEvapotranspiration")
    public @Nullable Double forecastEvapotranspiration;
    
    // Rain sensor fields
    @SerializedName("rainSensorType")
    public @Nullable String rainSensorType;
    
    @SerializedName("rainSensorDuration")
    public @Nullable Integer rainSensorDuration;
    
    @SerializedName("rainSensorStatus")
    public @Nullable String rainSensorStatus;
    
    // Water budget fields
    @SerializedName("waterBudgetAdjustment")
    public @Nullable Double waterBudgetAdjustment;
    
    @SerializedName("waterBudgetPercentage")
    public @Nullable Double waterBudgetPercentage;
    
    @SerializedName("waterBudgetReason")
    public @Nullable String waterBudgetReason;
    
    // Zone run details
    @SerializedName("zoneNumber")
    public @Nullable Integer zoneNumber;
    
    @SerializedName("zoneRunOrder")
    public @Nullable Integer zoneRunOrder;
    
    @SerializedName("zoneRunTime")
    public @Nullable Integer zoneRunTime;
    
    @SerializedName("zoneRemainingTime")
    public @Nullable Integer zoneRemainingTime;
    
    @SerializedName("zoneStartDate")
    public @Nullable Instant zoneStartDate;
    
    @SerializedName("zoneEndDate")
    public @Nullable Instant zoneEndDate;
    
    @SerializedName("zoneCompletedDate")
    public @Nullable Instant zoneCompletedDate;
    
    // Professional irrigation data
    @SerializedName("zoneSoilType")
    public @Nullable String zoneSoilType;
    
    @SerializedName("zoneCropType")
    public @Nullable String zoneCropType;
    
    @SerializedName("zoneCropCoefficient")
    public @Nullable Double zoneCropCoefficient;
    
    @SerializedName("zoneNozzleType")
    public @Nullable String zoneNozzleType;
    
    @SerializedName("zoneNozzleRate")
    public @Nullable Double zoneNozzleRate;
    
    @SerializedName("zoneSlopeType")
    public @Nullable String zoneSlopeType;
    
    @SerializedName("zoneShadeType")
    public @Nullable String zoneShadeType;
    
    @SerializedName("zoneEfficiency")
    public @Nullable Double zoneEfficiency;
    
    @SerializedName("zoneRootDepth")
    public @Nullable Double zoneRootDepth;
    
    @SerializedName("zoneArea")
    public @Nullable Double zoneArea;
    
    // Device status details
    @SerializedName("deviceStatus")
    public @Nullable String deviceStatus;
    
    @SerializedName("devicePaused")
    public @Nullable Boolean devicePaused;
    
    @SerializedName("deviceRainDelayHours")
    public @Nullable Integer deviceRainDelayHours;
    
    @SerializedName("deviceRainDelayEndDate")
    public @Nullable Instant deviceRainDelayEndDate;
    
    // Schedule details
    @SerializedName("scheduleType")
    public @Nullable String scheduleType;
    
    @SerializedName("scheduleTotalDuration")
    public @Nullable Integer scheduleTotalDuration;
    
    @SerializedName("scheduleTotalZones")
    public @Nullable Integer scheduleTotalZones;
    
    @SerializedName("scheduleCompletedZones")
    public @Nullable Integer scheduleCompletedZones;
    
    @SerializedName("scheduleSkippedZones")
    public @Nullable Integer scheduleSkippedZones;
    
    // Metadata
    @SerializedName("metadata")
    public @Nullable Map<String, Object> metadata;
    
    @SerializedName("links")
    public @Nullable Map<String, String> links;
    
    /**
     * Check if this is a device status event
     */
    public boolean isDeviceStatusEvent() {
        return type != null && type.startsWith("DEVICE_STATUS_EVENT_");
    }
    
    /**
     * Check if this is a zone status event
     */
    public boolean isZoneStatusEvent() {
        return type != null && type.startsWith("ZONE_STATUS_EVENT_");
    }
    
    /**
     * Check if this is a schedule status event
     */
    public boolean isScheduleStatusEvent() {
        return type != null && type.startsWith("SCHEDULE_STATUS_EVENT_");
    }
    
    /**
     * Check if this is a rain delay event
     */
    public boolean isRainDelayEvent() {
        return TYPE_RAIN_DELAY.equals(type);
    }
    
    /**
     * Check if this is a weather intelligence event
     */
    public boolean isWeatherIntelEvent() {
        return TYPE_WEATHER_INTEL_SKIP.equals(type);
    }
    
    /**
     * Check if this is a rain sensor event
     */
    public boolean isRainSensorEvent() {
        return TYPE_RAIN_SENSOR_DETECTION.equals(type);
    }
    
    /**
     * Check if this is a water budget event
     */
    public boolean isWaterBudgetEvent() {
        return TYPE_WATER_BUDGET.equals(type);
    }
    
    /**
     * Get event category (simplified)
     */
    public @Nullable String getEventCategory() {
        if (type == null) {
            return null;
        }
        
        if (isDeviceStatusEvent()) return "DEVICE";
        if (isZoneStatusEvent()) return "ZONE";
        if (isScheduleStatusEvent()) return "SCHEDULE";
        if (isRainDelayEvent()) return "RAIN_DELAY";
        if (isWeatherIntelEvent()) return "WEATHER";
        if (isRainSensorEvent()) return "RAIN_SENSOR";
        if (isWaterBudgetEvent()) return "WATER_BUDGET";
        
        return "OTHER";
    }
    
    /**
     * Get simplified event type
     */
    public @Nullable String getSimpleType() {
        if (type == null) {
            return null;
        }
        
        // Remove EVENT_ prefix and category for simpler display
        String simple = type
                .replace("_EVENT_", "_")
                .replace("EVENT_", "");
        
        // Convert to title case
        return simple.toLowerCase()
                .replace("_", " ")
                .transform(s -> s.substring(0, 1).toUpperCase() + s.substring(1));
    }
    
    /**
     * Get duration in minutes
     */
    public @Nullable Double getDurationMinutes() {
        if (duration == null) {
            return null;
        }
        return duration / 60.0;
    }
    
    /**
     * Get total duration in minutes
     */
    public @Nullable Double getTotalDurationMinutes() {
        if (totalDuration == null) {
            return null;
        }
        return totalDuration / 60.0;
    }
    
    /**
     * Get zone run time in minutes
     */
    public @Nullable Double getZoneRunTimeMinutes() {
        if (zoneRunTime == null) {
            return null;
        }
        return zoneRunTime / 60.0;
    }
    
    /**
     * Get zone remaining time in minutes
     */
    public @Nullable Double getZoneRemainingTimeMinutes() {
        if (zoneRemainingTime == null) {
            return null;
        }
        return zoneRemainingTime / 60.0;
    }
    
    /**
     * Check if event has professional irrigation data
     */
    public boolean hasProfessionalData() {
        return zoneSoilType != null || zoneCropType != null || zoneCropCoefficient != null ||
               zoneNozzleType != null || zoneNozzleRate != null || zoneSlopeType != null ||
               zoneShadeType != null || zoneEfficiency != null || zoneRootDepth != null ||
               zoneArea != null;
    }
    
    /**
     * Get professional data as map
     */
    public @Nullable Map<String, Object> getProfessionalData() {
        if (!hasProfessionalData()) {
            return null;
        }
        
        return Map.of(
            "soilType", zoneSoilType != null ? zoneSoilType : "Unknown",
            "cropType", zoneCropType != null ? zoneCropType : "Unknown",
            "cropCoefficient", zoneCropCoefficient != null ? zoneCropCoefficient : 0.0,
            "nozzleType", zoneNozzleType != null ? zoneNozzleType : "Unknown",
            "nozzleRate", zoneNozzleRate != null ? zoneNozzleRate : 0.0,
            "slopeType", zoneSlopeType != null ? zoneSlopeType : "Unknown",
            "shadeType", zoneShadeType != null ? zoneShadeType : "Unknown",
            "efficiency", zoneEfficiency != null ? zoneEfficiency : 0.0,
            "rootDepth", zoneRootDepth != null ? zoneRootDepth : 0.0,
            "area", zoneArea != null ? zoneArea : 0.0
        );
    }
    
    /**
     * Get event summary text
     */
    public String getEventSummary() {
        StringBuilder sb = new StringBuilder();
        
        if (type != null) {
            sb.append(getSimpleType()).append(": ");
        }
        
        if (deviceName != null) {
            sb.append(deviceName);
        } else if (deviceId != null) {
            sb.append("Device ").append(deviceId.substring(0, 8));
        }
        
        if (zoneName != null) {
            sb.append(" - ").append(zoneName);
        } else if (zoneId != null) {
            sb.append(" - Zone ").append(zoneId.substring(0, 8));
        }
        
        if (summary != null) {
            sb.append(" (").append(summary).append(")");
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("RachioEvent{id=%s, type=%s, deviceId=%s, zoneId=%s, createDate=%s}",
                id, type, deviceId, zoneId, createDate);
    }
}
