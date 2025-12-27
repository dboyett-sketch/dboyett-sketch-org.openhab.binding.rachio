package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for Rachio Event Summary
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioEventSummary {
    @Nullable
    public String id;

    @Nullable
    public String deviceId;

    @Nullable
    public String zoneId;

    @Nullable
    public String eventType;

    @Nullable
    public Instant timestamp;

    @Nullable
    public String summary;

    @Nullable
    public Map<String, Object> details;

    @Override
    public String toString() {
        return "RachioEventSummary [eventType=" + eventType + ", deviceId=" + deviceId + ", timestamp=" + timestamp
                + "]";
    }
}
