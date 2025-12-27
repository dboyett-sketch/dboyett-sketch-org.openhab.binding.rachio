package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for Device Alerts
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioAlert {
    @Nullable
    public String id;

    @Nullable
    public String type; // OFFLINE, ONLINE, RAIN_DELAY, SCHEDULE_SKIP, etc.

    @Nullable
    public String severity; // INFO, WARNING, ERROR

    @Nullable
    public String message;

    @Nullable
    public Instant timestamp;

    @Nullable
    public Boolean acknowledged;

    @Nullable
    public Map<String, Object> details;

    @Override
    public String toString() {
        return "RachioAlert [type=" + type + ", severity=" + severity + ", message=" + message + "]";
    }
}
