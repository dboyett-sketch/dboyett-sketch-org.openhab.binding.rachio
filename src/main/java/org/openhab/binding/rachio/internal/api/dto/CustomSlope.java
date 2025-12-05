package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import com.google.gson.annotations.SerializedName;

/**
 * DTO for Slope Type from Rachio API
 * Based on Rachio professional irrigation data
 * 
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class CustomSlope {
    // Slope identification
    @SerializedName("id")
    public String id;
    
    @SerializedName("name")
    public String name;
    
    @SerializedName("custom")
    public boolean custom;
    
    // Slope characteristics
    @SerializedName("grade")
    public double grade; // percentage slope (0-100)
    
    @SerializedName("angle")
    public double angle; // degrees
    
    @SerializedName("runoffFactor")
    public double runoffFactor; // 0-1 (1 = high runoff)
    
    @SerializedName("infiltrationReduction")
    public double infiltrationReduction; // percentage reduction
    
    // Helper methods
    
    /**
     * Get slope summary
     */
    public String getSummary() {
        return String.format("%s (%.1f%% grade)", name, grade);
    }
    
    /**
     * Check if this is a custom slope type
     */
    public boolean isCustom() {
        return custom;
    }
    
    /**
     * Check if slope requires cycle and soak
     */
    public boolean requiresCycleSoak() {
        return grade > 10.0; // More than 10% slope
    }
    
    /**
     * Get adjusted infiltration rate
     */
    public double getAdjustedInfiltrationRate(double baseRate) {
        return baseRate * (1.0 - (infiltrationReduction / 100.0));
    }
    
    /**
     * Get slope properties map
     */
    public java.util.Map<String, Object> getProperties() {
        java.util.Map<String, Object> props = new java.util.HashMap<>();
        props.put("id", id);
        props.put("name", name);
        props.put("custom", custom);
        props.put("grade", grade);
        props.put("angle", angle);
        props.put("runoffFactor", runoffFactor);
        props.put("infiltrationReduction", infiltrationReduction);
        return props;
    }
    
    // Common slope type constants
    public static final String NAME_FLAT = "Flat";
    public static final String NAME_GENTLE = "Gentle";
    public static final String NAME_MODERATE = "Moderate";
    public static final String NAME_STEEP = "Steep";
    public static final String NAME_VERY_STEEP = "Very Steep";
}
