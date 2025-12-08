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
    
    @SerializedName("model")
    private String model = "";
    
    @SerializedName("serialNumber")
    private String serialNumber = "";
    
    @SerializedName("macAddress")
    private String macAddress = "";
    
    @SerializedName("status")
    private String status = "";
    
    @SerializedName("zones")
    @Nullable
    private List<RachioZone> zones;
    
    @SerializedName("latitude")
    private double latitude;
    
    @SerializedName("longitude")
    private double longitude;
    
    @SerializedName("elevation")
    private double elevation;
    
    @SerializedName("timeZone")
    private String timeZone = "";
    
    @SerializedName("flexScheduleRules")
    private boolean flexScheduleRules;
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getModel() { return model; }
    public String getSerialNumber() { return serialNumber; }
    public String getMacAddress() { return macAddress; }
    public String getStatus() { return status; }
    @Nullable
    public List<RachioZone> getZones() { return zones; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getElevation() { return elevation; }
    public String getTimeZone() { return timeZone; }
    public boolean isFlexScheduleRules() { return flexScheduleRules; }
    
    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setModel(String model) { this.model = model; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
    public void setStatus(String status) { this.status = status; }
    public void setZones(@Nullable List<RachioZone> zones) { this.zones = zones; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setElevation(double elevation) { this.elevation = elevation; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
    public void setFlexScheduleRules(boolean flexScheduleRules) { this.flexScheduleRules = flexScheduleRules; }
}
