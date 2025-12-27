package org.openhab.binding.rachio.internal.api;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * DTO representing a list of webhooks from Rachio API
 * 
 * @author dboyett-sketch
 */
public class RachioApiWebHookList {

    @SerializedName("webhooks")
    public List<RachioApiWebHookEntry> webhooks;

    @SerializedName("deviceId")
    public String deviceId;

    @SerializedName("total")
    public Integer total;

    @SerializedName("limit")
    public Integer limit;

    @SerializedName("offset")
    public Integer offset;

    /**
     * Default constructor for Gson
     */
    public RachioApiWebHookList() {
        this.webhooks = new ArrayList<>();
    }

    /**
     * Constructor with device ID
     */
    public RachioApiWebHookList(String deviceId) {
        this();
        this.deviceId = deviceId;
    }

    /**
     * Constructor with webhooks list
     */
    public RachioApiWebHookList(List<RachioApiWebHookEntry> webhooks) {
        this.webhooks = webhooks != null ? webhooks : new ArrayList<>();
        updateTotal();
    }

    /**
     * Get the list of webhooks
     */
    public List<RachioApiWebHookEntry> getWebhooks() {
        return webhooks;
    }

    /**
     * Get webhook by ID
     */
    public RachioApiWebHookEntry getWebhookById(String webhookId) {
        if (webhookId == null || webhooks == null) {
            return null;
        }

        for (RachioApiWebHookEntry webhook : webhooks) {
            if (webhookId.equals(webhook.id)) {
                return webhook;
            }
        }
        return null;
    }

    /**
     * Get webhook by URL
     */
    public RachioApiWebHookEntry getWebhookByUrl(String url) {
        if (url == null || webhooks == null) {
            return null;
        }

        for (RachioApiWebHookEntry webhook : webhooks) {
            if (url.equals(webhook.url)) {
                return webhook;
            }
        }
        return null;
    }

    /**
     * Get webhook by external ID (OpenHAB instance identifier)
     */
    public RachioApiWebHookEntry getWebhookByExternalId(String externalId) {
        if (externalId == null || webhooks == null) {
            return null;
        }

        for (RachioApiWebHookEntry webhook : webhooks) {
            if (externalId.equals(webhook.externalId)) {
                return webhook;
            }
        }
        return null;
    }

    /**
     * Check if webhook exists by ID
     */
    public boolean containsWebhook(String webhookId) {
        return getWebhookById(webhookId) != null;
    }

    /**
     * Check if webhook exists by URL
     */
    public boolean containsWebhookWithUrl(String url) {
        return getWebhookByUrl(url) != null;
    }

    /**
     * Check if webhook exists by external ID
     */
    public boolean containsWebhookWithExternalId(String externalId) {
        return getWebhookByExternalId(externalId) != null;
    }

    /**
     * Add a webhook to the list
     */
    public void addWebhook(RachioApiWebHookEntry webhook) {
        if (webhook == null) {
            return;
        }

        if (webhooks == null) {
            webhooks = new ArrayList<>();
        }

        // Check for duplicates by ID
        if (!containsWebhook(webhook.id)) {
            webhooks.add(webhook);
            updateTotal();
        }
    }

    /**
     * Remove webhook by ID
     */
    public boolean removeWebhook(String webhookId) {
        RachioApiWebHookEntry webhook = getWebhookById(webhookId);
        if (webhook != null) {
            boolean removed = webhooks.remove(webhook);
            if (removed) {
                updateTotal();
            }
            return removed;
        }
        return false;
    }

    /**
     * Remove webhook by URL
     */
    public boolean removeWebhookByUrl(String url) {
        RachioApiWebHookEntry webhook = getWebhookByUrl(url);
        if (webhook != null) {
            boolean removed = webhooks.remove(webhook);
            if (removed) {
                updateTotal();
            }
            return removed;
        }
        return false;
    }

    /**
     * Remove webhook by external ID
     */
    public boolean removeWebhookByExternalId(String externalId) {
        RachioApiWebHookEntry webhook = getWebhookByExternalId(externalId);
        if (webhook != null) {
            boolean removed = webhooks.remove(webhook);
            if (removed) {
                updateTotal();
            }
            return removed;
        }
        return false;
    }

    /**
     * Get all webhook IDs
     */
    public List<String> getWebhookIds() {
        List<String> ids = new ArrayList<>();
        if (webhooks != null) {
            for (RachioApiWebHookEntry webhook : webhooks) {
                if (webhook.id != null) {
                    ids.add(webhook.id);
                }
            }
        }
        return ids;
    }

    /**
     * Get all webhook URLs
     */
    public List<String> getWebhookUrls() {
        List<String> urls = new ArrayList<>();
        if (webhooks != null) {
            for (RachioApiWebHookEntry webhook : webhooks) {
                if (webhook.url != null) {
                    urls.add(webhook.url);
                }
            }
        }
        return urls;
    }

    /**
     * Get webhooks for a specific event type
     */
    public List<RachioApiWebHookEntry> getWebhooksForEvent(String eventType) {
        List<RachioApiWebHookEntry> filtered = new ArrayList<>();
        if (webhooks != null && eventType != null) {
            for (RachioApiWebHookEntry webhook : webhooks) {
                if (webhook.eventTypes != null && webhook.eventTypes.contains(eventType)) {
                    filtered.add(webhook);
                }
            }
        }
        return filtered;
    }

    /**
     * Check if any webhook is subscribed to an event type
     */
    public boolean hasWebhookForEvent(String eventType) {
        if (webhooks == null || eventType == null) {
            return false;
        }

        for (RachioApiWebHookEntry webhook : webhooks) {
            if (webhook.eventTypes != null && webhook.eventTypes.contains(eventType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get active webhooks (status = "ACTIVE")
     */
    public List<RachioApiWebHookEntry> getActiveWebhooks() {
        List<RachioApiWebHookEntry> active = new ArrayList<>();
        if (webhooks != null) {
            for (RachioApiWebHookEntry webhook : webhooks) {
                if ("ACTIVE".equalsIgnoreCase(webhook.status)) {
                    active.add(webhook);
                }
            }
        }
        return active;
    }

    /**
     * Get inactive webhooks (status != "ACTIVE")
     */
    public List<RachioApiWebHookEntry> getInactiveWebhooks() {
        List<RachioApiWebHookEntry> inactive = new ArrayList<>();
        if (webhooks != null) {
            for (RachioApiWebHookEntry webhook : webhooks) {
                if (!"ACTIVE".equalsIgnoreCase(webhook.status)) {
                    inactive.add(webhook);
                }
            }
        }
        return inactive;
    }

    /**
     * Get webhooks created by OpenHAB (externalId starts with "openhab_")
     */
    public List<RachioApiWebHookEntry> getOpenHABWebhooks() {
        List<RachioApiWebHookEntry> openhabWebhooks = new ArrayList<>();
        if (webhooks != null) {
            for (RachioApiWebHookEntry webhook : webhooks) {
                if (webhook.externalId != null && webhook.externalId.startsWith("openhab_")) {
                    openhabWebhooks.add(webhook);
                }
            }
        }
        return openhabWebhooks;
    }

    /**
     * Update total count based on current webhooks
     */
    public void updateTotal() {
        if (webhooks != null) {
            total = webhooks.size();
        } else {
            total = 0;
        }
    }

    /**
     * Get effective total (returns total field or list size)
     */
    public int getEffectiveTotal() {
        if (total != null) {
            return total;
        }
        return webhooks != null ? webhooks.size() : 0;
    }

    /**
     * Check if list is empty
     */
    public boolean isEmpty() {
        return getEffectiveTotal() == 0;
    }

    /**
     * Check if list has reached limit
     */
    public boolean isFull() {
        if (limit == null || total == null) {
            return false;
        }
        return total >= limit;
    }

    /**
     * Clear all webhooks
     */
    public void clear() {
        if (webhooks != null) {
            webhooks.clear();
        }
        updateTotal();
    }

    /**
     * Merge another webhook list into this one
     */
    public void merge(RachioApiWebHookList other) {
        if (other == null || other.webhooks == null) {
            return;
        }

        if (webhooks == null) {
            webhooks = new ArrayList<>();
        }

        for (RachioApiWebHookEntry webhook : other.webhooks) {
            if (!containsWebhook(webhook.id)) {
                webhooks.add(webhook);
            }
        }

        updateTotal();
    }

    /**
     * Validate webhook list
     */
    public boolean isValid() {
        // Must have device ID if webhooks exist
        if (webhooks != null && !webhooks.isEmpty()) {
            return deviceId != null && !deviceId.isEmpty();
        }

        // Empty list is valid
        return true;
    }

    /**
     * Get summary string for logging
     */
    public String toSummaryString() {
        return String.format("WebHookList[device=%s, total=%d, active=%d]", deviceId, getEffectiveTotal(),
                getActiveWebhooks().size());
    }

    @Override
    public String toString() {
        return "RachioApiWebHookList{" + "deviceId='" + deviceId + '\'' + ", total=" + getEffectiveTotal() + ", limit="
                + limit + ", offset=" + offset + ", webhooks=" + (webhooks != null ? webhooks.size() : 0) + '}';
    }
}
