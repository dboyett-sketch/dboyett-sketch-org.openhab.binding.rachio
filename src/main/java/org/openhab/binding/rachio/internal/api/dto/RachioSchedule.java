package org.openhab.binding.rachio.internal.api.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for Rachio irrigation schedules
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioSchedule {

    // Schedule type constants
    public static final String TYPE_FIXED = "FIXED";
    public static final String TYPE_FLEX = "FLEX";
    public static final String TYPE_FLEX_MONTHLY = "FLEX_MONTHLY";
    public static final String TYPE_MANUAL = "MANUAL";
    public static final String TYPE_QUICK_RUN = "QUICK_RUN";
    
    // Schedule status constants
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_PAUSED = "PAUSED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_SKIPPED = "SKIPPED";
    public static final String STATUS_RUNNING = "RUNNING";
    
    // Cycle type constants
    public static final String CYCLE_TYPE_SINGLE = "SINGLE";
    public static final String CYCLE_TYPE_CYCLE_SOAK = "CYCLE_SOAK";
    public static final String CYCLE_TYPE_SMART_CYCLE = "SMART_CYCLE";
    
    // Schedule fields
    @SerializedName("id")
    public @Nullable String id;
    
    @SerializedName("name")
    public @Nullable String name;
    
    @SerializedName("description")
    public @Nullable String description;
    
    @SerializedName("type")
    public @Nullable String type;
    
    @SerializedName("status")
    public @Nullable String status;
    
    @SerializedName("enabled")
    public @Nullable Boolean enabled;
    
    @SerializedName("deviceId")
    public @Nullable String deviceId;
    
    @SerializedName("deviceName")
    public @Nullable String deviceName;
    
    @SerializedName("createdDate")
    public @Nullable Instant createdDate;
    
    @SerializedName("updatedDate")
    public @Nullable Instant updatedDate;
    
    @SerializedName("lastRunDate")
    public @Nullable Instant lastRunDate;
    
    @SerializedName("nextRunDate")
    public @Nullable Instant nextRunDate;
    
    @SerializedName("totalRuns")
    public @Nullable Integer totalRuns;
    
    @SerializedName("totalRunTime")
    public @Nullable Integer totalRunTime;
    
    @SerializedName("totalWaterUsage")
    public @Nullable Double totalWaterUsage;
    
    @SerializedName("totalWaterSavings")
    public @Nullable Double totalWaterSavings;
    
    @SerializedName("averageRunTime")
    public @Nullable Double averageRunTime;
    
    @SerializedName("averageWaterUsage")
    public @Nullable Double averageWaterUsage;
    
    @SerializedName("successRate")
    public @Nullable Double successRate;
    
    @SerializedName("skipRate")
    public @Nullable Double skipRate;
    
    @SerializedName("weatherSkipRate")
    public @Nullable Double weatherSkipRate;
    
    // Schedule timing
    @SerializedName("startTime")
    public @Nullable LocalTime startTime;
    
    @SerializedName("startDate")
    public @Nullable Instant startDate;
    
    @SerializedName("endDate")
    public @Nullable Instant endDate;
    
    @SerializedName("duration")
    public @Nullable Integer duration;
    
    @SerializedName("totalDuration")
    public @Nullable Integer totalDuration;
    
    @SerializedName("frequency")
    public @Nullable ScheduleFrequency frequency;
    
    @SerializedName("wateringDays")
    public @Nullable List<DayOfWeek> wateringDays;
    
    @SerializedName("specificDays")
    public @Nullable List<Instant> specificDays;
    
    @SerializedName("excludedDates")
    public @Nullable List<Instant> excludedDates;
    
    // Zone configuration
    @SerializedName("zones")
    public @Nullable List<ScheduleZone> zones;
    
    @SerializedName("zoneOrder")
    public @Nullable List<Integer> zoneOrder;
    
    @SerializedName("totalZones")
    public @Nullable Integer totalZones;
    
    // Smart features
    @SerializedName("flexSchedule")
    public @Nullable Boolean flexSchedule;
    
    @SerializedName("flexScheduleSettings")
    public @Nullable FlexScheduleSettings flexScheduleSettings;
    
    @SerializedName("smartCycle")
    public @Nullable Boolean smartCycle;
    
    @SerializedName("smartCycleSettings")
    public @Nullable SmartCycleSettings smartCycleSettings;
    
    @SerializedName("cycleSoak")
    public @Nullable Boolean cycleSoak;
    
    @SerializedName("cycleSoakSettings")
    public @Nullable CycleSoakSettings cycleSoakSettings;
    
    @SerializedName("weatherIntelligence")
    public @Nullable Boolean weatherIntelligence;
    
    @SerializedName("weatherIntelligenceSettings")
    public @Nullable WeatherIntelligenceSettings weatherIntelligenceSettings;
    
    @SerializedName("waterBudget")
    public @Nullable Double waterBudget;
    
    @SerializedName("waterBudgetSettings")
    public @Nullable WaterBudgetSettings waterBudgetSettings;
    
    // Advanced settings
    @SerializedName("advancedSettings")
    public @Nullable AdvancedSettings advancedSettings;
    
    @SerializedName("metadata")
    public @Nullable Map<String, Object> metadata;
    
    /**
     * Schedule frequency
     */
    @NonNullByDefault
    public static class ScheduleFrequency {
        @SerializedName("type")
        public @Nullable String type; // DAILY, WEEKLY, BIWEEKLY, MONTHLY, CUSTOM
        
        @SerializedName("interval")
        public @Nullable Integer interval;
        
        @SerializedName("customDays")
        public @Nullable List<Integer> customDays;
        
        @SerializedName("customWeeks")
        public @Nullable List<Integer> customWeeks;
        
        @SerializedName("customMonths")
        public @Nullable List<Integer> customMonths;
    }
    
    /**
     * Schedule zone configuration
     */
    @NonNullByDefault
    public static class ScheduleZone {
        @SerializedName("zoneId")
        public @Nullable String zoneId;
        
        @SerializedName("zoneName")
        public @Nullable String zoneName;
        
        @SerializedName("zoneNumber")
        public @Nullable Integer zoneNumber;
        
        @SerializedName("duration")
        public @Nullable Integer duration;
        
        @SerializedName("adjustedDuration")
        public @Nullable Integer adjustedDuration;
        
        @SerializedName("runtime")
        public @Nullable Integer runtime;
        
        @SerializedName("order")
        public @Nullable Integer order;
        
        @SerializedName("enabled")
        public @Nullable Boolean enabled;
        
        @SerializedName("customDuration")
        public @Nullable Boolean customDuration;
        
        @SerializedName("waterAdjustment")
        public @Nullable Double waterAdjustment;
        
        @SerializedName("soilType")
        public @Nullable String soilType;
        
        @SerializedName("cropType")
        public @Nullable String cropType;
        
        @SerializedName("cropCoefficient")
        public @Nullable Double cropCoefficient;
        
        @SerializedName("nozzleType")
        public @Nullable String nozzleType;
        
        @SerializedName("nozzleRate")
        public @Nullable Double nozzleRate;
        
        @SerializedName("efficiency")
        public @Nullable Double efficiency;
        
        @SerializedName("rootDepth")
        public @Nullable Double rootDepth;
        
        @SerializedName("area")
        public @Nullable Double area;
        
        @SerializedName("slopeType")
        public @Nullable String slopeType;
        
        @SerializedName("shadeType")
        public @Nullable String shadeType;
    }
    
    /**
     * Flex schedule settings
     */
    @NonNullByDefault
    public static class FlexScheduleSettings {
        @SerializedName("enabled")
        public @Nullable Boolean enabled;
        
        @SerializedName("windowStartTime")
        public @Nullable LocalTime windowStartTime;
        
        @SerializedName("windowEndTime")
        public @Nullable LocalTime windowEndTime;
        
        @SerializedName("windowDuration")
        public @Nullable Integer windowDuration;
        
        @SerializedName("allowSplitting")
        public @Nullable Boolean allowSplitting;
        
        @SerializedName("maxSplits")
        public @Nullable Integer maxSplits;
        
        @SerializedName("minSplitDuration")
        public @Nullable Integer minSplitDuration;
        
        @SerializedName("weatherAdjustment")
        public @Nullable Boolean weatherAdjustment;
        
        @SerializedName("soilMoistureAdjustment")
        public @Nullable Boolean soilMoistureAdjustment;
        
        @SerializedName("priority")
        public @Nullable Integer priority;
    }
    
    /**
     * Smart cycle settings
     */
    @NonNullByDefault
    public static class SmartCycleSettings {
        @SerializedName("enabled")
        public @Nullable Boolean enabled;
        
        @SerializedName("cycleDuration")
        public @Nullable Integer cycleDuration;
        
        @SerializedName("soakDuration")
        public @Nullable Integer soakDuration;
        
        @SerializedName("maxCycles")
        public @Nullable Integer maxCycles;
        
        @SerializedName("minCycleDuration")
        public @Nullable Integer minCycleDuration;
        
        @SerializedName("soilType")
        public @Nullable String soilType;
        
        @SerializedName("slopeType")
        public @Nullable String slopeType;
        
        @SerializedName("efficiencyImprovement")
        public @Nullable Double efficiencyImprovement;
        
        @SerializedName("waterSavings")
        public @Nullable Double waterSavings;
    }
    
    /**
     * Cycle soak settings
     */
    @NonNullByDefault
    public static class CycleSoakSettings {
        @SerializedName("enabled")
        public @Nullable Boolean enabled;
        
        @SerializedName("cycleDuration")
        public @Nullable Integer cycleDuration;
        
        @SerializedName("soakDuration")
        public @Nullable Integer soakDuration;
        
        @SerializedName("cycles")
        public @Nullable Integer cycles;
        
        @SerializedName("totalDuration")
        public @Nullable Integer totalDuration;
        
        @SerializedName("soilType")
        public @Nullable String soilType;
        
        @SerializedName("slopeType")
        public @Nullable String slopeType;
        
        @SerializedName("runoffReduction")
        public @Nullable Double runoffReduction;
        
        @SerializedName("efficiencyImprovement")
        public @Nullable Double efficiencyImprovement;
    }
    
    /**
     * Weather intelligence settings
     */
    @NonNullByDefault
    public static class WeatherIntelligenceSettings {
        @SerializedName("enabled")
        public @Nullable Boolean enabled;
        
        @SerializedName("skipOnRain")
        public @Nullable Boolean skipOnRain;
        
        @SerializedName("skipOnFreeze")
        public @Nullable Boolean skipOnFreeze;
        
        @SerializedName("skipOnWind")
        public @Nullable Boolean skipOnWind;
        
        @SerializedName("rainThreshold")
        public @Nullable Double rainThreshold;
        
        @SerializedName("freezeThreshold")
        public @Nullable Double freezeThreshold;
        
        @SerializedName("windThreshold")
        public @Nullable Double windThreshold;
        
        @SerializedName("forecastDays")
        public @Nullable Integer forecastDays;
        
        @SerializedName("weatherProvider")
        public @Nullable String weatherProvider;
        
        @SerializedName("adjustForEvapotranspiration")
        public @Nullable Boolean adjustForEvapotranspiration;
        
        @SerializedName("adjustForSolarRadiation")
        public @Nullable Boolean adjustForSolarRadiation;
        
        @SerializedName("adjustForHumidity")
        public @Nullable Boolean adjustForHumidity;
        
        @SerializedName("skipRate")
        public @Nullable Double skipRate;
        
        @SerializedName("waterSavings")
        public @Nullable Double waterSavings;
    }
    
    /**
     * Water budget settings
     */
    @NonNullByDefault
    public static class WaterBudgetSettings {
        @SerializedName("enabled")
        public @Nullable Boolean enabled;
        
        @SerializedName("budgetPercentage")
        public @Nullable Double budgetPercentage;
        
        @SerializedName("adjustmentIncrement")
        public @Nullable Double adjustmentIncrement;
        
        @SerializedName("minPercentage")
        public @Nullable Double minPercentage;
        
        @SerializedName("maxPercentage")
        public @Nullable Double maxPercentage;
        
        @SerializedName("seasonalAdjustment")
        public @Nullable Boolean seasonalAdjustment;
        
        @SerializedName("monthlyAdjustments")
        public @Nullable Map<String, Double> monthlyAdjustments;
        
        @SerializedName("weatherAdjustment")
        public @Nullable Boolean weatherAdjustment;
        
        @SerializedName("soilMoistureAdjustment")
        public @Nullable Boolean soilMoistureAdjustment;
        
        @SerializedName("waterSavings")
        public @Nullable Double waterSavings;
    }
    
    /**
     * Advanced settings
     */
    @NonNullByDefault
    public static class AdvancedSettings {
        @SerializedName("allowOverlap")
        public @Nullable Boolean allowOverlap;
        
        @SerializedName("maxConcurrentZones")
        public @Nullable Integer maxConcurrentZones;
        
        @SerializedName("pauseBetweenZones")
        public @Nullable Integer pauseBetweenZones;
        
        @SerializedName("retryOnFailure")
        public @Nullable Boolean retryOnFailure;
        
        @SerializedName("maxRetries")
        public @Nullable Integer maxRetries;
        
        @SerializedName("retryDelay")
        public @Nullable Integer retryDelay;
        
        @SerializedName("notifyOnStart")
        public @Nullable Boolean notifyOnStart;
        
        @SerializedName("notifyOnComplete")
        public @Nullable Boolean notifyOnComplete;
        
        @SerializedName("notifyOnSkip")
        public @Nullable Boolean notifyOnSkip;
        
        @SerializedName("notifyOnFailure")
        public @Nullable Boolean notifyOnFailure;
        
        @SerializedName("rainDelayHandling")
        public @Nullable String rainDelayHandling;
        
        @SerializedName("seasonalShutdown")
        public @Nullable Boolean seasonalShutdown;
        
        @SerializedName("shutdownStartMonth")
        public @Nullable Integer shutdownStartMonth;
        
        @SerializedName("shutdownEndMonth")
        public @Nullable Integer shutdownEndMonth;
        
        @SerializedName("maintenanceMode")
        public @Nullable Boolean maintenanceMode;
    }
    
    // Utility methods
    
    /**
     * Check if schedule is active
     */
    public boolean isActive() {
        return STATUS_ACTIVE.equals(status) && (enabled == null || enabled);
    }
    
    /**
     * Check if schedule is running
     */
    public boolean isRunning() {
        return STATUS_RUNNING.equals(status);
    }
    
    /**
     * Check if schedule is paused
     */
    public boolean isPaused() {
        return STATUS_PAUSED.equals(status);
    }
    
    /**
     * Check if schedule is completed
     */
    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status);
    }
    
    /**
     * Check if schedule is skipped
     */
    public boolean isSkipped() {
        return STATUS_SKIPPED.equals(status);
    }
    
    /**
     * Check if schedule is enabled
     */
    public boolean isEnabled() {
        return enabled != null && enabled;
    }
    
    /**
     * Check if schedule is a flex schedule
     */
    public boolean isFlexSchedule() {
        return TYPE_FLEX.equals(type) || TYPE_FLEX_MONTHLY.equals(type) || 
               (flexSchedule != null && flexSchedule);
    }
    
    /**
     * Check if schedule uses smart cycle
     */
    public boolean usesSmartCycle() {
        return smartCycle != null && smartCycle;
    }
    
    /**
     * Check if schedule uses cycle soak
     */
    public boolean usesCycleSoak() {
        return cycleSoak != null && cycleSoak;
    }
    
    /**
     * Check if schedule uses weather intelligence
     */
    public boolean usesWeatherIntelligence() {
        return weatherIntelligence != null && weatherIntelligence;
    }
    
    /**
     * Check if schedule has water budget
     */
    public boolean hasWaterBudget() {
        return waterBudget != null && waterBudget > 0;
    }
    
    /**
     * Get total runtime in minutes
     */
    public @Nullable Double getTotalRuntimeMinutes() {
        if (totalRunTime != null) {
            return totalRunTime / 60.0;
        }
        return null;
    }
    
    /**
     * Get average runtime in minutes
     */
    public @Nullable Double getAverageRuntimeMinutes() {
        if (averageRunTime != null) {
            return averageRunTime / 60.0;
        }
        return null;
    }
    
    /**
     * Get duration in minutes
     */
    public @Nullable Double getDurationMinutes() {
        if (duration != null) {
            return duration / 60.0;
        }
        return null;
    }
    
    /**
     * Get total duration in minutes
     */
    public @Nullable Double getTotalDurationMinutes() {
        if (totalDuration != null) {
            return totalDuration / 60.0;
        }
        return null;
    }
    
    /**
     * Get next run time as Instant
     */
    public @Nullable Instant getNextRunTime() {
        return nextRunDate;
    }
    
    /**
     * Get last run time as Instant
     */
    public @Nullable Instant getLastRunTime() {
        return lastRunDate;
    }
    
    /**
     * Get schedule zones
     */
    public @Nullable List<ScheduleZone> getZones() {
        return zones;
    }
    
    /**
     * Get zone count
     */
    public int getZoneCount() {
        if (zones != null) {
            return zones.size();
        }
        return totalZones != null ? totalZones : 0;
    }
    
    /**
     * Get enabled zone count
     */
    public int getEnabledZoneCount() {
        if (zones == null) {
            return 0;
        }
        int count = 0;
        for (ScheduleZone zone : zones) {
            if (zone.enabled != null && zone.enabled) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Get schedule summary
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        
        if (name != null) {
            sb.append(name);
        } else {
            sb.append("Unnamed Schedule");
        }
        
        if (type != null) {
            sb.append(" (").append(type);
            if (isFlexSchedule()) {
                sb.append(" Flex");
            }
            sb.append(")");
        }
        
        if (status != null) {
            sb.append(" - ").append(status);
        }
        
        if (nextRunDate != null) {
            sb.append(" - Next: ").append(nextRunDate);
        }
        
        if (totalZones != null) {
            sb.append(" - Zones: ").append(totalZones);
        }
        
        if (duration != null) {
            sb.append(" - Duration: ").append(getDurationMinutes()).append(" min");
        }
        
        return sb.toString();
    }
    
    /**
     * Get schedule details as map
     */
    public @Nullable Map<String, Object> getDetailsMap() {
        java.util.HashMap<String, Object> map = new java.util.HashMap<>();
        
        map.put("id", id);
        map.put("name", name);
        map.put("type", type);
        map.put("status", status);
        map.put("enabled", isEnabled());
        map.put("active", isActive());
        map.put("running", isRunning());
        map.put("paused", isPaused());
        
        if (deviceId != null) map.put("deviceId", deviceId);
        if (deviceName != null) map.put("deviceName", deviceName);
        
        if (createdDate != null) map.put("createdDate", createdDate.toString());
        if (updatedDate != null) map.put("updatedDate", updatedDate.toString());
        if (lastRunDate != null) map.put("lastRunDate", lastRunDate.toString());
        if (nextRunDate != null) map.put("nextRunDate", nextRunDate.toString());
        
        map.put("totalRuns", totalRuns);
        map.put("totalRunTime", totalRunTime);
        map.put("totalRuntimeMinutes", getTotalRuntimeMinutes());
        map.put("totalWaterUsage", totalWaterUsage);
        map.put("totalWaterSavings", totalWaterSavings);
        map.put("averageRunTime", averageRunTime);
        map.put("averageRuntimeMinutes", getAverageRuntimeMinutes());
        map.put("averageWaterUsage", averageWaterUsage);
        map.put("successRate", successRate);
        map.put("skipRate", skipRate);
        map.put("weatherSkipRate", weatherSkipRate);
        
        if (startTime != null) map.put("startTime", startTime.toString());
        if (startDate != null) map.put("startDate", startDate.toString());
        if (endDate != null) map.put("endDate", endDate.toString());
        map.put("duration", duration);
        map.put("durationMinutes", getDurationMinutes());
        map.put("totalDuration", totalDuration);
        map.put("totalDurationMinutes", getTotalDurationMinutes());
        
        map.put("zoneCount", getZoneCount());
        map.put("enabledZoneCount", getEnabledZoneCount());
        map.put("totalZones", totalZones);
        
        map.put("flexSchedule", isFlexSchedule());
        map.put("smartCycle", usesSmartCycle());
        map.put("cycleSoak", usesCycleSoak());
        map.put("weatherIntelligence", usesWeatherIntelligence());
        map.put("waterBudget", hasWaterBudget());
        if (waterBudget != null) map.put("waterBudgetPercentage", waterBudget);
        
        return map;
    }
    
    /**
     * Check if schedule should run today
     */
    public boolean shouldRunToday() {
        if (!isActive() || !isEnabled()) {
            return false;
        }
        
        // Check if today is a watering day
        if (wateringDays != null) {
            DayOfWeek today = java.time.LocalDate.now().getDayOfWeek();
            return wateringDays.contains(today);
        }
        
        // Check specific days
        if (specificDays != null) {
            Instant today = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
            for (Instant day : specificDays) {
                if (day.equals(today)) {
                    return true;
                }
            }
        }
        
        // Check excluded dates
        if (excludedDates != null) {
            Instant today = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
            for (Instant excluded : excludedDates) {
                if (excluded.equals(today)) {
                    return false;
                }
            }
        }
        
        // Default to frequency check
        if (frequency != null && frequency.type != null) {
            // Simplified frequency check
            // In a real implementation, this would be more complex
            return true;
        }
        
        return false;
    }
    
    @Override
    public String toString() {
        return String.format("RachioSchedule{id=%s, name=%s, type=%s, status=%s, enabled=%s, zones=%d}",
                id, name, type, status, enabled, getZoneCount());
    }
}
