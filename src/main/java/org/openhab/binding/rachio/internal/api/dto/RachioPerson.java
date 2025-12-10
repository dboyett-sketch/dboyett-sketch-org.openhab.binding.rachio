package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * The {@link RachioPerson} class defines person data from the Rachio API
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioPerson {
    
    @SerializedName("id")
    private @Nullable String id;
    
    @SerializedName("username")
    private @Nullable String username;
    
    @SerializedName("email")
    private @Nullable String email;
    
    @SerializedName("fullName")
    private @Nullable String fullName;
    
    @SerializedName("devices")
    private @Nullable List<RachioDevice> devices;
    
    // Getters and setters
    
    public @Nullable String getId() {
        return id;
    }
    
    public void setId(@Nullable String id) {
        this.id = id;
    }
    
    public @Nullable String getUsername() {
        return username;
    }
    
    public void setUsername(@Nullable String username) {
        this.username = username;
    }
    
    public @Nullable String getEmail() {
        return email;
    }
    
    public void setEmail(@Nullable String email) {
        this.email = email;
    }
    
    public @Nullable String getFullName() {
        return fullName;
    }
    
    public void setFullName(@Nullable String fullName) {
        this.fullName = fullName;
    }
    
    // ADDED THIS MISSING METHOD - This fixes compilation errors
    public @Nullable List<RachioDevice> getDevices() {
        return devices;
    }
    
    public void setDevices(@Nullable List<RachioDevice> devices) {
        this.devices = devices;
    }
    
    // Helper methods
    
    public boolean hasDevices() {
        return devices != null && !devices.isEmpty();
    }
    
    public int getDeviceCount() {
        return devices != null ? devices.size() : 0;
    }
    
    public @Nullable RachioDevice getFirstDevice() {
        if (devices != null && !devices.isEmpty()) {
            return devices.get(0);
        }
        return null;
    }
    
    public @Nullable RachioDevice getDeviceById(String deviceId) {
        if (devices != null && deviceId != null) {
            for (RachioDevice device : devices) {
                if (deviceId.equals(device.getId())) {
                    return device;
                }
            }
        }
        return null;
    }
    
    public boolean hasDevice(String deviceId) {
        return getDeviceById(deviceId) != null;
    }
    
    @Override
    public String toString() {
        return "RachioPerson{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", devices=" + getDeviceCount() +
                '}';
    }
    
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RachioPerson [");
        sb.append("id=").append(id);
        sb.append(", username=").append(username);
        sb.append(", fullName=").append(fullName);
        sb.append(", email=").append(email != null ? "set" : "null");
        sb.append(", deviceCount=").append(getDeviceCount());
        if (devices != null) {
            sb.append(", devices=[");
            for (int i = 0; i < devices.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(devices.get(i).getName());
            }
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }
}
