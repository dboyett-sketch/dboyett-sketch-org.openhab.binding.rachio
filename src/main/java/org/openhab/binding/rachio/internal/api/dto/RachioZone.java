package org.openhab.binding.rachio.internal.api.dto;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

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
    
    @SerializedName("rootZoneDepth")
    public double rootZoneDepth;
    
    @SerializedName("availableWater")
    public double availableWater;
    
    @SerializedName("efficiency")
    public double efficiency;
    
    @SerializedName("yardAreaSquareFeet")
    public double yardAreaSquareFeet;
    
    @SerializedName("maxRuntime")
    public int maxRuntime;
    
    @SerializedName("runtime")
    public int runtime;
    
    @SerializedName("imageUrl")
    public String imageUrl = "";
    
    @SerializedName("lastWateredDate")
    public long lastWateredDate;
    
    // Default constructor for Gson
    public RachioZone() {
    }
    
    // Helper method to get properties for discovery
    public void populateProperties(Map<String, String> properties) {
        properties.put("zoneNumber", String.valueOf(zoneNumber));
        properties.put("enabled", String.valueOf(enabled));
        
        // Fix: Use .name field instead of .getName() method
        if (customSoil != null && customSoil.name != null) {
            properties.put("soilType", customSoil.name);
        }
        
        if (customCrop != null && customCrop.name != null) {
            properties.put("cropType", customCrop.name);
        }
        
        if (customNozzle != null && customNozzle.name != null) {
            properties.put("nozzleType", customNozzle.name);
        }
        
        if (customSlope != null && customSlope.name != null) {
            properties.put("slopeType", customSlope.name);
        }
        
        if (customShade != null && customShade.name != null) {
            properties.put("shadeType", customShade.name);
        }
    }
    
    // Helper methods
    public boolean isRunning() {
        return runtime > 0;
    }
    
    public double getAreaSquareMeters() {
        return yardAreaSquareFeet * 0.092903;
    }
}
