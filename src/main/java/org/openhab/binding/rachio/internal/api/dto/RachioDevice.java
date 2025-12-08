package org.openhab.binding.rachio.internal.api.dto;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;

import com.google.gson.annotations.SerializedName;

@NonNullByDefault
public class RachioDevice {
    @SerializedName("id")
    public String id = "";
    
    @SerializedName("status")
    public String status = "";
    
    @SerializedName("name")
    public String name = "";
    
    @SerializedName("model")
    public String model = "";
    
    @SerializedName("serialNumber")
    public String serialNumber = "";
    
    @SerializedName("macAddress")
    public String macAddress = "";
    
    @SerializedName("zones")
    public @Nullable List<RachioZone> zones;
    
    @SerializedName("scheduleRules")
    public @Nullable List<Object> scheduleRules;
    
    @SerializedName("flexScheduleRules")
    public @Nullable List<Object> flexScheduleRules;
    
    @SerializedName("latitude")
    public double latitude;
    
    @SerializedName("longitude")
    public double longitude;
    
    @SerializedName("timeZone")
    public String timeZone = "";
    
    @SerializedName("createdDate")
    public long createdDate;
    
    @SerializedName("updatedDate")
    public long updatedDate;
    
    @SerializedName("on")
    public boolean on;
    
    @SerializedName("rainDelayExpirationDate")
    public long rainDelayExpirationDate;
    
    @SerializedName("scheduleModeType")
    public String scheduleModeType = "";
    
    @SerializedName("deleted")
    public boolean deleted;
    
    @SerializedName("homeKitCompatible")
    public boolean homeKitCompatible;
    
    @SerializedName("touchStandby")
    public boolean touchStandby;
    
    // Default constructor for Gson
    public RachioDevice() {
    }
    
    public void updateStatus(ThingStatus status, ThingStatusDetail detail, String description) {
        // This is a placeholder - actual implementation would update the device status
        // In a real implementation, this would be handled by the device handler
    }
    
    public boolean isPaused() {
        return "PAUSED".equals(status);
    }
    
    public boolean isOnline() {
        return "ONLINE".equals(status);
    }
    
    public boolean isOffline() {
        return "OFFLINE".equals(status);
    }
    
    // Helper method to get the first zone
    public @Nullable RachioZone getZone(String zoneId) {
        if (zones != null) {
            for (RachioZone zone : zones) {
                if (zone.id.equals(zoneId)) {
                    return zone;
                }
            }
        }
        return null;
    }
    
    // Helper method to update device status (for bridge handler)
    public void updateDeviceStatus(ThingStatus status, ThingStatusDetail detail, String description) {
        // In the actual device handler, this would update the thing status
        // For the DTO, we just store the status string
        this.status = status.name();
    }
}
