package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.core.common.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Service for managing Rachio webhook registration and health checks
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioWebHookServletService {
    private static final String WEBHOOK_DESCRIPTION = "OpenHAB Rachio Binding";
    private static final int WEBHOOK_HEALTH_CHECK_INTERVAL = 3600; // 1 hour in seconds

    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServletService.class);
    private final RachioBridgeHandler bridgeHandler;
    private final Gson gson;
    
    private @Nullable ScheduledFuture<?> healthCheckFuture;
    private @Nullable String registeredWebhookId;
    private boolean webhookRegistered = false;

    public RachioWebHookServletService(RachioBridgeHandler bridgeHandler, Gson gson) {
        this.bridgeHandler = bridgeHandler;
        this.gson = gson;
    }

    /**
     * Initialize the webhook service
     */
    public void initialize() {
        logger.debug("Initializing Rachio webhook service");
        scheduleHealthCheck();
    }

    /**
     * Dispose of the webhook service
     */
    public void dispose() {
        logger.debug("Disposing Rachio webhook service");
        
        // Cancel health check
        ScheduledFuture<?> healthCheckFuture = this.healthCheckFuture;
        if (healthCheckFuture != null && !healthCheckFuture.isCancelled()) {
            healthCheckFuture.cancel(true);
            this.healthCheckFuture = null;
        }
        
        // Unregister webhook if registered
        if (webhookRegistered) {
            unregisterWebhook();
        }
    }

    /**
     * Register a webhook with Rachio API
     * 
     * @param http The RachioHttp client
     * @param deviceId The device ID
     * @param webhookUrl The webhook URL
     * @param externalId An external ID for the webhook
     */
    public synchronized void registerWebhook(@Nullable RachioHttp http, String deviceId, String webhookUrl, String externalId) {
        if (http == null) {
            logger.warn("Cannot register webhook: HTTP client is null");
            return;
        }

        if (webhookRegistered) {
            logger.debug("Webhook already registered, skipping");
            return;
        }

        try {
            logger.debug("Registering webhook for device {} with URL: {}", deviceId, webhookUrl);
            
            // Create webhook registration request
            String jsonRequest = String.format(
                "{\"type\":\"WEBHOOK\",\"externalId\":\"%s\",\"url\":\"%s\",\"eventTypes\":[\"DEVICE_STATUS_EVENT\",\"ZONE_STATUS_EVENT\",\"RAIN_DELAY_EVENT\",\"WEATHER_INTELLIGENCE_EVENT\",\"WATER_BUDGET_EVENT\",\"SCHEDULE_STATUS_EVENT\",\"RAIN_SENSOR_DETECTION_EVENT\"],\"device\":[{\"id\":\"%s\"}]}",
                externalId, webhookUrl, deviceId
            );

            String response = http.executePost("/webhook", jsonRequest);
            
            if (response != null && !response.isEmpty()) {
                RachioApiWebHookEntry webhookEntry = gson.fromJson(response, RachioApiWebHookEntry.class);
                if (webhookEntry != null && webhookEntry.id != null) {
                    registeredWebhookId = webhookEntry.id;
                    webhookRegistered = true;
                    logger.info("Successfully registered webhook with ID: {}", registeredWebhookId);
                } else {
                    logger.warn("Failed to parse webhook registration response");
                }
            } else {
                logger.warn("Empty response from webhook registration");
            }
        } catch (IOException e) {
            logger.warn("Failed to register webhook: {}", e.getMessage(), e);
        } catch (JsonSyntaxException e) {
            logger.warn("Invalid JSON response from webhook registration: {}", e.getMessage(), e);
        }
    }

    /**
     * Unregister a webhook from Rachio API
     */
    public synchronized void unregisterWebhook() {
        if (!webhookRegistered || registeredWebhookId == null) {
            logger.debug("No webhook registered or ID missing");
            return;
        }

        try {
            RachioHttp http = bridgeHandler.getRachioHttp();
            if (http == null) {
                logger.warn("Cannot unregister webhook: HTTP client is null");
                return;
            }

            logger.debug("Unregistering webhook with ID: {}", registeredWebhookId);
            
            String response = http.executeDelete("/webhook/" + registeredWebhookId);
            
            if (response != null && !response.isEmpty()) {
                logger.info("Successfully unregistered webhook with ID: {}", registeredWebhookId);
                webhookRegistered = false;
                registeredWebhookId = null;
            } else {
                logger.warn("Empty response from webhook unregistration");
            }
        } catch (IOException e) {
            logger.warn("Failed to unregister webhook: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.warn("Error unregistering webhook: {}", e.getMessage(), e);
        }
    }

    /**
     * Check webhook health and re-register if needed
     * 
     * @param http The RachioHttp client
     */
    public synchronized void checkWebhookHealth(@Nullable RachioHttp http) {
        if (http == null) {
            logger.warn("Cannot check webhook health: HTTP client is null");
            return;
        }

        try {
            // List webhooks to check if ours is still registered
            String response = http.executeGet("/webhook");
            
            if (response != null && !response.isEmpty()) {
                RachioApiWebHookList webhookList = gson.fromJson(response, RachioApiWebHookList.class);
                
                boolean found = false;
                if (webhookList != null && webhookList.data != null) {
                    for (RachioApiWebHookEntry entry : webhookList.data) {
                        if (registeredWebhookId != null && registeredWebhookId.equals(entry.id)) {
                            found = true;
                            break;
                        }
                    }
                }
                
                if (!found) {
                    logger.warn("Registered webhook not found, attempting to re-register");
                    webhookRegistered = false;
                    registeredWebhookId = null;
                    
                    // Get current configuration
                    String deviceId = bridgeHandler.getDeviceId();
                    String webhookUrl = bridgeHandler.getWebhookUrl();
                    
                    if (deviceId != null && webhookUrl != null) {
                        registerWebhook(http, deviceId, webhookUrl, bridgeHandler.getThing().getUID().getId());
                    }
                } else {
                    logger.debug("Webhook health check passed");
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to check webhook health: {}", e.getMessage(), e);
        } catch (JsonSyntaxException e) {
            logger.warn("Invalid JSON response from webhook health check: {}", e.getMessage(), e);
        }
    }

    /**
     * Schedule periodic webhook health checks
     */
    private void scheduleHealthCheck() {
        healthCheckFuture = ThreadPoolManager.getScheduledPool("rachio-webhook-health")
            .scheduleWithFixedDelay(this::performHealthCheck, 
                WEBHOOK_HEALTH_CHECK_INTERVAL, 
                WEBHOOK_HEALTH_CHECK_INTERVAL, 
                TimeUnit.SECONDS);
        
        logger.debug("Scheduled webhook health checks every {} seconds", WEBHOOK_HEALTH_CHECK_INTERVAL);
    }

    /**
     * Perform a webhook health check
     */
    private void performHealthCheck() {
        try {
            RachioHttp http = bridgeHandler.getRachioHttp();
            if (http != null) {
                checkWebhookHealth(http);
            }
        } catch (Exception e) {
            logger.warn("Error performing webhook health check: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if a webhook is registered
     * 
     * @return true if webhook is registered
     */
    public boolean isWebhookRegistered() {
        return webhookRegistered;
    }

    /**
     * Get the registered webhook ID
     * 
     * @return the webhook ID or null if not registered
     */
    public @Nullable String getRegisteredWebhookId() {
        return registeredWebhookId;
    }
}
