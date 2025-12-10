package org.openhab.binding.rachio.internal.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.*;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

/**
 * The {@link RachioHttp} class handles all HTTP communication with the Rachio API
 * Uses modern HttpClient with HTTP/2 support and proper rate limiting
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioHttp {

    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);
    
    private final HttpClient httpClient;
    private final String apiKey;
    private final Gson gson;
    
    // Rate limiting tracking
    private final Map<String, RateLimitInfo> rateLimitCache = new ConcurrentHashMap<>();
    private int remainingRequests = 60; // Default Rachio limit
    private int limit = 60;
    private long resetTime = 0;
    
    // Cache for responses
    private final Map<String, CacheEntry> responseCache = new ConcurrentHashMap<>();
    
    /**
     * Rate limit information structure
     */
    private static class RateLimitInfo {
        int remaining;
        int limit;
        long resetTime;
        
        RateLimitInfo(int remaining, int limit, long resetTime) {
            this.remaining = remaining;
            this.limit = limit;
            this.resetTime = resetTime;
        }
    }
    
    /**
     * Cache entry for API responses
     */
    private static class CacheEntry {
        Object data;
        long timestamp;
        long ttl;
        
        CacheEntry(Object data, long ttl) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
            this.ttl = ttl;
        }
        
        boolean isValid() {
            return System.currentTimeMillis() - timestamp < ttl;
        }
    }

    public RachioHttp(String apiKey, HttpClientFactory httpClientFactory) {
        this.apiKey = apiKey;
        
        // Create HTTP client with modern settings
        this.httpClient = httpClientFactory.getCommonHttpClient();
        
        // Configure Gson with custom type adapters
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .registerTypeAdapterFactory(new SafeReflectiveTypeAdapterFactory())
                .create();
        
        logger.debug("RachioHttp initialized with API key: {}...", 
                apiKey.substring(0, Math.min(apiKey.length(), 8)));
    }

    /**
     * Make an authenticated API request to Rachio
     */
    private <T> @Nullable T makeRequest(String endpoint, String method, @Nullable Object body, 
                                        Class<T> responseType, boolean useCache) throws RachioApiException {
        
        // Check rate limits before making request
        checkRateLimits();
        
        // Build cache key
        String cacheKey = endpoint + ":" + method + ":" + (body != null ? body.hashCode() : 0);
        
        // Return cached response if available and valid
        if (useCache) {
            CacheEntry cached = responseCache.get(cacheKey);
            if (cached != null && cached.isValid()) {
                logger.debug("Returning cached response for {}", endpoint);
                return responseType.cast(cached.data);
            }
        }
        
        try {
            // Build request
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + endpoint))
                    .header(HEADER_AUTHORIZATION, "Bearer " + apiKey)
                    .header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                    .header(HEADER_ACCEPT, CONTENT_TYPE_JSON)
                    .timeout(Duration.ofSeconds(30));
            
            // Set method and body
            switch (method.toUpperCase()) {
                case "GET":
                    requestBuilder.GET();
                    break;
                case "POST":
                    if (body != null) {
                        String jsonBody = gson.toJson(body);
                        requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
                    } else {
                        requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
                    }
                    break;
                case "PUT":
                    if (body != null) {
                        String jsonBody = gson.toJson(body);
                        requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody));
                    } else {
                        requestBuilder.PUT(HttpRequest.BodyPublishers.noBody());
                    }
                    break;
                case "DELETE":
                    requestBuilder.DELETE();
                    break;
                default:
                    throw new RachioApiException("Unsupported HTTP method: " + method);
            }
            
            HttpRequest request = requestBuilder.build();
            
            // Execute request
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            // Update rate limit information from headers
            updateRateLimits(response);
            
            // Handle response
            int statusCode = response.statusCode();
            String responseBody = response.body();
            
            logger.debug("API {} {} -> Status: {}, Body length: {}", 
                    method, endpoint, statusCode, responseBody.length());
            
            if (statusCode >= 200 && statusCode < 300) {
                // Parse successful response
                T result = parseResponse(responseBody, responseType);
                
                // Cache successful GET responses
                if (useCache && "GET".equals(method)) {
                    long ttl = endpoint.contains("forecast") ? CACHE_DURATION_FORECAST : 
                              endpoint.contains("device") ? CACHE_DURATION_DEVICES : 
                              CACHE_DURATION_ZONES;
                    responseCache.put(cacheKey, new CacheEntry(result, ttl));
                }
                
                return result;
            } else {
                // Handle error response
                handleErrorResponse(statusCode, responseBody, endpoint);
                return null;
            }
            
        } catch (IOException e) {
            logger.error("Network error calling {} {}: {}", method, endpoint, e.getMessage(), e);
            throw new RachioApiException("Network error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Request interrupted for {} {}", method, endpoint, e);
            throw new RachioApiException("Request interrupted", e);
        } catch (JsonSyntaxException e) {
            logger.error("JSON parsing error for {} {}: {}", method, endpoint, e.getMessage(), e);
            throw new RachioApiException("JSON parsing error: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error calling {} {}: {}", method, endpoint, e.getMessage(), e);
            throw new RachioApiException("Unexpected error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse JSON response
     */
    private <T> T parseResponse(String json, Class<T> type) throws RachioApiException {
        try {
            return gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse JSON: {}", json.substring(0, Math.min(json.length(), 200)), e);
            throw new RachioApiException("Failed to parse API response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Handle error responses
     */
    private void handleErrorResponse(int statusCode, String responseBody, String endpoint) 
            throws RachioApiException {
        
        String errorMessage;
        try {
            // Try to parse error response
            Map<?, ?> error = gson.fromJson(responseBody, Map.class);
            errorMessage = error.containsKey("error") ? error.get("error").toString() : 
                          error.containsKey("message") ? error.get("message").toString() : 
                          "Unknown error";
        } catch (Exception e) {
            errorMessage = responseBody.length() > 200 ? 
                    responseBody.substring(0, 200) + "..." : responseBody;
        }
        
        logger.warn("API error {} for {}: {}", statusCode, endpoint, errorMessage);
        
        switch (statusCode) {
            case 400:
                throw new RachioApiException("Bad request: " + errorMessage);
            case 401:
                throw new RachioApiException("Unauthorized - check API key");
            case 403:
                throw new RachioApiException("Forbidden - insufficient permissions");
            case 404:
                throw new RachioApiException("Resource not found: " + endpoint);
            case 429:
                throw new RachioApiException("Rate limit exceeded - too many requests");
            case 500:
            case 502:
            case 503:
            case 504:
                throw new RachioApiException("Rachio API server error: " + errorMessage);
            default:
                throw new RachioApiException("HTTP " + statusCode + ": " + errorMessage);
        }
    }
    
    /**
     * Update rate limit information from response headers
     */
    private void updateRateLimits(HttpResponse<String> response) {
        try {
            String remainingHeader = response.headers().firstValue(HEADER_RATE_LIMIT_REMAINING).orElse(null);
            String limitHeader = response.headers().firstValue(HEADER_RATE_LIMIT_LIMIT).orElse(null);
            String resetHeader = response.headers().firstValue(HEADER_RATE_LIMIT_RESET).orElse(null);
            
            if (remainingHeader != null) {
                remainingRequests = Integer.parseInt(remainingHeader);
            }
            if (limitHeader != null) {
                limit = Integer.parseInt(limitHeader);
            }
            if (resetHeader != null) {
                resetTime = Long.parseLong(resetHeader) * 1000; // Convert to milliseconds
            }
            
            // Store in cache
            rateLimitCache.put("global", new RateLimitInfo(remainingRequests, limit, resetTime));
            
            logger.debug("Rate limits: {}/{} remaining, resets at {}", 
                    remainingRequests, limit, new Date(resetTime));
            
        } catch (Exception e) {
            logger.debug("Could not parse rate limit headers", e);
        }
    }
    
    /**
     * Check rate limits and apply adaptive throttling
     */
    private void checkRateLimits() throws RachioApiException {
        // Check if we're rate limited
        if (remainingRequests <= 0) {
            long now = System.currentTimeMillis();
            if (now < resetTime) {
                long waitTime = resetTime - now;
                logger.warn("Rate limit exceeded. Waiting {} ms before next request", waitTime);
                try {
                    Thread.sleep(Math.min(waitTime, 60000)); // Max 60 second sleep
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RachioApiException("Interrupted while waiting for rate limit reset");
                }
            }
            // Reset after waiting
            remainingRequests = limit;
        }
        
        // Adaptive throttling based on remaining requests
        if (remainingRequests < RATE_LIMIT_THRESHOLD_CRITICAL) {
            logger.debug("Critical rate limit: {} remaining. Applying aggressive throttling", remainingRequests);
            try {
                Thread.sleep(RATE_LIMIT_REFRESH_INTERVAL_CRITICAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else if (remainingRequests < RATE_LIMIT_THRESHOLD_LOW) {
            logger.debug("Low rate limit: {} remaining. Applying moderate throttling", remainingRequests);
            try {
                Thread.sleep(RATE_LIMIT_REFRESH_INTERVAL_SLOW);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Get person information (devices, webhooks, etc.)
     */
    public @Nullable RachioPerson getPersonInfo() throws RachioApiException {
        logger.debug("Getting person info");
        return makeRequest(API_PERSON, "GET", null, RachioPerson.class, true);
    }
    
    /**
     * Get device details
     */
    public @Nullable RachioDevice getDevice(String deviceId) throws RachioApiException {
        logger.debug("Getting device {}", deviceId);
        return makeRequest(API_DEVICE + "/" + deviceId, "GET", null, RachioDevice.class, true);
    }
    
    /**
     * Turn device on
     */
    public boolean turnDeviceOn(String deviceId) throws RachioApiException {
        logger.debug("Turning device {} on", deviceId);
        Map<String, Object> body = Map.of("id", deviceId);
        makeRequest(API_DEVICE_ON, "PUT", body, Map.class, false);
        return true;
    }
    
    /**
     * Turn device off
     */
    public boolean turnDeviceOff(String deviceId) throws RachioApiException {
        logger.debug("Turning device {} off", deviceId);
        Map<String, Object> body = Map.of("id", deviceId);
        makeRequest(API_DEVICE_OFF, "PUT", body, Map.class, false);
        return true;
    }
    
    /**
     * Pause/unpause device
     */
    public boolean pauseDevice(String deviceId, boolean pause) throws RachioApiException {
        logger.debug("{} device {}", pause ? "Pausing" : "Unpausing", deviceId);
        Map<String, Object> body = Map.of("id", deviceId, "duration", pause ? 1 : 0);
        makeRequest(API_DEVICE_PAUSE, "PUT", body, Map.class, false);
        return true;
    }
    
    /**
     * Stop all watering
     */
    public boolean stopWatering(String deviceId) throws RachioApiException {
        logger.debug("Stopping all watering on device {}", deviceId);
        Map<String, Object> body = Map.of("id", deviceId);
        makeRequest(API_DEVICE_STOP_WATER, "PUT", body, Map.class, false);
        return true;
    }
    
    /**
     * Start a zone
     */
    public boolean startZone(String zoneId, int durationSeconds) throws RachioApiException {
        logger.debug("Starting zone {} for {} seconds", zoneId, durationSeconds);
        Map<String, Object> body = Map.of("id", zoneId, "duration", durationSeconds);
        makeRequest(API_ZONE_START, "PUT", body, Map.class, false);
        return true;
    }
    
    /**
     * Start multiple zones
     */
    public boolean startMultipleZones(List<String> zoneIds, List<Integer> durations) throws RachioApiException {
        logger.debug("Starting {} zones", zoneIds.size());
        
        // Create zones array
        Map<String, Object>[] zonesArray = new Map[zoneIds.size()];
        for (int i = 0; i < zoneIds.size(); i++) {
            zonesArray[i] = Map.of("id", zoneIds.get(i), "duration", durations.get(i));
        }
        
        Map<String, Object> body = Map.of("zones", zonesArray);
        makeRequest(API_ZONE_START_MULTIPLE, "PUT", body, Map.class, false);
        return true;
    }
    
    /**
     * Stop a zone
     */
    public boolean stopZone(String zoneId) throws RachioApiException {
        logger.debug("Stopping zone {}", zoneId);
        Map<String, Object> body = Map.of("id", zoneId);
        makeRequest(API_ZONE_STOP, "PUT", body, Map.class, false);
        return true;
    }
    
    /**
     * Run a zone with schedule
     */
    public boolean runZone(String zoneId, Map<String, Object> schedule) throws RachioApiException {
        logger.debug("Running zone {} with schedule", zoneId);
        Map<String, Object> body = new HashMap<>();
        body.put("id", zoneId);
        body.putAll(schedule);
        makeRequest(API_ZONE_RUN, "PUT", body, Map.class, false);
        return true;
    }
    
    /**
     * Set zone properties
     */
    public boolean setZone(String zoneId, Map<String, Object> properties) throws RachioApiException {
        logger.debug("Setting properties for zone {}", zoneId);
        Map<String, Object> body = new HashMap<>();
        body.put("id", zoneId);
        body.putAll(properties);
        makeRequest(API_ZONE_SET, "PUT", body, Map.class, false);
        return true;
    }
    
    /**
     * Set zone enabled status
     */
    public boolean setZoneEnabled(String zoneId, boolean enabled) throws RachioApiException {
        logger.debug("Setting zone {} enabled = {}", zoneId, enabled);
        Map<String, Object> body = Map.of("id", zoneId, "enabled", enabled);
        return setZone(zoneId, body);
    }
    
    /**
     * Run all zones on device
     */
    public boolean runAllZones(String deviceId) throws RachioApiException {
        logger.debug("Running all zones on device {}", deviceId);
        // Get device to find all zones
        RachioDevice device = getDevice(deviceId);
        if (device == null || device.getZones() == null) {
            throw new RachioApiException("Device or zones not found");
        }
        
        // Start all zones with default duration
        List<String> zoneIds = new ArrayList<>();
        List<Integer> durations = new ArrayList<>();
        for (RachioZone zone : device.getZones()) {
            if (zone.isEnabled()) {
                zoneIds.add(zone.getId());
                durations.add(zone.getRuntime() != null ? zone.getRuntime() * 60 : 600); // Default 10 min
            }
        }
        
        if (!zoneIds.isEmpty()) {
            return startMultipleZones(zoneIds, durations);
        }
        
        return false;
    }
    
    /**
     * Run next zone on device
     */
    public boolean runNextZone(String deviceId) throws RachioApiException {
        logger.debug("Running next zone on device {}", deviceId);
        // This would need logic to determine which zone is "next"
        // For now, run the first enabled zone
        RachioDevice device = getDevice(deviceId);
        if (device == null || device.getZones() == null) {
            throw new RachioApiException("Device or zones not found");
        }
        
        for (RachioZone zone : device.getZones()) {
            if (zone.isEnabled()) {
                return startZone(zone.getId(), zone.getRuntime() != null ? zone.getRuntime() * 60 : 600);
            }
        }
        
        return false;
    }
    
    /**
     * Set rain delay
     */
    public boolean rainDelay(String deviceId, int hours) throws RachioApiException {
        logger.debug("Setting rain delay to {} hours for device {}", hours, deviceId);
        Map<String, Object> body = Map.of("id", deviceId, "duration", hours * 3600);
        makeRequest("/device/rain_delay", "PUT", body, Map.class, false);
        return true;
    }
    
    /**
     * Set water budget percentage
     */
    public boolean setWaterBudget(String deviceId, int percentage) throws RachioApiException {
        logger.debug("Setting water budget to {}% for device {}", percentage, deviceId);
        Map<String, Object> body = Map.of("id", deviceId, "percentage", percentage);
        makeRequest("/device/water_budget", "PUT", body, Map.class, false);
        return true;
    }
    
    /**
     * Get watering history for a zone
     */
    public @Nullable List<ZoneRunStatus> getWateringHistory(String zoneId) throws RachioApiException {
        logger.debug("Getting watering history for zone {}", zoneId);
        String endpoint = String.format("/zone/%s/watering", zoneId);
        ZoneRunStatus[] history = makeRequest(endpoint, "GET", null, ZoneRunStatus[].class, true);
        return history != null ? Arrays.asList(history) : null;
    }
    
    /**
     * Get schedule for device
     */
    public @Nullable RachioSchedule getSchedule(String deviceId) throws RachioApiException {
        logger.debug("Getting schedule for device {}", deviceId);
        String endpoint = String.format("/device/%s/schedule", deviceId);
        return makeRequest(endpoint, "GET", null, RachioSchedule.class, true);
    }
    
    /**
     * Set schedule for device
     */
    public boolean setSchedule(String deviceId, RachioSchedule schedule) throws RachioApiException {
        logger.debug("Setting schedule for device {}", deviceId);
        String endpoint = String.format("/device/%s/schedule", deviceId);
        makeRequest(endpoint, "PUT", schedule, Map.class, false);
        return true;
    }
    
    /**
     * Delete schedule
     */
    public boolean deleteSchedule(String deviceId, String scheduleId) throws RachioApiException {
        logger.debug("Deleting schedule {} for device {}", scheduleId, deviceId);
        String endpoint = String.format("/device/%s/schedule/%s", deviceId, scheduleId);
        makeRequest(endpoint, "DELETE", null, Map.class, false);
        return true;
    }
    
    /**
     * Get weather forecast for device
     */
    public @Nullable RachioForecast getForecast(String deviceId) throws RachioApiException {
        logger.debug("Getting forecast for device {}", deviceId);
        String endpoint = String.format("/device/%s/forecast", deviceId);
        return makeRequest(endpoint, "GET", null, RachioForecast.class, true);
    }
    
    /**
     * Get water usage for device
     */
    public @Nullable RachioUsage getUsage(String deviceId) throws RachioApiException {
        logger.debug("Getting usage for device {}", deviceId);
        String endpoint = String.format("/device/%s/usage", deviceId);
        return makeRequest(endpoint, "GET", null, RachioUsage.class, true);
    }
    
    /**
     * Get savings data for device
     */
    public @Nullable RachioSavings getSavings(String deviceId) throws RachioApiException {
        logger.debug("Getting savings for device {}", deviceId);
        String endpoint = String.format("/device/%s/savings", deviceId);
        return makeRequest(endpoint, "GET", null, RachioSavings.class, true);
    }
    
    /**
     * Get alerts for device
     */
    public @Nullable List<RachioAlert> getAlerts(String deviceId) throws RachioApiException {
        logger.debug("Getting alerts for device {}", deviceId);
        String endpoint = String.format("/device/%s/alerts", deviceId);
        RachioAlert[] alerts = makeRequest(endpoint, "GET", null, RachioAlert[].class, true);
        return alerts != null ? Arrays.asList(alerts) : null;
    }
    
    /**
     * Get all webhooks
     */
    public @Nullable RachioApiWebHookList getWebhooks() throws RachioApiException {
        logger.debug("Getting webhooks");
        return makeRequest(API_WEBHOOK, "GET", null, RachioApiWebHookList.class, true);
    }
    
    /**
     * Create a webhook
     */
    public @Nullable RachioApiWebHookEntry createWebhook(String url, String externalId, String[] eventTypes) 
            throws RachioApiException {
        logger.debug("Creating webhook for URL: {}", url);
        Map<String, Object> body = Map.of(
            "url", url,
            "externalId", externalId,
            "eventTypes", eventTypes
        );
        return makeRequest(API_WEBHOOK, "POST", body, RachioApiWebHookEntry.class, false);
    }
    
    /**
     * Delete a webhook
     */
    public boolean deleteWebhook(String webhookId) throws RachioApiException {
        logger.debug("Deleting webhook: {}", webhookId);
        String endpoint = API_WEBHOOK + "/" + webhookId;
        makeRequest(endpoint, "DELETE", null, Map.class, false);
        return true;
    }
    
    /**
     * Parse webhook event from JSON
     */
    public static @Nullable RachioWebHookEvent parseWebhookEvent(String json, String eventType) {
        try {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                    .create();
            
            RachioWebHookEvent event = gson.fromJson(json, RachioWebHookEvent.class);
            if (event != null) {
                event.setEventType(eventType);
            }
            return event;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Clear response cache
     */
    public void clearCache() {
        responseCache.clear();
        logger.debug("Response cache cleared");
    }
    
    /**
     * Clear cache for specific endpoint
     */
    public void clearCache(String endpoint) {
        responseCache.keySet().removeIf(key -> key.startsWith(endpoint));
        logger.debug("Cache cleared for {}", endpoint);
    }
    
    /**
     * Get remaining API requests
     */
    public int getRemainingRequests() {
        return remainingRequests;
    }
    
    /**
     * Get rate limit
     */
    public int getRateLimit() {
        return limit;
    }
    
    /**
     * Get rate limit reset time
     */
    public long getResetTime() {
        return resetTime;
    }
    
    /**
     * Get rate limit status as percentage
     */
    public int getRateLimitPercentage() {
        if (limit <= 0) return 0;
        return (int) ((remainingRequests / (double) limit) * 100);
    }
    
    /**
     * Check if rate limit is critical
     */
    public boolean isRateLimitCritical() {
        return remainingRequests < RATE_LIMIT_THRESHOLD_CRITICAL;
    }
    
    /**
     * Check if rate limit is low
     */
    public boolean isRateLimitLow() {
        return remainingRequests < RATE_LIMIT_THRESHOLD_LOW;
    }
}
