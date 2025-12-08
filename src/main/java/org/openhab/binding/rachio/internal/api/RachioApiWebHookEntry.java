package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

@NonNullByDefault
public class RachioApiWebHookEntry {
    @SerializedName("id")
    private String id = "";

    @SerializedName("url")
    private String url = "";

    @SerializedName("externalId")
    @Nullable
    private String externalId;

    @SerializedName("eventTypes")
    @Nullable
    private String[] eventTypes;

    // Getters
    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    @Nullable
    public String getExternalId() {
        return externalId;
    }

    @Nullable
    public String[] getEventTypes() {
        return eventTypes;
    }
}
