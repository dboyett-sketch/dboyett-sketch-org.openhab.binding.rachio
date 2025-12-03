package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

/**
 * The {@link RachioDeviceConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class RachioDeviceConfiguration {
    public String deviceId = "";
    public int zoneDuration = DEFAULT_DURATION;
    public int refresh = DEFAULT_POLLING_INTERVAL;

    /**
     * Validate configuration
     */
    public boolean isValid() {
        return deviceId != null && !deviceId.trim().isEmpty();
    }

    /**
     * Get configuration summary for logging
     */
    public String getConfigSummary() {
        return String.format("RachioDeviceConfiguration{deviceId='%s', zoneDuration=%d, refresh=%d}", 
                           deviceId, zoneDuration, refresh);
    }

    @Override
    public String toString() {
        return getConfigSummary();
    }
}