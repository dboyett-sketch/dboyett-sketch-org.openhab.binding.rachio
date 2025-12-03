package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.rachio.internal.api.dto.ZoneRunStatus;

/**
 * Rachio Event class for webhook event processing
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioEvent {
    public String timestamp;
    public String summary;
    public String topic;
    public String type;
    public String subType;
    public String zoneName;
    public Integer zoneNumber;
    public String zoneRunState;
    public ZoneRunStatus zoneRunStatus;
    public Integer duration;
    
    public RachioEvent() {
        // Default constructor
    }
    
    // Getters and setters
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }
    
    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    
    public Integer getZoneNumber() { return zoneNumber; }
    public void setZoneNumber(Integer zoneNumber) { this.zoneNumber = zoneNumber; }
    
    public String getZoneRunState() { return zoneRunState; }
    public void setZoneRunState(String zoneRunState) { this.zoneRunState = zoneRunState; }
    
    public ZoneRunStatus getZoneRunStatus() { return zoneRunStatus; }
    public void setZoneRunStatus(ZoneRunStatus zoneRunStatus) { this.zoneRunStatus = zoneRunStatus; }
    
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
}