package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/**
 * DTO for Crop Type from Rachio API
 * Based on Rachio professional irrigation data
 * 
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class CustomCrop {
    // Crop identification
    @SerializedName("id")
    public String id;
    
    @SerializedName("name")
    public String name;
    
    @SerializedName("custom")
    public boolean custom;
    
    // Crop coefficient (Kc) values
    @SerializedName("coefficient")
    public double coefficient; // Crop coefficient (Kc)
    
    @SerializedName("initialKc")
    public double initialKc;
    
    @SerializedName("midSeasonKc")
    public double midSeasonKc;
    
    @SerializedName("endSeasonKc")
    public double endSeasonKc;
    
    @SerializedName("maxKc")
    public double maxKc;
    
    // Crop characteristics
    @SerializedName("type")
    public String type; // TURFGRASS, TREES, SHRUBS, VEGETABLES, FLOWERS
    
    @SerializedName("rootDepth")
    public double rootDepth; // inches
    
    @SerializedName("maturityDays")
    public int maturityDays;
    
    @SerializedName("waterUseCategory")
    public String waterUseCategory; // LOW, MEDIUM, HIGH
    
    @SerializedName("droughtTolerant")
    public boolean droughtTolerant;
    
    // Helper methods
    
    /**
     * Get crop coefficient for specific growth stage
     */
    public double getCoefficientForStage(String stage) {
        switch (stage.toUpperCase()) {
            case "INITIAL": return initialKc;
            case "MID": return midSeasonKc;
            case "END": return endSeasonKc;
            case "MAX": return maxKc;
            default: return coefficient;
        }
    }
    
    /**
     * Get crop summary
     */
    public String getSummary() {
        return String.format("%s (Kc: %.2f, Root: %.1f in)", 
            name, coefficient, rootDepth);
    }
    
    /**
     * Check if this is a custom crop type
     */
    public boolean isCustom() {
        return custom;
    }
    
    /**
     * Get water use level
     */
    public String getWaterUseLevel() {
        return waterUseCategory;
    }
    
    /**
     * Check if crop is drought tolerant
     */
    public boolean isDroughtTolerant() {
        return droughtTolerant;
    }
    
    /**
     * Get crop properties map
     */
    public java.util.Map<String, Object> getProperties() {
        java.util.Map<String, Object> props = new java.util.HashMap<>();
        props.put("id", id);
        props.put("name", name);
        props.put("custom", custom);
        props.put("coefficient", coefficient);
        props.put("initialKc", initialKc);
        props.put("midSeasonKc", midSeasonKc);
        props.put("endSeasonKc", endSeasonKc);
        props.put("maxKc", maxKc);
        props.put("type", type);
        props.put("rootDepth", rootDepth);
        props.put("maturityDays", maturityDays);
        props.put("waterUseCategory", waterUseCategory);
        props.put("droughtTolerant", droughtTolerant);
        return props;
    }
    
    // Common crop type constants
    public static final String TYPE_TURFGRASS = "TURFGRASS";
    public static final String TYPE_TREES = "TREES";
    public static final String TYPE_SHRUBS = "SHRUBS";
    public static final String TYPE_VEGETABLES = "VEGETABLES";
    public static final String TYPE_FLOWERS = "FLOWERS";
    public static final String TYPE_GROUNDCOVER = "GROUNDCOVER";
    public static final String TYPE_VINES = "VINES";
    
    // Water use categories
    public static final String WATER_USE_LOW = "LOW";
    public static final String WATER_USE_MEDIUM = "MEDIUM";
    public static final String WATER_USE_HIGH = "HIGH";
}
