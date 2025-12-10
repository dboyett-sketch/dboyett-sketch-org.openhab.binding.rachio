package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link CustomSlope} class defines slope data for a zone
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class CustomSlope {

    @SerializedName("type")
    private @Nullable String type;

    @SerializedName("name")
    private @Nullable String name;

    @SerializedName("description")
    private @Nullable String description;

    @SerializedName("degree")
    private @Nullable Double degree;

    @SerializedName("percentage")
    private @Nullable Double percentage;

    /**
     * Get the slope type
     *
     * @return slope type
     */
    public @Nullable String getType() {
        return type;
    }

    /**
     * Set the slope type
     *
     * @param type slope type
     */
    public void setType(@Nullable String type) {
        this.type = type;
    }

    /**
     * Get the slope name
     *
     * @return slope name
     */
    public @Nullable String getName() {
        return name;
    }

    /**
     * Set the slope name
     *
     * @param name slope name
     */
    public void setName(@Nullable String name) {
        this.name = name;
    }

    /**
     * Get the slope description
     *
     * @return slope description
     */
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * Set the slope description
     *
     * @param description slope description
     */
    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    /**
     * Get slope in degrees
     *
     * @return slope in degrees
     */
    public @Nullable Double getDegree() {
        return degree;
    }

    /**
     * Set slope in degrees
     *
     * @param degree slope in degrees
     */
    public void setDegree(@Nullable Double degree) {
        this.degree = degree;
    }

    /**
     * Get slope percentage
     *
     * @return slope percentage
     */
    public @Nullable Double getPercentage() {
        return percentage;
    }

    /**
     * Set slope percentage
     *
     * @param percentage slope percentage
     */
    public void setPercentage(@Nullable Double percentage) {
        this.percentage = percentage;
    }

    /**
     * Check if slope data is valid
     *
     * @return true if valid
     */
    public boolean isValid() {
        return type != null && !type.isEmpty();
    }

    /**
     * Get string representation
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "CustomSlope{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", degree=" + degree +
                ", percentage=" + percentage +
                '}';
    }

    /**
     * Get slope type as human readable string
     *
     * @return human readable slope type
     */
    public String getTypeDescription() {
        if (type == null) {
            return "Unknown";
        }
        switch (type.toLowerCase()) {
            case "flat":
                return "Flat - 0-2% slope";
            case "gentle":
                return "Gentle - 2-6% slope";
            case "moderate":
                return "Moderate - 6-12% slope";
            case "steep":
                return "Steep - 12-25% slope";
            case "very_steep":
                return "Very Steep - 25%+ slope";
            case "custom":
                return "Custom - User-defined slope";
            default:
                return type;
        }
    }

    /**
     * Get slope percentage as formatted string
     *
     * @return formatted slope percentage
     */
    public String getPercentageFormatted() {
        if (percentage == null) {
            return "N/A";
        }
        return String.format("%.1f%%", percentage);
    }

    /**
     * Get slope degree as formatted string
     *
     * @return formatted slope degree
     */
    public String getDegreeFormatted() {
        if (degree == null) {
            return "N/A";
        }
        return String.format("%.1f°", degree);
    }

    /**
     * Convert percentage to degrees
     *
     * @return degrees or null
     */
    public @Nullable Double percentageToDegrees() {
        if (percentage == null) {
            return null;
        }
        return Math.atan(percentage / 100.0) * (180.0 / Math.PI);
    }

    /**
     * Convert degrees to percentage
     *
     * @return percentage or null
     */
    public @Nullable Double degreesToPercentage() {
        if (degree == null) {
            return null;
        }
        return Math.tan(degree * Math.PI / 180.0) * 100.0;
    }
}
