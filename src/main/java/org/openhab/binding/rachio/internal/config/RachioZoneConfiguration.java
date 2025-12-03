package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

/**
 * The {@link RachioZoneConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class RachioZoneConfiguration {
    public String deviceId = "";
    public String zoneId = "";
    public int duration = DEFAULT_DURATION;
    public int refresh = DEFAULT_POLLING_INTERVAL;

    /**
     * Validate configuration
     */
    public boolean isValid() {
        return deviceId != null && !deviceId.trim().isEmpty() && 
               zoneId != null && !zoneId.trim().isEmpty();
    }

    /**
     * Get configuration summary for logging
     */
    public String getConfigSummary() {
        return String.format("RachioZoneConfiguration{deviceId='%s', zoneId='%s', duration=%d, refresh=%d}", 
                           deviceId, zoneId, duration, refresh);
    }

    @Override
    public String toString() {
        return getConfigSummary();
    }
}