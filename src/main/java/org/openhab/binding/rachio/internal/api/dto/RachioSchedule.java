package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public class RachioSchedule {
    public @Nullable String id;
    public @Nullable String name;
    public @Nullable String type;
    public @Nullable Boolean enabled;
    public @Nullable Instant startDate;
    public @Nullable Integer totalDuration;
    public @Nullable List<ScheduleZone> zones;
    
    @NonNullByDefault
    public static class ScheduleZone {
        public @Nullable String zoneId;
        public @Nullable Integer duration;
        public @Nullable Integer order;
        
        public @Nullable String getZoneId() { return zoneId; }
        public @Nullable Integer getDuration() { return duration; }
        public @Nullable Integer getOrder() { return order; }
    }
    
    public @Nullable String getId() { return id; }
    public @Nullable String getName() { return name; }
    public @Nullable String getType() { return type; }
    public @Nullable Boolean isEnabled() { return enabled; }
    public @Nullable Instant getStartDate() { return startDate; }
    public @Nullable Integer getTotalDuration() { return totalDuration; }
    public @Nullable List<ScheduleZone> getZones() { return zones; }
}
