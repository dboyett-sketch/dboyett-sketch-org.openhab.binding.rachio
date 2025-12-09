package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * DTO for Weather Forecast
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioForecast {
    @Nullable
    public Instant generatedAt;
    
    @Nullable
    public String units; // US, METRIC
    
    @Nullable
    public List<ForecastDay> days;
    
    @Nullable
    public Double currentTemperature;
    
    @Nullable
    public Double currentHumidity;
    
    @Nullable
    public Double currentWindSpeed;
    
    @Nullable
    public Double currentPrecipitation;
    
    @Nullable
    public Double evapotranspiration; // ET value for watering calculations
    
    @Override
    public String toString() {
        return "RachioForecast [generatedAt=" + generatedAt + ", days=" + 
               (days != null ? days.size() : 0) + ", evapotranspiration=" + evapotranspiration + "]";
    }
}
