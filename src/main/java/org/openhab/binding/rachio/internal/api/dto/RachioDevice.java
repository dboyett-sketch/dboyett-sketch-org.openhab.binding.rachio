package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for Rachio device
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDevice {
    public @Nullable String id;
    public @Nullable String name;
    public @Nullable String model;
    
    @SerializedName("serialNumber")
    public @Nullable String serialNumber;
    
    @SerializedName("macAddress")
    public @Nullable String macAddress;
    
    public @Nullable String status;
    
    @SerializedName("rainDelay")
    public @Nullable Integer rainDelay;
    
    @SerializedName("rainDelayExpirationDate")
    public @Nullable Instant rainDelayExpirationDate;
    
    @SerializedName("zones")
    public @Nullable List<RachioZone> zones;
    
    @SerializedName("createdDate")
    public @Nullable Instant createdDate;
    
    @SerializedName("updatedDate")
    public @Nullable Instant updatedDate;
    
    // Professional irrigation data
    @SerializedName("elevation")
    public @Nullable Double elevation;
    
    @SerializedName("flexScheduleRules")
    public @Nullable Boolean flexScheduleRules;
    
    @SerializedName("latitude")
    public @Nullable Double latitude;
    
    @SerializedName("longitude")
    public @Nullable Double longitude;
    
    @SerializedName("timeZone")
    public @Nullable String timeZone;
    
    @SerializedName("scheduleModeType")
    public @Nullable String scheduleModeType;
    
    // Getters for compatibility with existing code
    public @Nullable String getId() {
        return id;
    }
    
    public @Nullable String getName() {
        return name;
    }
    
    public @Nullable String getModel() {
        return model;
    }
    
    public @Nullable String getSerialNumber() {
        return serialNumber;
    }
    
    public @Nullable String getMacAddress() {
        return macAddress;
    }
    
    public @Nullable String getStatus() {
        return status;
    }
    
    public @Nullable Integer getRainDelay() {
        return rainDelay;
    }
    
    public @Nullable Instant getRainDelayExpirationDate() {
        return rainDelayExpirationDate;
    }
    
    public @Nullable List<RachioZone> getZones() {
        return zones;
    }
    
    public @Nullable Instant getCreatedDate() {
        return createdDate;
    }
    
    public @Nullable Instant getUpdatedDate() {
        return updatedDate;
    }
    
    public @Nullable Double getElevation() {
        return elevation;
    }
    
    public @Nullable Boolean isFlexScheduleRules() {
        return flexScheduleRules;
    }
    
    public @Nullable Double getLatitude() {
        return latitude;
    }
    
    public @Nullable Double getLongitude() {
        return longitude;
    }
    
    public @Nullable String getTimeZone() {
        return timeZone;
    }
    
    public @Nullable String getScheduleModeType() {
        return scheduleModeType;
    }
}
