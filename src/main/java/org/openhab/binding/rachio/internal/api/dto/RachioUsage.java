package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import com.google.gson.annotations.SerializedName;

@NonNullByDefault
public class RachioUsage {
    public String deviceId;
    
    @SerializedName("totalDuration")
    public long totalDuration; // seconds
    
    @SerializedName("totalWaterUsage")
    public double totalWaterUsage; // gallons
    
    @SerializedName("estimatedSavings")
    public double estimatedSavings; // gallons
    
    @SerializedName("currentMonth")
    public UsagePeriod currentMonth;
    
    @SerializedName("currentYear")
    public UsagePeriod currentYear;
    
    @SerializedName("allTime")
    public UsagePeriod allTime;
    
    public static class UsagePeriod {
        public long duration;
        public double waterUsage;
        public double savings;
        public int runs;
    }
}
