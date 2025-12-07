package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO representing Rachio water usage and savings data
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioUsage {
    
    // Device identification
    @SerializedName("deviceId")
    private String deviceId = "";
    
    @SerializedName("generatedAt")
    private Instant generatedAt;
    
    // Period information
    @SerializedName("periodStart")
    private Instant periodStart;
    
    @SerializedName("periodEnd")
    private Instant periodEnd;
    
    @SerializedName("periodType")
    private String periodType = ""; // "DAY", "WEEK", "MONTH", "YEAR", "CUSTOM"
    
    // Total usage and savings (was causing compilation errors)
    @SerializedName("totalUsage")
    private Double totalUsage; // in cubic meters
    
    @SerializedName("totalSavings")
    private Double totalSavings; // in cubic meters
    
    // Additional metrics
    @SerializedName("baselineUsage")
    private Double baselineUsage; // in cubic meters
    
    @SerializedName("percentSavings")
    private Double percentSavings; // percentage
    
    @SerializedName("carbonSavings")
    private Double carbonSavings; // in kg CO2
    
    @SerializedName("moneySavings")
    private Double moneySavings; // in USD
    
    @SerializedName("energySavings")
    private Double energySavings; // in kWh
    
    // Environmental impact
    @SerializedName("treesSaved")
    private Double treesSaved; // number of trees
    
    @SerializedName("carMilesSaved")
    private Double carMilesSaved; // miles
    
    @SerializedName("showersSaved")
    private Double showersSaved; // number of showers
    
    @SerializedName("laundryLoadsSaved")
    private Double laundryLoadsSaved; // number of loads
    
    // Zone breakdown
    @SerializedName("zoneUsage")
    private List<ZoneUsage> zoneUsage = List.of();
    
    // Monthly breakdown
    @SerializedName("monthlyBreakdown")
    private List<MonthlyUsage> monthlyBreakdown = List.of();
    
    // Daily breakdown
    @SerializedName("dailyBreakdown")
    private List<DailyUsage> dailyBreakdown = List.of();
    
    // Weather impact
    @SerializedName("rainfall")
    private Double rainfall; // in inches or mm
    
    @SerializedName("evapotranspiration")
    private Double evapotranspiration; // in inches or mm
    
    @SerializedName("temperature")
    private Double temperature; // average temperature
    
    @SerializedName("weatherAdjustedSavings")
    private Double weatherAdjustedSavings; // in cubic meters
    
    // Units
    @SerializedName("units")
    private String units = "METRIC"; // "METRIC" or "US"
    
    // Currency
    @SerializedName("currency")
    private String currency = "USD";
    
    // Additional properties
    @SerializedName("properties")
    private Map<String, Object> properties = Map.of();
    
    // Getters and setters
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public @Nullable Instant getGeneratedAt() {
        return generatedAt;
    }
    
    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
    
    public @Nullable Instant getPeriodStart() {
        return periodStart;
    }
    
    public void setPeriodStart(Instant periodStart) {
        this.periodStart = periodStart;
    }
    
    public @Nullable Instant getPeriodEnd() {
        return periodEnd;
    }
    
    public void setPeriodEnd(Instant periodEnd) {
        this.periodEnd = periodEnd;
    }
    
    public String getPeriodType() {
        return periodType;
    }
    
    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }
    
    // FIXED: Removed @Nullable from method parameter (was causing compilation error)
    public @Nullable Double getTotalUsage() {
        return totalUsage;
    }
    
    public void setTotalUsage(Double totalUsage) {
        this.totalUsage = totalUsage;
    }
    
    // FIXED: Removed @Nullable from method parameter (was causing compilation error)
    public @Nullable Double getTotalSavings() {
        return totalSavings;
    }
    
    public void setTotalSavings(Double totalSavings) {
        this.totalSavings = totalSavings;
    }
    
    public @Nullable Double getBaselineUsage() {
        return baselineUsage;
    }
    
    public void setBaselineUsage(Double baselineUsage) {
        this.baselineUsage = baselineUsage;
    }
    
    public @Nullable Double getPercentSavings() {
        return percentSavings;
    }
    
    public void setPercentSavings(Double percentSavings) {
        this.percentSavings = percentSavings;
    }
    
    public @Nullable Double getCarbonSavings() {
        return carbonSavings;
    }
    
    public void setCarbonSavings(Double carbonSavings) {
        this.carbonSavings = carbonSavings;
    }
    
    public @Nullable Double getMoneySavings() {
        return moneySavings;
    }
    
    public void setMoneySavings(Double moneySavings) {
        this.moneySavings = moneySavings;
    }
    
    public @Nullable Double getEnergySavings() {
        return energySavings;
    }
    
    public void setEnergySavings(Double energySavings) {
        this.energySavings = energySavings;
    }
    
    public @Nullable Double getTreesSaved() {
        return treesSaved;
    }
    
    public void setTreesSaved(Double treesSaved) {
        this.treesSaved = treesSaved;
    }
    
    public @Nullable Double getCarMilesSaved() {
        return carMilesSaved;
    }
    
    public void setCarMilesSaved(Double carMilesSaved) {
        this.carMilesSaved = carMilesSaved;
    }
    
    public @Nullable Double getShowersSaved() {
        return showersSaved;
    }
    
    public void setShowersSaved(Double showersSaved) {
        this.showersSaved = showersSaved;
    }
    
    public @Nullable Double getLaundryLoadsSaved() {
        return laundryLoadsSaved;
    }
    
    public void setLaundryLoadsSaved(Double laundryLoadsSaved) {
        this.laundryLoadsSaved = laundryLoadsSaved;
    }
    
    public List<ZoneUsage> getZoneUsage() {
        return zoneUsage;
    }
    
    public void setZoneUsage(List<ZoneUsage> zoneUsage) {
        this.zoneUsage = zoneUsage;
    }
    
    public List<MonthlyUsage> getMonthlyBreakdown() {
        return monthlyBreakdown;
    }
    
    public void setMonthlyBreakdown(List<MonthlyUsage> monthlyBreakdown) {
        this.monthlyBreakdown = monthlyBreakdown;
    }
    
    public List<DailyUsage> getDailyBreakdown() {
        return dailyBreakdown;
    }
    
    public void setDailyBreakdown(List<DailyUsage> dailyBreakdown) {
        this.dailyBreakdown = dailyBreakdown;
    }
    
    public @Nullable Double getRainfall() {
        return rainfall;
    }
    
    public void setRainfall(Double rainfall) {
        this.rainfall = rainfall;
    }
    
    public @Nullable Double getEvapotranspiration() {
        return evapotranspiration;
    }
    
    public void setEvapotranspiration(Double evapotranspiration) {
        this.evapotranspiration = evapotranspiration;
    }
    
    public @Nullable Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    public @Nullable Double getWeatherAdjustedSavings() {
        return weatherAdjustedSavings;
    }
    
    public void setWeatherAdjustedSavings(Double weatherAdjustedSavings) {
        this.weatherAdjustedSavings = weatherAdjustedSavings;
    }
    
    public String getUnits() {
        return units;
    }
    
    public void setUnits(String units) {
        this.units = units;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
    
    // Helper methods
    public boolean isMetric() {
        return "METRIC".equalsIgnoreCase(units);
    }
    
    public String getVolumeUnit() {
        return isMetric() ? "m³" : "gal";
    }
    
    public String getLengthUnit() {
        return isMetric() ? "mm" : "in";
    }
    
    public String getTemperatureUnit() {
        return isMetric() ? "°C" : "°F";
    }
    
    public double getTotalUsageInGallons() {
        if (totalUsage == null) return 0.0;
        return isMetric() ? totalUsage * 264.172 : totalUsage; // m³ to gallons
    }
    
    public double getTotalSavingsInGallons() {
        if (totalSavings == null) return 0.0;
        return isMetric() ? totalSavings * 264.172 : totalSavings; // m³ to gallons
    }
    
    public double getRainfallInInches() {
        if (rainfall == null) return 0.0;
        return isMetric() ? rainfall / 25.4 : rainfall; // mm to inches
    }
    
    public double getEvapotranspirationInInches() {
        if (evapotranspiration == null) return 0.0;
        return isMetric() ? evapotranspiration / 25.4 : evapotranspiration; // mm to inches
    }
    
    public double getTemperatureInFahrenheit() {
        if (temperature == null) return 0.0;
        return isMetric() ? (temperature * 9.0 / 5.0) + 32.0 : temperature;
    }
    
    public @Nullable ZoneUsage getZoneUsageById(String zoneId) {
        for (ZoneUsage zone : zoneUsage) {
            if (zone.getZoneId().equals(zoneId)) {
                return zone;
            }
        }
        return null;
    }
    
    public double getTotalZoneRuntime() {
        double total = 0.0;
        for (ZoneUsage zone : zoneUsage) {
            total += zone.getRuntime();
        }
        return total;
    }
    
    public int getTotalWateringEvents() {
        int total = 0;
        for (ZoneUsage zone : zoneUsage) {
            total += zone.getWateringEvents();
        }
        return total;
    }
    
    public boolean hasSavings() {
        return totalSavings != null && totalSavings > 0.0;
    }
    
    public String getFormattedPeriod() {
        if (periodStart == null || periodEnd == null) {
            return "Unknown Period";
        }
        
        switch (periodType.toUpperCase()) {
            case "DAY":
                return DateTimeFormatter.ofPattern("MMMM d, yyyy").format(periodStart.atZone(java.time.ZoneId.systemDefault()));
            case "WEEK":
                return String.format("Week of %s", 
                    DateTimeFormatter.ofPattern("MMMM d").format(periodStart.atZone(java.time.ZoneId.systemDefault())));
            case "MONTH":
                return DateTimeFormatter.ofPattern("MMMM yyyy").format(periodStart.atZone(java.time.ZoneId.systemDefault()));
            case "YEAR":
                return DateTimeFormatter.ofPattern("yyyy").format(periodStart.atZone(java.time.ZoneId.systemDefault()));
            default:
                return String.format("%s to %s", 
                    DateTimeFormatter.ofPattern("MMM d").format(periodStart.atZone(java.time.ZoneId.systemDefault())),
                    DateTimeFormatter.ofPattern("MMM d, yyyy").format(periodEnd.atZone(java.time.ZoneId.systemDefault())));
        }
    }
    
    @Override
    public String toString() {
        return String.format("RachioUsage[deviceId=%s, period=%s, usage=%.2f%s, savings=%.2f%s]", 
            deviceId, getFormattedPeriod(), totalUsage, getVolumeUnit(), totalSavings, getVolumeUnit());
    }
    
    /**
     * Zone-specific usage data
     */
    public static class ZoneUsage {
        @SerializedName("zoneId")
        private String zoneId = "";
        
        @SerializedName("zoneName")
        private String zoneName = "";
        
        @SerializedName("zoneNumber")
        private int zoneNumber = 0;
        
        @SerializedName("usage")
        private Double usage; // in cubic meters
        
        @SerializedName("savings")
        private Double savings; // in cubic meters
        
        @SerializedName("baselineUsage")
        private Double baselineUsage; // in cubic meters
        
        @SerializedName("percentSavings")
        private Double percentSavings; // percentage
        
        @SerializedName("runtime")
        private Double runtime; // in seconds
        
        @SerializedName("wateringEvents")
        private Integer wateringEvents = 0;
        
        @SerializedName("averageRuntime")
        private Double averageRuntime; // in seconds
        
        @SerializedName("lastWatered")
        private Instant lastWatered;
        
        public String getZoneId() { return zoneId; }
        public void setZoneId(String zoneId) { this.zoneId = zoneId; }
        
        public String getZoneName() { return zoneName; }
        public void setZoneName(String zoneName) { this.zoneName = zoneName; }
        
        public int getZoneNumber() { return zoneNumber; }
        public void setZoneNumber(int zoneNumber) { this.zoneNumber = zoneNumber; }
        
        public @Nullable Double getUsage() { return usage; }
        public void setUsage(Double usage) { this.usage = usage; }
        
        public @Nullable Double getSavings() { return savings; }
        public void setSavings(Double savings) { this.savings = savings; }
        
        public @Nullable Double getBaselineUsage() { return baselineUsage; }
        public void setBaselineUsage(Double baselineUsage) { this.baselineUsage = baselineUsage; }
        
        public @Nullable Double getPercentSavings() { return percentSavings; }
        public void setPercentSavings(Double percentSavings) { this.percentSavings = percentSavings; }
        
        public double getRuntime() { return runtime != null ? runtime : 0.0; }
        public void setRuntime(Double runtime) { this.runtime = runtime; }
        
        public int getWateringEvents() { return wateringEvents != null ? wateringEvents : 0; }
        public void setWateringEvents(Integer wateringEvents) { this.wateringEvents = wateringEvents; }
        
        public @Nullable Double getAverageRuntime() { return averageRuntime; }
        public void setAverageRuntime(Double averageRuntime) { this.averageRuntime = averageRuntime; }
        
        public @Nullable Instant getLastWatered() { return lastWatered; }
        public void setLastWatered(Instant lastWatered) { this.lastWatered = lastWatered; }
        
        @Override
        public String toString() {
            return String.format("ZoneUsage[zone=%s, usage=%.2f, savings=%.2f]", 
                zoneName, usage, savings);
        }
    }
    
    /**
     * Monthly usage breakdown
     */
    public static class MonthlyUsage {
        @SerializedName("month")
        private YearMonth month;
        
        @SerializedName("usage")
        private Double usage; // in cubic meters
        
        @SerializedName("savings")
        private Double savings; // in cubic meters
        
        @SerializedName("rainfall")
        private Double rainfall; // in inches or mm
        
        @SerializedName("wateringEvents")
        private Integer wateringEvents = 0;
        
        public @Nullable YearMonth getMonth() { return month; }
        public void setMonth(YearMonth month) { this.month = month; }
        
        public @Nullable Double getUsage() { return usage; }
        public void setUsage(Double usage) { this.usage = usage; }
        
        public @Nullable Double getSavings() { return savings; }
        public void setSavings(Double savings) { this.savings = savings; }
        
        public @Nullable Double getRainfall() { return rainfall; }
        public void setRainfall(Double rainfall) { this.rainfall = rainfall; }
        
        public int getWateringEvents() { return wateringEvents != null ? wateringEvents : 0; }
        public void setWateringEvents(Integer wateringEvents) { this.wateringEvents = wateringEvents; }
        
        public String getMonthName() {
            if (month == null) return "";
            return month.format(DateTimeFormatter.ofPattern("MMM yyyy"));
        }
        
        @Override
        public String toString() {
            return String.format("MonthlyUsage[month=%s, usage=%.2f, savings=%.2f]", 
                getMonthName(), usage, savings);
        }
    }
    
    /**
     * Daily usage breakdown
     */
    public static class DailyUsage {
        @SerializedName("date")
        private LocalDate date;
        
        @SerializedName("usage")
        private Double usage; // in cubic meters
        
        @SerializedName("savings")
        private Double savings; // in cubic meters
        
        @SerializedName("rainfall")
        private Double rainfall; // in inches or mm
        
        @SerializedName("temperature")
        private Double temperature; // average temperature
        
        @SerializedName("wateringEvents")
        private Integer wateringEvents = 0;
        
        @SerializedName("runtime")
        private Double runtime; // in seconds
        
        public @Nullable LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        
        public @Nullable Double getUsage() { return usage; }
        public void setUsage(Double usage) { this.usage = usage; }
        
        public @Nullable Double getSavings() { return savings; }
        public void setSavings(Double savings) { this.savings = savings; }
        
        public @Nullable Double getRainfall() { return rainfall; }
        public void setRainfall(Double rainfall) { this.rainfall = rainfall; }
        
        public @Nullable Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        
        public int getWateringEvents() { return wateringEvents != null ? wateringEvents : 0; }
        public void setWateringEvents(Integer wateringEvents) { this.wateringEvents = wateringEvents; }
        
        public double getRuntime() { return runtime != null ? runtime : 0.0; }
        public void setRuntime(Double runtime) { this.runtime = runtime; }
        
        public String getFormattedDate() {
            if (date == null) return "";
            return date.format(DateTimeFormatter.ofPattern("MMM d"));
        }
        
        @Override
        public String toString() {
            return String.format("DailyUsage[date=%s, usage=%.2f, rainfall=%.2f]", 
                getFormattedDate(), usage, rainfall);
        }
    }
}
