package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for Rachio Device
 *
 * @author Brian G. - Initial contribution (from 2.5 binding)
 * @author Daniel B. - Major rewrite with professional data
 */
@NonNullByDefault
public class RachioDevice {
    private String id = "";
    private String name = "";
    private String serialNumber = "";
    private String model = "";
    private String macAddress = "";
    private String firmwareVersion = "";
    private String status = "";
    
    @SerializedName("online")
    private boolean online = false;
    
    @SerializedName("paused")
    private boolean paused = false;
    
    @SerializedName("createdDate")
    private @Nullable Instant createdDate;
    
    @SerializedName("updatedDate")
    private @Nullable Instant updatedDate;
    
    @SerializedName("lastHeardFrom")
    private @Nullable Instant lastHeardFrom;
    
    @SerializedName("rainDelayExpirationDate")
    private @Nullable Instant rainDelayExpirationDate;
    
    @SerializedName("rainDelayStartDate")
    private @Nullable Instant rainDelayStartDate;
    
    @SerializedName("zones")
    private List<RachioZone> zones = new ArrayList<>();
    
    @SerializedName("schedules")
    private List<Object> schedules = new ArrayList<>();
    
    @SerializedName("flexScheduleRules")
    private List<Object> flexScheduleRules = new ArrayList<>();
    
    @SerializedName("weatherIntelligence")
    private @Nullable WeatherIntelligence weatherIntelligence;
    
    @SerializedName("latitude")
    private @Nullable Double latitude;
    
    @SerializedName("longitude")
    private @Nullable Double longitude;
    
    @SerializedName("elevation")
    private @Nullable Double elevation; // feet
    
    @SerializedName("zip")
    private @Nullable String zipCode;
    
    @SerializedName("timezone")
    private @Nullable String timezone;
    
    @SerializedName("wateringScaleFactor")
    private @Nullable Double wateringScaleFactor;
    
    @SerializedName("wateringScaleStartDate")
    private @Nullable Instant wateringScaleStartDate;
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSerialNumber() {
        return serialNumber;
    }
    
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getMacAddress() {
        return macAddress;
    }
    
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
    
    public String getFirmwareVersion() {
        return firmwareVersion;
    }
    
    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public boolean isOnline() {
        return online;
    }
    
    public void setOnline(boolean online) {
        this.online = online;
    }
    
    public boolean isPaused() {
        return paused;
    }
    
    public void setPaused(boolean paused) {
        this.paused = paused;
    }
    
    public @Nullable Instant getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(@Nullable Instant createdDate) {
        this.createdDate = createdDate;
    }
    
    public @Nullable Instant getUpdatedDate() {
        return updatedDate;
    }
    
    public void setUpdatedDate(@Nullable Instant updatedDate) {
        this.updatedDate = updatedDate;
    }
    
    public @Nullable Instant getLastHeardFrom() {
        return lastHeardFrom;
    }
    
    public void setLastHeardFrom(@Nullable Instant lastHeardFrom) {
        this.lastHeardFrom = lastHeardFrom;
    }
    
    public @Nullable Instant getRainDelayExpirationDate() {
        return rainDelayExpirationDate;
    }
    
    public void setRainDelayExpirationDate(@Nullable Instant rainDelayExpirationDate) {
        this.rainDelayExpirationDate = rainDelayExpirationDate;
    }
    
    public @Nullable Instant getRainDelayStartDate() {
        return rainDelayStartDate;
    }
    
    public void setRainDelayStartDate(@Nullable Instant rainDelayStartDate) {
        this.rainDelayStartDate = rainDelayStartDate;
    }
    
    public List<RachioZone> getZones() {
        return zones;
    }
    
    public void setZones(List<RachioZone> zones) {
        this.zones = zones;
    }
    
    public List<Object> getSchedules() {
        return schedules;
    }
    
    public void setSchedules(List<Object> schedules) {
        this.schedules = schedules;
    }
    
    public List<Object> getFlexScheduleRules() {
        return flexScheduleRules;
    }
    
    public void setFlexScheduleRules(List<Object> flexScheduleRules) {
        this.flexScheduleRules = flexScheduleRules;
    }
    
    public @Nullable WeatherIntelligence getWeatherIntelligence() {
        return weatherIntelligence;
    }
    
    public void setWeatherIntelligence(@Nullable WeatherIntelligence weatherIntelligence) {
        this.weatherIntelligence = weatherIntelligence;
    }
    
    public @Nullable Double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(@Nullable Double latitude) {
        this.latitude = latitude;
    }
    
    public @Nullable Double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(@Nullable Double longitude) {
        this.longitude = longitude;
    }
    
    public @Nullable Double getElevation() {
        return elevation;
    }
    
    public void setElevation(@Nullable Double elevation) {
        this.elevation = elevation;
    }
    
    public @Nullable String getZipCode() {
        return zipCode;
    }
    
    public void setZipCode(@Nullable String zipCode) {
        this.zipCode = zipCode;
    }
    
    public @Nullable String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(@Nullable String timezone) {
        this.timezone = timezone;
    }
    
    public @Nullable Double getWateringScaleFactor() {
        return wateringScaleFactor;
    }
    
    public void setWateringScaleFactor(@Nullable Double wateringScaleFactor) {
        this.wateringScaleFactor = wateringScaleFactor;
    }
    
    public @Nullable Instant getWateringScaleStartDate() {
        return wateringScaleStartDate;
    }
    
    public void setWateringScaleStartDate(@Nullable Instant wateringScaleStartDate) {
        this.wateringScaleStartDate = wateringScaleStartDate;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RachioDevice that = (RachioDevice) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "RachioDevice{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", model='" + model + '\'' +
                ", online=" + online +
                ", paused=" + paused +
                ", zones=" + zones.size() +
                ", weatherIntel=" + (weatherIntelligence != null ? "enabled" : "disabled") +
                '}';
    }
}

/**
 * Weather Intelligence DTO
 */
@NonNullByDefault
class WeatherIntelligence {
    private boolean enabled = false;
    private @Nullable String status;
    private @Nullable String skipReason;
    private @Nullable Instant lastSkipDate;
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public @Nullable String getStatus() {
        return status;
    }
    
    public void setStatus(@Nullable String status) {
        this.status = status;
    }
    
    public @Nullable String getSkipReason() {
        return skipReason;
    }
    
    public void setSkipReason(@Nullable String skipReason) {
        this.skipReason = skipReason;
    }
    
    public @Nullable Instant getLastSkipDate() {
        return lastSkipDate;
    }
    
    public void setLastSkipDate(@Nullable Instant lastSkipDate) {
        this.lastSkipDate = lastSkipDate;
    }
}
