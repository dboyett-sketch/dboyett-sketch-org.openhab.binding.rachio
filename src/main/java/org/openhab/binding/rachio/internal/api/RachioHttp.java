package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.api.dto.RachioSchedule;
import org.openhab.binding.rachio.internal.api.dto.RachioForecast;
import org.openhab.binding.rachio.internal.api.dto.RachioUsage;
import org.openhab.binding.rachio.internal.api.dto.RachioAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Handles HTTP communication with the Rachio API using Java's modern HttpClient
 */
@NonNullByDefault
public class RachioHttp {
    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);

    // Use Java's HttpClient instead of OpenHAB's
    private final HttpClient httpClient;
    private final RachioBridgeConfiguration config;
    private final Gson gson;

    // Rate limiting tracking
    private final Map<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();
    private static final String RATE_LIMIT_HEADER = "X-RateLimit-Remaining";
    private static final String RATE_RESET_HEADER = "X-RateLimit-Reset";

    private static class RateLimitInfo {
        int remaining;
        Instant resetTime;
        Instant lastUpdated;
    }

    // FIXED: Removed HttpClientFactory parameter - using Java HttpClient directly
    public RachioHttp(RachioBridgeConfiguration config) {
        this.config = config;
        
        // Create Java HttpClient with appropriate settings
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        
        // Configure Gson for JSON parsing
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
    }

    // ============ GENERIC HTTP METHODS ============
    
    /**
     * Make a GET request to the Rachio API
     */
    public @Nullable String get(String endpoint) throws RachioApiException {
        return makeRequest("GET", endpoint, null);
    }

    /**
     * Make a POST request to the Rachio API
     */
    public @Nullable String post(String endpoint, @Nullable Object data) throws RachioApiException {
        return makeRequest("POST", endpoint, data);
    }

    /**
     * Make a PUT request to the Rachio API
     */
    public @Nullable String put(String endpoint, @Nullable Object data) throws RachioApiException {
        return makeRequest("PUT", endpoint, data);
    }

    /**
     * Make a DELETE request to the Rachio API
     */
    public @Nullable String delete(String endpoint) throws RachioApiException {
        return makeRequest("DELETE", endpoint, null);
    }

    /**
     * Generic HTTP request method using Java HttpClient
     */
    private @Nullable String makeRequest(String method, String endpoint, @Nullable Object data) throws RachioApiException {
        String url = "https://api.rachio/1/public/" + endpoint;
        
        try {
            // Build the request using Java HttpClient
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + config.apiKey)
                    .header("Accept", "application/json")
                    .header("User-Agent", "OpenHAB-Rachio-Binding/5.0.1");

            // Add request body for POST/PUT
            if (data != null && (method.equals("POST") || method.equals("PUT"))) {
                String jsonBody = gson.toJson(data);
                requestBuilder
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
            } else {
                requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            HttpRequest request = requestBuilder.build();
            
            logger.debug("Making {} request to: {}", method, url);
            if (data != null) {
                logger.trace("Request body: {}", gson.toJson(data));
            }

            // Send the request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Handle rate limiting headers
            handleRateLimitHeaders(response.headers().map(), endpoint);
            
            int statusCode = response.statusCode();
            String responseBody = response.body();
            
            logger.debug("Response status: {} for {}", statusCode, endpoint);
            logger.trace("Response body: {}", responseBody);
            
            if (statusCode >= 200 && statusCode < 300) {
                return responseBody;
            } else {
                throw new RachioApiException("HTTP " + statusCode + " for " + endpoint + ": " + responseBody);
            }
            
        } catch (IOException e) {
            throw new RachioApiException("IO error during API call to " + endpoint, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RachioApiException("Request interrupted for " + endpoint, e);
        }
    }

    // ============ RACHIO API SPECIFIC METHODS ============
    
    /**
     * Get person info
     */
    public @Nullable RachioPerson getPersonInfo() throws RachioApiException {
        String response = get("person/info");
        if (response != null) {
            return gson.fromJson(response, RachioPerson.class);
        }
        return null;
    }

    /**
     * Get all devices for the person
     */
    public @Nullable List<RachioDevice> getDevices() throws RachioApiException {
        // First get person info to get personId
        RachioPerson person = getPersonInfo();
        if (person == null || person.id == null) {
            throw new RachioApiException("Could not get person info");
        }
        
        String response = get("person/" + person.id + "/device");
        if (response != null) {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            return gson.fromJson(json.get("devices"), new com.google.gson.reflect.TypeToken<List<RachioDevice>>(){}.getType());
        }
        return null;
    }

    /**
     * Get specific device
     */
    public @Nullable RachioDevice getDevice(@Nullable String deviceId) throws RachioApiException {
        if (deviceId == null || deviceId.isEmpty()) {
            throw new RachioApiException("Device ID is required");
        }
        String response = get("device/" + deviceId);
        if (response != null) {
            return gson.fromJson(response, RachioDevice.class);
        }
        return null;
    }

    /**
     * Get specific zone
     */
    public @Nullable RachioZone getZone(@Nullable String deviceId, @Nullable String zoneId) throws RachioApiException {
        if (deviceId == null || deviceId.isEmpty() || zoneId == null || zoneId.isEmpty()) {
            throw new RachioApiException("Device ID and Zone ID are required");
        }
        String response = get("zone/" + zoneId);
        if (response != null) {
            return gson.fromJson(response, RachioZone.class);
        }
        return null;
    }

    /**
     * Start a zone
     */
    public void startZone(@Nullable String deviceId, @Nullable String zoneId, int duration) throws RachioApiException {
        if (deviceId == null || deviceId.isEmpty() || zoneId == null || zoneId.isEmpty()) {
            throw new RachioApiException("Device ID and Zone ID are required");
        }
        
        JsonObject json = new JsonObject();
        json.addProperty("id", zoneId);
        json.addProperty("duration", duration);
        
        post("zone/start", json);
    }

    /**
     * Stop a zone
     */
    public void stopZone(@Nullable String deviceId, @Nullable String zoneId) throws RachioApiException {
        if (deviceId == null || deviceId.isEmpty() || zoneId == null || zoneId.isEmpty()) {
            throw new RachioApiException("Device ID and Zone ID are required");
        }
        
        JsonObject json = new JsonObject();
        json.addProperty("id", zoneId);
        
        put("zone/stop", json);
    }

    /**
     * Set zone enabled/disabled
     */
    public void setZoneEnabled(@Nullable String deviceId, @Nullable String zoneId, boolean enabled) throws RachioApiException {
        if (deviceId == null || deviceId.isEmpty() || zoneId == null || zoneId.isEmpty()) {
            throw new RachioApiException("Device ID and Zone ID are required");
        }
        
        JsonObject json = new JsonObject();
        json.addProperty("enabled", enabled);
        
        put("zone/" + zoneId, json);
    }

    /**
     * Start rain delay
     */
    public void startRainDelay(@Nullable String deviceId, int duration) throws RachioApiException {
        if (deviceId == null || deviceId.isEmpty()) {
            throw new RachioApiException("Device ID is required");
        }
        
        JsonObject json = new JsonObject();
        json.addProperty("duration", duration);
        
        post("device/" + deviceId + "/rainDelay", json);
    }

    /**
     * Stop rain delay
     */
    public void stopRainDelay(@Nullable String deviceId) throws RachioApiException {
        if (deviceId == null || deviceId.isEmpty()) {
            throw new RachioApiException("Device ID is required");
        }
        
        delete("device/" + deviceId + "/rainDelay");
    }

    /**
     * Get webhooks
     */
    public @Nullable RachioApiWebHookList getWebhooks() throws RachioApiException {
        String response = get("webhook");
        if (response != null) {
            return gson.fromJson(response, RachioApiWebHookList.class);
        }
        return null;
    }

    /**
     * Register a webhook
     */
    public void registerWebhook(String url, String externalId) throws RachioApiException {
        JsonObject json = new JsonObject();
        json.addProperty("url", url);
        json.addProperty("externalId", externalId);
        json.addProperty("eventTypes", "DEVICE_STATUS_EVENT,ZONE_STATUS_EVENT,RAIN_DELAY_EVENT");
        
        post("webhook", json);
    }

    /**
     * Delete all webhooks
     */
    public void deleteAllWebhooks() throws RachioApiException {
        delete("webhook/all");
    }

    /**
     * Get device forecast
     */
    public @Nullable RachioForecast getForecast(@Nullable String deviceId) throws RachioApiException {
        if (deviceId == null || deviceId.isEmpty()) {
            throw new RachioApiException("Device ID is required");
        }
        String response = get("device/" + deviceId + "/forecast");
        if (response != null) {
            return gson.fromJson(response, RachioForecast.class);
        }
        return null;
    }

    /**
     * Get device usage
     */
    public @Nullable RachioUsage getUsage(@Nullable String deviceId) throws RachioApiException {
        if (deviceId == null || deviceId.isEmpty()) {
            throw new RachioApiException("Device ID is required");
        }
        String response = get("device/" + deviceId + "/usage");
        if (response != null) {
            return gson.fromJson(response, RachioUsage.class);
        }
        return null;
    }

    /**
     * Get device alerts
     */
    public @Nullable List<RachioAlert> getAlerts(@Nullable String deviceId) throws RachioApiException {
        if (deviceId == null || deviceId.isEmpty()) {
            throw new RachioApiException("Device ID is required");
        }
        String response = get("device/" + deviceId + "/alerts");
        if (response != null) {
            return gson.fromJson(response, new com.google.gson.reflect.TypeToken<List<RachioAlert>>(){}.getType());
        }
        return null;
    }

    /**
     * Get device schedules
     */
    public @Nullable List<RachioSchedule> getSchedules(@Nullable String deviceId) throws RachioApiException {
        if (deviceId == null || deviceId.isEmpty()) {
            throw new RachioApiException("Device ID is required");
        }
        String response = get("device/" + deviceId + "/schedule");
        if (response != null) {
            return gson.fromJson(response, new com.google.gson.reflect.TypeToken<List<RachioSchedule>>(){}.getType());
        }
        return null;
    }

    /**
     * Get rate limits
     */
    public Map<String, Integer> getRateLimits() {
        Map<String, Integer> limits = new ConcurrentHashMap<>();
        for (Map.Entry<String, RateLimitInfo> entry : rateLimitMap.entrySet()) {
            limits.put(entry.getKey(), entry.getValue().remaining);
        }
        return limits;
    }

    // ============ UTILITY METHODS ============
    
    /**
     * Handle rate limiting headers from Rachio API
     */
    private void handleRateLimitHeaders(Map<String, java.util.List<String>> headers, String endpoint) {
        if (headers.containsKey(RATE_LIMIT_HEADER.toLowerCase())) {
            try {
                String remainingStr = headers.get(RATE_LIMIT_HEADER.toLowerCase()).get(0);
                String resetStr = headers.containsKey(RATE_RESET_HEADER.toLowerCase()) 
                    ? headers.get(RATE_RESET_HEADER.toLowerCase()).get(0) 
                    : null;
                
                RateLimitInfo info = new RateLimitInfo();
                info.remaining = Integer.parseInt(remainingStr);
                info.lastUpdated = Instant.now();
                
                if (resetStr != null) {
                    try {
                        // Rachio uses UNIX timestamp for reset
                        long resetTimestamp = Long.parseLong(resetStr);
                        info.resetTime = Instant.ofEpochSecond(resetTimestamp);
                    } catch (NumberFormatException e) {
                        logger.debug("Invalid reset timestamp: {}", resetStr);
                        info.resetTime = Instant.now().plusSeconds(3600); // Default 1 hour
                    }
                }
                
                rateLimitMap.put(endpoint, info);
                logger.debug("Rate limit for {}: {} remaining, resets at {}", 
                    endpoint, info.remaining, info.resetTime);
                
            } catch (Exception e) {
                logger.debug("Failed to parse rate limit headers", e);
            }
        }
    }

    /**
     * Get remaining rate limit for an endpoint
     */
    public int getRemainingRateLimit(String endpoint) {
        RateLimitInfo info = rateLimitMap.get(endpoint);
        return info != null ? info.remaining : -1;
    }

    /**
     * Get time until rate limit resets
     */
    public @Nullable Instant getRateLimitReset(String endpoint) {
        RateLimitInfo info = rateLimitMap.get(endpoint);
        return info != null ? info.resetTime : null;
    }

    /**
     * Check if we're approaching rate limit
     */
    public boolean isApproachingRateLimit(String endpoint) {
        RateLimitInfo info = rateLimitMap.get(endpoint);
        if (info == null) return false;
        
        // Consider approaching if less than 20% remaining or less than 10 requests
        return info.remaining < 10 || (info.remaining < 20 && info.resetTime != null 
            && Instant.now().isBefore(info.resetTime.minusSeconds(300)));
    }

    /**
     * Get the HttpClient instance (for testing or advanced use)
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Get the Gson instance
     */
    public Gson getGson() {
        return gson;
    }

    /**
     * Test the API connection
     */
    public boolean testConnection() {
        try {
            String response = get("person/info");
            if (response != null) {
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                return json.has("id") && !json.get("id").isJsonNull();
            }
            return false;
        } catch (RachioApiException e) {
            logger.debug("Connection test failed", e);
            return false;
        }
    }

    /**
     * Close resources (though HttpClient is auto-closeable)
     */
    public void close() {
        // Java HttpClient doesn't need explicit closing in this configuration
        // It will be garbage collected
    }
}
