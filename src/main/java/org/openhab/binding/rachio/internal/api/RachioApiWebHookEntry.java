package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import java.time.Instant;

/**
 * DTO for Rachio Webhook API Entry
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioApiWebHookEntry {
    @Nullable
    public String id;
    
    @Nullable
    public String url;
    
    @Nullable
    public String externalId;
    
    @Nullable
    public Instant createdAt;
    
    @Nullable
    public String status; // ACTIVE, INACTIVE
    
    @Override
    public String toString() {
        return "RachioApiWebHookEntry [id=" + id + ", url=" + url + ", externalId=" + externalId + "]";
    }
}
