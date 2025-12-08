package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioForecast;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioUsage;
import org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * HTTP client for Rachio API with professional features and rate limiting
 *
 * @author Damion Boyett - Enhanced with modern HttpClient and professional features
 */
@NonNullByDefault
public class RachioHttp implements RachioActions {
    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);
    
    private static final String API_BASE_URL = "https://api.rach.io/1/public";
    private static final int REQUEST_TIMEOUT_SECONDS = 30;
    private static final int MAX_RETRIES = 3;
    private static final long RATE_LIMIT_RESET_BUFFER_MS = 1000;
    
    private final HttpClient httpClient;
    private final Gson gson;
    private final String apiKey;
    private final RachioBridgeHandler bridgeHandler;
    
    // Rate limiting tracking
    private int rateLimitRemaining = 100;
    private int rateLimitLimit = 100;
    private Instant rateLimitReset = Instant.now().plusSeconds(3600);
    private Instant lastRequestTime = Instant.now();
    private boolean rateLimitExceeded = false;
    
    // Cache management
    private @Nullable RachioPerson cachedPerson;
    private @Nullable List<RachioDevice> cachedDevices;
    private Instant cacheTimestamp = Instant.MIN;
    private static final long CACHE_DURATION_SECONDS = 300; // 5 minutes
    
    public RachioHttp(String apiKey, RachioBridgeHandler bridgeHandler) {
        this.apiKey = apiKey;
        this.bridgeHandler = bridgeHandler;
        
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .version(HttpClient.Version.HTTP_2)
                .build();
        
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
        
        logger.debug("RachioHttp initialized for bridge: {}", bridgeHandler.getThing().getUID());
    }
    
    @Override
    public RachioPerson getPersonInfo() throws RachioApiException {
        // Check cache first
        if (cachedPerson != null && 
            Instant.now().minusSeconds(CACHE_DURATION_SECONDS).isBefore(cacheTimestamp)) {
            logger.debug("Returning cached person info");
            return cachedPerson;
        }
        
        String response = executeGet("/person/info");
        RachioPerson person = gson.fromJson(response, RachioPerson.class);
        
        // Update cache
        cachedPerson = person;
        cacheTimestamp = Instant.now();
        
        return person;
    }
    
    @Override
    public List<RachioDevice> getDeviceList() throws RachioApiException {
        // Check cache first
        if (cachedDevices != null && 
            Instant.now().minusSeconds(CACHE_DURATION_SECONDS).isBefore(cacheTimestamp)) {
            logger.debug("Returning cached device list");
            return cachedDevices;
        }
        
        String response = executeGet("/person/device");
        Type deviceListType = new TypeToken<List<RachioDevice>>() {}.getType();
        List<RachioDevice> devices = gson.fromJson(response, deviceListType);
        
        // Update cache
        cachedDevices = devices;
        cacheTimestamp = Instant.now();
        
        // Also update person cache if it exists
        if (cachedPerson != null) {
            cachedPerson.setDevices(devices);
        }
        
        return devices;
    }
    
    @Override
    public RachioDevice getDevice(String deviceId) throws RachioApiException {
        // Try to get from cache first
        if (cachedDevices != null) {
            for (RachioDevice device : cachedDevices) {
                if (deviceId.equals(device.getId())) {
                    return device;
                }
            }
        }
        
        // Not in cache, fetch from API
        String response = executeGet("/device/" + deviceId);
        return gson.fromJson(response, RachioDevice.class);
    }
    
    @Override
    public void startZone(String zoneId, String deviceId, int duration, String source) throws RachioApiException {
        String body = String.format("{\"id\":\"%s\",\"duration\":%d}", zoneId, duration);
        executePut("/zone/start", body);
        logger.info("Started zone {} on device {} for {} seconds (source: {})", 
                   zoneId, deviceId, duration, source);
    }
    
    @Override
    public void stopZone(String zoneId, String deviceId, String source) throws RachioApiException {
        String body = String.format("{\"id\":\"%s\"}", zoneId);
        executePut("/zone/stop", body);
        logger.info("Stopped zone {} on device {} (source: {})", zoneId, deviceId, source);
    }
    
    @Override
    public void runAllZones(String deviceId, int duration, String source) throws RachioApiException {
        String body = String.format("{\"duration\":%d}", duration);
        executePut("/device/" + deviceId + "/start", body);
        logger.info("Started all zones on device {} for {} seconds (source: {})", 
                   deviceId, duration, source);
    }
    
    @Override
    public void runNextZone(String deviceId, String source) throws RachioApiException {
        executePut("/device/" + deviceId + "/start_zone", "");
        logger.info("Started next zone on device {} (source: {})", deviceId, source);
    }
    
    @Override
    public void stopWatering(String deviceId, String source) throws RachioApiException {
        executePut("/device/" + deviceId + "/stop_water", "");
        logger.info("Stopped all watering on device {} (source: {})", deviceId, source);
    }
    
    @Override
    public void setRainDelay(String deviceId, int duration) throws RachioApiException {
        String body = String.format("{\"duration\":%d}", duration);
        executePut("/device/" + deviceId + "/rain_delay", body);
        logger.info("Set rain delay on device {} for {} seconds", deviceId, duration);
    }
    
    @Override
    public void setZoneEnabled(String zoneId, String deviceId, boolean enabled, String source) throws RachioApiException {
        String enabledStr = enabled ? "true" : "false";
        String body = String.format("{\"enabled\":%s}", enabledStr);
        executePut("/zone/" + zoneId, body);
        logger.info("{} zone {} on device {} (source: {})", 
                   enabled ? "Enabled" : "Disabled", zoneId, deviceId, source);
    }
    
    @Override
    public RachioForecast getDeviceForecast(String deviceId) throws RachioApiException {
        String response = executeGet("/device/" + deviceId + "/forecast");
        return gson.fromJson(response, RachioForecast.class);
    }
    
    @Override
    public RachioUsage getDeviceUsage(String deviceId) throws RachioApiException {
        String response = executeGet("/device/" + deviceId + "/usage");
        return gson.fromJson(response, RachioUsage.class);
    }
    
    @Override
    public void updateRateLimitStatus() {
        // This method is called after each request to update rate limit tracking
        // The actual rate limit headers are parsed in executeRequest()
        logger.debug("Rate limit status: {}/{} remaining, resets at {}", 
                    rateLimitRemaining, rateLimitLimit, rateLimitReset);
    }
    
    @Override
    public String getRateLimitInfo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());
        
        return String.format("Remaining: %d/%d, Reset: %s, Exceeded: %s", 
                rateLimitRemaining, rateLimitLimit, 
                formatter.format(rateLimitReset),
                rateLimitExceeded ? "Yes" : "No");
    }
    
    private String executeGet(String endpoint) throws RachioApiException {
        return executeRequest("GET", endpoint, null);
    }
    
    private String executePut(String endpoint, String body) throws RachioApiException {
        return executeRequest("PUT", endpoint, body);
    }
    
    private String executeRequest(String method, String endpoint, @Nullable String body) throws RachioApiException {
        // Check rate limits before making request
        checkRateLimits();
        
        String url = API_BASE_URL + endpoint;
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS));
        
        if ("PUT".equals(method) && body != null) {
            requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body));
        } else {
            requestBuilder.GET();
        }
        
        HttpRequest request = requestBuilder.build();
        
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                logger.debug("Executing {} request to: {}", method, endpoint);
                HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
                lastRequestTime = Instant.now();
                
                // Parse rate limit headers
                parseRateLimitHeaders(response);
                
                int statusCode = response.statusCode();
                String responseBody = response.body();
                
                if (statusCode >= 200 && statusCode < 300) {
                    logger.trace("API response ({}): {}", statusCode, responseBody);
                    return responseBody;
                } else {
                    handleErrorResponse(statusCode, responseBody, endpoint);
                }
                
            } catch (IOException e) {
                attempt++;
                logger.debug("IO error on attempt {}/{} for {}: {}", 
                           attempt, MAX_RETRIES, endpoint, e.getMessage());
                if (attempt >= MAX_RETRIES) {
                    throw new RachioApiException("IO error after " + MAX_RETRIES + " attempts: " + e.getMessage(), e);
                }
                sleepBeforeRetry(attempt);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RachioApiException("Request interrupted: " + e.getMessage(), e);
            }
        }
        
        throw new RachioApiException("Failed after " + MAX_RETRIES + " attempts");
    }
    
    private void checkRateLimits() throws RachioApiException {
        Instant now = Instant.now();
        
        // If rate limit exceeded, check if reset time has passed
        if (rateLimitExceeded) {
            if (now.isAfter(rateLimitReset)) {
                rateLimitExceeded = false;
                rateLimitRemaining = rateLimitLimit;
                logger.info("Rate limit reset, resuming normal operation");
            } else {
                long secondsRemaining = rateLimitReset.getEpochSecond() - now.getEpochSecond();
                throw new RachioApiException(
                    String.format("Rate limit exceeded. Resets in %d seconds", secondsRemaining));
            }
        }
        
        // If we're running low on requests, log warning
        if (rateLimitRemaining <= 10) {
            logger.warn("Low rate limit remaining: {}/{}", rateLimitRemaining, rateLimitLimit);
        }
        
        // Enforce minimum time between requests when rate limit is low
        if (rateLimitRemaining <= 5) {
            long timeSinceLastRequest = Duration.between(lastRequestTime, now).toMillis();
            long minInterval = 2000; // 2 seconds between requests when limit is very low
            
            if (timeSinceLastRequest < minInterval) {
                try {
                    long sleepTime = minInterval - timeSinceLastRequest;
                    logger.debug("Rate limit low, sleeping for {} ms", sleepTime);
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    private void parseRateLimitHeaders(HttpResponse<String> response) {
        // Parse X-RateLimit-* headers
        response.headers().firstValue("X-RateLimit-Remaining").ifPresent(value -> {
            try {
                rateLimitRemaining = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.debug("Invalid X-RateLimit-Remaining header: {}", value);
            }
        });
        
        response.headers().firstValue("X-RateLimit-Limit").ifPresent(value -> {
            try {
                rateLimitLimit = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.debug("Invalid X-RateLimit-Limit header: {}", value);
            }
        });
        
        response.headers().firstValue("X-RateLimit-Reset").ifPresent(value -> {
            try {
                long resetSeconds = Long.parseLong(value);
                rateLimitReset = Instant.ofEpochSecond(resetSeconds).plusMillis(RATE_LIMIT_RESET_BUFFER_MS);
            } catch (NumberFormatException e) {
                logger.debug("Invalid X-RateLimit-Reset header: {}", value);
            }
        });
        
        // Check if we hit rate limit
        if (response.statusCode() == 429) {
            rateLimitExceeded = true;
            logger.warn("Rate limit exceeded! Reset at {}", rateLimitReset);
        }
        
        // Update bridge handler status
        updateBridgeStatus();
    }
    
    private void updateBridgeStatus() {
        // This method now properly calls the protected updateStatus method through the bridge handler
        if (rateLimitExceeded) {
            bridgeHandler.updateBridgeStatus(ThingStatus.ONLINE, 
                ThingStatusDetail.CONFIGURATION_ERROR, 
                "Rate limit exceeded");
        } else if (rateLimitRemaining <= 5) {
            bridgeHandler.updateBridgeStatus(ThingStatus.ONLINE, 
                ThingStatusDetail.NONE, 
                "Rate limit low: " + rateLimitRemaining + "/" + rateLimitLimit);
        } else {
            bridgeHandler.updateBridgeStatus(ThingStatus.ONLINE, 
                ThingStatusDetail.NONE, 
                "API connected");
        }
    }
    
    private void handleErrorResponse(int statusCode, String responseBody, String endpoint) throws RachioApiException {
        logger.debug("API error {} for {}: {}", statusCode, endpoint, responseBody);
        
        switch (statusCode) {
            case 401:
                throw new RachioApiException("Unauthorized - Invalid API key");
            case 403:
                throw new RachioApiException("Forbidden - Check API key permissions");
            case 404:
                throw new RachioApiException("Resource not found: " + endpoint);
            case 429:
                throw new RachioApiException("Rate limit exceeded");
            case 500:
            case 502:
            case 503:
            case 504:
                throw new RachioApiException("Server error (" + statusCode + ") - Please try again later");
            default:
                throw new RachioApiException("API error " + statusCode + ": " + responseBody);
        }
    }
    
    private void sleepBeforeRetry(int attempt) {
        try {
            // Exponential backoff: 1s, 2s, 4s
            long sleepTime = (long) Math.pow(2, attempt - 1) * 1000;
            logger.debug("Sleeping for {} ms before retry {}", sleepTime, attempt);
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void invalidateCache() {
        cachedPerson = null;
        cachedDevices = null;
        cacheTimestamp = Instant.MIN;
        logger.debug("Cache invalidated");
    }
    
    public void invalidateDeviceCache(String deviceId) {
        if (cachedDevices != null) {
            cachedDevices.removeIf(device -> deviceId.equals(device.getId()));
        }
        logger.debug("Cache invalidated for device: {}", deviceId);
    }
    
    public boolean isRateLimitExceeded() {
        return rateLimitExceeded;
    }
    
    public int getRateLimitRemaining() {
        return rateLimitRemaining;
    }
    
    public int getRateLimitLimit() {
        return rateLimitLimit;
    }
    
    public Instant getRateLimitReset() {
        return rateLimitReset;
    }
    
    public long getRateLimitResetSeconds() {
        Instant now = Instant.now();
        if (rateLimitReset.isAfter(now)) {
            return rateLimitReset.getEpochSecond() - now.getEpochSecond();
        }
        return 0;
    }
    
    /**
     * Process webhook event from Rachio
     * 
     * @param event the webhook event
     */
    public void processWebhookEvent(RachioWebhookEvent event) {
        if (event == null) {
            logger.debug("Received null webhook event");
            return;
        }
        
        String deviceId = event.getDeviceId(); // Fixed: Use getter method
        String eventType = event.getType(); // Fixed: Use getter method
        
        if (deviceId == null || deviceId.isEmpty()) {
            logger.debug("Webhook event missing device ID");
            return;
        }
        
        logger.info("Processing webhook event: {} for device: {}", eventType, deviceId);
        
        // Invalidate cache for this device
        invalidateDeviceCache(deviceId);
        
        // Notify bridge handler
        bridgeHandler.handleWebhookEvent(event);
        
        // Update device status if needed
        updateDeviceStatusFromEvent(event);
    }
    
    private void updateDeviceStatusFromEvent(RachioWebhookEvent event) {
        String deviceId = event.getDeviceId(); // Fixed: Use getter method
        if (deviceId == null) {
            return;
        }
        
        try {
            // Refresh device data
            RachioDevice device = getDevice(deviceId);
            
            // Update any zone statuses
            List<RachioZone> zones = device.getZones(); // Fixed: Use getter method
            if (zones != null) {
                for (RachioZone zone : zones) {
                    // Check if this zone is mentioned in the event
                    // This would need to be implemented based on event details
                    if (event.getType() != null && event.getType().contains("ZONE")) {
                        // Update zone status
                        // Implementation depends on specific event structure
                    }
                }
            }
            
        } catch (RachioApiException e) {
            logger.debug("Error updating device status from webhook: {}", e.getMessage());
        }
    }
    
    /**
     * Get device ID from webhook event
     * 
     * @param event the webhook event
     * @return device ID or null
     */
    @Nullable
    public String getDeviceIdFromEvent(RachioWebhookEvent event) {
        return event != null ? event.getDeviceId() : null; // Fixed: Use getter method
    }
    
    @Override
    public void clearRateLimitExceeded() {
        if (rateLimitExceeded && Instant.now().isAfter(rateLimitReset)) {
            rateLimitExceeded = false;
            rateLimitRemaining = rateLimitLimit;
            logger.info("Cleared rate limit exceeded flag");
        }
    }
    
    @Override
    public boolean shouldThrottle() {
        return rateLimitRemaining <= 10;
    }
    
    @Override
    public long getThrottleDelayMs() {
        if (rateLimitRemaining <= 5) {
            return 2000; // 2 seconds
        } else if (rateLimitRemaining <= 10) {
            return 1000; // 1 second
        } else if (rateLimitRemaining <= 20) {
            return 500; // 0.5 seconds
        }
        return 0;
    }
}
