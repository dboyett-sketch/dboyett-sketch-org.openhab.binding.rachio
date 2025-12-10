package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link RachioZoneConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZoneConfiguration {

    // Required configuration
    public String zoneId = "";

    // Optional configuration with defaults
    public @Nullable Integer defaultDuration;
    public @Nullable String zoneName;
    public @Nullable Integer zoneNumber;
    public @Nullable Boolean enabled;
    public @Nullable Integer maxRuntime;

    /**
     * Get the zone ID
     *
     * @return zone ID
     */
    public String getZoneId() {
        return zoneId;
    }

    /**
     * Set the zone ID
     *
     * @param zoneId zone ID
     */
    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    /**
     * Get the default duration in minutes
     *
     * @return default duration
     */
    public @Nullable Integer getDefaultDuration() {
        return defaultDuration;
    }

    /**
     * Set the default duration
     *
     * @param defaultDuration default duration in minutes
     */
    public void setDefaultDuration(@Nullable Integer defaultDuration) {
        this.defaultDuration = defaultDuration;
    }

    /**
     * Get the zone name
     *
     * @return zone name
     */
    public @Nullable String getZoneName() {
        return zoneName;
    }

    /**
     * Set the zone name
     *
     * @param zoneName zone name
     */
    public void setZoneName(@Nullable String zoneName) {
        this.zoneName = zoneName;
    }

    /**
     * Get the zone number
     *
     * @return zone number
     */
    public @Nullable Integer getZoneNumber() {
        return zoneNumber;
    }

    /**
     * Set the zone number
     *
     * @param zoneNumber zone number
     */
    public void setZoneNumber(@Nullable Integer zoneNumber) {
        this.zoneNumber = zoneNumber;
    }

    /**
     * Check if zone is enabled
     *
     * @return true if enabled
     */
    public @Nullable Boolean isEnabled() {
        return enabled;
    }

    /**
     * Set zone enabled status
     *
     * @param enabled enabled status
     */
    public void setEnabled(@Nullable Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get maximum runtime in minutes
     *
     * @return maximum runtime
     */
    public @Nullable Integer getMaxRuntime() {
        return maxRuntime;
    }

    /**
     * Set maximum runtime
     *
     * @param maxRuntime maximum runtime in minutes
     */
    public void setMaxRuntime(@Nullable Integer maxRuntime) {
        this.maxRuntime = maxRuntime;
    }

    /**
     * Get default duration with fallback value
     *
     * @param fallback fallback value if not set
     * @return default duration or fallback
     */
    public int getDefaultDurationWithFallback(int fallback) {
        return defaultDuration != null ? defaultDuration : fallback;
    }

    /**
     * Validate configuration
     *
     * @return true if configuration is valid
     */
    public boolean isValid() {
        return zoneId != null && !zoneId.isEmpty();
    }

    /**
     * Get string representation
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "RachioZoneConfiguration{" +
                "zoneId='" + zoneId + '\'' +
                ", defaultDuration=" + defaultDuration +
                ", zoneName='" + zoneName + '\'' +
                ", zoneNumber=" + zoneNumber +
                ", enabled=" + enabled +
                ", maxRuntime=" + maxRuntime +
                '}';
    }

    /**
     * Get configuration summary
     *
     * @return configuration summary
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Zone ID: ").append(zoneId);
        if (zoneName != null && !zoneName.isEmpty()) {
            sb.append(", Name: ").append(zoneName);
        }
        if (zoneNumber != null) {
            sb.append(", Number: ").append(zoneNumber);
        }
        if (defaultDuration != null) {
            sb.append(", Default Duration: ").append(defaultDuration).append(" min");
        }
        if (enabled != null) {
            sb.append(", Enabled: ").append(enabled);
        }
        if (maxRuntime != null) {
            sb.append(", Max Runtime: ").append(maxRuntime).append(" min");
        }
        return sb.toString();
    }
}
