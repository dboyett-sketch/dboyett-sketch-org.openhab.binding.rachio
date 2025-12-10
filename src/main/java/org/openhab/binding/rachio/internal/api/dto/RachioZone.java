package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * The {@link RachioZone} class defines a zone from the Rachio API
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZone {

    @SerializedName("id")
    private @Nullable String id;

    @SerializedName("zoneNumber")
    private int zoneNumber = 0;

    @SerializedName("name")
    private @Nullable String name;

    @SerializedName("enabled")
    private boolean enabled = true;

    @SerializedName("runtime")
    private @Nullable Integer runtime;

    @SerializedName("customSoil")
    private @Nullable CustomSoil customSoil;

    @SerializedName("customCrop")
    private @Nullable CustomCrop customCrop;

    @SerializedName("customNozzle")
    private @Nullable CustomNozzle customNozzle;

    @SerializedName("customSlope")
    private @Nullable CustomSlope customSlope;

    @SerializedName("customShade")
    private @Nullable CustomShade customShade;

    @SerializedName("rootDepth")
    private @Nullable Double rootDepth;

    @SerializedName("efficiency")
    private @Nullable Double efficiency;

    @SerializedName("waterAdjustment")
    private @Nullable List<Double> waterAdjustment;

    @SerializedName("area")
    private @Nullable Double area;

    @SerializedName("availableWater")
    private @Nullable Double availableWater;

    @SerializedName("managementAllowedDepletion")
    private @Nullable Double managementAllowedDepletion;

    @SerializedName("fixedRuntime")
    private @Nullable Integer fixedRuntime;

    @SerializedName("imageUrl")
    private @Nullable String imageUrl;

    @SerializedName("lastWateredDate")
    private @Nullable String lastWateredDate;

    @SerializedName("scheduleDataModified")
    private boolean scheduleDataModified = false;

    // Getters and setters

    public @Nullable String getId() {
        return id;
    }

    public void setId(@Nullable String id) {
        this.id = id;
    }

    public int getZoneNumber() {
        return zoneNumber;
    }

    public void setZoneNumber(int zoneNumber) {
        this.zoneNumber = zoneNumber;
    }

    public @Nullable String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public @Nullable Integer getRuntime() {
        return runtime;
    }

    public void setRuntime(@Nullable Integer runtime) {
        this.runtime = runtime;
    }

    // Professional irrigation data getters

    public @Nullable CustomSoil getSoil() {
        return customSoil;
    }

    public void setSoil(@Nullable CustomSoil customSoil) {
        this.customSoil = customSoil;
    }

    public @Nullable CustomCrop getCrop() {
        return customCrop;
    }

    public void setCrop(@Nullable CustomCrop customCrop) {
        this.customCrop = customCrop;
    }

    public @Nullable CustomNozzle getNozzle() {
        return customNozzle;
    }

    public void setNozzle(@Nullable CustomNozzle customNozzle) {
        this.customNozzle = customNozzle;
    }

    public @Nullable CustomSlope getSlope() {
        return customSlope;
    }

    public void setSlope(@Nullable CustomSlope customSlope) {
        this.customSlope = customSlope;
    }

    public @Nullable CustomShade getShade() {
        return customShade;
    }

    public void setShade(@Nullable CustomShade customShade) {
        this.customShade = customShade;
    }

    public @Nullable Double getRootDepth() {
        return rootDepth;
    }

    public void setRootDepth(@Nullable Double rootDepth) {
        this.rootDepth = rootDepth;
    }

    public @Nullable Double getEfficiency() {
        return efficiency;
    }

    public void setEfficiency(@Nullable Double efficiency) {
        this.efficiency = efficiency;
    }

    public @Nullable List<Double> getWaterAdjustment() {
        return waterAdjustment;
    }

    public void setWaterAdjustment(@Nullable List<Double> waterAdjustment) {
        this.waterAdjustment = waterAdjustment;
    }

    public @Nullable Double getArea() {
        return area;
    }

    public void setArea(@Nullable Double area) {
        this.area = area;
    }

    public @Nullable Double getAvailableWater() {
        return availableWater;
    }

    public void setAvailableWater(@Nullable Double availableWater) {
        this.availableWater = availableWater;
    }

    public @Nullable Double getManagementAllowedDepletion() {
        return managementAllowedDepletion;
    }

    public void setManagementAllowedDepletion(@Nullable Double managementAllowedDepletion) {
        this.managementAllowedDepletion = managementAllowedDepletion;
    }

    public @Nullable Integer getFixedRuntime() {
        return fixedRuntime;
    }

    public void setFixedRuntime(@Nullable Integer fixedRuntime) {
        this.fixedRuntime = fixedRuntime;
    }

    public @Nullable String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(@Nullable String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public @Nullable String getLastWateredDate() {
        return lastWateredDate;
    }

    public void setLastWateredDate(@Nullable String lastWateredDate) {
        this.lastWateredDate = lastWateredDate;
    }

    public boolean isScheduleDataModified() {
        return scheduleDataModified;
    }

    public void setScheduleDataModified(boolean scheduleDataModified) {
        this.scheduleDataModified = scheduleDataModified;
    }

    // Helper methods for professional irrigation features

    public boolean hasProfessionalData() {
        return customSoil != null || customCrop != null || customNozzle != null || 
               customSlope != null || customShade != null || rootDepth != null || 
               efficiency != null || area != null;
    }

    public String getZoneSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Zone ").append(zoneNumber);
        if (name != null && !name.isEmpty()) {
            summary.append(": ").append(name);
        }
        summary.append(" (").append(enabled ? "Enabled" : "Disabled").append(")");
        if (runtime != null) {
            summary.append(" Runtime: ").append(runtime).append(" min");
        }
        return summary.toString();
    }

    public @Nullable Double getWaterAdjustmentLevel(int level) {
        if (waterAdjustment != null && level >= 1 && level <= waterAdjustment.size()) {
            return waterAdjustment.get(level - 1);
        }
        return null;
    }

    public double getCalculatedRuntime(double et0) {
        // Simplified runtime calculation based on crop coefficient, area, and efficiency
        double runtime = 0;
        
        if (customCrop != null && customCrop.getCoefficient() != null) {
            double kc = customCrop.getCoefficient();
            if (area != null && efficiency != null && efficiency > 0) {
                // Basic calculation: (Area * ET0 * Kc) / (Efficiency * ApplicationRate)
                if (customNozzle != null && customNozzle.getRate() != null && customNozzle.getRate() > 0) {
                    runtime = (area * et0 * kc) / (efficiency * customNozzle.getRate() * 60);
                }
            }
        }
        
        return runtime;
    }

    @Override
    public String toString() {
        return "RachioZone{" +
                "id='" + id + '\'' +
                ", zoneNumber=" + zoneNumber +
                ", name='" + name + '\'' +
                ", enabled=" + enabled +
                ", runtime=" + runtime +
                ", customSoil=" + (customSoil != null) +
                ", customCrop=" + (customCrop != null) +
                ", customNozzle=" + (customNozzle != null) +
                ", customSlope=" + (customSlope != null) +
                ", customShade=" + (customShade != null) +
                ", rootDepth=" + rootDepth +
                ", efficiency=" + efficiency +
                ", waterAdjustment=" + (waterAdjustment != null ? waterAdjustment.size() : 0) +
                ", area=" + area +
                '}';
    }

    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RachioZone [");
        sb.append("id=").append(id);
        sb.append(", number=").append(zoneNumber);
        sb.append(", name=").append(name);
        sb.append(", enabled=").append(enabled);
        sb.append(", runtime=").append(runtime).append(" min");
        
        if (customSoil != null) {
            sb.append(", soil=").append(customSoil.getType());
        }
        if (customCrop != null) {
            sb.append(", crop=").append(customCrop.getType()).append(" (Kc=").append(customCrop.getCoefficient()).append(")");
        }
        if (customNozzle != null) {
            sb.append(", nozzle=").append(customNozzle.getType()).append(" (").append(customNozzle.getRate()).append(" in/hr)");
        }
        if (rootDepth != null) {
            sb.append(", rootDepth=").append(rootDepth).append(" in");
        }
        if (efficiency != null) {
            sb.append(", efficiency=").append(efficiency * 100).append("%");
        }
        if (area != null) {
            sb.append(", area=").append(area).append(" sq ft");
        }
        
        sb.append("]");
        return sb.toString();
    }
}
