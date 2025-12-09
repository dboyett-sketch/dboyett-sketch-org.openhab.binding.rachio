package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public class RachioApiWebHookEntry {
    public @Nullable String id;
    public @Nullable String url;
    public @Nullable String externalId;
    
    public @Nullable String getId() { return id; }
    public @Nullable String getUrl() { return url; }
    public @Nullable String getExternalId() { return externalId; }
}
