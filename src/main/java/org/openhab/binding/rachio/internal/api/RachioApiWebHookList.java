package org.openhab.binding.rachio.internal.api;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public class RachioApiWebHookList {
    public @Nullable List<RachioApiWebHookEntry> data;
    
    public @Nullable List<RachioApiWebHookEntry> getData() { return data; }
}
