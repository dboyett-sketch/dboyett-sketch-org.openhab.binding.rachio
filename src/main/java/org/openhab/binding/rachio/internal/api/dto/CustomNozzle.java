package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/**
 * DTO for Nozzle Type from Rachio API
 * Based on Rachio professional irrigation data
 * 
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class CustomNozzle {
    // Nozzle identification
    @SerializedName("id")
    public String id;
    
    @SerializedName("name")
    public String name;
    
    @SerializedName("custom")
    public boolean custom;
    
    // Nozzle performance
    @SerializedName("inchesPerHour")
    public double inchesPerHour; // Application rate
    
    @SerializedName("precipitationRate")
    public double precipitationRate; // Alternative name for inchesPerHour
    
    @SerializedName("distributionUniformity")
    public double distributionUniformity; // DU percentage (0-1)
    
    @SerializedName("sprayRadius")
    public double sprayRadius; // feet
    
    @SerializedName("flowRate")
    public double flowRate; // gallons per minute
    
    // Nozzle characteristics
    @SerializedName("type")
    public String type; // SPRAY, ROTOR, DRIP, BUBBLER, MICRO_SPRAY
    
    @SerializedName("pattern")
    public String pattern; // FULL, PART, STRIP, etc.
    
    @SerializedName("pressure")
    public double pressure; // PSI
    
    @SerializedName("manufacturer")
    public @Nullable String manufacturer;
    
    @SerializedName("model")
    public @Nullable String model;
    
    // Helper methods
    
    /**
     * Get application rate in different units
     */
    public double getApplicationRate(String unit) {
        switch (unit.toUpperCase()) {
            case "MM/HR":
                return inchesPerHour * 25.4; // Convert inches to mm
            case "CM/HR":
                return inchesPerHour * 2.54; // Convert inches to cm
            case "IN/HR":
            default:
                return inchesPerHour;
        }
    }
    
    /**
     * Get nozzle summary
     */
    public String getSummary() {
        return String.format("%s (%.2f in/hr, DU: %.0f%%)", 
            name, inchesPerHour, distributionUniformity * 100);
    }
    
    /**
     * Check if this is a custom nozzle type
     */
    public boolean isCustom() {
        return custom;
    }
    
    /**
     * Get effective application rate considering uniformity
     */
    public double getEffectiveApplicationRate() {
        return inchesPerHour * distributionUniformity;
    }
    
    /**
     * Calculate flow rate for specific area
     */
    public double calculateFlowForArea(double areaSqFt, double runtimeHours) {
        // inches applied = rate * hours
        double inchesApplied = inchesPerHour * runtimeHours;
        // gallons = inches * area * 0.623
        return inchesApplied * areaSqFt * 0.623;
    }
    
    /**
     * Get nozzle properties map
     */
    public java.util.Map<String, Object> getProperties() {
        java.util.Map<String, Object> props = new java.util.HashMap<>();
        props.put("id", id);
        props.put("name", name);
        props.put("custom", custom);
        props.put("inchesPerHour", inchesPerHour);
        props.put("precipitationRate", precipitationRate);
        props.put("distributionUniformity", distributionUniformity);
        props.put("sprayRadius", sprayRadius);
        props.put("flowRate", flowRate);
        props.put("type", type);
        props.put("pattern", pattern);
        props.put("pressure", pressure);
        if (manufacturer != null) props.put("manufacturer", manufacturer);
        if (model != null) props.put("model", model);
        return props;
    }
    
    // Common nozzle type constants
    public static final String TYPE_SPRAY = "SPRAY";
    public static final String TYPE_ROTOR = "ROTOR";
    public static final String TYPE_DRIP = "DRIP";
    public static final String TYPE_BUBBLER = "BUBBLER";
    public static final String TYPE_MICRO_SPRAY = "MICRO_SPRAY";
    public static final String TYPE_IMPACT = "IMPACT";
    public static final String TYPE_STREAM = "STREAM";
    
    // Pattern constants
    public static final String PATTERN_FULL = "FULL";
    public static final String PATTERN_PART = "PART";
    public static final String PATTERN_STRIP = "STRIP";
    public static final String PATTERN_QUARTER = "QUARTER";
    public static final String PATTERN_HALF = "HALF";
}
