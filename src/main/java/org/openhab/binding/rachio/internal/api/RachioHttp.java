package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.io.net.http.HttpRequestBuilder;
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

    public RachioHttp(HttpClientFactory httpClientFactory, RachioBridgeConfiguration config) {
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
        String url = "https://api.rach.io/1/public/" + endpoint;
        
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

    /**
     * Make an asynchronous GET request
     */
    public void getAsync(String endpoint, HttpCallback callback) {
        String url = "https://api.rach.io/1/public/" + endpoint;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + config.apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    handleRateLimitHeaders(response.headers().map(), endpoint);
                    return response;
                })
                .thenAccept(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError(new RachioApiException("HTTP " + response.statusCode()));
                    }
                })
                .exceptionally(throwable -> {
                    callback.onError(new RachioApiException("Async request failed", throwable));
                    return null;
                });
    }

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
     * Interface for async callbacks
     */
    public interface HttpCallback {
        void onSuccess(@Nullable String response);
        void onError(RachioApiException error);
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
