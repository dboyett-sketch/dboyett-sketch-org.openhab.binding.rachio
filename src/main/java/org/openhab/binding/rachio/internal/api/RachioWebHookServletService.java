package org.openhab.binding.rachio.internal.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.binding.rachio.internal.api.dto.RachioWebHookEvent;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

/**
 * The {@link RachioWebHookServletService} handles webhook callbacks from Rachio
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
@Component(service = RachioWebHookServletService.class)
public class RachioWebHookServletService extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServletService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Headers expected from Rachio webhooks
    private static final String WEBHOOK_SIGNATURE_HEADER = "X-Rachio-Signature";
    private static final String WEBHOOK_EVENT_HEADER = "X-Rachio-Event";
    private static final String WEBHOOK_DELIVERY_HEADER = "X-Rachio-Delivery";

    private final ConcurrentMap<String, RachioBridgeHandler> bridgeHandlers = new ConcurrentHashMap<>();
    private final ThingRegistry thingRegistry;

    @Activate
    public RachioWebHookServletService(@Reference ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
        logger.info("RachioWebHookServletService initialized");
    }

    @Override
    @Deactivate
    public void destroy() {
        bridgeHandlers.clear();
        logger.debug("RachioWebHookServletService destroyed");
        super.destroy();
    }

    /**
     * Register a bridge handler for webhook processing
     */
    public void registerBridgeHandler(RachioBridgeHandler handler) {
        if (handler != null && handler.getThing().getUID() != null) {
            String bridgeId = handler.getThing().getUID().getId();
            bridgeHandlers.put(bridgeId, handler);
            logger.debug("Registered bridge handler for webhooks: {}", bridgeId);
        }
    }

    /**
     * Unregister a bridge handler
     */
    public void unregisterBridgeHandler(RachioBridgeHandler handler) {
        if (handler != null && handler.getThing().getUID() != null) {
            String bridgeId = handler.getThing().getUID().getId();
            bridgeHandlers.remove(bridgeId);
            logger.debug("Unregistered bridge handler from webhooks: {}", bridgeId);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.debug("Received POST request at Rachio webhook endpoint");

        // 1. Extract and validate headers
        String signature = req.getHeader(WEBHOOK_SIGNATURE_HEADER);
        String eventType = req.getHeader(WEBHOOK_EVENT_HEADER);
        String deliveryId = req.getHeader(WEBHOOK_DELIVERY_HEADER);

        if (signature == null || signature.isEmpty()) {
            logger.warn("Webhook request missing required header: {}", WEBHOOK_SIGNATURE_HEADER);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing signature header");
            return;
        }
        if (eventType == null || eventType.isEmpty()) {
            logger.warn("Webhook request missing required header: {}", WEBHOOK_EVENT_HEADER);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing event type header");
            return;
        }

        // 2. Read the request body
        String requestBody = readRequestBody(req);
        if (requestBody == null || requestBody.isEmpty()) {
            logger.warn("Webhook request has empty body");
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Empty request body");
            return;
        }

        logger.trace("Webhook received - Event: {}, Delivery: {}, Body: {}", eventType, deliveryId, requestBody);

        // 3. Parse the JSON body into a RachioWebHookEvent DTO
        RachioWebHookEvent event;
        try {
            event = objectMapper.readValue(requestBody, RachioWebHookEvent.class);
            event.setEventType(eventType); // Ensure the header type is set on the object
        } catch (Exception e) {
            logger.error("Failed to parse webhook JSON body: {}", e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON payload");
            return;
        }

        // 4. Find the appropriate bridge handler for this event
        RachioBridgeHandler targetHandler = findTargetBridgeHandler(event);
        if (targetHandler == null) {
            logger.warn("No online bridge handler found for webhook event: {}", eventType);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No matching bridge found");
            return;
        }

        // 5. Validate the webhook signature (HMAC)
        String webhookSecret = targetHandler.getWebhookSecret();
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            logger.warn("Webhook secret not configured for bridge: {}. Cannot validate signature.", targetHandler.getThing().getUID());
            // Depending on your security policy, you might reject the request here.
            // resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Webhook secret not configured");
            // return;
        } else {
            // Use your RachioSecurity class to validate the HMAC signature
            if (!RachioSecurity.validateWebhookSignature(requestBody, signature, webhookSecret)) {
                logger.warn("Invalid HMAC signature for webhook. Event: {}, Bridge: {}", eventType, targetHandler.getThing().getUID());
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
                return;
            }
            logger.debug("Webhook HMAC signature validated successfully.");
        }

        // 6. Process the validated event
        try {
            targetHandler.handleWebhookEvent(event);
            logger.info("Successfully processed webhook event: {} (Delivery: {}) for bridge: {}", eventType, deliveryId, targetHandler.getThing().getUID());
        } catch (Exception e) {
            logger.error("Bridge handler failed to process webhook event: {}", eventType, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Event processing failed");
            return;
        }

        // 7. Send success response
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain");
        resp.getWriter().write("Webhook received successfully");
        logger.debug("Webhook response sent.");
    }

    /**
     * Read the entire request body as a String.
     */
    private @Nullable String readRequestBody(HttpServletRequest req) throws IOException {
        StringBuilder buffer = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        }
        return buffer.toString();
    }

    /**
     * Find the target bridge handler for a given webhook event.
     * Logic: First, try to find a bridge that matches the device/zone in the event.
     * If none found, return the first registered (and online) bridge handler.
     */
    private @Nullable RachioBridgeHandler findTargetBridgeHandler(RachioWebHookEvent event) {
        // Priority 1: Find a bridge by the event's device ID
        if (event.getDeviceId() != null && !event.getDeviceId().isEmpty()) {
            for (RachioBridgeHandler handler : bridgeHandlers.values()) {
                // This is a simplistic check. A more robust implementation might require
                // the bridge to know which device IDs it manages.
                if (handler.getThing().getStatus() == ThingStatus.ONLINE) {
                    // For now, assume the first online bridge can handle it.
                    // You could enhance this by checking the bridge's API client cache.
                    logger.debug("Routing event for device {} to online bridge: {}", event.getDeviceId(), handler.getThing().getUID());
                    return handler;
                }
            }
        }

        // Priority 2: Find any online bridge from our registered map
        for (RachioBridgeHandler handler : bridgeHandlers.values()) {
            if (handler.getThing().getStatus() == ThingStatus.ONLINE) {
                logger.debug("Routing event to generic online bridge: {}", handler.getThing().getUID());
                return handler;
            }
        }

        // Priority 3: Fallback to the ThingRegistry to find any Rachio bridge
        logger.debug("No bridge found in local map, searching ThingRegistry...");
        Collection<Thing> things = thingRegistry.getAll();
        for (Thing thing : things) {
            if (THING_TYPE_BRIDGE.equals(thing.getThingTypeUID()) && thing.getStatus() == ThingStatus.ONLINE) {
                if (thing.getHandler() instanceof RachioBridgeHandler) {
                    RachioBridgeHandler handler = (RachioBridgeHandler) thing.getHandler();
                    logger.debug("Found online bridge via ThingRegistry: {}", handler.getThing().getUID());
                    // Add it to our local map for future use
                    bridgeHandlers.put(handler.getThing().getUID().getId(), handler);
                    return handler;
                }
            }
        }

        logger.warn("No online Rachio bridge handler found to process webhook.");
        return null;
    }

    /**
     * Get all registered bridge handlers (primarily for debugging).
     */
    public ConcurrentMap<String, RachioBridgeHandler> getBridgeHandlers() {
        return new ConcurrentHashMap<>(bridgeHandlers);
    }
}
