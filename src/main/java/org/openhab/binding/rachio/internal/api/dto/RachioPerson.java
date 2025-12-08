package org.openhab.binding.rachio.internal.api.dto;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

@NonNullByDefault
public class RachioPerson {
    @SerializedName("id")
    private String id = "";

    @SerializedName("firstName")
    private String firstName = "";

    @SerializedName("lastName")
    private String lastName = "";

    @SerializedName("email")
    private String email = "";

    @SerializedName("devices")
    private List<RachioDevice> devices = List.of();

    @SerializedName("username")
    @Nullable
    private String username;

    // Getters
    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public List<RachioDevice> getDevices() {
        return devices;
    }

    @Nullable
    public String getUsername() {
        return username;
    }
}
