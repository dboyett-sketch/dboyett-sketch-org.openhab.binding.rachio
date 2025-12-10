package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link CustomShade} class defines shade data for a zone
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class CustomShade {

    @SerializedName("type")
    private @Nullable String type;

    @SerializedName("name")
    private @Nullable String name;

    @SerializedName("description")
    private @Nullable String description;

    @SerializedName("percentage")
    private @Nullable Double percentage;

    @SerializedName("hours")
    private @Nullable Double hours;

    /**
     * Get the shade type
     *
     * @return shade type
     */
    public @Nullable String getType() {
        return type;
    }

    /**
     * Set the shade type
     *
     * @param type shade type
     */
    public void setType(@Nullable String type) {
        this.type = type;
    }

    /**
     * Get the shade name
     *
     * @return shade name
     */
    public @Nullable String getName() {
        return name;
    }

    /**
     * Set the shade name
     *
     * @param name shade name
     */
    public void setName(@Nullable String name) {
        this.name = name;
    }

    /**
     * Get the shade description
     *
     * @return shade description
     */
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * Set the shade description
     *
     * @param description shade description
     */
    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    /**
     * Get shade percentage (0-100)
     *
     * @return shade percentage
     */
    public @Nullable Double getPercentage() {
        return percentage;
    }

    /**
     * Set shade percentage
     *
     * @param percentage shade percentage (0-100)
     */
    public void setPercentage(@Nullable Double percentage) {
        this.percentage = percentage;
    }

    /**
     * Get daily shade hours
     *
     * @return daily shade hours
     */
    public @Nullable Double getHours() {
        return hours;
    }

    /**
     * Set daily shade hours
     *
     * @param hours daily shade hours
     */
    public void setHours(@Nullable Double hours) {
        this.hours = hours;
    }

    /**
     * Check if shade data is valid
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
        return "CustomShade{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", percentage=" + percentage +
                ", hours=" + hours +
                '}';
    }

    /**
     * Get shade type as human readable string
     *
     * @return human readable shade type
     */
    public String getTypeDescription() {
        if (type == null) {
            return "Unknown";
        }
        switch (type.toLowerCase()) {
            case "full_sun":
                return "Full Sun - 6+ hours of direct sun";
            case "partial_sun":
                return "Partial Sun - 4-6 hours of sun";
            case "partial_shade":
                return "Partial Shade - 2-4 hours of sun";
            case "full_shade":
                return "Full Shade - Less than 2 hours of sun";
            case "custom":
                return "Custom - User-defined shade";
            default:
                return type;
        }
    }

    /**
     * Get shade percentage as formatted string
     *
     * @return formatted shade percentage
     */
    public String getPercentageFormatted() {
        if (percentage == null) {
            return "N/A";
        }
        return String.format("%.0f%%", percentage);
    }

    /**
     * Get shade hours as formatted string
     *
     * @return formatted shade hours
     */
    public String getHoursFormatted() {
        if (hours == null) {
            return "N/A";
        }
        return String.format("%.1f hours", hours);
    }

    /**
     * Get sun exposure percentage
     *
     * @return sun exposure percentage
     */
    public @Nullable Double getSunExposurePercentage() {
        if (percentage == null) {
            return null;
        }
        return 100.0 - percentage;
    }

    /**
     * Check if area is in full sun
     *
     * @return true if full sun
     */
    public boolean isFullSun() {
        return percentage != null && percentage <= 20;
    }

    /**
     * Check if area is in partial sun
     *
     * @return true if partial sun
     */
    public boolean isPartialSun() {
        return percentage != null && percentage > 20 && percentage <= 50;
    }

    /**
     * Check if area is in shade
     *
     * @return true if shade
     */
    public boolean isShade() {
        return percentage != null && percentage > 50;
    }
}
