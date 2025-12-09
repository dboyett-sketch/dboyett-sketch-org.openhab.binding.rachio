package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * HTTP client for Rachio API communication
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioHttp {
    private static final String API_BASE_URL = "https://api.rach.io/1/public";
    private static final int REQUEST_TIMEOUT_SECONDS = 30;
    private static final int MAX_RETRIES = 3;

    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);
    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson;

    private int rateLimitRemaining = 100;
    private int rateLimitReset = 3600;
    private long lastRequestTime = 0;

    public RachioHttp(String apiKey, HttpClient httpClient, Gson gson) {
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.gson = gson;
    }

    /**
     * Constructor with HttpClientFactory for compatibility
     */
    public RachioHttp(String apiKey, HttpClientFactory httpClientFactory, Gson gson) {
        this.apiKey = apiKey;
        this.httpClient = httpClientFactory.getCommonHttpClient();
        this.gson = gson;
    }

    /**
     * Execute a GET request
     */
    public @Nullable String executeGet(String endpoint) throws IOException {
        return executeRequest("GET", endpoint, null);
    }

    /**
     * Execute a POST request
     */
    public @Nullable String executePost(String endpoint, @Nullable String body) throws IOException {
        return executeRequest("POST", endpoint, body);
    }

    /**
     * Execute a PUT request
     */
    public @Nullable String executePut(String endpoint, @Nullable String body) throws IOException {
        return executeRequest("PUT", endpoint, body);
    }

    /**
     * Execute a DELETE request
     */
    public @Nullable String executeDelete(String endpoint) throws IOException {
        return executeRequest("DELETE", endpoint, null);
    }

    /**
     * Execute an HTTP request with retry logic
     */
    private @Nullable String executeRequest(String method, String endpoint, @Nullable String body) throws IOException {
        // Check rate limits
        if (rateLimitRemaining <= 0) {
            long waitTime = calculateWaitTime();
            if (waitTime > 0) {
                logger.warn("Rate limit exceeded, waiting {} seconds before retry", waitTime);
                try {
                    Thread.sleep(waitTime * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted while waiting for rate limit", e);
                }
            }
        }

        URI uri = URI.create(API_BASE_URL + endpoint);
        
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(uri)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS));

        if (body != null && !body.isEmpty()) {
            requestBuilder.method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        HttpRequest request = requestBuilder.build();

        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                lastRequestTime = System.currentTimeMillis();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                // Update rate limit information
                updateRateLimits(response.headers());
                
                int statusCode = response.statusCode();
                
                if (statusCode >= 200 && statusCode < 300) {
                    return response.body();
                } else if (statusCode == 429) { // Too Many Requests
                    rateLimitRemaining = 0;
                    long waitTime = calculateWaitTime();
                    logger.warn("Rate limited (429), waiting {} seconds", waitTime);
                    Thread.sleep(waitTime * 1000);
                    retryCount++;
                    continue;
                } else if (statusCode >= 500) { // Server error
                    logger.warn("Server error {} for {} {}, retry {}/{}", 
                        statusCode, method, endpoint, retryCount + 1, MAX_RETRIES);
                    retryCount++;
                    if (retryCount < MAX_RETRIES) {
                        Thread.sleep(1000 * (long) Math.pow(2, retryCount)); // Exponential backoff
                    }
                    continue;
                } else {
                    throw new IOException("HTTP " + statusCode + " for " + method + " " + endpoint + ": " + response.body());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            } catch (Exception e) {
                if (retryCount == MAX_RETRIES - 1) {
                    throw new IOException("Failed to execute request after " + MAX_RETRIES + " retries: " + e.getMessage(), e);
                }
                retryCount++;
                try {
                    Thread.sleep(1000 * (long) Math.pow(2, retryCount)); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted during retry", ie);
                }
            }
        }

        return null;
    }

    /**
     * Update rate limit information from response headers
     */
    private void updateRateLimits(java.net.http.HttpHeaders headers) {
        try {
            String remaining = headers.firstValue("X-RateLimit-Remaining").orElse(null);
            if (remaining != null) {
                rateLimitRemaining = Integer.parseInt(remaining);
            }
            
            String reset = headers.firstValue("X-RateLimit-Reset").orElse(null);
            if (reset != null) {
                rateLimitReset = Integer.parseInt(reset);
            }
        } catch (NumberFormatException e) {
            logger.debug("Failed to parse rate limit headers: {}", e.getMessage());
        }
    }

    /**
     * Calculate wait time based on rate limits
     */
    private long calculateWaitTime() {
        long currentTime = System.currentTimeMillis() / 1000;
        long resetTime = lastRequestTime / 1000 + rateLimitReset;
        
        if (resetTime > currentTime) {
            return resetTime - currentTime;
        }
        return 0;
    }

    /**
     * Test API connection
     */
    public void testConnection() throws IOException {
        try {
            String response = executeGet("/person/info");
            if (response == null || response.isEmpty()) {
                throw new IOException("Empty response from API test");
            }
            logger.debug("API connection test successful");
        } catch (IOException e) {
            throw new IOException("Failed to connect to Rachio API: " + e.getMessage(), e);
        }
    }

    /**
     * Get the Gson instance
     */
    public Gson getGson() {
        return gson;
    }

    /**
     * Get remaining rate limit
     */
    public int getRateLimitRemaining() {
        return rateLimitRemaining;
    }

    /**
     * Get rate limit reset time in seconds
     */
    public int getRateLimitReset() {
        return rateLimitReset;
    }

    /**
     * Get device forecast
     */
    public @Nullable String getDeviceForecast(String deviceId) throws IOException {
        return executeGet("/device/" + deviceId + "/forecast");
    }

    /**
     * Run all zones
     */
    public @Nullable String runAllZones(String deviceId, int duration) throws IOException {
        String body = String.format("{\"duration\":%d}", duration);
        return executePut("/device/" + deviceId + "/start_all_zones", body);
    }

    /**
     * Stop watering
     */
    public @Nullable String stopWatering(String deviceId) throws IOException {
        return executePut("/device/" + deviceId + "/stop_water", null);
    }

    /**
     * Set rain delay
     */
    public @Nullable String rainDelay(String deviceId, int duration) throws IOException {
        String body = String.format("{\"duration\":%d}", duration);
        return executePut("/device/" + deviceId + "/rain_delay", body);
    }

    /**
     * Run next zone
     */
    public @Nullable String runNextZone(String deviceId) throws IOException {
        return executePut("/device/" + deviceId + "/start_next_zone", null);
    }

    /**
     * Start a zone
     */
    public @Nullable String startZone(String deviceId, String zoneId, int duration) throws IOException {
        String body = String.format("{\"duration\":%d}", duration);
        return executePut("/zone/" + zoneId + "/start", body);
    }

    /**
     * Stop a zone
     */
    public @Nullable String stopZone(String deviceId, String zoneId) throws IOException {
        return executePut("/zone/" + zoneId + "/stop", null);
    }

    /**
     * Set zone enabled status
     */
    public @Nullable String setZoneEnabled(String deviceId, String zoneId, boolean enabled) throws IOException {
        String body = String.format("{\"enabled\":%s}", enabled);
        return executePut("/zone/" + zoneId + "/enabled", body);
    }

    /**
     * Get device usage
     */
    public @Nullable String getDeviceUsage(String deviceId) throws IOException {
        return executeGet("/device/" + deviceId + "/usage");
    }

    /**
     * Get device savings
     */
    public @Nullable String getDeviceSavings(String deviceId) throws IOException {
        return executeGet("/device/" + deviceId + "/savings");
    }

    /**
     * Pause device
     */
    public @Nullable String pauseDevice(String deviceId) throws IOException {
        return executePut("/device/" + deviceId + "/pause", null);
    }

    /**
     * Get device alerts
     */
    public @Nullable String getDeviceAlerts(String deviceId) throws IOException {
        return executeGet("/device/" + deviceId + "/alerts");
    }

    /**
     * Get zone watering history
     */
    public @Nullable String getZoneWateringHistory(String zoneId) throws IOException {
        return executeGet("/zone/" + zoneId + "/watering");
    }

    /**
     * Get device schedules
     */
    public @Nullable String getDeviceSchedules(String deviceId) throws IOException {
        return executeGet("/device/" + deviceId + "/schedule");
    }
}
