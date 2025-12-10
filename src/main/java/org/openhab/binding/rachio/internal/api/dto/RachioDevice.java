package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a Rachio device (controller)
 *
 * @author Brian Gleeson - Initial contribution
 * @author Michael F. - Adaptations for OH3/OH4
 * @author Damion B. - Professional features for OH5
 */
@NonNullByDefault
public class RachioDevice {
    @SerializedName("id")
    public String id = "";

    @SerializedName("name")
    public String name = "";

    @SerializedName("createdDate")
    public Instant createdDate = Instant.now();

    @SerializedName("latitude")
    public double latitude = 0.0;

    @SerializedName("longitude")
    public double longitude = 0.0;

    @SerializedName("timeZone")
    public String timeZone = "";

    @SerializedName("serialNumber")
    public String serialNumber = "";

    @SerializedName("macAddress")
    public String macAddress = "";

    @SerializedName("model")
    public String model = "";

    @SerializedName("on")
    public boolean on = true;

    @SerializedName("status")
    @Nullable
    public String status;

    @SerializedName("scheduleModeType")
    @Nullable
    public String scheduleModeType;

    @SerializedName("paused")
    public boolean paused = false;

    @SerializedName("scheduleModeModifiedDate")
    @Nullable
    public Instant scheduleModeModifiedDate;

    @SerializedName("scheduleModeModifiedBy")
    @Nullable
    public String scheduleModeModifiedBy;

    @SerializedName("zones")
    @Nullable
    public List<RachioZone> zones;

    @SerializedName("flexScheduleRules")
    @Nullable
    public Object flexScheduleRules;

    @SerializedName("deleted")
    public boolean deleted = false;

    @SerializedName("homeKitCompatible")
    public boolean homeKitCompatible = false;

    @SerializedName("frostProtectTemp")
    public int frostProtectTemp = 2;

    @SerializedName("frostProtectEnabled")
    public boolean frostProtectEnabled = false;

    @SerializedName("modelVersion")
    @Nullable
    public String modelVersion;

    @SerializedName("firmwareVersion")
    @Nullable
    public String firmwareVersion;

    @SerializedName("rainDelayExpirationDate")
    @Nullable
    public Instant rainDelayExpirationDate;

    @SerializedName("rainDelayStartDate")
    @Nullable
    public Instant rainDelayStartDate;

    @SerializedName("rainDelayDuration")
    public int rainDelayDuration = 0;

    @SerializedName("generation")
    public int generation = 0;

    @SerializedName("scheduleTypes")
    @Nullable
    public List<String> scheduleTypes;

    @SerializedName("serialDataAvailable")
    public boolean serialDataAvailable = false;

    @SerializedName("softwareVersion")
    @Nullable
    public String softwareVersion;

    @SerializedName("hardwareVersion")
    @Nullable
    public String hardwareVersion;

    @SerializedName("sku")
    @Nullable
    public String sku;

    @SerializedName("batteryPowered")
    public boolean batteryPowered = false;

    @SerializedName("batteryVoltage")
    public double batteryVoltage = 0.0;

    @SerializedName("batteryPoweredRadioVersion")
    @Nullable
    public String batteryPoweredRadioVersion;

    @SerializedName("batteryPoweredModel")
    @Nullable
    public String batteryPoweredModel;

    // New fields for professional features
    @SerializedName("wateringStatus")
    @Nullable
    public String wateringStatus;

    @SerializedName("zonesEnabled")
    public int zonesEnabled = 0;

    @SerializedName("totalZones")
    public int totalZones = 0;

    @SerializedName("lastHeardFrom")
    @Nullable
    public Instant lastHeardFrom;

    @SerializedName("online")
    public boolean online = true;

    @SerializedName("sleepMode")
    public boolean sleepMode = false;

    @SerializedName("currentSchedule")
    @Nullable
    public String currentSchedule;

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getModel() {
        return model;
    }

    public boolean isOn() {
        return on;
    }

    @Nullable
    public String getStatus() {
        return status;
    }

    @Nullable
    public String getScheduleModeType() {
        return scheduleModeType;
    }

    public boolean isPaused() {
        return paused;
    }

    @Nullable
    public Instant getScheduleModeModifiedDate() {
        return scheduleModeModifiedDate;
    }

    @Nullable
    public String getScheduleModeModifiedBy() {
        return scheduleModeModifiedBy;
    }

    @Nullable
    public List<RachioZone> getZones() {
        return zones;
    }

    @Nullable
    public Object getFlexScheduleRules() {
        return flexScheduleRules;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isHomeKitCompatible() {
        return homeKitCompatible;
    }

    public int getFrostProtectTemp() {
        return frostProtectTemp;
    }

    public boolean isFrostProtectEnabled() {
        return frostProtectEnabled;
    }

    @Nullable
    public String getModelVersion() {
        return modelVersion;
    }

    @Nullable
    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    @Nullable
    public Instant getRainDelayExpirationDate() {
        return rainDelayExpirationDate;
    }

    @Nullable
    public Instant getRainDelayStartDate() {
        return rainDelayStartDate;
    }

    public int getRainDelayDuration() {
        return rainDelayDuration;
    }

    public int getGeneration() {
        return generation;
    }

    @Nullable
    public List<String> getScheduleTypes() {
        return scheduleTypes;
    }

    public boolean isSerialDataAvailable() {
        return serialDataAvailable;
    }

    @Nullable
    public String getSoftwareVersion() {
        return softwareVersion;
    }

    @Nullable
    public String getHardwareVersion() {
        return hardwareVersion;
    }

    @Nullable
    public String getSku() {
        return sku;
    }

    public boolean isBatteryPowered() {
        return batteryPowered;
    }

    public double getBatteryVoltage() {
        return batteryVoltage;
    }

    @Nullable
    public String getBatteryPoweredRadioVersion() {
        return batteryPoweredRadioVersion;
    }

    @Nullable
    public String getBatteryPoweredModel() {
        return batteryPoweredModel;
    }

    // Getters for new professional fields
    @Nullable
    public String getWateringStatus() {
        return wateringStatus;
    }

    public int getZonesEnabled() {
        return zonesEnabled;
    }

    public int getTotalZones() {
        return totalZones;
    }

    @Nullable
    public Instant getLastHeardFrom() {
        return lastHeardFrom;
    }

    public boolean isOnline() {
        return online;
    }

    public boolean isSleepMode() {
        return sleepMode;
    }

    @Nullable
    public String getCurrentSchedule() {
        return currentSchedule;
    }

    // Convenience methods
    public boolean hasZones() {
        return zones != null && !zones.isEmpty();
    }

    public int getZoneCount() {
        return zones != null ? zones.size() : 0;
    }

    public boolean isActive() {
        return on && !deleted && !paused;
    }

    public boolean hasRainDelay() {
        return rainDelayDuration > 0 && rainDelayExpirationDate != null 
            && rainDelayExpirationDate.isAfter(Instant.now());
    }

    @Override
    public String toString() {
        return "RachioDevice [id=" + id + ", name=" + name + ", model=" + model + ", on=" + on + ", paused=" + paused
                + ", firmwareVersion=" + firmwareVersion + ", zones=" + (zones != null ? zones.size() : 0) + "]";
    }
}
