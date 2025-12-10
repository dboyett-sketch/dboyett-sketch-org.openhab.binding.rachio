package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link CustomCrop} class defines crop data for a zone
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class CustomCrop {

    @SerializedName("type")
    private @Nullable String type;

    @SerializedName("coefficient")
    private @Nullable Double coefficient;

    @SerializedName("name")
    private @Nullable String name;

    @SerializedName("description")
    private @Nullable String description;

    @SerializedName("rootDepth")
    private @Nullable Double rootDepth;

    /**
     * Get the crop type
     *
     * @return crop type
     */
    public @Nullable String getType() {
        return type;
    }

    /**
     * Set the crop type
     *
     * @param type crop type
     */
    public void setType(@Nullable String type) {
        this.type = type;
    }

    /**
     * Get the crop coefficient (Kc value)
     *
     * @return crop coefficient
     */
    public @Nullable Double getCoefficient() {
        return coefficient;
    }

    /**
     * Set the crop coefficient
     *
     * @param coefficient crop coefficient
     */
    public void setCoefficient(@Nullable Double coefficient) {
        this.coefficient = coefficient;
    }

    /**
     * Get the crop name
     *
     * @return crop name
     */
    public @Nullable String getName() {
        return name;
    }

    /**
     * Set the crop name
     *
     * @param name crop name
     */
    public void setName(@Nullable String name) {
        this.name = name;
    }

    /**
     * Get the crop description
     *
     * @return crop description
     */
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * Set the crop description
     *
     * @param description crop description
     */
    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    /**
     * Get the root depth (inches)
     *
     * @return root depth
     */
    public @Nullable Double getRootDepth() {
        return rootDepth;
    }

    /**
     * Set the root depth
     *
     * @param rootDepth root depth
     */
    public void setRootDepth(@Nullable Double rootDepth) {
        this.rootDepth = rootDepth;
    }

    /**
     * Check if crop data is valid (has type and coefficient)
     *
     * @return true if valid
     */
    public boolean isValid() {
        return type != null && !type.isEmpty() && coefficient != null;
    }

    /**
     * Get string representation
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "CustomCrop{" +
                "type='" + type + '\'' +
                ", coefficient=" + coefficient +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", rootDepth=" + rootDepth +
                '}';
    }

    /**
     * Get crop type as human readable string
     *
     * @return human readable crop type
     */
    public String getTypeDescription() {
        if (type == null) {
            return "Unknown";
        }
        switch (type.toLowerCase()) {
            case "turf_grass":
                return "Turf Grass - Lawns, athletic fields";
            case "warm_season_turf":
                return "Warm Season Turf - Bermuda, Zoysia";
            case "cool_season_turf":
                return "Cool Season Turf - Fescue, Bluegrass";
            case "annual_flowers":
                return "Annual Flowers - Seasonal color";
            case "perennial_flowers":
                return "Perennial Flowers - Year-round blooms";
            case "ground_cover":
                return "Ground Cover - Low-growing plants";
            case "shrubs":
                return "Shrubs - Woody plants";
            case "trees":
                return "Trees - Large woody plants";
            case "vegetables":
                return "Vegetables - Edible plants";
            case "fruit_trees":
                return "Fruit Trees - Apple, Peach, etc.";
            case "vines":
                return "Vines - Climbing plants";
            case "cacti_succulents":
                return "Cacti & Succulents - Drought tolerant";
            case "herbs":
                return "Herbs - Culinary & medicinal";
            case "custom":
                return "Custom - User-defined crop type";
            default:
                return type;
        }
    }

    /**
     * Get crop coefficient as formatted string
     *
     * @return formatted crop coefficient
     */
    public String getCoefficientFormatted() {
        if (coefficient == null) {
            return "N/A";
        }
        return String.format("%.2f", coefficient);
    }

    /**
     * Get root depth as formatted string
     *
     * @return formatted root depth
     */
    public String getRootDepthFormatted() {
        if (rootDepth == null) {
            return "N/A";
        }
        return String.format("%.1f in", rootDepth);
    }

    /**
     * Get water requirement based on crop coefficient
     *
     * @param referenceEvapotranspiration reference ET value
     * @return calculated water requirement
     */
    public @Nullable Double calculateWaterRequirement(@Nullable Double referenceEvapotranspiration) {
        if (coefficient == null || referenceEvapotranspiration == null) {
            return null;
        }
        return coefficient * referenceEvapotranspiration;
    }
}
