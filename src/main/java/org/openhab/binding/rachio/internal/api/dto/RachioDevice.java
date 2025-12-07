package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO representing a Rachio device (controller)
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDevice {
    
    // Device identification
    @SerializedName("id")
    private String id = "";
    
    @SerializedName("name")
    private String name = "";
    
    @SerializedName("model")
    private String model = "";
    
    @SerializedName("serialNumber")
    private String serialNumber = "";
    
    @SerializedName("macAddress")
    private String macAddress = "";
    
    // Status fields
    @SerializedName("status")
    private String status = "";
    
    @SerializedName("online")
    private boolean online = false;
    
    @SerializedName("weatherIntelligenceEnabled")
    private boolean weatherIntelligenceEnabled = false;
    
    @SerializedName("scheduleModeType")
    private String scheduleModeType = "";
    
    @SerializedName("zones")
    private List<RachioZone> zones = List.of();
    
    @SerializedName("scheduleRules")
    private List<Object> scheduleRules = List.of();
    
    @SerializedName("flexScheduleRules")
    private List<Object> flexScheduleRules = List.of();
    
    // Timestamps
    @SerializedName("createdDate")
    private Instant createdDate;
    
    @SerializedName("lastHeardFromDate")
    private Instant lastHeardFromDate;
    
    @SerializedName("lastWateredDate")
    private Instant lastWateredDate;
    
    @SerializedName("lastRainDelayStartDate")
    private Instant lastRainDelayStartDate;
    
    @SerializedName("lastRainDelayEndDate")
    private Instant lastRainDelayEndDate;
    
    // Rain delay
    @SerializedName("rainDelayExpirationDate")
    private Instant rainDelayExpirationDate;
    
    @SerializedName("rainDelayStartDate")
    private Instant rainDelayStartDate;
    
    @SerializedName("rainDelayEndDate")
    private Instant rainDelayEndDate;
    
    // Current conditions
    @SerializedName("rainDelayCounter")
    private int rainDelayCounter = 0;
    
    @SerializedName("paused")
    private boolean paused = false;
    
    @SerializedName("pausedUntilDate")
    private Instant pausedUntilDate;
    
    // Location
    @SerializedName("latitude")
    private double latitude = 0.0;
    
    @SerializedName("longitude")
    private double longitude = 0.0;
    
    @SerializedName("timeZone")
    private String timeZone = "";
    
    @SerializedName("elevation")
    private double elevation = 0.0;
    
    @SerializedName("zip")
    private String zip = "";
    
    // Hardware info
    @SerializedName("hardwareVersion")
    private String hardwareVersion = "";
    
    @SerializedName("firmwareVersion")
    private String firmwareVersion = "";
    
    @SerializedName("softwareVersion")
    private String softwareVersion = "";
    
    // Advanced features
    @SerializedName("onboarding")
    private boolean onboarding = false;
    
    @SerializedName("touchEnabled")
    private boolean touchEnabled = false;
    
    @SerializedName("touchStandaloneEnabled")
    private boolean touchStandaloneEnabled = false;
    
    @SerializedName("touchRainDelayEnabled")
    private boolean touchRainDelayEnabled = false;
    
    @SerializedName("touchAdvancedEnabled")
    private boolean touchAdvancedEnabled = false;
    
    @SerializedName("masterValve")
    private boolean masterValve = false;
    
    @SerializedName("masterValveZoneId")
    private String masterValveZoneId = "";
    
    // Water budget
    @SerializedName("waterBudget")
    private double waterBudget = 100.0;
    
    @SerializedName("fixedWaterBudget")
    private boolean fixedWaterBudget = false;
    
    // Custom images
    @SerializedName("customImageUrl")
    private String customImageUrl = "";
    
    @SerializedName("customImageEnabled")
    private boolean customImageEnabled = false;
    
    // Additional properties (catch-all for any extra fields)
    @SerializedName("properties")
    private Map<String, Object> properties = Map.of();
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getSerialNumber() {
        return serialNumber;
    }
    
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
    
    public String getMacAddress() {
        return macAddress;
    }
    
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public boolean isOnline() {
        return online;
    }
    
    public void setOnline(boolean online) {
        this.online = online;
    }
    
    public boolean isWeatherIntelligenceEnabled() {
        return weatherIntelligenceEnabled;
    }
    
    public void setWeatherIntelligenceEnabled(boolean weatherIntelligenceEnabled) {
        this.weatherIntelligenceEnabled = weatherIntelligenceEnabled;
    }
    
    public String getScheduleModeType() {
        return scheduleModeType;
    }
    
    public void setScheduleModeType(String scheduleModeType) {
        this.scheduleModeType = scheduleModeType;
    }
    
    public List<RachioZone> getZones() {
        return zones;
    }
    
    public void setZones(List<RachioZone> zones) {
        this.zones = zones;
    }
    
    public List<Object> getScheduleRules() {
        return scheduleRules;
    }
    
    public void setScheduleRules(List<Object> scheduleRules) {
        this.scheduleRules = scheduleRules;
    }
    
    public List<Object> getFlexScheduleRules() {
        return flexScheduleRules;
    }
    
    public void setFlexScheduleRules(List<Object> flexScheduleRules) {
        this.flexScheduleRules = flexScheduleRules;
    }
    
    public @Nullable Instant getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }
    
    public @Nullable Instant getLastHeardFromDate() {
        return lastHeardFromDate;
    }
    
    public void setLastHeardFromDate(Instant lastHeardFromDate) {
        this.lastHeardFromDate = lastHeardFromDate;
    }
    
    public @Nullable Instant getLastWateredDate() {
        return lastWateredDate;
    }
    
    public void setLastWateredDate(Instant lastWateredDate) {
        this.lastWateredDate = lastWateredDate;
    }
    
    public @Nullable Instant getLastRainDelayStartDate() {
        return lastRainDelayStartDate;
    }
    
    public void setLastRainDelayStartDate(Instant lastRainDelayStartDate) {
        this.lastRainDelayStartDate = lastRainDelayStartDate;
    }
    
    public @Nullable Instant getLastRainDelayEndDate() {
        return lastRainDelayEndDate;
    }
    
    public void setLastRainDelayEndDate(Instant lastRainDelayEndDate) {
        this.lastRainDelayEndDate = lastRainDelayEndDate;
    }
    
    public @Nullable Instant getRainDelayExpirationDate() {
        return rainDelayExpirationDate;
    }
    
    public void setRainDelayExpirationDate(Instant rainDelayExpirationDate) {
        this.rainDelayExpirationDate = rainDelayExpirationDate;
    }
    
    public @Nullable Instant getRainDelayStartDate() {
        return rainDelayStartDate;
    }
    
    public void setRainDelayStartDate(Instant rainDelayStartDate) {
        this.rainDelayStartDate = rainDelayStartDate;
    }
    
    public @Nullable Instant getRainDelayEndDate() {
        return rainDelayEndDate;
    }
    
    public void setRainDelayEndDate(Instant rainDelayEndDate) {
        this.rainDelayEndDate = rainDelayEndDate;
    }
    
    public int getRainDelayCounter() {
        return rainDelayCounter;
    }
    
    public void setRainDelayCounter(int rainDelayCounter) {
        this.rainDelayCounter = rainDelayCounter;
    }
    
    public boolean isPaused() {
        return paused;
    }
    
    public void setPaused(boolean paused) {
        this.paused = paused;
    }
    
    public @Nullable Instant getPausedUntilDate() {
        return pausedUntilDate;
    }
    
    public void setPausedUntilDate(Instant pausedUntilDate) {
        this.pausedUntilDate = pausedUntilDate;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public String getTimeZone() {
        return timeZone;
    }
    
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
    
    public double getElevation() {
        return elevation;
    }
    
    public void setElevation(double elevation) {
        this.elevation = elevation;
    }
    
    public String getZip() {
        return zip;
    }
    
    public void setZip(String zip) {
        this.zip = zip;
    }
    
    public String getHardwareVersion() {
        return hardwareVersion;
    }
    
    public void setHardwareVersion(String hardwareVersion) {
        this.hardwareVersion = hardwareVersion;
    }
    
    public String getFirmwareVersion() {
        return firmwareVersion;
    }
    
    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }
    
    public String getSoftwareVersion() {
        return softwareVersion;
    }
    
    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }
    
    public boolean isOnboarding() {
        return onboarding;
    }
    
    public void setOnboarding(boolean onboarding) {
        this.onboarding = onboarding;
    }
    
    public boolean isTouchEnabled() {
        return touchEnabled;
    }
    
    public void setTouchEnabled(boolean touchEnabled) {
        this.touchEnabled = touchEnabled;
    }
    
    public boolean isTouchStandaloneEnabled() {
        return touchStandaloneEnabled;
    }
    
    public void setTouchStandaloneEnabled(boolean touchStandaloneEnabled) {
        this.touchStandaloneEnabled = touchStandaloneEnabled;
    }
    
    public boolean isTouchRainDelayEnabled() {
        return touchRainDelayEnabled;
    }
    
    public void setTouchRainDelayEnabled(boolean touchRainDelayEnabled) {
        this.touchRainDelayEnabled = touchRainDelayEnabled;
    }
    
    public boolean isTouchAdvancedEnabled() {
        return touchAdvancedEnabled;
    }
    
    public void setTouchAdvancedEnabled(boolean touchAdvancedEnabled) {
        this.touchAdvancedEnabled = touchAdvancedEnabled;
    }
    
    public boolean hasMasterValve() {
        return masterValve;
    }
    
    public void setMasterValve(boolean masterValve) {
        this.masterValve = masterValve;
    }
    
    public String getMasterValveZoneId() {
        return masterValveZoneId;
    }
    
    public void setMasterValveZoneId(String masterValveZoneId) {
        this.masterValveZoneId = masterValveZoneId;
    }
    
    public double getWaterBudget() {
        return waterBudget;
    }
    
    public void setWaterBudget(double waterBudget) {
        this.waterBudget = waterBudget;
    }
    
    public boolean isFixedWaterBudget() {
        return fixedWaterBudget;
    }
    
    public void setFixedWaterBudget(boolean fixedWaterBudget) {
        this.fixedWaterBudget = fixedWaterBudget;
    }
    
    public String getCustomImageUrl() {
        return customImageUrl;
    }
    
    public void setCustomImageUrl(String customImageUrl) {
        this.customImageUrl = customImageUrl;
    }
    
    public boolean isCustomImageEnabled() {
        return customImageEnabled;
    }
    
    public void setCustomImageEnabled(boolean customImageEnabled) {
        this.customImageEnabled = customImageEnabled;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
    
    /**
     * Get a specific zone by ID
     */
    public @Nullable RachioZone getZoneById(String zoneId) {
        for (RachioZone zone : zones) {
            if (zone.getId().equals(zoneId)) {
                return zone;
            }
        }
        return null;
    }
    
    /**
     * Get a specific zone by zone number
     */
    public @Nullable RachioZone getZoneByNumber(int zoneNumber) {
        for (RachioZone zone : zones) {
            if (zone.getZoneNumber() == zoneNumber) {
                return zone;
            }
        }
        return null;
    }
    
    /**
     * Check if device has any active zones
     */
    public boolean hasActiveZones() {
        for (RachioZone zone : zones) {
            if (zone.isRunning()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get the number of enabled zones
     */
    public int getEnabledZoneCount() {
        int count = 0;
        for (RachioZone zone : zones) {
            if (zone.isEnabled()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Get total runtime for all enabled zones (in seconds)
     */
    public int getTotalZoneRuntime() {
        int total = 0;
        for (RachioZone zone : zones) {
            if (zone.isEnabled()) {
                total += zone.getRuntime();
            }
        }
        return total;
    }
    
    /**
     * Check if device is in rain delay
     */
    public boolean isInRainDelay() {
        return rainDelayCounter > 0 || rainDelayExpirationDate != null;
    }
    
    /**
     * Get device status as OpenHAB ThingStatus
     */
    public org.openhab.core.thing.ThingStatus getThingStatus() {
        if (online) {
            return org.openhab.core.thing.ThingStatus.ONLINE;
        } else {
            return org.openhab.core.thing.ThingStatus.OFFLINE;
        }
    }
    
    /**
     * Get device status detail
     */
    public org.openhab.core.thing.ThingStatusDetail getThingStatusDetail() {
        if (!online) {
            return org.openhab.core.thing.ThingStatusDetail.COMMUNICATION_ERROR;
        } else if (paused) {
            return org.openhab.core.thing.ThingStatusDetail.PAUSED;
        } else if (isInRainDelay()) {
            return org.openhab.core.thing.ThingStatusDetail.RAIN_DELAY;
        } else {
            return org.openhab.core.thing.ThingStatusDetail.NONE;
        }
    }
    
    @Override
    public String toString() {
        return String.format("RachioDevice[id=%s, name=%s, model=%s, online=%s, zones=%d]", 
            id, name, model, online, zones.size());
    }
}
