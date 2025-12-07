package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO representing Rachio weather forecast data
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioForecast {
    
    // Forecast metadata
    @SerializedName("deviceId")
    private String deviceId = "";
    
    @SerializedName("forecastGeneratedAt")
    private Instant forecastGeneratedAt;
    
    @SerializedName("units")
    private String units = "US"; // "US" or "METRIC"
    
    @SerializedName("timezone")
    private String timezone = "";
    
    // Current conditions (was causing compilation errors)
    @SerializedName("temperature")
    private Double temperature; // in Fahrenheit or Celsius
    
    @SerializedName("precipitation")
    private Double precipitation; // in inches or mm
    
    @SerializedName("evapotranspiration")
    private Double evapotranspiration; // in inches or mm
    
    // Additional current conditions
    @SerializedName("humidity")
    private Double humidity; // percentage
    
    @SerializedName("windSpeed")
    private Double windSpeed; // in mph or km/h
    
    @SerializedName("windDirection")
    private Double windDirection; // degrees
    
    @SerializedName("solarRadiation")
    private Double solarRadiation; // W/m²
    
    @SerializedName("cloudCover")
    private Double cloudCover; // percentage
    
    @SerializedName("dewPoint")
    private Double dewPoint; // in Fahrenheit or Celsius
    
    @SerializedName("pressure")
    private Double pressure; // in hPa
    
    @SerializedName("uvIndex")
    private Double uvIndex;
    
    @SerializedName("visibility")
    private Double visibility; // in miles or km
    
    // Forecast intervals (hourly)
    @SerializedName("hourly")
    private List<HourlyForecast> hourly = List.of();
    
    // Daily forecasts
    @SerializedName("daily")
    private List<DailyForecast> daily = List.of();
    
    // Weather alerts
    @SerializedName("alerts")
    private List<WeatherAlert> alerts = List.of();
    
    // Forecast source
    @SerializedName("source")
    private String source = "";
    
    @SerializedName("sourceUrl")
    private String sourceUrl = "";
    
    // Getters and setters
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public @Nullable Instant getForecastGeneratedAt() {
        return forecastGeneratedAt;
    }
    
    public void setForecastGeneratedAt(Instant forecastGeneratedAt) {
        this.forecastGeneratedAt = forecastGeneratedAt;
    }
    
    public String getUnits() {
        return units;
    }
    
    public void setUnits(String units) {
        this.units = units;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    // CRITICAL: These were the missing fields causing compilation errors
    public @Nullable Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    public @Nullable Double getPrecipitation() {
        return precipitation;
    }
    
    public void setPrecipitation(Double precipitation) {
        this.precipitation = precipitation;
    }
    
    public @Nullable Double getEvapotranspiration() {
        return evapotranspiration;
    }
    
    public void setEvapotranspiration(Double evapotranspiration) {
        this.evapotranspiration = evapotranspiration;
    }
    
    public @Nullable Double getHumidity() {
        return humidity;
    }
    
    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }
    
    public @Nullable Double getWindSpeed() {
        return windSpeed;
    }
    
    public void setWindSpeed(Double windSpeed) {
        this.windSpeed = windSpeed;
    }
    
    public @Nullable Double getWindDirection() {
        return windDirection;
    }
    
    public void setWindDirection(Double windDirection) {
        this.windDirection = windDirection;
    }
    
    public @Nullable Double getSolarRadiation() {
        return solarRadiation;
    }
    
    public void setSolarRadiation(Double solarRadiation) {
        this.solarRadiation = solarRadiation;
    }
    
    public @Nullable Double getCloudCover() {
        return cloudCover;
    }
    
    public void setCloudCover(Double cloudCover) {
        this.cloudCover = cloudCover;
    }
    
    public @Nullable Double getDewPoint() {
        return dewPoint;
    }
    
    public void setDewPoint(Double dewPoint) {
        this.dewPoint = dewPoint;
    }
    
    public @Nullable Double getPressure() {
        return pressure;
    }
    
    public void setPressure(Double pressure) {
        this.pressure = pressure;
    }
    
    public @Nullable Double getUvIndex() {
        return uvIndex;
    }
    
    public void setUvIndex(Double uvIndex) {
        this.uvIndex = uvIndex;
    }
    
    public @Nullable Double getVisibility() {
        return visibility;
    }
    
    public void setVisibility(Double visibility) {
        this.visibility = visibility;
    }
    
    public List<HourlyForecast> getHourly() {
        return hourly;
    }
    
    public void setHourly(List<HourlyForecast> hourly) {
        this.hourly = hourly;
    }
    
    public List<DailyForecast> getDaily() {
        return daily;
    }
    
    public void setDaily(List<DailyForecast> daily) {
        this.daily = daily;
    }
    
    public List<WeatherAlert> getAlerts() {
        return alerts;
    }
    
    public void setAlerts(List<WeatherAlert> alerts) {
        this.alerts = alerts;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getSourceUrl() {
        return sourceUrl;
    }
    
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
    
    // Helper methods
    public boolean isMetric() {
        return "METRIC".equalsIgnoreCase(units);
    }
    
    public String getTemperatureUnit() {
        return isMetric() ? "°C" : "°F";
    }
    
    public String getPrecipitationUnit() {
        return isMetric() ? "mm" : "in";
    }
    
    public String getWindSpeedUnit() {
        return isMetric() ? "km/h" : "mph";
    }
    
    public String getPressureUnit() {
        return "hPa";
    }
    
    public String getVisibilityUnit() {
        return isMetric() ? "km" : "mi";
    }
    
    public @Nullable HourlyForecast getCurrentHourlyForecast() {
        if (hourly.isEmpty()) {
            return null;
        }
        Instant now = Instant.now();
        for (HourlyForecast forecast : hourly) {
            if (forecast.getTime() != null && !forecast.getTime().isAfter(now)) {
                return forecast;
            }
        }
        return hourly.get(0);
    }
    
    public @Nullable DailyForecast getTodayForecast() {
        if (daily.isEmpty()) {
            return null;
        }
        LocalDate today = LocalDate.now();
        for (DailyForecast forecast : daily) {
            if (forecast.getDate() != null && forecast.getDate().equals(today)) {
                return forecast;
            }
        }
        return daily.get(0);
    }
    
    public boolean hasActiveAlerts() {
        return alerts != null && !alerts.isEmpty();
    }
    
    public double getEffectiveEvapotranspiration() {
        // Calculate effective ET based on crop coefficient (if available)
        // This would typically be used with zone crop coefficients
        return evapotranspiration != null ? evapotranspiration : 0.0;
    }
    
    @Override
    public String toString() {
        return String.format("RachioForecast[deviceId=%s, temp=%s°%s, precip=%s%s, et=%s%s]", 
            deviceId, temperature, getTemperatureUnit(), precipitation, getPrecipitationUnit(), 
            evapotranspiration, getPrecipitationUnit());
    }
    
    /**
     * Hourly forecast data
     */
    public static class HourlyForecast {
        @SerializedName("time")
        private Instant time;
        
        @SerializedName("temperature")
        private Double temperature;
        
        @SerializedName("precipitation")
        private Double precipitation;
        
        @SerializedName("precipitationProbability")
        private Double precipitationProbability;
        
        @SerializedName("evapotranspiration")
        private Double evapotranspiration;
        
        @SerializedName("humidity")
        private Double humidity;
        
        @SerializedName("windSpeed")
        private Double windSpeed;
        
        @SerializedName("windDirection")
        private Double windDirection;
        
        @SerializedName("solarRadiation")
        private Double solarRadiation;
        
        @SerializedName("cloudCover")
        private Double cloudCover;
        
        @SerializedName("dewPoint")
        private Double dewPoint;
        
        @SerializedName("pressure")
        private Double pressure;
        
        @SerializedName("uvIndex")
        private Double uvIndex;
        
        @SerializedName("visibility")
        private Double visibility;
        
        @SerializedName("icon")
        private String icon;
        
        @SerializedName("summary")
        private String summary;
        
        public @Nullable Instant getTime() { return time; }
        public void setTime(Instant time) { this.time = time; }
        
        public @Nullable Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        
        public @Nullable Double getPrecipitation() { return precipitation; }
        public void setPrecipitation(Double precipitation) { this.precipitation = precipitation; }
        
        public @Nullable Double getPrecipitationProbability() { return precipitationProbability; }
        public void setPrecipitationProbability(Double precipitationProbability) { this.precipitationProbability = precipitationProbability; }
        
        public @Nullable Double getEvapotranspiration() { return evapotranspiration; }
        public void setEvapotranspiration(Double evapotranspiration) { this.evapotranspiration = evapotranspiration; }
        
        public @Nullable Double getHumidity() { return humidity; }
        public void setHumidity(Double humidity) { this.humidity = humidity; }
        
        public @Nullable Double getWindSpeed() { return windSpeed; }
        public void setWindSpeed(Double windSpeed) { this.windSpeed = windSpeed; }
        
        public @Nullable Double getWindDirection() { return windDirection; }
        public void setWindDirection(Double windDirection) { this.windDirection = windDirection; }
        
        public @Nullable Double getSolarRadiation() { return solarRadiation; }
        public void setSolarRadiation(Double solarRadiation) { this.solarRadiation = solarRadiation; }
        
        public @Nullable Double getCloudCover() { return cloudCover; }
        public void setCloudCover(Double cloudCover) { this.cloudCover = cloudCover; }
        
        public @Nullable Double getDewPoint() { return dewPoint; }
        public void setDewPoint(Double dewPoint) { this.dewPoint = dewPoint; }
        
        public @Nullable Double getPressure() { return pressure; }
        public void setPressure(Double pressure) { this.pressure = pressure; }
        
        public @Nullable Double getUvIndex() { return uvIndex; }
        public void setUvIndex(Double uvIndex) { this.uvIndex = uvIndex; }
        
        public @Nullable Double getVisibility() { return visibility; }
        public void setVisibility(Double visibility) { this.visibility = visibility; }
        
        public String getIcon() { return icon != null ? icon : ""; }
        public void setIcon(String icon) { this.icon = icon; }
        
        public String getSummary() { return summary != null ? summary : ""; }
        public void setSummary(String summary) { this.summary = summary; }
        
        public String getFormattedTime(ZoneId zoneId) {
            if (time == null) return "";
            return DateTimeFormatter.ofPattern("h a").format(time.atZone(zoneId));
        }
        
        @Override
        public String toString() {
            return String.format("HourlyForecast[time=%s, temp=%s, precip=%s]", 
                time, temperature, precipitation);
        }
    }
    
    /**
     * Daily forecast data
     */
    public static class DailyForecast {
        @SerializedName("date")
        private LocalDate date;
        
        @SerializedName("temperatureHigh")
        private Double temperatureHigh;
        
        @SerializedName("temperatureLow")
        private Double temperatureLow;
        
        @SerializedName("precipitation")
        private Double precipitation;
        
        @SerializedName("precipitationProbability")
        private Double precipitationProbability;
        
        @SerializedName("evapotranspiration")
        private Double evapotranspiration;
        
        @SerializedName("humidity")
        private Double humidity;
        
        @SerializedName("windSpeed")
        private Double windSpeed;
        
        @SerializedName("windDirection")
        private Double windDirection;
        
        @SerializedName("solarRadiation")
        private Double solarRadiation;
        
        @SerializedName("cloudCover")
        private Double cloudCover;
        
        @SerializedName("dewPoint")
        private Double dewPoint;
        
        @SerializedName("pressure")
        private Double pressure;
        
        @SerializedName("uvIndex")
        private Double uvIndex;
        
        @SerializedName("sunriseTime")
        private Instant sunriseTime;
        
        @SerializedName("sunsetTime")
        private Instant sunsetTime;
        
        @SerializedName("moonPhase")
        private Double moonPhase;
        
        @SerializedName("icon")
        private String icon;
        
        @SerializedName("summary")
        private String summary;
        
        public @Nullable LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        
        public @Nullable Double getTemperatureHigh() { return temperatureHigh; }
        public void setTemperatureHigh(Double temperatureHigh) { this.temperatureHigh = temperatureHigh; }
        
        public @Nullable Double getTemperatureLow() { return temperatureLow; }
        public void setTemperatureLow(Double temperatureLow) { this.temperatureLow = temperatureLow; }
        
        public @Nullable Double getPrecipitation() { return precipitation; }
        public void setPrecipitation(Double precipitation) { this.precipitation = precipitation; }
        
        public @Nullable Double getPrecipitationProbability() { return precipitationProbability; }
        public void setPrecipitationProbability(Double precipitationProbability) { this.precipitationProbability = precipitationProbability; }
        
        public @Nullable Double getEvapotranspiration() { return evapotranspiration; }
        public void setEvapotranspiration(Double evapotranspiration) { this.evapotranspiration = evapotranspiration; }
        
        public @Nullable Double getHumidity() { return humidity; }
        public void setHumidity(Double humidity) { this.humidity = humidity; }
        
        public @Nullable Double getWindSpeed() { return windSpeed; }
        public void setWindSpeed(Double windSpeed) { this.windSpeed = windSpeed; }
        
        public @Nullable Double getWindDirection() { return windDirection; }
        public void setWindDirection(Double windDirection) { this.windDirection = windDirection; }
        
        public @Nullable Double getSolarRadiation() { return solarRadiation; }
        public void setSolarRadiation(Double solarRadiation) { this.solarRadiation = solarRadiation; }
        
        public @Nullable Double getCloudCover() { return cloudCover; }
        public void setCloudCover(Double cloudCover) { this.cloudCover = cloudCover; }
        
        public @Nullable Double getDewPoint() { return dewPoint; }
        public void setDewPoint(Double dewPoint) { this.dewPoint = dewPoint; }
        
        public @Nullable Double getPressure() { return pressure; }
        public void setPressure(Double pressure) { this.pressure = pressure; }
        
        public @Nullable Double getUvIndex() { return uvIndex; }
        public void setUvIndex(Double uvIndex) { this.uvIndex = uvIndex; }
        
        public @Nullable Instant getSunriseTime() { return sunriseTime; }
        public void setSunriseTime(Instant sunriseTime) { this.sunriseTime = sunriseTime; }
        
        public @Nullable Instant getSunsetTime() { return sunsetTime; }
        public void setSunsetTime(Instant sunsetTime) { this.sunsetTime = sunsetTime; }
        
        public @Nullable Double getMoonPhase() { return moonPhase; }
        public void setMoonPhase(Double moonPhase) { this.moonPhase = moonPhase; }
        
        public String getIcon() { return icon != null ? icon : ""; }
        public void setIcon(String icon) { this.icon = icon; }
        
        public String getSummary() { return summary != null ? summary : ""; }
        public void setSummary(String summary) { this.summary = summary; }
        
        public String getFormattedDate() {
            if (date == null) return "";
            return DateTimeFormatter.ofPattern("MMM d").format(date);
        }
        
        @Override
        public String toString() {
            return String.format("DailyForecast[date=%s, high=%s, low=%s, precip=%s]", 
                date, temperatureHigh, temperatureLow, precipitation);
        }
    }
    
    /**
     * Weather alert data
     */
    public static class WeatherAlert {
        @SerializedName("title")
        private String title;
        
        @SerializedName("description")
        private String description;
        
        @SerializedName("severity")
        private String severity;
        
        @SerializedName("effective")
        private Instant effective;
        
        @SerializedName("expires")
        private Instant expires;
        
        @SerializedName("uri")
        private String uri;
        
        public String getTitle() { return title != null ? title : ""; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description != null ? description : ""; }
        public void setDescription(String description) { this.description = description; }
        
        public String getSeverity() { return severity != null ? severity : ""; }
        public void setSeverity(String severity) { this.severity = severity; }
        
        public @Nullable Instant getEffective() { return effective; }
        public void setEffective(Instant effective) { this.effective = effective; }
        
        public @Nullable Instant getExpires() { return expires; }
        public void setExpires(Instant expires) { this.expires = expires; }
        
        public String getUri() { return uri != null ? uri : ""; }
        public void setUri(String uri) { this.uri = uri; }
        
        public boolean isActive() {
            Instant now = Instant.now();
            return (effective == null || !effective.isAfter(now)) && 
                   (expires == null || expires.isAfter(now));
        }
        
        @Override
        public String toString() {
            return String.format("WeatherAlert[title=%s, severity=%s]", title, severity);
        }
    }
}
