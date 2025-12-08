package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for Rachio Device
 *
 * @author Damion Boyett - Enhanced with professional features
 */
@NonNullByDefault
public class RachioDevice {
    @SerializedName("id")
    public String id = "";
    
    @SerializedName("status")
    public String status = "";
    
    @SerializedName("name")
    public String name = "";
    
    @SerializedName("model")
    public String model = "";
    
    @SerializedName("serialNumber")
    public String serialNumber = "";
    
    @SerializedName("macAddress")
    public String macAddress = "";
    
    @SerializedName("createdDate")
    public @Nullable Instant createdDate;
    
    @SerializedName("homeKitCompatible")
    public boolean homeKitCompatible;
    
    @SerializedName("latitude")
    public double latitude;
    
    @SerializedName("longitude")
    public double longitude;
    
    @SerializedName("elevation")
    public double elevation;
    
    @SerializedName("scheduleModeType")
    public @Nullable String scheduleModeType;
    
    @SerializedName("scheduleModeType")
    public @Nullable String scheduleMode;
    
    @SerializedName("scheduleModeModifiedDate")
    public @Nullable Instant scheduleModeModifiedDate;
    
    @SerializedName("timeZone")
    public @Nullable String timeZone;
    
    @SerializedName("zones")
    public @Nullable List<RachioZone> zones;
    
    @SerializedName("zoneCount")
    public int zoneCount;
    
    @SerializedName("on")
    public boolean on;
    
    @SerializedName("rainDelayExpirationDate")
    public @Nullable Instant rainDelayExpirationDate;
    
    @SerializedName("rainDelayStartDate")
    public @Nullable Instant rainDelayStartDate;
    
    @SerializedName("rainDelayState")
    public boolean rainDelayState;
    
    @SerializedName("flexScheduleRules")
    public boolean flexScheduleRules;
    
    // Professional irrigation analytics
    @SerializedName("totalWateringTime")
    public @Nullable Long totalWateringTime;
    
    @SerializedName("totalWaterUsage")
    public @Nullable Double totalWaterUsage;
    
    @SerializedName("waterSavings")
    public @Nullable Double waterSavings;
    
    @SerializedName("moneySavings")
    public @Nullable Double moneySavings;
    
    @SerializedName("co2Savings")
    public @Nullable Double co2Savings;
    
    // Device health metrics
    @SerializedName("batteryLevel")
    public @Nullable Integer batteryLevel;
    
    @SerializedName("rssi")
    public @Nullable Integer rssi;
    
    @SerializedName("signalStrength")
    public @Nullable Integer signalStrength;
    
    @SerializedName("lastHeardFrom")
    public @Nullable Instant lastHeardFrom;
    
    @SerializedName("firmwareVersion")
    public @Nullable String firmwareVersion;
    
    // Add getters for all fields
    public String getId() {
        return id;
    }
    
    public String getStatus() {
        return status;
    }
    
    public String getName() {
        return name;
    }
    
    public String getModel() {
        return model;
    }
    
    public String getSerialNumber() {
        return serialNumber;
    }
    
    public String getMacAddress() {
        return macAddress;
    }
    
    @Nullable
    public Instant getCreatedDate() {
        return createdDate;
    }
    
    public boolean isHomeKitCompatible() {
        return homeKitCompatible;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public double getElevation() {
        return elevation;
    }
    
    @Nullable
    public String getScheduleModeType() {
        return scheduleModeType;
    }
    
    @Nullable
    public String getScheduleMode() {
        return scheduleMode;
    }
    
    @Nullable
    public Instant getScheduleModeModifiedDate() {
        return scheduleModeModifiedDate;
    }
    
    @Nullable
    public String getTimeZone() {
        return timeZone;
    }
    
    @Nullable
    public List<RachioZone> getZones() {
        return zones;
    }
    
    public int getZoneCount() {
        return zoneCount;
    }
    
    public boolean isOn() {
        return on;
    }
    
    @Nullable
    public Instant getRainDelayExpirationDate() {
        return rainDelayExpirationDate;
    }
    
    @Nullable
    public Instant getRainDelayStartDate() {
        return rainDelayStartDate;
    }
    
    public boolean isRainDelayState() {
        return rainDelayState;
    }
    
    public boolean isFlexScheduleRules() {
        return flexScheduleRules;
    }
    
    @Nullable
    public Long getTotalWateringTime() {
        return totalWateringTime;
    }
    
    @Nullable
    public Double getTotalWaterUsage() {
        return totalWaterUsage;
    }
    
    @Nullable
    public Double getWaterSavings() {
        return waterSavings;
    }
    
    @Nullable
    public Double getMoneySavings() {
        return moneySavings;
    }
    
    @Nullable
    public Double getCo2Savings() {
        return co2Savings;
    }
    
    @Nullable
    public Integer getBatteryLevel() {
        return batteryLevel;
    }
    
    @Nullable
    public Integer getRssi() {
        return rssi;
    }
    
    @Nullable
    public Integer getSignalStrength() {
        return signalStrength;
    }
    
    @Nullable
    public Instant getLastHeardFrom() {
        return lastHeardFrom;
    }
    
    @Nullable
    public String getFirmwareVersion() {
        return firmwareVersion;
    }
    
    // Setters for mutable fields
    public void setStatus(String status) {
        this.status = status;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setZones(@Nullable List<RachioZone> zones) {
        this.zones = zones;
        if (zones != null) {
            this.zoneCount = zones.size();
        }
    }
    
    public void setOn(boolean on) {
        this.on = on;
    }
    
    public void setRainDelayState(boolean rainDelayState) {
        this.rainDelayState = rainDelayState;
    }
    
    // Professional utility methods
    public boolean isOnline() {
        return "ONLINE".equals(status);
    }
    
    public boolean isOffline() {
        return "OFFLINE".equals(status);
    }
    
    public boolean isSleep() {
        return "SLEEP".equals(status);
    }
    
    public boolean isWatering() {
        if (zones != null) {
            for (RachioZone zone : zones) {
                if ("STARTED".equals(zone.getZoneRunStatus())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public @Nullable RachioZone getZoneById(String zoneId) {
        if (zones != null) {
            for (RachioZone zone : zones) {
                if (zoneId.equals(zone.getId())) {
                    return zone;
                }
            }
        }
        return null;
    }
    
    public @Nullable RachioZone getZoneByName(String zoneName) {
        if (zones != null) {
            for (RachioZone zone : zones) {
                if (zoneName.equals(zone.getName())) {
                    return zone;
                }
            }
        }
        return null;
    }
    
    public int getActiveZoneCount() {
        int active = 0;
        if (zones != null) {
            for (RachioZone zone : zones) {
                if (zone.isEnabled()) {
                    active++;
                }
            }
        }
        return active;
    }
    
    public double getTotalZoneArea() {
        double total = 0.0;
        if (zones != null) {
            for (RachioZone zone : zones) {
                total += zone.getArea();
            }
        }
        return total;
    }
    
    public ThingStatus getThingStatus() {
        if (!on) {
            return ThingStatus.OFFLINE;
        }
        if (rainDelayState) {
            return ThingStatus.ONLINE; // Device is online but in rain delay
        }
        return isOnline() ? ThingStatus.ONLINE : ThingStatus.OFFLINE;
    }
    
    public ThingStatusDetail getThingStatusDetail() {
        if (!on) {
            return ThingStatusDetail.DISABLED;
        }
        if (rainDelayState) {
            return ThingStatusDetail.NONE; // Fixed: Use NONE instead of non-existent RAIN_DELAY
        }
        if ("PAUSED".equals(scheduleMode)) {
            return ThingStatusDetail.PAUSED; // Fixed: Use PAUSED constant
        }
        return ThingStatusDetail.NONE;
    }
    
    public String getThingStatusDescription() {
        if (!on) {
            return "Device turned off";
        }
        if (rainDelayState) {
            return "Rain delay active";
        }
        if ("PAUSED".equals(scheduleMode)) {
            return "Schedule paused";
        }
        if (isWatering()) {
            return "Watering in progress";
        }
        return status;
    }
    
    public boolean hasRainDelay() {
        return rainDelayState && rainDelayExpirationDate != null;
    }
    
    public long getRainDelayRemainingSeconds() {
        if (rainDelayExpirationDate != null) {
            Instant now = Instant.now();
            if (rainDelayExpirationDate.isAfter(now)) {
                return rainDelayExpirationDate.getEpochSecond() - now.getEpochSecond();
            }
        }
        return 0;
    }
    
    public boolean isInFlexSchedule() {
        return "FLEX".equals(scheduleModeType);
    }
    
    public boolean isInFixedSchedule() {
        return "FIXED".equals(scheduleModeType);
    }
    
    public boolean isInManualSchedule() {
        return "MANUAL".equals(scheduleModeType);
    }
    
    // Professional analytics methods
    public double getWaterEfficiency() {
        if (totalWaterUsage != null && totalWaterUsage > 0 && waterSavings != null) {
            return (waterSavings / (waterSavings + totalWaterUsage)) * 100.0;
        }
        return 0.0;
    }
    
    public String getDeviceSummary() {
        return String.format("%s (%s) - %s, Zones: %d/%d, Status: %s", 
                name, model, serialNumber, getActiveZoneCount(), zoneCount, status);
    }
    
    @Override
    public String toString() {
        return "RachioDevice{id='" + id + "', name='" + name + "', model='" + model + 
               "', serialNumber='" + serialNumber + "', status='" + status + 
               "', zones=" + zoneCount + ", online=" + isOnline() + 
               ", rainDelay=" + rainDelayState + ", flexSchedule=" + flexScheduleRules + 
               ", on=" + on + ", scheduleMode='" + scheduleMode + "'}";
    }
}
