package org.openhab.binding.rachio.internal.config;

import org.openhab.core.config.core.Configuration;

/**
 * Configuration class for Rachio Zone Thing
 * 
 * @author dboyett-sketch
 */
public class RachioZoneConfiguration {

    // Required Configuration
    public String zoneId;
    public String deviceId;

    // Polling Configuration
    public Integer statusPollingInterval = 30; // seconds, default 30 seconds
    public Boolean enableWateringStatus = true;

    // Watering Configuration
    public Integer defaultDuration = 300; // seconds, default 5 minutes
    public Integer maxDuration = 3600; // seconds, default 1 hour
    public Boolean allowQuickRun = true;

    // Display Configuration
    public String customName;
    public Boolean showZoneImage = true;
    public Integer imageRefreshInterval = 600; // seconds, default 10 minutes

    // Behavior Configuration
    public Boolean autoEnableOnStart = false;
    public Boolean autoDisableOnError = true;
    public Integer cooldownPeriod = 60; // seconds between watering starts

    // Notification Configuration
    public Boolean notifyOnWateringStart = false;
    public Boolean notifyOnWateringStop = false;
    public Boolean notifyOnZoneEnabled = false;
    public Boolean notifyOnZoneDisabled = true;

    // Advanced Configuration
    public Boolean overrideDeviceSettings = false;
    public Integer customRuntime = null; // Override zone runtime if set
    public Boolean cacheZoneData = true;
    public Integer cacheTTL = 180; // seconds, default 3 minutes

    /**
     * Default constructor
     */
    public RachioZoneConfiguration() {
        // Default values already set above
    }

    /**
     * Validate configuration
     */
    public boolean isValid() {
        // Zone ID and Device ID are required
        if (zoneId == null || zoneId.trim().isEmpty()) {
            return false;
        }

        if (deviceId == null || deviceId.trim().isEmpty()) {
            return false;
        }

        // Validate IDs format (Rachio IDs are typically UUIDs)
        if (!zoneId.matches("[a-fA-F0-9\\-]+")) {
            return false;
        }

        if (!deviceId.matches("[a-fA-F0-9\\-]+")) {
            return false;
        }

        // Validate polling interval
        if (statusPollingInterval != null && (statusPollingInterval < 5 || statusPollingInterval > 300)) {
            return false; // 5 seconds to 5 minutes
        }

        // Validate durations
        if (defaultDuration != null && (defaultDuration < 30 || defaultDuration > 14400)) {
            return false; // 30 seconds to 4 hours
        }

        if (maxDuration != null && (maxDuration < 60 || maxDuration > 28800)) {
            return false; // 1 minute to 8 hours
        }

        // Validate max >= default
        if (defaultDuration != null && maxDuration != null && defaultDuration > maxDuration) {
            return false;
        }

        // Validate image refresh interval
        if (imageRefreshInterval != null && (imageRefreshInterval < 60 || imageRefreshInterval > 86400)) {
            return false; // 1 minute to 24 hours
        }

        // Validate cooldown period
        if (cooldownPeriod != null && (cooldownPeriod < 0 || cooldownPeriod > 3600)) {
            return false; // 0 to 1 hour
        }

        // Validate cache TTL
        if (cacheTTL != null && (cacheTTL < 10 || cacheTTL > 3600)) {
            return false; // 10 seconds to 1 hour
        }

        // Validate custom runtime if set
        if (customRuntime != null && (customRuntime < 30 || customRuntime > 14400)) {
            return false; // 30 seconds to 4 hours
        }

        return true;
    }

    /**
     * Get validation errors
     */
    public String getValidationErrors() {
        StringBuilder errors = new StringBuilder();

        if (zoneId == null || zoneId.trim().isEmpty()) {
            errors.append("Zone ID is required. ");
        } else if (!zoneId.matches("[a-fA-F0-9\\-]+")) {
            errors.append("Zone ID must be a valid identifier (letters, numbers, hyphens). ");
        }

        if (deviceId == null || deviceId.trim().isEmpty()) {
            errors.append("Device ID is required. ");
        } else if (!deviceId.matches("[a-fA-F0-9\\-]+")) {
            errors.append("Device ID must be a valid identifier (letters, numbers, hyphens). ");
        }

        if (statusPollingInterval != null && (statusPollingInterval < 5 || statusPollingInterval > 300)) {
            errors.append("Status polling interval must be between 5 and 300 seconds. ");
        }

        if (defaultDuration != null && (defaultDuration < 30 || defaultDuration > 14400)) {
            errors.append("Default duration must be between 30 and 14400 seconds. ");
        }

        if (maxDuration != null && (maxDuration < 60 || maxDuration > 28800)) {
            errors.append("Max duration must be between 60 and 28800 seconds. ");
        }

        if (defaultDuration != null && maxDuration != null && defaultDuration > maxDuration) {
            errors.append("Default duration cannot exceed max duration. ");
        }

        if (imageRefreshInterval != null && (imageRefreshInterval < 60 || imageRefreshInterval > 86400)) {
            errors.append("Image refresh interval must be between 60 and 86400 seconds. ");
        }

        if (cooldownPeriod != null && (cooldownPeriod < 0 || cooldownPeriod > 3600)) {
            errors.append("Cooldown period must be between 0 and 3600 seconds. ");
        }

        if (cacheTTL != null && (cacheTTL < 10 || cacheTTL > 3600)) {
            errors.append("Cache TTL must be between 10 and 3600 seconds. ");
        }

        if (customRuntime != null && (customRuntime < 30 || customRuntime > 14400)) {
            errors.append("Custom runtime must be between 30 and 14400 seconds. ");
        }

        return errors.toString().trim();
    }

    /**
     * Get display name (custom name or zone ID)
     */
    public String getDisplayName() {
        if (customName != null && !customName.trim().isEmpty()) {
            return customName.trim();
        }
        return "Zone " + zoneId;
    }

    /**
     * Check if watering status updates are enabled
     */
    public boolean isWateringStatusEnabled() {
        return enableWateringStatus != null ? enableWateringStatus : true;
    }

    /**
     * Check if zone images should be shown
     */
    public boolean shouldShowZoneImage() {
        return showZoneImage != null ? showZoneImage : true;
    }

    /**
     * Check if quick run is allowed
     */
    public boolean isQuickRunAllowed() {
        return allowQuickRun != null ? allowQuickRun : true;
    }

    /**
     * Check if auto-enable on start is enabled
     */
    public boolean isAutoEnableOnStart() {
        return autoEnableOnStart != null ? autoEnableOnStart : false;
    }

    /**
     * Check if auto-disable on error is enabled
     */
    public boolean isAutoDisableOnError() {
        return autoDisableOnError != null ? autoDisableOnError : true;
    }

    /**
     * Check if device settings should be overridden
     */
    public boolean shouldOverrideDeviceSettings() {
        return overrideDeviceSettings != null ? overrideDeviceSettings : false;
    }

    /**
     * Get effective status polling interval
     */
    public int getEffectiveStatusPollingInterval() {
        if (statusPollingInterval != null && statusPollingInterval >= 5) {
            return statusPollingInterval;
        }
        return 30; // Default 30 seconds
    }

    /**
     * Get effective default duration
     */
    public int getEffectiveDefaultDuration() {
        if (defaultDuration != null && defaultDuration >= 30) {
            return defaultDuration;
        }
        return 300; // Default 5 minutes
    }

    /**
     * Get effective max duration
     */
    public int getEffectiveMaxDuration() {
        if (maxDuration != null && maxDuration >= 60) {
            return maxDuration;
        }
        return 3600; // Default 1 hour
    }

    /**
     * Get effective image refresh interval
     */
    public int getEffectiveImageRefreshInterval() {
        if (imageRefreshInterval != null && imageRefreshInterval >= 60) {
            return imageRefreshInterval;
        }
        return 600; // Default 10 minutes
    }

    /**
     * Get effective cooldown period
     */
    public int getEffectiveCooldownPeriod() {
        if (cooldownPeriod != null && cooldownPeriod >= 0) {
            return cooldownPeriod;
        }
        return 60; // Default 60 seconds
    }

    /**
     * Get effective cache TTL
     */
    public int getEffectiveCacheTTL() {
        if (cacheTTL != null && cacheTTL >= 10) {
            return cacheTTL;
        }
        return 180; // Default 3 minutes
    }

    /**
     * Get effective runtime (custom override or default)
     */
    public Integer getEffectiveRuntime() {
        if (customRuntime != null && customRuntime >= 30) {
            return customRuntime;
        }
        return null; // Use zone's default runtime
    }

    /**
     * Check if caching is enabled
     */
    public boolean isCacheEnabled() {
        return cacheZoneData != null ? cacheZoneData : true;
    }

    /**
     * Update configuration from OpenHAB Configuration object
     */
    public void updateFromConfiguration(Configuration config) {
        if (config == null) {
            return;
        }

        zoneId = (String) config.get("zoneId");
        deviceId = (String) config.get("deviceId");
        customName = (String) config.get("customName");

        // Polling Configuration
        Object pollObj = config.get("statusPollingInterval");
        if (pollObj instanceof Number) {
            statusPollingInterval = ((Number) pollObj).intValue();
        } else if (pollObj instanceof String) {
            try {
                statusPollingInterval = Integer.parseInt((String) pollObj);
            } catch (NumberFormatException e) {
                // Keep default
            }
        }

        Object statusObj = config.get("enableWateringStatus");
        if (statusObj instanceof Boolean) {
            enableWateringStatus = (Boolean) statusObj;
        } else if (statusObj instanceof String) {
            enableWateringStatus = Boolean.parseBoolean((String) statusObj);
        }

        // Watering Configuration
        Object durationObj = config.get("defaultDuration");
        if (durationObj instanceof Number) {
            defaultDuration = ((Number) durationObj).intValue();
        } else if (durationObj instanceof String) {
            try {
                defaultDuration = Integer.parseInt((String) durationObj);
            } catch (NumberFormatException e) {
                // Keep default
            }
        }

        Object maxObj = config.get("maxDuration");
        if (maxObj instanceof Number) {
            maxDuration = ((Number) maxObj).intValue();
        } else if (maxObj instanceof String) {
            try {
                maxDuration = Integer.parseInt((String) maxObj);
            } catch (NumberFormatException e) {
                // Keep default
            }
        }

        Object quickRunObj = config.get("allowQuickRun");
        if (quickRunObj instanceof Boolean) {
            allowQuickRun = (Boolean) quickRunObj;
        } else if (quickRunObj instanceof String) {
            allowQuickRun = Boolean.parseBoolean((String) quickRunObj);
        }

        // Display Configuration
        Object imageObj = config.get("showZoneImage");
        if (imageObj instanceof Boolean) {
            showZoneImage = (Boolean) imageObj;
        } else if (imageObj instanceof String) {
            showZoneImage = Boolean.parseBoolean((String) imageObj);
        }

        Object imageRefreshObj = config.get("imageRefreshInterval");
        if (imageRefreshObj instanceof Number) {
            imageRefreshInterval = ((Number) imageRefreshObj).intValue();
        } else if (imageRefreshObj instanceof String) {
            try {
                imageRefreshInterval = Integer.parseInt((String) imageRefreshObj);
            } catch (NumberFormatException e) {
                // Keep default
            }
        }

        // Behavior Configuration
        Object autoEnableObj = config.get("autoEnableOnStart");
        if (autoEnableObj instanceof Boolean) {
            autoEnableOnStart = (Boolean) autoEnableObj;
        } else if (autoEnableObj instanceof String) {
            autoEnableOnStart = Boolean.parseBoolean((String) autoEnableObj);
        }

        Object autoDisableObj = config.get("autoDisableOnError");
        if (autoDisableObj instanceof Boolean) {
            autoDisableOnError = (Boolean) autoDisableObj;
        } else if (autoDisableObj instanceof String) {
            autoDisableOnError = Boolean.parseBoolean((String) autoDisableObj);
        }

        Object cooldownObj = config.get("cooldownPeriod");
        if (cooldownObj instanceof Number) {
            cooldownPeriod = ((Number) cooldownObj).intValue();
        } else if (cooldownObj instanceof String) {
            try {
                cooldownPeriod = Integer.parseInt((String) cooldownObj);
            } catch (NumberFormatException e) {
                // Keep default
            }
        }

        // Notification Configuration
        Object notifyStartObj = config.get("notifyOnWateringStart");
        if (notifyStartObj instanceof Boolean) {
            notifyOnWateringStart = (Boolean) notifyStartObj;
        } else if (notifyStartObj instanceof String) {
            notifyOnWateringStart = Boolean.parseBoolean((String) notifyStartObj);
        }

        Object notifyStopObj = config.get("notifyOnWateringStop");
        if (notifyStopObj instanceof Boolean) {
            notifyOnWateringStop = (Boolean) notifyStopObj;
        } else if (notifyStopObj instanceof String) {
            notifyOnWateringStop = Boolean.parseBoolean((String) notifyStopObj);
        }

        Object notifyEnableObj = config.get("notifyOnZoneEnabled");
        if (notifyEnableObj instanceof Boolean) {
            notifyOnZoneEnabled = (Boolean) notifyEnableObj;
        } else if (notifyEnableObj instanceof String) {
            notifyOnZoneEnabled = Boolean.parseBoolean((String) notifyEnableObj);
        }

        Object notifyDisableObj = config.get("notifyOnZoneDisabled");
        if (notifyDisableObj instanceof Boolean) {
            notifyOnZoneDisabled = (Boolean) notifyDisableObj;
        } else if (notifyDisableObj instanceof String) {
            notifyOnZoneDisabled = Boolean.parseBoolean((String) notifyDisableObj);
        }

        // Advanced Configuration
        Object overrideObj = config.get("overrideDeviceSettings");
        if (overrideObj instanceof Boolean) {
            overrideDeviceSettings = (Boolean) overrideObj;
        } else if (overrideObj instanceof String) {
            overrideDeviceSettings = Boolean.parseBoolean((String) overrideObj);
        }

        Object runtimeObj = config.get("customRuntime");
        if (runtimeObj instanceof Number) {
            customRuntime = ((Number) runtimeObj).intValue();
        } else if (runtimeObj instanceof String) {
            try {
                customRuntime = Integer.parseInt((String) runtimeObj);
            } catch (NumberFormatException e) {
                customRuntime = null;
            }
        }

        Object cacheObj = config.get("cacheZoneData");
        if (cacheObj instanceof Boolean) {
            cacheZoneData = (Boolean) cacheObj;
        } else if (cacheObj instanceof String) {
            cacheZoneData = Boolean.parseBoolean((String) cacheObj);
        }

        Object cacheTTLObj = config.get("cacheTTL");
        if (cacheTTLObj instanceof Number) {
            cacheTTL = ((Number) cacheTTLObj).intValue();
        } else if (cacheTTLObj instanceof String) {
            try {
                cacheTTL = Integer.parseInt((String) cacheTTLObj);
            } catch (NumberFormatException e) {
                // Keep default
            }
        }
    }

    /**
     * Convert to OpenHAB Configuration object
     */
    public Configuration toConfiguration() {
        Configuration config = new Configuration();

        config.put("zoneId", zoneId);
        config.put("deviceId", deviceId);
        config.put("customName", customName);
        config.put("statusPollingInterval", statusPollingInterval);
        config.put("enableWateringStatus", enableWateringStatus);
        config.put("defaultDuration", defaultDuration);
        config.put("maxDuration", maxDuration);
        config.put("allowQuickRun", allowQuickRun);
        config.put("showZoneImage", showZoneImage);
        config.put("imageRefreshInterval", imageRefreshInterval);
        config.put("autoEnableOnStart", autoEnableOnStart);
        config.put("autoDisableOnError", autoDisableOnError);
        config.put("cooldownPeriod", cooldownPeriod);
        config.put("notifyOnWateringStart", notifyOnWateringStart);
        config.put("notifyOnWateringStop", notifyOnWateringStop);
        config.put("notifyOnZoneEnabled", notifyOnZoneEnabled);
        config.put("notifyOnZoneDisabled", notifyOnZoneDisabled);
        config.put("overrideDeviceSettings", overrideDeviceSettings);
        config.put("customRuntime", customRuntime);
        config.put("cacheZoneData", cacheZoneData);
        config.put("cacheTTL", cacheTTL);

        return config;
    }

    @Override
    public String toString() {
        return "RachioZoneConfiguration{" + "zoneId='" + zoneId + '\'' + ", deviceId='" + deviceId + '\''
                + ", customName='" + customName + '\'' + ", statusPollingInterval=" + statusPollingInterval
                + ", enableWateringStatus=" + enableWateringStatus + ", defaultDuration=" + defaultDuration
                + ", maxDuration=" + maxDuration + ", allowQuickRun=" + allowQuickRun + ", showZoneImage="
                + showZoneImage + ", imageRefreshInterval=" + imageRefreshInterval + ", autoEnableOnStart="
                + autoEnableOnStart + ", autoDisableOnError=" + autoDisableOnError + ", cooldownPeriod="
                + cooldownPeriod + ", overrideDeviceSettings=" + overrideDeviceSettings + ", customRuntime="
                + customRuntime + ", cacheZoneData=" + cacheZoneData + ", cacheTTL=" + cacheTTL + '}';
    }
}
