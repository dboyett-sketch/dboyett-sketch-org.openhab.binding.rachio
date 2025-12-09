package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO for Schedule Management
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioSchedule {
    @Nullable
    public String id;
    
    @Nullable
    public String name;
    
    @Nullable
    public Boolean enabled;
    
    @Nullable
    public String type; // FLEX_MONTHLY, FLEX_WEEKLY, FIXED
    
    @Nullable
    public Instant startDate;
    
    @Nullable
    public Integer totalDuration; // seconds
    
    @Nullable
    public List<ScheduleZone> zones;
    
    @Nullable
    public Map<String, Object> flexSettings; // flex schedule specific settings
    
    @Nullable
    public Map<String, Object> weatherIntelligence; // weather adjustment settings
    
    @Override
    public String toString() {
        return "RachioSchedule [id=" + id + ", name=" + name + ", type=" + type + ", enabled=" + enabled + "]";
    }
    
    @NonNullByDefault
    public static class ScheduleZone {
        @Nullable
        public String zoneId;
        
        @Nullable
        public Integer duration; // seconds
        
        @Nullable
        public Integer order;
        
        @Override
        public String toString() {
            return "ScheduleZone [zoneId=" + zoneId + ", duration=" + duration + "]";
        }
    }
}
