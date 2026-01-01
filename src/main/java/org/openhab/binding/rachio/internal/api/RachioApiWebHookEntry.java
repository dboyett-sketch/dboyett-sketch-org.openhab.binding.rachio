package org.openhab.binding.rachio.internal.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * DTO representing a single webhook entry from Rachio API
 * 
 * @author dboyett-sketch
 */
public class RachioApiWebHookEntry {

    @SerializedName("id")
    public String id;

    @SerializedName("url")
    public String url;

    @SerializedName("externalId")
    public String externalId;

    @SerializedName("eventTypes")
    public List<String> eventTypes;

    @SerializedName("status")
    public String status;

    @SerializedName("createdDate")
    public Instant createdDate;

    @SerializedName("updatedDate")
    public Instant updatedDate;

    @SerializedName("deviceId")
    public String deviceId;

    @SerializedName("userId")
    public String userId;

    @SerializedName("failureCount")
    public Integer failureCount;

    @SerializedName("lastFailureDate")
    public Instant lastFailureDate;

    @SerializedName("lastFailureReason")
    public String lastFailureReason;

    @SerializedName("lastSuccessDate")
    public Instant lastSuccessDate;

    @SerializedName("secret")
    public String secret;

    /**
     * Default constructor for Gson
     */
    public RachioApiWebHookEntry() {
        this.eventTypes = new ArrayList<>();
    }

    /**
     * Constructor with required fields
     */
    public RachioApiWebHookEntry(String id, String url, String deviceId) {
        this();
        this.id = id;
        this.url = url;
        this.deviceId = deviceId;
        this.status = "ACTIVE";
    }

    /**
     * Constructor for creating new webhook
     */
    public RachioApiWebHookEntry(String url, String externalId, List<String> eventTypes, String deviceId) {
        this();
        this.url = url;
        this.externalId = externalId;
        this.eventTypes = eventTypes != null ? eventTypes : new ArrayList<>();
        this.deviceId = deviceId;
        this.status = "ACTIVE";
        this.createdDate = Instant.now();
    }

    /**
     * Get webhook ID
     */
    public String getId() {
        return id;
    }

    /**
     * Get webhook URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Get external ID (OpenHAB instance identifier)
     */
    public String getExternalId() {
        return externalId;
    }

    /**
     * Check if this webhook belongs to OpenHAB
     */
    public boolean isOpenHABWebhook() {
        return externalId != null && externalId.startsWith("openhab_");
    }

    /**
     * Get event types this webhook is subscribed to
     */
    public List<String> getEventTypes() {
        return eventTypes;
    }

    /**
     * Check if webhook is subscribed to a specific event type
     */
    public boolean isSubscribedToEvent(String eventType) {
        return eventTypes != null && eventTypes.contains(eventType);
    }

    /**
     * Add event type to subscription
     */
    public void addEventType(String eventType) {
        if (eventType == null || eventType.isEmpty()) {
            return;
        }

        if (eventTypes == null) {
            eventTypes = new ArrayList<>();
        }

        if (!eventTypes.contains(eventType)) {
            eventTypes.add(eventType);
        }
    }

    /**
     * Remove event type from subscription
     */
    public void removeEventType(String eventType) {
        if (eventTypes != null && eventType != null) {
            eventTypes.remove(eventType);
        }
    }

    /**
     * Get webhook status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Check if webhook is active
     */
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    /**
     * Check if webhook is inactive
     */
    public boolean isInactive() {
        return !isActive();
    }

    /**
     * Check if webhook is disabled
     */
    public boolean isDisabled() {
        return "DISABLED".equalsIgnoreCase(status);
    }

    /**
     * Get created date
     */
    public Instant getCreatedDate() {
        return createdDate;
    }

    /**
     * Get updated date
     */
    public Instant getUpdatedDate() {
        return updatedDate;
    }

    /**
     * Get device ID
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Get user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Get failure count
     */
    public int getFailureCount() {
        return failureCount != null ? failureCount : 0;
    }

    /**
     * Check if webhook has failures
     */
    public boolean hasFailures() {
        return getFailureCount() > 0;
    }

    /**
     * Get last failure date
     */
    public Instant getLastFailureDate() {
        return lastFailureDate;
    }

    /**
     * Get last failure reason
     */
    public String getLastFailureReason() {
        return lastFailureReason;
    }

    /**
     * Get last success date
     */
    public Instant getLastSuccessDate() {
        return lastSuccessDate;
    }

    /**
     * Get webhook secret (for signature validation)
     */
    public String getSecret() {
        return secret;
    }

    /**
     * Check if webhook has a secret
     */
    public boolean hasSecret() {
        return secret != null && !secret.isEmpty();
    }

    /**
     * Update webhook status
     */
    public void setStatus(String status) {
        this.status = status;
        this.updatedDate = Instant.now();
    }

    /**
     * Mark webhook as successful
     */
    public void markSuccess() {
        this.lastSuccessDate = Instant.now();
        this.updatedDate = Instant.now();

        // Reset failure count on success
        if (this.failureCount != null && this.failureCount > 0) {
            this.failureCount = 0;
            this.lastFailureReason = null;
        }
    }

    /**
     * Mark webhook as failed
     */
    public void markFailure(String reason) {
        this.lastFailureDate = Instant.now();
        this.lastFailureReason = reason;
        this.updatedDate = Instant.now();

        // Increment failure count
        if (this.failureCount == null) {
            this.failureCount = 1;
        } else {
            this.failureCount++;
        }
    }

    /**
     * Get webhook age in days
     */
    public long getAgeInDays() {
        if (createdDate == null) {
            return 0;
        }

        Instant now = Instant.now();
        long seconds = now.getEpochSecond() - createdDate.getEpochSecond();
        return seconds / (24 * 60 * 60);
    }

    /**
     * Get days since last success
     */
    public long getDaysSinceLastSuccess() {
        if (lastSuccessDate == null) {
            return Long.MAX_VALUE;
        }

        Instant now = Instant.now();
        long seconds = now.getEpochSecond() - lastSuccessDate.getEpochSecond();
        return seconds / (24 * 60 * 60);
    }

    /**
     * Get days since last failure
     */
    public long getDaysSinceLastFailure() {
        if (lastFailureDate == null) {
            return Long.MAX_VALUE;
        }

        Instant now = Instant.now();
        long seconds = now.getEpochSecond() - lastFailureDate.getEpochSecond();
        return seconds / (24 * 60 * 60);
    }

    /**
     * Check if webhook needs maintenance (too many failures or old)
     */
    public boolean needsMaintenance() {
        // Too many failures
        if (getFailureCount() >= 5) {
            return true;
        }

        // Very old webhook with recent failures
        if (getAgeInDays() > 30 && hasFailures() && getDaysSinceLastFailure() < 7) {
            return true;
        }

        // No success in a long time
        if (getDaysSinceLastSuccess() > 30 && isActive()) {
            return true;
        }

        return false;
    }

    /**
     * Check if webhook should be recreated
     */
    public boolean shouldRecreate() {
        // Disabled webhooks
        if (isDisabled()) {
            return true;
        }

        // Too many failures
        if (getFailureCount() >= 10) {
            return true;
        }

        // Very old with problems
        if (getAgeInDays() > 90 && (hasFailures() || getDaysSinceLastSuccess() > 60)) {
            return true;
        }

        return false;
    }

    /**
     * Validate webhook entry
     */
    public boolean isValid() {
        // Required fields
        if (id == null || id.isEmpty()) {
            return false;
        }

        if (url == null || url.isEmpty()) {
            return false;
        }

        if (deviceId == null || deviceId.isEmpty()) {
            return false;
        }

        // URL validation
        if (!isValidUrl(url)) {
            return false;
        }

        // Status validation
        if (status == null || status.isEmpty()) {
            return false;
        }

        // Event types validation (must have at least one if active)
        if (isActive() && (eventTypes == null || eventTypes.isEmpty())) {
            return false;
        }

        return true;
    }

    /**
     * Basic URL validation
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.length() < 10) {
            return false;
        }

        // Check for valid protocol
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }

        // Check for basic structure
        if (!url.contains("://") || !url.contains(".")) {
            return false;
        }

        return true;
    }

    /**
     * Create a copy of this webhook entry
     */
    public RachioApiWebHookEntry copy() {
        RachioApiWebHookEntry copy = new RachioApiWebHookEntry();
        copy.id = this.id;
        copy.url = this.url;
        copy.externalId = this.externalId;
        copy.eventTypes = this.eventTypes != null ? new ArrayList<>(this.eventTypes) : new ArrayList<>();
        copy.status = this.status;
        copy.createdDate = this.createdDate;
        copy.updatedDate = this.updatedDate;
        copy.deviceId = this.deviceId;
        copy.userId = this.userId;
        copy.failureCount = this.failureCount;
        copy.lastFailureDate = this.lastFailureDate;
        copy.lastFailureReason = this.lastFailureReason;
        copy.lastSuccessDate = this.lastSuccessDate;
        copy.secret = this.secret;
        return copy;
    }

    /**
     * Create a new webhook entry for OpenHAB
     */
    public static RachioApiWebHookEntry createForOpenHAB(String url, String instanceId, String deviceId,
            List<String> eventTypes) {

        String externalId = "openhab_" + instanceId + "_" + Instant.now().getEpochSecond();

        RachioApiWebHookEntry webhook = new RachioApiWebHookEntry();
        webhook.url = url;
        webhook.externalId = externalId;
        webhook.eventTypes = eventTypes != null ? new ArrayList<>(eventTypes) : getDefaultEventTypes();
        webhook.deviceId = deviceId;
        webhook.status = "ACTIVE";
        webhook.createdDate = Instant.now();
        webhook.updatedDate = Instant.now();

        return webhook;
    }

    /**
     * Get default event types for OpenHAB webhooks
     */
    public static List<String> getDefaultEventTypes() {
        List<String> eventTypes = new ArrayList<>();
        eventTypes.add("ZONE_STARTED");
        eventTypes.add("ZONE_STOPPED");
        eventTypes.add("ZONE_COMPLETED");
        eventTypes.add("DEVICE_STATUS");
        eventTypes.add("RAIN_DELAY");
        return eventTypes;
    }

    /**
     * Create summary string for logging
     */
    public String toSummaryString() {
        return String.format("WebHook[id=%s, url=%s, device=%s, status=%s, events=%d]", id,
                url != null ? url.substring(0, Math.min(url.length(), 30)) + "..." : "null", deviceId, status,
                eventTypes != null ? eventTypes.size() : 0);
    }

    @Override
    public String toString() {
        return "RachioApiWebHookEntry{" + "id='" + id + '\'' + ", url='" + url + '\'' + ", externalId='" + externalId
                + '\'' + ", eventTypes=" + (eventTypes != null ? eventTypes.size() : 0) + ", status='" + status + '\''
                + ", deviceId='" + deviceId + '\'' + ", failureCount=" + getFailureCount() + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        RachioApiWebHookEntry that = (RachioApiWebHookEntry) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
