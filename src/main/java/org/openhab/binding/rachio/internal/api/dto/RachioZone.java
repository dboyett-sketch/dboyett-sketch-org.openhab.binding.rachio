package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for Rachio Zone
 *
 * @author Brian G. - Initial contribution (from 2.5 binding)
 * @author Daniel B. - Major rewrite with professional data
 */
@NonNullByDefault
public class RachioZone {
    private String id = "";
    private String name = "";
    private int zoneNumber;
    private boolean enabled = true;
    private int duration = 600; // Default 10 minutes in seconds
    
    @SerializedName("customSoil")
    private @Nullable CustomSoil customSoil;
    
    @SerializedName("customCrop")
    private @Nullable CustomCrop customCrop;
    
    @SerializedName("customNozzle")
    private @Nullable CustomNozzle customNozzle;
    
    @SerializedName("customSlope")
    private @Nullable CustomSlope customSlope;
    
    @SerializedName("customShade")
    private @Nullable CustomShade customShade;
    
    @SerializedName("rootZoneDepth")
    private @Nullable Double rootZoneDepth; // inches
    
    @SerializedName("efficiency")
    private @Nullable Double efficiency; // 0.0-1.0
    
    @SerializedName("yardAreaSquareFeet")
    private @Nullable Double yardAreaSquareFeet;
    
    @SerializedName("wateringAdjustmentRuntimes")
    private @Nullable Integer[] wateringAdjustmentRuntimes; // Levels 1-5 in seconds
    
    @SerializedName("availableWater")
    private @Nullable Double availableWater; // inches
    
    @SerializedName("waterBudget")
    private @Nullable Integer waterBudget; // percentage
    
    @SerializedName("lastWateredDate")
    private @Nullable Instant lastWateredDate;
    
    @SerializedName("imageUrl")
    private @Nullable String imageUrl;
    
    @SerializedName("status")
    private @Nullable ZoneRunStatus status;
    
    @SerializedName("scheduleDataModifiedDate")
    private @Nullable Instant scheduleDataModifiedDate;
    
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
    
    public int getZoneNumber() {
        return zoneNumber;
    }
    
    public void setZoneNumber(int zoneNumber) {
        this.zoneNumber = zoneNumber;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public @Nullable CustomSoil getCustomSoil() {
        return customSoil;
    }
    
    public void setCustomSoil(@Nullable CustomSoil customSoil) {
        this.customSoil = customSoil;
    }
    
    public @Nullable CustomCrop getCustomCrop() {
        return customCrop;
    }
    
    public void setCustomCrop(@Nullable CustomCrop customCrop) {
        this.customCrop = customCrop;
    }
    
    public @Nullable CustomNozzle getCustomNozzle() {
        return customNozzle;
    }
    
    public void setCustomNozzle(@Nullable CustomNozzle customNozzle) {
        this.customNozzle = customNozzle;
    }
    
    public @Nullable CustomSlope getCustomSlope() {
        return customSlope;
    }
    
    public void setCustomSlope(@Nullable CustomSlope customSlope) {
        this.customSlope = customSlope;
    }
    
    public @Nullable CustomShade getCustomShade() {
        return customShade;
    }
    
    public void setCustomShade(@Nullable CustomShade customShade) {
        this.customShade = customShade;
    }
    
    public @Nullable Double getRootZoneDepth() {
        return rootZoneDepth;
    }
    
    public void setRootZoneDepth(@Nullable Double rootZoneDepth) {
        this.rootZoneDepth = rootZoneDepth;
    }
    
    public @Nullable Double getEfficiency() {
        return efficiency;
    }
    
    public void setEfficiency(@Nullable Double efficiency) {
        this.efficiency = efficiency;
    }
    
    public @Nullable Double getYardAreaSquareFeet() {
        return yardAreaSquareFeet;
    }
    
    public void setYardAreaSquareFeet(@Nullable Double yardAreaSquareFeet) {
        this.yardAreaSquareFeet = yardAreaSquareFeet;
    }
    
    public @Nullable Integer[] getWateringAdjustmentRuntimes() {
        return wateringAdjustmentRuntimes;
    }
    
    public void setWateringAdjustmentRuntimes(@Nullable Integer[] wateringAdjustmentRuntimes) {
        this.wateringAdjustmentRuntimes = wateringAdjustmentRuntimes;
    }
    
    public @Nullable Double getAvailableWater() {
        return availableWater;
    }
    
    public void setAvailableWater(@Nullable Double availableWater) {
        this.availableWater = availableWater;
    }
    
    public @Nullable Integer getWaterBudget() {
        return waterBudget;
    }
    
    public void setWaterBudget(@Nullable Integer waterBudget) {
        this.waterBudget = waterBudget;
    }
    
    public @Nullable Instant getLastWateredDate() {
        return lastWateredDate;
    }
    
    public void setLastWateredDate(@Nullable Instant lastWateredDate) {
        this.lastWateredDate = lastWateredDate;
    }
    
    public @Nullable String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(@Nullable String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public @Nullable ZoneRunStatus getStatus() {
        return status;
    }
    
    public void setStatus(@Nullable ZoneRunStatus status) {
        this.status = status;
    }
    
    public @Nullable Instant getScheduleDataModifiedDate() {
        return scheduleDataModifiedDate;
    }
    
    public void setScheduleDataModifiedDate(@Nullable Instant scheduleDataModifiedDate) {
        this.scheduleDataModifiedDate = scheduleDataModifiedDate;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RachioZone that = (RachioZone) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "RachioZone{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", zoneNumber=" + zoneNumber +
                ", enabled=" + enabled +
                ", duration=" + duration +
                ", soil=" + (customSoil != null ? customSoil.getName() : "null") +
                ", crop=" + (customCrop != null ? customCrop.getName() : "null") +
                ", nozzle=" + (customNozzle != null ? customNozzle.getName() : "null") +
                ", rootDepth=" + rootZoneDepth +
                ", efficiency=" + efficiency +
                ", area=" + yardAreaSquareFeet +
                '}';
    }
}
