package org.openhab.binding.rachio.internal.api.dto;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for Rachio zone information
 * 
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZone {
    @SerializedName("id")
    private String id = "";
    
    @SerializedName("zoneNumber")
    private int zoneNumber;
    
    @SerializedName("name")
    private String name = "";
    
    @SerializedName("enabled")
    private boolean enabled;
    
    @SerializedName("runtime")
    private int runtime;
    
    @SerializedName("maxRuntime")
    private int maxRuntime;
    
    @SerializedName("area")
    private double area;
    
    // Professional irrigation data
    @SerializedName("soil")
    @Nullable
    private CustomSoil soil;
    
    @SerializedName("crop")
    @Nullable
    private CustomCrop crop;
    
    @SerializedName("nozzle")
    @Nullable
    private CustomNozzle nozzle;
    
    @SerializedName("slope")
    @Nullable
    private CustomSlope slope;
    
    @SerializedName("shade")
    @Nullable
    private CustomShade shade;
    
    @SerializedName("rootZoneDepth")
    private double rootZoneDepth;
    
    @SerializedName("efficiency")
    private double efficiency;
    
    @SerializedName("availableWater")
    private double availableWater;
    
    @SerializedName("wateringAdjustmentRuntimes")
    @Nullable
    private List<Integer> wateringAdjustmentRuntimes;
    
    // Getters
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
    
    public int getRuntime() {
        return runtime;
    }
    
    public int getMaxRuntime() {
        return maxRuntime;
    }
    
    public double getArea() {
        return area;
    }
    
    @Nullable
    public String getSoilType() {
        return soil != null ? soil.getName() : "";
    }
    
    @Nullable
    public String getCropType() {
        return crop != null ? crop.getName() : "";
    }
    
    public double getCropCoefficient() {
        return crop != null ? crop.getCoefficient() : 0.0;
    }
    
    @Nullable
    public String getNozzleType() {
        return nozzle != null ? nozzle.getName() : "";
    }
    
    public double getNozzleRate() {
        return nozzle != null ? nozzle.getInchesPerHour() : 0.0;
    }
    
    @Nullable
    public String getSlopeType() {
        return slope != null ? slope.getName() : "";
    }
    
    @Nullable
    public String getShadeType() {
        return shade != null ? shade.getName() : "";
    }
    
    public double getRootZoneDepth() {
        return rootZoneDepth;
    }
    
    public double getEfficiency() {
        return efficiency;
    }
    
    public double getAvailableWater() {
        return availableWater;
    }
    
    @Nullable
    public List<Integer> getWateringAdjustmentRuntimes() {
        return wateringAdjustmentRuntimes;
    }
    
    // Getters for raw objects (if needed)
    @Nullable
    public CustomSoil getSoil() {
        return soil;
    }
    
    @Nullable
    public CustomCrop getCrop() {
        return crop;
    }
    
    @Nullable
    public CustomNozzle getNozzle() {
        return nozzle;
    }
    
    @Nullable
    public CustomSlope getSlope() {
        return slope;
    }
    
    @Nullable
    public CustomShade getShade() {
        return shade;
    }
    
    // Setters
    public void setId(String id) {
        this.id = id;
    }
    
    public void setZoneNumber(int zoneNumber) {
        this.zoneNumber = zoneNumber;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void setRuntime(int runtime) {
        this.runtime = runtime;
    }
    
    public void setMaxRuntime(int maxRuntime) {
        this.maxRuntime = maxRuntime;
    }
    
    public void setArea(double area) {
        this.area = area;
    }
    
    public void setSoil(@Nullable CustomSoil soil) {
        this.soil = soil;
    }
    
    public void setCrop(@Nullable CustomCrop crop) {
        this.crop = crop;
    }
    
    public void setNozzle(@Nullable CustomNozzle nozzle) {
        this.nozzle = nozzle;
    }
    
    public void setSlope(@Nullable CustomSlope slope) {
        this.slope = slope;
    }
    
    public void setShade(@Nullable CustomShade shade) {
        this.shade = shade;
    }
    
    public void setRootZoneDepth(double rootZoneDepth) {
        this.rootZoneDepth = rootZoneDepth;
    }
    
    public void setEfficiency(double efficiency) {
        this.efficiency = efficiency;
    }
    
    public void setAvailableWater(double availableWater) {
        this.availableWater = availableWater;
    }
    
    public void setWateringAdjustmentRuntimes(@Nullable List<Integer> wateringAdjustmentRuntimes) {
        this.wateringAdjustmentRuntimes = wateringAdjustmentRuntimes;
    }
    
    // Helper method to get adjustment runtimes as array
    @Nullable
    public int[] getWateringAdjustmentRuntimesArray() {
        if (wateringAdjustmentRuntimes == null || wateringAdjustmentRuntimes.isEmpty()) {
            return null;
        }
        return wateringAdjustmentRuntimes.stream().mapToInt(Integer::intValue).toArray();
    }
    
    @Override
    public String toString() {
        return "RachioZone [id=" + id + ", name=" + name + ", zoneNumber=" + zoneNumber + ", enabled=" + enabled
                + ", runtime=" + runtime + ", area=" + area + "]";
    }
}
