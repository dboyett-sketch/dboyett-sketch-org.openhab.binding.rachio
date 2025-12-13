package org.openhab.binding.rachio.internal.api;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modern HTTP client for Rachio API with rate limiting, retries, and connection pooling
 */
public class RachioHttpClient {
    private final Logger logger = LoggerFactory.getLogger(RachioHttpClient.class);
    
    private final HttpClient client;
    private final String apiKey;
    private final AtomicInteger remainingRequests = new AtomicInteger(60); // Rachio rate limit
    private final Object rateLimitLock = new Object();
    
    // Singleton instance for connection pooling
    private static HttpClient sharedClient;
    
    public RachioHttpClient(String apiKey) {
        this.apiKey = apiKey;
        this.client = getOrCreateSharedClient();
    }
    
    private static synchronized HttpClient getOrCreateSharedClient() {
        if (sharedClient == null) {
            sharedClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_2)  // HTTP/2 for better performance
                .build();
        }
        return sharedClient;
    }
    
    public String executeRequest(HttpRequest.Builder requestBuilder) throws RachioApiException {
        return executeRequest(requestBuilder, 0);
    }
    
    private String executeRequest(HttpRequest.Builder requestBuilder, int retryCount) 
            throws RachioApiException {
        
        // Wait if rate limit is exceeded
        waitForRateLimit();
        
        // Add common headers
        requestBuilder
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .header("User-Agent", "OpenHAB-Rachio-Binding/5.0")
            .timeout(Duration.ofSeconds(15));
        
        HttpRequest request = requestBuilder.build();
        
        try {
            HttpResponse<String> response = client.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            // Update rate limit from headers
            updateRateLimit(response.headers());
            
            int statusCode = response.statusCode();
            
            if (statusCode >= 200 && statusCode < 300) {
                return response.body();
            } else if (shouldRetry(statusCode) && retryCount < 3) {
                logger.debug("Request failed with status {}, retrying... (attempt {})", 
                    statusCode, retryCount + 1);
                Thread.sleep(calculateBackoff(retryCount));
                return executeRequest(requestBuilder, retryCount + 1);
            } else {
                throw new RachioApiException("HTTP " + statusCode + ": " + 
                    response.body(), statusCode);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RachioApiException("Request interrupted", e);
        } catch (Exception e) {
            throw new RachioApiException("HTTP request failed", e);
        }
    }
    
    private void waitForRateLimit() {
        synchronized (rateLimitLock) {
            while (remainingRequests.get() <= 0) {
                try {
                    // Rachio: 60 requests per minute
                    logger.debug("Rate limit reached, waiting 60 seconds...");
                    rateLimitLock.wait(60000); // Wait 60 seconds
                    remainingRequests.set(60); // Reset counter
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            remainingRequests.decrementAndGet();
        }
    }
    
    private void updateRateLimit(java.net.http.HttpHeaders headers) {
        headers.firstValue("X-RateLimit-Remaining").ifPresent(value -> {
            try {
                int remaining = Integer.parseInt(value);
                synchronized (rateLimitLock) {
                    remainingRequests.set(Math.min(remaining, remainingRequests.get()));
                }
            } catch (NumberFormatException e) {
                logger.debug("Could not parse rate limit header: {}", value);
            }
        });
    }
    
    private boolean shouldRetry(int statusCode) {
        // Retry on server errors (5xx) and rate limits (429)
        return statusCode == 429 || statusCode >= 500;
    }
    
    private long calculateBackoff(int retryCount) {
        // Exponential backoff with jitter: 1s, 2s, 4s...
        long baseDelay = 1000L * (1L << retryCount); // 2^retryCount seconds
        long jitter = (long) (Math.random() * 1000); // Add up to 1s jitter
        return baseDelay + jitter;
    }
    
    // Convenience methods
    public String get(String url) throws RachioApiException {
        return executeRequest(HttpRequest.newBuilder().uri(URI.create(url)).GET());
    }
    
    public String post(String url, String body) throws RachioApiException {
        return executeRequest(HttpRequest.newBuilder()
            .uri(URI.create(url))
            .POST(HttpRequest.BodyPublishers.ofString(body)));
    }
    
    public String put(String url, String body) throws RachioApiException {
        return executeRequest(HttpRequest.newBuilder()
            .uri(URI.create(url))
            .PUT(HttpRequest.BodyPublishers.ofString(body)));
    }
    
    public void delete(String url) throws RachioApiException {
        executeRequest(HttpRequest.newBuilder()
            .uri(URI.create(url))
            .DELETE());
    }
}
