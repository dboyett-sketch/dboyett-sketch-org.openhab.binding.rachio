package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link RachioDeviceConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDeviceConfiguration {

    @Nullable
    public String deviceId;

    // ADDED: Getter method required to fix compilation errors
    @Nullable
    public String getDeviceId() {
        return deviceId;
    }

    @Override
    public String toString() {
        return "RachioDeviceConfiguration [deviceId=" + deviceId + "]";
    }
}
