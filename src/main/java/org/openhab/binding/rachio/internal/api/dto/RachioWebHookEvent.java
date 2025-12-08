package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO representing a Rachio webhook event
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioWebhookEvent {
    
    // Event metadata
    @SerializedName("id")
    private String id = "";
    
    @SerializedName("type")
    private String type = ""; // CRITICAL: This field was referenced in RachioHttp.java
    
    @SerializedName("eventType")
    private String eventType = "";
    
    @SerializedName("createdAt")
    private Instant createdAt;
    
    @SerializedName("receivedAt")
    private Instant receivedAt;
    
    // Device information
    @SerializedName("deviceId")
    private String deviceId = "";
    
    @SerializedName("deviceName")
    private String deviceName = "";
    
    // Zone information (for zone-related events)
    @SerializedName("zoneId")
    private String zoneId = "";
    
    @SerializedName("zoneName")
    private String zoneName = "";
    
    @SerializedName("zoneNumber")
    private Integer zoneNumber = 0;
    
    // Event-specific data
    @SerializedName("data")
    private Map<String, Object> data = Map.of();
    
    @SerializedName("summary")
    private String summary = "";
    
    @SerializedName("description")
    private String description = "";
    
    // Webhook metadata
    @SerializedName("webhookId")
    private String webhookId = "";
    
    @SerializedName("externalId")
    private String externalId = "";
    
    @SerializedName("subscriptionId")
    private String subscriptionId = "";
    
    // Status information
    @SerializedName("status")
    private String status = "";
    
    @SerializedName("severity")
    private String severity = "";
    
    @SerializedName("acknowledged")
    private Boolean acknowledged = false;
    
    // Additional properties
    @SerializedName("properties")
    private Map<String, Object> properties = Map.of();
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    // CRITICAL: This getter was referenced in RachioHttp.java
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public @Nullable Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public @Nullable Instant getReceivedAt() {
        return receivedAt;
    }
    
    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    public String getZoneId() {
        return zoneId;
    }
    
    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }
    
    public String getZoneName() {
        return zoneName;
    }
    
    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }
    
    public Integer getZoneNumber() {
        return zoneNumber;
    }
    
    public void setZoneNumber(Integer zoneNumber) {
        this.zoneNumber = zoneNumber;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getWebhookId() {
        return webhookId;
    }
    
    public void setWebhookId(String webhookId) {
        this.webhookId = webhookId;
    }
    
    public String getExternalId() {
        return externalId;
    }
    
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
    
    public String getSubscriptionId() {
        return subscriptionId;
    }
    
    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public Boolean isAcknowledged() {
        return acknowledged;
    }
    
    public void setAcknowledged(Boolean acknowledged) {
        this.acknowledged = acknowledged;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
    
    // Helper methods
    public boolean isZoneEvent() {
        return !zoneId.isEmpty();
    }
    
    public boolean isDeviceEvent() {
        return !deviceId.isEmpty() && zoneId.isEmpty();
    }
    
    public boolean isSystemEvent() {
        return deviceId.isEmpty() && zoneId.isEmpty();
    }
    
    public String getEventCategory() {
        if (type.contains("ZONE")) {
            return "ZONE";
        } else if (type.contains("DEVICE")) {
            return "DEVICE";
        } else if (type.contains("SCHEDULE")) {
            return "SCHEDULE";
        } else if (type.contains("WEATHER")) {
            return "WEATHER";
        } else if (type.contains("RAIN")) {
            return "RAIN";
        } else if (type.contains("ALERT")) {
            return "ALERT";
        } else {
            return "SYSTEM";
        }
    }
    
    public boolean isCritical() {
        return "CRITICAL".equalsIgnoreCase(severity) || 
               "ERROR".equalsIgnoreCase(severity) || 
               "FATAL".equalsIgnoreCase(severity);
    }
    
    public boolean isWarning() {
        return "WARNING".equalsIgnoreCase(severity) || 
               "ALERT".equalsIgnoreCase(severity);
    }
    
    public boolean isInformational() {
        return "INFO".equalsIgnoreCase(severity) || 
               "INFORMATIONAL".equalsIgnoreCase(severity) ||
               severity.isEmpty();
    }
    
    @Nullable
    public Object getDataValue(String key) {
        return data.get(key);
    }
    
    @Nullable
    public String getDataString(String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }
    
    @Nullable
    public Integer getDataInt(String key) {
        Object value = data.get(key);
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
    
    @Nullable
    public Double getDataDouble(String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    @Nullable
    public Boolean getDataBoolean(String key) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return null;
    }
    
    // Common event data accessors
    @Nullable
    public String getZoneStatus() {
        return getDataString("status");
    }
    
    @Nullable
    public Integer getDuration() {
        return getDataInt("duration");
    }
    
    @Nullable
    public Double getWaterVolume() {
        return getDataDouble("waterVolume");
    }
    
    @Nullable
    public String getDeviceStatus() {
        return getDataString("status");
    }
    
    @Nullable
    public Integer getRainDelayHours() {
        return getDataInt("hours");
    }
    
    @Nullable
    public Double getRainfall() {
        return getDataDouble("rainfall");
    }
    
    @Nullable
    public Double getEvapotranspiration() {
        return getDataDouble("evapotranspiration");
    }
    
    @Nullable
    public String getScheduleStatus() {
        return getDataString("status");
    }
    
    @Nullable
    public String getAlertType() {
        return getDataString("alertType");
    }
    
    @Nullable
    public String getAlertMessage() {
        return getDataString("message");
    }
    
    public String getFormattedTimestamp() {
        if (createdAt == null) {
            return "Unknown time";
        }
        return createdAt.toString(); // Could use DateTimeFormatter for better formatting
    }
    
    public String getEventDisplayName() {
        switch (type) {
            case "ZONE_STATUS":
                return "Zone Status";
            case "ZONE_STARTED":
                return "Zone Started";
            case "ZONE_STOPPED":
                return "Zone Stopped";
            case "ZONE_COMPLETED":
                return "Zone Completed";
            case "ZONE_CYCLE_COMPLETED":
                return "Zone Cycle Completed";
            case "ZONE_SKIPPED":
                return "Zone Skipped";
            case "DEVICE_STATUS":
                return "Device Status";
            case "DEVICE_ONLINE":
                return "Device Online";
            case "DEVICE_OFFLINE":
                return "Device Offline";
            case "RAIN_DELAY_STARTED":
                return "Rain Delay Started";
            case "RAIN_DELAY_ENDED":
                return "Rain Delay Ended";
            case "WEATHER_INTEL":
                return "Weather Intelligence";
            case "WEATHER_SKIP":
                return "Weather Skip";
            case "WATER_BUDGET":
                return "Water Budget";
            case "SCHEDULE_STATUS":
                return "Schedule Status";
            case "SCHEDULE_STARTED":
                return "Schedule Started";
            case "SCHEDULE_COMPLETED":
                return "Schedule Completed";
            case "SCHEDULE_SKIPPED":
                return "Schedule Skipped";
            case "RAIN_SENSOR_DETECTION":
                return "Rain Sensor Detection";
            case "ALERT":
                return "Alert";
            case "SERVICE":
                return "Service";
            default:
                return type.replace("_", " ");
        }
    }
    
    @Override
    public String toString() {
        return String.format("RachioWebhookEvent[id=%s, type=%s, device=%s, zone=%s, time=%s]", 
            id, type, deviceId, zoneId, createdAt);
    }
}
