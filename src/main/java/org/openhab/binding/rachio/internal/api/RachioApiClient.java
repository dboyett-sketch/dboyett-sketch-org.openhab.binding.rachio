package org.openhab.binding.rachio.internal.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.openhab.binding.rachio.internal.api.dto.*;
import org.openhab.core.common.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Business logic layer for Rachio API interactions
 */
public class RachioApiClient {
    private final Logger logger = LoggerFactory.getLogger(RachioApiClient.class);
    
    private static final String BASE_URL = "https://api.rach.io/1/public";
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private final RachioHttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    
    public RachioApiClient(String apiKey) {
        this.httpClient = new RachioHttpClient(apiKey);
        this.scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(
            new NamedThreadFactory("rachio-polling", true)
        );
        
        // Start periodic status updates
        startPolling();
    }
    
    public void dispose() {
        scheduler.shutdown();
    }
    
    // Device operations
    public List<RachioDevice> getDevices() throws RachioApiException {
        String response = httpClient.get(BASE_URL + "/person/info");
        try {
            RachioPerson person = mapper.readValue(response, RachioPerson.class);
            return person.getDevices();
        } catch (Exception e) {
            throw new RachioApiException("Failed to parse devices", e);
        }
    }
    
    public RachioDevice getDevice(String deviceId) throws RachioApiException {
        String response = httpClient.get(BASE_URL + "/device/" + deviceId);
        try {
            return mapper.readValue(response, RachioDevice.class);
        } catch (Exception e) {
            throw new RachioApiException("Failed to parse device", e);
        }
    }
    
    // Zone operations
    public void startZone(String zoneId, int duration) throws RachioApiException {
        String body = String.format("{\"id\":\"%s\",\"duration\":%d}", zoneId, duration);
        httpClient.put(BASE_URL + "/zone/start", body);
    }
    
    public void stopWatering(String deviceId) throws RachioApiException {
        httpClient.put(BASE_URL + "/device/" + deviceId + "/stop_water", "{}");
    }
    
    // Webhook operations
    public void registerWebhook(String url, String externalId) throws RachioApiException {
        String body = String.format(
            "{\"url\":\"%s\",\"externalId\":\"%s\",\"eventTypes\":[\"DEVICE_STATUS_EVENT\"]}",
            url, externalId
        );
        httpClient.post(BASE_URL + "/webhook", body);
    }
    
    public void unregisterWebhook(String webhookId) throws RachioApiException {
        httpClient.delete(BASE_URL + "/webhook/" + webhookId);
    }
    
    // Weather integration (uses existing DTOs)
    public RachioForecast getForecast(String deviceId) throws RachioApiException {
        String response = httpClient.get(BASE_URL + "/device/" + deviceId + "/forecast");
        try {
            return mapper.readValue(response, RachioForecast.class);
        } catch (Exception e) {
            throw new RachioApiException("Failed to parse forecast", e);
        }
    }
    
    // Polling for status updates
    private void startPolling() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                // Poll device status every 5 minutes
                // This can update thing status in handlers
                logger.debug("Performing periodic Rachio status check");
                // Implementation would notify listeners
            } catch (Exception e) {
                logger.debug("Polling failed: {}", e.getMessage());
            }
        }, 5, 5, TimeUnit.MINUTES);
    }
}
