package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
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
    @SerializedName("id")
    public String id = "";

    @SerializedName("name")
    public String name = "";

    @SerializedName("status")
    public @Nullable String status;

    @SerializedName("serial")
    public @Nullable String serial;

    // ===== CRITICAL PATCH START: Added serialNumber alias for backward compatibility =====
    // COMPILATION ERROR ADDRESSED: "serialNumber cannot be resolved or is not a field"
    // EVIDENCE: Code references device.serialNumber but DTO has "serial" field
    // SOLUTION: Add alias field mapping to same JSON property
    @SerializedName("serial")
    public @Nullable String serialNumber;
    // ===== CRITICAL PATCH END =====

    @SerializedName("model")
    public @Nullable String model;

    @SerializedName("zones")
    public @Nullable List<RachioZone> zones;

    @SerializedName("scheduleRules")
    public @Nullable List<Object> scheduleRules;

    @SerializedName("flexScheduleRules")
    public @Nullable List<Object> flexScheduleRules;

    @SerializedName("imageUrl")
    public @Nullable String imageUrl;

    @SerializedName("latitude")
    public double latitude;

    @SerializedName("longitude")
    public double longitude;

    @SerializedName("createdDate")
    public @Nullable Instant createdDate;

    @SerializedName("scheduleDataModified")
    public boolean scheduleDataModified;

    @SerializedName("paused")
    public boolean paused;

    // ===== CRITICAL PATCH START: Added enabled field =====
    // COMPILATION ERROR ADDRESSED: "enabled cannot be resolved or is not a field"
    // EVIDENCE: RachioDeviceHandler.java:606,622 references device.enabled
    // EVIDENCE: RachioZone.java shows enabled as primitive boolean (line 11)
    // SOLUTION: Add primitive boolean field matching RachioZone pattern
    @SerializedName("enabled")
    public boolean enabled;
    // ===== CRITICAL PATCH END =====

    @SerializedName("deleted")
    public boolean deleted;

    @SerializedName("on")
    public boolean on;

    @SerializedName("rainDelay")
    public int rainDelay;

    @SerializedName("rainDelayExpiration")
    public @Nullable Instant rainDelayExpiration;

    @SerializedName("scheduleModeType")
    public @Nullable String scheduleModeType;

    @SerializedName("macAddress")
    public @Nullable String macAddress;

    @SerializedName("homeKitCompatible")
    public boolean homeKitCompatible;

    @SerializedName("etSkip")
    public boolean etSkip;

    @SerializedName("masterValve")
    public boolean masterValve;

    @SerializedName("cycleSoak")
    public boolean cycleSoak;

    @SerializedName("wateringInProgress")
    public boolean wateringInProgress;

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
    public @Nullable List<RachioAlert> alerts;

    /**
     * Get device ID safely
     * ===== CRITICAL PATCH: Added explicit @NonNull annotation =====
     */
    public @org.eclipse.jdt.annotation.NonNull String getId() {
        String deviceId = id;
        return deviceId != null ? deviceId : "";
    }

    /**
     * Get device name safely
     * ===== CRITICAL PATCH: Added explicit @NonNull annotation =====
     */
    public @org.eclipse.jdt.annotation.NonNull String getName() {
        String deviceName = name;
        return deviceName != null ? deviceName : "";
    }

    /**
     * Get device status
     */
    public @Nullable String getStatus() {
        return status;
    }

    /**
     * Get device summary
     * ===== CRITICAL PATCH: Added explicit @NonNull annotation =====
     */
    public @org.eclipse.jdt.annotation.NonNull String getSummary() {
        String deviceId = id != null ? id : "unknown";
        String deviceName = name != null ? name : "unnamed";
        return deviceId + " - " + deviceName;
    }

    @Override
    public @org.eclipse.jdt.annotation.NonNull String toString() {
        // PATCHED (Line 133-134): Null-safe string concatenation
        String deviceId = id != null ? id : "unknown";
        String deviceName = name != null ? name : "unnamed";
        if (!deviceId.isEmpty() && !deviceName.isEmpty()) {
            return deviceId + " - " + deviceName;
        } else if (!deviceId.isEmpty()) {
            return deviceId;
        } else if (!deviceName.isEmpty()) {
            return deviceName;
        }
        return "RachioDevice[unknown]";
    }

    /**
     * Get display name for UI
     * ===== CRITICAL PATCH: Added explicit @NonNull annotation =====
     */
    public @org.eclipse.jdt.annotation.NonNull String getDisplayName() {
        String deviceId = id != null ? id : "";
        String deviceName = name != null ? name : "";
        if (!deviceId.isEmpty() && !deviceName.isEmpty()) {
            return deviceId + " - " + deviceName;
        } else if (!deviceName.isEmpty()) {
            return deviceName;
        } else if (!deviceId.isEmpty()) {
            return "Device " + deviceId;
        }
        return "Unknown Device";
    }

    /**
     * Get short identifier
     * ===== CRITICAL PATCH: Added explicit @NonNull annotation =====
     */
    public @org.eclipse.jdt.annotation.NonNull String getShortId() {
        String deviceId = id != null ? id : "";
        if (deviceId.length() > 8) {
            return deviceId.substring(0, 8) + "...";
        }
        return !deviceId.isEmpty() ? deviceId : "unknown";
    }

    /**
     * Get log identifier
     * ===== CRITICAL PATCH: Added explicit @NonNull annotation =====
     */
    public @org.eclipse.jdt.annotation.NonNull String getLogId() {
        String deviceId = id != null ? id : "";
        String deviceName = name != null ? name : "";

        if (!deviceId.isEmpty() && !deviceName.isEmpty()) {
            return deviceId + " (" + deviceName + ")";
        } else if (!deviceId.isEmpty()) {
            return deviceId;
        } else if (!deviceName.isEmpty()) {
            return deviceName;
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
     */
    public int getZonesCount() {
        return zones != null ? zones.size() : 0;
    }

    /**
     * Check if device has zones
     */
    public boolean hasZones() {
        return zones != null && !zones.isEmpty();
    }

    /**
     * Check if device is paused
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Check if device is enabled
     * ===== CRITICAL PATCH: Added method for new enabled field =====
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
     */
    public @Nullable String getImageUrl() {
        return imageUrl;
    }

    /**
     * Get coordinates
     * ===== CRITICAL PATCH: Added explicit @NonNull annotation =====
     */
    public @org.eclipse.jdt.annotation.NonNull String getCoordinates() {
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
     * ===== CRITICAL PATCH: Added explicit @NonNull annotation =====
     */
    public @org.eclipse.jdt.annotation.NonNull String getSerialSafe() {
        String serialNum = serialNumber;
        String serialVal = serial;
        return serialNum != null ? serialNum : (serialVal != null ? serialVal : "");
    }

    /**
     * Get model safely
     * ===== CRITICAL PATCH: Added explicit @NonNull annotation =====
     */
    public @org.eclipse.jdt.annotation.NonNull String getModelSafe() {
        String modelStr = model;
        return modelStr != null ? modelStr : "";
    }

    /**
     * Get MAC address safely
     * ===== CRITICAL PATCH: Added explicit @NonNull annotation =====
     */
    public @org.eclipse.jdt.annotation.NonNull String getMacAddressSafe() {
        String address = macAddress;
        return address != null ? address : "";
    }

    /**
     * Get schedule mode safely
     * ===== CRITICAL PATCH: Added explicit @NonNull annotation =====
     */
    public @org.eclipse.jdt.annotation.NonNull String getScheduleModeSafe() {
        String mode = scheduleModeType;
        return mode != null ? mode : "AUTO";
    }

    // ===== CRITICAL PATCH START: Added getters for Gson-deserialized fields =====
    // COMPILATION ERROR: "Null type mismatch" for fields serialized by Gson
    // EVIDENCE: Gson sets these fields AFTER constructor, bypassing @NonNullByDefault
    // SOLUTION: Add safe getter methods that handle null values with explicit @NonNull

    /**
     * Get serial number with null safety
     * ===== CRITICAL PATCH: Added explicit @NonNull annotation =====
     */
    public @org.eclipse.jdt.annotation.NonNull String getSerialNumberSafe() {
        String serialNum = serialNumber;
        return serialNum != null ? serialNum : "";
    }

    /**
     * Get MAC address with null safety
     * ===== CRITICAL PATCH: Added explicit @NonNull annotation =====
     */
    public @org.eclipse.jdt.annotation.NonNull String getMacAddressNonNull() {
        String addr = macAddress;
        return addr != null ? addr : "";
    }

    /**
     * Get model with null safety
     * ===== CRITICAL PATCH: Added explicit @NonNull annotation =====
     */
    public @org.eclipse.jdt.annotation.NonNull String getModelNonNull() {
        String modelStr = model;
        return modelStr != null ? modelStr : "";
    }

    /**
     * Get status with null safety
     * ===== CRITICAL PATCH: Added explicit @NonNull annotation =====
     */
    public @org.eclipse.jdt.annotation.NonNull String getStatusNonNull() {
        String statusStr = status;
        return statusStr != null ? statusStr : "UNKNOWN";
    }
    // ===== CRITICAL PATCH END =====
}
