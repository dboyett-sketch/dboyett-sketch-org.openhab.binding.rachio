package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.time.Instant;

/**
 * DTO for Rachio Event from API
 * Used for /device/{id}/event endpoint
 * 
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioEvent {
    @SerializedName("id")
    public String id;
    
    @SerializedName("deviceId")
    public String deviceId;
    
    @SerializedName("type")
    public String type; // ZONE_STATUS, DEVICE_STATUS, SCHEDULE_STATUS, etc.
    
    @SerializedName("category")
    public String category; // ZONE, DEVICE, SCHEDULE, WEATHER, etc.
    
    @SerializedName("timestamp")
    public Instant timestamp;
    
    @SerializedName("summary")
    public String summary;
    
    @SerializedName("icon")
    public @Nullable String icon;
    
    @SerializedName("zoneId")
    public @Nullable String zoneId;
    
    @SerializedName("zoneNumber")
    public @Nullable Integer zoneNumber;
    
    @SerializedName("scheduleId")
    public @Nullable String scheduleId;
    
    @SerializedName("duration")
    public @Nullable Integer duration;
    
    @SerializedName("status")
    public @Nullable String status;
    
    // Helper methods
    
    /**
     * Check if this is a zone event
     */
    public boolean isZoneEvent() {
        return "ZONE".equals(category);
    }
    
    /**
     * Check if this is a device event
     */
    public boolean isDeviceEvent() {
        return "DEVICE".equals(category);
    }
    
    /**
     * Check if this is a schedule event
     */
    public boolean isScheduleEvent() {
        return "SCHEDULE".equals(category);
    }
    
    /**
     * Get event summary
     */
    public String getEventSummary() {
        return String.format("%s: %s at %s", type, summary, timestamp);
    }
    
    /**
     * Get event details
     */
    public java.util.Map<String, Object> getDetails() {
        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("id", id);
        details.put("deviceId", deviceId);
        details.put("type", type);
        details.put("category", category);
        details.put("timestamp", timestamp.toString());
        details.put("summary", summary);
        
        if (zoneId != null) details.put("zoneId", zoneId);
        if (zoneNumber != null) details.put("zoneNumber", zoneNumber);
        if (scheduleId != null) details.put("scheduleId", scheduleId);
        if (duration != null) details.put("duration", duration);
        if (status != null) details.put("status", status);
        if (icon != null) details.put("icon", icon);
        
        return details;
    }
}
