package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * DTO for Rachio Person from Rachio API
 * Based on: GET /person/info endpoint
 * 
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioPerson {
    // Person identification
    public String id;
    
    @SerializedName("username")
    public String username;
    
    @SerializedName("fullName")
    public String fullName;
    
    @SerializedName("email")
    public String email;
    
    // Account information
    @SerializedName("createdAt")
    public String createdAt;
    
    @SerializedName("updatedAt")
    public String updatedAt;
    
    @SerializedName("deleted")
    public boolean deleted;
    
    // Account status
    @SerializedName("verified")
    public boolean verified;
    
    @SerializedName("disabled")
    public boolean disabled;
    
    // Billing information
    @SerializedName("hasPaymentInfo")
    public boolean hasPaymentInfo;
    
    @SerializedName("subscriptionPlan")
    public @Nullable String subscriptionPlan;
    
    @SerializedName("subscriptionStatus")
    public @Nullable String subscriptionStatus;
    
    @SerializedName("subscriptionExpiration")
    public @Nullable String subscriptionExpiration;
    
    // Account limits
    @SerializedName("maxDevices")
    public int maxDevices;
    
    @SerializedName("maxZones")
    public int maxZones;
    
    @SerializedName("maxSchedules")
    public int maxSchedules;
    
    // Devices associated with this person
    @SerializedName("devices")
    public @Nullable List<RachioDevice> devices;
    
    // Address information
    @SerializedName("streetAddress")
    public @Nullable String streetAddress;
    
    @SerializedName("city")
    public @Nullable String city;
    
    @SerializedName("state")
    public @Nullable String state;
    
    @SerializedName("zip")
    public @Nullable String zip;
    
    @SerializedName("country")
    public @Nullable String country;
    
    // Preferences
    @SerializedName("units")
    public String units = "US"; // US or METRIC
    
    @SerializedName("language")
    public String language = "en";
    
    @SerializedName("timeZone")
    public String timeZone;
    
    // API access
    @SerializedName("apiKey")
    public @Nullable String apiKey;
    
    @SerializedName("apiKeyCreatedAt")
    public @Nullable String apiKeyCreatedAt;
    
    @SerializedName("apiKeyLastUsed")
    public @Nullable String apiKeyLastUsed;
    
    // Helper methods
    
    /**
     * Get device count
     */
    public int getDeviceCount() {
        return devices != null ? devices.size() : 0;
    }
    
    /**
     * Get total zone count across all devices
     */
    public int getTotalZoneCount() {
        if (devices == null) return 0;
        
        int total = 0;
        for (RachioDevice device : devices) {
            total += device.getZoneCount();
        }
        return total;
    }
    
    /**
     * Get enabled zone count across all devices
     */
    public int getEnabledZoneCount() {
        if (devices == null) return 0;
        
        int total = 0;
        for (RachioDevice device : devices) {
            total += device.getEnabledZoneCount();
        }
        return total;
    }
    
    /**
     * Find device by ID
     */
    public @Nullable RachioDevice findDevice(String deviceId) {
        if (devices == null) return null;
        
        for (RachioDevice device : devices) {
            if (deviceId.equals(device.id)) {
                return device;
            }
        }
        return null;
    }
    
    /**
     * Find device by name
     */
    public @Nullable RachioDevice findDeviceByName(String deviceName) {
        if (devices == null) return null;
        
        for (RachioDevice device : devices) {
            if (deviceName.equals(device.name)) {
                return device;
            }
        }
        return null;
    }
    
    /**
     * Get all zone IDs across all devices
     */
    public List<String> getAllZoneIds() {
        List<String> zoneIds = new java.util.ArrayList<>();
        
        if (devices != null) {
            for (RachioDevice device : devices) {
                if (device.zones != null) {
                    for (RachioZone zone : device.zones) {
                        zoneIds.add(zone.id);
                    }
                }
            }
        }
        
        return zoneIds;
    }
    
    /**
     * Get all zones across all devices
     */
    public List<RachioZone> getAllZones() {
        List<RachioZone> allZones = new java.util.ArrayList<>();
        
        if (devices != null) {
            for (RachioDevice device : devices) {
                if (device.zones != null) {
                    allZones.addAll(device.zones);
                }
            }
        }
        
        return allZones;
    }
    
    /**
     * Find zone by ID
     */
    public @Nullable RachioZone findZone(String zoneId) {
        if (devices == null) return null;
        
        for (RachioDevice device : devices) {
            if (device.zones != null) {
                for (RachioZone zone : device.zones) {
                    if (zoneId.equals(zone.id)) {
                        return zone;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Check if account is active
     */
    public boolean isAccountActive() {
        return !disabled && !deleted && verified;
    }
    
    /**
     * Check if account has subscription
     */
    public boolean hasSubscription() {
        return subscriptionPlan != null && !subscriptionPlan.isEmpty();
    }
    
    /**
     * Check if using metric units
     */
    public boolean isMetricUnits() {
        return "METRIC".equalsIgnoreCase(units);
    }
    
    /**
     * Check if using US units
     */
    public boolean isUSUnits() {
        return "US".equalsIgnoreCase(units);
    }
    
    /**
     * Get account summary
     */
    public String getSummary() {
        return String.format("Account: %s (%s), Devices: %d, Zones: %d", 
            fullName, username, getDeviceCount(), getTotalZoneCount());
    }
    
    /**
     * Get account status
     */
    public String getAccountStatus() {
        if (disabled) return "DISABLED";
        if (deleted) return "DELETED";
        if (!verified) return "UNVERIFIED";
        return "ACTIVE";
    }
    
    /**
     * Get subscription info
     */
    public String getSubscriptionInfo() {
        if (subscriptionPlan == null || subscriptionPlan.isEmpty()) {
            return "No subscription";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Plan: ").append(subscriptionPlan);
        
        if (subscriptionStatus != null && !subscriptionStatus.isEmpty()) {
            sb.append(", Status: ").append(subscriptionStatus);
        }
        
        if (subscriptionExpiration != null && !subscriptionExpiration.isEmpty()) {
            sb.append(", Expires: ").append(subscriptionExpiration);
        }
        
        return sb.toString();
    }
    
    /**
     * Get address string
     */
    public String getAddress() {
        if (streetAddress == null || streetAddress.isEmpty()) {
            return "No address";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(streetAddress);
        
        if (city != null && !city.isEmpty()) {
            sb.append(", ").append(city);
        }
        
        if (state != null && !state.isEmpty()) {
            sb.append(", ").append(state);
        }
        
        if (zip != null && !zip.isEmpty()) {
            sb.append(" ").append(zip);
        }
        
        if (country != null && !country.isEmpty()) {
            sb.append(", ").append(country);
        }
        
        return sb.toString();
    }
    
    /**
     * Get API key status
     */
    public String getApiKeyStatus() {
        if (apiKey == null || apiKey.isEmpty()) {
            return "No API key";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("API Key: Present");
        
        if (apiKeyCreatedAt != null && !apiKeyCreatedAt.isEmpty()) {
            sb.append(", Created: ").append(apiKeyCreatedAt);
        }
        
        if (apiKeyLastUsed != null && !apiKeyLastUsed.isEmpty()) {
            sb.append(", Last Used: ").append(apiKeyLastUsed);
        }
        
        return sb.toString();
    }
    
    /**
     * Get account limits info
     */
    public String getLimitsInfo() {
        return String.format("Limits: Devices: %d/%d, Zones: %d/%d, Schedules: %d/%d",
            getDeviceCount(), maxDevices,
            getTotalZoneCount(), maxZones,
            0, maxSchedules); // Schedule count not directly available
    }
    
    /**
     * Get all device names
     */
    public List<String> getDeviceNames() {
        List<String> names = new java.util.ArrayList<>();
        
        if (devices != null) {
            for (RachioDevice device : devices) {
                names.add(device.name);
            }
        }
        
        return names;
    }
    
    /**
     * Get device IDs
     */
    public List<String> getDeviceIds() {
        List<String> ids = new java.util.ArrayList<>();
        
        if (devices != null) {
            for (RachioDevice device : devices) {
                ids.add(device.id);
            }
        }
        
        return ids;
    }
    
    /**
     * Check if person has any devices
     */
    public boolean hasDevices() {
        return devices != null && !devices.isEmpty();
    }
    
    /**
     * Check if person has any zones
     */
    public boolean hasZones() {
        if (devices == null) return false;
        
        for (RachioDevice device : devices) {
            if (device.zones != null && !device.zones.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get person information map
     */
    public java.util.Map<String, Object> getInfo() {
        java.util.Map<String, Object> info = new java.util.HashMap<>();
        info.put("id", id);
        info.put("username", username);
        info.put("fullName", fullName);
        info.put("email", email);
        info.put("accountStatus", getAccountStatus());
        info.put("verified", verified);
        info.put("disabled", disabled);
        info.put("deleted", deleted);
        info.put("deviceCount", getDeviceCount());
        info.put("zoneCount", getTotalZoneCount());
        info.put("enabledZoneCount", getEnabledZoneCount());
        info.put("hasPaymentInfo", hasPaymentInfo);
        info.put("hasSubscription", hasSubscription());
        info.put("subscriptionPlan", subscriptionPlan);
        info.put("units", units);
        info.put("language", language);
        info.put("timeZone", timeZone);
        info.put("hasApiKey", apiKey != null && !apiKey.isEmpty());
        
        if (streetAddress != null) info.put("address", getAddress());
        if (subscriptionStatus != null) info.put("subscriptionStatus", subscriptionStatus);
        if (subscriptionExpiration != null) info.put("subscriptionExpiration", subscriptionExpiration);
        
        return info;
    }
}
