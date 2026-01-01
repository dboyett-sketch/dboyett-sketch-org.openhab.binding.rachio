package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO representing weather forecast data from Rachio
 * 
 * @author dboyett-sketch
 */
public class RachioForecast {

    @SerializedName("deviceId")
    public String deviceId;

    @SerializedName("latitude")
    public Double latitude;

    @SerializedName("longitude")
    public Double longitude;

    @SerializedName("timeZone")
    public String timeZone;

    @SerializedName("units")
    public String units;

    @SerializedName("current")
    public CurrentConditions current;

    @SerializedName("hourly")
    public List<HourlyForecast> hourly;

    @SerializedName("daily")
    public List<DailyForecast> daily;

    @SerializedName("forecastDays")
    public List<DailyForecast> forecastDays = new ArrayList<>();

    @SerializedName("alerts")
    public List<WeatherAlert> alerts;

    @SerializedName("generatedAt")
    public Instant generatedAt;

    @SerializedName("expiresAt")
    public Instant expiresAt;

    /**
     * Default constructor for Gson
     */
    public RachioForecast() {
        this.hourly = new ArrayList<>();
        this.daily = new ArrayList<>();
        this.alerts = new ArrayList<>();
    }

    /**
     * Constructor with device ID
     */
    public RachioForecast(String deviceId) {
        this();
        this.deviceId = deviceId;
    }

    /**
     * Get device ID
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Get latitude
     */
    public Double getLatitude() {
        return latitude;
    }

    /**
     * Get longitude
     */
    public Double getLongitude() {
        return longitude;
    }

    /**
     * Check if coordinates are available
     */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    /**
     * Get coordinates as string
     */
    public String getCoordinatesString() {
        if (hasCoordinates()) {
            return String.format("%.6f, %.6f", latitude, longitude);
        }
        return null;
    }

    /**
     * Get timezone
     */
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * Get units (imperial/metric)
     */
    public String getUnits() {
        return units;
    }

    /**
     * Check if using imperial units
     */
    public boolean isImperial() {
        return "imperial".equalsIgnoreCase(units);
    }

    /**
     * Check if using metric units
     */
    public boolean isMetric() {
        return "metric".equalsIgnoreCase(units);
    }

    /**
     * Get current conditions
     */
    public CurrentConditions getCurrent() {
        return current;
    }

    /**
     * Get hourly forecasts
     */
    public List<HourlyForecast> getHourly() {
        return hourly;
    }

    /**
     * Get number of hourly forecasts
     */
    public int getHourlyCount() {
        return hourly != null ? hourly.size() : 0;
    }

    /**
     * Get hourly forecast for specific hour offset (0 = current hour)
     */
    public HourlyForecast getHourlyForecast(int hoursFromNow) {
        if (hourly == null || hoursFromNow < 0 || hoursFromNow >= hourly.size()) {
            return null;
        }
        return hourly.get(hoursFromNow);
    }

    /**
     * Get daily forecasts
     */
    public List<DailyForecast> getDaily() {
        return daily;
    }

    /**
     * Get number of daily forecasts
     */
    public int getDailyCount() {
        return daily != null ? daily.size() : 0;
    }

    /**
     * Get forecast for today (index 0)
     */
    public DailyForecast getToday() {
        return getDailyForecast(0);
    }

    /**
     * Get forecast for tomorrow (index 1)
     */
    public DailyForecast getTomorrow() {
        return getDailyForecast(1);
    }

    /**
     * Get daily forecast for specific day offset (0 = today)
     */
    public DailyForecast getDailyForecast(int daysFromNow) {
        if (daily == null || daysFromNow < 0 || daysFromNow >= daily.size()) {
            return null;
        }
        return daily.get(daysFromNow);
    }

    /**
     * Get weather alerts
     */
    public List<WeatherAlert> getAlerts() {
        return alerts;
    }

    /**
     * Get number of alerts
     */
    public int getAlertCount() {
        return alerts != null ? alerts.size() : 0;
    }

    /**
     * Check if there are any weather alerts
     */
    public boolean hasAlerts() {
        return getAlertCount() > 0;
    }

    /**
     * Get active alerts (not expired)
     */
    public List<WeatherAlert> getActiveAlerts() {
        List<WeatherAlert> active = new ArrayList<>();
        if (alerts != null) {
            for (WeatherAlert alert : alerts) {
                if (alert.isActive()) {
                    active.add(alert);
                }
            }
        }
        return active;
    }

    /**
     * Get generation timestamp
     */
    public Instant getGeneratedAt() {
        return generatedAt;
    }

    /**
     * Get expiration timestamp
     */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /**
     * Check if forecast is still valid (not expired)
     */
    public boolean isValid() {
        if (expiresAt == null) {
            return true; // No expiration means always valid
        }

        Instant now = Instant.now();
        return now.isBefore(expiresAt);
    }

    /**
     * Get hours until expiration
     */
    public double getHoursUntilExpiration() {
        if (expiresAt == null) {
            return Double.MAX_VALUE;
        }

        Instant now = Instant.now();
        if (now.isAfter(expiresAt)) {
            return 0;
        }

        long seconds = expiresAt.getEpochSecond() - now.getEpochSecond();
        return seconds / 3600.0;
    }

    /**
     * Get forecast age in minutes
     */
    public long getAgeInMinutes() {
        if (generatedAt == null) {
            return Long.MAX_VALUE;
        }

        Instant now = Instant.now();
        long seconds = now.getEpochSecond() - generatedAt.getEpochSecond();
        return seconds / 60;
    }

    /**
     * Check if forecast is fresh (less than 30 minutes old)
     */
    public boolean isFresh() {
        return getAgeInMinutes() < 30;
    }

    /**
     * Get precipitation probability for next 24 hours
     */
    public Double get24HourPrecipitationProbability() {
        if (hourly == null || hourly.isEmpty()) {
            return null;
        }

        double maxProbability = 0;
        int hoursToCheck = Math.min(24, hourly.size());

        for (int i = 0; i < hoursToCheck; i++) {
            HourlyForecast hour = hourly.get(i);
            if (hour != null && hour.precipProbability != null) {
                maxProbability = Math.max(maxProbability, hour.precipProbability);
            }
        }

        return maxProbability;
    }

    /**
     * Get total precipitation for next 24 hours (inches/mm)
     */
    public Double get24HourPrecipitation() {
        if (hourly == null || hourly.isEmpty()) {
            return null;
        }

        double total = 0;
        int hoursToCheck = Math.min(24, hourly.size());

        for (int i = 0; i < hoursToCheck; i++) {
            HourlyForecast hour = hourly.get(i);
            if (hour != null && hour.precipAccumulation != null) {
                total += hour.precipAccumulation;
            }
        }

        return total;
    }

    /**
     * Check if precipitation expected in next 24 hours
     */
    public boolean hasPrecipitationExpected() {
        Double probability = get24HourPrecipitationProbability();
        return probability != null && probability > 0.3; // 30% threshold
    }

    /**
     * Get recommended watering adjustment based on forecast
     */
    public Integer getRecommendedWateringAdjustment() {
        if (!hasPrecipitationExpected()) {
            return 0; // No adjustment needed
        }

        Double precip = get24HourPrecipitationProbability();
        if (precip == null) {
            return 0;
        }

        // Calculate adjustment percentage (reduce watering if rain expected)
        if (precip > 0.8) {
            return -80; // 80% reduction for heavy rain
        } else if (precip > 0.6) {
            return -60; // 60% reduction
        } else if (precip > 0.4) {
            return -40; // 40% reduction
        } else if (precip > 0.2) {
            return -20; // 20% reduction
        }

        return 0;
    }

    /**
     * Validate forecast data
     */
    public boolean isValidData() {
        return deviceId != null && !deviceId.isEmpty() && generatedAt != null
                && (current != null || (hourly != null && !hourly.isEmpty()));
    }

    /**
     * Create summary string for logging
     */
    public String toSummaryString() {
        return String.format("Forecast[device=%s, temp=%s, condition=%s, alerts=%d]", deviceId,
                current != null && current.temperature != null ? current.temperature + "°" : "N/A",
                current != null && current.condition != null ? current.condition : "N/A", getAlertCount());
    }

    @Override
    public String toString() {
        return "RachioForecast{" + "deviceId='" + deviceId + '\'' + ", current="
                + (current != null ? current.temperature + "°" : "null") + ", hourly=" + getHourlyCount() + ", daily="
                + getDailyCount() + ", alerts=" + getAlertCount() + ", generatedAt=" + generatedAt + '}';
    }

    /**
     * Current weather conditions
     */
    public static class CurrentConditions {

        @SerializedName("timestamp")
        public Instant timestamp;

        @SerializedName("temperature")
        public Double temperature;

        @SerializedName("feelsLike")
        public Double feelsLike;

        @SerializedName("condition")
        public String condition;

        @SerializedName("conditionCode")
        public String conditionCode;

        @SerializedName("humidity")
        public Double humidity;

        @SerializedName("windSpeed")
        public Double windSpeed;

        @SerializedName("windDirection")
        public Integer windDirection;

        @SerializedName("windGust")
        public Double windGust;

        @SerializedName("pressure")
        public Double pressure;

        @SerializedName("visibility")
        public Double visibility;

        @SerializedName("uvIndex")
        public Double uvIndex;

        @SerializedName("precipProbability")
        public Double precipProbability;

        @SerializedName("precipIntensity")
        public Double precipIntensity;

        @SerializedName("precipAccumulation")
        public Double precipAccumulation;

        @SerializedName("precipType")
        public String precipType;

        @SerializedName("cloudCover")
        public Double cloudCover;

        @SerializedName("dewPoint")
        public Double dewPoint;

        /**
         * Get temperature with unit
         */
        public String getTemperatureString() {
            if (temperature == null)
                return "N/A";
            return String.format("%.1f°", temperature);
        }

        /**
         * Get feels-like temperature with unit
         */
        public String getFeelsLikeString() {
            if (feelsLike == null)
                return "N/A";
            return String.format("%.1f°", feelsLike);
        }

        /**
         * Get humidity percentage
         */
        public String getHumidityString() {
            if (humidity == null)
                return "N/A";
            return String.format("%.0f%%", humidity * 100);
        }

        /**
         * Get wind speed with unit
         */
        public String getWindSpeedString() {
            if (windSpeed == null)
                return "N/A";
            return String.format("%.1f mph", windSpeed);
        }

        /**
         * Get precipitation probability
         */
        public String getPrecipProbabilityString() {
            if (precipProbability == null)
                return "N/A";
            return String.format("%.0f%%", precipProbability * 100);
        }

        /**
         * Check if precipitation is expected
         */
        public boolean hasPrecipitation() {
            return precipProbability != null && precipProbability > 0.1;
        }

        /**
         * Get UV index level
         */
        public String getUvLevel() {
            if (uvIndex == null)
                return "N/A";

            if (uvIndex <= 2)
                return "Low";
            else if (uvIndex <= 5)
                return "Moderate";
            else if (uvIndex <= 7)
                return "High";
            else if (uvIndex <= 10)
                return "Very High";
            else
                return "Extreme";
        }

        @Override
        public String toString() {
            return "CurrentConditions{" + "temperature=" + temperature + ", condition='" + condition + '\''
                    + ", humidity=" + (humidity != null ? humidity * 100 : null) + "%" + ", windSpeed=" + windSpeed
                    + '}';
        }
    }

    /**
     * Hourly forecast
     */
    public static class HourlyForecast {

        @SerializedName("timestamp")
        public Instant timestamp;

        @SerializedName("temperature")
        public Double temperature;

        @SerializedName("feelsLike")
        public Double feelsLike;

        @SerializedName("condition")
        public String condition;

        @SerializedName("conditionCode")
        public String conditionCode;

        @SerializedName("precipProbability")
        public Double precipProbability;

        @SerializedName("precipIntensity")
        public Double precipIntensity;

        @SerializedName("precipAccumulation")
        public Double precipAccumulation;

        @SerializedName("precipType")
        public String precipType;

        @SerializedName("humidity")
        public Double humidity;

        @SerializedName("windSpeed")
        public Double windSpeed;

        @SerializedName("windDirection")
        public Integer windDirection;

        @SerializedName("cloudCover")
        public Double cloudCover;

        @SerializedName("uvIndex")
        public Double uvIndex;

        /**
         * Get hour of day (0-23)
         */
        public Integer getHour() {
            if (timestamp == null)
                return null;

            // Convert to local time based on forecast timezone
            // Simplified version - would need timezone conversion in real implementation
            return timestamp.atZone(java.time.ZoneId.systemDefault()).getHour();
        }

        /**
         * Check if this hour has precipitation
         */
        public boolean hasPrecipitation() {
            return precipProbability != null && precipProbability > 0.1;
        }

        @Override
        public String toString() {
            return "HourlyForecast{" + "timestamp=" + timestamp + ", temperature=" + temperature + ", condition='"
                    + condition + '\'' + ", precipProbability="
                    + (precipProbability != null ? precipProbability * 100 : null) + "%" + '}';
        }
    }

    /**
     * Daily forecast - FIXED WITH MISSING FIELDS
     */
    public static class DailyForecast {

        @SerializedName("date")
        public LocalDate date;

        @SerializedName("highTemperature")
        public Double highTemperature;

        @SerializedName("lowTemperature")
        public Double lowTemperature;

        @SerializedName("condition")
        public String condition;

        @SerializedName("conditionCode")
        public String conditionCode;

        @SerializedName("precipProbability")
        public Double precipProbability;

        @SerializedName("precipAccumulation")
        public Double precipAccumulation;

        @SerializedName("precipType")
        public String precipType;

        @SerializedName("humidity")
        public Double humidity;

        @SerializedName("windSpeed")
        public Double windSpeed;

        @SerializedName("windDirection")
        public Integer windDirection;

        @SerializedName("cloudCover")
        public Double cloudCover;

        @SerializedName("uvIndex")
        public Double uvIndex;

        @SerializedName("sunrise")
        public Instant sunrise;

        @SerializedName("sunset")
        public Instant sunset;

        @SerializedName("moonPhase")
        public Double moonPhase;

        // ===== ADDED FIELDS TO FIX COMPILATION ERRORS =====

        /**
         * Temperature high - for compatibility with RachioDeviceHandler.java
         * This field provides the same data as highTemperature but with a different name
         * to match the handler's expected field names
         */
        @SerializedName("temperatureHigh")
        @Nullable
        public Double temperatureHigh;

        /**
         * Temperature low - for compatibility with RachioDeviceHandler.java
         * This field provides the same data as lowTemperature but with a different name
         * to match the handler's expected field names
         */
        @SerializedName("temperatureLow")
        @Nullable
        public Double temperatureLow;

        /**
         * Evapotranspiration data - for compatibility with RachioDeviceHandler.java
         */
        @SerializedName("evapotranspiration")
        @Nullable
        public Double evapotranspiration;

        /**
         * Smart skip recommendation - for compatibility with RachioDeviceHandler.java
         */
        @SerializedName("smartSkip")
        @Nullable
        public Boolean smartSkip;

        /**
         * Get day of week
         */
        public String getDayOfWeek() {
            if (date == null)
                return null;
            return date.getDayOfWeek().toString();
        }

        /**
         * Get short day name (Mon, Tue, etc.)
         */
        public String getShortDayName() {
            if (date == null)
                return null;
            return date.getDayOfWeek().toString().substring(0, 3);
        }

        /**
         * Check if precipitation is expected
         */
        public boolean hasPrecipitation() {
            return precipProbability != null && precipProbability > 0.1;
        }

        /**
         * Get temperature range string
         */
        public String getTemperatureRange() {
            if (highTemperature == null || lowTemperature == null) {
                return "N/A";
            }
            return String.format("%.0f° / %.0f°", highTemperature, lowTemperature);
        }

        /**
         * Get precipitation probability string
         */
        public String getPrecipProbabilityString() {
            if (precipProbability == null)
                return "N/A";
            return String.format("%.0f%%", precipProbability * 100);
        }

        /**
         * COMPATIBILITY GETTER: Gets temperatureHigh field.
         * First tries the dedicated temperatureHigh field, then falls back to highTemperature.
         */
        @Nullable
        public Double getTemperatureHigh() {
            return temperatureHigh != null ? temperatureHigh : highTemperature;
        }

        /**
         * COMPATIBILITY GETTER: Gets temperatureLow field.
         * First tries the dedicated temperatureLow field, then falls back to lowTemperature.
         */
        @Nullable
        public Double getTemperatureLow() {
            return temperatureLow != null ? temperatureLow : lowTemperature;
        }

        /**
         * COMPATIBILITY GETTER: Gets evapotranspiration field.
         */
        @Nullable
        public Double getEvapotranspiration() {
            return evapotranspiration;
        }

        /**
         * COMPATIBILITY GETTER: Gets smartSkip field.
         */
        @Nullable
        public Boolean getSmartSkip() {
            return smartSkip;
        }

        @Override
        public String toString() {
            return "DailyForecast{" + "date=" + date + ", highTemperature=" + highTemperature + ", lowTemperature="
                    + lowTemperature + ", condition='" + condition + '\'' + ", precipProbability="
                    + (precipProbability != null ? precipProbability * 100 : null) + "%" + '}';
        }
    }

    /**
     * Weather alert
     */
    public static class WeatherAlert {

        @SerializedName("id")
        public String id;

        @SerializedName("title")
        public String title;

        @SerializedName("description")
        public String description;

        @SerializedName("severity")
        public String severity;

        @SerializedName("urgency")
        public String urgency;

        @SerializedName("certainty")
        public String certainty;

        @SerializedName("event")
        public String event;

        @SerializedName("headline")
        public String headline;

        @SerializedName("instruction")
        public String instruction;

        @SerializedName("effective")
        public Instant effective;

        @SerializedName("expires")
        public Instant expires;

        @SerializedName("onset")
        public Instant onset;

        @SerializedName("ends")
        public Instant ends;

        @SerializedName("area")
        public String area;

        /**
         * Check if alert is active (not expired)
         */
        public boolean isActive() {
            if (expires == null) {
                return true; // No expiration means always active
            }

            Instant now = Instant.now();
            return now.isBefore(expires);
        }

        /**
         * Check if alert is severe
         */
        public boolean isSevere() {
            return "extreme".equalsIgnoreCase(severity) || "severe".equalsIgnoreCase(severity);
        }

        /**
         * Get alert priority (higher = more severe)
         */
        public int getPriority() {
            if ("extreme".equalsIgnoreCase(severity))
                return 100;
            else if ("severe".equalsIgnoreCase(severity))
                return 80;
            else if ("moderate".equalsIgnoreCase(severity))
                return 60;
            else if ("minor".equalsIgnoreCase(severity))
                return 40;
            else
                return 20;
        }

        @Override
        public String toString() {
            return "WeatherAlert{" + "title='" + title + '\'' + ", severity='" + severity + '\'' + ", event='" + event
                    + '\'' + ", active=" + isActive() + '}';
        }
    }

    /**
     * COMPATIBILITY METHOD: Gets forecast data in the simple format
     * expected by older handler code (RachioDeviceHandler.java).
     * This prevents compilation errors without changing the DTO structure.
     */
    public @Nullable DailyForecast getLegacyForecastDay(int index) {
        // First, try the dedicated 'forecastDays' list
        if (forecastDays != null && index >= 0 && index < forecastDays.size()) {
            return forecastDays.get(index);
        }
        // Fall back to the main 'daily' list
        if (daily != null && index >= 0 && index < daily.size()) {
            return daily.get(index);
        }
        return null;
    }
}
