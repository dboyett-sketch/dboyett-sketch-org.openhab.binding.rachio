package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO for Rachio Zone
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZone {
    @Nullable
    public String id;
    
    @Nullable
    public String deviceId;
    
    @Nullable
    public Integer zoneNumber;
    
    @Nullable
    public String name;
    
    @Nullable
    public Boolean enabled;
    
    @Nullable
    public Integer runtime;
    
    @Nullable
    public String status; // STARTED, STOPPED, COMPLETED
    
    @Nullable
    public CustomSoil customSoil;
    
    @Nullable
    public CustomCrop customCrop;
    
    @Nullable
    public CustomNozzle customNozzle;
    
    @Nullable
    public CustomSlope customSlope;
    
    @Nullable
    public CustomShade customShade;
    
    @Nullable
    public Double rootDepth; // inches
    
    @Nullable
    public Double efficiency; // percentage
    
    @Nullable
    public Map<Integer, Double> waterAdjustment; // levels 1-5
    
    @Nullable
    public Double area; // square feet
    
    // Additional professional data fields
    @Nullable
    public Double availableWater;
    
    @Nullable
    public Double cropCoefficient;
    
    @Nullable
    public Double nozzleInchesPerHour;
    
    @Nullable
    public String soilType;
    
    @Nullable
    public String cropType;
    
    @Nullable
    public String nozzleType;
    
    @Nullable
    public String slopeType;
    
    @Nullable
    public String shadeType;

    @Override
    public String toString() {
        return "RachioZone [id=" + id + ", name=" + name + ", zoneNumber=" + zoneNumber + ", enabled=" + enabled + 
               ", runtime=" + runtime + ", status=" + status + ", area=" + area + "]";
    }
    
    // Helper method to get water adjustment level
    public double getWaterAdjustmentLevel(int level) {
        if (waterAdjustment != null && waterAdjustment.containsKey(level)) {
            return waterAdjustment.get(level);
        }
        return 0.0;
    }
}
