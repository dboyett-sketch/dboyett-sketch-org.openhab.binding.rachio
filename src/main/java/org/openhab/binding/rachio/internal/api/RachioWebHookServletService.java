package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.ThingHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openhab.binding.rachio.internal.api.dto.RachioWebHookEvent;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

/**
 * The {@link RachioWebHookServletService} handles webhook callbacks from Rachio
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
@Component(service = RachioWebHookServletService.class)
public class RachioWebHookServletService extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServletService.class);
    
    private final ConcurrentMap<String, RachioBridgeHandler> bridgeHandlers = new ConcurrentHashMap<>();
    private final HttpClientFactory httpClientFactory;
    private final ThingRegistry thingRegistry;
    
    private @Nullable RachioSecurity security;

    @Activate
    public RachioWebHookServletService(
            @Reference HttpClientFactory httpClientFactory, 
            @Reference ThingRegistry thingRegistry) {
        this.httpClientFactory = httpClientFactory;
        this.thingRegistry = thingRegistry;
        this.security = new RachioSecurity();
        logger.debug("RachioWebHookServletService initialized and registered as OSGi service");
    }

    /**
     * Register a bridge handler for webhook processing
     */
    public void registerBridgeHandler(RachioBridgeHandler handler) {
        if (handler != null && handler.getThing().getUID() != null) {
            String bridgeId = handler.getThing().getUID().getId();
            bridgeHandlers.put(bridgeId, handler);
            logger.debug("Registered bridge handler: {}", bridgeId);
        }
    }

    /**
     * Unregister a bridge handler
     */
    public void unregisterBridgeHandler(RachioBridgeHandler handler) {
        if (handler != null && handler.getThing().getUID() != null) {
            String bridgeId = handler.getThing().getUID().getId();
            bridgeHandlers.remove(bridgeId);
            logger.debug("Unregistered bridge handler: {}", bridgeId);
        }
    }

    /**
     * Get the bridge handler for a specific bridge ID
     */
    public @Nullable RachioBridgeHandler getBridgeHandler(String bridgeId) {
        return bridgeHandlers.get(bridgeId);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.debug("Received webhook callback");
        
        // Extract webhook signature for validation
        String signature = req.getHeader(WEBHOOK_SIGNATURE_HEADER);
        String eventType = req.getHeader(WEBHOOK_EVENT_HEADER);
        String deliveryId = req.getHeader(WEBHOOK_DELIVERY_HEADER);
        
        if (signature == null || signature.isEmpty()) {
            logger.warn("Webhook request missing signature header");
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing signature header");
            return;
        }
        
        if (eventType == null || eventType.isEmpty()) {
            logger.warn("Webhook request missing event type header");
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing event type header");
            return;
        }
        
        // Read request body for validation and processing
        String requestBody = readRequestBody(req);
        if (requestBody == null || requestBody.isEmpty()) {
            logger.warn("Webhook request has empty body");
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Empty request body");
            return;
        }
        
        // Find the appropriate bridge handler for this webhook
        RachioBridgeHandler targetHandler = findTargetBridgeHandler(requestBody, eventType);
        if (targetHandler == null) {
            logger.warn("No bridge handler found for webhook event: {}", eventType);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No matching bridge found");
            return;
        }
        
        // Validate webhook signature
        RachioSecurity localSecurity = security;
        if (localSecurity == null) {
            logger.error("Security component not initialized");
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Security error");
            return;
        }
        
        // Get webhook secret from bridge configuration
        String webhookSecret = targetHandler.getBridgeConfig() != null ? 
                targetHandler.getBridgeConfig().webhookSecret : null;
        
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            logger.warn("Webhook secret not configured for bridge: {}", targetHandler.getThing().getUID());
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Webhook secret not configured");
            return;
        }
        
        // Validate HMAC signature
        if (!localSecurity.validateWebhookSignature(requestBody, signature, webhookSecret)) {
            logger.warn("Invalid webhook signature for bridge: {}", targetHandler.getThing().getUID());
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
            return;
        }
        
        logger.debug("Webhook validation successful for event: {} (delivery: {})", eventType, deliveryId);
        
        // Process the webhook event
        processWebhookEvent(targetHandler, eventType, requestBody, deliveryId);
        
        // Send success response
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain");
        resp.getWriter().write("Webhook received successfully");
    }

    /**
     * Read the request body from HttpServletRequest
     */
    private @Nullable String readRequestBody(HttpServletRequest req) throws IOException {
        try {
            return req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);
        } catch (Exception e) {
            logger.error("Error reading request body", e);
            return null;
        }
    }

    /**
     * Find the target bridge handler for a webhook event
     */
    private @Nullable RachioBridgeHandler findTargetBridgeHandler(String requestBody, String eventType) {
        // Try to extract device/zone ID from request body to find the right bridge
        // This is a simplified implementation - in reality you'd parse the JSON
        logger.debug("Looking for bridge handler for event: {}", eventType);
        
        // Use the ThingRegistry to find bridges
        Collection<Thing> things = thingRegistry.getAll();
        for (Thing thing : things) {
            if (THING_TYPE_BRIDGE.equals(thing.getThingTypeUID())) {
                ThingHandler handler = thing.getHandler();
                if (handler instanceof RachioBridgeHandler) {
                    RachioBridgeHandler bridgeHandler = (RachioBridgeHandler) handler;
                    
                    // Check if this bridge is online and configured for webhooks
                    if (bridgeHandler.getThing().getStatus() == ThingStatus.ONLINE && 
                        bridgeHandler.isWebhookConfigured()) {
                        logger.debug("Found online bridge handler: {}", bridgeHandler.getThing().getUID());
                        return bridgeHandler;
                    }
                }
            }
        }
        
        // Fallback to first registered bridge handler
        if (!bridgeHandlers.isEmpty()) {
            return bridgeHandlers.values().iterator().next();
        }
        
        return null;
    }

    /**
     * Process a validated webhook event
     */
    private void processWebhookEvent(RachioBridgeHandler bridgeHandler, String eventType, 
                                     String requestBody, @Nullable String deliveryId) {
        logger.debug("Processing webhook event: {} for bridge: {}", 
                eventType, bridgeHandler.getThing().getUID());
        
        try {
            // Parse the webhook event based on event type
            RachioWebHookEvent event = RachioHttp.parseWebhookEvent(requestBody, eventType);
            if (event == null) {
                logger.warn("Failed to parse webhook event: {}", eventType);
                return;
            }
            
            // Update device/zone states based on event
            switch (eventType) {
                case EVENT_ZONE_STATUS:
                    handleZoneStatusEvent(bridgeHandler, event);
                    break;
                case EVENT_DEVICE_STATUS:
                    handleDeviceStatusEvent(bridgeHandler, event);
                    break;
                case EVENT_RAIN_DELAY:
                    handleRainDelayEvent(bridgeHandler, event);
                    break;
                case EVENT_WEATHER_INTEL:
                    handleWeatherIntelEvent(bridgeHandler, event);
                    break;
                case EVENT_WATER_BUDGET:
                    handleWaterBudgetEvent(bridgeHandler, event);
                    break;
                case EVENT_SCHEDULE_STATUS:
                    handleScheduleStatusEvent(bridgeHandler, event);
                    break;
                case EVENT_RAIN_SENSOR:
                    handleRainSensorEvent(bridgeHandler, event);
                    break;
                default:
                    logger.debug("Unhandled webhook event type: {}", eventType);
            }
            
            logger.info("Processed webhook event: {} (delivery: {})", eventType, deliveryId);
            
        } catch (Exception e) {
            logger.error("Error processing webhook event: {}", eventType, e);
        }
    }

    /**
     * Handle ZONE_STATUS events
     */
    private void handleZoneStatusEvent(RachioBridgeHandler bridgeHandler, RachioWebHookEvent event) {
        logger.debug("Handling ZONE_STATUS event: {}", event);
        // Update zone state in the appropriate zone handler
        // This would typically trigger a refresh of the zone data
        if (event.getZoneId() != null) {
            bridgeHandler.notifyStatusListeners(ThingStatus.ONLINE, ThingStatusDetail.NONE, 
                    "Zone status updated via webhook");
        }
    }

    /**
     * Handle DEVICE_STATUS events
     */
    private void handleDeviceStatusEvent(RachioBridgeHandler bridgeHandler, RachioWebHookEvent event) {
        logger.debug("Handling DEVICE_STATUS event: {}", event);
        // Update device state
        if (event.getDeviceId() != null) {
            bridgeHandler.notifyStatusListeners(ThingStatus.ONLINE, ThingStatusDetail.NONE, 
                    "Device status updated via webhook");
        }
    }

    /**
     * Handle RAIN_DELAY events
     */
    private void handleRainDelayEvent(RachioBridgeHandler bridgeHandler, RachioWebHookEvent event) {
        logger.debug("Handling RAIN_DELAY event: {}", event);
        // Update rain delay state
    }

    /**
     * Handle WEATHER_INTEL events
     */
    private void handleWeatherIntelEvent(RachioBridgeHandler bridgeHandler, RachioWebHookEvent event) {
        logger.debug("Handling WEATHER_INTEL event: {}", event);
        // Update weather intelligence data
    }

    /**
     * Handle WATER_BUDGET events
     */
    private void handleWaterBudgetEvent(RachioBridgeHandler bridgeHandler, RachioWebHookEvent event) {
        logger.debug("Handling WATER_BUDGET event: {}", event);
        // Update water budget
    }

    /**
     * Handle SCHEDULE_STATUS events
     */
    private void handleScheduleStatusEvent(RachioBridgeHandler bridgeHandler, RachioWebHookEvent event) {
        logger.debug("Handling SCHEDULE_STATUS event: {}", event);
        // Update schedule status
    }

    /**
     * Handle RAIN_SENSOR events
     */
    private void handleRainSensorEvent(RachioBridgeHandler bridgeHandler, RachioWebHookEvent event) {
        logger.debug("Handling RAIN_SENSOR event: {}", event);
        // Update rain sensor status
    }

    /**
     * Get all registered bridge handlers (for debugging)
     */
    public ConcurrentMap<String, RachioBridgeHandler> getBridgeHandlers() {
        return new ConcurrentHashMap<>(bridgeHandlers);
    }

    /**
     * Get the webhook URL for a specific bridge
     */
    public @Nullable String getWebhookUrl(String bridgeId) {
        RachioBridgeHandler handler = bridgeHandlers.get(bridgeId);
        if (handler != null && handler.getBridgeConfig() != null) {
            return handler.getBridgeConfig().webhookUrl;
        }
        return null;
    }

    /**
     * Register webhook with Rachio API for a specific bridge
     */
    public boolean registerWebhook(RachioBridgeHandler bridgeHandler) {
        if (bridgeHandler.getHttpClient() == null || bridgeHandler.getBridgeConfig() == null) {
            logger.error("Cannot register webhook: HttpClient or configuration not available");
            return false;
        }
        
        String webhookUrl = bridgeHandler.getBridgeConfig().webhookUrl;
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            logger.error("Cannot register webhook: URL not configured");
            return false;
        }
        
        try {
            RachioApiWebHookList existingWebhooks = bridgeHandler.getHttpClient().getWebhooks();
            
            // Check if webhook already exists
            boolean webhookExists = false;
            if (existingWebhooks != null) {
                for (RachioApiWebHookEntry webhook : existingWebhooks.getData()) {
                    if (webhookUrl.equals(webhook.getUrl())) {
                        webhookExists = true;
                        logger.debug("Webhook already registered: {}", webhook.getId());
                        break;
                    }
                }
            }
            
            // Register new webhook if needed
            if (!webhookExists) {
                String externalId = "openhab-" + bridgeHandler.getThing().getUID().getId();
                RachioApiWebHookEntry newWebhook = bridgeHandler.getHttpClient().createWebhook(
                        webhookUrl, externalId, new String[] {
                            EVENT_ZONE_STATUS,
                            EVENT_DEVICE_STATUS,
                            EVENT_RAIN_DELAY,
                            EVENT_WEATHER_INTEL,
                            EVENT_WATER_BUDGET,
                            EVENT_SCHEDULE_STATUS,
                            EVENT_RAIN_SENSOR
                        });
                
                if (newWebhook != null && newWebhook.getId() != null) {
                    logger.info("Registered new webhook: {} for bridge: {}", 
                            newWebhook.getId(), bridgeHandler.getThing().getUID());
                    return true;
                } else {
                    logger.error("Failed to register webhook for bridge: {}", 
                            bridgeHandler.getThing().getUID());
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error registering webhook for bridge: {}", 
                    bridgeHandler.getThing().getUID(), e);
            return false;
        }
    }

    /**
     * Unregister webhook from Rachio API
     */
    public boolean unregisterWebhook(RachioBridgeHandler bridgeHandler) {
        if (bridgeHandler.getHttpClient() == null) {
            logger.error("Cannot unregister webhook: HttpClient not available");
            return false;
        }
        
        String webhookUrl = bridgeHandler.getBridgeConfig() != null ? 
                bridgeHandler.getBridgeConfig().webhookUrl : null;
        
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            logger.debug("No webhook URL configured for bridge: {}", bridgeHandler.getThing().getUID());
            return true; // Nothing to unregister
        }
        
        try {
            RachioApiWebHookList existingWebhooks = bridgeHandler.getHttpClient().getWebhooks();
            if (existingWebhooks != null) {
                for (RachioApiWebHookEntry webhook : existingWebhooks.getData()) {
                    if (webhookUrl.equals(webhook.getUrl())) {
                        boolean success = bridgeHandler.getHttpClient().deleteWebhook(webhook.getId());
                        if (success) {
                            logger.info("Unregistered webhook: {} for bridge: {}", 
                                    webhook.getId(), bridgeHandler.getThing().getUID());
                        } else {
                            logger.error("Failed to unregister webhook: {} for bridge: {}", 
                                    webhook.getId(), bridgeHandler.getThing().getUID());
                        }
                        return success;
                    }
                }
            }
            
            logger.debug("No webhook found for URL: {} (bridge: {})", 
                    webhookUrl, bridgeHandler.getThing().getUID());
            return true; // Webhook not found, nothing to do
            
        } catch (Exception e) {
            logger.error("Error unregistering webhook for bridge: {}", 
                    bridgeHandler.getThing().getUID(), e);
            return false;
        }
    }

    @Override
    public void destroy() {
        logger.debug("Destroying RachioWebHookServletService");
        bridgeHandlers.clear();
        security = null;
        super.destroy();
    }
}
