package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for Rachio Webhook Events
 *
 * @author Daniel B. - Complete professional event handling
 */
@NonNullByDefault
public class RachioWebhookEvent {
    @SerializedName("eventType")
    private String eventType = "";
    
    @SerializedName("deviceId")
    private @Nullable String deviceId;
    
    @SerializedName("zoneId")
    private @Nullable String zoneId;
    
    @SerializedName("zoneName")
    private @Nullable String zoneName;
    
    @SerializedName("scheduleId")
    private @Nullable String scheduleId;
    
    @SerializedName("status")
    private @Nullable String status;
    
    @SerializedName("duration")
    private @Nullable Integer duration;
    
    @SerializedName("rainDelayEndTime")
    private @Nullable Instant rainDelayEndTime;
    
    @SerializedName("rainDelayStartTime")
    private @Nullable Instant rainDelayStartTime;
    
    @SerializedName("smartSkipEnabled")
    private @Nullable Boolean smartSkipEnabled;
    
    @SerializedName("skipReason")
    private @Nullable String skipReason;
    
    @SerializedName("waterBudgetPercent")
    private @Nullable Integer waterBudgetPercent;
    
    @SerializedName("rainDetected")
    private @Nullable Boolean rainDetected;
    
    @SerializedName("alertType")
    private @Nullable String alertType;
    
    @SerializedName("alertMessage")
    private @Nullable String alertMessage;
    
    @SerializedName("userId")
    private @Nullable String userId;
    
    @SerializedName("homeId")
    private @Nullable String homeId;
    
    @SerializedName("timestamp")
    private @Nullable Instant timestamp;
    
    @SerializedName("subscriptionId")
    private @Nullable String subscriptionId;
    
    // Getters and Setters
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public @Nullable String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(@Nullable String deviceId) {
        this.deviceId = deviceId;
    }
    
    public @Nullable String getZoneId() {
        return zoneId;
    }
    
    public void setZoneId(@Nullable String zoneId) {
        this.zoneId = zoneId;
    }
    
    public @Nullable String getZoneName() {
        return zoneName;
    }
    
    public void setZoneName(@Nullable String zoneName) {
        this.zoneName = zoneName;
    }
    
    public @Nullable String getScheduleId() {
        return scheduleId;
    }
    
    public void setScheduleId(@Nullable String scheduleId) {
        this.scheduleId = scheduleId;
    }
    
    public @Nullable String getStatus() {
        return status;
    }
    
    public void setStatus(@Nullable String status) {
        this.status = status;
    }
    
    public @Nullable Integer getDuration() {
        return duration;
    }
    
    public void setDuration(@Nullable Integer duration) {
        this.duration = duration;
    }
    
    public @Nullable Instant getRainDelayEndTime() {
        return rainDelayEndTime;
    }
    
    public void setRainDelayEndTime(@Nullable Instant rainDelayEndTime) {
        this.rainDelayEndTime = rainDelayEndTime;
    }
    
    public @Nullable Instant getRainDelayStartTime() {
        return rainDelayStartTime;
    }
    
    public void setRainDelayStartTime(@Nullable Instant rainDelayStartTime) {
        this.rainDelayStartTime = rainDelayStartTime;
    }
    
    public @Nullable Boolean getSmartSkipEnabled() {
        return smartSkipEnabled;
    }
    
    public void setSmartSkipEnabled(@Nullable Boolean smartSkipEnabled) {
        this.smartSkipEnabled = smartSkipEnabled;
    }
    
    public @Nullable String getSkipReason() {
        return skipReason;
    }
    
    public void setSkipReason(@Nullable String skipReason) {
        this.skipReason = skipReason;
    }
    
    public @Nullable Integer getWaterBudgetPercent() {
        return waterBudgetPercent;
    }
    
    public void setWaterBudgetPercent(@Nullable Integer waterBudgetPercent) {
        this.waterBudgetPercent = waterBudgetPercent;
    }
    
    public @Nullable Boolean getRainDetected() {
        return rainDetected;
    }
    
    public void setRainDetected(@Nullable Boolean rainDetected) {
        this.rainDetected = rainDetected;
    }
    
    public @Nullable String getAlertType() {
        return alertType;
    }
    
    public void setAlertType(@Nullable String alertType) {
        this.alertType = alertType;
    }
    
    public @Nullable String getAlertMessage() {
        return alertMessage;
    }
    
    public void setAlertMessage(@Nullable String alertMessage) {
        this.alertMessage = alertMessage;
    }
    
    public @Nullable String getUserId() {
        return userId;
    }
    
    public void setUserId(@Nullable String userId) {
        this.userId = userId;
    }
    
    public @Nullable String getHomeId() {
        return homeId;
    }
    
    public void setHomeId(@Nullable String homeId) {
        this.homeId = homeId;
    }
    
    public @Nullable Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(@Nullable Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public @Nullable String getSubscriptionId() {
        return subscriptionId;
    }
    
    public void setSubscriptionId(@Nullable String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RachioWebhookEvent that = (RachioWebhookEvent) o;
        return Objects.equals(eventType, that.eventType) &&
               Objects.equals(deviceId, that.deviceId) &&
               Objects.equals(zoneId, that.zoneId) &&
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(eventType, deviceId, zoneId, timestamp);
    }
    
    @Override
    public String toString() {
        return "RachioWebhookEvent{" +
                "eventType='" + eventType + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", zoneId='" + zoneId + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
