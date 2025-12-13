package org.openhab.binding.rachio.internal.api;

// REMOVED: Incompatible Java HTTP client imports
// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;

// ADDED: OpenHAB 5.x Jetty HttpClient imports
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;

// ADDED: OpenHAB HttpClientFactory import (CRITICAL FOR 5.x)
import org.openhab.core.io.net.http.HttpClientFactory;

// ADDED: OSGi Component annotations
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

// ADDED: Standard Java imports
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// ADDED: Null safety annotation
import org.eclipse.jdt.annotation.Nullable;

// Existing imports (keep these)
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP client for communicating with the Rachio API.
 * 
 * CRITICAL CHANGE: Uses OpenHAB's Jetty HttpClient via HttpClientFactory
 * for OpenHAB 5.x compatibility.
 * 
 * @author Your Name
 */
@Component(service = RachioHttpClient.class, immediate = true)
public class RachioHttpClient {
    private final Logger logger = LoggerFactory.getLogger(RachioHttpClient.class);
    
    // CRITICAL CHANGE: Use OpenHAB's HttpClientFactory instead of java.net.http.HttpClient
    private HttpClient httpClient;
    private final Gson gson;
    
    // CRITICAL CHANGE: API configuration managed via component
    private String apiKey;
    
    /**
     * Constructor for JSON parsing setup
     */
    public RachioHttpClient() {
        this.gson = new GsonBuilder()
            .registerTypeAdapterFactory(new SafeReflectiveTypeAdapterFactory())
            .create();
        this.apiKey = "";
    }
    
    /**
     * CRITICAL CHANGE: HttpClientFactory injection via OSGi @Reference
     * This is REQUIRED for OpenHAB 5.x compatibility
     */
    @Reference
    public void setHttpClientFactory(HttpClientFactory httpClientFactory) {
        logger.debug("HttpClientFactory injected");
        this.httpClient = httpClientFactory.getCommonHttpClient();
    }
    
    /**
     * Unset method for OSGi dynamic reference
     */
    public void unsetHttpClientFactory(HttpClientFactory httpClientFactory) {
        logger.debug("HttpClientFactory unset");
        this.httpClient = null;
    }
    
    /**
     * Activation method for OSGi component
     */
    @Activate
    public void activate() {
        logger.info("Rachio HTTP Client activated");
    }
    
    /**
     * Deactivation method for OSGi component
     */
    @Deactivate
    public void deactivate() {
        logger.info("Rachio HTTP Client deactivated");
    }
    
    /**
     * Update method for configuration changes
     */
    @Modified
    public void modified() {
        logger.debug("Rachio HTTP Client configuration modified");
    }
    
    // CRITICAL CHANGE: Removed old constructor that created java.net.http.HttpClient
    // public RachioHttpClient(String apiKey) {
    //     this.apiKey = apiKey;
    //     this.client = HttpClient.newBuilder()
    //         .connectTimeout(Duration.ofSeconds(10))
    //         .build();
    // }
    
    /**
     * Set the API key for Rachio API requests
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    /**
     * CRITICAL CHANGE: New implementation using OpenHAB's Jetty HttpClient
     * 
     * Executes an HTTP request to the Rachio API
     * 
     * @param url The URL to request
     * @param method The HTTP method (GET, POST, PUT, DELETE)
     * @param body Optional request body for POST/PUT
     * @return The response body as a string
     * @throws RachioApiException If the request fails
     */
    public String executeRequest(String url, String method, @Nullable String body) throws RachioApiException {
        // Validate HttpClient is available
        if (httpClient == null) {
            throw new RachioApiException("HttpClient not initialized. HttpClientFactory not injected.");
        }
        
        // Validate API key
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RachioApiException("API key not configured");
        }
        
        try {
            logger.debug("Executing {} request to: {}", method, url);
            
            // Convert method string to HttpMethod enum
            HttpMethod httpMethod = HttpMethod.fromString(method);
            
            // Build the request using Jetty HttpClient
            Request request = httpClient.newRequest(url)
                .method(httpMethod)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .timeout(15, TimeUnit.SECONDS); // Reasonable timeout for Rachio API
            
            // Add body for POST/PUT requests
            if (body != null && !body.isEmpty() && 
                (httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT)) {
                request.header("Content-Type", "application/json")
                       .content(new StringContentProvider(body, StandardCharsets.UTF_8));
            }
            
            // Execute the request
            ContentResponse response = request.send();
            
            // Log response status for debugging
            logger.debug("Response status: {} for {} {}", response.getStatus(), method, url);
            
            // Check for successful response
            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                String responseBody = response.getContentAsString();
                logger.trace("Response body: {}", responseBody);
                return responseBody;
            } else {
                // Handle HTTP error responses
                String errorBody = response.getContentAsString();
                logger.warn("HTTP {} error from Rachio API: {}", response.getStatus(), errorBody);
                throw new RachioApiException("HTTP " + response.getStatus() + ": " + errorBody);
            }
            
        } catch (TimeoutException e) {
            logger.warn("Request timeout for {} {}", method, url, e);
            throw new RachioApiException("Request timeout after 15 seconds", e);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid HTTP method: {}", method, e);
            throw new RachioApiException("Invalid HTTP method: " + method, e);
        } catch (Exception e) {
            logger.warn("HTTP request failed for {} {}", method, url, e);
            throw new RachioApiException("HTTP request failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Helper method for GET requests
     */
    public String get(String url) throws RachioApiException {
        return executeRequest(url, "GET", null);
    }
    
    /**
     * Helper method for POST requests
     */
    public String post(String url, String body) throws RachioApiException {
        return executeRequest(url, "POST", body);
    }
    
    /**
     * Helper method for PUT requests
     */
    public String put(String url, String body) throws RachioApiException {
        return executeRequest(url, "PUT", body);
    }
    
    /**
     * Helper method for DELETE requests
     */
    public String delete(String url) throws RachioApiException {
        return executeRequest(url, "DELETE", null);
    }
    
    /**
     * Gets person information from Rachio API
     * CRITICAL CHANGE: Uses new executeRequest method
     */
    public RachioPerson getPersonInfo() throws RachioApiException {
        String url = "https://api.rach.io/1/public/person/info";
        String response = get(url);
        return gson.fromJson(response, RachioPerson.class);
    }
    
    /**
     * Gets device information from Rachio API
     * CRITICAL CHANGE: Uses new executeRequest method
     */
    public RachioDevice getDevice(String deviceId) throws RachioApiException {
        String url = "https://api.rach.io/1/public/device/" + deviceId;
        String response = get(url);
        return gson.fromJson(response, RachioDevice.class);
    }
    
    /**
     * Starts a zone
     * CRITICAL CHANGE: Uses new executeRequest method
     */
    public void startZone(String zoneId, int duration) throws RachioApiException {
        String url = "https://api.rach.io/1/public/zone/start";
        String body = String.format("{\"id\":\"%s\",\"duration\":%d}", zoneId, duration);
        post(url, body);
    }
    
    /**
     * Stops watering
     * CRITICAL CHANGE: Uses new executeRequest method
     */
    public void stopWatering(String deviceId) throws RachioApiException {
        String url = "https://api.rach.io/1/public/device/stop_water";
        String body = String.format("{\"id\":\"%s\"}", deviceId);
        put(url, body);
    }
    
    /**
     * Gets webhooks for a device
     * CRITICAL CHANGE: Uses new executeRequest method
     */
    public String getWebhooks(String deviceId) throws RachioApiException {
        String url = "https://api.rach.io/1/public/device/" + deviceId + "/webhook";
        return get(url);
    }
    
    /**
     * Creates a webhook
     * CRITICAL CHANGE: Uses new executeRequest method
     */
    public void createWebhook(String deviceId, String externalId, String url) throws RachioApiException {
        String apiUrl = "https://api.rach.io/1/public/device/" + deviceId + "/webhook";
        String body = String.format("{\"externalId\":\"%s\",\"url\":\"%s\"}", externalId, url);
        post(apiUrl, body);
    }
    
    /**
     * Deletes a webhook
     * CRITICAL CHANGE: Uses new executeRequest method
     */
    public void deleteWebhook(String deviceId, String webhookId) throws RachioApiException {
        String url = "https://api.rach.io/1/public/device/" + deviceId + "/webhook/" + webhookId;
        delete(url);
    }
    
    /**
     * Test method to verify API connection
     * CRITICAL CHANGE: Uses new executeRequest method
     */
    public boolean testConnection() {
        try {
            getPersonInfo();
            return true;
        } catch (RachioApiException e) {
            logger.debug("Connection test failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * CRITICAL CHANGE: Getter for httpClient for testing/diagnostics
     * Note: Should only be used internally or for testing
     */
    protected HttpClient getHttpClient() {
        return httpClient;
    }
    
    /**
     * CRITICAL CHANGE: Getter for API key (for debugging only)
     */
    protected String getApiKey() {
        return apiKey;
    }
}
