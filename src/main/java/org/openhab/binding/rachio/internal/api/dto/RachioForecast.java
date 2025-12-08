package org.openhab.binding.rachio.internal.api.dto;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class RachioForecast {
    private List<ForecastDay> forecast;
    
    public List<ForecastDay> getForecast() { return forecast; }
    public void setForecast(List<ForecastDay> forecast) { this.forecast = forecast; }
}
