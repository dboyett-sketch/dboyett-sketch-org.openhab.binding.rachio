package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents webhook event data from Rachio
 */
@NonNullByDefault
public class RachioWebhookEvent {
    public String eventType = "";
    public String deviceId = "";
    public String timestamp = "";
    public String summary = "";
    @Nullable
    public WebhookDevice device;
    @Nullable
    public WebhookZone zone;
    @Nullable
    public WebhookNotifier notifier;
    
    public static class WebhookDevice {
        public String id = "";
        public String name = "";
        public boolean on;
        public String status = "";
    }
    
    public static class WebhookZone {
        public String id = "";
        public String name = "";
        public int zoneNumber;
        public int duration;
        public String status = "";
    }
    
    public static class WebhookNotifier {
        public String id = "";
        public String type = "";
        public String summary = "";
    }
}