package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * The {@link RachioDevice} class defines a device from the Rachio API
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDevice {

    @SerializedName("id")
    private @Nullable String id;

    @SerializedName("serialNumber")
    private @Nullable String serialNumber;

    @SerializedName("macAddress")
    private @Nullable String macAddress;

    @SerializedName("name")
    private @Nullable String name;

    @SerializedName("model")
    private @Nullable String model;

    @SerializedName("status")
    private @Nullable String status;

    @SerializedName("zones")
    private @Nullable List<RachioZone> zones;

    @SerializedName("rainDelay")
    private @Nullable Integer rainDelay;

    @SerializedName("rainDelayExpiration")
    private @Nullable Long rainDelayExpiration;

    @SerializedName("waterBudget")
    private @Nullable Integer waterBudget;

    @SerializedName("paused")
    private @Nullable Boolean paused;

    @SerializedName("on")
    private @Nullable Boolean on;

    @SerializedName("scheduleModeType")
    private @Nullable String scheduleModeType;

    @SerializedName("latitude")
    private @Nullable Double latitude;

    @SerializedName("longitude")
    private @Nullable Double longitude;

    @SerializedName("timeZone")
    private @Nullable String timeZone;

    @SerializedName("elevation")
    private @Nullable Double elevation;

    @SerializedName("createdDate")
    private @Nullable String createdDate;

    @SerializedName("updatedDate")
    private @Nullable String updatedDate;

    @SerializedName("deleted")
    private boolean deleted = false;

    @SerializedName("homeKitCompatible")
    private boolean homeKitCompatible = false;

    @SerializedName("flexScheduleRules")
    private @Nullable List<Object> flexScheduleRules;

    @SerializedName("weatherIntelligence")
    private @Nullable Object weatherIntelligence;

    @SerializedName("vibrationChecked")
    private boolean vibrationChecked = false;

    @SerializedName("weatherIntelligenceSuspended")
    private boolean weatherIntelligenceSuspended = false;

    @SerializedName("rainSensor")
    private @Nullable Boolean rainSensor;

    @SerializedName("weatherSkip")
    private @Nullable Boolean weatherSkip;

    // Getters and setters

    public @Nullable String getId() {
        return id;
    }

    public void setId(@Nullable String id) {
        this.id = id;
    }

    public @Nullable String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(@Nullable String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public @Nullable String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(@Nullable String macAddress) {
        this.macAddress = macAddress;
    }

    public @Nullable String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public @Nullable String getModel() {
        return model;
    }

    public void setModel(@Nullable String model) {
        this.model = model;
    }

    public @Nullable String getStatus() {
        return status;
    }

    public void setStatus(@Nullable String status) {
        this.status = status;
    }

    public @Nullable List<RachioZone> getZones() {
        return zones;
    }

    public void setZones(@Nullable List<RachioZone> zones) {
        this.zones = zones;
    }

    public @Nullable Integer getRainDelay() {
        return rainDelay;
    }

    public void setRainDelay(@Nullable Integer rainDelay) {
        this.rainDelay = rainDelay;
    }

    public @Nullable Long getRainDelayExpiration() {
        return rainDelayExpiration;
    }

    public void setRainDelayExpiration(@Nullable Long rainDelayExpiration) {
        this.rainDelayExpiration = rainDelayExpiration;
    }

    public @Nullable Integer getWaterBudget() {
        return waterBudget;
    }

    public void setWaterBudget(@Nullable Integer waterBudget) {
        this.waterBudget = waterBudget;
    }

    public @Nullable Boolean isPaused() {
        return paused;
    }

    public void setPaused(@Nullable Boolean paused) {
        this.paused = paused;
    }

    public @Nullable Boolean isOn() {
        return on;
    }

    public void setOn(@Nullable Boolean on) {
        this.on = on;
    }

    public @Nullable String getScheduleModeType() {
        return scheduleModeType;
    }

    public void setScheduleModeType(@Nullable String scheduleModeType) {
        this.scheduleModeType = scheduleModeType;
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

    public @Nullable String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(@Nullable String timeZone) {
        this.timeZone = timeZone;
    }

    public @Nullable Double getElevation() {
        return elevation;
    }

    public void setElevation(@Nullable Double elevation) {
        this.elevation = elevation;
    }

    public @Nullable String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(@Nullable String createdDate) {
        this.createdDate = createdDate;
    }

    public @Nullable String getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(@Nullable String updatedDate) {
        this.updatedDate = updatedDate;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isHomeKitCompatible() {
        return homeKitCompatible;
    }

    public void setHomeKitCompatible(boolean homeKitCompatible) {
        this.homeKitCompatible = homeKitCompatible;
    }

    public @Nullable List<Object> getFlexScheduleRules() {
        return flexScheduleRules;
    }

    public void setFlexScheduleRules(@Nullable List<Object> flexScheduleRules) {
        this.flexScheduleRules = flexScheduleRules;
    }

    public @Nullable Object getWeatherIntelligence() {
        return weatherIntelligence;
    }

    public void setWeatherIntelligence(@Nullable Object weatherIntelligence) {
        this.weatherIntelligence = weatherIntelligence;
    }

    public boolean isVibrationChecked() {
        return vibrationChecked;
    }

    public void setVibrationChecked(boolean vibrationChecked) {
        this.vibrationChecked = vibrationChecked;
    }

    public boolean isWeatherIntelligenceSuspended() {
        return weatherIntelligenceSuspended;
    }

    public void setWeatherIntelligenceSuspended(boolean weatherIntelligenceSuspended) {
        this.weatherIntelligenceSuspended = weatherIntelligenceSuspended;
    }

    public @Nullable Boolean isRainSensor() {
        return rainSensor;
    }

    public void setRainSensor(@Nullable Boolean rainSensor) {
        this.rainSensor = rainSensor;
    }

    public @Nullable Boolean isWeatherSkip() {
        return weatherSkip;
    }

    public void setWeatherSkip(@Nullable Boolean weatherSkip) {
        this.weatherSkip = weatherSkip;
    }

    // Helper methods

    public boolean isOnline() {
        return "ONLINE".equals(status);
    }

    public boolean isOffline() {
        return "OFFLINE".equals(status);
    }

    public int getZoneCount() {
        return zones != null ? zones.size() : 0;
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

    public @Nullable RachioZone getZoneByNumber(int zoneNumber) {
        if (zones != null) {
            for (RachioZone zone : zones) {
                if (zone.getZoneNumber() == zoneNumber) {
                    return zone;
                }
            }
        }
        return null;
    }

    public boolean hasActiveRainDelay() {
        if (rainDelay != null && rainDelay > 0) {
            if (rainDelayExpiration != null) {
                return System.currentTimeMillis() < rainDelayExpiration * 1000;
            }
            return true;
        }
        return false;
    }

    public long getRainDelayRemainingSeconds() {
        if (hasActiveRainDelay() && rainDelayExpiration != null) {
            long remaining = (rainDelayExpiration * 1000) - System.currentTimeMillis();
            return Math.max(0, remaining / 1000);
        }
        return 0;
    }

    public String getDeviceSummary() {
        StringBuilder summary = new StringBuilder();
        if (name != null) {
            summary.append(name);
        }
        if (model != null) {
            summary.append(" (").append(model).append(")");
        }
        summary.append(" - ").append(status != null ? status : "UNKNOWN");
        if (zones != null) {
            summary.append(" - ").append(zones.size()).append(" zones");
        }
        return summary.toString();
    }

    public boolean hasProfessionalIrrigationData() {
        if (zones != null) {
            for (RachioZone zone : zones) {
                if (zone.hasProfessionalData()) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getEnabledZoneCount() {
        int count = 0;
        if (zones != null) {
            for (RachioZone zone : zones) {
                if (zone.isEnabled()) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public String toString() {
        return "RachioDevice{" +
                "id='" + id + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", name='" + name + '\'' +
                ", model='" + model + '\'' +
                ", status='" + status + '\'' +
                ", zones=" + (zones != null ? zones.size() : 0) +
                ", rainDelay=" + rainDelay +
                ", waterBudget=" + waterBudget +
                ", paused=" + paused +
                ", on=" + on +
                ", rainSensor=" + rainSensor +
                ", weatherSkip=" + weatherSkip +
                '}';
    }

    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RachioDevice [");
        sb.append("id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", model=").append(model);
        sb.append(", status=").append(status);
        sb.append(", serial=").append(serialNumber);
        sb.append(", mac=").append(macAddress);
        sb.append(", zones=").append(getZoneCount());
        sb.append(", enabledZones=").append(getEnabledZoneCount());
        sb.append(", rainDelay=").append(rainDelay).append(" hrs");
        sb.append(", waterBudget=").append(waterBudget).append("%");
        sb.append(", paused=").append(paused);
        sb.append(", on=").append(on);
        sb.append(", rainSensor=").append(rainSensor);
        sb.append(", weatherSkip=").append(weatherSkip);
        if (latitude != null && longitude != null) {
            sb.append(", location=(").append(latitude).append(", ").append(longitude).append(")");
        }
        sb.append("]");
        return sb.toString();
    }
}
