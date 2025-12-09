package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import java.time.Instant;

/**
 * DTO for Zone Run Status
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class ZoneRunStatus {
    @Nullable
    public String zoneId;
    
    @Nullable
    public String status; // STARTED, STOPPED, COMPLETED
    
    @Nullable
    public Instant startTime;
    
    @Nullable
    public Instant endTime;
    
    @Nullable
    public Integer duration; // seconds
    
    @Nullable
    public Double waterUsed; // gallons
    
    @Override
    public String toString() {
        return "ZoneRunStatus [zoneId=" + zoneId + ", status=" + status + ", duration=" + duration + "]";
    }
}
