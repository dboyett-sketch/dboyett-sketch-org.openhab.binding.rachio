package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for Rachio weather forecast data
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioForecast {

    // Forecast summary
    @SerializedName("summary")
    public @Nullable Summary summary;
    
    @SerializedName("currentConditions")
    public @Nullable CurrentConditions currentConditions;
    
    @SerializedName("hourlyForecast")
    public @Nullable List<HourlyForecast> hourlyForecast;
    
    @SerializedName("dailyForecast")
    public @Nullable List<DailyForecast> dailyForecast;
    
    @SerializedName("units")
    public @Nullable String units;
    
    @SerializedName("generatedAt")
    public @Nullable Instant generatedAt;
    
    @SerializedName("validFor")
    public @Nullable Instant validFor;
    
    @SerializedName("location")
    public @Nullable Location location;
    
    @SerializedName("weatherProvider")
    public @Nullable String weatherProvider;
    
    /**
     * Forecast summary
     */
    @NonNullByDefault
    public static class Summary {
        @SerializedName("today")
        public @Nullable DaySummary today;
        
        @SerializedName("tomorrow")
        public @Nullable DaySummary tomorrow;
        
        @SerializedName("next7Days")
        public @Nullable Next7DaysSummary next7Days;
        
        @SerializedName("wateringRecommendation")
        public @Nullable String wateringRecommendation;
        
        @SerializedName("skipRecommendation")
        public @Nullable Boolean skipRecommendation;
        
        @SerializedName("skipReason")
        public @Nullable String skipReason;
        
        @SerializedName("confidence")
        public @Nullable Double confidence;
    }
    
    /**
     * Day summary
     */
    @NonNullByDefault
    public static class DaySummary {
        @SerializedName("date")
        public @Nullable LocalDate date;
        
        @SerializedName("highTemperature")
        public @Nullable Double highTemperature;
        
        @SerializedName("lowTemperature")
        public @Nullable Double lowTemperature;
        
        @SerializedName("precipitation")
        public @Nullable Double precipitation;
        
        @SerializedName("precipitationProbability")
        public @Nullable Double precipitationProbability;
        
        @SerializedName("precipitationType")
        public @Nullable String precipitationType;
        
        @SerializedName("evapotranspiration")
        public @Nullable Double evapotranspiration;
        
        @SerializedName("solarRadiation")
        public @Nullable Double solarRadiation;
        
        @SerializedName("windSpeed")
        public @Nullable Double windSpeed;
        
        @SerializedName("windGust")
        public @Nullable Double windGust;
        
        @SerializedName("windDirection")
        public @Nullable String windDirection;
        
        @SerializedName("humidity")
        public @Nullable Double humidity;
        
        @SerializedName("cloudCover")
        public @Nullable Double cloudCover;
        
        @SerializedName("uvIndex")
        public @Nullable Double uvIndex;
        
        @SerializedName("condition")
        public @Nullable String condition;
        
        @SerializedName("conditionIcon")
        public @Nullable String conditionIcon;
        
        @SerializedName("wateringAdjustment")
        public @Nullable Double wateringAdjustment;
        
        @SerializedName("skipWatering")
        public @Nullable Boolean skipWatering;
        
        @SerializedName("skipReason")
        public @Nullable String skipReason;
    }
    
    /**
     * Next 7 days summary
     */
    @NonNullByDefault
    public static class Next7DaysSummary {
        @SerializedName("totalPrecipitation")
        public @Nullable Double totalPrecipitation;
        
        @SerializedName("averageTemperature")
        public @Nullable Double averageTemperature;
        
        @SerializedName("totalEvapotranspiration")
        public @Nullable Double totalEvapotranspiration;
        
        @SerializedName("wateringDays")
        public @Nullable Integer wateringDays;
        
        @SerializedName("skipDays")
        public @Nullable Integer skipDays;
        
        @SerializedName("waterSavings")
        public @Nullable Double waterSavings;
        
        @SerializedName("waterSavingsPercentage")
        public @Nullable Double waterSavingsPercentage;
    }
    
    /**
     * Current conditions
     */
    @NonNullByDefault
    public static class CurrentConditions {
        @SerializedName("timestamp")
        public @Nullable Instant timestamp;
        
        @SerializedName("temperature")
        public @Nullable Double temperature;
        
        @SerializedName("feelsLike")
        public @Nullable Double feelsLike;
        
        @SerializedName("humidity")
        public @Nullable Double humidity;
        
        @SerializedName("dewPoint")
        public @Nullable Double dewPoint;
        
        @SerializedName("pressure")
        public @Nullable Double pressure;
        
        @SerializedName("windSpeed")
        public @Nullable Double windSpeed;
        
        @SerializedName("windGust")
        public @Nullable Double windGust;
        
        @SerializedName("windDirection")
        public @Nullable String windDirection;
        
        @SerializedName("windDirectionDegrees")
        public @Nullable Integer windDirectionDegrees;
        
        @SerializedName("visibility")
        public @Nullable Double visibility;
        
        @SerializedName("cloudCover")
        public @Nullable Double cloudCover;
        
        @SerializedName("uvIndex")
        public @Nullable Double uvIndex;
        
        @SerializedName("precipitation")
        public @Nullable Double precipitation;
        
        @SerializedName("precipitationType")
        public @Nullable String precipitationType;
        
        @SerializedName("condition")
        public @Nullable String condition;
        
        @SerializedName("conditionIcon")
        public @Nullable String conditionIcon;
        
        @SerializedName("solarRadiation")
        public @Nullable Double solarRadiation;
        
        @SerializedName("evapotranspiration")
        public @Nullable Double evapotranspiration;
        
        @SerializedName("lastRain")
        public @Nullable Instant lastRain;
        
        @SerializedName("rainLast24Hours")
        public @Nullable Double rainLast24Hours;
    }
    
    /**
     * Hourly forecast
     */
    @NonNullByDefault
    public static class HourlyForecast {
        @SerializedName("timestamp")
        public @Nullable Instant timestamp;
        
        @SerializedName("hour")
        public @Nullable Integer hour;
        
        @SerializedName("temperature")
        public @Nullable Double temperature;
        
        @SerializedName("feelsLike")
        public @Nullable Double feelsLike;
        
        @SerializedName("humidity")
        public @Nullable Double humidity;
        
        @SerializedName("dewPoint")
        public @Nullable Double dewPoint;
        
        @SerializedName("pressure")
        public @Nullable Double pressure;
        
        @SerializedName("windSpeed")
        public @Nullable Double windSpeed;
        
        @SerializedName("windGust")
        public @Nullable Double windGust;
        
        @SerializedName("windDirection")
        public @Nullable String windDirection;
        
        @SerializedName("windDirectionDegrees")
        public @Nullable Integer windDirectionDegrees;
        
        @SerializedName("cloudCover")
        public @Nullable Double cloudCover;
        
        @SerializedName("precipitation")
        public @Nullable Double precipitation;
        
        @SerializedName("precipitationProbability")
        public @Nullable Double precipitationProbability;
        
        @SerializedName("precipitationType")
        public @Nullable String precipitationType;
        
        @SerializedName("condition")
        public @Nullable String condition;
        
        @SerializedName("conditionIcon")
        public @Nullable String conditionIcon;
        
        @SerializedName("solarRadiation")
        public @Nullable Double solarRadiation;
        
        @SerializedName("evapotranspiration")
        public @Nullable Double evapotranspiration;
        
        @SerializedName("freezeRisk")
        public @Nullable Double freezeRisk;
        
        @SerializedName("wateringValue")
        public @Nullable Double wateringValue;
        
        @SerializedName("skipWatering")
        public @Nullable Boolean skipWatering;
    }
    
    /**
     * Daily forecast
     */
    @NonNullByDefault
    public static class DailyForecast {
        @SerializedName("date")
        public @Nullable LocalDate date;
        
        @SerializedName("dayOfWeek")
        public @Nullable String dayOfWeek;
        
        @SerializedName("highTemperature")
        public @Nullable Double highTemperature;
        
        @SerializedName("lowTemperature")
        public @Nullable Double lowTemperature;
        
        @SerializedName("averageTemperature")
        public @Nullable Double averageTemperature;
        
        @SerializedName("morningTemperature")
        public @Nullable Double morningTemperature;
        
        @SerializedName("afternoonTemperature")
        public @Nullable Double afternoonTemperature;
        
        @SerializedName("eveningTemperature")
        public @Nullable Double eveningTemperature;
        
        @SerializedName("nightTemperature")
        public @Nullable Double nightTemperature;
        
        @SerializedName("precipitation")
        public @Nullable Double precipitation;
        
        @SerializedName("precipitationProbability")
        public @Nullable Double precipitationProbability;
        
        @SerializedName("precipitationType")
        public @Nullable String precipitationType;
        
        @SerializedName("snowfall")
        public @Nullable Double snowfall;
        
        @SerializedName("snowDepth")
        public @Nullable Double snowDepth;
        
        @SerializedName("windSpeed")
        public @Nullable Double windSpeed;
        
        @SerializedName("windGust")
        public @Nullable Double windGust;
        
        @SerializedName("windDirection")
        public @Nullable String windDirection;
        
        @SerializedName("humidity")
        public @Nullable Double humidity;
        
        @SerializedName("dewPoint")
        public @Nullable Double dewPoint;
        
        @SerializedName("cloudCover")
        public @Nullable Double cloudCover;
        
        @SerializedName("uvIndex")
        public @Nullable Double uvIndex;
        
        @SerializedName("visibility")
        public @Nullable Double visibility;
        
        @SerializedName("sunrise")
        public @Nullable String sunrise;
        
        @SerializedName("sunset")
        public @Nullable String sunset;
        
        @SerializedName("moonPhase")
        public @Nullable String moonPhase;
        
        @SerializedName("condition")
        public @Nullable String condition;
        
        @SerializedName("conditionIcon")
        public @Nullable String conditionIcon;
        
        @SerializedName("solarRadiation")
        public @Nullable Double solarRadiation;
        
        @SerializedName("evapotranspiration")
        public @Nullable Double evapotranspiration;
        
        @SerializedName("wateringAdjustment")
        public @Nullable Double wateringAdjustment;
        
        @SerializedName("skipWatering")
        public @Nullable Boolean skipWatering;
        
        @SerializedName("skipReason")
        public @Nullable String skipReason;
        
        @SerializedName("freezeRisk")
        public @Nullable Double freezeRisk;
        
        @SerializedName("wateringValue")
        public @Nullable Double wateringValue;
        
        @SerializedName("etWateringAdjustment")
        public @Nullable Double etWateringAdjustment;
        
        @SerializedName("rainWateringAdjustment")
        public @Nullable Double rainWateringAdjustment;
        
        @SerializedName("windWateringAdjustment")
        public @Nullable Double windWateringAdjustment;
        
        @SerializedName("humidityWateringAdjustment")
        public @Nullable Double humidityWateringAdjustment;
        
        @SerializedName("temperatureWateringAdjustment")
        public @Nullable Double temperatureWateringAdjustment;
    }
    
    /**
     * Location information
     */
    @NonNullByDefault
    public static class Location {
        @SerializedName("latitude")
        public @Nullable Double latitude;
        
        @SerializedName("longitude")
        public @Nullable Double longitude;
        
        @SerializedName("elevation")
        public @Nullable Double elevation;
        
        @SerializedName("timezone")
        public @Nullable String timezone;
        
        @SerializedName("city")
        public @Nullable String city;
        
        @SerializedName("state")
        public @Nullable String state;
        
        @SerializedName("country")
        public @Nullable String country;
        
        @SerializedName("zipCode")
        public @Nullable String zipCode;
        
        @SerializedName("stationId")
        public @Nullable String stationId;
        
        @SerializedName("stationDistance")
        public @Nullable Double stationDistance;
        
        @SerializedName("stationName")
        public @Nullable String stationName;
    }
    
    // Utility methods
    
    /**
     * Get today's forecast
     */
    public @Nullable DaySummary getToday() {
        if (summary != null && summary.today != null) {
            return summary.today;
        }
        return null;
    }
    
    /**
     * Get tomorrow's forecast
     */
    public @Nullable DaySummary getTomorrow() {
        if (summary != null && summary.tomorrow != null) {
            return summary.tomorrow;
        }
        return null;
    }
    
    /**
     * Get next 7 days summary
     */
    public @Nullable Next7DaysSummary getNext7Days() {
        if (summary != null && summary.next7Days != null) {
            return summary.next7Days;
        }
        return null;
    }
    
    /**
     * Get watering recommendation
     */
    public @Nullable String getWateringRecommendation() {
        if (summary != null && summary.wateringRecommendation != null) {
            return summary.wateringRecommendation;
        }
        return null;
    }
    
    /**
     * Check if watering should be skipped today
     */
    public boolean shouldSkipWateringToday() {
        DaySummary today = getToday();
        return today != null && today.skipWatering != null && today.skipWatering;
    }
    
    /**
     * Get skip reason for today
     */
    public @Nullable String getSkipReasonToday() {
        DaySummary today = getToday();
        if (today != null && today.skipReason != null) {
            return today.skipReason;
        }
        if (summary != null && summary.skipReason != null) {
            return summary.skipReason;
        }
        return null;
    }
    
    /**
     * Get today's evapotranspiration
     */
    public @Nullable Double getEvapotranspirationToday() {
        DaySummary today = getToday();
        return today != null ? today.evapotranspiration : null;
    }
    
    /**
     * Get today's precipitation
     */
    public @Nullable Double getPrecipitationToday() {
        DaySummary today = getToday();
        return today != null ? today.precipitation : null;
    }
    
    /**
     * Get today's high temperature
     */
    public @Nullable Double getHighTemperatureToday() {
        DaySummary today = getToday();
        return today != null ? today.highTemperature : null;
    }
    
    /**
     * Get today's low temperature
     */
    public @Nullable Double getLowTemperatureToday() {
        DaySummary today = getToday();
        return today != null ? today.lowTemperature : null;
    }
    
    /**
     * Get current temperature
     */
    public @Nullable Double getCurrentTemperature() {
        if (currentConditions != null && currentConditions.temperature != null) {
            return currentConditions.temperature;
        }
        return null;
    }
    
    /**
     * Get current conditions
     */
    public @Nullable String getCurrentConditions() {
        if (currentConditions != null && currentConditions.condition != null) {
            return currentConditions.condition;
        }
        return null;
    }
    
    /**
     * Get watering adjustment for today
     */
    public @Nullable Double getWateringAdjustmentToday() {
        DaySummary today = getToday();
        return today != null ? today.wateringAdjustment : null;
    }
    
    /**
     * Get forecast generation time
     */
    public @Nullable Instant getGeneratedAt() {
        return generatedAt;
    }
    
    /**
     * Get location information
     */
    public @Nullable Location getLocation() {
        return location;
    }
    
    /**
     * Get weather provider
     */
    public @Nullable String getWeatherProvider() {
        return weatherProvider;
    }
    
    /**
     * Check if forecast data is available
     */
    public boolean hasForecastData() {
        return summary != null || currentConditions != null || 
               (dailyForecast != null && !dailyForecast.isEmpty()) ||
               (hourlyForecast != null && !hourlyForecast.isEmpty());
    }
    
    /**
     * Get forecast age in minutes
     */
    public @Nullable Long getForecastAgeMinutes() {
        if (generatedAt != null) {
            Instant now = Instant.now();
            long seconds = now.getEpochSecond() - generatedAt.getEpochSecond();
            return seconds / 60;
        }
        return null;
    }
    
    /**
     * Get formatted forecast summary
     */
    public String getFormattedSummary() {
        StringBuilder sb = new StringBuilder();
        
        if (location != null && location.city != null) {
            sb.append(location.city);
            if (location.state != null) {
                sb.append(", ").append(location.state);
            }
            sb.append(": ");
        }
        
        DaySummary today = getToday();
        if (today != null) {
            sb.append(today.condition != null ? today.condition : "Unknown conditions");
            
            if (today.highTemperature != null && today.lowTemperature != null) {
                sb.append(", High: ").append(String.format("%.0f", today.highTemperature)).append("°");
                sb.append(", Low: ").append(String.format("%.0f", today.lowTemperature)).append("°");
            }
            
            if (today.precipitationProbability != null && today.precipitationProbability > 0) {
                sb.append(", Precip: ").append(String.format("%.0f", today.precipitationProbability)).append("%");
            }
            
            if (shouldSkipWateringToday()) {
                String reason = getSkipReasonToday();
                sb.append(", Skip watering");
                if (reason != null) {
                    sb.append(" (").append(reason).append(")");
                }
            }
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("RachioForecast{generatedAt=%s, hasData=%s, location=%s}",
                generatedAt, hasForecastData(), 
                location != null && location.city != null ? location.city : "Unknown");
    }
}
