package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for Rachio Event (alternative structure)
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioEvent {
    @Nullable
    public String id;
    
    @Nullable
    public String type;
    
    @Nullable
    public String deviceId;
    
    @Nullable
    public String zoneId;
    
    @Nullable
    public Instant timestamp;
    
    @Nullable
    public Map<String, Object> properties;
    
    @Nullable
    public String severity; // INFO, WARNING, ERROR
    
    @Nullable
    public String category; // IRRIGATION, SYSTEM, WEATHER
    
    @Override
    public String toString() {
        return "RachioEvent [type=" + type + ", deviceId=" + deviceId + ", zoneId=" + zoneId + 
               ", category=" + category + "]";
    }
}
