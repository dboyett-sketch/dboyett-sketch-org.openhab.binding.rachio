package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/**
 * DTO for Rachio Webhook Entry from API
 * 
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioApiWebHookEntry {
    @SerializedName("id")
    public String id;
    
    @SerializedName("url")
    public String url;
    
    @SerializedName("externalId")
    public @Nullable String externalId;
    
    @SerializedName("deviceId")
    public String deviceId;
    
    @SerializedName("eventTypes")
    public String eventTypes;
    
    @SerializedName("createdAt")
    public String createdAt;
    
    @SerializedName("updatedAt")
    public @Nullable String updatedAt;
    
    // Helper methods
    
    /**
     * Check if webhook is registered for specific event type
     */
    public boolean isRegisteredForEvent(String eventType) {
        return eventTypes != null && eventTypes.contains(eventType);
    }
    
    /**
     * Get webhook summary
     */
    public String getSummary() {
        return String.format("Webhook %s for device %s (Events: %s)", 
            id, deviceId, eventTypes);
    }
}
