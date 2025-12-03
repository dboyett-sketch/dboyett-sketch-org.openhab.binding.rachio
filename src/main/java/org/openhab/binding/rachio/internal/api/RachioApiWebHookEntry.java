package org.openhab.binding.rachio.internal.api;

public class RachioApiWebHookEntry {
    public String id;
    public String eventType;
    public String url;
    public String externalId; // ← Add this
}