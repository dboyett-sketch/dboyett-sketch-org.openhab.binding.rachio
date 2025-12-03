package org.openhab.binding.rachio.internal.api.dto;

import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents device event summary from Rachio API
 */
@NonNullByDefault
public class RachioEventSummary {
    public String deviceId = "";
    public String status = "";
    @Nullable
    public List<ZoneSummary> zoneData;
    @Nullable
    public List<Event> events;
    
    public static class ZoneSummary {
        public String zoneId = "";
        public String name = "";
        public boolean enabled;
        public int runtime;
        public String imageUrl = "";
    }
    
    public static class Event {
        public String id = "";
        public String type = "";
        public String timestamp = "";
        public String summary = "";
        @Nullable
        public EventDevice device;
        @Nullable
        public EventZone zone;
    }
    
    public static class EventDevice {
        public String id = "";
        public String name = "";
        public boolean on;
    }
    
    public static class EventZone {
        public String id = "";
        public String name = "";
        public int zoneNumber;
        public int duration;
    }
}