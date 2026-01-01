package org.openhab.binding.rachio.internal.config;

import org.openhab.core.config.core.Configuration;

/**
 * Configuration class for Rachio Device Thing
 * 
 * @author dboyett-sketch
 */
public class RachioDeviceConfiguration {

    // Required Configuration
    public String deviceId;

    // Polling Configuration
    public Integer pollingInterval = 60; // seconds, default 1 minute
    public Boolean enableStatusUpdates = true;

    // Display Configuration
    public String customName;
    public Boolean showImages = true;
    public Integer imageRefreshInterval = 300; // seconds, default 5 minutes

    // Watering Configuration
    public Integer defaultWateringDuration = 300; // seconds, default 5 minutes
    public Boolean allowManualOverride = true;
    public Boolean respectRainDelay = true;

    // Notification Configuration
    public Boolean notifyOnWateringStart = false;
    public Boolean notifyOnWateringStop = false;
    public Boolean notifyOnRainDelay = true;
    public Boolean notifyOnDeviceOffline = true;

    // Advanced Configuration
    public Boolean autoRefreshOnCommand = true;
    public Boolean cacheDeviceData = true;
    public Integer cacheTTL = 300; // seconds, default 5 minutes

    /**
     * Default constructor
     */
    public RachioDeviceConfiguration() {
        // Default values already set above
    }

    /**
     * Validate configuration
     */
    public boolean isValid() {
        // Device ID is required
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return false;
        }

        // Validate device ID format (Rachio device IDs are typically UUIDs)
        if (!deviceId.matches("[a-fA-F0-9\\-]+")) {
            return false;
        }

        // Validate polling interval
        if (pollingInterval != null && (pollingInterval < 10 || pollingInterval > 3600)) {
            return false; // 10 seconds to 1 hour
        }

        // Validate image refresh interval
        if (imageRefreshInterval != null && (imageRefreshInterval < 60 || imageRefreshInterval > 86400)) {
            return false; // 1 minute to 24 hours
        }

        // Validate default watering duration
        if (defaultWateringDuration != null && (defaultWateringDuration < 30 || defaultWateringDuration > 14400)) {
            return false; // 30 seconds to 4 hours
        }

        // Validate cache TTL
        if (cacheTTL != null && (cacheTTL < 10 || cacheTTL > 3600)) {
            return false; // 10 seconds to 1 hour
        }

        return true;
    }

    /**
     * Get validation errors
     */
    public String getValidationErrors() {
        StringBuilder errors = new StringBuilder();

        if (deviceId == null || deviceId.trim().isEmpty()) {
            errors.append("Device ID is required. ");
        } else if (!deviceId.matches("[a-fA-F0-9\\-]+")) {
            errors.append("Device ID must be a valid identifier (letters, numbers, hyphens). ");
        }

        if (pollingInterval != null && (pollingInterval < 10 || pollingInterval > 3600)) {
            errors.append("Polling interval must be between 10 and 3600 seconds. ");
        }

        if (imageRefreshInterval != null && (imageRefreshInterval < 60 || imageRefreshInterval > 86400)) {
            errors.append("Image refresh interval must be between 60 and 86400 seconds. ");
        }

        if (defaultWateringDuration != null && (defaultWateringDuration < 30 || defaultWateringDuration > 14400)) {
            errors.append("Default watering duration must be between 30 and 14400 seconds. ");
        }

        if (cacheTTL != null && (cacheTTL < 10 || cacheTTL > 3600)) {
            errors.append("Cache TTL must be between 10 and 3600 seconds. ");
        }

        return errors.toString().trim();
    }

    /**
     * Get display name (custom name or device ID)
     */
    public String getDisplayName() {
        if (customName != null && !customName.trim().isEmpty()) {
            return customName.trim();
        }
        return deviceId;
    }

    /**
     * Check if images should be shown
     */
    public boolean shouldShowImages() {
        return showImages != null ? showImages : true;
    }

    /**
     * Check if status updates are enabled
     */
    public boolean isStatusUpdatesEnabled() {
        return enableStatusUpdates != null ? enableStatusUpdates : true;
    }

    /**
     * Get effective polling interval
     */
    public int getEffectivePollingInterval() {
        if (pollingInterval != null && pollingInterval >= 10) {
            return pollingInterval;
        }
        return 60; // Default 1 minute
    }

    /**
     * Get effective image refresh interval
     */
    public int getEffectiveImageRefreshInterval() {
        if (imageRefreshInterval != null && imageRefreshInterval >= 60) {
            return imageRefreshInterval;
        }
        return 300; // Default 5 minutes
    }

    /**
     * Get effective default watering duration
     */
    public int getEffectiveDefaultWateringDuration() {
        if (defaultWateringDuration != null && defaultWateringDuration >= 30) {
            return defaultWateringDuration;
        }
        return 300; // Default 5 minutes
    }

    /**
     * Get effective cache TTL
     */
    public int getEffectiveCacheTTL() {
        if (cacheTTL != null && cacheTTL >= 10) {
            return cacheTTL;
        }
        return 300; // Default 5 minutes
    }

    /**
     * Check if auto-refresh is enabled
     */
    public boolean isAutoRefreshEnabled() {
        return autoRefreshOnCommand != null ? autoRefreshOnCommand : true;
    }

    /**
     * Check if caching is enabled
     */
    public boolean isCacheEnabled() {
        return cacheDeviceData != null ? cacheDeviceData : true;
    }

    /**
     * Check if manual override is allowed
     */
    public boolean isManualOverrideAllowed() {
        return allowManualOverride != null ? allowManualOverride : true;
    }

    /**
     * Check if rain delay should be respected
     */
    public boolean shouldRespectRainDelay() {
        return respectRainDelay != null ? respectRainDelay : true;
    }

    /**
     * Update configuration from OpenHAB Configuration object
     */
    public void updateFromConfiguration(Configuration config) {
        if (config == null) {
            return;
        }

        deviceId = (String) config.get("deviceId");
        customName = (String) config.get("customName");

        // Polling Configuration
        Object pollObj = config.get("pollingInterval");
        if (pollObj instanceof Number) {
            pollingInterval = ((Number) pollObj).intValue();
        } else if (pollObj instanceof String) {
            try {
                pollingInterval = Integer.parseInt((String) pollObj);
            } catch (NumberFormatException e) {
                // Keep default
            }
        }

        Object statusObj = config.get("enableStatusUpdates");
        if (statusObj instanceof Boolean) {
            enableStatusUpdates = (Boolean) statusObj;
        } else if (statusObj instanceof String) {
            enableStatusUpdates = Boolean.parseBoolean((String) statusObj);
        }

        // Display Configuration
        Object showImagesObj = config.get("showImages");
        if (showImagesObj instanceof Boolean) {
            showImages = (Boolean) showImagesObj;
        } else if (showImagesObj instanceof String) {
            showImages = Boolean.parseBoolean((String) showImagesObj);
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

        // Watering Configuration
        Object durationObj = config.get("defaultWateringDuration");
        if (durationObj instanceof Number) {
            defaultWateringDuration = ((Number) durationObj).intValue();
        } else if (durationObj instanceof String) {
            try {
                defaultWateringDuration = Integer.parseInt((String) durationObj);
            } catch (NumberFormatException e) {
                // Keep default
            }
        }

        Object manualObj = config.get("allowManualOverride");
        if (manualObj instanceof Boolean) {
            allowManualOverride = (Boolean) manualObj;
        } else if (manualObj instanceof String) {
            allowManualOverride = Boolean.parseBoolean((String) manualObj);
        }

        Object rainDelayObj = config.get("respectRainDelay");
        if (rainDelayObj instanceof Boolean) {
            respectRainDelay = (Boolean) rainDelayObj;
        } else if (rainDelayObj instanceof String) {
            respectRainDelay = Boolean.parseBoolean((String) rainDelayObj);
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

        Object notifyRainObj = config.get("notifyOnRainDelay");
        if (notifyRainObj instanceof Boolean) {
            notifyOnRainDelay = (Boolean) notifyRainObj;
        } else if (notifyRainObj instanceof String) {
            notifyOnRainDelay = Boolean.parseBoolean((String) notifyRainObj);
        }

        Object notifyOfflineObj = config.get("notifyOnDeviceOffline");
        if (notifyOfflineObj instanceof Boolean) {
            notifyOnDeviceOffline = (Boolean) notifyOfflineObj;
        } else if (notifyOfflineObj instanceof String) {
            notifyOnDeviceOffline = Boolean.parseBoolean((String) notifyOfflineObj);
        }

        // Advanced Configuration
        Object autoRefreshObj = config.get("autoRefreshOnCommand");
        if (autoRefreshObj instanceof Boolean) {
            autoRefreshOnCommand = (Boolean) autoRefreshObj;
        } else if (autoRefreshObj instanceof String) {
            autoRefreshOnCommand = Boolean.parseBoolean((String) autoRefreshObj);
        }

        Object cacheObj = config.get("cacheDeviceData");
        if (cacheObj instanceof Boolean) {
            cacheDeviceData = (Boolean) cacheObj;
        } else if (cacheObj instanceof String) {
            cacheDeviceData = Boolean.parseBoolean((String) cacheObj);
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

        config.put("deviceId", deviceId);
        config.put("customName", customName);
        config.put("pollingInterval", pollingInterval);
        config.put("enableStatusUpdates", enableStatusUpdates);
        config.put("showImages", showImages);
        config.put("imageRefreshInterval", imageRefreshInterval);
        config.put("defaultWateringDuration", defaultWateringDuration);
        config.put("allowManualOverride", allowManualOverride);
        config.put("respectRainDelay", respectRainDelay);
        config.put("notifyOnWateringStart", notifyOnWateringStart);
        config.put("notifyOnWateringStop", notifyOnWateringStop);
        config.put("notifyOnRainDelay", notifyOnRainDelay);
        config.put("notifyOnDeviceOffline", notifyOnDeviceOffline);
        config.put("autoRefreshOnCommand", autoRefreshOnCommand);
        config.put("cacheDeviceData", cacheDeviceData);
        config.put("cacheTTL", cacheTTL);

        return config;
    }

    @Override
    public String toString() {
        return "RachioDeviceConfiguration{" + "deviceId='" + deviceId + '\'' + ", customName='" + customName + '\''
                + ", pollingInterval=" + pollingInterval + ", enableStatusUpdates=" + enableStatusUpdates
                + ", showImages=" + showImages + ", imageRefreshInterval=" + imageRefreshInterval
                + ", defaultWateringDuration=" + defaultWateringDuration + ", allowManualOverride="
                + allowManualOverride + ", respectRainDelay=" + respectRainDelay + ", autoRefreshOnCommand="
                + autoRefreshOnCommand + ", cacheDeviceData=" + cacheDeviceData + ", cacheTTL=" + cacheTTL + '}';
    }
}
