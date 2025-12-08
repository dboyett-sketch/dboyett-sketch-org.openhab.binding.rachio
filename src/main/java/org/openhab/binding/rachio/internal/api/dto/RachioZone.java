package org.openhab.binding.rachio.internal.api.dto;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.SerializedName;

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
    
    @SerializedName("soilType")
    private String soilType = "";
    
    @SerializedName("cropType")
    private String cropType = "";
    
    @SerializedName("nozzleType")
    private String nozzleType = "";
    
    @SerializedName("slopeType")
    private String slopeType = "";
    
    @SerializedName("shadeType")
    private String shadeType = "";
    
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
    public String getId() { return id; }
    public int getZoneNumber() { return zoneNumber; }
    public String getName() { return name; }
    public boolean isEnabled() { return enabled; }
    public int getRuntime() { return runtime; }
    public int getMaxRuntime() { return maxRuntime; }
    public double getArea() { return area; }
    public String getSoilType() { return soilType; }
    public String getCropType() { return cropType; }
    public String getNozzleType() { return nozzleType; }
    public String getSlopeType() { return slopeType; }
    public String getShadeType() { return shadeType; }
    public double getRootZoneDepth() { return rootZoneDepth; }
    public double getEfficiency() { return efficiency; }
    public double getAvailableWater() { return availableWater; }
    @Nullable
    public List<Integer> getWateringAdjustmentRuntimes() { return wateringAdjustmentRuntimes; }
    
    // Setters
    public void setId(String id) { this.id = id; }
    public void setZoneNumber(int zoneNumber) { this.zoneNumber = zoneNumber; }
    public void setName(String name) { this.name = name; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setRuntime(int runtime) { this.runtime = runtime; }
    public void setMaxRuntime(int maxRuntime) { this.maxRuntime = maxRuntime; }
    public void setArea(double area) { this.area = area; }
    public void setSoilType(String soilType) { this.soilType = soilType; }
    public void setCropType(String cropType) { this.cropType = cropType; }
    public void setNozzleType(String nozzleType) { this.nozzleType = nozzleType; }
    public void setSlopeType(String slopeType) { this.slopeType = slopeType; }
    public void setShadeType(String shadeType) { this.shadeType = shadeType; }
    public void setRootZoneDepth(double rootZoneDepth) { this.rootZoneDepth = rootZoneDepth; }
    public void setEfficiency(double efficiency) { this.efficiency = efficiency; }
    public void setAvailableWater(double availableWater) { this.availableWater = availableWater; }
    public void setWateringAdjustmentRuntimes(@Nullable List<Integer> wateringAdjustmentRuntimes) { 
        this.wateringAdjustmentRuntimes = wateringAdjustmentRuntimes; 
    }
}
