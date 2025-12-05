package org.openhab.binding.rachio.internal.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.*;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles HTTP communication with Rachio API
 * Based on Rachio API documentation: https://rachio.readme.io/reference/getting-started
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioHttp {
    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);

    // CORRECTED: Base URL from Rachio API docs
    private static final String RACHIO_API_URL = "https://api.rach.io/1/public";
    private static final String CONTENT_TYPE = "application/json";
    private static final String USER_AGENT = "OpenHAB-Rachio-Binding/5.0";
    
    private final HttpClient httpClient;
    private final Gson gson;
    private final String apiKey;
    
    // Rate limiting tracking
    private final Map<String, RateLimitInfo> rateLimitInfo = new ConcurrentHashMap<>();
    
    private static class RateLimitInfo {
        int remaining = 17280; // Rachio daily limit: 17280 calls/day
        int limit = 17280;
        Instant resetTime = Instant.now().plusSeconds(86400); // 24 hours
        String deviceId;
        
        RateLimitInfo(String deviceId) {
            this.deviceId = deviceId;
        }
        
        boolean isLimited() {
            return remaining <= 100; // Slow down when < 100 calls left
        }
        
        double getUsagePercentage() {
            return limit > 0 ? ((double) (limit - remaining) / limit) * 100.0 : 0.0;
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
    private void parseRateLimitHeaders(java.net.http.HttpHeaders headers, String endpoint) {
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
            
            // Parse rate limit headers (Rachio uses X-RateLimit headers)
            List<String> remainingList = headers.allValues("X-RateLimit-Remaining");
            List<String> limitList = headers.allValues("X-RateLimit-Limit");
            List<String> resetList = headers.allValues("X-RateLimit-Reset");
            
            if (!remainingList.isEmpty()) {
                try {
                    info.remaining = Integer.parseInt(remainingList.get(0));
                } catch (NumberFormatException e) {
                    logger.debug("Invalid X-RateLimit-Remaining header: {}", remainingList.get(0));
                }
            }
            
            if (!limitList.isEmpty()) {
                try {
                    info.limit = Integer.parseInt(limitList.get(0));
                } catch (NumberFormatException e) {
                    logger.debug("Invalid X-RateLimit-Limit header: {}", limitList.get(0));
                }
            }
            
            if (!resetList.isEmpty()) {
                try {
                    // Parse as Unix timestamp (seconds since epoch)
                    long resetSeconds = Long.parseLong(resetList.get(0));
                    info.resetTime = Instant.ofEpochSecond(resetSeconds);
                } catch (Exception e) {
                    logger.debug("Invalid X-RateLimit-Reset header: {}", resetList.get(0));
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
                if (responseBody.length() > 100) {
                    errorMessage = "HTTP " + statusCode + ": " + responseBody.substring(0, 100) + "...";
                } else {
                    errorMessage = "HTTP " + statusCode + ": " + responseBody;
                }
            }
        } else {
            errorMessage = "HTTP " + statusCode + " for endpoint: " + endpoint;
        }
        
        logger.error("Rachio API error: {}", errorMessage);
        throw new RachioApiException(errorMessage);
    }

    // ========== PERSON ENDPOINTS ==========
    
    /**
     * Get person information (GET /person/info)
     */
    public @Nullable RachioPerson getPerson() throws RachioApiException {
        JsonElement response = makeRequest("GET", "/person/info", null);
        if (response != null && response.isJsonObject()) {
            return gson.fromJson(response, RachioPerson.class);
        }
        return null;
    }

    // ========== DEVICE ENDPOINTS ==========
    
    /**
     * Get device information (GET /device/{id})
     */
    public @Nullable RachioDevice getDevice(String deviceId) throws RachioApiException {
        JsonElement response = makeRequest("GET", "/device/" + deviceId, null);
        if (response != null && response.isJsonObject()) {
            return gson.fromJson(response, RachioDevice.class);
        }
        return null;
    }
    
    /**
     * Get device events (GET /device/{id}/event?hours={hours})
     */
    public @Nullable RachioEventSummary getDeviceEvents(String deviceId, int hours) throws RachioApiException {
        JsonElement response = makeRequest("GET", "/device/" + deviceId + "/event?hours=" + hours, null);
        if (response != null && response.isJsonObject()) {
            return gson.fromJson(response, RachioEventSummary.class);
        }
        return null;
    }
    
    /**
     * Get device forecast (GET /device/{id}/forecast)
     */
    public @Nullable RachioForecast getDeviceForecast(String deviceId) throws RachioApiException {
        JsonElement response = makeRequest("GET", "/device/" + deviceId + "/forecast", null);
        if (response != null && response.isJsonObject()) {
            return gson.fromJson(response, RachioForecast.class);
        }
        return null;
    }
    
    /**
     * Get device usage (GET /device/{id}/usage)
     */
    public @Nullable RachioUsage getDeviceUsage(String deviceId) throws RachioApiException {
        JsonElement response = makeRequest("GET", "/device/" + deviceId + "/usage", null);
        if (response != null && response.isJsonObject()) {
            return gson.fromJson(response, RachioUsage.class);
        }
        return null;
    }
    
    /**
     * Get device savings (GET /device/{id}/savings)
     */
    public @Nullable JsonElement getDeviceSavings(String deviceId) throws RachioApiException {
        return makeRequest("GET", "/device/" + deviceId + "/savings", null);
    }
    
    /**
     * Get device alerts (GET /device/{id}/alerts)
     */
    public @Nullable List<RachioAlert> getDeviceAlerts(String deviceId) throws RachioApiException {
        JsonElement response = makeRequest("GET", "/device/" + deviceId + "/alerts", null);
        if (response != null && response.isJsonArray()) {
            RachioAlert[] alerts = gson.fromJson(response, RachioAlert[].class);
            return Arrays.asList(alerts);
        }
        return null;
    }

    // ========== DEVICE CONTROL ENDPOINTS ==========
    
    /**
     * Stop watering (PUT /device/stop_water)
     */
    public void stopWatering(String deviceId) throws RachioApiException {
        JsonObject body = new JsonObject();
        body.addProperty("id", deviceId);
        
        makeRequest("PUT", "/device/stop_water", gson.toJson(body));
        logger.info("Stopped watering on device {}", deviceId);
    }
    
    /**
     * Set rain delay (PUT /device/rain_delay)
     */
    public void setRainDelay(String deviceId, int hours) throws RachioApiException {
        if (hours < 0 || hours > 168) {
            throw new RachioApiException("Rain delay hours must be between 0 and 168");
        }
        
        JsonObject body = new JsonObject();
        body.addProperty("id", deviceId);
        body.addProperty("duration", hours * 3600); // Convert hours to seconds
        
        makeRequest("PUT", "/device/rain_delay", gson.toJson(body));
        logger.info("Set rain delay on device {} to {} hours", deviceId, hours);
    }
    
    /**
     * Pause device (PUT /device/pause)
     */
    public void pauseDevice(String deviceId, boolean paused) throws RachioApiException {
        JsonObject body = new JsonObject();
        body.addProperty("id", deviceId);
        body.addProperty("paused", paused);
        
        makeRequest("PUT", "/device/pause", gson.toJson(body));
        logger.info("Device {} {}", deviceId, paused ? "paused" : "unpaused");
    }

    // ========== ZONE ENDPOINTS ==========
    
    /**
     * Start a zone (PUT /zone/start)
     */
    public void startZone(String zoneId, int duration) throws RachioApiException {
        if (duration <= 0) {
            throw new RachioApiException("Duration must be greater than 0");
        }
        
        JsonObject body = new JsonObject();
        body.addProperty("id", zoneId);
        body.addProperty("duration", duration);
        
        makeRequest("PUT", "/zone/start", gson.toJson(body));
        logger.info("Started zone {} for {} seconds", zoneId, duration);
    }
    
    /**
     * Start multiple zones (PUT /zone/start_multiple)
     */
    public void startMultipleZones(Map<String, Integer> zoneDurations) throws RachioApiException {
        JsonObject body = new JsonObject();
        JsonArray zonesArray = new JsonArray();
        
        for (Map.Entry<String, Integer> entry : zoneDurations.entrySet()) {
            JsonObject zoneObj = new JsonObject();
            zoneObj.addProperty("id", entry.getKey());
            zoneObj.addProperty("duration", entry.getValue());
            zonesArray.add(zoneObj);
        }
        body.add("zones", zonesArray);
        
        makeRequest("PUT", "/zone/start_multiple", gson.toJson(body));
        logger.info("Started {} zones", zoneDurations.size());
    }
    
    /**
     * Enable/disable zone (PUT /zone/enable)
     */
    public void setZoneEnabled(String zoneId, boolean enabled) throws RachioApiException {
        JsonObject body = new JsonObject();
        body.addProperty("id", zoneId);
        body.addProperty("enabled", enabled);
        
        makeRequest("PUT", "/zone/enable", gson.toJson(body));
        logger.info("Zone {} {}", zoneId, enabled ? "enabled" : "disabled");
    }
    
    /**
     * Get zone watering history (GET /zone/{id}/watering?days={days})
     */
    public @Nullable JsonElement getZoneWateringHistory(String zoneId, int days) throws RachioApiException {
        return makeRequest("GET", "/zone/" + zoneId + "/watering?days=" + days, null);
    }

    // ========== SCHEDULE ENDPOINTS ==========
    
    /**
     * Get device schedules (GET /device/{id}/schedule)
     */
    public @Nullable List<RachioSchedule> getDeviceSchedules(String deviceId) throws RachioApiException {
        JsonElement response = makeRequest("GET", "/device/" + deviceId + "/schedule", null);
        if (response != null && response.isJsonArray()) {
            RachioSchedule[] schedules = gson.fromJson(response, RachioSchedule[].class);
            return Arrays.asList(schedules);
        }
        return null;
    }
    
    /**
     * Start schedule (PUT /schedule/start)
     */
    public void startSchedule(String scheduleId) throws RachioApiException {
        JsonObject body = new JsonObject();
        body.addProperty("id", scheduleId);
        
        makeRequest("PUT", "/schedule/start", gson.toJson(body));
        logger.info("Started schedule {}", scheduleId);
    }
    
    /**
     * Stop schedule (PUT /schedule/stop)
     */
    public void stopSchedule(String scheduleId) throws RachioApiException {
        JsonObject body = new JsonObject();
        body.addProperty("id", scheduleId);
        
        makeRequest("PUT", "/schedule/stop", gson.toJson(body));
        logger.info("Stopped schedule {}", scheduleId);
    }

    // ========== WEBHOOK ENDPOINTS ==========
    
    /**
     * Register webhook (POST /webhook)
     */
    public void registerWebhook(String deviceId, String url, String externalId) throws RachioApiException {
        JsonObject body = new JsonObject();
        body.addProperty("url", url);
        body.addProperty("externalId", externalId);
        body.addProperty("deviceId", deviceId);
        // Register for all event types
        body.addProperty("eventTypes", "DEVICE_STATUS_EVENT,ZONE_STATUS_EVENT,SCHEDULE_STATUS_EVENT,RAIN_DELAY_EVENT,WEATHER_INTELLIGENCE_EVENT,WATER_BUDGET_EVENT,RAIN_SENSOR_DETECTION_EVENT");
        
        makeRequest("POST", "/webhook", gson.toJson(body));
        logger.info("Registered webhook for device {} with URL {}", deviceId, url);
    }
    
    /**
     * Delete webhook (DELETE /webhook)
     */
    public void deleteWebhook(String webhookId) throws RachioApiException {
        JsonObject body = new JsonObject();
        body.addProperty("id", webhookId);
        
        makeRequest("DELETE", "/webhook", gson.toJson(body));
        logger.info("Deleted webhook {}", webhookId);
    }
    
    /**
     * List webhooks for device (GET /webhook/device/{deviceId})
     */
    public @Nullable List<RachioApiWebHookEntry> listWebhooks(String deviceId) throws RachioApiException {
        JsonElement response = makeRequest("GET", "/webhook/device/" + deviceId, null);
        if (response != null && response.isJsonArray()) {
            RachioApiWebHookList list = gson.fromJson(response, RachioApiWebHookList.class);
            return list.webhooks;
        }
        return null;
    }

    // ========== COMPLEX OPERATIONS ==========
    
    /**
     * Run all zones sequentially
     */
    public void runAllZones(String thingId, int duration, String deviceId) throws RachioApiException {
        logger.info("Running all zones on device {} for {} seconds each", deviceId, duration);
        
        if (duration <= 0) {
            throw new RachioApiException("Duration must be greater than 0");
        }
        
        // Get device to find all zones
        RachioDevice device = getDevice(deviceId);
        if (device == null || device.zones == null) {
            throw new RachioApiException("Could not retrieve device zones");
        }
        
        // Start each enabled zone
        for (RachioZone zone : device.zones) {
            if (zone.enabled) {
                try {
                    startZone(zone.id, duration);
                    logger.debug("Started zone {} for {} seconds", zone.name, duration);
                } catch (RachioApiException e) {
                    logger.error("Failed to start zone {}: {}", zone.name, e.getMessage());
                    // Continue with other zones
                }
            }
        }
        
        logger.info("Completed starting all zones on device {}", deviceId);
    }
    
    /**
     * Run next available zone
     */
    public void runNextZone(String thingId, int duration, String deviceId) throws RachioApiException {
        logger.info("Running next zone on device {} for {} seconds", deviceId, duration);
        
        if (duration <= 0) {
            throw new RachioApiException("Duration must be greater than 0");
        }
        
        // Get device to find zones
        RachioDevice device = getDevice(deviceId);
        if (device == null || device.zones == null) {
            throw new RachioApiException("Could not retrieve device zones");
        }
        
        // Find first enabled zone
        for (RachioZone zone : device.zones) {
            if (zone.enabled) {
                try {
                    startZone(zone.id, duration);
                    logger.info("Started zone {} for {} seconds", zone.name, duration);
                    return;
                } catch (RachioApiException e) {
                    logger.debug("Zone {} failed, trying next zone", zone.name);
                    // Continue to next zone
                }
            }
        }
        
        throw new RachioApiException("No enabled zones available to run");
    }

    // ========== RATE LIMIT MANAGEMENT ==========
    
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
            result.put("remaining", 17280);
            result.put("limit", 17280);
            result.put("resetTime", Instant.now().plusSeconds(86400).toString());
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
}
