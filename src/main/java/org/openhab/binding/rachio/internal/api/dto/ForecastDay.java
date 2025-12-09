package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public class ForecastDay {
    public @Nullable Double temp; // Temperature in Celsius
    public @Nullable Double precip; // Precipitation in mm
    public @Nullable Double humidity; // Humidity percentage
    public @Nullable Double wind; // Wind speed in km/h
    public @Nullable Double solar; // Solar radiation in W/m²
    public @Nullable Double et; // Evapotranspiration in mm
    
    public @Nullable Double getTemp() { return temp; }
    public @Nullable Double getPrecip() { return precip; }
    public @Nullable Double getHumidity() { return humidity; }
    public @Nullable Double getWind() { return wind; }
    public @Nullable Double getSolar() { return solar; }
    public @Nullable Double getEt() { return et; }
}
