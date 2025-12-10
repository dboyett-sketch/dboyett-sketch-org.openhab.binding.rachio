package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link CustomNozzle} class defines nozzle data for a zone
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class CustomNozzle {

    @SerializedName("type")
    private @Nullable String type;

    @SerializedName("rate")
    private @Nullable Double rate;

    @SerializedName("name")
    private @Nullable String name;

    @SerializedName("description")
    private @Nullable String description;

    @SerializedName("efficiency")
    private @Nullable Double efficiency;

    @SerializedName("radius")
    private @Nullable Double radius;

    @SerializedName("pattern")
    private @Nullable String pattern;

    /**
     * Get the nozzle type
     *
     * @return nozzle type
     */
    public @Nullable String getType() {
        return type;
    }

    /**
     * Set the nozzle type
     *
     * @param type nozzle type
     */
    public void setType(@Nullable String type) {
        this.type = type;
    }

    /**
     * Get the application rate (inches per hour)
     *
     * @return application rate
     */
    public @Nullable Double getRate() {
        return rate;
    }

    /**
     * Set the application rate
     *
     * @param rate application rate
     */
    public void setRate(@Nullable Double rate) {
        this.rate = rate;
    }

    /**
     * Get the nozzle name
     *
     * @return nozzle name
     */
    public @Nullable String getName() {
        return name;
    }

    /**
     * Set the nozzle name
     *
     * @param name nozzle name
     */
    public void setName(@Nullable String name) {
        this.name = name;
    }

    /**
     * Get the nozzle description
     *
     * @return nozzle description
     */
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * Set the nozzle description
     *
     * @param description nozzle description
     */
    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    /**
     * Get the nozzle efficiency (percentage)
     *
     * @return nozzle efficiency
     */
    public @Nullable Double getEfficiency() {
        return efficiency;
    }

    /**
     * Set the nozzle efficiency
     *
     * @param efficiency nozzle efficiency
     */
    public void setEfficiency(@Nullable Double efficiency) {
        this.efficiency = efficiency;
    }

    /**
     * Get the nozzle radius (feet)
     *
     * @return nozzle radius
     */
    public @Nullable Double getRadius() {
        return radius;
    }

    /**
     * Set the nozzle radius
     *
     * @param radius nozzle radius
     */
    public void setRadius(@Nullable Double radius) {
        this.radius = radius;
    }

    /**
     * Get the spray pattern
     *
     * @return spray pattern
     */
    public @Nullable String getPattern() {
        return pattern;
    }

    /**
     * Set the spray pattern
     *
     * @param pattern spray pattern
     */
    public void setPattern(@Nullable String pattern) {
        this.pattern = pattern;
    }

    /**
     * Check if nozzle data is valid (has type and rate)
     *
     * @return true if valid
     */
    public boolean isValid() {
        return type != null && !type.isEmpty() && rate != null;
    }

    /**
     * Get string representation
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "CustomNozzle{" +
                "type='" + type + '\'' +
                ", rate=" + rate +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", efficiency=" + efficiency +
                ", radius=" + radius +
                ", pattern='" + pattern + '\'' +
                '}';
    }

    /**
     * Get nozzle type as human readable string
     *
     * @return human readable nozzle type
     */
    public String getTypeDescription() {
        if (type == null) {
            return "Unknown";
        }
        switch (type.toLowerCase()) {
            case "rotor":
                return "Rotor - Rotary sprinkler heads";
            case "spray":
                return "Spray - Fixed spray heads";
            case "rotary_nozzle":
                return "Rotary Nozzle - Multi-stream rotary";
            case "bubbler":
                return "Bubbler - Low flow, surface watering";
            case "drip":
                return "Drip - Low volume drip irrigation";
            case "micro_spray":
                return "Micro Spray - Small area spray";
            case "impact":
                return "Impact - Large area rotary";
            case "gear_drive":
                return "Gear Drive - Gear-driven rotary";
            case "shrub_adapter":
                return "Shrub Adapter - For shrub beds";
            case "custom":
                return "Custom - User-defined nozzle type";
            default:
                return type;
        }
    }

    /**
     * Get application rate as formatted string
     *
     * @return formatted application rate
     */
    public String getRateFormatted() {
        if (rate == null) {
            return "N/A";
        }
        return String.format("%.2f in/hr", rate);
    }

    /**
     * Get efficiency as formatted string
     *
     * @return formatted efficiency
     */
    public String getEfficiencyFormatted() {
        if (efficiency == null) {
            return "N/A";
        }
        return String.format("%.0f%%", efficiency * 100);
    }

    /**
     * Get radius as formatted string
     *
     * @return formatted radius
     */
    public String getRadiusFormatted() {
        if (radius == null) {
            return "N/A";
        }
        return String.format("%.1f ft", radius);
    }

    /**
     * Calculate runtime for a given water depth
     *
     * @param waterDepth depth of water needed (inches)
     * @return runtime in minutes
     */
    public @Nullable Double calculateRuntime(@Nullable Double waterDepth) {
        if (rate == null || waterDepth == null || rate == 0) {
            return null;
        }
        return (waterDepth / rate) * 60; // Convert hours to minutes
    }

    /**
     * Calculate water applied for a given runtime
     *
     * @param runtime runtime in minutes
     * @return water depth in inches
     */
    public @Nullable Double calculateWaterApplied(@Nullable Double runtime) {
        if (rate == null || runtime == null) {
            return null;
        }
        return rate * (runtime / 60); // Convert minutes to hours
    }
}
