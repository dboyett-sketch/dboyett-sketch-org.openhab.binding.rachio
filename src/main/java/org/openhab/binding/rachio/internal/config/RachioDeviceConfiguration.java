package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link RachioDeviceConfiguration} class contains fields mapping device configuration parameters.
 *
 * @author Brian G. - Initial contribution (from 2.5 binding)
 * @author Daniel B. - Major rewrite for OpenHAB 5.x
 */
@NonNullByDefault
public class RachioDeviceConfiguration {
    // Required: Device ID from Rachio
    public String deviceId = "";

    // Optional: Device name override
    public @Nullable String deviceName;

    // Optional: Polling configuration
    public int pollingInterval = 120; // seconds
    public boolean autoRefresh = true;

    // Optional: Professional features
    public boolean monitorForecast = true;
    public int forecastUpdateInterval = 3600; // seconds
    public boolean monitorWaterUsage = true;
    public int waterUsageUpdateInterval = 7200; // seconds
    public boolean monitorSavings = true;
    public boolean monitorAlerts = true;
    public int alertsUpdateInterval = 300; // seconds
    public boolean monitorSchedules = true;

    // Optional: Control settings
    public int defaultZoneRuntime = 600; // seconds (10 minutes)
    public int maxZoneRuntime = 7200; // seconds (2 hours)
    public int minZoneRuntime = 60; // seconds (1 minute)
    public boolean confirmStopWatering = true;
    public boolean confirmRainDelay = true;

    // Optional: Display settings
    public @Nullable String displayUnits; // "imperial" or "metric"
    public boolean showAdvancedChannels = false;
    public boolean groupByCategory = true;

    // Optional: Notification settings
    public boolean notifyZoneStart = false;
    public boolean notifyZoneComplete = false;
    public boolean notifyRainDelay = true;
    public boolean notifyDeviceOffline = true;
    public boolean notifyAlerts = true;

    @Override
    public String toString() {
        return "RachioDeviceConfiguration [deviceId=" + deviceId +
                ", deviceName=" + deviceName +
                ", pollingInterval=" + pollingInterval +
                ", monitorForecast=" + monitorForecast +
                ", monitorWaterUsage=" + monitorWaterUsage +
                "]";
    }
}
