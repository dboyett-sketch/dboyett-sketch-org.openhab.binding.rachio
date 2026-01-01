package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for Rachio webhook events.
 *
 * @author Brian Higginbotham - Initial contribution
 */
@NonNullByDefault
public class RachioWebHookEvent {
    @SerializedName("id")
    public @Nullable String id;

    @SerializedName("eventType")
    public @Nullable String eventType;

    @SerializedName("deviceId")
    public @Nullable String deviceId;

    @SerializedName("zoneId")
    public @Nullable String zoneId;

    @SerializedName("scheduleId")
    public @Nullable String scheduleId;

    @SerializedName("timestamp")
    public @Nullable Instant timestamp;

    @SerializedName("summary")
    public @Nullable String summary;

    @SerializedName("subType")
    public @Nullable String subType;

    @SerializedName("deviceName")
    public @Nullable String deviceName;

    @SerializedName("zoneName")
    public @Nullable String zoneName;

    @SerializedName("scheduleName")
    public @Nullable String scheduleName;

    @SerializedName("duration")
    public @Nullable Integer duration;

    @SerializedName("totalWater")
    public @Nullable Double totalWater;

    /**
     * Gets the event ID.
     *
     * @return the event ID or null if not set
     */
    public @Nullable String getId() {
        return id;
    }

    /**
     * Sets the event ID.
     *
     * @param id the event ID
     */
    public void setId(@Nullable String id) {
        this.id = id;
    }

    /**
     * Gets the event type.
     *
     * @return the event type or null if not set
     */
    public @Nullable String getEventType() {
        return eventType;
    }

    /**
     * Sets the event type.
     *
     * @param eventType the event type
     */
    public void setEventType(@Nullable String eventType) {
        this.eventType = eventType;
    }

    /**
     * Gets the device ID.
     *
     * @return the device ID or null if not set
     */
    public @Nullable String getDeviceId() {
        return deviceId;
    }

    /**
     * Sets the device ID.
     *
     * @param deviceId the device ID
     */
    public void setDeviceId(@Nullable String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Gets the zone ID.
     *
     * @return the zone ID or null if not set
     */
    public @Nullable String getZoneId() {
        return zoneId;
    }

    /**
     * Sets the zone ID.
     *
     * @param zoneId the zone ID
     */
    public void setZoneId(@Nullable String zoneId) {
        this.zoneId = zoneId;
    }

    /**
     * Gets the schedule ID.
     *
     * @return the schedule ID or null if not set
     */
    public @Nullable String getScheduleId() {
        return scheduleId;
    }

    /**
     * Sets the schedule ID.
     *
     * @param scheduleId the schedule ID
     */
    public void setScheduleId(@Nullable String scheduleId) {
        this.scheduleId = scheduleId;
    }

    /**
     * Gets the event timestamp.
     *
     * @return the timestamp or null if not set
     */
    public @Nullable Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the event timestamp.
     *
     * @param timestamp the timestamp
     */
    public void setTimestamp(@Nullable Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the event summary.
     *
     * @return the summary or null if not set
     */
    public @Nullable String getSummary() {
        return summary;
    }

    /**
     * Sets the event summary.
     *
     * @param summary the summary
     */
    public void setSummary(@Nullable String summary) {
        this.summary = summary;
    }

    /**
     * Gets the event subtype.
     *
     * @return the subtype or null if not set
     */
    public @Nullable String getSubType() {
        return subType;
    }

    /**
     * Sets the event subtype.
     *
     * @param subType the subtype
     */
    public void setSubType(@Nullable String subType) {
        this.subType = subType;
    }

    /**
     * Gets the device name.
     *
     * @return the device name or null if not set
     */
    public @Nullable String getDeviceName() {
        return deviceName;
    }

    /**
     * Sets the device name.
     *
     * @param deviceName the device name
     */
    public void setDeviceName(@Nullable String deviceName) {
        this.deviceName = deviceName;
    }

    /**
     * Gets the zone name.
     *
     * @return the zone name or null if not set
     */
    public @Nullable String getZoneName() {
        return zoneName;
    }

    /**
     * Sets the zone name.
     *
     * @param zoneName the zone name
     */
    public void setZoneName(@Nullable String zoneName) {
        this.zoneName = zoneName;
    }

    /**
     * Gets the schedule name.
     *
     * @return the schedule name or null if not set
     */
    public @Nullable String getScheduleName() {
        return scheduleName;
    }

    /**
     * Sets the schedule name.
     *
     * @param scheduleName the schedule name
     */
    public void setScheduleName(@Nullable String scheduleName) {
        this.scheduleName = scheduleName;
    }

    /**
     * Gets the event duration.
     *
     * @return the duration in seconds or null if not set
     */
    public @Nullable Integer getDuration() {
        return duration;
    }

    /**
     * Sets the event duration.
     *
     * @param duration the duration in seconds
     */
    public void setDuration(@Nullable Integer duration) {
        this.duration = duration;
    }

    /**
     * Gets the total water used.
     *
     * @return the total water in litres or null if not set
     */
    public @Nullable Double getTotalWater() {
        return totalWater;
    }

    /**
     * Sets the total water used.
     *
     * @param totalWater the total water in litres
     */
    public void setTotalWater(@Nullable Double totalWater) {
        this.totalWater = totalWater;
    }

    @Override
    public String toString() {
        return "RachioWebHookEvent [id=" + id + ", eventType=" + eventType + ", deviceId=" + deviceId + ", zoneId="
                + zoneId + ", scheduleId=" + scheduleId + ", timestamp=" + timestamp + ", summary=" + summary
                + ", subType=" + subType + ", deviceName=" + deviceName + ", zoneName=" + zoneName + ", scheduleName="
                + scheduleName + ", duration=" + duration + ", totalWater=" + totalWater + "]";
    }
}
