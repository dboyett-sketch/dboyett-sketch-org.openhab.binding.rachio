package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for Rachio alerts and notifications
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioAlert {

    // Alert type constants
    public static final String TYPE_DEVICE_OFFLINE = "DEVICE_OFFLINE";
    public static final String TYPE_DEVICE_ONLINE = "DEVICE_ONLINE";
    public static final String TYPE_RAIN_DELAY_STARTED = "RAIN_DELAY_STARTED";
    public static final String TYPE_RAIN_DELAY_ENDED = "RAIN_DELAY_ENDED";
    public static final String TYPE_RAIN_SENSOR_DETECTED = "RAIN_SENSOR_DETECTED";
    public static final String TYPE_SCHEDULE_SKIPPED = "SCHEDULE_SKIPPED";
    public static final String TYPE_SCHEDULE_COMPLETED = "SCHEDULE_COMPLETED";
    public static final String TYPE_ZONE_COMPLETED = "ZONE_COMPLETED";
    public static final String TYPE_ZONE_STARTED = "ZONE_STARTED";
    public static final String TYPE_ZONE_STOPPED = "ZONE_STOPPED";
    public static final String TYPE_WATER_BUDGET_ADJUSTED = "WATER_BUDGET_ADJUSTED";
    public static final String TYPE_FREEZE_WARNING = "FREEZE_WARNING";
    public static final String TYPE_HIGH_TEMP_WARNING = "HIGH_TEMP_WARNING";
    public static final String TYPE_LOW_BATTERY = "LOW_BATTERY";
    public static final String TYPE_CONNECTIVITY_ISSUE = "CONNECTIVITY_ISSUE";
    public static final String TYPE_SOIL_SENSOR_ALERT = "SOIL_SENSOR_ALERT";
    public static final String TYPE_FLOW_SENSOR_ALERT = "FLOW_SENSOR_ALERT";
    public static final String TYPE_VALVE_ALERT = "VALVE_ALERT";
    public static final String TYPE_MAINTENANCE_REMINDER = "MAINTENANCE_REMINDER";
    public static final String TYPE_WATER_RESTRICTION = "WATER_RESTRICTION";
    
    // Alert severity constants
    public static final String SEVERITY_INFO = "INFO";
    public static final String SEVERITY_WARNING = "WARNING";
    public static final String SEVERITY_ERROR = "ERROR";
    public static final String SEVERITY_CRITICAL = "CRITICAL";
    
    // Alert category constants
    public static final String CATEGORY_DEVICE = "DEVICE";
    public static final String CATEGORY_ZONE = "ZONE";
    public static final String CATEGORY_SCHEDULE = "SCHEDULE";
    public static final String CATEGORY_WEATHER = "WEATHER";
    public static final String CATEGORY_SENSOR = "SENSOR";
    public static final String CATEGORY_MAINTENANCE = "MAINTENANCE";
    public static final String CATEGORY_COMPLIANCE = "COMPLIANCE";
    public static final String CATEGORY_SYSTEM = "SYSTEM";
    
    // Alert fields
    @SerializedName("id")
    public @Nullable String id;
    
    @SerializedName("type")
    public @Nullable String type;
    
    @SerializedName("category")
    public @Nullable String category;
    
    @SerializedName("severity")
    public @Nullable String severity;
    
    @SerializedName("title")
    public @Nullable String title;
    
    @SerializedName("message")
    public @Nullable String message;
    
    @SerializedName("description")
    public @Nullable String description;
    
    @SerializedName("deviceId")
    public @Nullable String deviceId;
    
    @SerializedName("deviceName")
    public @Nullable String deviceName;
    
    @SerializedName("zoneId")
    public @Nullable String zoneId;
    
    @SerializedName("zoneName")
    public @Nullable String zoneName;
    
    @SerializedName("zoneNumber")
    public @Nullable Integer zoneNumber;
    
    @SerializedName("scheduleId")
    public @Nullable String scheduleId;
    
    @SerializedName("scheduleName")
    public @Nullable String scheduleName;
    
    @SerializedName("sensorId")
    public @Nullable String sensorId;
    
    @SerializedName("sensorType")
    public @Nullable String sensorType;
    
    @SerializedName("sensorName")
    public @Nullable String sensorName;
    
    @SerializedName("createdDate")
    public @Nullable Instant createdDate;
    
    @SerializedName("updatedDate")
    public @Nullable Instant updatedDate;
    
    @SerializedName("acknowledgedDate")
    public @Nullable Instant acknowledgedDate;
    
    @SerializedName("resolvedDate")
    public @Nullable Instant resolvedDate;
    
    @SerializedName("expirationDate")
    public @Nullable Instant expirationDate;
    
    @SerializedName("acknowledged")
    public @Nullable Boolean acknowledged;
    
    @SerializedName("resolved")
    public @Nullable Boolean resolved;
    
    @SerializedName("active")
    public @Nullable Boolean active;
    
    @SerializedName("dismissable")
    public @Nullable Boolean dismissable;
    
    @SerializedName("autoResolve")
    public @Nullable Boolean autoResolve;
    
    @SerializedName("priority")
    public @Nullable Integer priority;
    
    // Alert data
    @SerializedName("data")
    public @Nullable AlertData data;
    
    @SerializedName("metadata")
    public @Nullable Map<String, Object> metadata;
    
    @SerializedName("actions")
    public @Nullable List<AlertAction> actions;
    
    @SerializedName("relatedAlerts")
    public @Nullable List<String> relatedAlerts;
    
    /**
     * Alert data with type-specific fields
     */
    @NonNullByDefault
    public static class AlertData {
        // Device offline/online
        @SerializedName("offlineDuration")
        public @Nullable Long offlineDuration;
        
        @SerializedName("lastSeen")
        public @Nullable Instant lastSeen;
        
        @SerializedName("connectionType")
        public @Nullable String connectionType;
        
        @SerializedName("signalStrength")
        public @Nullable Integer signalStrength;
        
        // Rain delay
        @SerializedName("rainDelayHours")
        public @Nullable Integer rainDelayHours;
        
        @SerializedName("rainDelayStart")
        public @Nullable Instant rainDelayStart;
        
        @SerializedName("rainDelayEnd")
        public @Nullable Instant rainDelayEnd;
        
        @SerializedName("rainDelayReason")
        public @Nullable String rainDelayReason;
        
        // Rain sensor
        @SerializedName("rainAmount")
        public @Nullable Double rainAmount;
        
        @SerializedName("rainDuration")
        public @Nullable Integer rainDuration;
        
        @SerializedName("sensorTriggeredAt")
        public @Nullable Instant sensorTriggeredAt;
        
        @SerializedName("sensorLocation")
        public @Nullable String sensorLocation;
        
        // Schedule
        @SerializedName("scheduleType")
        public @Nullable String scheduleType;
        
        @SerializedName("scheduleStartTime")
        public @Nullable Instant scheduleStartTime;
        
        @SerializedName("scheduleDuration")
        public @Nullable Integer scheduleDuration;
        
        @SerializedName("skipReason")
        public @Nullable String skipReason;
        
        @SerializedName("skipDate")
        public @Nullable Instant skipDate;
        
        @SerializedName("completedZones")
        public @Nullable Integer completedZones;
        
        @SerializedName("totalZones")
        public @Nullable Integer totalZones;
        
        // Zone
        @SerializedName("zoneDuration")
        public @Nullable Integer zoneDuration;
        
        @SerializedName("zoneStartTime")
        public @Nullable Instant zoneStartTime;
        
        @SerializedName("zoneEndTime")
        public @Nullable Instant zoneEndTime;
        
        @SerializedName("zoneRunOrder")
        public @Nullable Integer zoneRunOrder;
        
        @SerializedName("zoneWaterUsage")
        public @Nullable Double zoneWaterUsage;
        
        // Water budget
        @SerializedName("waterBudgetPercentage")
        public @Nullable Double waterBudgetPercentage;
        
        @SerializedName("waterBudgetAdjustment")
        public @Nullable Double waterBudgetAdjustment;
        
        @SerializedName("waterBudgetReason")
        public @Nullable String waterBudgetReason;
        
        @SerializedName("waterSavings")
        public @Nullable Double waterSavings;
        
        // Weather warnings
        @SerializedName("temperature")
        public @Nullable Double temperature;
        
        @SerializedName("freezeTemperature")
        public @Nullable Double freezeTemperature;
        
        @SerializedName("highTemperature")
        public @Nullable Double highTemperature;
        
        @SerializedName("warningDuration")
        public @Nullable Integer warningDuration;
        
        @SerializedName("forecastDate")
        public @Nullable Instant forecastDate;
        
        // Battery
        @SerializedName("batteryLevel")
        public @Nullable Integer batteryLevel;
        
        @SerializedName("batteryVoltage")
        public @Nullable Double batteryVoltage;
        
        @SerializedName("batteryType")
        public @Nullable String batteryType;
        
        @SerializedName("estimatedDaysRemaining")
        public @Nullable Integer estimatedDaysRemaining;
        
        // Connectivity
        @SerializedName("connectionIssues")
        public @Nullable Integer connectionIssues;
        
        @SerializedName("lastSuccessfulConnection")
        public @Nullable Instant lastSuccessfulConnection;
        
        @SerializedName("retryCount")
        public @Nullable Integer retryCount;
        
        // Soil sensor
        @SerializedName("soilMoisture")
        public @Nullable Double soilMoisture;
        
        @SerializedName("soilTemperature")
        public @Nullable Double soilTemperature;
        
        @SerializedName("soilMoistureThreshold")
        public @Nullable Double soilMoistureThreshold;
        
        @SerializedName("soilSensorLocation")
        public @Nullable String soilSensorLocation;
        
        @SerializedName("soilSensorDepth")
        public @Nullable Double soilSensorDepth;
        
        // Flow sensor
        @SerializedName("flowRate")
        public @Nullable Double flowRate;
        
        @SerializedName("expectedFlowRate")
        public @Nullable Double expectedFlowRate;
        
        @SerializedName("flowDeviation")
        public @Nullable Double flowDeviation;
        
        @SerializedName("totalFlow")
        public @Nullable Double totalFlow;
        
        @SerializedName("leakDetected")
        public @Nullable Boolean leakDetected;
        
        @SerializedName("clogDetected")
        public @Nullable Boolean clogDetected;
        
        // Valve
        @SerializedName("valveNumber")
        public @Nullable Integer valveNumber;
        
        @SerializedName("valveStatus")
        public @Nullable String valveStatus;
        
        @SerializedName("valveFault")
        public @Nullable String valveFault;
        
        @SerializedName("valveResistance")
        public @Nullable Double valveResistance;
        
        @SerializedName("valveCycles")
        public @Nullable Integer valveCycles;
        
        // Maintenance
        @SerializedName("maintenanceType")
        public @Nullable String maintenanceType;
        
        @SerializedName("maintenanceDueDate")
        public @Nullable Instant maintenanceDueDate;
        
        @SerializedName("daysUntilDue")
        public @Nullable Integer daysUntilDue;
        
        @SerializedName("lastMaintenanceDate")
        public @Nullable Instant lastMaintenanceDate;
        
        // Water restriction
        @SerializedName("restrictionType")
        public @Nullable String restrictionType;
        
        @SerializedName("restrictionStart")
        public @Nullable Instant restrictionStart;
        
        @SerializedName("restrictionEnd")
        public @Nullable Instant restrictionEnd;
        
        @SerializedName("restrictionDays")
        public @Nullable List<String> restrictionDays;
        
        @SerializedName("restrictionHours")
        public @Nullable String restrictionHours;
        
        @SerializedName("restrictionAuthority")
        public @Nullable String restrictionAuthority;
        
        @SerializedName("restrictionReference")
        public @Nullable String restrictionReference;
    }
    
    /**
     * Alert action
     */
    @NonNullByDefault
    public static class AlertAction {
        @SerializedName("id")
        public @Nullable String id;
        
        @SerializedName("type")
        public @Nullable String type;
        
        @SerializedName("label")
        public @Nullable String label;
        
        @SerializedName("description")
        public @Nullable String description;
        
        @SerializedName("url")
        public @Nullable String url;
        
        @SerializedName("method")
        public @Nullable String method;
        
        @SerializedName("requiresConfirmation")
        public @Nullable Boolean requiresConfirmation;
        
        @SerializedName("confirmationMessage")
        public @Nullable String confirmationMessage;
        
        @SerializedName("enabled")
        public @Nullable Boolean enabled;
    }
    
    // Utility methods
    
    /**
     * Check if alert is active
     */
    public boolean isActive() {
        return active != null && active;
    }
    
    /**
     * Check if alert is acknowledged
     */
    public boolean isAcknowledged() {
        return acknowledged != null && acknowledged;
    }
    
    /**
     * Check if alert is resolved
     */
    public boolean isResolved() {
        return resolved != null && resolved;
    }
    
    /**
     * Check if alert is dismissable
     */
    public boolean isDismissable() {
        return dismissable != null && dismissable;
    }
    
    /**
     * Check if alert auto-resolves
     */
    public boolean isAutoResolve() {
        return autoResolve != null && autoResolve;
    }
    
    /**
     * Get alert age in minutes
     */
    public @Nullable Long getAgeMinutes() {
        if (createdDate != null) {
            Instant now = Instant.now();
            long seconds = now.getEpochSecond() - createdDate.getEpochSecond();
            return seconds / 60;
        }
        return null;
    }
    
    /**
     * Get time since acknowledgment in minutes
     */
    public @Nullable Long getTimeSinceAcknowledgmentMinutes() {
        if (acknowledgedDate != null) {
            Instant now = Instant.now();
            long seconds = now.getEpochSecond() - acknowledgedDate.getEpochSecond();
            return seconds / 60;
        }
        return null;
    }
    
    /**
     * Get time since resolution in minutes
     */
    public @Nullable Long getTimeSinceResolutionMinutes() {
        if (resolvedDate != null) {
            Instant now = Instant.now();
            long seconds = now.getEpochSecond() - resolvedDate.getEpochSecond();
            return seconds / 60;
        }
        return null;
    }
    
    /**
     * Check if alert is expired
     */
    public boolean isExpired() {
        if (expirationDate != null) {
            return Instant.now().isAfter(expirationDate);
        }
        return false;
    }
    
    /**
     * Get alert priority (higher = more important)
     */
    public int getPriority() {
        return priority != null ? priority : 0;
    }
    
    /**
     * Check if this is a device alert
     */
    public boolean isDeviceAlert() {
        return CATEGORY_DEVICE.equals(category) || 
               (type != null && (type.contains("DEVICE") || deviceId != null));
    }
    
    /**
     * Check if this is a zone alert
     */
    public boolean isZoneAlert() {
        return CATEGORY_ZONE.equals(category) || 
               (type != null && (type.contains("ZONE") || zoneId != null));
    }
    
    /**
     * Check if this is a schedule alert
     */
    public boolean isScheduleAlert() {
        return CATEGORY_SCHEDULE.equals(category) || 
               (type != null && (type.contains("SCHEDULE") || scheduleId != null));
    }
    
    /**
     * Check if this is a weather alert
     */
    public boolean isWeatherAlert() {
        return CATEGORY_WEATHER.equals(category) || 
               (type != null && (type.contains("RAIN") || type.contains("FREEZE") || 
                               type.contains("TEMP") || type.contains("WEATHER")));
    }
    
    /**
     * Check if this is a sensor alert
     */
    public boolean isSensorAlert() {
        return CATEGORY_SENSOR.equals(category) || 
               (type != null && (type.contains("SENSOR") || sensorId != null));
    }
    
    /**
     * Check if this is a maintenance alert
     */
    public boolean isMaintenanceAlert() {
        return CATEGORY_MAINTENANCE.equals(category) || 
               (type != null && type.contains("MAINTENANCE"));
    }
    
    /**
     * Check if this is a compliance alert
     */
    public boolean isComplianceAlert() {
        return CATEGORY_COMPLIANCE.equals(category) || 
               (type != null && type.contains("RESTRICTION"));
    }
    
    /**
     * Get formatted alert title
     */
    public String getFormattedTitle() {
        if (title != null) {
            return title;
        }
        
        StringBuilder sb = new StringBuilder();
        if (severity != null) {
            sb.append(severity).append(": ");
        }
        
        if (type != null) {
            sb.append(type.replace("_", " "));
        } else if (category != null) {
            sb.append(category).append(" Alert");
        } else {
            sb.append("Unknown Alert");
        }
        
        return sb.toString();
    }
    
    /**
     * Get formatted alert message
     */
    public String getFormattedMessage() {
        if (message != null) {
            return message;
        }
        
        StringBuilder sb = new StringBuilder();
        
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
        
        if (scheduleName != null) {
            sb.append(" - ").append(scheduleName);
        } else if (scheduleId != null) {
            sb.append(" - Schedule ").append(scheduleId.substring(0, 8));
        }
        
        if (description != null) {
            if (sb.length() > 0) {
                sb.append(": ");
            }
            sb.append(description);
        }
        
        return sb.length() > 0 ? sb.toString() : "No message available";
    }
    
    /**
     * Get alert summary
     */
    public String getSummary() {
        return String.format("%s - %s (Priority: %d, Active: %s, Ack: %s)",
                getFormattedTitle(),
                createdDate != null ? createdDate.toString() : "Unknown time",
                getPriority(),
                isActive() ? "Yes" : "No",
                isAcknowledged() ? "Yes" : "No");
    }
    
    /**
     * Get alert data as map
     */
    public @Nullable Map<String, Object> getAlertDataMap() {
        if (data == null) {
            return null;
        }
        
        java.util.HashMap<String, Object> map = new java.util.HashMap<>();
        
        // Add basic alert info
        map.put("id", id);
        map.put("type", type);
        map.put("category", category);
        map.put("severity", severity);
        map.put("title", getFormattedTitle());
        map.put("message", getFormattedMessage());
        map.put("active", isActive());
        map.put("acknowledged", isAcknowledged());
        map.put("resolved", isResolved());
        map.put("priority", getPriority());
        map.put("ageMinutes", getAgeMinutes());
        
        // Add device/zone/schedule info
        if (deviceId != null) map.put("deviceId", deviceId);
        if (deviceName != null) map.put("deviceName", deviceName);
        if (zoneId != null) map.put("zoneId", zoneId);
        if (zoneName != null) map.put("zoneName", zoneName);
        if (zoneNumber != null) map.put("zoneNumber", zoneNumber);
        if (scheduleId != null) map.put("scheduleId", scheduleId);
        if (scheduleName != null) map.put("scheduleName", scheduleName);
        if (sensorId != null) map.put("sensorId", sensorId);
        if (sensorType != null) map.put("sensorType", sensorType);
        if (sensorName != null) map.put("sensorName", sensorName);
        
        // Add timestamps
        if (createdDate != null) map.put("createdDate", createdDate.toString());
        if (updatedDate != null) map.put("updatedDate", updatedDate.toString());
        if (acknowledgedDate != null) map.put("acknowledgedDate", acknowledgedDate.toString());
        if (resolvedDate != null) map.put("resolvedDate", resolvedDate.toString());
        if (expirationDate != null) map.put("expirationDate", expirationDate.toString());
        
        return map;
    }
    
    @Override
    public String toString() {
        return String.format("RachioAlert{id=%s, type=%s, severity=%s, device=%s, active=%s}",
                id, type, severity, deviceName, isActive());
    }
}
