package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * DTO for Rachio Device
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDevice {
    @Nullable
    public String id;
    
    @Nullable
    public String name;
    
    @Nullable
    public String model;
    
    @Nullable
    public String status; // ONLINE, OFFLINE, SLEEP
    
    @Nullable
    public Boolean online;
    
    @Nullable
    public Long rainDelay; // seconds remaining
    
    @Nullable
    public Long rainDelayExpiration; // epoch seconds
    
    @Nullable
    public String scheduleMode; // MANUAL, FLEX, FIXED
    
    @Nullable
    public String serialNumber;
    
    @Nullable
    public String macAddress;
    
    @Nullable
    public Instant createdDate;
    
    @Nullable
    public Instant updatedDate;
    
    @Nullable
    public Double latitude;
    
    @Nullable
    public Double longitude;
    
    @Nullable
    public String timeZone;
    
    @Nullable
    public Boolean deleted;
    
    @Nullable
    public List<RachioZone> zones;
    
    @Nullable
    public List<Schedule> schedules;
    
    // Professional monitoring fields
    @Nullable
    public Double waterBudget;
    
    @Nullable
    public Instant lastWatered;
    
    @Nullable
    public Double totalWaterUsage; // gallons
    
    @Nullable
    public Double waterSavings; // gallons
    
    @Override
    public String toString() {
        return "RachioDevice [id=" + id + ", name=" + name + ", model=" + model + ", status=" + status + 
               ", online=" + online + ", zones=" + (zones != null ? zones.size() : 0) + "]";
    }
    
    // Nested Schedule class
    @NonNullByDefault
    public static class Schedule {
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
        
        @Override
        public String toString() {
            return "Schedule [id=" + id + ", name=" + name + ", type=" + type + ", enabled=" + enabled + "]";
        }
    }
}
