package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for Rachio Webhook Event
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioWebHookEvent {
    @Nullable
    public String eventType; // ZONE_STATUS, DEVICE_STATUS, etc.
    
    @Nullable
    public String deviceId;
    
    @Nullable
    public String zoneId;
    
    @Nullable
    public Instant timestamp;
    
    @Nullable
    public String summary;
    
    @Nullable
    public Map<String, Object> data; // Event-specific data
    
    @Override
    public String toString() {
        return "RachioWebHookEvent [eventType=" + eventType + ", deviceId=" + deviceId + 
               ", zoneId=" + zoneId + ", timestamp=" + timestamp + "]";
    }
}
