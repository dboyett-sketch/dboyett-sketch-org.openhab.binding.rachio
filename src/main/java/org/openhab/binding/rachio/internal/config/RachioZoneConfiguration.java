package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link RachioZoneConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZoneConfiguration {

    @Nullable
    public String zoneId;
    
    @Nullable
    public String deviceId;
    
    @Nullable
    public Integer defaultRuntime;

    @Override
    public String toString() {
        return "RachioZoneConfiguration [zoneId=" + zoneId 
            + ", deviceId=" + deviceId 
            + ", defaultRuntime=" + defaultRuntime + "]";
    }
}
