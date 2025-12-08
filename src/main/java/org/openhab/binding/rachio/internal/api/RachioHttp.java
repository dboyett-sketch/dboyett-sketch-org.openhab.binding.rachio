package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioEventSummary;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioWebHookEvent;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Handles HTTP communication with the Rachio API
 */
@NonNullByDefault
public class RachioHttp {
    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);

    private static final String API_BASE_URL = "https://api.rach.io/1/public";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);

    private final HttpClient httpClient;
    private final String apiKey;
    private final Gson gson;
    private final ScheduledExecutorService scheduler;

    // Rate limiting tracking
    private final Map<String, RateLimitInfo> rateLimitCache = new ConcurrentHashMap<>();
    private volatile int currentRateLimit = 14400; // Default: 14400 calls per day
    private volatile int remainingCalls = 14400;
    private volatile Instant resetTime = Instant.now().plus(Duration.ofDays(1));

    private static class RateLimitInfo {
        int limit;
        int remaining;
        Instant reset;
    }

    public RachioHttp(String apiKey, HttpClientFactory httpClientFactory) {
        this.apiKey = apiKey;
        this.httpClient = httpClientFactory.getCommonHttpClient();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Start periodic rate limit cleanup
        scheduler.scheduleAtFixedRate(this::cleanupRateLimitCache, 1, 1, TimeUnit.HOURS);
    }

    /**
     * Get person information
     */
    public @Nullable RachioPerson getPersonInfo() throws IOException, InterruptedException {
        String url = API_BASE_URL + "/person/info";
        String response = executeGet(url);
        
        if (response != null) {
            try {
                return gson.fromJson(response, RachioPerson.class);
            } catch (Exception e) {
                logger.error("Failed to parse person info: {}", e.getMessage(), e);
                throw new IOException("Failed to parse person info response", e);
            }
        }
        return null;
    }

    /**
     * Get device information
     */
    public @Nullable RachioDevice getDeviceInfo(String deviceId) throws IOException, InterruptedException {
        String url = API_BASE_URL + "/device/" + deviceId;
        String response = executeGet(url);
        
        if (response != null) {
            try {
                return gson.fromJson(response, RachioDevice.class);
            } catch (Exception e) {
                logger.error("Failed to parse device info: {}", e.getMessage(), e);
                throw new IOException("Failed to parse device info response", e);
            }
        }
        return null;
    }

    /**
     * Get zone information
     */
    public @Nullable RachioZone getZoneInfo(String zoneId) throws IOException, InterruptedException {
        String url = API_BASE_URL + "/zone/" + zoneId;
        String response = executeGet(url);
        
        if (response != null) {
            try {
                return gson.fromJson(response, RachioZone.class);
            } catch (Exception e) {
                logger.error("Failed to parse zone info: {}", e.getMessage(), e);
                throw new IOException("Failed to parse zone info response", e);
            }
        }
        return null;
    }

    /**
     * Execute GET request
     */
    public String executeGet(String url) throws IOException, InterruptedException {
        return executeRequest(url, "GET", null);
    }

    /**
     * Execute PUT request
     */
    public String executePut(String url, String body) throws IOException, InterruptedException {
        return executeRequest(url, "PUT", body);
    }

    /**
     * Execute POST request
     */
    public String executePost(String url, String body) throws IOException, InterruptedException {
        return executeRequest(url, "POST", body);
    }

    /**
     * Execute DELETE request
     */
    public String executeDelete(String url) throws IOException, InterruptedException {
        return executeRequest(url, "DELETE", null);
    }

    /**
     * Execute HTTP request with retry logic
     */
    private String executeRequest(String url, String method, @Nullable String body)
            throws IOException, InterruptedException {
        
        int retryCount = 0;
        IOException lastException = null;
        
        while (retryCount <= MAX_RETRIES) {
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(DEFAULT_TIMEOUT);

                switch (method) {
                    case "GET":
                        requestBuilder.GET();
                        break;
                    case "POST":
                        requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                        break;
                    case "PUT":
                        requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                        break;
                    case "DELETE":
                        requestBuilder.DELETE();
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported HTTP method: " + method);
                }

                HttpRequest request = requestBuilder.build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                // Update rate limiting from headers
                updateRateLimitInfo(response.headers().map());

                int statusCode = response.statusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    return response.body();
                } else if (statusCode == 429) { // Rate limited
                    handleRateLimit();
                    retryCount++;
                    if (retryCount <= MAX_RETRIES) {
                        Thread.sleep(RETRY_DELAY.toMillis() * retryCount);
                        continue;
                    }
                } else if (statusCode >= 500) { // Server error - retry
                    retryCount++;
                    if (retryCount <= MAX_RETRIES) {
                        Thread.sleep(RETRY_DELAY.toMillis() * retryCount);
                        continue;
                    }
                }

                throw new IOException("HTTP " + method + " request failed with status " + statusCode + ": " + response.body());

            } catch (IOException e) {
                lastException = e;
                retryCount++;
                if (retryCount <= MAX_RETRIES) {
                    logger.debug("Request failed, retrying ({}/{}): {}", retryCount, MAX_RETRIES, e.getMessage());
                    Thread.sleep(RETRY_DELAY.toMillis() * retryCount);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }
        
        throw new IOException("Request failed after " + MAX_RETRIES + " retries", lastException);
    }

    /**
     * Update rate limit information from response headers
     */
    private void updateRateLimitInfo(Map<String, List<String>> headers) {
        try {
            List<String> limitHeaders = headers.get("X-RateLimit-Limit");
            List<String> remainingHeaders = headers.get("X-RateLimit-Remaining");
            List<String> resetHeaders = headers.get("X-RateLimit-Reset");

            if (limitHeaders != null && !limitHeaders.isEmpty()) {
                currentRateLimit = Integer.parseInt(limitHeaders.get(0));
            }
            if (remainingHeaders != null && !remainingHeaders.isEmpty()) {
                remainingCalls = Integer.parseInt(remainingHeaders.get(0));
            }
            if (resetHeaders != null && !resetHeaders.isEmpty()) {
                try {
                    resetTime = Instant.ofEpochSecond(Long.parseLong(resetHeaders.get(0)));
                } catch (NumberFormatException e) {
                    logger.debug("Failed to parse rate limit reset time: {}", resetHeaders.get(0));
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to update rate limit info: {}", e.getMessage());
        }
    }

    /**
     * Handle rate limiting by adjusting behavior
     */
    private void handleRateLimit() {
        logger.warn("Rate limit exceeded, reducing polling frequency");
        // Reduce polling frequency when rate limited
        // This would be implemented in the handlers that use this class
    }

    /**
     * Clean up old rate limit cache entries
     */
    private void cleanupRateLimitCache() {
        Instant now = Instant.now();
        rateLimitCache.entrySet().removeIf(entry -> entry.getValue().reset.isBefore(now));
    }

    /**
     * Get current rate limit
     */
    public int getCurrentRateLimit() {
        return currentRateLimit;
    }

    /**
     * Get remaining calls
     */
    public int getRemainingCalls() {
        return remainingCalls;
    }

    /**
     * Get rate limit reset time
     */
    public Instant getResetTime() {
        return resetTime;
    }

    /**
     * Parse webhook event from JSON
     */
    public @Nullable RachioWebHookEvent parseWebhookEvent(String json) {
        try {
            return gson.fromJson(json, RachioWebHookEvent.class);
        } catch (Exception e) {
            logger.error("Failed to parse webhook event: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parse event summary from JSON
     */
    public @Nullable RachioEventSummary parseEventSummary(String json) {
        try {
            return gson.fromJson(json, RachioEventSummary.class);
        } catch (Exception e) {
            logger.error("Failed to parse event summary: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Shutdown the HTTP client
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
