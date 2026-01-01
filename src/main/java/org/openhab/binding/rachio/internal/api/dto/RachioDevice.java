package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for Rachio device
 * 
 * @author dboyett-sketch
 */
@NonNullByDefault
public class RachioDevice {
    // ===== CRITICAL PATCH START: Initialize all fields for Gson safety =====
    // COMPILATION WARNING ADDRESSED: "Potential null pointer access"
    // EVIDENCE: Gson sets fields AFTER constructor, bypassing @NonNullByDefault
    // SOLUTION: Initialize all fields and add safe getter methods

    @SerializedName("id")
    public String id = "";

    @SerializedName("name")
    public String name = "";

    // ===== PATCHED: Remove @Nullable - use empty string default instead =====
    // EVIDENCE: Maven warning "The nullness annotation is redundant with a default"
    // SOLUTION: Initialize to empty string, handle in getters
    @SerializedName("status")
    public String status = "";

    // ===== PATCHED: Remove @Nullable, initialize both serial fields to same default =====
    // EVIDENCE: Warnings for redundant null checks on serial fields
    // SOLUTION: Single initialization for both serial aliases
    @SerializedName("serial")
    public String serial = "";

    // ===== PATCHED: Alias field properly initialized =====
    @SerializedName("serial")
    public String serialNumber = "";

    @SerializedName("model")
    public String model = "";

    // ===== PATCHED: Initialize collections to empty lists =====
    // EVIDENCE: Potential null pointer access warnings
    // SOLUTION: Use empty ArrayList for Gson to populate
    @SerializedName("zones")
    public List<RachioZone> zones = new ArrayList<>();

    @SerializedName("scheduleRules")
    public List<Object> scheduleRules = new ArrayList<>();

    @SerializedName("flexScheduleRules")
    public List<Object> flexScheduleRules = new ArrayList<>();

    @SerializedName("imageUrl")
    public String imageUrl = "";

    @SerializedName("latitude")
    public double latitude = 0.0;

    @SerializedName("longitude")
    public double longitude = 0.0;

    // ===== PATCHED: Instant fields remain @Nullable (time can be null) =====
    // EVIDENCE: Time fields can legitimately be null in API responses
    // SOLUTION: Keep @Nullable but add safe getters
    @SerializedName("createdDate")
    public @Nullable Instant createdDate;

    @SerializedName("scheduleDataModified")
    public boolean scheduleDataModified = false;

    @SerializedName("paused")
    public boolean paused = false;

    @SerializedName("enabled")
    public boolean enabled = true; // Default to enabled

    @SerializedName("deleted")
    public boolean deleted = false;

    @SerializedName("on")
    public boolean on = false;

    @SerializedName("rainDelay")
    public int rainDelay = 0;

    @SerializedName("rainDelayExpiration")
    public @Nullable Instant rainDelayExpiration;

    @SerializedName("scheduleModeType")
    public String scheduleModeType = "AUTO";

    @SerializedName("macAddress")
    public String macAddress = "";

    @SerializedName("homeKitCompatible")
    public boolean homeKitCompatible = false;

    @SerializedName("etSkip")
    public boolean etSkip = false;

    @SerializedName("masterValve")
    public boolean masterValve = false;

    @SerializedName("cycleSoak")
    public boolean cycleSoak = false;

    @SerializedName("wateringInProgress")
    public boolean wateringInProgress = false;

    // ===== PATCHED: Object fields remain @Nullable (can be complex types) =====
    @SerializedName("location")
    public @Nullable Object location;

    @SerializedName("usage")
    public @Nullable RachioUsage usage;

    @SerializedName("forecast")
    public @Nullable RachioForecast forecast;

    @SerializedName("savings")
    public @Nullable RachioSavings savings;

    @SerializedName("person")
    public @Nullable RachioPerson person;

    @SerializedName("alerts")
    public List<RachioAlert> alerts = new ArrayList<>();

    /**
     * ===== CRITICAL PATCH: Default constructor for Gson =====
     * Gson requires a no-args constructor
     */
    public RachioDevice() {
        // All fields initialized at declaration for Gson safety
    }

    /**
     * ===== CRITICAL PATCH: Copy constructor for safe updates =====
     */
    public RachioDevice(RachioDevice other) {
        this.id = other.id;
        this.name = other.name;
        this.status = other.status;
        this.serial = other.serial;
        this.serialNumber = other.serialNumber;
        this.model = other.model;
        this.zones = new ArrayList<>(other.zones);
        this.scheduleRules = new ArrayList<>(other.scheduleRules);
        this.flexScheduleRules = new ArrayList<>(other.flexScheduleRules);
        this.imageUrl = other.imageUrl;
        this.latitude = other.latitude;
        this.longitude = other.longitude;
        this.createdDate = other.createdDate;
        this.scheduleDataModified = other.scheduleDataModified;
        this.paused = other.paused;
        this.enabled = other.enabled;
        this.deleted = other.deleted;
        this.on = other.on;
        this.rainDelay = other.rainDelay;
        this.rainDelayExpiration = other.rainDelayExpiration;
        this.scheduleModeType = other.scheduleModeType;
        this.macAddress = other.macAddress;
        this.homeKitCompatible = other.homeKitCompatible;
        this.etSkip = other.etSkip;
        this.masterValve = other.masterValve;
        this.cycleSoak = other.cycleSoak;
        this.wateringInProgress = other.wateringInProgress;
        this.location = other.location;
        this.usage = other.usage;
        this.forecast = other.forecast;
        this.savings = other.savings;
        this.person = other.person;
        this.alerts = new ArrayList<>(other.alerts);
    }
    // ===== CRITICAL PATCH END =====

    /**
     * Get device ID safely
     * ===== PATCHED: Remove redundant null check =====
     * EVIDENCE: id initialized to "" and marked @NonNullByDefault
     * SOLUTION: Direct return since field cannot be null
     */
    public String getId() {
        return id;
    }

    /**
     * Get device name safely
     * ===== PATCHED: Remove redundant null check =====
     */
    public String getName() {
        return name;
    }

    /**
     * Get device status
     * ===== PATCHED: Direct return with empty string default =====
     */
    public String getStatus() {
        return status;
    }

    /**
     * Get device summary
     * ===== PATCHED: Use isEmpty() instead of null checks =====
     * EVIDENCE: Maven warning "Redundant null check"
     * SOLUTION: Check for empty strings, not null
     */
    public String getSummary() {
        if (!id.isEmpty() && !name.isEmpty()) {
            return id + " - " + name;
        } else if (!id.isEmpty()) {
            return id;
        } else if (!name.isEmpty()) {
            return name;
        }
        return "unknown - unnamed";
    }

    @Override
    public String toString() {
        // ===== PATCHED: Use isEmpty() pattern =====
        if (!id.isEmpty() && !name.isEmpty()) {
            return id + " - " + name;
        } else if (!id.isEmpty()) {
            return id;
        } else if (!name.isEmpty()) {
            return name;
        }
        return "RachioDevice[unknown]";
    }

    /**
     * Get display name for UI
     * ===== PATCHED: Use isEmpty() pattern =====
     */
    public String getDisplayName() {
        if (!id.isEmpty() && !name.isEmpty()) {
            return id + " - " + name;
        } else if (!name.isEmpty()) {
            return name;
        } else if (!id.isEmpty()) {
            return "Device " + id;
        }
        return "Unknown Device";
    }

    /**
     * Get short identifier
     * ===== PATCHED: Use isEmpty() pattern =====
     */
    public String getShortId() {
        if (id.length() > 8) {
            return id.substring(0, 8) + "...";
        }
        return !id.isEmpty() ? id : "unknown";
    }

    /**
     * Get log identifier
     * ===== PATCHED: Use isEmpty() pattern =====
     */
    public String getLogId() {
        if (!id.isEmpty() && !name.isEmpty()) {
            return id + " (" + name + ")";
        } else if (!id.isEmpty()) {
            return id;
        } else if (!name.isEmpty()) {
            return name;
        }
        return "unknown-device";
    }

    /**
     * Check if device is online
     */
    public boolean isOnline() {
        return "ONLINE".equals(status);
    }

    /**
     * Check if device is offline
     */
    public boolean isOffline() {
        return "OFFLINE".equals(status);
    }

    /**
     * Check if device is in sleep mode
     */
    public boolean isSleeping() {
        return "SLEEP".equals(status);
    }

    /**
     * Get zones count safely
     * ===== PATCHED: Direct access, zones initialized to empty list =====
     */
    public int getZonesCount() {
        return zones.size();
    }

    /**
     * Check if device has zones
     * ===== PATCHED: Direct access, zones initialized to empty list =====
     */
    public boolean hasZones() {
        return !zones.isEmpty();
    }

    /**
     * Check if device is paused
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Check if device is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Check if device is deleted
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Check if device is powered on
     */
    public boolean isPoweredOn() {
        return on;
    }

    /**
     * Get rain delay in hours
     */
    public int getRainDelayHours() {
        return rainDelay / 3600; // Convert seconds to hours
    }

    /**
     * Check if rain delay is active
     */
    public boolean hasRainDelay() {
        return rainDelay > 0;
    }

    /**
     * Get image URL safely
     * ===== PATCHED: Return empty string instead of null =====
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Get coordinates
     * ===== PATCHED: Direct string concatenation =====
     */
    public String getCoordinates() {
        return latitude + "," + longitude;
    }

    /**
     * Check if device has location data
     */
    public boolean hasLocation() {
        return latitude != 0.0 || longitude != 0.0;
    }

    /**
     * Check if device has schedule data modified
     */
    public boolean hasScheduleDataModified() {
        return scheduleDataModified;
    }

    /**
     * Check if device is homekit compatible
     */
    public boolean isHomeKitCompatible() {
        return homeKitCompatible;
    }

    /**
     * Check if ET skip is enabled
     */
    public boolean isEtSkipEnabled() {
        return etSkip;
    }

    /**
     * Check if master valve is present
     */
    public boolean hasMasterValve() {
        return masterValve;
    }

    /**
     * Check if cycle soak is enabled
     */
    public boolean isCycleSoakEnabled() {
        return cycleSoak;
    }

    /**
     * Check if watering is in progress
     */
    public boolean isWateringInProgress() {
        return wateringInProgress;
    }

    /**
     * Get serial number safely
     * ===== PATCHED: Use serialNumber field directly =====
     */
    public String getSerialSafe() {
        return !serialNumber.isEmpty() ? serialNumber : serial;
    }

    /**
     * Get model safely
     * ===== PATCHED: Direct field access =====
     */
    public String getModelSafe() {
        return model;
    }

    /**
     * Get MAC address safely
     * ===== PATCHED: Direct field access =====
     */
    public String getMacAddressSafe() {
        return macAddress;
    }

    /**
     * Get schedule mode safely
     * ===== PATCHED: Direct field access =====
     */
    public String getScheduleModeSafe() {
        return !scheduleModeType.isEmpty() ? scheduleModeType : "AUTO";
    }

    // ===== CRITICAL PATCH: Safe getters for backward compatibility =====
    // These methods are called by RachioDeviceHandler.java
    // Must maintain exact method signatures for compilation

    /**
     * Get serial number with null safety
     * ===== PATCHED: Direct field access =====
     */
    public String getSerialNumberSafe() {
        return serialNumber;
    }

    /**
     * Get MAC address with null safety
     * ===== PATCHED: Direct field access =====
     */
    public String getMacAddressNonNull() {
        return macAddress;
    }

    /**
     * Get model with null safety
     * ===== PATCHED: Direct field access =====
     */
    public String getModelNonNull() {
        return model;
    }

    /**
     * Get status with null safety
     * ===== PATCHED: Direct field access =====
     */
    public String getStatusNonNull() {
        return !status.isEmpty() ? status : "UNKNOWN";
    }

    // ===== CRITICAL PATCH: Added equals() and hashCode() for completeness =====
    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RachioDevice other = (RachioDevice) obj;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
