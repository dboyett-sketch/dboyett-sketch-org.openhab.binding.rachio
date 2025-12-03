package org.openhab.binding.rachio.internal.api;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * HTTP client for Rachio API calls using modern Java HttpClient
 * Now includes rate limit monitoring, adaptive behavior, and WEBHOOK SUPPORT
 * FIXED: Webhook endpoints, rate limit header parsing for ISO date format
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class RachioHttp {
    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);
    private final Gson gson = new Gson();
    private final Map<String, String> thingApiKeys = new HashMap<>();

    // HttpClient with longer timeout for webhook operations
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    // ===== RATE LIMIT TRACKING =====
    private int rateLimitLimit = 3500; // Default Rachio hourly limit (from your logs: 3500)
    private int rateLimitRemaining = 3500;
    private long rateLimitResetTime = 0; // Unix timestamp when limit resets
    private boolean rateLimitWasWarning = false;
    private boolean rateLimitWasCritical = false;
    // ===== END RATE LIMIT TRACKING =====

    // ===== WEBHOOK SECRET MANAGEMENT =====
    private final Map<String, String> webhookSecrets = new HashMap<>(); // webhookId -> secret
    // ===== END WEBHOOK SECRET MANAGEMENT =====

    // ===== WEBHOOK EVENT TYPE CONFIGURATION =====
    // Default webhook events to register (using Rachio event type IDs)
    private static final List<String> DEFAULT_WEBHOOK_EVENT_IDS = Arrays.asList(
        WEBHOOK_EVENT_ID_DEVICE_STATUS,          // "5"
        WEBHOOK_EVENT_ID_RAIN_DELAY,             // "6"
        WEBHOOK_EVENT_ID_WEATHER_INTELLIGENCE,   // "7"
        WEBHOOK_EVENT_ID_WATER_BUDGET,           // "8"
        WEBHOOK_EVENT_ID_SCHEDULE_STATUS,        // "9"
        WEBHOOK_EVENT_ID_ZONE_STATUS,            // "10"
        WEBHOOK_EVENT_ID_RAIN_SENSOR_DETECTION,  // "11"
        WEBHOOK_EVENT_ID_ZONE_DELTA,             // "12"
        WEBHOOK_EVENT_ID_DELTA                   // "14"
    );
    // ===== END WEBHOOK EVENT TYPE CONFIGURATION =====

    public RachioHttp() {
    }

    public void activate() {
        logger.debug("RachioHttp activated with webhook support");
    }

    public void deactivate() {
        thingApiKeys.clear();
        webhookSecrets.clear();
        logger.debug("RachioHttp deactivated");
    }

    public void registerThing(String thingId, String apiKey) {
        thingApiKeys.put(thingId, apiKey);
        logger.debug("Registered API key for thing: {}", thingId);
    }

    public void unregisterThing(String thingId) {
        thingApiKeys.remove(thingId);
        logger.debug("Unregistered API key for thing: {}", thingId);
    }

    @Nullable
    private String getApiKey(String thingId) {
        return thingApiKeys.get(thingId);
    }

    /**
     * Get any available API key (from bridge)
     */
    @Nullable
    private String getAnyApiKey() {
        if (!thingApiKeys.isEmpty()) {
            // Return the first available API key (should be from bridge)
            return thingApiKeys.values().iterator().next();
        }
        return null;
    }

    // ===== WEBHOOK API METHODS - FIXED VERSION =====

    /**
     * Register a webhook with Rachio API - FIXED
     * 
     * @param thingId The thing ID (bridge) making the request
     * @param callbackUrl The URL where Rachio should send webhooks
     * @param externalId External identifier for this webhook (usually bridge ID)
     * @param eventTypeIds List of event type IDs to subscribe to (use DEFAULT_WEBHOOK_EVENT_IDS)
     * @return The webhook ID from Rachio, or null if failed
     * @throws RachioException If registration fails
     */
    @Nullable
    public String registerWebhook(String thingId, String callbackUrl, String externalId, String deviceId, List<String> eventTypeIds) 
            throws RachioException {
        
        String apiKey = getApiKey(thingId);
        if (apiKey == null) {
            throw new RachioException("No API key registered for thing: " + thingId);
        }

        try {
            // FIX: Use correct endpoint (NO double "public" - base URL doesn't have it)
            String url = RACHIO_API_BASE + API_WEBHOOK_REGISTER;
            logger.debug("Registering webhook at URL: {}", url);
            
            // FIX: Build correct JSON payload based on Rachio API requirements
            Map<String, Object> request = new HashMap<>();
            
            // Device info object (REQUIRED by Rachio API)
            Map<String, String> deviceMap = new HashMap<>();
            deviceMap.put("id", deviceId);
            request.put("device", deviceMap);
            
            request.put("externalId", externalId);
            request.put("url", callbackUrl);
            
            // Event types must be array of objects with "id" field
            List<Map<String, String>> eventTypesList = new ArrayList<>();
            for (String eventTypeId : eventTypeIds) {
                Map<String, String> eventTypeMap = new HashMap<>();
                eventTypeMap.put("id", eventTypeId);
                eventTypesList.add(eventTypeMap);
            }
            request.put("eventTypes", eventTypesList);
            
            String payload = gson.toJson(request);
            logger.debug("Webhook registration payload: {}", payload);
            
            String response = httpPost(url, apiKey, payload);
            
            // Parse response
            Map<String, Object> responseMap = gson.fromJson(response, Map.class);
            if (responseMap != null && responseMap.containsKey("id")) {
                String webhookId = (String) responseMap.get("id");
                
                // Store webhook secret if provided
                if (responseMap.containsKey("secret")) {
                    String secret = (String) responseMap.get("secret");
                    webhookSecrets.put(webhookId, secret);
                    logger.info("Registered webhook successfully. ID: {}, Secret stored.", webhookId);
                } else {
                    logger.info("Registered webhook successfully. ID: {}", webhookId);
                }
                
                return webhookId;
            } else {
                logger.error("Webhook registration failed - invalid response: {}", response);
                throw new RachioException("Invalid webhook registration response");
            }
            
        } catch (IOException | InterruptedException | JsonSyntaxException e) {
            throw new RachioException("Failed to register webhook: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a webhook from Rachio API - FIXED
     * 
     * @param thingId The thing ID (bridge) making the request
     * @param webhookId The webhook ID to delete
     * @throws RachioException If deletion fails
     */
    public void deleteWebhook(String thingId, String webhookId) throws RachioException {
        String apiKey = getApiKey(thingId);
        if (apiKey == null) {
            throw new RachioException("No API key registered for thing: " + thingId);
        }

        try {
            // FIX: Use correct endpoint
            String url = RACHIO_API_BASE + API_WEBHOOK_DELETE.replace("{webhookId}", webhookId);
            logger.debug("Deleting webhook at URL: {}", url);
            
            httpDelete(url, apiKey);
            
            // Remove secret from storage
            webhookSecrets.remove(webhookId);
            logger.info("Successfully deleted webhook: {}", webhookId);
            
        } catch (IOException | InterruptedException e) {
            throw new RachioException("Failed to delete webhook " + webhookId + ": " + e.getMessage(), e);
        }
    }

    /**
     * List all webhooks for a specific device - FIXED
     * 
     * @param thingId The thing ID (bridge) making the request
     * @param deviceId The device ID to list webhooks for
     * @return List of webhook entries
     * @throws RachioException If listing fails
     */
    public List<RachioApiWebHookEntry> listWebhooks(String thingId, String deviceId) throws RachioException {
        String apiKey = getApiKey(thingId);
        if (apiKey == null) {
            throw new RachioException("No API key registered for thing: " + thingId);
        }

        try {
            // FIX: Use correct endpoint with device ID
            String url = RACHIO_API_BASE + API_WEBHOOK_LIST.replace("{deviceId}", deviceId);
            logger.debug("Listing webhooks at URL: {}", url);
            
            String response = httpGet(url, apiKey);
            
            // Rachio returns an array of webhook objects
            RachioApiWebHookEntry[] webhookArray = gson.fromJson(response, RachioApiWebHookEntry[].class);
            if (webhookArray != null) {
                List<RachioApiWebHookEntry> webhooks = Arrays.asList(webhookArray);
                logger.debug("Found {} webhooks for device: {}", webhooks.size(), deviceId);
                return webhooks;
            } else {
                logger.debug("No webhooks found or invalid response for device: {}", deviceId);
                return new ArrayList<>();
            }
            
        } catch (IOException | InterruptedException | JsonSyntaxException e) {
            throw new RachioException("Failed to list webhooks for device " + deviceId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Clear all webhooks for a device - FIXED
     * 
     * @param thingId The thing ID (bridge) making the request
     * @param deviceId The device ID to clear webhooks for
     * @throws RachioException If clearing fails
     */
    public void clearAllWebhooks(String thingId, String deviceId) throws RachioException {
        List<RachioApiWebHookEntry> webhooks = listWebhooks(thingId, deviceId);
        int clearedCount = 0;
        
        for (RachioApiWebHookEntry webhook : webhooks) {
            try {
                deleteWebhook(thingId, webhook.id);
                clearedCount++;
                logger.debug("Cleared webhook: {}", webhook.id);
            } catch (Exception e) {
                logger.warn("Failed to delete webhook {} during clearAll: {}", webhook.id, e.getMessage());
                // Continue with other webhooks
            }
        }
        
        if (clearedCount > 0) {
            logger.info("Cleared {} webhooks for device: {}", clearedCount, deviceId);
        } else {
            logger.debug("No webhooks to clear for device: {}", deviceId);
        }
    }

    /**
     * Get webhook secret for HMAC validation
     * 
     * @param webhookId The webhook ID
     * @return The secret, or null if not found
     */
    @Nullable
    public String getWebhookSecret(String webhookId) {
        return webhookSecrets.get(webhookId);
    }

    /**
     * Get default webhook event type IDs
     */
    public List<String> getDefaultWebhookEventIds() {
        return new ArrayList<>(DEFAULT_WEBHOOK_EVENT_IDS);
    }

    /**
     * Validate HMAC signature for webhook
     * 
     * @param payload The raw webhook payload
     * @param receivedSignature The signature from X-Rachio-Signature header
     * @param webhookId The webhook ID from X-Rachio-Webhook-ID header
     * @return true if signature is valid
     */
    public boolean validateWebhookSignature(String payload, String receivedSignature, String webhookId) {
        String secret = getWebhookSecret(webhookId);
        if (secret == null || secret.isEmpty()) {
            logger.warn("No secret found for webhook ID: {}, cannot validate signature", webhookId);
            return false;
        }

        try {
            // Rachio uses SHA256 HMAC with webhook secret
            // Format: webhookId + payload
            String data = webhookId + payload;
            String computedSignature = computeHmacSha256(data, secret);
            
            // Constant-time comparison to prevent timing attacks
            boolean isValid = constantTimeEquals(computedSignature, receivedSignature);
            
            if (!isValid) {
                logger.warn("Webhook signature validation FAILED for webhook ID: {}", webhookId);
                logger.debug("Computed: {}, Received: {}", 
                    computedSignature.length() > 16 ? computedSignature.substring(0, 16) + "..." : computedSignature,
                    receivedSignature.length() > 16 ? receivedSignature.substring(0, 16) + "..." : receivedSignature);
            } else {
                logger.debug("Webhook signature validated successfully for webhook ID: {}", webhookId);
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.error("Error validating webhook signature: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Compute HMAC-SHA256 signature
     */
    private String computeHmacSha256(String data, String secret) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
            secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Constant-time string comparison to prevent timing attacks
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    // ===== WEBHOOK DTOs =====

    public static class RachioApiWebHookEntry {
        public String id;
        public String url;
        public String externalId;
        public Device device;
        public List<EventType> eventTypes;
        
        public static class Device {
            public String id;
            public String name;
        }
        
        public static class EventType {
            public String id;
            public String name;
        }
    }

    // ===== END WEBHOOK API METHODS =====

    /**
     * Get person information - FIXED to get full person data with devices
     */
    public @Nullable RachioPerson getPerson(String thingId) throws RachioException {
        String apiKey = getApiKey(thingId);
        if (apiKey == null) {
            throw new RachioException("No API key registered for thing: " + thingId);
        }

        try {
            // Step 1: Get basic person info to get the person ID
            String url = RACHIO_API_BASE + API_PERSON_INFO;
            logger.debug("Getting person info from: {}", url);
            String response = httpGet(url, apiKey);
            logger.debug("Raw API response for person info: {}", response);
            
            RachioPerson basicPerson = gson.fromJson(response, RachioPerson.class);
            if (basicPerson == null || basicPerson.id.isEmpty()) {
                throw new RachioException("Invalid person response from API");
            }
            
            // Step 2: Get full person data with devices using the person ID
            String fullPersonUrl = RACHIO_API_BASE + API_PERSON_BY_ID + basicPerson.id;
            logger.debug("Getting full person data from: {}", fullPersonUrl);
            String fullResponse = httpGet(fullPersonUrl, apiKey);
            logger.debug("Raw API response for full person data: {}", fullResponse);
            
            RachioPerson fullPerson = gson.fromJson(fullResponse, RachioPerson.class);
            
            // Debug logging for parsed person data
            if (fullPerson != null) {
                logger.debug("Parsed person: id={}, username={}, fullName={}, devices count={}", 
                            fullPerson.id, fullPerson.username, fullPerson.fullName,
                            fullPerson.devices != null ? fullPerson.devices.size() : 0);
                if (fullPerson.devices != null) {
                    for (RachioPerson.Device device : fullPerson.devices) {
                        logger.debug("Device: id={}, name={}, status={}, zones count={}", 
                                    device.id, device.name, device.status,
                                    device.zones != null ? device.zones.size() : 0);
                    }
                }
            }
            return fullPerson;
        } catch (IOException | InterruptedException | JsonSyntaxException e) {
            throw new RachioException("Failed to get person info: " + e.getMessage(), e);
        }
    }

    /**
     * Get device information - FIXED to use bridge API key
     */
    public @Nullable RachioDevice getDevice(String thingId, String deviceId) throws RachioException {
        // FIX: Use correct endpoint construction
        String url = RACHIO_API_BASE + API_DEVICE + deviceId;
        logger.debug("Getting device from: {}", url);
        
        // FIX: Use any available API key from registered things (bridge)
        String apiKey = getAnyApiKey();
        if (apiKey == null) {
            throw new RachioException("No API key registered for bridge");
        }

        try {
            String response = httpGet(url, apiKey);
            return gson.fromJson(response, RachioDevice.class);
        } catch (IOException | InterruptedException | JsonSyntaxException e) {
            throw new RachioException("Failed to get device " + deviceId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Get zone information - FIXED to use bridge API key
     */
    public @Nullable RachioZone getZone(String zoneId) throws RachioException {
        // FIX: Use any available API key from registered things (bridge)
        String apiKey = getAnyApiKey();
        if (apiKey == null) {
            throw new RachioException("No API keys registered");
        }

        String url = RACHIO_API_BASE + API_ZONE + zoneId;
        logger.debug("Getting zone from: {}", url);

        try {
            String response = httpGet(url, apiKey);
            return gson.fromJson(response, RachioZone.class);
        } catch (IOException | InterruptedException | JsonSyntaxException e) {
            throw new RachioException("Failed to get zone " + zoneId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Start all zones on a device - FIXED to use bridge API key
     */
    public void runAllZones(String thingId, int duration) throws RachioException {
        String apiKey = getAnyApiKey(); // FIX: Use any API key
        if (apiKey == null) {
            throw new RachioException("No API key registered for bridge");
        }

        // This would require device ID, so we need to handle this differently
        // For now, log that this needs implementation
        logger.debug("runAllZones called with duration: {} seconds", duration);
        // TODO: Implement actual API call
    }

    /**
     * Start next zone on a device - FIXED to use bridge API key
     */
    public void runNextZone(String thingId, int duration) throws RachioException {
        String apiKey = getAnyApiKey(); // FIX: Use any API key
        if (apiKey == null) {
            throw new RachioException("No API key registered for bridge");
        }

        logger.debug("runNextZone called with duration: {} seconds", duration);
        // TODO: Implement actual API call
    }

    /**
     * Stop watering on a device - FIXED to use bridge API key
     */
    public void stopWatering(String thingId, String deviceId) throws RachioException {
        String apiKey = getAnyApiKey(); // FIX: Use any API key
        if (apiKey == null) {
            throw new RachioException("No API key registered for bridge");
        }

        String url = RACHIO_API_BASE + API_DEVICE_STOP;
        String payload = "{\"id\":\"" + deviceId + "\"}";
        logger.debug("Stopping watering at URL: {}", url);

        try {
            httpPut(url, apiKey, payload);
            logger.debug("Stopped watering on device: {}", deviceId);
        } catch (IOException | InterruptedException e) {
            throw new RachioException("Failed to stop watering on device " + deviceId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Set rain delay - FIXED to use bridge API key
     */
    public void rainDelay(String thingId, int duration) throws RachioException {
        String apiKey = getAnyApiKey(); // FIX: Use any API key
        if (apiKey == null) {
            throw new RachioException("No API key registered for bridge");
        }

        logger.debug("rainDelay called with duration: {} hours", duration);
        // TODO: Implement actual API call - requires device ID
    }

    /**
     * Start a specific zone - FIXED to use bridge API key
     */
    public void startZone(String thingId, String zoneId, int duration) throws RachioException {
        String apiKey = getAnyApiKey(); // FIX: Use any API key
        if (apiKey == null) {
            throw new RachioException("No API key registered for bridge");
        }

        String url = RACHIO_API_BASE + API_ZONE_START;
        String payload = "{\"id\":\"" + zoneId + "\",\"duration\":" + duration + "}";
        logger.debug("Starting zone at URL: {}", url);

        try {
            httpPut(url, apiKey, payload);
            logger.debug("Started zone: {} for {} seconds", zoneId, duration);
        } catch (IOException | InterruptedException e) {
            throw new RachioException("Failed to start zone " + zoneId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Enable/disable a zone - FIXED to use bridge API key
     */
    public void setZoneEnabled(String thingId, String zoneId, boolean enabled) throws RachioException {
        String apiKey = getAnyApiKey(); // FIX: Use any API key
        if (apiKey == null) {
            throw new RachioException("No API key registered for bridge");
        }

        String url = RACHIO_API_BASE + API_ZONE_ENABLE;
        String payload = "{\"id\":\"" + zoneId + "\",\"enabled\":" + enabled + "}";
        logger.debug("Setting zone enabled at URL: {}", url);

        try {
            httpPut(url, apiKey, payload);
            logger.debug("{} zone: {}", enabled ? "Enabled" : "Disabled", zoneId);
        } catch (IOException | InterruptedException e) {
            throw new RachioException("Failed to set zone enabled state for " + zoneId + ": " + e.getMessage(), e);
        }
    }

    // ===== ENHANCED HTTP METHODS WITH RATE LIMIT TRACKING =====
    
    /**
     * Perform HTTP GET request using modern HttpClient with rate limit tracking
     */
    private String httpGet(String urlString, String apiKey) throws IOException, InterruptedException {
        logger.debug("HTTP GET: {}", urlString);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Parse rate limit headers from response
        parseRateLimitHeaders(response.headers());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        } else {
            logger.error("HTTP GET failed: {} - {}", response.statusCode(), response.body());
            throw new IOException("HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Perform HTTP PUT request using modern HttpClient with rate limit tracking
     */
    private void httpPut(String urlString, String apiKey, String payload) throws IOException, InterruptedException {
        logger.debug("HTTP PUT: {}", urlString);
        logger.debug("PUT Payload: {}", payload);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .PUT(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Parse rate limit headers from response
        parseRateLimitHeaders(response.headers());
        
        // Rachio API returns 204 No Content for many successful operations
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            logger.error("HTTP PUT failed: {} - {}", response.statusCode(), response.body());
            throw new IOException("HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Perform HTTP POST request for webhook registration
     */
    private String httpPost(String urlString, String apiKey, String payload) throws IOException, InterruptedException {
        logger.debug("HTTP POST: {}", urlString);
        logger.debug("POST Payload: {}", payload);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Parse rate limit headers from response
        parseRateLimitHeaders(response.headers());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            logger.debug("HTTP POST successful: {}", response.statusCode());
            return response.body();
        } else {
            logger.error("HTTP POST failed: {} - {}", response.statusCode(), response.body());
            throw new IOException("HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Perform HTTP DELETE request for webhook deletion
     */
    private void httpDelete(String urlString, String apiKey) throws IOException, InterruptedException {
        logger.debug("HTTP DELETE: {}", urlString);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Parse rate limit headers from response
        parseRateLimitHeaders(response.headers());
        
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            logger.error("HTTP DELETE failed: {} - {}", response.statusCode(), response.body());
            throw new IOException("HTTP " + response.statusCode() + " - " + response.body());
        }
    }
    
    /**
     * Parse rate limit headers from HTTP response - FIXED FOR ISO DATE FORMAT
     */
    private void parseRateLimitHeaders(java.net.http.HttpHeaders headers) {
        // Rachio uses X-RateLimit-* headers (based on common REST API patterns)
        
        // 1. Parse X-RateLimit-Limit (number)
        headers.firstValue("X-RateLimit-Limit").ifPresent(limit -> {
            try {
                int newLimit = Integer.parseInt(limit);
                if (newLimit > 0) {
                    rateLimitLimit = newLimit;
                    logger.trace("Rate limit: {} calls per hour", newLimit);
                }
            } catch (NumberFormatException e) {
                logger.debug("Invalid X-RateLimit-Limit header: {}", limit);
            }
        });
        
        // 2. Parse X-RateLimit-Remaining (number)
        headers.firstValue("X-RateLimit-Remaining").ifPresent(remaining -> {
            try {
                int newRemaining = Integer.parseInt(remaining);
                if (newRemaining >= 0) {
                    rateLimitRemaining = newRemaining;
                    logger.trace("Rate limit remaining: {} calls", newRemaining);
                }
            } catch (NumberFormatException e) {
                logger.debug("Invalid X-RateLimit-Remaining header: {}", remaining);
            }
        });
        
        // 3. Parse X-RateLimit-Reset (ISO DATE STRING) - FIXED!
        headers.firstValue("X-RateLimit-Reset").ifPresent(reset -> {
            try {
                // Rachio sends ISO date string like "2025-12-03T00:00:00Z"
                // Parse it to Instant, then convert to Unix timestamp
                Instant resetInstant = Instant.parse(reset);
                long resetTime = resetInstant.getEpochSecond();
                
                if (resetTime > 0) {
                    rateLimitResetTime = resetTime;
                    
                    // Calculate seconds until reset
                    long now = System.currentTimeMillis() / 1000;
                    long secondsUntilReset = Math.max(0, resetTime - now);
                    
                    logger.trace("Rate limit reset: {} ({} seconds from now)", reset, secondsUntilReset);
                }
            } catch (DateTimeParseException e) {
                // Try alternative format: might be Unix timestamp as string
                try {
                    long resetTime = Long.parseLong(reset);
                    if (resetTime > 0) {
                        rateLimitResetTime = resetTime;
                        logger.trace("Rate limit reset (Unix): {}", resetTime);
                    }
                } catch (NumberFormatException e2) {
                    logger.debug("Invalid X-RateLimit-Reset header (not ISO or Unix): {}", reset);
                }
            } catch (Exception e) {
                logger.debug("Error parsing X-RateLimit-Reset header: {}", e.getMessage());
            }
        });
        
        // 4. Log state transitions and warnings
        logRateLimitStateTransitions();
        
        // 5. Debug logging for rate limit state
        long secondsUntilReset = getRateLimitSecondsUntilReset();
        logger.debug("Rate limit: {}/{} calls ({}%), reset in {}s", 
                    rateLimitRemaining, rateLimitLimit, getRateLimitPercent(), secondsUntilReset);
    }
    
    /**
     * Log warnings when rate limits approach thresholds
     */
    private void logRateLimitStateTransitions() {
        boolean isWarning = isRateLimitWarning();
        boolean isCritical = isRateLimitCritical();
        
        // Log when entering warning state
        if (isWarning && !rateLimitWasWarning) {
            logger.warn("RACHIO RATE LIMIT WARNING: {} calls remaining ({}%). Limit resets in {} seconds.",
                       rateLimitRemaining, getRateLimitPercent(),
                       getRateLimitSecondsUntilReset());
            rateLimitWasWarning = true;
        }
        
        // Log when entering critical state
        if (isCritical && !rateLimitWasCritical) {
            logger.error("RACHIO RATE LIMIT CRITICAL: {} calls remaining ({}%). Limit resets in {} seconds.",
                        rateLimitRemaining, getRateLimitPercent(),
                        getRateLimitSecondsUntilReset());
            rateLimitWasCritical = true;
        }
        
        // Log when recovering from critical to warning
        if (rateLimitWasCritical && !isCritical && isWarning) {
            logger.warn("RACHIO RATE LIMIT RECOVERED from critical to warning: {} calls remaining ({}%).",
                       rateLimitRemaining, getRateLimitPercent());
            rateLimitWasCritical = false;
        }
        
        // Log when recovering from warning to normal
        if (rateLimitWasWarning && !isWarning && !isCritical) {
            logger.info("RACHIO RATE LIMIT RECOVERED to normal: {} calls remaining ({}%).",
                       rateLimitRemaining, getRateLimitPercent());
            rateLimitWasWarning = false;
            rateLimitWasCritical = false;
        }
    }
    
    /**
     * Get current rate limit information as array: [limit, remaining, resetTime]
     */
    public int[] getRateLimitInfo() {
        return new int[]{rateLimitLimit, rateLimitRemaining, (int) rateLimitResetTime};
    }
    
    /**
     * Get adaptive polling multiplier based on rate limit status
     * Returns 1.0 for normal, 2.0 for warning, 3.0 for critical
     */
    public double getAdaptivePollingMultiplier() {
        if (isRateLimitCritical()) {
            return 3.0; // 3x slower when critical
        } else if (isRateLimitWarning()) {
            return 2.0; // 2x slower when warning
        }
        return 1.0; // Normal speed
    }
    
    /**
     * Get rate limit percentage remaining
     */
    public int getRateLimitPercent() {
        if (rateLimitLimit <= 0) return 100;
        return (int) ((rateLimitRemaining / (double) rateLimitLimit) * 100);
    }
    
    /**
     * Get formatted rate limit status for display
     */
    public String getRateLimitStatusText() {
        int percent = getRateLimitPercent();
        if (isRateLimitCritical()) {
            return String.format("Critical: %d calls remaining (%d%%)", rateLimitRemaining, percent);
        } else if (isRateLimitWarning()) {
            return String.format("Warning: %d calls remaining (%d%%)", rateLimitRemaining, percent);
        } else {
            return String.format("Normal: %d/%d calls (%d%%)", rateLimitRemaining, rateLimitLimit, percent);
        }
    }
    
    /**
     * Check if rate limit is in warning state (10% or less remaining)
     */
    public boolean isRateLimitWarning() {
        return getRateLimitPercent() <= 10;
    }
    
    /**
     * Check if rate limit is in critical state (2% or less remaining)
     */
    public boolean isRateLimitCritical() {
        return getRateLimitPercent() <= 2;
    }
    
    /**
     * Check if rate limit is healthy enough for normal operation
     */
    public boolean isRateLimitHealthy() {
        return !isRateLimitCritical();
    }
    
    /**
     * Get seconds until rate limit resets
     */
    public long getRateLimitSecondsUntilReset() {
        if (rateLimitResetTime == 0) return 0;
        long now = System.currentTimeMillis() / 1000;
        long secondsUntilReset = Math.max(0, rateLimitResetTime - now);
        
        // Log if we have a valid reset time
        if (secondsUntilReset > 0 && secondsUntilReset < 3600) {
            logger.trace("Rate limit resets in {} seconds", secondsUntilReset);
        }
        
        return secondsUntilReset;
    }
    
    /**
     * Get the current rate limit remaining calls
     */
    public int getRateLimitRemaining() {
        return rateLimitRemaining;
    }
    
    /**
     * Get the current rate limit total
     */
    public int getRateLimitTotal() {
        return rateLimitLimit;
    }
    
    /**
     * Get the rate limit reset timestamp
     */
    public long getRateLimitResetTime() {
        return rateLimitResetTime;
    }
}