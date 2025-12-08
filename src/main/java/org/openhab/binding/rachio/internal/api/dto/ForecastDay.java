package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

@NonNullByDefault
public class ForecastDay {
    @SerializedName("day")
    private int day;
    
    @SerializedName("precipIntensity")
    private double precipIntensity;
    
    @SerializedName("precipProbability")
    private double precipProbability;
    
    @SerializedName("precipType")
    private String precipType = "";
    
    @SerializedName("temperatureHigh")
    private double temperatureHigh;
    
    @SerializedName("temperatureLow")
    private double temperatureLow;
    
    @SerializedName("humidity")
    private double humidity;
    
    @SerializedName("windSpeed")
    private double windSpeed;
    
    @SerializedName("windGust")
    private double windGust;
    
    @SerializedName("windBearing")
    private int windBearing;
    
    @SerializedName("cloudCover")
    private double cloudCover;
    
    @SerializedName("pressure")
    private double pressure;
    
    @SerializedName("dewPoint")
    private double dewPoint;
    
    @SerializedName("ozone")
    private double ozone;
    
    @SerializedName("uvIndex")
    private int uvIndex;
    
    @SerializedName("sunriseTime")
    private Instant sunriseTime;
    
    @SerializedName("sunsetTime")
    private Instant sunsetTime;
    
    @SerializedName("moonPhase")
    private double moonPhase;
    
    @SerializedName("summary")
    private String summary = "";
    
    @SerializedName("icon")
    private String icon = "";
    
    @SerializedName("temperatureMin")
    private double temperatureMin;
    
    @SerializedName("temperatureMax")
    private double temperatureMax;
    
    @SerializedName("apparentTemperatureHigh")
    private double apparentTemperatureHigh;
    
    @SerializedName("apparentTemperatureLow")
    private double apparentTemperatureLow;
    
    @SerializedName("apparentTemperatureMin")
    private double apparentTemperatureMin;
    
    @SerializedName("apparentTemperatureMax")
    private double apparentTemperatureMax;
    
    // Getters
    public int getDay() { return day; }
    public double getPrecipIntensity() { return precipIntensity; }
    public double getPrecipProbability() { return precipProbability; }
    public String getPrecipType() { return precipType; }
    public double getTemperatureHigh() { return temperatureHigh; }
    public double getTemperatureLow() { return temperatureLow; }
    public double getHumidity() { return humidity; }
    public double getWindSpeed() { return windSpeed; }
    public double getWindGust() { return windGust; }
    public int getWindBearing() { return windBearing; }
    public double getCloudCover() { return cloudCover; }
    public double getPressure() { return pressure; }
    public double getDewPoint() { return dewPoint; }
    public double getOzone() { return ozone; }
    public int getUvIndex() { return uvIndex; }
    public Instant getSunriseTime() { return sunriseTime; }
    public Instant getSunsetTime() { return sunsetTime; }
    public double getMoonPhase() { return moonPhase; }
    public String getSummary() { return summary; }
    public String getIcon() { return icon; }
    public double getTemperatureMin() { return temperatureMin; }
    public double getTemperatureMax() { return temperatureMax; }
    public double getApparentTemperatureHigh() { return apparentTemperatureHigh; }
    public double getApparentTemperatureLow() { return apparentTemperatureLow; }
    public double getApparentTemperatureMin() { return apparentTemperatureMin; }
    public double getApparentTemperatureMax() { return apparentTemperatureMax; }
}
