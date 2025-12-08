package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

@NonNullByDefault
public class RachioWebHookEvent {
    @SerializedName("type")
    private String type = "";
    
    @SerializedName("subType")
    private String subType = "";
    
    @SerializedName("deviceId")
    private String deviceId = "";
    
    @SerializedName("zoneId")
    @Nullable
    private String zoneId;
    
    @SerializedName("eventId")
    private String eventId = "";
    
    @SerializedName("createDate")
    private Instant createDate;
    
    @SerializedName("summary")
    @Nullable
    private String summary;
    
    // Getters
    public String getType() {
        return type;
    }
    
    public String getSubType() {
        return subType;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    @Nullable
    public String getZoneId() {
        return zoneId;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public Instant getCreateDate() {
        return createDate;
    }
    
    @Nullable
    public String getSummary() {
        return summary;
    }
    
    // Setters
    public void setType(String type) {
        this.type = type;
    }
    
    public void setSubType(String subType) {
        this.subType = subType;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public void setZoneId(@Nullable String zoneId) {
        this.zoneId = zoneId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public void setCreateDate(Instant createDate) {
        this.createDate = createDate;
    }
    
    public void setSummary(@Nullable String summary) {
        this.summary = summary;
    }
    
    @Override
    public String toString() {
        return "RachioWebHookEvent [type=" + type + ", subType=" + subType + ", deviceId=" + deviceId 
                + ", zoneId=" + zoneId + ", eventId=" + eventId + "]";
    }
}
