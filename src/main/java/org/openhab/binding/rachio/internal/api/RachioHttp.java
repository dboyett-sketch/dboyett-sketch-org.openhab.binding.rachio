package org.openhab.binding.rachio.internal.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.ZoneRunStatus;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.api.dto.RachioEventSummary;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.io.net.http.HttpUtil;
import org.openhab.core.library.types.DateTimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Handles HTTP communication with Rachio API
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioHttp {
    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);

    private static final String RACHIO_API_URL = "https://api.rach.io/1/public";
    private static final String CONTENT_TYPE = "application/json";
    private static final String USER_AGENT = "OpenHAB-Rachio-Binding/5.0";
    
    private final HttpClient httpClient;
    private final Gson gson;
    private final String apiKey;
    
    // Rate limiting tracking
    private final Map<String, RateLimitInfo> rateLimitInfo = new ConcurrentHashMap<>();
    
    private static class RateLimitInfo {
        int remaining;
        int limit;
        Instant resetTime;
        String deviceId;
        
        RateLimitInfo(String deviceId) {
            this.deviceId = deviceId;
            this.remaining = 60; // Default assumption
            this.limit = 60;
            this.resetTime = Instant.now().plusSeconds(3600);
        }
        
        boolean isLimited() {
            return remaining <= 5;
        }
        
        double getUsagePercentage() {
            return ((double) (limit - remaining) / limit) * 100.0;
        }
    }

    public RachioHttp(HttpClientFactory httpClientFactory, String apiKey) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
        this.apiKey = apiKey;
        
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Instant.class, new InstantTypeAdapter());
        this.gson = gsonBuilder.create();
    }

    /**
     * Makes an authenticated request to the Rachio API
     */
    private @Nullable JsonElement makeRequest(String method, String endpoint, @Nullable String body) 
            throws RachioApiException {
        try {
            String url = RACHIO_API_URL + endpoint;
            logger.debug("Making {} request to: {}", method, url);
            
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", CONTENT_TYPE)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", CONTENT_TYPE)
                    .timeout(Duration.ofSeconds(30));
            
            if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)) {
                if (body != null) {
                    requestBuilder.method(method, HttpRequest.BodyPublishers.ofString(body));
                } else {
                    requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
                }
            } else {
                requestBuilder.GET();
            }
            
            HttpRequest request = requestBuilder.build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Parse rate limiting headers
            parseRateLimitHeaders(response.headers(), endpoint);
            
            int statusCode = response.statusCode();
            String responseBody = response.body();
            
            logger.trace("Response status: {}, body: {}", statusCode, responseBody);
            
            if (statusCode >= 200 && statusCode < 300) {
                if (responseBody != null && !responseBody.isEmpty()) {
                    try {
                        return JsonParser.parseString(responseBody);
                    } catch (JsonParseException e) {
                        logger.warn("Failed to parse JSON response: {}", responseBody, e);
                        throw new RachioApiException("Invalid JSON response from Rachio API");
                    }
                }
                return null;
            } else {
                handleErrorResponse(statusCode, responseBody, endpoint);
                return null;
            }
        } catch (URISyntaxException e) {
            logger.error("Invalid URL syntax", e);
            throw new RachioApiException("Invalid API endpoint URL");
        } catch (IOException | InterruptedException e) {
            logger.error("HTTP request failed", e);
            throw new RachioApiException("HTTP communication error: " + e.getMessage());
        }
    }

    /**
     * Parse rate limiting headers from response
     */
    private void parseRateLimitHeaders(HttpHeaders headers, String endpoint) {
        try {
            // Extract device ID from endpoint if possible
            String deviceId = "unknown";
            if (endpoint.contains("/device/")) {
                String[] parts = endpoint.split("/device/");
                if (parts.length > 1) {
                    String[] deviceParts = parts[1].split("/");
                    if (deviceParts.length > 0) {
                        deviceId = deviceParts[0];
                    }
                }
            }
            
            RateLimitInfo info = rateLimitInfo.computeIfAbsent(deviceId, RateLimitInfo::new);
            
            // Parse standard rate limit headers
            String remainingStr = headers.firstValue("X-RateLimit-Remaining").orElse(null);
            String limitStr = headers.firstValue("X-RateLimit-Limit").orElse(null);
            String resetStr = headers.firstValue("X-RateLimit-Reset").orElse(null);
            
            if (remainingStr != null) {
                try {
                    info.remaining = Integer.parseInt(remainingStr);
                } catch (NumberFormatException e) {
                    logger.debug("Invalid X-RateLimit-Remaining header: {}", remainingStr);
                }
            }
            
            if (limitStr != null) {
                try {
                    info.limit = Integer.parseInt(limitStr);
                } catch (NumberFormatException e) {
                    logger.debug("Invalid X-RateLimit-Limit header: {}", limitStr);
                }
            }
            
            if (resetStr != null) {
                try {
                    // Parse ISO 8601 timestamp
                    Instant resetInstant = Instant.parse(resetStr);
                    info.resetTime = resetInstant;
                } catch (Exception e) {
                    logger.debug("Invalid X-RateLimit-Reset header: {}", resetStr);
                }
            }
            
            logger.debug("Rate limit for device {}: {}/{} remaining, resets at {}", 
                    deviceId, info.remaining, info.limit, info.resetTime);
            
        } catch (Exception e) {
            logger.debug("Error parsing rate limit headers", e);
        }
    }

    private void handleErrorResponse(int statusCode, @Nullable String responseBody, String endpoint) 
            throws RachioApiException {
        String errorMessage = "API request failed";
        
        if (responseBody != null && !responseBody.isEmpty()) {
            try {
                JsonObject errorJson = JsonParser.parseString(responseBody).getAsJsonObject();
                if (errorJson.has("error") && errorJson.get("error").isJsonObject()) {
                    JsonObject errorObj = errorJson.get("error").getAsJsonObject();
                    if (errorObj.has("message")) {
                        errorMessage = errorObj.get("message").getAsString();
                    }
                } else if (errorJson.has("message")) {
                    errorMessage = errorJson.get("message").getAsString();
                }
            } catch (Exception e) {
                // If we can't parse the error, use the raw response
                errorMessage = "HTTP " + statusCode + ": " + responseBody;
            }
        } else {
            errorMessage = "HTTP " + statusCode + " for endpoint: " + endpoint;
        }
        
        logger.error("Rachio API error: {}", errorMessage);
        throw new RachioApiException(errorMessage);
    }

    /**
     * Get person information
     */
    public @Nullable RachioPerson getPerson() throws RachioApiException {
        JsonElement response = makeRequest("GET", "/person/info", null);
        if (response != null && response.isJsonObject()) {
            return gson.fromJson(response, RachioPerson.class);
        }
        return null;
    }

    /**
     * Get device information
     */
    public @Nullable RachioDevice getDevice(String deviceId) throws RachioApiException {
        JsonElement response = makeRequest("GET", "/device/" + deviceId, null);
        if (response != null && response.isJsonObject()) {
            return gson.fromJson(response, RachioDevice.class);
        }
        return null;
    }

    /**
     * Get events for a device
     */
    public @Nullable RachioEventSummary getEvents(String deviceId, int hours) throws RachioApiException {
        JsonElement response = makeRequest("GET", "/device/" + deviceId + "/event?hours=" + hours, null);
        if (response != null && response.isJsonObject()) {
            return gson.fromJson(response, RachioEventSummary.class);
        }
        return null;
    }

    /**
     * Start a zone
     */
    public void startZone(String zoneId, int duration) throws RachioApiException {
        JsonObject body = new JsonObject();
        body.addProperty("id", zoneId);
        body.addProperty("duration", duration);
        
        makeRequest("PUT", "/zone/start", gson.toJson(body));
    }

    /**
     * Stop watering
     */
    public void stopWatering(String deviceId) throws RachioApiException {
        JsonObject body = new JsonObject();
        body.addProperty("id", deviceId);
        
        makeRequest("PUT", "/device/stop_water", gson.toJson(body));
    }

    /**
     * Set zone enabled state
     */
    public void setZoneEnabled(String zoneId, boolean enabled) throws RachioApiException {
        JsonObject body = new JsonObject();
        body.addProperty("id", zoneId);
        body.addProperty("enabled", enabled);
        
        makeRequest("PUT", "/zone/enable", gson.toJson(body));
    }

    /**
     * IMPLEMENTED: Run all zones sequentially
     */
    public void runAllZones(String thingId, int duration, String deviceId) throws RachioApiException {
        logger.info("Running all zones on device {} for {} seconds", deviceId, duration);
        
        // Get device details to find all zones
        RachioDevice device = getDevice(deviceId);
        if (device == null || device.zones == null) {
            throw new RachioApiException("Could not retrieve device zones");
        }
        
        List<String> enabledZones = new ArrayList<>();
        for (RachioZone zone : device.zones) {
            if (zone.enabled) {
                enabledZones.add(zone.id);
            }
        }
        
        if (enabledZones.isEmpty()) {
            logger.warn("No enabled zones found on device {}", deviceId);
            return;
        }
        
        // Start each enabled zone sequentially
        for (String zoneId : enabledZones) {
            try {
                startZone(zoneId, duration);
                logger.debug("Started zone {} for {} seconds", zoneId, duration);
                
                // Wait for the duration before starting next zone
                Thread.sleep(duration * 1000L);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RachioApiException("Run all zones interrupted");
            } catch (RachioApiException e) {
                logger.error("Failed to start zone {}: {}", zoneId, e.getMessage());
                // Continue with other zones
            }
        }
        
        logger.info("Completed running all zones on device {}", deviceId);
    }

    /**
     * IMPLEMENTED: Set rain delay
     */
    public void rainDelay(String thingId, int hours, String deviceId) throws RachioApiException {
        logger.info("Setting rain delay on device {} for {} hours", deviceId, hours);
        
        if (hours < 0 || hours > 168) { // 7 days max per Rachio API
            throw new RachioApiException("Rain delay hours must be between 0 and 168");
        }
        
        JsonObject body = new JsonObject();
        body.addProperty("id", deviceId);
        body.addProperty("duration", hours * 3600); // Convert hours to seconds
        
        makeRequest("PUT", "/device/rain_delay", gson.toJson(body));
        
        // Cancel rain delay if hours is 0
        if (hours == 0) {
            logger.debug("Rain delay cancelled for device {}", deviceId);
        }
    }

    /**
     * IMPLEMENTED: Run next zone
     */
    public void runNextZone(String thingId, int duration, String deviceId) throws RachioApiException {
        logger.info("Running next zone on device {} for {} seconds", deviceId, duration);
        
        // Get device details
        RachioDevice device = getDevice(deviceId);
        if (device == null || device.zones == null) {
            throw new RachioApiException("Could not retrieve device zones");
        }
        
        // Find the first enabled zone that's not currently running
        // Note: This is a simplified implementation - in production you'd want 
        // to check actual running status from device state
        for (RachioZone zone : device.zones) {
            if (zone.enabled) {
                try {
                    startZone(zone.id, duration);
                    logger.debug("Started next zone {} for {} seconds", zone.id, duration);
                    return;
                } catch (RachioApiException e) {
                    logger.debug("Zone {} failed, trying next zone", zone.id);
                    // Continue to next zone
                }
            }
        }
        
        throw new RachioApiException("No enabled zones available to run");
    }

    /**
     * Register a webhook
     */
    public void registerWebhook(String deviceId, String url, String externalId) throws RachioApiException {
        JsonObject body = new JsonObject();
        body.addProperty("url", url);
        body.addProperty("externalId", externalId);
        body.addProperty("deviceId", deviceId);
        body.addProperty("eventTypes", "DEVICE_STATUS_EVENT,ZONE_STATUS_EVENT,SCHEDULE_STATUS_EVENT");
        
        makeRequest("POST", "/webhook", gson.toJson(body));
    }

    /**
     * Delete a webhook
     */
    public void deleteWebhook(String webhookId) throws RachioApiException {
        JsonObject body = new JsonObject();
        body.addProperty("id", webhookId);
        
        makeRequest("DELETE", "/webhook", gson.toJson(body));
    }

    /**
     * List webhooks for a device
     */
    public @Nullable List<RachioApiWebHookEntry> listWebhooks(String deviceId) throws RachioApiException {
        JsonElement response = makeRequest("GET", "/webhook/device/" + deviceId, null);
        if (response != null && response.isJsonArray()) {
            RachioApiWebHookList list = gson.fromJson(response, RachioApiWebHookList.class);
            return list.webhooks;
        }
        return null;
    }

    /**
     * Get rate limit information for a device
     */
    public Map<String, Object> getRateLimitInfo(@Nullable String deviceId) {
        String key = deviceId != null ? deviceId : "default";
        RateLimitInfo info = rateLimitInfo.get(key);
        
        Map<String, Object> result = new HashMap<>();
        if (info != null) {
            result.put("remaining", info.remaining);
            result.put("limit", info.limit);
            result.put("resetTime", info.resetTime.toString());
            result.put("isLimited", info.isLimited());
            result.put("usagePercentage", info.getUsagePercentage());
            result.put("deviceId", info.deviceId);
        } else {
            result.put("remaining", 60);
            result.put("limit", 60);
            result.put("resetTime", Instant.now().plusSeconds(3600).toString());
            result.put("isLimited", false);
            result.put("usagePercentage", 0.0);
            result.put("deviceId", deviceId != null ? deviceId : "unknown");
        }
        
        return result;
    }
    
    /**
     * Check if rate limited for a device
     */
    public boolean isRateLimited(@Nullable String deviceId) {
        String key = deviceId != null ? deviceId : "default";
        RateLimitInfo info = rateLimitInfo.get(key);
        return info != null && info.isLimited();
    }
    
    // Helper class for HttpHeaders since we need to extract multiple values
    private static class HttpHeaders {
        private final Map<String, List<String>> headers;
        
        HttpHeaders(java.net.http.HttpHeaders original) {
            this.headers = new HashMap<>();
            original.map().forEach((key, values) -> {
                headers.put(key.toLowerCase(), new ArrayList<>(values));
            });
        }
        
        @Nullable String firstValue(String headerName) {
            List<String> values = headers.get(headerName.toLowerCase());
            return values != null && !values.isEmpty() ? values.get(0) : null;
        }
    }
}
