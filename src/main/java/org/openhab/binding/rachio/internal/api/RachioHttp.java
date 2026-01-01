package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP client for Rachio API
 *
 * @author Damion Boyett - Refactor
 */
@NonNullByDefault
public class RachioHttp {
    private static final Logger LOGGER = LoggerFactory.getLogger(RachioHttp.class);

    private static String apiKey = "";
    private static final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

    // ===== RATE LIMIT CALLBACK INTERFACE WITH THREAD SAFETY =====
    /**
     * Callback interface for rate limit updates.
     * Enables bridge handler to receive real-time rate limit data.
     */
    public interface RateLimitCallback {
        void onRateLimitUpdate(int remaining, int limit, long reset, String status);
    }

    private static @Nullable RateLimitCallback rateLimitCallback = null;
    private static final ReentrantLock callbackLock = new ReentrantLock();

    /**
     * Set the callback for rate limit updates.
     * Bridge handler must call this during initialization.
     * 
     * @param callback The callback implementation
     */
    public static void setRateLimitCallback(@Nullable RateLimitCallback callback) {
        callbackLock.lock();
        try {
            rateLimitCallback = callback;
            LOGGER.debug("Rate limit callback {}", callback != null ? "set" : "cleared");
        } finally {
            callbackLock.unlock();
        }
    }

    /**
     * Get the current rate limit callback safely.
     * 
     * @return The current callback or null
     */
    private static @Nullable RateLimitCallback getRateLimitCallback() {
        callbackLock.lock();
        try {
            return rateLimitCallback;
        } finally {
            callbackLock.unlock();
        }
    }

    /**
     * Set the API key for all HTTP requests
     * 
     * @param key Rachio API key
     */
    public static void setApiKey(String key) {
        apiKey = key;
        LOGGER.debug("Rachio API key set (length: {})", key.length());
    }

    /**
     * Make a GET request to the Rachio API
     *
     * @param endpoint API endpoint (without base URL)
     * @return Response body
     * @throws RachioApiException if the request fails
     */
    public static String get(String endpoint) throws RachioApiException {
        return makeRequest("GET", endpoint, null);
    }

    /**
     * Make a PUT request to the Rachio API
     *
     * @param endpoint API endpoint (without base URL)
     * @param body Request body (JSON string)
     * @throws RachioApiException if the request fails
     */
    public static void put(String endpoint, String body) throws RachioApiException {
        makeRequest("PUT", endpoint, body);
    }

    /**
     * Make a POST request to the Rachio API
     *
     * @param endpoint API endpoint (without base URL)
     * @param body Request body (JSON string)
     * @return Response body
     * @throws RachioApiException if the request fails
     */
    public static String post(String endpoint, String body) throws RachioApiException {
        return makeRequest("POST", endpoint, body);
    }

    /**
     * Make a DELETE request to the Rachio API
     *
     * @param endpoint API endpoint (without base URL)
     * @throws RachioApiException if the request fails
     */
    public static void delete(String endpoint) throws RachioApiException {
        makeRequest("DELETE", endpoint, null);
    }

    /**
     * ===== Core HTTP request implementation =====
     * Make an HTTP request to the Rachio API
     *
     * @param method HTTP method (GET, PUT, POST, DELETE)
     * @param endpoint API endpoint
     * @param body Request body (null for GET and DELETE)
     * @return Response body for GET and POST, empty string for PUT and DELETE
     * @throws RachioApiException if the request fails
     */
    private static String makeRequest(String method, String endpoint, @Nullable String body) throws RachioApiException {

        if (apiKey.isEmpty()) {
            throw new RachioApiException("Rachio API key not set. Call setApiKey() first.");
        }

        // Build full URL
        String url = RachioBindingConstants.API_BASE_URL + endpoint;
        LOGGER.debug("Making {} request to: {}", method, url);

        try {
            // Build request
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(url))
                    .header("Authorization", RachioBindingConstants.RACHIO_AUTH_BEARER_PREFIX + apiKey)
                    .header("Content-Type", "application/json").header("Accept", "application/json");

            // Add method-specific configuration
            switch (method) {
                case "GET":
                    requestBuilder.GET();
                    break;
                case "PUT":
                    if (body != null) {
                        requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body));
                    } else {
                        requestBuilder.PUT(HttpRequest.BodyPublishers.noBody());
                    }
                    break;
                case "POST":
                    if (body != null) {
                        requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
                    } else {
                        requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
                    }
                    break;
                case "DELETE":
                    requestBuilder.DELETE();
                    break;
                default:
                    throw new RachioApiException("Unsupported HTTP method: " + method);
            }

            HttpRequest request = requestBuilder.build();

            // Send request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Check response status
            int statusCode = response.statusCode();
            LOGGER.debug("Response status: {} for {}", statusCode, endpoint);

            // Handle rate limiting headers
            handleRateLimitHeaders(response);

            // Check for errors
            if (statusCode >= 400) {
                String errorMessage = "HTTP " + statusCode + " for " + endpoint;
                String responseBody = response.body();
                if (!responseBody.isEmpty()) {
                    errorMessage += ": " + responseBody;
                }

                throw new RachioApiException(errorMessage);
            }

            // Return response body for GET and POST
            if ("GET".equals(method) || "POST".equals(method)) {
                return response.body();

            }

            return "";

        } catch (IOException e) {
            throw new RachioApiException("Network error for " + endpoint + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RachioApiException("Request interrupted for " + endpoint + ": " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new RachioApiException("Invalid URL or request for " + endpoint + ": " + e.getMessage(), e);
        }
    }

    /**
     * ===== Rate limiting header handling with thread-safe callback support =====
     * Extract and log rate limit information from response headers
     *
     * @param response HTTP response
     */
    private static void handleRateLimitHeaders(HttpResponse<String> response) {
        // Get rate limit headers
        java.util.Optional<String> remainingOpt = response.headers()
                .firstValue(RachioBindingConstants.HEADER_RATE_LIMIT_REMAINING);
        java.util.Optional<String> resetOpt = response.headers()
                .firstValue(RachioBindingConstants.HEADER_RATE_LIMIT_RESET);
        java.util.Optional<String> limitOpt = response.headers()
                .firstValue(RachioBindingConstants.HEADER_RATE_LIMIT_LIMIT);

        int remaining = -1;
        int limit = RachioBindingConstants.MAX_RATE_LIMIT; // Default if header missing
        long resetSeconds = 0;
        String status = RachioBindingConstants.STATUS_NORMAL;

        if (remainingOpt.isPresent()) {
            try {
                remaining = Integer.parseInt(remainingOpt.get());

                // Parse limit if available, otherwise use default
                if (limitOpt.isPresent()) {
                    try {
                        limit = Integer.parseInt(limitOpt.get());
                    } catch (NumberFormatException e) {
                        LOGGER.debug("Failed to parse rate limit limit header: {}", limitOpt.get());
                    }
                }

                // Calculate percentage safely
                double remainingDouble = remaining;
                double limitDouble = limit;
                int percentRemaining = (limit > 0) ? (int) ((remainingDouble / limitDouble) * 100) : 100;

                // Determine status based on README thresholds
                if (percentRemaining <= 2) {
                    status = RachioBindingConstants.STATUS_CRITICAL;
                } else if (percentRemaining <= 10) {
                    status = RachioBindingConstants.STATUS_WARNING;
                } else {
                    status = RachioBindingConstants.STATUS_NORMAL;
                }

                LOGGER.debug("Rate limit: {}/{} remaining ({}%), status: {}", remaining, limit, percentRemaining,
                        status);

                // Log warning if critical
                if (RachioBindingConstants.STATUS_CRITICAL.equals(status)) {
                    LOGGER.warn("Rachio API rate limit critical: {} calls remaining ({}%)", remaining,
                            percentRemaining);
                }

            } catch (NumberFormatException e) {
                LOGGER.debug("Failed to parse rate limit header: {}", remainingOpt.get());
            }
        }

        if (resetOpt.isPresent()) {
            try {
                resetSeconds = Long.parseLong(resetOpt.get());
                LOGGER.debug("Rate limit resets in {} seconds", resetSeconds);
            } catch (NumberFormatException e) {
                LOGGER.debug("Failed to parse rate limit reset header: {}", resetOpt.get());
            }
        }

        // ===== Thread-safe callback notification =====
        if (remaining >= 0) { // Only notify if we successfully parsed remaining
            RateLimitCallback callback = getRateLimitCallback();
            if (callback != null) {
                try {
                    callback.onRateLimitUpdate(remaining, limit, resetSeconds, status);
                    LOGGER.trace("Rate limit callback executed: {}/{}, reset: {}, status: {}", remaining, limit,
                            resetSeconds, status);
                } catch (Exception e) {
                    LOGGER.debug("Error in rate limit callback: {}", e.getMessage(), e);
                }
            } else {
                LOGGER.trace("No rate limit callback registered");
            }
        }
    }

    /**
     * ===== Helper method for testing connection =====
     * Test the API connection with the current API key
     *
     * @return true if connection successful
     */
    public static boolean testConnection() {
        try {
            // Simple GET request to person endpoint to test connection
            get(RachioBindingConstants.API_PERSON_ENDPOINT);
            return true;
        } catch (RachioApiException e) {
            LOGGER.debug("API connection test failed: {}", e.getMessage());
            return false;
        }
    }
}
