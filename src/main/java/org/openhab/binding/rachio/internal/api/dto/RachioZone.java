package org.openhab.binding.rachio.internal.api.dto;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for Rachio zone
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZone {
    public @Nullable String id;
    public @Nullable String name;
    
    @SerializedName("zoneNumber")
    public @Nullable Integer zoneNumber;
    
    public @Nullable Boolean enabled;
    
    @SerializedName("runtime")
    public @Nullable Integer runtime;
    
    public @Nullable String status;
    
    // Professional irrigation data
    @SerializedName("area")
    public @Nullable Double area;
    
    @SerializedName("soil")
    public @Nullable CustomSoil soil;
    
    @SerializedName("crop")
    public @Nullable CustomCrop crop;
    
    @SerializedName("nozzle")
    public @Nullable CustomNozzle nozzle;
    
    @SerializedName("slope")
    public @Nullable CustomSlope slope;
    
    @SerializedName("shade")
    public @Nullable CustomShade shade;
    
    @SerializedName("rootZoneDepth")
    public @Nullable Double rootZoneDepth;
    
    @SerializedName("efficiency")
    public @Nullable Double efficiency;
    
    @SerializedName("availableWater")
    public @Nullable Double availableWater;
    
    @SerializedName("wateringAdjustmentRuntimes")
    public @Nullable List<Integer> wateringAdjustmentRuntimes;
    
    @SerializedName("imageUrl")
    public @Nullable String imageUrl;
    
    // Getters for compatibility with existing code
    public @Nullable String getId() {
        return id;
    }
    
    public @Nullable String getName() {
        return name;
    }
    
    public @Nullable Integer getZoneNumber() {
        return zoneNumber;
    }
    
    public @Nullable Boolean isEnabled() {
        return enabled;
    }
    
    public @Nullable Integer getRuntime() {
        return runtime;
    }
    
    public @Nullable String getStatus() {
        return status;
    }
    
    public @Nullable Double getArea() {
        return area;
    }
    
    public @Nullable CustomSoil getSoil() {
        return soil;
    }
    
    public @Nullable String getSoilType() {
        return soil != null ? soil.name : null;
    }
    
    public @Nullable CustomCrop getCrop() {
        return crop;
    }
    
    public @Nullable String getCropType() {
        return crop != null ? crop.name : null;
    }
    
    public @Nullable CustomNozzle getNozzle() {
        return nozzle;
    }
    
    public @Nullable String getNozzleType() {
        return nozzle != null ? nozzle.name : null;
    }
    
    public @Nullable CustomSlope getSlope() {
        return slope;
    }
    
    public @Nullable String getSlopeType() {
        return slope != null ? slope.name : null;
    }
    
    public @Nullable CustomShade getShade() {
        return shade;
    }
    
    public @Nullable String getShadeType() {
        return shade != null ? shade.name : null;
    }
    
    public @Nullable Double getRootZoneDepth() {
        return rootZoneDepth;
    }
    
    public @Nullable Double getEfficiency() {
        return efficiency;
    }
    
    public @Nullable Double getAvailableWater() {
        return availableWater;
    }
    
    public @Nullable List<Integer> getWateringAdjustmentRuntimes() {
        return wateringAdjustmentRuntimes;
    }
    
    public @Nullable String getImageUrl() {
        return imageUrl;
    }
}
