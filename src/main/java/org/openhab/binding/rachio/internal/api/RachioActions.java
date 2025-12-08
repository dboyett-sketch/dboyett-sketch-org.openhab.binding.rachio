package org.openhab.binding.rachio.internal.api;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;

@NonNullByDefault
public interface RachioActions {
    // Rate limiting and monitoring
    int getAdaptivePollingMultiplier();
    
    // Device actions
    void startZone(String deviceId, String zoneId, int duration, String source) throws RachioApiException, IOException;
    void stopWatering(String deviceId, String source) throws RachioApiException, IOException;
    void runAllZones(String deviceId, int duration, String source) throws RachioApiException, IOException;
    void setZoneEnabled(String deviceId, String zoneId, boolean enabled, String source) throws RachioApiException, IOException;
    void rainDelay(String deviceId, int duration) throws RachioApiException, IOException;
    void runNextZone(String deviceId, String source) throws RachioApiException, IOException;
    
    // Person/Device info
    @Nullable RachioPerson getPerson() throws RachioApiException, IOException;
    
    // Webhook validation
    boolean validateWebhookSignature(String payload, String signature, String webhookKey);
}
