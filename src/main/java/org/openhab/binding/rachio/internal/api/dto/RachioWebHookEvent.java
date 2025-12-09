package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for Rachio webhook events
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioWebHookEvent {
    public @Nullable String type;
    public @Nullable String subType;
    
    @SerializedName("deviceId")
    public @Nullable String deviceId;
    
    @SerializedName("zoneId")
    public @Nullable String zoneId;
    
    @SerializedName("summary")
    public @Nullable String summary;
    
    @SerializedName("eventDate")
    public @Nullable Instant eventDate;
    
    @SerializedName("rainDelay")
    public @Nullable Integer rainDelay;
    
    @SerializedName("duration")
    public @Nullable Integer duration;
    
    @SerializedName("zoneRunStatus")
    public @Nullable String zoneRunStatus;
    
    @SerializedName("waterBudget")
    public @Nullable Double waterBudget;
    
    // Getters for compatibility
    public @Nullable String getType() { return type; }
    public @Nullable String getSubType() { return subType; }
    public @Nullable String getDeviceId() { return deviceId; }
    public @Nullable String getZoneId() { return zoneId; }
    public @Nullable String getSummary() { return summary; }
    public @Nullable Instant getEventDate() { return eventDate; }
    public @Nullable Integer getRainDelay() { return rainDelay; }
    public @Nullable Integer getDuration() { return duration; }
    public @Nullable String getZoneRunStatus() { return zoneRunStatus; }
    public @Nullable Double getWaterBudget() { return waterBudget; }
}
