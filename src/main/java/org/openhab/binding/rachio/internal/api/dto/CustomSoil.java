package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/**
 * DTO for Soil Type from Rachio API
 * Based on Rachio professional irrigation data
 * 
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class CustomSoil {
    // Soil identification
    @SerializedName("id")
    public String id;
    
    @SerializedName("name")
    public String name;
    
    @SerializedName("custom")
    public boolean custom;
    
    // Soil properties
    @SerializedName("availableWater")
    public double availableWater; // inches per foot
    
    @SerializedName("infiltrationRate")
    public double infiltrationRate; // inches per hour
    
    @SerializedName("wiltingPoint")
    public double wiltingPoint; // inches per foot
    
    @SerializedName("fieldCapacity")
    public double fieldCapacity; // inches per foot
    
    // Soil characteristics
    @SerializedName("texture")
    public String texture; // SAND, LOAM, CLAY, etc.
    
    @SerializedName("density")
    public double density; // g/cm³
    
    @SerializedName("porosity")
    public double porosity; // percentage
    
    // Helper methods
    
    /**
     * Get available water in inches for specific root depth
     */
    public double getAvailableWaterForDepth(double rootDepthInches) {
        return (availableWater / 12.0) * rootDepthInches;
    }
    
    /**
     * Get soil summary
     */
    public String getSummary() {
        return String.format("%s (AW: %.2f in/ft, Infil: %.2f in/hr)", 
            name, availableWater, infiltrationRate);
    }
    
    /**
     * Check if this is a custom soil type
     */
    public boolean isCustom() {
        return custom;
    }
    
    /**
     * Get soil properties map
     */
    public java.util.Map<String, Object> getProperties() {
        java.util.Map<String, Object> props = new java.util.HashMap<>();
        props.put("id", id);
        props.put("name", name);
        props.put("custom", custom);
        props.put("availableWater", availableWater);
        props.put("infiltrationRate", infiltrationRate);
        props.put("wiltingPoint", wiltingPoint);
        props.put("fieldCapacity", fieldCapacity);
        props.put("texture", texture);
        props.put("density", density);
        props.put("porosity", porosity);
        return props;
    }
    
    // Common soil type constants
    public static final String TEXTURE_SAND = "SAND";
    public static final String TEXTURE_LOAM = "LOAM";
    public static final String TEXTURE_CLAY = "CLAY";
    public static final String TEXTURE_SILT = "SILT";
    public static final String TEXTURE_SANDY_LOAM = "SANDY_LOAM";
    public static final String TEXTURE_CLAY_LOAM = "CLAY_LOAM";
    public static final String TEXTURE_SILT_LOAM = "SILT_LOAM";
}
