package org.openhab.binding.rachio.internal.api.dto;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

@NonNullByDefault
public class RachioDevice {
    @SerializedName("id")
    private String id = "";

    @SerializedName("name")
    private String name = "";

    @SerializedName("status")
    private String status = "";

    @SerializedName("serialNumber")
    private String serialNumber = "";

    @SerializedName("model")
    private String model = "";

    @SerializedName("zones")
    private List<RachioZone> zones = List.of();

    @SerializedName("on")
    private boolean on = false;

    @SerializedName("scheduleRules")
    @Nullable
    private List<Object> scheduleRules;

    @SerializedName("flexScheduleRules")
    @Nullable
    private List<Object> flexScheduleRules;

    @SerializedName("latitude")
    private double latitude = 0.0;

    @SerializedName("longitude")
    private double longitude = 0.0;

    @SerializedName("timeZone")
    private String timeZone = "";

    @SerializedName("createdAt")
    private long createdAt = 0;

    @SerializedName("updatedAt")
    private long updatedAt = 0;

    @SerializedName("scheduleModeType")
    @Nullable
    private String scheduleModeType;

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getModel() {
        return model;
    }

    public List<RachioZone> getZones() {
        return zones;
    }

    public boolean isOn() {
        return on;
    }

    @Nullable
    public List<Object> getScheduleRules() {
        return scheduleRules;
    }

    @Nullable
    public List<Object> getFlexScheduleRules() {
        return flexScheduleRules;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    @Nullable
    public String getScheduleModeType() {
        return scheduleModeType;
    }
}
