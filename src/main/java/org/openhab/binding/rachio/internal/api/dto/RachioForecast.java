package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

@NonNullByDefault
public class RachioForecast {
    public String deviceId;
    public String summary;
    public List<ForecastDay> forecast;
    
    public static class ForecastDay {
        @SerializedName("day")
        public String day;
        
        @SerializedName("precip")
        public double precipitation;
        
        @SerializedName("temp")
        public double temperature;
        
        @SerializedName("humidity")
        public double humidity;
        
        @SerializedName("wind")
        public double windSpeed;
        
        @SerializedName("et")
        public double evapotranspiration;
        
        @SerializedName("skip")
        public boolean skip;
        
        @SerializedName("skipReason")
        public @Nullable String skipReason;
    }
}
