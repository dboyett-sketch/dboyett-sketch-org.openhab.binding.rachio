package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO representing a Rachio irrigation zone
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZone {
    
    // Zone identification
    @SerializedName("id")
    private String id = "";
    
    @SerializedName("zoneNumber")
    private int zoneNumber = 0;
    
    @SerializedName("name")
    private String name = "";
    
    @SerializedName("enabled")
    private boolean enabled = true;
    
    @SerializedName("runtime")
    private int runtime = 0; // in minutes
    
    // Run status (critical field that was missing)
    @SerializedName("zoneRunStatus")
    private ZoneRunStatus zoneRunStatus = ZoneRunStatus.NOT_RUNNING;
    
    // Soil data
    @SerializedName("customSoil")
    private CustomSoil customSoil = new CustomSoil();
    
    @SerializedName("availableWater")
    private double availableWater = 0.0; // inches per foot
    
    // Crop data
    @SerializedName("customCrop")
    private CustomCrop customCrop = new CustomCrop();
    
    @SerializedName("cropCoefficient")
    private double cropCoefficient = 0.0;
    
    // Nozzle data
    @SerializedName("customNozzle")
    private CustomNozzle customNozzle = new CustomNozzle();
    
    @SerializedName("inchesPerHour")
    private double inchesPerHour = 0.0;
    
    // Slope and shade
    @SerializedName("customSlope")
    private CustomSlope customSlope = new CustomSlope();
    
    @SerializedName("customShade")
    private CustomShade customShade = new CustomShade();
    
    // Root depth
    @SerializedName("rootDepth")
    private double rootDepth = 0.0; // inches
    
    // Efficiency
    @SerializedName("efficiency")
    private double efficiency = 0.0; // percentage
    
    // Water adjustment levels
    @SerializedName("adjustmentLevel1")
    private double adjustmentLevel1 = 0.0;
    
    @SerializedName("adjustmentLevel2")
    private double adjustmentLevel2 = 0.0;
    
    @SerializedName("adjustmentLevel3")
    private double adjustmentLevel3 = 0.0;
    
    @SerializedName("adjustmentLevel4")
    private double adjustmentLevel4 = 0.0;
    
    @SerializedName("adjustmentLevel5")
    private double adjustmentLevel5 = 0.0;
    
    // Area
    @SerializedName("area")
    private double area = 0.0; // square feet
    
    // Zone geometry and position
    @SerializedName("imageUrl")
    private String imageUrl = "";
    
    @SerializedName("sortOrder")
    private int sortOrder = 0;
    
    // Current status
    @SerializedName("lastWateredDate")
    private Instant lastWateredDate;
    
    @SerializedName("lastRunDuration")
    private int lastRunDuration = 0; // in seconds
    
    @SerializedName("lastRunStartDate")
    private Instant lastRunStartDate;
    
    @SerializedName("lastRunEndDate")
    private Instant lastRunEndDate;
    
    // Schedule information
    @SerializedName("nextStartDate")
    private Instant nextStartDate;
    
    @SerializedName("scheduleRules")
    private List<Object> scheduleRules = List.of();
    
    @SerializedName("flexScheduleRules")
    private List<Object> flexScheduleRules = List.of();
    
    // Watering history
    @SerializedName("wateringHistory")
    private List<WateringEvent> wateringHistory = List.of();
    
    // Zone properties
    @SerializedName("properties")
    private Map<String, Object> properties = Map.of();
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public int getZoneNumber() {
        return zoneNumber;
    }
    
    public void setZoneNumber(int zoneNumber) {
        this.zoneNumber = zoneNumber;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getRuntime() {
        return runtime;
    }
    
    public void setRuntime(int runtime) {
        this.runtime = runtime;
    }
    
    // CRITICAL: This was the missing field causing compilation errors
    public ZoneRunStatus getZoneRunStatus() {
        return zoneRunStatus;
    }
    
    public void setZoneRunStatus(ZoneRunStatus zoneRunStatus) {
        this.zoneRunStatus = zoneRunStatus;
    }
    
    public CustomSoil getCustomSoil() {
        return customSoil;
    }
    
    public void setCustomSoil(CustomSoil customSoil) {
        this.customSoil = customSoil;
    }
    
    public double getAvailableWater() {
        return availableWater;
    }
    
    public void setAvailableWater(double availableWater) {
        this.availableWater = availableWater;
    }
    
    public CustomCrop getCustomCrop() {
        return customCrop;
    }
    
    public void setCustomCrop(CustomCrop customCrop) {
        this.customCrop = customCrop;
    }
    
    public double getCropCoefficient() {
        return cropCoefficient;
    }
    
    public void setCropCoefficient(double cropCoefficient) {
        this.cropCoefficient = cropCoefficient;
    }
    
    public CustomNozzle getCustomNozzle() {
        return customNozzle;
    }
    
    public void setCustomNozzle(CustomNozzle customNozzle) {
        this.customNozzle = customNozzle;
    }
    
    public double getInchesPerHour() {
        return inchesPerHour;
    }
    
    public void setInchesPerHour(double inchesPerHour) {
        this.inchesPerHour = inchesPerHour;
    }
    
    public CustomSlope getCustomSlope() {
        return customSlope;
    }
    
    public void setCustomSlope(CustomSlope customSlope) {
        this.customSlope = customSlope;
    }
    
    public CustomShade getCustomShade() {
        return customShade;
    }
    
    public void setCustomShade(CustomShade customShade) {
        this.customShade = customShade;
    }
    
    public double getRootDepth() {
        return rootDepth;
    }
    
    public void setRootDepth(double rootDepth) {
        this.rootDepth = rootDepth;
    }
    
    public double getEfficiency() {
        return efficiency;
    }
    
    public void setEfficiency(double efficiency) {
        this.efficiency = efficiency;
    }
    
    public double getAdjustmentLevel1() {
        return adjustmentLevel1;
    }
    
    public void setAdjustmentLevel1(double adjustmentLevel1) {
        this.adjustmentLevel1 = adjustmentLevel1;
    }
    
    public double getAdjustmentLevel2() {
        return adjustmentLevel2;
    }
    
    public void setAdjustmentLevel2(double adjustmentLevel2) {
        this.adjustmentLevel2 = adjustmentLevel2;
    }
    
    public double getAdjustmentLevel3() {
        return adjustmentLevel3;
    }
    
    public void setAdjustmentLevel3(double adjustmentLevel3) {
        this.adjustmentLevel3 = adjustmentLevel3;
    }
    
    public double getAdjustmentLevel4() {
        return adjustmentLevel4;
    }
    
    public void setAdjustmentLevel4(double adjustmentLevel4) {
        this.adjustmentLevel4 = adjustmentLevel4;
    }
    
    public double getAdjustmentLevel5() {
        return adjustmentLevel5;
    }
    
    public void setAdjustmentLevel5(double adjustmentLevel5) {
        this.adjustmentLevel5 = adjustmentLevel5;
    }
    
    public double getArea() {
        return area;
    }
    
    public void setArea(double area) {
        this.area = area;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public int getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public @Nullable Instant getLastWateredDate() {
        return lastWateredDate;
    }
    
    public void setLastWateredDate(Instant lastWateredDate) {
        this.lastWateredDate = lastWateredDate;
    }
    
    public int getLastRunDuration() {
        return lastRunDuration;
    }
    
    public void setLastRunDuration(int lastRunDuration) {
        this.lastRunDuration = lastRunDuration;
    }
    
    public @Nullable Instant getLastRunStartDate() {
        return lastRunStartDate;
    }
    
    public void setLastRunStartDate(Instant lastRunStartDate) {
        this.lastRunStartDate = lastRunStartDate;
    }
    
    public @Nullable Instant getLastRunEndDate() {
        return lastRunEndDate;
    }
    
    public void setLastRunEndDate(Instant lastRunEndDate) {
        this.lastRunEndDate = lastRunEndDate;
    }
    
    public @Nullable Instant getNextStartDate() {
        return nextStartDate;
    }
    
    public void setNextStartDate(Instant nextStartDate) {
        this.nextStartDate = nextStartDate;
    }
    
    public List<Object> getScheduleRules() {
        return scheduleRules;
    }
    
    public void setScheduleRules(List<Object> scheduleRules) {
        this.scheduleRules = scheduleRules;
    }
    
    public List<Object> getFlexScheduleRules() {
        return flexScheduleRules;
    }
    
    public void setFlexScheduleRules(List<Object> flexScheduleRules) {
        this.flexScheduleRules = flexScheduleRules;
    }
    
    public List<WateringEvent> getWateringHistory() {
        return wateringHistory;
    }
    
    public void setWateringHistory(List<WateringEvent> wateringHistory) {
        this.wateringHistory = wateringHistory;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
    
    // Helper methods
    public boolean isRunning() {
        return zoneRunStatus.isActive();
    }
    
    public boolean hasCompleted() {
        return zoneRunStatus.isCompleted();
    }
    
    public boolean isScheduled() {
        return zoneRunStatus.isScheduled();
    }
    
    public double getWaterPerWeek() {
        // Calculate water requirement in inches per week
        return inchesPerHour * (runtime / 60.0) * 7.0;
    }
    
    public double getWaterVolumePerCycle() {
        // Calculate water volume in gallons per watering cycle
        // area (sq ft) * depth (inches) / 12 = cubic feet
        // cubic feet * 7.48 = gallons
        double depthInches = inchesPerHour * (runtime / 60.0);
        return (area * depthInches / 12.0) * 7.48;
    }
    
    public double getSoilMoistureDeficit() {
        // Calculate soil moisture deficit in inches
        return rootDepth * availableWater * (1.0 - (cropCoefficient / 100.0));
    }
    
    public double getEffectiveRuntime() {
        // Calculate effective runtime considering efficiency
        return runtime * (efficiency / 100.0);
    }
    
    public String getSoilTypeName() {
        return customSoil != null ? customSoil.getName() : "Unknown";
    }
    
    public String getCropTypeName() {
        return customCrop != null ? customCrop.getName() : "Unknown";
    }
    
    public String getNozzleTypeName() {
        return customNozzle != null ? customNozzle.getName() : "Unknown";
    }
    
    public String getSlopeTypeName() {
        return customSlope != null ? customSlope.getName() : "Unknown";
    }
    
    public String getShadeTypeName() {
        return customShade != null ? customShade.getName() : "Unknown";
    }
    
    public LocalDateTime getNextStartLocalDateTime(@Nullable ZoneId timeZone) {
        if (nextStartDate == null) {
            return null;
        }
        ZoneId tz = timeZone != null ? timeZone : ZoneId.systemDefault();
        return LocalDateTime.ofInstant(nextStartDate, tz);
    }
    
    public int getSecondsUntilNextRun(@Nullable ZoneId timeZone) {
        if (nextStartDate == null) {
            return -1;
        }
        Instant now = Instant.now();
        if (nextStartDate.isBefore(now)) {
            return 0;
        }
        return (int) (nextStartDate.getEpochSecond() - now.getEpochSecond());
    }
    
    @Override
    public String toString() {
        return String.format("RachioZone[id=%s, name=%s, zoneNumber=%d, enabled=%s, runtime=%d, status=%s]", 
            id, name, zoneNumber, enabled, runtime, zoneRunStatus);
    }
    
    /**
     * Inner class for watering event history
     */
    public static class WateringEvent {
        @SerializedName("startDate")
        private Instant startDate;
        
        @SerializedName("endDate")
        private Instant endDate;
        
        @SerializedName("duration")
        private int duration; // in seconds
        
        @SerializedName("waterVolume")
        private double waterVolume; // in gallons
        
        @SerializedName("status")
        private String status;
        
        @SerializedName("weatherAdjusted")
        private boolean weatherAdjusted;
        
        @SerializedName("forecastId")
        private String forecastId;
        
        public Instant getStartDate() { return startDate; }
        public void setStartDate(Instant startDate) { this.startDate = startDate; }
        
        public Instant getEndDate() { return endDate; }
        public void setEndDate(Instant endDate) { this.endDate = endDate; }
        
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
        
        public double getWaterVolume() { return waterVolume; }
        public void setWaterVolume(double waterVolume) { this.waterVolume = waterVolume; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public boolean isWeatherAdjusted() { return weatherAdjusted; }
        public void setWeatherAdjusted(boolean weatherAdjusted) { this.weatherAdjusted = weatherAdjusted; }
        
        public String getForecastId() { return forecastId; }
        public void setForecastId(String forecastId) { this.forecastId = forecastId; }
        
        @Override
        public String toString() {
            return String.format("WateringEvent[start=%s, duration=%ds, volume=%.2f gal]", 
                startDate, duration, waterVolume);
        }
    }
}
