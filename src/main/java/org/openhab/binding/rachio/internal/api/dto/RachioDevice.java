package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO for Rachio Device from Rachio API
 * Based on: https://rachio.readme.io/reference/getting-started
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDevice {
    // Device identification
    public String id;
    public String name;
    public String status; // ONLINE, OFFLINE, SLEEP_MODE
    
    // Device information
    @SerializedName("serialNumber")
    public String serialNumber;
    
    @SerializedName("model")
    public String model;
    
    @SerializedName("macAddress")
    public @Nullable String macAddress;
    
    @SerializedName("latitude")
    public double latitude;
    
    @SerializedName("longitude")
    public double longitude;
    
    @SerializedName("timeZone")
    public String timeZone;
    
    @SerializedName("zip")
    public String zipCode;
    
    // Device capabilities
    @SerializedName("on")
    public boolean poweredOn;
    
    @SerializedName("paused")
    public boolean paused;
    
    @SerializedName("deleted")
    public boolean deleted;
    
    // Device status
    @SerializedName("rainDelayExpirationDate")
    public @Nullable Instant rainDelayExpirationDate;
    
    @SerializedName("rainDelayStartDate")
    public @Nullable Instant rainDelayStartDate;
    
    @SerializedName("rainDelay")
    public int rainDelay; // seconds remaining
    
    @SerializedName("lastWateredDate")
    public @Nullable Instant lastWateredDate;
    
    @SerializedName("lastRainDelayStartDate")
    public @Nullable Instant lastRainDelayStartDate;
    
    @SerializedName("lastConnectivity")
    public @Nullable Instant lastConnectivity;
    
    // Device settings
    @SerializedName("createdDate")
    public Instant createdDate;
    
    @SerializedName("updatedDate")
    public Instant updatedDate;
    
    @SerializedName("homeKitCompatible")
    public boolean homeKitCompatible;
    
    @SerializedName("touchSleepActive")
    public boolean touchSleepActive;
    
    @SerializedName("touchSleepDelay")
    public int touchSleepDelay;
    
    @SerializedName("touchSleepDelayEnabled")
    public boolean touchSleepDelayEnabled;
    
    // Network information
    @SerializedName("network")
    public @Nullable NetworkInfo network;
    
    @SerializedName("scheduleModeType")
    public String scheduleModeType; // MANUAL, AUTO
    
    @SerializedName("scheduleModeModifiedDate")
    public @Nullable Instant scheduleModeModifiedDate;
    
    // Weather intelligence
    @SerializedName("weatherIntelligenceOn")
    public boolean weatherIntelligenceOn;
    
    @SerializedName("wateringAdjustmentRuntimes")
    public int[] wateringAdjustmentRuntimes = new int[0];
    
    // Device components
    @SerializedName("zones")
    public @Nullable List<RachioZone> zones;
    
    @SerializedName("schedules")
    public @Nullable List<ScheduleSummary> schedules;
    
    // Rachio 3 specific features
    @SerializedName("generation")
    public int generation; // 1, 2, or 3
    
    @SerializedName("flexScheduleRules")
    public @Nullable Object flexScheduleRules;
    
    @SerializedName("seasonalAdjustment")
    public int seasonalAdjustment; // -100 to 100
    
    @SerializedName("cycleSoak")
    public boolean cycleSoakEnabled;
    
    @SerializedName("cycleSoakDuration")
    public int cycleSoakDuration;
    
    @SerializedName("cycleSoakSoakDuration")
    public int cycleSoakSoakDuration;
    
    @SerializedName("etSkipMultiplier")
    public double etSkipMultiplier;
    
    // Nested classes
    
    public static class NetworkInfo {
        @SerializedName("ssid")
        public String ssid;
        
        @SerializedName("strength")
        public int signalStrength;
        
        @SerializedName("online")
        public boolean online;
    }
    
    public static class ScheduleSummary {
        @SerializedName("id")
        public String id;
        
        @SerializedName("name")
        public String name;
        
        @SerializedName("enabled")
        public boolean enabled;
        
        @SerializedName("scheduleType")
        public String scheduleType; // "FIXED", "FLEX", "FLEX_DAILY"
        
        @SerializedName("startDate")
        public @Nullable String startDate;
        
        @SerializedName("endDate")
        public @Nullable String endDate;
        
        @SerializedName("totalDuration")
        public int totalDuration;
    }
    
    // Helper methods
    
    /**
     * Check if device is online
     */
    public boolean isOnline() {
        return "ONLINE".equals(status);
    }
    
    /**
     * Check if device is offline
     */
    public boolean isOffline() {
        return "OFFLINE".equals(status);
    }
    
    /**
     * Check if device is in sleep mode
     */
    public boolean isSleepMode() {
        return "SLEEP_MODE".equals(status);
    }
    
    /**
     * Check if rain delay is active
     */
    public boolean isRainDelayActive() {
        return rainDelay > 0;
    }
    
    /**
     * Get rain delay remaining in hours
     */
    public double getRainDelayRemainingHours() {
        return rainDelay / 3600.0;
    }
    
    /**
     * Check if device is a Rachio 3
     */
    public boolean isRachio3() {
        return generation == 3;
    }
    
    /**
     * Get zone count
     */
    public int getZoneCount() {
        return zones != null ? zones.size() : 0;
    }
    
    /**
     * Get schedule count
     */
    public int getScheduleCount() {
        return schedules != null ? schedules.size() : 0;
    }
    
    /**
     * Get enabled zone count
     */
    public int getEnabledZoneCount() {
        if (zones == null) return 0;
        
        int count = 0;
        for (RachioZone zone : zones) {
            if (zone.enabled) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Get zone by ID
     */
    public @Nullable RachioZone getZone(String zoneId) {
        if (zones == null) return null;
        
        for (RachioZone zone : zones) {
            if (zoneId.equals(zone.id)) {
                return zone;
            }
        }
        return null;
    }
    
    /**
     * Get zone by number
     */
    public @Nullable RachioZone getZone(int zoneNumber) {
        if (zones == null) return null;
        
        for (RachioZone zone : zones) {
            if (zone.zoneNumber == zoneNumber) {
                return zone;
            }
        }
        return null;
    }
    
    /**
     * Get all enabled zones
     */
    public @Nullable List<RachioZone> getEnabledZones() {
        if (zones == null) return null;
        
        return zones.stream()
                .filter(zone -> zone.enabled)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Check if weather intelligence is enabled
     */
    public boolean isWeatherIntelligenceEnabled() {
        return weatherIntelligenceOn;
    }
    
    /**
     * Check if cycle and soak is enabled
     */
    public boolean isCycleSoakEnabled() {
        return cycleSoakEnabled;
    }
    
    /**
     * Get seasonal adjustment percentage
     */
    public int getSeasonalAdjustment() {
        return seasonalAdjustment;
    }
    
    /**
     * Get device summary for logging
     */
    public String getSummary() {
        return String.format("Device %s (%s): %s, %d zones, %s", 
                name, model, status, 
                getZoneCount(), 
                isOnline() ? "Online" : "Offline");
    }
    
    /**
     * Get detailed device info
     */
    public Map<String, Object> getInfo() {
        Map<String, Object> info = new java.util.HashMap<>();
        info.put("id", id);
        info.put("name", name);
        info.put("model", model);
        info.put("status", status);
        info.put("serial", serialNumber);
        info.put("zones", getZoneCount());
        info.put("enabledZones", getEnabledZoneCount());
        info.put("online", isOnline());
        info.put("rainDelayActive", isRainDelayActive());
        info.put("rainDelayHours", getRainDelayRemainingHours());
        info.put("weatherIntelligence", isWeatherIntelligenceEnabled());
        info.put("cycleSoak", isCycleSoakEnabled());
        info.put("seasonalAdjustment", getSeasonalAdjustment());
        info.put("generation", generation);
        info.put("latitude", latitude);
        info.put("longitude", longitude);
        info.put("timeZone", timeZone);
        
        return info;
    }
}
