package org.openhab.binding.rachio.internal.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.dto.*;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.io.net.http.HttpRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@link RachioHttp} class handles HTTP communication with the Rachio API
 * using the modern HttpClient with HTTP/2 support.
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioHttp {

    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);
    private final String apiKey;
    private final org.openhab.core.io.net.http.HttpClient httpClient;
    private final Gson gson;
    
    private final Map<String, String> rateLimits = new ConcurrentHashMap<>();
    private final Map<String, Object> deviceCache = new ConcurrentHashMap<>();
    private long lastCacheInvalidation = System.currentTimeMillis();
    private static final long CACHE_TTL = 30000; // 30 seconds

    // Gson type adapters for custom parsing
    private final InstantTypeAdapter instantTypeAdapter = new InstantTypeAdapter();

    public RachioHttp(String apiKey, HttpClientFactory httpClientFactory) {
        this.apiKey = apiKey;
        this.httpClient = httpClientFactory.getCommonHttpClient();
        
        // Configure Gson with custom type adapters
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Instant.class, instantTypeAdapter);
        gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        this.gson = gsonBuilder.create();
        
        logger.debug("RachioHttp initialized with API key: {}", apiKey.substring(0, Math.min(apiKey.length(), 8)) + "...");
    }

    // ===== Device Methods =====
    
    public @Nullable RachioPerson getPerson() throws RachioApiException {
        String url = RachioBindingConstants.RACHIO_API_BASE_URL + "/1/public/person/info";
        return executeGetRequest(url, RachioPerson.class);
    }
    
    public @Nullable List<RachioDevice> getDevices() throws RachioApiException {
        RachioPerson person = getPerson();
        if (person != null && person.devices != null) {
            // Cache devices
            for (RachioDevice device : person.devices) {
                deviceCache.put(device.id, device);
            }
            return person.devices;
        }
        return Collections.emptyList();
    }
    
    public @Nullable RachioDevice getDevice(String deviceId) throws RachioApiException {
        // Check cache first
        if (System.currentTimeMillis() - lastCacheInvalidation > CACHE_TTL) {
            deviceCache.clear();
            lastCacheInvalidation = System.currentTimeMillis();
        }
        
        Object cached = deviceCache.get(deviceId);
        if (cached instanceof RachioDevice) {
            logger.debug("Returning cached device: {}", deviceId);
            return (RachioDevice) cached;
        }
        
        // Fetch from API
        String url = RachioBindingConstants.RACHIO_API_BASE_URL + "/1/public/device/" + deviceId;
        RachioDevice device = executeGetRequest(url, RachioDevice.class);
        if (device != null) {
            deviceCache.put(deviceId, device);
        }
        return device;
    }
    
    public boolean updateDevice(String deviceId, Map<String, Object> updates) throws RachioApiException {
        String url = RachioBindingConstants.RACHIO_API_BASE_URL + "/1/public/device/" + deviceId;
        String json = gson.toJson(updates);
        return executePutRequest(url, json);
    }
    
    // ===== Zone Methods =====
    
    public @Nullable RachioZone getZone(String deviceId, String zoneId) throws RachioApiException {
        // First get the device to find the zone
        RachioDevice device = getDevice(deviceId);
        if (device != null && device.zones != null) {
            for (RachioZone zone : device.zones) {
                if (zoneId.equals(zone.id)) {
                    return zone;
                }
            }
        }
        return null;
    }
    
    public @Nullable List<RachioZone> getZones(String deviceId) throws RachioApiException {
        RachioDevice device = getDevice(deviceId);
        if (device != null) {
            return device.zones;
        }
        return Collections.emptyList();
    }
    
    // ===== Zone Control Methods =====
    
    public void startZone(String deviceId, String zoneId, int durationSeconds) throws RachioApiException {
        String url = RachioBindingConstants.RACHIO_API_BASE_URL + "/1/public/zone/start";
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", zoneId);
        payload.put("duration", durationSeconds);
        
        String json = gson.toJson(payload);
        executePutRequest(url, json);
        logger.debug("Started zone {} for {} seconds", zoneId, durationSeconds);
    }
    
    public void stopZone(String deviceId, String zoneId) throws RachioApiException {
        String url = RachioBindingConstants.RACHIO_API_BASE_URL + "/1/public/zone/stop";
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", zoneId);
        
        String json = gson.toJson(payload);
        executePutRequest(url, json);
        logger.debug("Stopped zone {}", zoneId);
    }
    
    public void setZoneEnabled(String deviceId, String zoneId, boolean enabled) throws RachioApiException {
        String url = RachioBindingConstants.RACHIO_API_BASE_URL + "/1/public/zone/" + (enabled ? "enable" : "disable");
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", zoneId);
        
        String json = gson.toJson(payload);
        executePutRequest(url, json);
        logger.debug("Set zone {} enabled to {}", zoneId, enabled);
    }
    
    public void runMultipleZones(String deviceId, List<Map<String, Object>> zoneRuns) throws RachioApiException {
        String url = RachioBindingConstants.RACHIO_API_BASE_URL + "/1/public/zone/start_multiple";
        String json = gson.toJson(zoneRuns);
        executePutRequest(url, json);
        logger.debug("Started multiple zones");
    }
    
    // ===== Device Control Methods =====
    
    public void startRainDelay(String deviceId, int durationMinutes) throws RachioApiException {
        String url = RachioBindingConstants.RACHIO_API_BASE_URL + "/1/public/device/rain_delay";
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", deviceId);
        payload.put("duration", durationMinutes);
        
        String json = gson.toJson(payload);
        executePutRequest(url, json);
        logger.debug("Started rain delay for device {} for {} minutes", deviceId, durationMinutes);
    }
    
    public void stopRainDelay(String deviceId) throws RachioApiException {
        String url = RachioBindingConstants.RACHIO_API_BASE_URL + "/1/public/device/rain_delay";
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", deviceId);
        payload.put("duration", 0);
        
        String json = gson.toJson(payload);
        executePutRequest(url, json);
        logger.debug("Stopped rain delay for device {}", deviceId);
    }
    
    public void pauseDevice(String deviceId, boolean pause) throws RachioApiException {
        String url = RachioBindingConstants.RACHIO_API_BASE_URL + "/1/public/device/" + (pause ? "pause" : "resume");
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", deviceId);
        
        String json = gson.toJson(payload);
        executePutRequest(url, json);
        logger.debug("Set device {} pause to {}", deviceId, pause);
    }
    
    // ===== Advanced Irrigation Features =====
    
    public @Nullable List<Map<String, Object>> getWateringHistory(String zoneId, int limit) throws RachioApiException {
        String url = RachioBindingConstants.RACHIO_API_BASE_URL + "/1/public/zone/" + zoneId + "/watering?limit=" + limit;
        return executeGetRequest(url, List.class);
    }
    
    public @Nullable RachioForecast getForecast(String deviceId) throws RachioApiException {
        String url = RachioBindingConstants.RACHIO_API_BASE_URL + "/1/public/device/" + deviceId + "/forecast";
        return executeGetRequest(url, RachioForecast.class);
    }
    
    public @Nullable RachioUsage getUsage(String deviceId) throws RachioApiException {
        String url = RachioBindingConstants.RACHIO_API_BASE_URL + "/1/public/device/" + deviceId + "/usage";
        return executeGetRequest(url, RachioUsage.class);
    }
    
    public @Nullable RachioSchedule getSchedule(String deviceId) throws RachioApiException {
        String url = RachioBindingConstants.RACHIO_API_BASE_URL + "/1/public/device/" + deviceId + "/schedule";
        return executeGetRequest(url, RachioSchedule.class);
    }
    
    public @Nullable List<RachioAlert> getAlerts(String deviceId) throws RachioApiException {
        String url = RachioBindingConstants.RACHIO_API_BASE_URL + "/1/public/device/" + deviceId + "/alerts";
        return executeGetRequest(url, List.class);
    }
    
    // ===== Webhook Methods =====
    
    public @Nullable List<RachioApiWebHookEntry> getWebhooks() throws RachioApiException {
        String url = RachioBindingConstants.RACHIO_API_BASE_URL + "/1/public/webhook";
        RachioApiWebHookList response = executeGetRequest(url, RachioApiWebHookList.class);
        return response != null ? response.data : Collections.emptyList();
    }
    
    public boolean registerWebhook(String url, String externalId) throws RachioApiException {
        String apiUrl = RachioBindingConstants.RACHIO_API_BASE_URL + "/1/public/webhook";
        Map<String, Object> payload = new HashMap<>();
        payload.put("url", url);
        payload.put("externalId", externalId);
        
        String json = gson.toJson(payload);
        return executePostRequest(apiUrl, json);
    }
    
    public boolean deleteWebhook(String webhookId) throws RachioApiException {
        String url = RachioBindingConstants.RACHIO_API_BASE_URL + "/1/public/webhook/" + webhookId;
        return executeDeleteRequest(url);
    }
    
    public boolean deleteAllWebhooks() throws RachioApiException {
        List<RachioApiWebHookEntry> webhooks = getWebhooks();
        boolean allDeleted = true;
        for (RachioApiWebHookEntry webhook : webhooks) {
            if (!deleteWebhook(webhook.id)) {
                allDeleted = false;
            }
        }
        return allDeleted;
    }
    
    // ===== Core HTTP Methods =====
    
    private <T> @Nullable T executeGetRequest(String url, Class<T> responseType) throws RachioApiException {
        try {
            HttpRequestBuilder builder = httpClient.newRequest(url)
                .withHeader("Authorization", "Bearer " + apiKey)
                .withHeader("Accept", "application/json")
                .withHeader("User-Agent", "OpenHAB-Rachio-Binding/5.0.1");
            
            logger.debug("GET request to: {}", url);
            
            String response = builder.getContentAsString();
            // Note: Removed getResponseHeaders() call as it doesn't exist in OpenHAB 5.x
            
            if (response != null && !response.isEmpty()) {
                return gson.fromJson(response, responseType);
            }
            
        } catch (IOException e) {
            throw new RachioApiException("GET request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RachioApiException("Unexpected error: " + e.getMessage(), e);
        }
        
        return null;
    }
    
    private boolean executePutRequest(String url, String jsonBody) throws RachioApiException {
        try {
            HttpRequestBuilder builder = httpClient.newRequest(url)
                .withHeader("Authorization", "Bearer " + apiKey)
                .withHeader("Content-Type", "application/json")
                .withHeader("Accept", "application/json")
                .withHeader("User-Agent", "OpenHAB-Rachio-Binding/5.0.1");
            
            logger.debug("PUT request to: {} with body: {}", url, jsonBody);
            
            builder.withContent(jsonBody, "application/json");
            String response = builder.getContentAsString();
            // Note: Removed getResponseHeaders() call
            
            // PUT requests typically return 200/204 on success
            return true;
            
        } catch (IOException e) {
            throw new RachioApiException("PUT request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RachioApiException("Unexpected error: " + e.getMessage(), e);
        }
    }
    
    private boolean executePostRequest(String url, String jsonBody) throws RachioApiException {
        try {
            HttpRequestBuilder builder = httpClient.newRequest(url)
                .withHeader("Authorization", "Bearer " + apiKey)
                .withHeader("Content-Type", "application/json")
                .withHeader("Accept", "application/json")
                .withHeader("User-Agent", "OpenHAB-Rachio-Binding/5.0.1");
            
            logger.debug("POST request to: {} with body: {}", url, jsonBody);
            
            builder.withContent(jsonBody, "application/json");
            String response = builder.getContentAsString();
            // Note: Removed getResponseHeaders() call
            
            // POST requests typically return 201 on success
            return true;
            
        } catch (IOException e) {
            throw new RachioApiException("POST request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RachioApiException("Unexpected error: " + e.getMessage(), e);
        }
    }
    
    private boolean executeDeleteRequest(String url) throws RachioApiException {
        try {
            // For DELETE in OpenHAB 5.x, we use withMethod
            HttpRequestBuilder builder = httpClient.newRequest(url)
                .withHeader("Authorization", "Bearer " + apiKey)
                .withHeader("Accept", "application/json")
                .withHeader("User-Agent", "OpenHAB-Rachio-Binding/5.0.1");
            
            logger.debug("DELETE request to: {}", url);
            
            // Use withMethod for DELETE
            builder.withMethod("DELETE");
            String response = builder.getContentAsString();
            
            return true;
            
        } catch (IOException e) {
            throw new RachioApiException("DELETE request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RachioApiException("Unexpected error: " + e.getMessage(), e);
        }
    }
    
    // Note: Removed updateRateLimits() method since getResponseHeaders() doesn't exist
    
    public Map<String, String> getRateLimits() {
        return new HashMap<>(rateLimits);
    }
    
    public void clearCache() {
        deviceCache.clear();
        lastCacheInvalidation = System.currentTimeMillis();
        logger.debug("Cache cleared");
    }
    
    public int getRateLimitRemaining() {
        String remaining = rateLimits.get(RachioBindingConstants.HEADER_RATE_LIMIT_REMAINING);
        if (remaining != null) {
            try {
                return Integer.parseInt(remaining);
            } catch (NumberFormatException e) {
                logger.warn("Invalid rate limit remaining value: {}", remaining);
            }
        }
        return 100; // Default
    }
    
    public boolean isRateLimitCritical() {
        return getRateLimitRemaining() <= RachioBindingConstants.RATE_LIMIT_CRITICAL_THRESHOLD;
    }
    
    // Method to manually set rate limits (for testing or from response parsing)
    public void setRateLimit(String header, String value) {
        rateLimits.put(header, value);
    }
}
