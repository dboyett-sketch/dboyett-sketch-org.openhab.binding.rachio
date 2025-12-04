package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link RachioZoneConfiguration} class contains fields mapping zone configuration parameters.
 *
 * @author Brian G. - Initial contribution (from 2.5 binding)
 * @author Daniel B. - Major rewrite for OpenHAB 5.x
 */
@NonNullByDefault
public class RachioZoneConfiguration {
    // Required: Zone ID from Rachio
    public String zoneId = "";

    // Required: Parent device ID
    public String deviceId = "";

    // Optional: Zone number (1-16)
    public int zoneNumber = 0;

    // Optional: Zone name override
    public @Nullable String zoneName;

    // Optional: Runtime settings
    public int defaultRuntime = 600; // seconds (10 minutes)
    public int maxRuntime = 7200; // seconds (2 hours)
    public int minRuntime = 60; // seconds (1 minute)
    public boolean enabled = true;

    // Optional: Professional irrigation settings (overrides from Rachio)
    public @Nullable String soilType;
    public @Nullable String cropType;
    public @Nullable Double cropCoefficient;
    public @Nullable String nozzleType;
    public @Nullable Double nozzleRate; // inches per hour
    public @Nullable String slopeType;
    public @Nullable String shadeType;
    public @Nullable Double rootDepth; // inches
    public @Nullable Double efficiency; // 0.0-1.0
    public @Nullable Double area; // square feet

    // Optional: Water adjustment runtimes (levels 1-5)
    public @Nullable Integer waterAdjustment1;
    public @Nullable Integer waterAdjustment2;
    public @Nullable Integer waterAdjustment3;
    public @Nullable Integer waterAdjustment4;
    public @Nullable Integer waterAdjustment5;

    // Optional: Monitoring settings
    public boolean monitorStatus = true;
    public boolean showProfessionalData = true;
    public boolean trackWateringHistory = false;
    public int historyDays = 30;

    // Optional: Control settings
    public boolean allowManualStart = true;
    public boolean allowManualStop = true;
    public boolean confirmStart = false;
    public boolean confirmStop = false;

    // Optional: Display settings
    public @Nullable String displayName;
    public @Nullable String displayIcon;
    public boolean showInDashboard = true;
    public int dashboardOrder = 0;

    // Optional: Automation settings
    public boolean enableAutomation = false;
    public @Nullable String automationRules;
    public boolean linkToWeather = false;
    public boolean linkToSoilMoisture = false;

    @Override
    public String toString() {
        return "RachioZoneConfiguration [zoneId=" + zoneId +
                ", deviceId=" + deviceId +
                ", zoneNumber=" + zoneNumber +
                ", defaultRuntime=" + defaultRuntime +
                ", enabled=" + enabled +
                ", soilType=" + soilType +
                ", cropType=" + cropType +
                "]";
    }

    /**
     * Get water adjustment runtime for a specific level (1-5)
     */
    public @Nullable Integer getWaterAdjustment(int level) {
        switch (level) {
            case 1: return waterAdjustment1;
            case 2: return waterAdjustment2;
            case 3: return waterAdjustment3;
            case 4: return waterAdjustment4;
            case 5: return waterAdjustment5;
            default: return null;
        }
    }

    /**
     * Set water adjustment runtime for a specific level (1-5)
     */
    public void setWaterAdjustment(int level, @Nullable Integer value) {
        switch (level) {
            case 1: waterAdjustment1 = value; break;
            case 2: waterAdjustment2 = value; break;
            case 3: waterAdjustment3 = value; break;
            case 4: waterAdjustment4 = value; break;
            case 5: waterAdjustment5 = value; break;
        }
    }

    /**
     * Check if professional irrigation data is configured
     */
    public boolean hasProfessionalData() {
        return soilType != null || cropType != null || cropCoefficient != null ||
               nozzleType != null || nozzleRate != null || slopeType != null ||
               shadeType != null || rootDepth != null || efficiency != null || area != null;
    }

    /**
     * Get all water adjustments as an array
     */
    public Integer[] getWaterAdjustments() {
        return new Integer[] {
            waterAdjustment1, waterAdjustment2, waterAdjustment3,
            waterAdjustment4, waterAdjustment5
        };
    }
}
