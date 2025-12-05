package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/**
 * DTO for Rachio Zone from Rachio API
 * Based on: https://rachio.readme.io/reference/getting-started
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZone {
    // Zone identification
    public String id;
    public String name;
    public int zoneNumber;
    
    // Zone status
    public boolean enabled;
    public String imageUrl;
    
    // Zone configuration
    @SerializedName("runtime")
    public int runtime; // seconds
    
    @SerializedName("maxRuntime")
    public int maxRuntime; // seconds
    
    @SerializedName("wateringAdjustmentRuntimes")
    public int[] wateringAdjustmentRuntimes = new int[5]; // 5 levels
    
    // Professional irrigation data
    @SerializedName("customSoil")
    public @Nullable CustomSoil customSoil;
    
    @SerializedName("customCrop")
    public @Nullable CustomCrop customCrop;
    
    @SerializedName("customNozzle")
    public @Nullable CustomNozzle customNozzle;
    
    @SerializedName("customSlope")
    public @Nullable CustomSlope customSlope;
    
    @SerializedName("customShade")
    public @Nullable CustomShade customShade;
    
    // Advanced irrigation settings
    @SerializedName("efficiency")
    public double efficiency = 0.75; // 75% default
    
    @SerializedName("rootZoneDepth")
    public double rootZoneDepth = 6.0; // inches default
    
    @SerializedName("yardAreaSquareFeet")
    public double yardAreaSquareFeet = 0.0;
    
    @SerializedName("availableWater")
    public double availableWater = 0.0;
    
    @SerializedName("allowedRuntime")
    public int allowedRuntime = 0;
    
    @SerializedName("saturatedDepthOfWater")
    public double saturatedDepthOfWater = 0.0;
    
    // Rachio 3 specific features
    @SerializedName("cycleSoak")
    public boolean cycleSoak = false;
    
    @SerializedName("cycleSoakDuration")
    public int cycleSoakDuration = 0;
    
    @SerializedName("cycleSoakSoakDuration")
    public int cycleSoakSoakDuration = 0;
    
    @SerializedName("seasonalAdjustment")
    public int seasonalAdjustment = 0;
    
    // Zone statistics
    @SerializedName("lastWateredDate")
    public @Nullable String lastWateredDate;
    
    @SerializedName("totalDuration")
    public long totalDuration = 0; // lifetime seconds
    
    @SerializedName("totalWaterUsage")
    public double totalWaterUsage = 0.0; // gallons
    
    // Zone state (from events/webhooks)
    @SerializedName("state")
    public @Nullable String state; // IDLE, RUNNING, COMPLETED
    
    @SerializedName("startDate")
    public @Nullable String startDate;
    
    @SerializedName("endDate")
    public @Nullable String endDate;
    
    @SerializedName("duration")
    public int duration = 0;
    
    // Helper methods
    
    /**
     * Check if zone is currently running
     */
    public boolean isRunning() {
        return "RUNNING".equals(state);
    }
    
    /**
     * Check if zone watering is completed
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(state);
    }
    
    /**
     * Check if zone is idle
     */
    public boolean isIdle() {
        return state == null || "IDLE".equals(state);
    }
    
    /**
     * Get zone status string
     */
    public String getStatus() {
        if (isRunning()) return "RUNNING";
        if (isCompleted()) return "COMPLETED";
        return "IDLE";
    }
    
    /**
     * Get adjusted runtime based on watering adjustment level (1-5)
     */
    public int getAdjustedRuntime(int adjustmentLevel) {
        if (adjustmentLevel < 1 || adjustmentLevel > 5) {
            return runtime;
        }
        if (wateringAdjustmentRuntimes != null && wateringAdjustmentRuntimes.length >= adjustmentLevel) {
            return wateringAdjustmentRuntimes[adjustmentLevel - 1];
        }
        return runtime;
    }
    
    /**
     * Get soil type name
     */
    public String getSoilType() {
        return customSoil != null ? customSoil.name : "Unknown";
    }
    
    /**
     * Get crop type name
     */
    public String getCropType() {
        return customCrop != null ? customCrop.name : "Unknown";
    }
    
    /**
     * Get crop coefficient
     */
    public double getCropCoefficient() {
        return customCrop != null ? customCrop.coefficient : 0.8;
    }
    
    /**
     * Get nozzle type
     */
    public String getNozzleType() {
        return customNozzle != null ? customNozzle.name : "Unknown";
    }
    
    /**
     * Get nozzle application rate (inches per hour)
     */
    public double getNozzleRate() {
        return customNozzle != null ? customNozzle.inchesPerHour : 1.0;
    }
    
    /**
     * Get slope type
     */
    public String getSlopeType() {
        return customSlope != null ? customSlope.name : "Flat";
    }
    
    /**
     * Get shade type
     */
    public String getShadeType() {
        return customShade != null ? customShade.name : "Full Sun";
    }
    
    /**
     * Get efficiency percentage
     */
    public double getEfficiency() {
        return efficiency * 100; // Convert to percentage
    }
    
    /**
     * Get root depth in inches
     */
    public double getRootDepth() {
        return rootZoneDepth;
    }
    
    /**
     * Get zone area in square feet
     */
    public double getArea() {
        return yardAreaSquareFeet;
    }
    
    /**
     * Get available water (inches)
     */
    public double getAvailableWater() {
        return availableWater;
    }
    
    /**
     * Get watering time for specific depth
     */
    public double getWateringTimeForDepth(double depthInches) {
        double rate = getNozzleRate();
        if (rate > 0) {
            return (depthInches / rate) * 3600; // Convert to seconds
        }
        return runtime;
    }
    
    /**
     * Check if zone has professional data configured
     */
    public boolean hasProfessionalData() {
        return customSoil != null || customCrop != null || customNozzle != null || 
               customSlope != null || customShade != null;
    }
    
    /**
     * Get professional data summary
     */
    public String getProfessionalSummary() {
        StringBuilder sb = new StringBuilder();
        if (customSoil != null) sb.append("Soil: ").append(customSoil.name).append(", ");
        if (customCrop != null) sb.append("Crop: ").append(customCrop.name).append(", ");
        if (customNozzle != null) sb.append("Nozzle: ").append(customNozzle.name).append(", ");
        if (customSlope != null) sb.append("Slope: ").append(customSlope.name).append(", ");
        if (customShade != null) sb.append("Shade: ").append(customShade.name);
        
        String result = sb.toString();
        if (result.endsWith(", ")) {
            result = result.substring(0, result.length() - 2);
        }
        return result;
    }
    
    /**
     * Get zone information map
     */
    public java.util.Map<String, Object> getInfo() {
        java.util.Map<String, Object> info = new java.util.HashMap<>();
        info.put("id", id);
        info.put("name", name);
        info.put("zoneNumber", zoneNumber);
        info.put("enabled", enabled);
        info.put("runtime", runtime);
        info.put("maxRuntime", maxRuntime);
        info.put("status", getStatus());
        info.put("efficiency", getEfficiency());
        info.put("rootDepth", getRootDepth());
        info.put("area", getArea());
        
        if (customSoil != null) info.put("soilType", customSoil.name);
        if (customCrop != null) {
            info.put("cropType", customCrop.name);
            info.put("cropCoefficient", customCrop.coefficient);
        }
        if (customNozzle != null) {
            info.put("nozzleType", customNozzle.name);
            info.put("nozzleRate", customNozzle.inchesPerHour);
        }
        if (customSlope != null) info.put("slopeType", customSlope.name);
        if (customShade != null) info.put("shadeType", customShade.name);
        
        return info;
    }
    
    /**
     * Get zone summary for logging
     */
    public String getSummary() {
        return String.format("Zone %d: %s (%s)", 
            zoneNumber, name, enabled ? "Enabled" : "Disabled");
    }
    
    /**
     * Get detailed zone description
     */
    public String getDescription() {
        return String.format("Zone %d: %s - Runtime: %ds, Soil: %s, Crop: %s, Nozzle: %s",
            zoneNumber, name, runtime,
            getSoilType(), getCropType(), getNozzleType());
    }
    
    /**
     * Check if cycle and soak is enabled for this zone
     */
    public boolean isCycleSoakEnabled() {
        return cycleSoak;
    }
    
    /**
     * Get cycle soak settings
     */
    public String getCycleSoakSettings() {
        if (cycleSoak) {
            return String.format("Cycle: %ds, Soak: %ds", 
                cycleSoakDuration, cycleSoakSoakDuration);
        }
        return "Disabled";
    }
    
    /**
     * Calculate water usage for a watering event
     */
    public double calculateWaterUsage(int durationSeconds) {
        // Convert runtime to hours
        double hours = durationSeconds / 3600.0;
        // Get application rate (in/hr)
        double rate = getNozzleRate();
        // Get area (sq ft)
        double area = getArea();
        // Calculate gallons (1 inch of water over 1 sq ft = 0.623 gallons)
        double inchesApplied = rate * hours;
        return inchesApplied * area * 0.623;
    }
}
