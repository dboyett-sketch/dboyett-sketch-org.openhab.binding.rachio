package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;

/**
 * DTO for Rachio Webhook List API Response
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioApiWebHookList {
    @Nullable
    public List<RachioApiWebHookEntry> data;
    
    @Override
    public String toString() {
        return "RachioApiWebHookList [data=" + (data != null ? data.size() : 0) + " entries]";
    }
}
