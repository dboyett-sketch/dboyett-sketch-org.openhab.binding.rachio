package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

@NonNullByDefault
public class RachioSchedule {
    public String id;
    public String name;
    public boolean enabled;
    public String scheduleType; // "FIXED", "FLEX", "FLEX_DAILY"
    
    @SerializedName("zones")
    public List<ScheduleZone> zones;
    
    @SerializedName("flexScheduleRules")
    public @Nullable FlexScheduleRules flexScheduleRules;
    
    @SerializedName("fixedScheduleRules")
    public @Nullable FixedScheduleRules fixedScheduleRules;
    
    @SerializedName("startDate")
    public @Nullable String startDate;
    
    @SerializedName("endDate")
    public @Nullable String endDate;
    
    @SerializedName("totalDuration")
    public int totalDuration;
    
    @SerializedName("cycleSoak")
    public boolean cycleSoak;
    
    @SerializedName("cycleSoakDuration")
    public int cycleSoakDuration;
    
    @SerializedName("cycleSoakSoakDuration")
    public int cycleSoakSoakDuration;
    
    public static class ScheduleZone {
        public String zoneId;
        public int duration;
        public int zoneNumber;
    }
    
    public static class FlexScheduleRules {
        public String operator;
        public int runtime;
        public List<FlexScheduleRule> rules;
    }
    
    public static class FlexScheduleRule {
        public String type; // "WATERING_DEPTH", "SOIL_DRY_OUT"
        public int value;
    }
    
    public static class FixedScheduleRules {
        public List<FixedScheduleRule> rules;
    }
    
    public static class FixedScheduleRule {
        public int startHour;
        public int startMinute;
        public List<Integer> daysOfWeek;
        public boolean cycleSoak;
    }
}
