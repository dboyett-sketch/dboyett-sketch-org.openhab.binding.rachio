package org.openhab.binding.rachio.internal.api.dto;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

@NonNullByDefault
public class RachioZone {
    @SerializedName("id")
    private String id = "";

    @SerializedName("zoneNumber")
    private int zoneNumber = 0;

    @SerializedName("name")
    private String name = "";

    @SerializedName("enabled")
    private boolean enabled = false;

    @SerializedName("runtime")
    private int runtime = 0;

    @SerializedName("maxRuntime")
    private int maxRuntime = 0;

    @SerializedName("imageUrl")
    @Nullable
    private String imageUrl;

    @SerializedName("soil")
    @Nullable
    private CustomSoil soil;

    @SerializedName("crop")
    @Nullable
    private CustomCrop crop;

    @SerializedName("nozzle")
    @Nullable
    private CustomNozzle nozzle;

    @SerializedName("slope")
    @Nullable
    private CustomSlope slope;

    @SerializedName("shade")
    @Nullable
    private CustomShade shade;

    @SerializedName("rootDepth")
    private double rootDepth = 0.0;

    @SerializedName("irrigationEfficiency")
    private double irrigationEfficiency = 0.0;

    @SerializedName("adjustmentLevels")
    @Nullable
    private List<Integer> adjustmentLevels;

    @SerializedName("zoneArea")
    private double zoneArea = 0.0;

    // Getters
    public String getId() {
        return id;
    }

    public int getZoneNumber() {
        return zoneNumber;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getRuntime() {
        return runtime;
    }

    public int getMaxRuntime() {
        return maxRuntime;
    }

    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    @Nullable
    public CustomSoil getSoil() {
        return soil;
    }

    @Nullable
    public CustomCrop getCrop() {
        return crop;
    }

    @Nullable
    public CustomNozzle getNozzle() {
        return nozzle;
    }

    @Nullable
    public CustomSlope getSlope() {
        return slope;
    }

    @Nullable
    public CustomShade getShade() {
        return shade;
    }

    public double getRootDepth() {
        return rootDepth;
    }

    public double getIrrigationEfficiency() {
        return irrigationEfficiency;
    }

    @Nullable
    public List<Integer> getAdjustmentLevels() {
        return adjustmentLevels;
    }

    public double getZoneArea() {
        return zoneArea;
    }

    // Helper methods for accessing nested objects
    @Nullable
    public String getSoilName() {
        return soil != null ? soil.getName() : null;
    }

    public double getSoilAvailableWater() {
        return soil != null ? soil.getAvailableWater() : 0.0;
    }

    @Nullable
    public String getCropName() {
        return crop != null ? crop.getName() : null;
    }

    public double getCropCoefficient() {
        return crop != null ? crop.getCoefficient() : 0.0;
    }

    @Nullable
    public String getNozzleName() {
        return nozzle != null ? nozzle.getName() : null;
    }

    public double getNozzleInchesPerHour() {
        return nozzle != null ? nozzle.getInchesPerHour() : 0.0;
    }

    @Nullable
    public String getSlopeName() {
        return slope != null ? slope.getName() : null;
    }

    @Nullable
    public String getShadeName() {
        return shade != null ? shade.getName() : null;
    }
}
