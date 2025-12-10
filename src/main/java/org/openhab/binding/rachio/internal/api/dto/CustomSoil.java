package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link CustomSoil} class defines soil data for a zone
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class CustomSoil {

    @SerializedName("type")
    private @Nullable String type;

    @SerializedName("availableWater")
    private @Nullable Double availableWater;

    @SerializedName("name")
    private @Nullable String name;

    @SerializedName("description")
    private @Nullable String description;

    /**
     * Get the soil type
     *
     * @return soil type
     */
    public @Nullable String getType() {
        return type;
    }

    /**
     * Set the soil type
     *
     * @param type soil type
     */
    public void setType(@Nullable String type) {
        this.type = type;
    }

    /**
     * Get the available water capacity (inches per foot)
     *
     * @return available water capacity
     */
    public @Nullable Double getAvailableWater() {
        return availableWater;
    }

    /**
     * Set the available water capacity
     *
     * @param availableWater available water capacity
     */
    public void setAvailableWater(@Nullable Double availableWater) {
        this.availableWater = availableWater;
    }

    /**
     * Get the soil name
     *
     * @return soil name
     */
    public @Nullable String getName() {
        return name;
    }

    /**
     * Set the soil name
     *
     * @param name soil name
     */
    public void setName(@Nullable String name) {
        this.name = name;
    }

    /**
     * Get the soil description
     *
     * @return soil description
     */
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * Set the soil description
     *
     * @param description soil description
     */
    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    /**
     * Check if soil data is valid (has type)
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
        return "CustomSoil{" +
                "type='" + type + '\'' +
                ", availableWater=" + availableWater +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    /**
     * Get soil type as human readable string
     *
     * @return human readable soil type
     */
    public String getTypeDescription() {
        if (type == null) {
            return "Unknown";
        }
        switch (type.toLowerCase()) {
            case "sand":
                return "Sand - Coarse texture, fast drainage";
            case "loamy_sand":
                return "Loamy Sand - Moderately coarse";
            case "sandy_loam":
                return "Sandy Loam - Medium texture";
            case "loam":
                return "Loam - Ideal soil, balanced";
            case "silt_loam":
                return "Silt Loam - Fine texture";
            case "clay_loam":
                return "Clay Loam - Moderately fine";
            case "silty_clay_loam":
                return "Silty Clay Loam - Fine texture";
            case "clay":
                return "Clay - Very fine texture, slow drainage";
            case "custom":
                return "Custom - User-defined soil type";
            default:
                return type;
        }
    }

    /**
     * Get available water as formatted string
     *
     * @return formatted available water
     */
    public String getAvailableWaterFormatted() {
        if (availableWater == null) {
            return "N/A";
        }
        return String.format("%.2f in/ft", availableWater);
    }
}
