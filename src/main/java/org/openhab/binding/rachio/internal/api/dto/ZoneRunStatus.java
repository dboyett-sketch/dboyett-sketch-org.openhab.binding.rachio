package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Zone Run Status DTO
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class ZoneRunStatus {
    public String scheduleType;
    public Long startTime;
    public Long endTime;
    public Integer duration;
    public String zoneId;
    public String zoneName;
    public Integer zoneNumber;
    
    public ZoneRunStatus() {
        // Default constructor
    }
    
    // Getters and setters
    public String getScheduleType() { return scheduleType; }
    public void setScheduleType(String scheduleType) { this.scheduleType = scheduleType; }
    
    public Long getStartTime() { return startTime; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }
    
    public Long getEndTime() { return endTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }
    
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    
    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
    
    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    
    public Integer getZoneNumber() { return zoneNumber; }
    public void setZoneNumber(Integer zoneNumber) { this.zoneNumber = zoneNumber; }
}