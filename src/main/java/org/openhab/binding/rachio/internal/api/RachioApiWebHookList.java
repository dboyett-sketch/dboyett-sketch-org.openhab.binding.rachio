package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import java.util.List;

/**
 * DTO for Rachio Webhook List from API
 * 
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioApiWebHookList {
    public List<RachioApiWebHookEntry> webhooks;
    
    // Helper methods
    
    /**
     * Check if list is empty
     */
    public boolean isEmpty() {
        return webhooks == null || webhooks.isEmpty();
    }
    
    /**
     * Get webhook count
     */
    public int getCount() {
        return webhooks != null ? webhooks.size() : 0;
    }
    
    /**
     * Find webhook by external ID
     */
    public @Nullable RachioApiWebHookEntry findWebhookByExternalId(String externalId) {
        if (webhooks != null) {
            for (RachioApiWebHookEntry webhook : webhooks) {
                if (externalId.equals(webhook.externalId)) {
                    return webhook;
                }
            }
        }
        return null;
    }
    
    /**
     * Find webhook by URL
     */
    public @Nullable RachioApiWebHookEntry findWebhookByUrl(String url) {
        if (webhooks != null) {
            for (RachioApiWebHookEntry webhook : webhooks) {
                if (url.equals(webhook.url)) {
                    return webhook;
                }
            }
        }
        return null;
    }
}
