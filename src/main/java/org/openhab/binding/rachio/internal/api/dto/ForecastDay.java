package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for individual forecast day
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class ForecastDay {
    @Nullable
    public Instant date;

    @Nullable
    public Double highTemperature;

    @Nullable
    public Double lowTemperature;

    @Nullable
    public Double precipitation;

    @Nullable
    public Double precipitationProbability;

    @Nullable
    public Double evapotranspiration;

    @Nullable
    public Boolean smartSkip; // whether watering should be skipped

    @Nullable
    public String condition; // SUNNY, CLOUDY, RAIN, etc.

    @Override
    public String toString() {
        return "ForecastDay [date=" + date + ", high=" + highTemperature + ", low=" + lowTemperature
                + ", precipitation=" + precipitation + ", smartSkip=" + smartSkip + "]";
    }
}
