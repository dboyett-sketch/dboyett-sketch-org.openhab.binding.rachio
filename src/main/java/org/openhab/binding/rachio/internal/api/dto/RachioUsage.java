package org.openhab.binding.rachio.internal.api.dto;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for Rachio water usage and savings data
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioUsage {

    // Basic usage data
    @SerializedName("total")
    public @Nullable Double total;
    
    @SerializedName("savings")
    public @Nullable Double savings;
    
    @SerializedName("baseline")
    public @Nullable Double baseline;
    
    @SerializedName("actual")
    public @Nullable Double actual;
    
    @SerializedName("percentage")
    public @Nullable Double percentage;
    
    @SerializedName("units")
    public @Nullable String units;
    
    @SerializedName("period")
    public @Nullable String period;
    
    @SerializedName("startDate")
    public @Nullable LocalDate startDate;
    
    @SerializedName("endDate")
    public @Nullable LocalDate endDate;
    
    @SerializedName("generatedAt")
    public @Nullable java.time.Instant generatedAt;
    
    // Detailed breakdown
    @SerializedName("byMonth")
    public @Nullable List<MonthlyUsage> byMonth;
    
    @SerializedName("byZone")
    public @Nullable List<ZoneUsage> byZone;
    
    @SerializedName("bySchedule")
    public @Nullable List<ScheduleUsage> bySchedule;
    
    @SerializedName("byDay")
    public @Nullable List<DailyUsage> byDay;
    
    @SerializedName("byHour")
    public @Nullable List<HourlyUsage> byHour;
    
    @SerializedName("weatherCorrection")
    public @Nullable Double weatherCorrection;
    
    @SerializedName("efficiencyImprovement")
    public @Nullable Double efficiencyImprovement;
    
    @SerializedName("scheduleOptimization")
    public @Nullable Double scheduleOptimization;
    
    // Environmental impact
    @SerializedName("co2Savings")
    public @Nullable Double co2Savings;
    
    @SerializedName("energySavings")
    public @Nullable Double energySavings;
    
    @SerializedName("moneySavings")
    public @Nullable Double moneySavings;
    
    @SerializedName("currency")
    public @Nullable String currency;
    
    @SerializedName("waterSource")
    public @Nullable String waterSource;
    
    @SerializedName("waterCost")
    public @Nullable Double waterCost;
    
    @SerializedName("waterCostUnit")
    public @Nullable String waterCostUnit;
    
    // Statistics
    @SerializedName("averageDailyUsage")
    public @Nullable Double averageDailyUsage;
    
    @SerializedName("peakDailyUsage")
    public @Nullable Double peakDailyUsage;
    
    @SerializedName("peakUsageDate")
    public @Nullable LocalDate peakUsageDate;
    
    @SerializedName("daysWatered")
    public @Nullable Integer daysWatered;
    
    @SerializedName("totalWateringEvents")
    public @Nullable Integer totalWateringEvents;
    
    @SerializedName("averageWateringDuration")
    public @Nullable Double averageWateringDuration;
    
    @SerializedName("totalWateringDuration")
    public @Nullable Double totalWateringDuration;
    
    // Metadata
    @SerializedName("deviceId")
    public @Nullable String deviceId;
    
    @SerializedName("deviceName")
    public @Nullable String deviceName;
    
    @SerializedName("location")
    public @Nullable String location;
    
    @SerializedName("climateZone")
    public @Nullable String climateZone;
    
    @SerializedName("referenceYear")
    public @Nullable Year referenceYear;
    
    @SerializedName("comparisonMethod")
    public @Nullable String comparisonMethod;
    
    @SerializedName("confidence")
    public @Nullable Double confidence;
    
    @SerializedName("notes")
    public @Nullable String notes;
    
    /**
     * Monthly usage breakdown
     */
    @NonNullByDefault
    public static class MonthlyUsage {
        @SerializedName("month")
        public @Nullable YearMonth month;
        
        @SerializedName("total")
        public @Nullable Double total;
        
        @SerializedName("savings")
        public @Nullable Double savings;
        
        @SerializedName("baseline")
        public @Nullable Double baseline;
        
        @SerializedName("actual")
        public @Nullable Double actual;
        
        @SerializedName("percentage")
        public @Nullable Double percentage;
        
        @SerializedName("daysWatered")
        public @Nullable Integer daysWatered;
        
        @SerializedName("wateringEvents")
        public @Nullable Integer wateringEvents;
        
        @SerializedName("averageTemperature")
        public @Nullable Double averageTemperature;
        
        @SerializedName("totalPrecipitation")
        public @Nullable Double totalPrecipitation;
        
        @SerializedName("totalEvapotranspiration")
        public @Nullable Double totalEvapotranspiration;
        
        @SerializedName("weatherCorrection")
        public @Nullable Double weatherCorrection;
        
        @SerializedName("efficiency")
        public @Nullable Double efficiency;
    }
    
    /**
     * Zone usage breakdown
     */
    @NonNullByDefault
    public static class ZoneUsage {
        @SerializedName("zoneId")
        public @Nullable String zoneId;
        
        @SerializedName("zoneName")
        public @Nullable String zoneName;
        
        @SerializedName("zoneNumber")
        public @Nullable Integer zoneNumber;
        
        @SerializedName("total")
        public @Nullable Double total;
        
        @SerializedName("percentage")
        public @Nullable Double percentage;
        
        @SerializedName("savings")
        public @Nullable Double savings;
        
        @SerializedName("baseline")
        public @Nullable Double baseline;
        
        @SerializedName("actual")
        public @Nullable Double actual;
        
        @SerializedName("wateringEvents")
        public @Nullable Integer wateringEvents;
        
        @SerializedName("totalDuration")
        public @Nullable Double totalDuration;
        
        @SerializedName("averageDuration")
        public @Nullable Double averageDuration;
        
        @SerializedName("area")
        public @Nullable Double area;
        
        @SerializedName("cropType")
        public @Nullable String cropType;
        
        @SerializedName("soilType")
        public @Nullable String soilType;
        
        @SerializedName("efficiency")
        public @Nullable Double efficiency;
        
        @SerializedName("nozzleRate")
        public @Nullable Double nozzleRate;
        
        @SerializedName("recommendedRuntime")
        public @Nullable Double recommendedRuntime;
        
        @SerializedName("actualRuntime")
        public @Nullable Double actualRuntime;
    }
    
    /**
     * Schedule usage breakdown
     */
    @NonNullByDefault
    public static class ScheduleUsage {
        @SerializedName("scheduleId")
        public @Nullable String scheduleId;
        
        @SerializedName("scheduleName")
        public @Nullable String scheduleName;
        
        @SerializedName("scheduleType")
        public @Nullable String scheduleType;
        
        @SerializedName("total")
        public @Nullable Double total;
        
        @SerializedName("percentage")
        public @Nullable Double percentage;
        
        @SerializedName("savings")
        public @Nullable Double savings;
        
        @SerializedName("baseline")
        public @Nullable Double baseline;
        
        @SerializedName("actual")
        public @Nullable Double actual;
        
        @SerializedName("wateringEvents")
        public @Nullable Integer wateringEvents;
        
        @SerializedName("totalDuration")
        public @Nullable Double totalDuration;
        
        @SerializedName("averageDuration")
        public @Nullable Double averageDuration;
        
        @SerializedName("zones")
        public @Nullable Integer zones;
        
        @SerializedName("flexSchedule")
        public @Nullable Boolean flexSchedule;
        
        @SerializedName("smartCycle")
        public @Nullable Boolean smartCycle;
        
        @SerializedName("cycleSoak")
        public @Nullable Boolean cycleSoak;
        
        @SerializedName("efficiency")
        public @Nullable Double efficiency;
        
        @SerializedName("skipRate")
        public @Nullable Double skipRate;
        
        @SerializedName("weatherSkipRate")
        public @Nullable Double weatherSkipRate;
        
        @SerializedName("manualSkipRate")
        public @Nullable Double manualSkipRate;
    }
    
    /**
     * Daily usage breakdown
     */
    @NonNullByDefault
    public static class DailyUsage {
        @SerializedName("date")
        public @Nullable LocalDate date;
        
        @SerializedName("dayOfWeek")
        public @Nullable String dayOfWeek;
        
        @SerializedName("total")
        public @Nullable Double total;
        
        @SerializedName("savings")
        public @Nullable Double savings;
        
        @SerializedName("baseline")
        public @Nullable Double baseline;
        
        @SerializedName("actual")
        public @Nullable Double actual;
        
        @SerializedName("wateringEvents")
        public @Nullable Integer wateringEvents;
        
        @SerializedName("totalDuration")
        public @Nullable Double totalDuration;
        
        @SerializedName("temperature")
        public @Nullable Double temperature;
        
        @SerializedName("precipitation")
        public @Nullable Double precipitation;
        
        @SerializedName("evapotranspiration")
        public @Nullable Double evapotranspiration;
        
        @SerializedName("skipWatering")
        public @Nullable Boolean skipWatering;
        
        @SerializedName("skipReason")
        public @Nullable String skipReason;
        
        @SerializedName("rainDelay")
        public @Nullable Boolean rainDelay;
        
        @SerializedName("rainDelayHours")
        public @Nullable Integer rainDelayHours;
    }
    
    /**
     * Hourly usage breakdown
     */
    @NonNullByDefault
    public static class HourlyUsage {
        @SerializedName("hour")
        public @Nullable Integer hour;
        
        @SerializedName("total")
        public @Nullable Double total;
        
        @SerializedName("percentage")
        public @Nullable Double percentage;
        
        @SerializedName("wateringEvents")
        public @Nullable Integer wateringEvents;
        
        @SerializedName("averageDuration")
        public @Nullable Double averageDuration;
        
        @SerializedName("temperature")
        public @Nullable Double temperature;
        
        @SerializedName("humidity")
        public @Nullable Double humidity;
        
        @SerializedName("windSpeed")
        public @Nullable Double windSpeed;
        
        @SerializedName("efficiency")
        public @Nullable Double efficiency;
    }
    
    // Utility methods
    
    /**
     * Get total water usage
     */
    public @Nullable Double getTotalUsage() {
        return total;
    }
    
    /**
     * Get total water savings
     */
    public @Nullable Double getTotalSavings() {
        return savings;
    }
    
    /**
     * Get savings percentage
     */
    public @Nullable Double getSavingsPercentage() {
        return percentage;
    }
    
    /**
     * Get baseline usage
     */
    public @Nullable Double getBaselineUsage() {
        return baseline;
    }
    
    /**
     * Get actual usage
     */
    public @Nullable Double getActualUsage() {
        return actual;
    }
    
    /**
     * Get CO2 savings
     */
    public @Nullable Double getCo2Savings() {
        return co2Savings;
    }
    
    /**
     * Get energy savings
     */
    public @Nullable Double getEnergySavings() {
        return energySavings;
    }
    
    /**
     * Get money savings
     */
    public @Nullable Double getMoneySavings() {
        return moneySavings;
    }
    
    /**
     * Get water cost
     */
    public @Nullable Double getWaterCost() {
        return waterCost;
    }
    
    /**
     * Get average daily usage
     */
    public @Nullable Double getAverageDailyUsage() {
        return averageDailyUsage;
    }
    
    /**
     * Get peak daily usage
     */
    public @Nullable Double getPeakDailyUsage() {
        return peakDailyUsage;
    }
    
    /**
     * Get peak usage date
     */
    public @Nullable LocalDate getPeakUsageDate() {
        return peakUsageDate;
    }
    
    /**
     * Get number of days watered
     */
    public @Nullable Integer getDaysWatered() {
        return daysWatered;
    }
    
    /**
     * Get total watering events
     */
    public @Nullable Integer getTotalWateringEvents() {
        return totalWateringEvents;
    }
    
    /**
     * Get average watering duration
     */
    public @Nullable Double getAverageWateringDuration() {
        return averageWateringDuration;
    }
    
    /**
     * Get total watering duration
     */
    public @Nullable Double getTotalWateringDuration() {
        return totalWateringDuration;
    }
    
    /**
     * Get monthly usage data
     */
    public @Nullable List<MonthlyUsage> getMonthlyUsage() {
        return byMonth;
    }
    
    /**
     * Get zone usage data
     */
    public @Nullable List<ZoneUsage> getZoneUsage() {
        return byZone;
    }
    
    /**
     * Get schedule usage data
     */
    public @Nullable List<ScheduleUsage> getScheduleUsage() {
        return bySchedule;
    }
    
    /**
     * Get daily usage data
     */
    public @Nullable List<DailyUsage> getDailyUsage() {
        return byDay;
    }
    
    /**
     * Get hourly usage data
     */
    public @Nullable List<HourlyUsage> getHourlyUsage() {
        return byHour;
    }
    
    /**
     * Get device ID
     */
    public @Nullable String getDeviceId() {
        return deviceId;
    }
    
    /**
     * Get device name
     */
    public @Nullable String getDeviceName() {
        return deviceName;
    }
    
    /**
     * Get location
     */
    public @Nullable String getLocation() {
        return location;
    }
    
    /**
     * Get climate zone
     */
    public @Nullable String getClimateZone() {
        return climateZone;
    }
    
    /**
     * Get reference year
     */
    public @Nullable Year getReferenceYear() {
        return referenceYear;
    }
    
    /**
     * Get usage period
     */
    public @Nullable String getPeriod() {
        return period;
    }
    
    /**
     * Get start date
     */
    public @Nullable LocalDate getStartDate() {
        return startDate;
    }
    
    /**
     * Get end date
     */
    public @Nullable LocalDate getEndDate() {
        return endDate;
    }
    
    /**
     * Get generated timestamp
     */
    public @Nullable java.time.Instant getGeneratedAt() {
        return generatedAt;
    }
    
    /**
     * Get weather correction factor
     */
    public @Nullable Double getWeatherCorrection() {
        return weatherCorrection;
    }
    
    /**
     * Get efficiency improvement
     */
    public @Nullable Double getEfficiencyImprovement() {
        return efficiencyImprovement;
    }
    
    /**
     * Get schedule optimization
     */
    public @Nullable Double getScheduleOptimization() {
        return scheduleOptimization;
    }
    
    /**
     * Check if usage data is available
     */
    public boolean hasUsageData() {
        return total != null || savings != null || (byMonth != null && !byMonth.isEmpty());
    }
    
    /**
     * Get formatted summary
     */
    public String getFormattedSummary() {
        StringBuilder sb = new StringBuilder();
        
        if (deviceName != null) {
            sb.append(deviceName).append(": ");
        }
        
        if (total != null && savings != null && percentage != null) {
            sb.append(String.format("Usage: %.1f %s, Savings: %.1f %s (%.0f%%)",
                    total, units != null ? units : "units",
                    savings, units != null ? units : "units",
                    percentage));
        } else if (total != null) {
            sb.append(String.format("Usage: %.1f %s",
                    total, units != null ? units : "units"));
        }
        
        if (period != null) {
            sb.append(" for ").append(period);
        }
        
        if (startDate != null && endDate != null) {
            sb.append(" (").append(startDate).append(" to ").append(endDate).append(")");
        }
        
        if (moneySavings != null && currency != null) {
            sb.append(String.format(", Saved: %.2f %s", moneySavings, currency));
        }
        
        return sb.toString();
    }
    
    /**
     * Get detailed breakdown as map
     */
    public @Nullable Map<String, Object> getDetailedBreakdown() {
        if (!hasUsageData()) {
            return null;
        }
        
        java.util.HashMap<String, Object> breakdown = new java.util.HashMap<>();
        
        if (total != null) breakdown.put("total", total);
        if (savings != null) breakdown.put("savings", savings);
        if (percentage != null) breakdown.put("percentage", percentage);
        if (baseline != null) breakdown.put("baseline", baseline);
        if (actual != null) breakdown.put("actual", actual);
        
        if (averageDailyUsage != null) breakdown.put("averageDailyUsage", averageDailyUsage);
        if (peakDailyUsage != null) breakdown.put("peakDailyUsage", peakDailyUsage);
        if (peakUsageDate != null) breakdown.put("peakUsageDate", peakUsageDate.toString());
        
        if (daysWatered != null) breakdown.put("daysWatered", daysWatered);
        if (totalWateringEvents != null) breakdown.put("totalWateringEvents", totalWateringEvents);
        if (averageWateringDuration != null) breakdown.put("averageWateringDuration", averageWateringDuration);
        if (totalWateringDuration != null) breakdown.put("totalWateringDuration", totalWateringDuration);
        
        if (co2Savings != null) breakdown.put("co2Savings", co2Savings);
        if (energySavings != null) breakdown.put("energySavings", energySavings);
        if (moneySavings != null) breakdown.put("moneySavings", moneySavings);
        if (waterCost != null) breakdown.put("waterCost", waterCost);
        
        if (weatherCorrection != null) breakdown.put("weatherCorrection", weatherCorrection);
        if (efficiencyImprovement != null) breakdown.put("efficiencyImprovement", efficiencyImprovement);
        if (scheduleOptimization != null) breakdown.put("scheduleOptimization", scheduleOptimization);
        
        return breakdown;
    }
    
    /**
     * Get zone with highest usage
     */
    public @Nullable ZoneUsage getZoneWithHighestUsage() {
        if (byZone == null || byZone.isEmpty()) {
            return null;
        }
        
        ZoneUsage highest = null;
        for (ZoneUsage zone : byZone) {
            if (zone.total != null && (highest == null || zone.total > highest.total)) {
                highest = zone;
            }
        }
        return highest;
    }
    
    /**
     * Get zone with highest savings
     */
    public @Nullable ZoneUsage getZoneWithHighestSavings() {
        if (byZone == null || byZone.isEmpty()) {
            return null;
        }
        
        ZoneUsage highest = null;
        for (ZoneUsage zone : byZone) {
            if (zone.savings != null && (highest == null || zone.savings > highest.savings)) {
                highest = zone;
            }
        }
        return highest;
    }
    
    /**
     * Get month with highest usage
     */
    public @Nullable MonthlyUsage getMonthWithHighestUsage() {
        if (byMonth == null || byMonth.isEmpty()) {
            return null;
        }
        
        MonthlyUsage highest = null;
        for (MonthlyUsage month : byMonth) {
            if (month.total != null && (highest == null || month.total > highest.total)) {
                highest = month;
            }
        }
        return highest;
    }
    
    /**
     * Get month with highest savings
     */
    public @Nullable MonthlyUsage getMonthWithHighestSavings() {
        if (byMonth == null || byMonth.isEmpty()) {
            return null;
        }
        
        MonthlyUsage highest = null;
        for (MonthlyUsage month : byMonth) {
            if (month.savings != null && (highest == null || month.savings > highest.savings)) {
                highest = month;
            }
        }
        return highest;
    }
    
    @Override
    public String toString() {
        return String.format("RachioUsage{device=%s, total=%.1f, savings=%.1f, percentage=%.0f%%}",
                deviceName != null ? deviceName : deviceId,
                total != null ? total : 0.0,
                savings != null ? savings : 0.0,
                percentage != null ? percentage : 0.0);
    }
}
