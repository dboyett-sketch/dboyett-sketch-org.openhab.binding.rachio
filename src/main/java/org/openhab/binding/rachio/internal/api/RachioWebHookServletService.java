package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Service for managing Rachio webhook registration and callback handling
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioWebHookServletService {

    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServletService.class);

    private static final String WEBHOOK_API_URL = "https://api.rach.io/1/public/webhook";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);
    private static final String USER_AGENT = "OpenHAB-Rachio-Binding/5.0";

    private final RachioBridgeHandler bridgeHandler;
    private final String apiKey;
    private final String webhookSecret;
    private final String externalUrl;
    private final HttpClient httpClient;
    private final Gson gson;
    private final List<RachioApiWebHookEntry> registeredWebhooks = new CopyOnWriteArrayList<>();

    private boolean initialized = false;
    private @Nullable String currentWebhookId;

    /**
     * Constructor
     */
    public RachioWebHookServletService(RachioBridgeHandler bridgeHandler, String apiKey, 
                                      String webhookSecret, String externalUrl) {
        this.bridgeHandler = bridgeHandler;
        this.apiKey = apiKey;
        this.webhookSecret = webhookSecret;
        this.externalUrl = externalUrl;
        
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        
        this.gson = new GsonBuilder().create();
        
        logger.debug("Webhook service initialized with external URL: {}", externalUrl);
    }

    /**
     * Initialize the webhook service
     */
    public void initialize() {
        if (initialized) {
            return;
        }
        
        try {
            // Check for existing webhooks
            List<RachioApiWebHookEntry> existingWebhooks = getWebhooks();
            
            // Check if we already have a webhook registered for our URL
            String callbackUrl = getCallbackUrl();
            for (RachioApiWebHookEntry webhook : existingWebhooks) {
                if (webhook.url != null && webhook.url.equals(callbackUrl)) {
                    currentWebhookId = webhook.id;
                    registeredWebhooks.add(webhook);
                    logger.info("Found existing webhook registration: {}", webhook.id);
                    initialized = true;
                    return;
                }
            }
            
            logger.debug("No existing webhook found for URL: {}", callbackUrl);
            initialized = true;
            
        } catch (Exception e) {
            logger.error("Failed to initialize webhook service: {}", e.getMessage(), e);
        }
    }

    /**
     * Register a webhook with Rachio
     */
    public boolean registerWebhook() {
        try {
            String callbackUrl = getCallbackUrl();
            logger.info("Registering webhook with callback URL: {}", callbackUrl);
            
            // Create webhook registration payload
            String payload = String.format(
                    "{\"url\":\"%s\",\"externalId\":\"%s\",\"eventTypes\":[%s]}",
                    callbackUrl,
                    "openhab-rachio-" + System.currentTimeMillis(),
                    getEventTypes()
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(WEBHOOK_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("User-Agent", USER_AGENT)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                RachioApiWebHookEntry webhook = gson.fromJson(response.body(), RachioApiWebHookEntry.class);
                if (webhook != null && webhook.id != null) {
                    currentWebhookId = webhook.id;
                    registeredWebhooks.add(webhook);
                    logger.info("Webhook registered successfully: {}", webhook.id);
                    return true;
                } else {
                    logger.error("Failed to parse webhook registration response");
                }
            } else {
                logger.error("Webhook registration failed: {} - {}", 
                        response.statusCode(), response.body());
            }
            
        } catch (IOException | InterruptedException e) {
            logger.error("Error registering webhook: {}", e.getMessage(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            logger.error("Unexpected error registering webhook: {}", e.getMessage(), e);
        }
        
        return false;
    }

    /**
     * Unregister the current webhook
     */
    public boolean unregisterWebhook() {
        String localWebhookId = currentWebhookId;
        if (localWebhookId == null || localWebhookId.isEmpty()) {
            logger.warn("No webhook ID to unregister");
            return true; // Nothing to unregister
        }
        
        try {
            String url = WEBHOOK_API_URL + "/" + localWebhookId;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("User-Agent", USER_AGENT)
                    .DELETE()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 204 || response.statusCode() == 200) {
                registeredWebhooks.removeIf(w -> localWebhookId.equals(w.id));
                currentWebhookId = null;
                logger.info("Webhook unregistered successfully: {}", localWebhookId);
                return true;
            } else {
                logger.error("Webhook unregistration failed: {} - {}", 
                        response.statusCode(), response.body());
            }
            
        } catch (IOException | InterruptedException e) {
            logger.error("Error unregistering webhook: {}", e.getMessage(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        
        return false;
    }

    /**
     * Get all registered webhooks for the account
     */
    public List<RachioApiWebHookEntry> getWebhooks() throws RachioApiException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(WEBHOOK_API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                RachioApiWebHookList webhookList = gson.fromJson(response.body(), RachioApiWebHookList.class);
                if (webhookList != null && webhookList.data != null) {
                    return new ArrayList<>(webhookList.data);
                }
            } else {
                logger.error("Failed to get webhooks: {} - {}", 
                        response.statusCode(), response.body());
                throw new RachioApiException("Failed to get webhooks: " + response.statusCode());
            }
            
        } catch (IOException | InterruptedException e) {
            logger.error("Error getting webhooks: {}", e.getMessage(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RachioApiException("Error getting webhooks", e);
        }
        
        return new ArrayList<>();
    }

    /**
     * Check if webhook is currently registered
     */
    public boolean isWebhookRegistered() {
        if (currentWebhookId == null) {
            return false;
        }
        
        try {
            List<RachioApiWebHookEntry> webhooks = getWebhooks();
            String callbackUrl = getCallbackUrl();
            
            for (RachioApiWebHookEntry webhook : webhooks) {
                if (webhook.id != null && webhook.id.equals(currentWebhookId) &&
                    webhook.url != null && webhook.url.equals(callbackUrl)) {
                    return true;
                }
            }
            
            // Webhook ID not found in list, clear it
            currentWebhookId = null;
            registeredWebhooks.clear();
            logger.warn("Webhook {} not found in registered webhooks list", currentWebhookId);
            
        } catch (RachioApiException e) {
            logger.error("Error checking webhook registration: {}", e.getMessage());
        }
        
        return false;
    }

    /**
     * Get the callback URL for webhooks
     */
    public String getCallbackUrl() {
        // Ensure URL ends with webhook path
        String baseUrl = externalUrl;
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        
        // Remove any existing webhook path to avoid duplication
        baseUrl = baseUrl.replace("/rachio/webhook", "");
        baseUrl = baseUrl.replace("/rachio/callback", "");
        
        return baseUrl + "rachio/webhook";
    }

    /**
     * Get comma-separated list of event types to subscribe to
     */
    private String getEventTypes() {
        // Subscribe to all relevant event types
        List<String> eventTypes = new ArrayList<>();
        
        // Device events
        eventTypes.add("\"DEVICE_STATUS_EVENT_ONLINE\"");
        eventTypes.add("\"DEVICE_STATUS_EVENT_OFFLINE\"");
        eventTypes.add("\"DEVICE_STATUS_EVENT_SLEEP\"");
        
        // Zone events
        eventTypes.add("\"ZONE_STATUS_EVENT_STARTED\"");
        eventTypes.add("\"ZONE_STATUS_EVENT_STOPPED\"");
        eventTypes.add("\"ZONE_STATUS_EVENT_COMPLETED\"");
        
        // Schedule events
        eventTypes.add("\"SCHEDULE_STATUS_EVENT_COMPLETED\"");
        eventTypes.add("\"SCHEDULE_STATUS_EVENT_STARTED\"");
        eventTypes.add("\"SCHEDULE_STATUS_EVENT_SKIPPED\"");
        
        // Weather and rain events
        eventTypes.add("\"RAIN_DELAY_EVENT\"");
        eventTypes.add("\"WEATHER_INTELLIGENCE_SKIP_EVENT\"");
        eventTypes.add("\"RAIN_SENSOR_DETECTION_EVENT\"");
        
        // Water budget events
        eventTypes.add("\"WATER_BUDGET_EVENT\"");
        
        // Join with commas
        return String.join(",", eventTypes);
    }

    /**
     * Get the webhook secret for signature validation
     */
    public String getWebhookSecret() {
        return webhookSecret;
    }

    /**
     * Get the bridge handler
     */
    public RachioBridgeHandler getBridgeHandler() {
        return bridgeHandler;
    }

    /**
     * Get current webhook ID
     */
    public @Nullable String getCurrentWebhookId() {
        return currentWebhookId;
    }

    /**
     * Get number of registered webhooks
     */
    public int getRegisteredWebhookCount() {
        return registeredWebhooks.size();
    }

    /**
     * Clear all webhook registrations for this service
     */
    public void clearAllWebhooks() {
        try {
            List<RachioApiWebHookEntry> webhooks = getWebhooks();
            String callbackUrl = getCallbackUrl();
            
            for (RachioApiWebHookEntry webhook : webhooks) {
                if (webhook.url != null && webhook.url.equals(callbackUrl) && webhook.id != null) {
                    // Unregister this webhook
                    String url = WEBHOOK_API_URL + "/" + webhook.id;
                    
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Authorization", "Bearer " + apiKey)
                            .header("User-Agent", USER_AGENT)
                            .DELETE()
                            .build();
                    
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    logger.debug("Cleared webhook: {}", webhook.id);
                }
            }
            
            registeredWebhooks.clear();
            currentWebhookId = null;
            logger.info("Cleared all webhook registrations");
            
        } catch (Exception e) {
            logger.error("Error clearing webhooks: {}", e.getMessage(), e);
        }
    }

    /**
     * Validate that the webhook service is properly configured
     */
    public boolean validateConfiguration() {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("API key is not configured");
            return false;
        }
        
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            logger.error("Webhook secret is not configured");
            return false;
        }
        
        if (externalUrl == null || externalUrl.isEmpty()) {
            logger.error("External URL is not configured");
            return false;
        }
        
        // Validate URL format
        try {
            URI uri = URI.create(externalUrl);
            if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) {
                logger.error("External URL must use http or https scheme");
                return false;
            }
        } catch (Exception e) {
            logger.error("Invalid external URL format: {}", e.getMessage());
            return false;
        }
        
        return true;
    }

    /**
     * Update the external URL (for dynamic DNS scenarios)
     */
    public void updateExternalUrl(String newExternalUrl) {
        // Unregister old webhook if registered
        if (currentWebhookId != null) {
            unregisterWebhook();
        }
        
        // Update URL and re-register
        // Note: externalUrl is final, so we'd need to create a new instance
        // or make it mutable. For now, this is a placeholder.
        logger.info("External URL update requested to: {}", newExternalUrl);
    }
}
