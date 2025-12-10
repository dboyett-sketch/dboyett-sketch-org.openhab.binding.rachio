package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents a webhook entry from the Rachio API
 */
@NonNullByDefault
public class RachioApiWebHookEntry {
    public @Nullable String id;
    public @Nullable String url;
    public @Nullable String externalId;
    public @Nullable String status;
    
    public @Nullable String getId() {
        return id;
    }
    
    public @Nullable String getUrl() {
        return url;
    }
    
    public @Nullable String getExternalId() {
        return externalId;
    }
    
    public @Nullable String getStatus() {
        return status;
    }
}
