package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link RachioSavings} class defines savings data from the Rachio API
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioSavings {

    @SerializedName("yearlySavings")
    private @Nullable Double yearlySavings;

    @SerializedName("monthlySavings")
    private @Nullable Double monthlySavings;

    @SerializedName("dailySavings")
    private @Nullable Double dailySavings;

    @SerializedName("totalSavings")
    private @Nullable Double totalSavings;

    @SerializedName("waterSaved")
    private @Nullable Double waterSaved;

    @SerializedName("moneySaved")
    private @Nullable Double moneySaved;

    @SerializedName("currency")
    private @Nullable String currency;

    @SerializedName("units")
    private @Nullable String units;

    @SerializedName("period")
    private @Nullable String period;

    @SerializedName("startDate")
    private @Nullable String startDate;

    @SerializedName("endDate")
    private @Nullable String endDate;

    @SerializedName("deviceId")
    private @Nullable String deviceId;

    @SerializedName("deviceName")
    private @Nullable String deviceName;

    @SerializedName("zoneCount")
    private @Nullable Integer zoneCount;

    @SerializedName("averageRuntime")
    private @Nullable Double averageRuntime;

    @SerializedName("averageWaterUsage")
    private @Nullable Double averageWaterUsage;

    // Getters and setters

    public @Nullable Double getYearlySavings() {
        return yearlySavings;
    }

    public void setYearlySavings(@Nullable Double yearlySavings) {
        this.yearlySavings = yearlySavings;
    }

    public @Nullable Double getMonthlySavings() {
        return monthlySavings;
    }

    public void setMonthlySavings(@Nullable Double monthlySavings) {
        this.monthlySavings = monthlySavings;
    }

    public @Nullable Double getDailySavings() {
        return dailySavings;
    }

    public void setDailySavings(@Nullable Double dailySavings) {
        this.dailySavings = dailySavings;
    }

    public @Nullable Double getTotalSavings() {
        return totalSavings;
    }

    public void setTotalSavings(@Nullable Double totalSavings) {
        this.totalSavings = totalSavings;
    }

    public @Nullable Double getWaterSaved() {
        return waterSaved;
    }

    public void setWaterSaved(@Nullable Double waterSaved) {
        this.waterSaved = waterSaved;
    }

    public @Nullable Double getMoneySaved() {
        return moneySaved;
    }

    public void setMoneySaved(@Nullable Double moneySaved) {
        this.moneySaved = moneySaved;
    }

    public @Nullable String getCurrency() {
        return currency;
    }

    public void setCurrency(@Nullable String currency) {
        this.currency = currency;
    }

    public @Nullable String getUnits() {
        return units;
    }

    public void setUnits(@Nullable String units) {
        this.units = units;
    }

    public @Nullable String getPeriod() {
        return period;
    }

    public void setPeriod(@Nullable String period) {
        this.period = period;
    }

    public @Nullable String getStartDate() {
        return startDate;
    }

    public void setStartDate(@Nullable String startDate) {
        this.startDate = startDate;
    }

    public @Nullable String getEndDate() {
        return endDate;
    }

    public void setEndDate(@Nullable String endDate) {
        this.endDate = endDate;
    }

    public @Nullable String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(@Nullable String deviceId) {
        this.deviceId = deviceId;
    }

    public @Nullable String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(@Nullable String deviceName) {
        this.deviceName = deviceName;
    }

    public @Nullable Integer getZoneCount() {
        return zoneCount;
    }

    public void setZoneCount(@Nullable Integer zoneCount) {
        this.zoneCount = zoneCount;
    }

    public @Nullable Double getAverageRuntime() {
        return averageRuntime;
    }

    public void setAverageRuntime(@Nullable Double averageRuntime) {
        this.averageRuntime = averageRuntime;
    }

    public @Nullable Double getAverageWaterUsage() {
        return averageWaterUsage;
    }

    public void setAverageWaterUsage(@Nullable Double averageWaterUsage) {
        this.averageWaterUsage = averageWaterUsage;
    }

    @Override
    public String toString() {
        return "RachioSavings{" + "yearlySavings=" + yearlySavings + ", monthlySavings=" + monthlySavings
                + ", dailySavings=" + dailySavings + ", totalSavings=" + totalSavings + ", waterSaved=" + waterSaved
                + ", moneySaved=" + moneySaved + ", currency='" + currency + '\'' + ", units='" + units + '\''
                + ", deviceId='" + deviceId + '\'' + '}';
    }
}
