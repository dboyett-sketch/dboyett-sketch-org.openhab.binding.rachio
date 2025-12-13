package org.openhab.binding.rachio.internal.api;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.rachio.internal.api.exception.RachioApiException;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component(service = RachioHttpClient.class)
public class RachioHttpClient {
    private final Logger logger = LoggerFactory.getLogger(RachioHttpClient.class);
    
    private HttpClient httpClient;
    
    @Reference
    public void setHttpClientFactory(HttpClientFactory httpClientFactory) {
        // CRITICAL: Use OpenHAB's shared HttpClient via factory
        this.httpClient = httpClientFactory.getCommonHttpClient();
        logger.debug("HttpClientFactory initialized for Rachio binding");
    }
    
    public void unsetHttpClientFactory(HttpClientFactory httpClientFactory) {
        this.httpClient = null;
    }
    
    /**
     * Execute HTTP request to Rachio API
     */
    public String executeRequest(String url, String method, String apiKey, 
                                 String body, String contentType) throws RachioApiException {
        if (httpClient == null) {
            throw new RachioApiException("HttpClient not initialized - binding not ready");
        }
        
        try {
            // Convert method string to HttpMethod
            HttpMethod httpMethod = HttpMethod.fromString(method);
            
            // Build request with proper headers
            Request request = httpClient.newRequest(url)
                .method(httpMethod)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .timeout(15, TimeUnit.SECONDS)
                .idleTimeout(30, TimeUnit.SECONDS);
            
            // Add body if present
            if (body != null && !body.isEmpty()) {
                request.header("Content-Type", contentType != null ? contentType : "application/json")
                       .content(new StringContentProvider(body, StandardCharsets.UTF_8));
            }
            
            // Execute request
            ContentResponse response = request.send();
            int status = response.getStatus();
            
            if (status >= 200 && status < 300) {
                String responseBody = response.getContentAsString();
                logger.trace("HTTP {} {} -> {}: {}", method, url, status, responseBody);
                return responseBody;
            } else {
                String errorBody = response.getContentAsString();
                logger.warn("HTTP {} {} -> {}: {}", method, url, status, errorBody);
                throw new RachioApiException("HTTP " + status + ": " + errorBody);
            }
        } catch (TimeoutException e) {
            logger.warn("Request timeout for {} {}", method, url, e);
            throw new RachioApiException("Request timeout after 15 seconds", e);
        } catch (Exception e) {
            logger.error("HTTP request failed for {} {}", method, url, e);
            throw new RachioApiException("HTTP request failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convenience method for GET requests
     */
    public String get(String url, String apiKey) throws RachioApiException {
        return executeRequest(url, "GET", apiKey, null, null);
    }
    
    /**
     * Convenience method for POST requests with JSON
     */
    public String post(String url, String apiKey, String jsonBody) throws RachioApiException {
        return executeRequest(url, "POST", apiKey, jsonBody, "application/json");
    }
    
    /**
     * Convenience method for PUT requests
     */
    public String put(String url, String apiKey, String jsonBody) throws RachioApiException {
        return executeRequest(url, "PUT", apiKey, jsonBody, "application/json");
    }
    
    /**
     * Convenience method for DELETE requests
     */
    public String delete(String url, String apiKey) throws RachioApiException {
        return executeRequest(url, "DELETE", apiKey, null, null);
    }
}
