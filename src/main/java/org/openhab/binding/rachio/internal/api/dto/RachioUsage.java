package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for Water Usage Analytics
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioUsage {
    @Nullable
    public String deviceId;
    
    @Nullable
    public Instant periodStart;
    
    @Nullable
    public Instant periodEnd;
    
    @Nullable
    public Double totalGallons;
    
    @Nullable
    public Double averageDailyGallons;
    
    @Nullable
    public Integer totalRuns;
    
    @Nullable
    public Integer totalRuntime; // seconds
    
    @Nullable
    public Map<String, Double> zoneGallons; // zoneId -> gallons
    
    @Nullable
    public Double savingsGallons; // compared to traditional irrigation
    
    @Nullable
    public Double savingsPercentage;
    
    @Nullable
    public Double environmentalImpact; // CO2 savings in kg
    
    @Override
    public String toString() {
        return "RachioUsage [deviceId=" + deviceId + ", totalGallons=" + totalGallons + 
               ", savingsPercentage=" + savingsPercentage + "]";
    }
}
