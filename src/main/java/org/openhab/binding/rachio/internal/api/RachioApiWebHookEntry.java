package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link RachioApiWebHookEntry} class defines a webhook entry from the Rachio API
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioApiWebHookEntry {

    @SerializedName("id")
    private @Nullable String id;

    @SerializedName("url")
    private @Nullable String url;

    @SerializedName("externalId")
    private @Nullable String externalId;

    @SerializedName("status")
    private @Nullable String status;

    @SerializedName("createdAt")
    private @Nullable String createdAt;

    @SerializedName("updatedAt")
    private @Nullable String updatedAt;

    @SerializedName("eventTypes")
    private @Nullable String[] eventTypes;

    /**
     * Default constructor
     */
    public RachioApiWebHookEntry() {
    }

    /**
     * Constructor with parameters
     *
     * @param id webhook ID
     * @param url webhook URL
     * @param externalId external ID
     */
    public RachioApiWebHookEntry(String id, String url, String externalId) {
        this.id = id;
        this.url = url;
        this.externalId = externalId;
        this.status = "ACTIVE";
    }

    /**
     * Get webhook ID
     *
     * @return webhook ID
     */
    public @Nullable String getId() {
        return id;
    }

    /**
     * Set webhook ID
     *
     * @param id webhook ID
     */
    public void setId(@Nullable String id) {
        this.id = id;
    }

    /**
     * Get webhook URL
     *
     * @return webhook URL
     */
    public @Nullable String getUrl() {
        return url;
    }

    /**
     * Set webhook URL
     *
     * @param url webhook URL
     */
    public void setUrl(@Nullable String url) {
        this.url = url;
    }

    /**
     * Get external ID
     *
     * @return external ID
     */
    public @Nullable String getExternalId() {
        return externalId;
    }

    /**
     * Set external ID
     *
     * @param externalId external ID
     */
    public void setExternalId(@Nullable String externalId) {
        this.externalId = externalId;
    }

    /**
     * Get webhook status
     *
     * @return status (ACTIVE, INACTIVE, etc.)
     */
    public @Nullable String getStatus() {
        return status;
    }

    /**
     * Set webhook status
     *
     * @param status status
     */
    public void setStatus(@Nullable String status) {
        this.status = status;
    }

    /**
     * Get creation timestamp
     *
     * @return creation timestamp
     */
    public @Nullable String getCreatedAt() {
        return createdAt;
    }

    /**
     * Set creation timestamp
     *
     * @param createdAt creation timestamp
     */
    public void setCreatedAt(@Nullable String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Get update timestamp
     *
     * @return update timestamp
     */
    public @Nullable String getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Set update timestamp
     *
     * @param updatedAt update timestamp
     */
    public void setUpdatedAt(@Nullable String updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Get event types
     *
     * @return array of event types
     */
    public @Nullable String[] getEventTypes() {
        return eventTypes;
    }

    /**
     * Set event types
     *
     * @param eventTypes array of event types
     */
    public void setEventTypes(@Nullable String[] eventTypes) {
        this.eventTypes = eventTypes;
    }

    /**
     * Check if webhook is active
     *
     * @return true if active
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    /**
     * Check if webhook is inactive
     *
     * @return true if inactive
     */
    public boolean isInactive() {
        return "INACTIVE".equals(status);
    }

    /**
     * Check if webhook supports a specific event type
     *
     * @param eventType event type to check
     * @return true if supported
     */
    public boolean supportsEventType(String eventType) {
        if (eventTypes == null || eventType == null) {
            return false;
        }
        for (String type : eventTypes) {
            if (eventType.equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get event types as comma-separated string
     *
     * @return comma-separated event types
     */
    public String getEventTypesString() {
        if (eventTypes == null || eventTypes.length == 0) {
            return "";
        }
        return String.join(", ", eventTypes);
    }

    /**
     * Check if webhook is valid (has required fields)
     *
     * @return true if valid
     */
    public boolean isValid() {
        return id != null && !id.isEmpty() && 
               url != null && !url.isEmpty() && 
               externalId != null && !externalId.isEmpty();
    }

    /**
     * Get string representation
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "RachioApiWebHookEntry{" +
                "id='" + id + '\'' +
                ", url='" + url + '\'' +
                ", externalId='" + externalId + '\'' +
                ", status='" + status + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", eventTypes=" + (eventTypes != null ? String.join(",", eventTypes) : "null") +
                '}';
    }

    /**
     * Get summary string
     *
     * @return summary string
     */
    public String getSummary() {
        return String.format("Webhook[id=%s, url=%s, status=%s]", id, url, status);
    }

    /**
     * Compare by ID
     *
     * @param obj other object
     * @return true if IDs match
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RachioApiWebHookEntry that = (RachioApiWebHookEntry) obj;
        return id != null && id.equals(that.id);
    }

    /**
     * Hash code based on ID
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
