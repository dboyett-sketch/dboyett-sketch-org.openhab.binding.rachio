package org.openhab.binding.rachio.internal.api.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * DTO representing a Rachio irrigation schedule
 * 
 * @author dboyett-sketch
 */
public class RachioSchedule {

    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name;

    @SerializedName("type")
    public String type;

    @SerializedName("status")
    public String status;

    @SerializedName("enabled")
    public Boolean enabled;

    @SerializedName("deviceId")
    public String deviceId;

    @SerializedName("zones")
    public List<ZoneSchedule> zones;

    @SerializedName("startDate")
    public Instant startDate;

    @SerializedName("endDate")
    public Instant endDate;

    @SerializedName("startTime")
    public String startTime; // HH:MM format

    @SerializedName("duration")
    public Integer duration; // Total duration in seconds

    @SerializedName("frequency")
    public Frequency frequency;

    @SerializedName("weatherIntelligence")
    public Boolean weatherIntelligence;

    @SerializedName("cycleSoak")
    public Boolean cycleSoak;

    @SerializedName("totalDuration")
    public Integer totalDuration; // Actual duration after adjustments

    @SerializedName("estimatedWaterUsage")
    public Double estimatedWaterUsage; // Gallons

    @SerializedName("createdDate")
    public Instant createdDate;

    @SerializedName("updatedDate")
    public Instant updatedDate;

    @SerializedName("lastRunDate")
    public Instant lastRunDate;

    @SerializedName("nextRunDate")
    public Instant nextRunDate;

    /**
     * Default constructor for Gson
     */
    public RachioSchedule() {
        this.zones = new ArrayList<>();
    }

    /**
     * Constructor with basic fields
     */
    public RachioSchedule(String id, String name, String type, String deviceId) {
        this();
        this.id = id;
        this.name = name;
        this.type = type;
        this.deviceId = deviceId;
        this.enabled = true;
        this.status = "ACTIVE";
    }

    /**
     * Get schedule ID
     */
    public String getId() {
        return id;
    }

    /**
     * Get schedule name
     */
    public String getName() {
        return name;
    }

    /**
     * Get schedule type
     */
    public String getType() {
        return type;
    }

    /**
     * Check if schedule is a fixed schedule
     */
    public boolean isFixedSchedule() {
        return "FIXED".equalsIgnoreCase(type);
    }

    /**
     * Check if schedule is a flex schedule
     */
    public boolean isFlexSchedule() {
        return "FLEX".equalsIgnoreCase(type);
    }

    /**
     * Check if schedule is a quick run
     */
    public boolean isQuickRun() {
        return "QUICK_RUN".equalsIgnoreCase(type);
    }

    /**
     * Check if schedule is a manual run
     */
    public boolean isManualRun() {
        return "MANUAL".equalsIgnoreCase(type);
    }

    /**
     * Get schedule status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Check if schedule is active
     */
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    /**
     * Check if schedule is paused
     */
    public boolean isPaused() {
        return "PAUSED".equalsIgnoreCase(status);
    }

    /**
     * Check if schedule is completed
     */
    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(status);
    }

    /**
     * Check if schedule is enabled
     */
    public boolean isEnabled() {
        return enabled != null && enabled;
    }

    /**
     * Get device ID
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Get zone schedules
     */
    public List<ZoneSchedule> getZones() {
        return zones;
    }

    /**
     * Get number of zones in schedule
     */
    public int getZoneCount() {
        return zones != null ? zones.size() : 0;
    }

    /**
     * Get zone schedule by zone ID
     */
    public ZoneSchedule getZoneSchedule(String zoneId) {
        if (zoneId == null || zones == null) {
            return null;
        }

        for (ZoneSchedule zoneSchedule : zones) {
            if (zoneId.equals(zoneSchedule.zoneId)) {
                return zoneSchedule;
            }
        }
        return null;
    }

    /**
     * Get start date
     */
    public Instant getStartDate() {
        return startDate;
    }

    /**
     * Get end date (if scheduled end)
     */
    public Instant getEndDate() {
        return endDate;
    }

    /**
     * Check if schedule has end date
     */
    public boolean hasEndDate() {
        return endDate != null;
    }

    /**
     * Get start time (HH:MM format)
     */
    public String getStartTime() {
        return startTime;
    }

    /**
     * Get start hour (0-23)
     */
    public Integer getStartHour() {
        if (startTime == null || !startTime.contains(":")) {
            return null;
        }

        try {
            String[] parts = startTime.split(":");
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get start minute (0-59)
     */
    public Integer getStartMinute() {
        if (startTime == null || !startTime.contains(":")) {
            return null;
        }

        try {
            String[] parts = startTime.split(":");
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get duration in seconds
     */
    public Integer getDuration() {
        return duration;
    }

    /**
     * Get duration in minutes
     */
    public Integer getDurationMinutes() {
        return duration != null ? duration / 60 : null;
    }

    /**
     * Get frequency information
     */
    public Frequency getFrequency() {
        return frequency;
    }

    /**
     * Check if weather intelligence is enabled
     */
    public boolean hasWeatherIntelligence() {
        return weatherIntelligence != null && weatherIntelligence;
    }

    /**
     * Check if cycle soak is enabled
     */
    public boolean hasCycleSoak() {
        return cycleSoak != null && cycleSoak;
    }

    /**
     * Get total duration after adjustments
     */
    public Integer getTotalDuration() {
        return totalDuration;
    }

    /**
     * Get estimated water usage in gallons
     */
    public Double getEstimatedWaterUsage() {
        return estimatedWaterUsage;
    }

    /**
     * Get created date
     */
    public Instant getCreatedDate() {
        return createdDate;
    }

    /**
     * Get updated date
     */
    public Instant getUpdatedDate() {
        return updatedDate;
    }

    /**
     * Get last run date
     */
    public Instant getLastRunDate() {
        return lastRunDate;
    }

    /**
     * Get next run date
     */
    public Instant getNextRunDate() {
        return nextRunDate;
    }

    /**
     * Check if schedule is currently running
     */
    public boolean isRunning() {
        if (lastRunDate == null || duration == null) {
            return false;
        }

        Instant now = Instant.now();
        Instant runEnd = lastRunDate.plusSeconds(duration);

        return now.isAfter(lastRunDate) && now.isBefore(runEnd);
    }

    /**
     * Check if schedule should run today
     */
    public boolean shouldRunToday() {
        if (nextRunDate == null) {
            return false;
        }

        Instant now = Instant.now();
        Instant tomorrow = now.plusSeconds(24 * 60 * 60);

        return (nextRunDate.isAfter(now) || nextRunDate.equals(now)) && nextRunDate.isBefore(tomorrow);
    }

    /**
     * Get hours until next run
     */
    public Double getHoursUntilNextRun() {
        if (nextRunDate == null) {
            return null;
        }

        Instant now = Instant.now();
        if (now.isAfter(nextRunDate)) {
            return 0.0;
        }

        long seconds = nextRunDate.getEpochSecond() - now.getEpochSecond();
        return seconds / 3600.0;
    }

    /**
     * Check if schedule is overdue (missed run)
     */
    public boolean isOverdue() {
        if (nextRunDate == null) {
            return false;
        }

        Instant now = Instant.now();
        return now.isAfter(nextRunDate);
    }

    /**
     * Get schedule age in days
     */
    public long getAgeInDays() {
        if (createdDate == null) {
            return 0;
        }

        Instant now = Instant.now();
        long seconds = now.getEpochSecond() - createdDate.getEpochSecond();
        return seconds / (24 * 60 * 60);
    }

    /**
     * Validate schedule data
     */
    public boolean isValid() {
        return id != null && !id.isEmpty() && name != null && !name.isEmpty() && type != null && !type.isEmpty()
                && deviceId != null && !deviceId.isEmpty()
                && (isQuickRun() || (startTime != null && !startTime.isEmpty()));
    }

    /**
     * Create summary string for logging
     */
    public String toSummaryString() {
        return String.format("Schedule[id=%s, name=%s, type=%s, zones=%d, nextRun=%s]", id, name, type, getZoneCount(),
                nextRunDate != null ? nextRunDate.toString() : "none");
    }

    @Override
    public String toString() {
        return "RachioSchedule{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", type='" + type + '\''
                + ", status='" + status + '\'' + ", enabled=" + enabled + ", deviceId='" + deviceId + '\'' + ", zones="
                + getZoneCount() + ", startTime='" + startTime + '\'' + ", duration=" + duration + ", nextRunDate="
                + nextRunDate + '}';
    }

    /**
     * Zone schedule information
     */
    public static class ZoneSchedule {

        @SerializedName("zoneId")
        public String zoneId;

        @SerializedName("zoneNumber")
        public Integer zoneNumber;

        @SerializedName("duration")
        public Integer duration; // Seconds

        @SerializedName("order")
        public Integer order;

        @SerializedName("cycleSoak")
        public Boolean cycleSoak;

        @SerializedName("cycleDuration")
        public Integer cycleDuration;

        @SerializedName("soakDuration")
        public Integer soakDuration;

        /**
         * Get zone ID
         */
        public String getZoneId() {
            return zoneId;
        }

        /**
         * Get zone number
         */
        public Integer getZoneNumber() {
            return zoneNumber;
        }

        /**
         * Get duration in seconds
         */
        public Integer getDuration() {
            return duration;
        }

        /**
         * Get duration in minutes
         */
        public Integer getDurationMinutes() {
            return duration != null ? duration / 60 : null;
        }

        /**
         * Get run order
         */
        public Integer getOrder() {
            return order;
        }

        /**
         * Check if cycle soak is enabled
         */
        public boolean hasCycleSoak() {
            return cycleSoak != null && cycleSoak;
        }

        /**
         * Get cycle duration (if cycle soak)
         */
        public Integer getCycleDuration() {
            return cycleDuration;
        }

        /**
         * Get soak duration (if cycle soak)
         */
        public Integer getSoakDuration() {
            return soakDuration;
        }

        /**
         * Get number of cycles (if cycle soak)
         */
        public Integer getCycleCount() {
            if (duration == null || cycleDuration == null || cycleDuration == 0) {
                return 1;
            }

            return duration / cycleDuration;
        }

        @Override
        public String toString() {
            return "ZoneSchedule{" + "zoneId='" + zoneId + '\'' + ", zoneNumber=" + zoneNumber + ", duration="
                    + duration + ", order=" + order + ", cycleSoak=" + cycleSoak + '}';
        }
    }

    /**
     * Schedule frequency information
     */
    public static class Frequency {

        @SerializedName("type")
        public String type;

        @SerializedName("interval")
        public Integer interval; // Days

        @SerializedName("daysOfWeek")
        public List<DayOfWeek> daysOfWeek;

        @SerializedName("weekInterval")
        public Integer weekInterval;

        @SerializedName("dayOfMonth")
        public Integer dayOfMonth;

        @SerializedName("monthInterval")
        public Integer monthInterval;

        /**
         * Get frequency type
         */
        public String getType() {
            return type;
        }

        /**
         * Check if daily frequency
         */
        public boolean isDaily() {
            return "DAILY".equalsIgnoreCase(type);
        }

        /**
         * Check if weekly frequency
         */
        public boolean isWeekly() {
            return "WEEKLY".equalsIgnoreCase(type);
        }

        /**
         * Check if monthly frequency
         */
        public boolean isMonthly() {
            return "MONTHLY".equalsIgnoreCase(type);
        }

        /**
         * Check if interval frequency (every N days)
         */
        public boolean isInterval() {
            return "INTERVAL".equalsIgnoreCase(type);
        }

        /**
         * Get interval in days
         */
        public Integer getInterval() {
            return interval;
        }

        /**
         * Get days of week (for weekly schedule)
         */
        public List<DayOfWeek> getDaysOfWeek() {
            return daysOfWeek;
        }

        /**
         * Check if runs on specific day of week
         */
        public boolean runsOnDay(DayOfWeek day) {
            return daysOfWeek != null && daysOfWeek.contains(day);
        }

        /**
         * Get week interval (every N weeks)
         */
        public Integer getWeekInterval() {
            return weekInterval;
        }

        /**
         * Get day of month (for monthly schedule)
         */
        public Integer getDayOfMonth() {
            return dayOfMonth;
        }

        /**
         * Get month interval (every N months)
         */
        public Integer getMonthInterval() {
            return monthInterval;
        }

        /**
         * Get human-readable frequency description
         */
        public String getDescription() {
            if (type == null) {
                return "Unknown";
            }

            switch (type.toUpperCase()) {
                case "DAILY":
                    return "Daily";

                case "WEEKLY":
                    if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
                        StringBuilder sb = new StringBuilder("Weekly on ");
                        for (int i = 0; i < daysOfWeek.size(); i++) {
                            if (i > 0) {
                                sb.append(i == daysOfWeek.size() - 1 ? " and " : ", ");
                            }
                            sb.append(daysOfWeek.get(i).toString());
                        }
                        return sb.toString();
                    }
                    return "Weekly";

                case "MONTHLY":
                    if (dayOfMonth != null) {
                        return String.format("Monthly on day %d", dayOfMonth);
                    }
                    return "Monthly";

                case "INTERVAL":
                    if (interval != null) {
                        return String.format("Every %d days", interval);
                    }
                    return "Interval";

                default:
                    return type;
            }
        }

        @Override
        public String toString() {
            return "Frequency{" + "type='" + type + '\'' + ", interval=" + interval + ", daysOfWeek=" + daysOfWeek
                    + ", description='" + getDescription() + '\'' + '}';
        }
    }
}
