package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.time.Instant;

/**
 * DTO for Zone Run Status from Rachio API
 * Based on Rachio zone watering events
 * 
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class ZoneRunStatus {
    // Run identification
    @SerializedName("zoneId")
    public String zoneId;
    
    @SerializedName("deviceId")
    public String deviceId;
    
    @SerializedName("runId")
    public String runId;
    
    // Run status
    @SerializedName("status")
    public String status; // STARTED, STOPPED, COMPLETED, PAUSED, RESUMED
    
    @SerializedName("previousStatus")
    public @Nullable String previousStatus;
    
    // Run timing
    @SerializedName("startTime")
    public Instant startTime;
    
    @SerializedName("endTime")
    public @Nullable Instant endTime;
    
    @SerializedName("duration")
    public int duration; // seconds
    
    @SerializedName("remainingDuration")
    public int remainingDuration; // seconds
    
    @SerializedName("scheduledDuration")
    public int scheduledDuration; // seconds
    
    // Water usage
    @SerializedName("waterUsage")
    public double waterUsage; // gallons
    
    @SerializedName("estimatedWaterUsage")
    public double estimatedWaterUsage; // gallons
    
    // Run details
    @SerializedName("zoneNumber")
    public int zoneNumber;
    
    @SerializedName("zoneName")
    public String zoneName;
    
    @SerializedName("deviceName")
    public String deviceName;
    
    @SerializedName("reason")
    public @Nullable String reason; // MANUAL, SCHEDULE, QUICK_RUN
    
    @SerializedName("scheduleId")
    public @Nullable String scheduleId;
    
    @SerializedName("scheduleName")
    public @Nullable String scheduleName;
    
    // Helper methods
    
    /**
     * Check if run is active
     */
    public boolean isActive() {
        return "STARTED".equals(status) || "RESUMED".equals(status);
    }
    
    /**
     * Check if run is completed
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }
    
    /**
     * Check if run was stopped
     */
    public boolean isStopped() {
        return "STOPPED".equals(status);
    }
    
    /**
     * Get elapsed time in seconds
     */
    public long getElapsedSeconds() {
        if (isActive()) {
            return Instant.now().getEpochSecond() - startTime.getEpochSecond();
        } else if (endTime != null) {
            return endTime.getEpochSecond() - startTime.getEpochSecond();
        }
        return 0;
    }
    
    /**
     * Get progress percentage
     */
    public double getProgressPercentage() {
        if (scheduledDuration > 0) {
            long elapsed = getElapsedSeconds();
            return Math.min(100.0, (elapsed * 100.0) / scheduledDuration);
        }
        return 0.0;
    }
    
    /**
     * Get run summary
     */
    public String getSummary() {
        return String.format("Zone %s: %s (%d/%d seconds, %.1f gallons)", 
            zoneName, status, getElapsedSeconds(), scheduledDuration, waterUsage);
    }
    
    /**
     * Get run details map
     */
    public java.util.Map<String, Object> getDetails() {
        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("zoneId", zoneId);
        details.put("zoneName", zoneName);
        details.put("zoneNumber", zoneNumber);
        details.put("deviceId", deviceId);
        details.put("deviceName", deviceName);
        details.put("status", status);
        details.put("startTime", startTime.toString());
        if (endTime != null) details.put("endTime", endTime.toString());
        details.put("duration", duration);
        details.put("remainingDuration", remainingDuration);
        details.put("scheduledDuration", scheduledDuration);
        details.put("waterUsage", waterUsage);
        details.put("estimatedWaterUsage", estimatedWaterUsage);
        details.put("elapsedSeconds", getElapsedSeconds());
        details.put("progressPercentage", getProgressPercentage());
        if (reason != null) details.put("reason", reason);
        if (scheduleId != null) details.put("scheduleId", scheduleId);
        if (scheduleName != null) details.put("scheduleName", scheduleName);
        if (previousStatus != null) details.put("previousStatus", previousStatus);
        return details;
    }
    
    // Status constants
    public static final String STATUS_STARTED = "STARTED";
    public static final String STATUS_STOPPED = "STOPPED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_PAUSED = "PAUSED";
    public static final String STATUS_RESUMED = "RESUMED";
    
    // Reason constants
    public static final String REASON_MANUAL = "MANUAL";
    public static final String REASON_SCHEDULE = "SCHEDULE";
    public static final String REASON_QUICK_RUN = "QUICK_RUN";
}
