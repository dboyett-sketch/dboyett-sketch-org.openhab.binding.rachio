package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public class RachioAlert {
    public @Nullable String id;
    public @Nullable String type;
    public @Nullable String message;
    public @Nullable Instant createdDate;
    public @Nullable Boolean read;
    
    public @Nullable String getId() { return id; }
    public @Nullable String getType() { return type; }
    public @Nullable String getMessage() { return message; }
    public @Nullable Instant getCreatedDate() { return createdDate; }
    public @Nullable Boolean isRead() { return read; }
}
