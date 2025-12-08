package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for Rachio Zone with professional irrigation data
 *
 * @author Damion Boyett - Enhanced with professional features
 */
@NonNullByDefault
public class RachioZone {
    @SerializedName("id")
    public String id = "";
    
    @SerializedName("zoneNumber")
    public int zoneNumber;
    
    @SerializedName("name")
    public String name = "";
    
    @SerializedName("enabled")
    public boolean enabled;
    
    @SerializedName("customNozzle")
    public @Nullable CustomNozzle customNozzle;
    
    @SerializedName("customSoil")
    public @Nullable CustomSoil customSoil;
    
    @SerializedName("customCrop")
    public @Nullable CustomCrop customCrop;
    
    @SerializedName("customSlope")
    public @Nullable CustomSlope customSlope;
    
    @SerializedName("customShade")
    public @Nullable CustomShade customShade;
    
    @SerializedName("availableWater")
    public double availableWater;
    
    @SerializedName("rootZoneDepth")
    public double rootZoneDepth;
    
    @SerializedName("managementAllowedDepletion")
    public double managementAllowedDepletion;
    
    @SerializedName("efficiency")
    public double efficiency;
    
    @SerializedName("yardAreaSquareFeet")
    public double yardAreaSquareFeet;
    
    @SerializedName("irrigationAmount")
    public double irrigationAmount;
    
    @SerializedName("maxRuntime")
    public int maxRuntime;
    
    @SerializedName("runtime")
    public int runtime;
    
    @SerializedName("zoneRunStatus")
    public @Nullable ZoneRunStatus zoneRunStatus;
    
    // Professional irrigation data
    @SerializedName("area")
    public double area;
    
    @SerializedName("lastWateredDate")
    public @Nullable String lastWateredDate;
    
    @SerializedName("lastRunDuration")
    public @Nullable Integer lastRunDuration;
    
    @SerializedName("totalWaterUsed")
    public @Nullable Double totalWaterUsed;
    
    @SerializedName("wateringAdjustmentRuntimes")
    public int[] wateringAdjustmentRuntimes = new int[5];
    
    @SerializedName("imageUrl")
    public @Nullable String imageUrl;
    
    @SerializedName("etCoefficient")
    public double etCoefficient;
    
    @SerializedName("saturationThreshold")
    public double saturationThreshold;
    
    @SerializedName("depthOfWater")
    public double depthOfWater;
    
    @SerializedName("fixedRuntime")
    public @Nullable Integer fixedRuntime;
    
    @SerializedName("forecastData")
    public @Nullable ZoneForecastData forecastData;
    
    // Add getters for all fields
    public String getId() {
        return id;
    }
    
    public int getZoneNumber() {
        return zoneNumber;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    @Nullable
    public CustomNozzle getCustomNozzle() {
        return customNozzle;
    }
    
    @Nullable
    public CustomSoil getCustomSoil() {
        return customSoil;
    }
    
    @Nullable
    public CustomCrop getCustomCrop() {
        return customCrop;
    }
    
    @Nullable
    public CustomSlope getCustomSlope() {
        return customSlope;
    }
    
    @Nullable
    public CustomShade getCustomShade() {
        return customShade;
    }
    
    public double getAvailableWater() {
        return availableWater;
    }
    
    public double getRootZoneDepth() {
        return rootZoneDepth;
    }
    
    public double getManagementAllowedDepletion() {
        return managementAllowedDepletion;
    }
    
    public double getEfficiency() {
        return efficiency;
    }
    
    public double getYardAreaSquareFeet() {
        return yardAreaSquareFeet;
    }
    
    public double getIrrigationAmount() {
        return irrigationAmount;
    }
    
    public int getMaxRuntime() {
        return maxRuntime;
    }
    
    public int getRuntime() {
        return runtime;
    }
    
    @Nullable
    public ZoneRunStatus getZoneRunStatus() {
        return zoneRunStatus;
    }
    
    public double getArea() {
        return area;
    }
    
    @Nullable
    public String getLastWateredDate() {
        return lastWateredDate;
    }
    
    @Nullable
    public Integer getLastRunDuration() {
        return lastRunDuration;
    }
    
    @Nullable
    public Double getTotalWaterUsed() {
        return totalWaterUsed;
    }
    
    public int[] getWateringAdjustmentRuntimes() {
        return wateringAdjustmentRuntimes;
    }
    
    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }
    
    public double getEtCoefficient() {
        return etCoefficient;
    }
    
    public double getSaturationThreshold() {
        return saturationThreshold;
    }
    
    public double getDepthOfWater() {
        return depthOfWater;
    }
    
    @Nullable
    public Integer getFixedRuntime() {
        return fixedRuntime;
    }
    
    @Nullable
    public ZoneForecastData getForecastData() {
        return forecastData;
    }
    
    // Setters for mutable fields
    public void setName(String name) {
        this.name = name;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void setRuntime(int runtime) {
        this.runtime = runtime;
    }
    
    public void setZoneRunStatus(@Nullable ZoneRunStatus zoneRunStatus) {
        this.zoneRunStatus = zoneRunStatus;
    }
    
    // Professional utility methods
    public boolean isWatering() {
        return zoneRunStatus != null && "STARTED".equals(zoneRunStatus.toString());
    }
    
    public boolean isStopped() {
        return zoneRunStatus == null || "STOPPED".equals(zoneRunStatus.toString());
    }
    
    public boolean isCompleted() {
        return zoneRunStatus != null && "COMPLETED".equals(zoneRunStatus.toString());
    }
    
    public String getSoilType() {
        if (customSoil != null) {
            String name = customSoil.getName(); // Fixed: Use getter method
            return name != null ? name : "CUSTOM";
        }
        return "UNKNOWN";
    }
    
    public String getCropType() {
        if (customCrop != null) {
            String name = customCrop.getName(); // Fixed: Use getter method
            return name != null ? name : "CUSTOM";
        }
        return "UNKNOWN";
    }
    
    public String getNozzleType() {
        if (customNozzle != null) {
            String name = customNozzle.getName(); // Fixed: Use getter method
            return name != null ? name : "CUSTOM";
        }
        return "UNKNOWN";
    }
    
    public String getSlopeType() {
        if (customSlope != null) {
            String name = customSlope.getName(); // Fixed: Use getter method
            return name != null ? name : "CUSTOM";
        }
        return "UNKNOWN";
    }
    
    public String getShadeType() {
        if (customShade != null) {
            String name = customShade.getName(); // Fixed: Use getter method
            return name != null ? name : "CUSTOM";
        }
        return "UNKNOWN";
    }
    
    public double getNozzleInchesPerHour() {
        if (customNozzle != null && customNozzle.inchesPerHour > 0) {
            return customNozzle.inchesPerHour;
        }
        return 0.0;
    }
    
    public double getCropCoefficient() {
        if (customCrop != null && customCrop.coefficient > 0) {
            return customCrop.coefficient;
        }
        return 1.0;
    }
    
    public double getSoilAvailableWater() {
        if (customSoil != null && customSoil.availableWater > 0) {
            return customSoil.availableWater;
        }
        return 0.0;
    }
    
    public int getAdjustmentRuntime(int level) {
        if (level >= 1 && level <= 5 && wateringAdjustmentRuntimes != null && 
            wateringAdjustmentRuntimes.length >= level) {
            return wateringAdjustmentRuntimes[level - 1];
        }
        return runtime;
    }
    
    public double calculateWaterVolume(double runtimeMinutes) {
        // Convert runtime minutes to hours
        double runtimeHours = runtimeMinutes / 60.0;
        // Calculate water volume in gallons: area (sq ft) * nozzle rate (in/hr) * runtime (hr) / 12
        double nozzleRate = getNozzleInchesPerHour();
        return (yardAreaSquareFeet * nozzleRate * runtimeHours) / 12.0;
    }
    
    public double getCurrentWaterVolume() {
        return calculateWaterVolume(runtime);
    }
    
    public boolean hasProfessionalData() {
        return customSoil != null || customCrop != null || customNozzle != null || 
               customSlope != null || customShade != null;
    }
    
    public String getZoneSummary() {
        return String.format("Zone %d: %s (%s) - %s soil, %s crop, %s nozzle", 
                zoneNumber, name, enabled ? "Enabled" : "Disabled",
                getSoilType(), getCropType(), getNozzleType());
    }
    
    public String getProfessionalDataSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Professional Data: ");
        if (customSoil != null) sb.append("Soil=").append(getSoilType()).append(" ");
        if (customCrop != null) sb.append("Crop=").append(getCropType()).append(" ");
        if (customNozzle != null) sb.append("Nozzle=").append(getNozzleType()).append("(")
          .append(getNozzleInchesPerHour()).append(" in/hr) ");
        if (customSlope != null) sb.append("Slope=").append(getSlopeType()).append(" ");
        if (customShade != null) sb.append("Shade=").append(getShadeType()).append(" ");
        sb.append("Area=").append(area).append(" sq ft");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "RachioZone{id='" + id + "', zoneNumber=" + zoneNumber + ", name='" + name + 
               "', enabled=" + enabled + ", runtime=" + runtime + 
               ", status=" + (zoneRunStatus != null ? zoneRunStatus.toString() : "null") + 
               ", area=" + area + ", soil='" + getSoilType() + 
               "', crop='" + getCropType() + "', nozzle='" + getNozzleType() + "'}";
    }
    
    /**
     * Inner class for zone forecast data
     */
    @NonNullByDefault
    public static class ZoneForecastData {
        @SerializedName("et")
        public double et;
        
        @SerializedName("precip")
        public double precip;
        
        @SerializedName("soilMoisture")
        public double soilMoisture;
        
        @SerializedName("nextWateringDate")
        public @Nullable String nextWateringDate;
        
        public double getEt() {
            return et;
        }
        
        public double getPrecip() {
            return precip;
        }
        
        public double getSoilMoisture() {
            return soilMoisture;
        }
        
        @Nullable
        public String getNextWateringDate() {
            return nextWateringDate;
        }
    }
}
