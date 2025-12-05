package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import com.google.gson.annotations.SerializedName;

/**
 * DTO for Shade Type from Rachio API
 * Based on Rachio professional irrigation data
 * 
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class CustomShade {
    // Shade identification
    @SerializedName("id")
    public String id;
    
    @SerializedName("name")
    public String name;
    
    @SerializedName("custom")
    public boolean custom;
    
    // Shade characteristics
    @SerializedName("sunExposure")
    public double sunExposure; // percentage (0-100)
    
    @SerializedName("evapotranspirationReduction")
    public double evapotranspirationReduction; // percentage
    
    @SerializedName("waterRequirementFactor")
    public double waterRequirementFactor; // multiplier (0-1)
    
    // Helper methods
    
    /**
     * Get shade summary
     */
    public String getSummary() {
        return String.format("%s (%.0f%% sun)", name, sunExposure);
    }
    
    /**
     * Check if this is a custom shade type
     */
    public boolean isCustom() {
        return custom;
    }
    
    /**
     * Get adjusted water requirement
     */
    public double getAdjustedWaterRequirement(double baseRequirement) {
        return baseRequirement * waterRequirementFactor;
    }
    
    /**
     * Get adjusted ET value
     */
    public double getAdjustedET(double baseET) {
        return baseET * (1.0 - (evapotranspirationReduction / 100.0));
    }
    
    /**
     * Get shade properties map
     */
    public java.util.Map<String, Object> getProperties() {
        java.util.Map<String, Object> props = new java.util.HashMap<>();
        props.put("id", id);
        props.put("name", name);
        props.put("custom", custom);
        props.put("sunExposure", sunExposure);
        props.put("evapotranspirationReduction", evapotranspirationReduction);
        props.put("waterRequirementFactor", waterRequirementFactor);
        return props;
    }
    
    // Common shade type constants
    public static final String NAME_FULL_SUN = "Full Sun";
    public static final String NAME_PART_SUN = "Part Sun";
    public static final String NAME_PART_SHADE = "Part Shade";
    public static final String NAME_FULL_SHADE = "Full Shade";
    public static final String NAME_DENSE_SHADE = "Dense Shade";
}
