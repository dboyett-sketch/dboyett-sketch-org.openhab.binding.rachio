package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Configuration class for Rachio Device
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDeviceConfiguration {

    // Required configuration
    public @Nullable String deviceId;
    
    // Optional configuration
    public @Nullable String customName;
    public int pollInterval = 120; // 2 minutes in seconds
    public boolean enableZoneDiscovery = true;
    public boolean enableStatusMonitoring = true;
    public boolean enableEventHistory = true;
    public int eventHistoryHours = 24;
    
    // Advanced configuration
    public boolean enableWaterUsageTracking = false;
    public boolean enableSavingsTracking = false;
    public boolean enableForecastIntegration = false;
    public boolean enableAlertMonitoring = false;
    
    // Device-specific overrides
    public int maxZoneRuntimeOverride = 0; // 0 = use default
    public int defaultRainDelayOverride = 0; // 0 = use default
    public boolean forceOnlineStatus = false;
    
    // Validation Methods
    
    /**
     * Validate the configuration
     * @return true if configuration is valid
     */
    public boolean isValid() {
        return deviceId != null && !deviceId.trim().isEmpty();
    }
    
    /**
     * Get device ID with null safety
     */
    public String getDeviceId() {
        String id = deviceId;
        return id != null ? id.trim() : "";
    }
    
    /**
     * Get custom name with null safety
     */
    public String getCustomName() {
        String name = customName;
        return name != null ? name.trim() : "";
    }
    
    /**
     * Get display name (custom name if set, otherwise device ID)
     */
    public String getDisplayName() {
        String custom = getCustomName();
        if (!custom.isEmpty()) {
            return custom;
        }
        String id = getDeviceId();
        return !id.isEmpty() ? id : "Unknown Device";
    }
    
    /**
     * Validate poll interval is within bounds
     */
    public void validatePollInterval() {
        if (pollInterval < 30) {
            pollInterval = 30; // Minimum 30 seconds
        }
        if (pollInterval > 1800) {
            pollInterval = 1800; // Maximum 30 minutes for device
        }
    }
    
    /**
     * Validate event history hours
     */
    public void validateEventHistory() {
        if (eventHistoryHours < 1) {
            eventHistoryHours = 1; // Minimum 1 hour
        }
        if (eventHistoryHours > 168) {
            eventHistoryHours = 168; // Maximum 1 week
        }
    }
    
    /**
     * Check if zone discovery is enabled
     */
    public boolean isZoneDiscoveryEnabled() {
        return enableZoneDiscovery;
    }
    
    /**
     * Check if status monitoring is enabled
     */
    public boolean isStatusMonitoringEnabled() {
        return enableStatusMonitoring;
    }
    
    /**
     * Check if event history is enabled
     */
    public boolean isEventHistoryEnabled() {
        return enableEventHistory;
    }
    
    /**
     * Check if water usage tracking is enabled
     */
    public boolean isWaterUsageTrackingEnabled() {
        return enableWaterUsageTracking;
    }
    
    /**
     * Check if savings tracking is enabled
     */
    public boolean isSavingsTrackingEnabled() {
        return enableSavingsTracking;
    }
    
    /**
     * Check if forecast integration is enabled
     */
    public boolean isForecastIntegrationEnabled() {
        return enableForecastIntegration;
    }
    
    /**
     * Check if alert monitoring is enabled
     */
    public boolean isAlertMonitoringEnabled() {
        return enableAlertMonitoring;
    }
    
    /**
     * Get max zone runtime (override or 0 for default)
     */
    public int getMaxZoneRuntime() {
        return maxZoneRuntimeOverride > 0 ? maxZoneRuntimeOverride : 0;
    }
    
    /**
     * Get default rain delay (override or 0 for default)
     */
    public int getDefaultRainDelay() {
        return defaultRainDelayOverride >= 0 ? defaultRainDelayOverride : -1;
    }
    
    /**
     * Check if forced online status is enabled
     */
    public boolean isForceOnlineStatusEnabled() {
        return forceOnlineStatus;
    }
    
    /**
     * Get configuration as string for logging
     */
    @Override
    public String toString() {
        return "RachioDeviceConfiguration{" +
                "deviceId='" + getDeviceId() + '\'' +
                ", customName='" + getCustomName() + '\'' +
                ", pollInterval=" + pollInterval +
                ", enableZoneDiscovery=" + enableZoneDiscovery +
                ", enableStatusMonitoring=" + enableStatusMonitoring +
                ", enableEventHistory=" + enableEventHistory +
                ", eventHistoryHours=" + eventHistoryHours +
                ", enableWaterUsageTracking=" + enableWaterUsageTracking +
                ", enableSavingsTracking=" + enableSavingsTracking +
                ", enableForecastIntegration=" + enableForecastIntegration +
                ", enableAlertMonitoring=" + enableAlertMonitoring +
                ", maxZoneRuntimeOverride=" + maxZoneRuntimeOverride +
                ", defaultRainDelayOverride=" + defaultRainDelayOverride +
                ", forceOnlineStatus=" + forceOnlineStatus +
                '}';
    }
    
    /**
     * Get configuration summary for status display
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Device ID: ").append(getDeviceId());
        if (!getCustomName().isEmpty()) {
            sb.append(" (").append(getCustomName()).append(")");
        }
        sb.append(", Polling: ").append(pollInterval).append("s");
        sb.append(", Zones: ").append(enableZoneDiscovery ? "Auto-discover" : "Manual");
        sb.append(", Events: ").append(enableEventHistory ? eventHistoryHours + "h" : "Disabled");
        return sb.toString();
    }
}
