package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for Rachio zone
 * 
 * @author <author>
 */
@NonNullByDefault
public class RachioZone {
    public String id = "";
    public int zoneNumber;
    public String name = "";
    public boolean enabled;
    public boolean customSchedule;
    public boolean customNozzle;
    public boolean customSoil;
    public boolean customSlope;
    public boolean customCrop;
    public boolean customShade;

    // === PATCH START: Added missing fields from compilation errors ===
    @SerializedName("deviceId")
    public @Nullable String deviceId;

    @SerializedName("crop")
    public @Nullable String crop;

    @SerializedName("soil")
    public @Nullable String soil;

    @SerializedName("slope")
    public @Nullable String slope;

    @SerializedName("shade")
    public @Nullable String shade;

    @SerializedName("nozzle")
    public @Nullable String nozzle;

    @SerializedName("saturatedDepthOfWater")
    public double saturatedDepthOfWater;

    @SerializedName("runtimeNoMultiplier")
    public int runtimeNoMultiplier;

    @SerializedName("zoneArea")
    public double zoneArea;

    // === CRITICAL FIX: Change type from String to Instant ===
    // Fields for last run details
    @SerializedName("startDate")
    public @Nullable Instant startDate; // Changed from String to Instant

    @SerializedName("endDate")
    public @Nullable Instant endDate; // Changed from String to Instant

    @SerializedName("duration")
    public int duration;

    @SerializedName("totalWater")
    public double totalWater;
    // === PATCH END ===

    @SerializedName("imageUrl")
    public @Nullable String imageUrl;

    @SerializedName("lastWateredDuration")
    public int lastWateredDuration;

    @SerializedName("lastWateredDate")
    public @Nullable String lastWateredDate;

    @SerializedName("scheduleDataModified")
    public boolean scheduleDataModified;

    @SerializedName("maxRuntime")
    public int maxRuntime;

    @SerializedName("runtime")
    public int runtime;

    @SerializedName("depthOfWater")
    public double depthOfWater;

    @SerializedName("efficiency")
    public double efficiency;

    @SerializedName("fixedRuntime")
    public int fixedRuntime;

    @SerializedName("yardAreaSquareFeet")
    public double yardAreaSquareFeet;

    @SerializedName("availableWater")
    public double availableWater;

    @SerializedName("rootZoneDepth")
    public double rootZoneDepth;

    @SerializedName("managementAllowedDepletion")
    public double managementAllowedDepletion;

    @SerializedName("estimatedArea")
    public double estimatedArea;

    @SerializedName("saturatedArea")
    public double saturatedArea;

    // Runtime adjustments
    @SerializedName("wateringAdjustmentRuntimes")
    public boolean wateringAdjustmentRuntimes;

    @SerializedName("wateringAdjustmentRuntimesPercent")
    public int wateringAdjustmentRuntimesPercent;

    // Custom settings (linked by ID)
    @SerializedName("customNozzleId")
    public @Nullable String customNozzleId;

    @SerializedName("customSoilId")
    public @Nullable String customSoilId;

    @SerializedName("customSlopeId")
    public @Nullable String customSlopeId;

    @SerializedName("customCropId")
    public @Nullable String customCropId;

    @SerializedName("customShadeId")
    public @Nullable String customShadeId;

    // Zone status
    @SerializedName("lastRunStatus")
    public @Nullable ZoneRunStatus lastRunStatus;

    @SerializedName("lastRunStartDate")
    public @Nullable String lastRunStartDate;

    @SerializedName("lastRunEndDate")
    public @Nullable String lastRunEndDate;

    @SerializedName("lastRunDuration")
    public int lastRunDuration;

    /**
     * Get zone ID
     */
    public String getId() {
        return id;
    }

    /**
     * Get zone number
     */
    public int getZoneNumber() {
        return zoneNumber;
    }

    /**
     * Get zone name
     */
    public String getName() {
        return name;
    }

    /**
     * Check if zone is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Check if zone has custom schedule
     */
    public boolean hasCustomSchedule() {
        return customSchedule;
    }

    /**
     * Get image URL
     */
    public @Nullable String getImageUrl() {
        return imageUrl;
    }

    /**
     * Get last watered duration
     */
    public int getLastWateredDuration() {
        return lastWateredDuration;
    }

    /**
     * Get last watered date
     */
    public @Nullable String getLastWateredDate() {
        return lastWateredDate;
    }

    /**
     * Check if schedule data was modified
     */
    public boolean isScheduleDataModified() {
        return scheduleDataModified;
    }

    /**
     * Get maximum runtime
     */
    public int getMaxRuntime() {
        return maxRuntime;
    }

    /**
     * Get scheduled runtime
     */
    public int getRuntime() {
        return runtime;
    }

    /**
     * Get depth of water
     */
    public double getDepthOfWater() {
        return depthOfWater;
    }

    /**
     * Get efficiency
     */
    public double getEfficiency() {
        return efficiency;
    }

    /**
     * Get fixed runtime
     */
    public int getFixedRuntime() {
        return fixedRuntime;
    }

    /**
     * Get yard area in square feet
     */
    public double getYardAreaSquareFeet() {
        return yardAreaSquareFeet;
    }

    /**
     * Get available water
     */
    public double getAvailableWater() {
        return availableWater;
    }

    /**
     * Get root zone depth
     */
    public double getRootZoneDepth() {
        return rootZoneDepth;
    }

    /**
     * Get management allowed depletion
     */
    public double getManagementAllowedDepletion() {
        return managementAllowedDepletion;
    }

    /**
     * Get estimated area
     */
    public double getEstimatedArea() {
        return estimatedArea;
    }

    /**
     * Get saturated area
     */
    public double getSaturatedArea() {
        return saturatedArea;
    }

    /**
     * Check if watering adjustment runtimes is enabled
     */
    public boolean isWateringAdjustmentRuntimes() {
        return wateringAdjustmentRuntimes;
    }

    /**
     * Get watering adjustment runtimes percent
     */
    public int getWateringAdjustmentRuntimesPercent() {
        return wateringAdjustmentRuntimesPercent;
    }

    /**
     * Get custom nozzle ID
     */
    public @Nullable String getCustomNozzleId() {
        return customNozzleId;
    }

    /**
     * Get custom soil ID
     */
    public @Nullable String getCustomSoilId() {
        return customSoilId;
    }

    /**
     * Get custom slope ID
     */
    public @Nullable String getCustomSlopeId() {
        return customSlopeId;
    }

    /**
     * Get custom crop ID
     */
    public @Nullable String getCustomCropId() {
        return customCropId;
    }

    /**
     * Get custom shade ID
     */
    public @Nullable String getCustomShadeId() {
        return customShadeId;
    }

    /**
     * Get last run status
     */
    public @Nullable ZoneRunStatus getLastRunStatus() {
        return lastRunStatus;
    }

    /**
     * Get last run start date
     */
    public @Nullable String getLastRunStartDate() {
        return lastRunStartDate;
    }

    /**
     * Get last run end date
     */
    public @Nullable String getLastRunEndDate() {
        return lastRunEndDate;
    }

    /**
     * Get last run duration
     */
    public int getLastRunDuration() {
        return lastRunDuration;
    }

    /**
     * Check if zone has custom nozzle
     */
    public boolean hasCustomNozzle() {
        return customNozzle;
    }

    /**
     * Check if zone has custom soil
     */
    public boolean hasCustomSoil() {
        return customSoil;
    }

    /**
     * Check if zone has custom slope
     */
    public boolean hasCustomSlope() {
        return customSlope;
    }

    /**
     * Check if zone has custom crop
     */
    public boolean hasCustomCrop() {
        return customCrop;
    }

    /**
     * Check if zone has custom shade
     */
    public boolean hasCustomShade() {
        return customShade;
    }

    /**
     * Update runtime for this zone
     */
    public void setRuntime(int runtime) {
        this.runtime = runtime;
    }

    /**
     * Update enabled status
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Update last run status
     */
    public void setLastRunStatus(@Nullable ZoneRunStatus status) {
        this.lastRunStatus = status;
    }

    /**
     * Update last run duration
     */
    public void setLastRunDuration(int duration) {
        this.lastRunDuration = duration;
    }

    // === PATCH START: Added getters for new fields ===

    /**
     * Get device ID
     */
    public @Nullable String getDeviceId() {
        return deviceId;
    }

    /**
     * Get crop type
     */
    public @Nullable String getCrop() {
        return crop;
    }

    /**
     * Get soil type
     */
    public @Nullable String getSoil() {
        return soil;
    }

    /**
     * Get slope type
     */
    public @Nullable String getSlope() {
        return slope;
    }

    /**
     * Get shade type
     */
    public @Nullable String getShade() {
        return shade;
    }

    /**
     * Get nozzle type
     */
    public @Nullable String getNozzle() {
        return nozzle;
    }

    /**
     * Get saturated depth of water
     */
    public double getSaturatedDepthOfWater() {
        return saturatedDepthOfWater;
    }

    /**
     * Get runtime without multiplier
     */
    public int getRuntimeNoMultiplier() {
        return runtimeNoMultiplier;
    }

    /**
     * Get zone area
     */
    public double getZoneArea() {
        return zoneArea;
    }

    /**
     * Get start date (for last run) - NOW RETURNS Instant
     */
    public @Nullable Instant getStartDate() { // Return type changed to Instant
        return startDate;
    }

    /**
     * Get end date (for last run) - NOW RETURNS Instant
     */
    public @Nullable Instant getEndDate() { // Return type changed to Instant
        return endDate;
    }

    /**
     * Get duration (for last run)
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Get total water (for last run)
     */
    public double getTotalWater() {
        return totalWater;
    }
    // === PATCH END ===
}
