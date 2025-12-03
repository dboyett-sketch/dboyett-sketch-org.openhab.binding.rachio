package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link RachioEventString} class provides string representations of Rachio events
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioEventString {

    public static String getEventString(RachioEvent event) {
        if (event == null) {
            return "Null event";
        }

        StringBuilder sb = new StringBuilder();
        
        // FIXED: Using public fields directly
        if (event.timestamp != null) {
            sb.append("Time: ").append(event.timestamp).append(" ");
        }
        
        if (event.summary != null) {
            sb.append("Summary: ").append(event.summary).append(" ");
        }
        
        if (event.topic != null) {
            sb.append("Topic: ").append(event.topic).append(" ");
        }
        
        if (event.type != null) {
            sb.append("Type: ").append(event.type).append(" ");
        }
        
        if (event.subType != null) {
            sb.append("SubType: ").append(event.subType).append(" ");
        }

        return sb.toString().trim();
    }

    public static String getZoneEventString(RachioEvent event) {
        if (event == null) {
            return "Null zone event";
        }

        StringBuilder sb = new StringBuilder();
        
        // FIXED: Using public fields directly
        if (event.timestamp != null) {
            sb.append("Time: ").append(event.timestamp).append(" ");
        }
        
        if (event.summary != null) {
            sb.append("Summary: ").append(event.summary).append(" ");
        }
        
        if (event.type != null) {
            sb.append("Type: ").append(event.type).append(" ");
        }
        
        if (event.subType != null) {
            sb.append("SubType: ").append(event.subType).append(" ");
        }
        
        if (event.zoneName != null) {
            sb.append("Zone: ").append(event.zoneName).append(" ");
        }
        
        if (event.zoneNumber != null) {
            sb.append("Zone #: ").append(event.zoneNumber).append(" ");
        }
        
        if (event.zoneRunState != null) {
            sb.append("State: ").append(event.zoneRunState).append(" ");
        }
        
        if (event.zoneRunStatus != null) {
            sb.append("Status: ").append(event.zoneRunStatus).append(" ");
        }
        
        // FIXED: Removed invalid references to zoneRunStatus subfields
        // These were causing "cannot find symbol" errors:
        // scheduleType = event.zoneRunStatus.scheduleType;
        // startTime = event.zoneRunStatus.startTime;  
        // endTime = event.zoneRunStatus.endTime;
        
        if (event.duration != null) {
            sb.append("Duration: ").append(event.duration).append("s ");
        }

        return sb.toString().trim();
    }

    public static String getEventType(RachioEvent event) {
        // FIXED: Using public field directly
        return event.type != null ? event.type : "UNKNOWN";
    }
}