package org.openhab.binding.rachio.internal.api.dto;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

@NonNullByDefault
public class RachioPerson {
    @SerializedName("id")
    private String id = "";
    
    @SerializedName("username")
    private String username = "";
    
    @SerializedName("fullName")
    private String fullName = "";
    
    @SerializedName("firstName")
    private String firstName = "";
    
    @SerializedName("lastName")
    private String lastName = "";
    
    @SerializedName("email")
    private String email = "";
    
    @SerializedName("devices")
    @Nullable
    private List<RachioDevice> devices;
    
    // Getters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    @Nullable
    public List<RachioDevice> getDevices() { return devices; }
    
    // Setters
    public void setId(String id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = email; }
    public void setDevices(@Nullable List<RachioDevice> devices) { this.devices = devices; }
}
