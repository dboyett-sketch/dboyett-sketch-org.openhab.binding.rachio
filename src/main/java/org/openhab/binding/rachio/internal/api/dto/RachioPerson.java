package org.openhab.binding.rachio.internal.api.dto;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for Rachio Person
 *
 * @author Damion Boyett - Enhanced with professional features
 */
@NonNullByDefault
public class RachioPerson {
    @SerializedName("id")
    public String id = "";
    
    @SerializedName("username")
    public String username = "";
    
    @SerializedName("email")
    public String email = "";
    
    @SerializedName("fullName")
    public String fullName = "";
    
    @SerializedName("createdDate")
    public @Nullable String createdDate;
    
    @SerializedName("devices")
    public @Nullable List<RachioDevice> devices;
    
    // Additional professional fields
    @SerializedName("timeZone")
    public @Nullable String timeZone;
    
    @SerializedName("language")
    public @Nullable String language;
    
    @SerializedName("units")
    public @Nullable String units;
    
    // Professional irrigation settings
    @SerializedName("enableRules")
    public @Nullable Boolean enableRules;
    
    @SerializedName("flexScheduleRules")
    public @Nullable Boolean flexScheduleRules;
    
    // Add getters for all fields
    public String getId() {
        return id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    @Nullable
    public String getCreatedDate() {
        return createdDate;
    }
    
    @Nullable
    public List<RachioDevice> getDevices() {
        return devices;
    }
    
    @Nullable
    public String getTimeZone() {
        return timeZone;
    }
    
    @Nullable
    public String getLanguage() {
        return language;
    }
    
    @Nullable
    public String getUnits() {
        return units;
    }
    
    @Nullable
    public Boolean getEnableRules() {
        return enableRules;
    }
    
    @Nullable
    public Boolean getFlexScheduleRules() {
        return flexScheduleRules;
    }
    
    // Setters for mutable fields
    public void setDevices(@Nullable List<RachioDevice> devices) {
        this.devices = devices;
    }
    
    public void setTimeZone(@Nullable String timeZone) {
        this.timeZone = timeZone;
    }
    
    public void setUnits(@Nullable String units) {
        this.units = units;
    }
    
    // Utility methods for device access
    @Nullable
    public RachioDevice getFirstDevice() {
        if (devices != null && !devices.isEmpty()) {
            return devices.get(0);
        }
        return null;
    }
    
    public int getTotalZoneCount() {
        int total = 0;
        if (devices != null) {
            for (RachioDevice device : devices) {
                // Use getZones() instead of direct field access
                List<RachioZone> zones = device.getZones();
                if (zones != null) {
                    total += zones.size();
                }
            }
        }
        return total;
    }
    
    public int getDeviceCount() {
        if (devices != null) {
            return devices.size();
        }
        return 0;
    }
    
    @Nullable
    public RachioDevice getDeviceById(String deviceId) {
        if (devices != null) {
            for (RachioDevice device : devices) {
                if (deviceId.equals(device.getId())) {
                    return device;
                }
            }
        }
        return null;
    }
    
    @Nullable
    public RachioZone getZoneById(String zoneId) {
        if (devices != null) {
            for (RachioDevice device : devices) {
                List<RachioZone> zones = device.getZones();
                if (zones != null) {
                    for (RachioZone zone : zones) {
                        if (zoneId.equals(zone.getId())) {
                            return zone;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    @Nullable
    public String getDeviceIdForZone(String zoneId) {
        if (devices != null) {
            for (RachioDevice device : devices) {
                List<RachioZone> zones = device.getZones();
                if (zones != null) {
                    for (RachioZone zone : zones) {
                        if (zoneId.equals(zone.getId())) {
                            return device.getId();
                        }
                    }
                }
            }
        }
        return null;
    }
    
    public boolean hasDevices() {
        return devices != null && !devices.isEmpty();
    }
    
    public boolean hasZones() {
        if (devices != null) {
            for (RachioDevice device : devices) {
                List<RachioZone> zones = device.getZones();
                if (zones != null && !zones.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // Professional method for user preferences
    public boolean usesMetric() {
        return "METRIC".equals(units);
    }
    
    public boolean usesImperial() {
        return "IMPERIAL".equals(units);
    }
    
    // Professional method for timezone handling
    @Nullable
    public String getTimeZoneOffset() {
        if (timeZone != null && timeZone.contains(" ")) {
            String[] parts = timeZone.split(" ");
            if (parts.length > 1) {
                return parts[parts.length - 1];
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "RachioPerson{id='" + id + "', username='" + username + "', email='" + email + 
               "', fullName='" + fullName + "', deviceCount=" + getDeviceCount() + 
               ", zoneCount=" + getTotalZoneCount() + ", timeZone='" + timeZone + 
               "', units='" + units + "', enableRules=" + enableRules + 
               ", flexScheduleRules=" + flexScheduleRules + "}";
    }
}
