package org.openhab.binding.rachio.internal.api;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.rachio.internal.api.dto.RachioWebHookEvent;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioSchedule;
import org.openhab.binding.rachio.internal.api.dto.RachioUsage;
import org.openhab.binding.rachio.internal.api.dto.RachioSavings;
import org.openhab.binding.rachio.internal.api.dto.RachioAlert;
import org.openhab.binding.rachio.internal.api.dto.RachioForecast;
import org.openhab.binding.rachio.internal.api.dto.RachioEventSummary;
import org.openhab.binding.rachio.internal.api.dto.ZoneRunStatus;
import org.openhab.binding.rachio.internal.api.exception.RachioApiException;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * HTTP client for Rachio API using OpenHAB's HttpClientFactory for 5.x compatibility
 * 
 * This replaces all direct HttpURLConnection usage with the Jetty HttpClient
 * provided by OpenHAB's HttpClientFactory, which is MANDATORY for OpenHAB 5.0.1+
 */
@Component(service = RachioHttp.class, immediate = true)
public class RachioHttp {
    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);
    
    // JSON parser with custom type adapters
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .registerTypeAdapterFactory(new SafeReflectiveTypeAdapterFactory())
            .create();
    
    // HTTP client via OpenHAB's factory (CRITICAL for 5.x compatibility)
    private HttpClientFactory httpClientFactory;
    private org.eclipse.jetty.client.HttpClient httpClient;
    
    // Base API URLs
    private static final String BASE_URL = "https://api.rach.io/1/public";
    private static final String WEBHOOK_URL = "https://api.rach.io/1/public/webhook";
    
    @Reference
    public void setHttpClientFactory(HttpClientFactory httpClientFactory) {
        this.httpClientFactory = httpClientFactory;
        // Use OpenHAB's shared HttpClient with proper configuration
        this.httpClient = httpClientFactory.getCommonHttpClient();
        logger.debug("HttpClientFactory initialized for RachioHttp");
    }
    
    public void unsetHttpClientFactory(HttpClientFactory httpClientFactory) {
        this.httpClient = null;
        this.httpClientFactory = null;
    }
    
    @Activate
    protected void activate() {
        logger.debug("RachioHttp service activated");
    }
    
    @Deactivate
    protected void deactivate() {
        logger.debug("RachioHttp service deactivated");
    }
    
    @Modified
    protected void modified() {
        logger.debug("RachioHttp service configuration modified");
    }
    
    /**
     * Execute HTTP request with proper error handling and logging
     */
    private String executeHttpRequest(String url, String method, String apiKey, 
                                     String body, String contentType) throws RachioApiException {
        if (httpClient == null) {
            throw new RachioApiException("HttpClient not initialized - binding service not ready");
        }
        
        try {
            // Convert method string to HttpMethod
            HttpMethod httpMethod = HttpMethod.fromString(method.toUpperCase());
            
            // Build request with proper headers
            Request request = httpClient.newRequest(url)
                .method(httpMethod)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .timeout(15, TimeUnit.SECONDS)
                .idleTimeout(30, TimeUnit.SECONDS);
            
            // Add body if present
            if (body != null && !body.isEmpty()) {
                String contentTypeHeader = contentType != null ? contentType : "application/json";
                request.header("Content-Type", contentTypeHeader)
                       .content(new StringContentProvider(body, StandardCharsets.UTF_8));
                
                logger.trace("HTTP {} to {} with body: {}", method, url, body);
            } else {
                logger.trace("HTTP {} to {}", method, url);
            }
            
            // Execute request
            ContentResponse response = request.send();
            int status = response.getStatus();
            String responseBody = response.getContentAsString();
            
            // Log based on status
            if (status >= 200 && status < 300) {
                logger.debug("HTTP {} {} -> {}", method, url, status);
                logger.trace("Response: {}", responseBody);
                return responseBody;
            } else if (status == 401) {
                logger.error("HTTP {} {} -> 401 Unauthorized. Invalid API key?", method, url);
                throw new RachioApiException("Unauthorized - check API key configuration");
            } else if (status == 404) {
                logger.warn("HTTP {} {} -> 404 Not Found", method, url);
                throw new RachioApiException("Resource not found at: " + url);
            } else if (status == 429) {
                logger.warn("HTTP {} {} -> 429 Rate Limited", method, url);
                throw new RachioApiException("Rate limited by Rachio API - try again later");
            } else if (status >= 500) {
                logger.error("HTTP {} {} -> {} Server Error: {}", method, url, status, responseBody);
                throw new RachioApiException("Rachio server error: HTTP " + status);
            } else {
                logger.warn("HTTP {} {} -> {}: {}", method, url, status, responseBody);
                throw new RachioApiException("HTTP " + status + ": " + responseBody);
            }
        } catch (TimeoutException e) {
            logger.warn("Request timeout for {} {} after 15 seconds", method, url, e);
            throw new RachioApiException("Request timeout - Rachio API may be unavailable", e);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid HTTP method or URL: {} {}", method, url, e);
            throw new RachioApiException("Invalid request configuration: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("HTTP request failed for {} {}: {}", method, url, e.getMessage(), e);
            throw new RachioApiException("HTTP request failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get person information for the API key
     */
    public RachioPerson getPerson(String apiKey) throws RachioApiException {
        String url = BASE_URL + "/person/info";
        String response = executeHttpRequest(url, "GET", apiKey, null, null);
        
        try {
            return gson.fromJson(response, RachioPerson.class);
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse person response: {}", response, e);
            throw new RachioApiException("Invalid JSON response from Rachio API", e);
        }
    }
    
    /**
     * Get all devices for the person
     */
    public List<RachioDevice> getDevices(String apiKey) throws RachioApiException {
        String url = BASE_URL + "/person/" + getPersonId(apiKey);
        String response = executeHttpRequest(url, "GET", apiKey, null, null);
        
        try {
            RachioPerson person = gson.fromJson(response, RachioPerson.class);
            return person.devices;
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse devices response: {}", response, e);
            throw new RachioApiException("Invalid JSON response for devices", e);
        }
    }
    
    /**
     * Get device by ID
     */
    public RachioDevice getDevice(String apiKey, String deviceId) throws RachioApiException {
        String url = BASE_URL + "/device/" + deviceId;
        String response = executeHttpRequest(url, "GET", apiKey, null, null);
        
        try {
            return gson.fromJson(response, RachioDevice.class);
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse device response: {}", response, e);
            throw new RachioApiException("Invalid JSON response for device " + deviceId, e);
        }
    }
    
    /**
     * Start a zone
     */
    public void startZone(String apiKey, String zoneId, int durationSeconds) throws RachioApiException {
        String url = BASE_URL + "/zone/start";
        String body = String.format("{\"id\":\"%s\",\"duration\":%d}", zoneId, durationSeconds);
        
        executeHttpRequest(url, "PUT", apiKey, body, "application/json");
        logger.debug("Started zone {} for {} seconds", zoneId, durationSeconds);
    }
    
    /**
     * Stop a zone
     */
    public void stopZone(String apiKey, String zoneId) throws RachioApiException {
        String url = BASE_URL + "/zone/stop";
        String body = String.format("{\"id\":\"%s\"}", zoneId);
        
        executeHttpRequest(url, "PUT", apiKey, body, "application/json");
        logger.debug("Stopped zone {}", zoneId);
    }
    
    /**
     * Get zone run status
     */
    public ZoneRunStatus getZoneRunStatus(String apiKey, String zoneId) throws RachioApiException {
        String url = BASE_URL + "/zone/" + zoneId + "/current_schedule";
        String response = executeHttpRequest(url, "GET", apiKey, null, null);
        
        try {
            return gson.fromJson(response, ZoneRunStatus.class);
        } catch (JsonSyntaxException e) {
            // If zone is not running, API returns empty object or 404
            if (response != null && response.trim().isEmpty()) {
                return new ZoneRunStatus(); // Return empty status
            }
            logger.error("Failed to parse zone run status: {}", response, e);
            throw new RachioApiException("Invalid JSON response for zone status", e);
        }
    }
    
    /**
     * Get device schedule
     */
    public List<RachioSchedule> getDeviceSchedule(String apiKey, String deviceId) throws RachioApiException {
        String url = BASE_URL + "/device/" + deviceId + "/schedule";
        String response = executeHttpRequest(url, "GET", apiKey, null, null);
        
        try {
            Type listType = new TypeToken<List<RachioSchedule>>(){}.getType();
            return gson.fromJson(response, listType);
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse schedule response: {}", response, e);
            throw new RachioApiException("Invalid JSON response for schedule", e);
        }
    }
    
    /**
     * Get device usage
     */
    public RachioUsage getDeviceUsage(String apiKey, String deviceId, int year, int month) throws RachioApiException {
        String url = BASE_URL + "/device/" + deviceId + "/waterusage/" + year + "/" + month;
        String response = executeHttpRequest(url, "GET", apiKey, null, null);
        
        try {
            return gson.fromJson(response, RachioUsage.class);
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse usage response: {}", response, e);
            throw new RachioApiException("Invalid JSON response for water usage", e);
        }
    }
    
    /**
     * Get device savings
     */
    public RachioSavings getDeviceSavings(String apiKey, String deviceId) throws RachioApiException {
        String url = BASE_URL + "/device/" + deviceId + "/savings";
        String response = executeHttpRequest(url, "GET", apiKey, null, null);
        
        try {
            return gson.fromJson(response, RachioSavings.class);
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse savings response: {}", response, e);
            throw new RachioApiException("Invalid JSON response for savings", e);
        }
    }
    
    /**
     * Get device alerts
     */
    public List<RachioAlert> getDeviceAlerts(String apiKey, String deviceId) throws RachioApiException {
        String url = BASE_URL + "/device/" + deviceId + "/alert";
        String response = executeHttpRequest(url, "GET", apiKey, null, null);
        
        try {
            Type listType = new TypeToken<List<RachioAlert>>(){}.getType();
            return gson.fromJson(response, listType);
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse alerts response: {}", response, e);
            throw new RachioApiException("Invalid JSON response for alerts", e);
        }
    }
    
    /**
     * Get forecast for device
     */
    public RachioForecast getForecast(String apiKey, String deviceId) throws RachioApiException {
        String url = BASE_URL + "/device/" + deviceId + "/forecast";
        String response = executeHttpRequest(url, "GET", apiKey, null, null);
        
        try {
            return gson.fromJson(response, RachioForecast.class);
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse forecast response: {}", response, e);
            throw new RachioApiException("Invalid JSON response for forecast", e);
        }
    }
    
    /**
     * Get event summary for device
     */
    public RachioEventSummary getEventSummary(String apiKey, String deviceId) throws RachioApiException {
        String url = BASE_URL + "/device/" + deviceId + "/event/summary";
        String response = executeHttpRequest(url, "GET", apiKey, null, null);
        
        try {
            return gson.fromJson(response, RachioEventSummary.class);
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse event summary: {}", response, e);
            throw new RachioApiException("Invalid JSON response for event summary", e);
        }
    }
    
    /**
     * Create webhook for device
     */
    public void createWebhook(String apiKey, String deviceId, String externalId, String url) throws RachioApiException {
        String apiUrl = WEBHOOK_URL;
        String body = String.format("{\"deviceId\":\"%s\",\"externalId\":\"%s\",\"url\":\"%s\"}", 
                                   deviceId, externalId, url);
        
        executeHttpRequest(apiUrl, "POST", apiKey, body, "application/json");
        logger.debug("Created webhook for device {} with externalId {}", deviceId, externalId);
    }
    
    /**
     * Delete webhook
     */
    public void deleteWebhook(String apiKey, String webhookId) throws RachioApiException {
        String url = WEBHOOK_URL + "/" + webhookId;
        
        executeHttpRequest(url, "DELETE", apiKey, null, null);
        logger.debug("Deleted webhook {}", webhookId);
    }
    
    /**
     * List webhooks for device
     */
    public List<String> listWebhooks(String apiKey, String deviceId) throws RachioApiException {
        String url = WEBHOOK_URL + "?deviceId=" + deviceId;
        String response = executeHttpRequest(url, "GET", apiKey, null, null);
        
        try {
            Type listType = new TypeToken<List<String>>(){}.getType();
            return gson.fromJson(response, listType);
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse webhooks response: {}", response, e);
            throw new RachioApiException("Invalid JSON response for webhooks", e);
        }
    }
    
    /**
     * Parse webhook event from JSON
     */
    public RachioWebHookEvent parseWebhookEvent(String json) throws RachioApiException {
        try {
            return gson.fromJson(json, RachioWebHookEvent.class);
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse webhook event JSON: {}", json, e);
            throw new RachioApiException("Invalid webhook event JSON", e);
        }
    }
    
    /**
     * Helper method to get person ID from API key
     */
    private String getPersonId(String apiKey) throws RachioApiException {
        RachioPerson person = getPerson(apiKey);
        if (person == null || person.id == null) {
            throw new RachioApiException("Could not retrieve person ID from Rachio API");
        }
        return person.id;
    }
    
    /**
     * Test API connection
     */
    public boolean testConnection(String apiKey) {
        try {
            getPerson(apiKey);
            return true;
        } catch (RachioApiException e) {
            logger.debug("Connection test failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get API rate limit information from response headers
     * Note: Rachio API includes rate limit info in headers
     */
    public void logRateLimitInfo(String apiKey) {
        try {
            // Make a lightweight request to get headers
            String url = BASE_URL + "/person/info";
            executeHttpRequest(url, "HEAD", apiKey, null, null);
            logger.debug("Rate limit headers retrieved (not yet implemented in binding)");
        } catch (RachioApiException e) {
            // Silently ignore - this is just for logging rate limits
        }
    }
}
