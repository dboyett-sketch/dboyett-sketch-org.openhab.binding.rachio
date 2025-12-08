package org.openhab.binding.rachio.internal.api.dto;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

@NonNullByDefault
public class RachioForecast {
    @SerializedName("summary")
    public String summary = "";
    
    @SerializedName("daily")
    public @Nullable List<ForecastDay> daily;
    
    // Current weather fields
    @SerializedName("currentTemp")
    public double currentTemp;
    
    @SerializedName("currentPrecip")
    public double currentPrecip;
    
    @SerializedName("currentHumidity")
    public double currentHumidity;
    
    @SerializedName("currentEt")
    public double currentEt;
    
    @SerializedName("currentWindSpeed")
    public double currentWindSpeed;
    
    @SerializedName("currentWindGust")
    public double currentWindGust;
    
    @SerializedName("currentWindDirection")
    public double currentWindDirection;
    
    @SerializedName("currentConditions")
    public String currentConditions = "";
    
    @SerializedName("currentIcon")
    public String currentIcon = "";
    
    @SerializedName("currentDateTimeEpoch")
    public long currentDateTimeEpoch;
    
    // Default constructor for Gson
    public RachioForecast() {
    }
}
