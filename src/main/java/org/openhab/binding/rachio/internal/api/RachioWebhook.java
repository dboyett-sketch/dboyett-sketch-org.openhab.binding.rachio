package org.openhab.binding.rachio.internal.api;

public class RachioWebhook {
    public String id = "";
    public String url = "";
    public String externalId = "";
    public String eventType = "";
    
    public String getId() { return id; }
    public String getUrl() { return url; }
    public String getExternalId() { return externalId; }
    public String getEventType() { return eventType; }
}