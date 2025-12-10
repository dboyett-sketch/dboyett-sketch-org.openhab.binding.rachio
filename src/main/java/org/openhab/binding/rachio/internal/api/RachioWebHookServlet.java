package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Servlet for handling Rachio webhook events
 */
@NonNullByDefault
@Component(service = javax.servlet.Servlet.class, property = "alias=/rachio/webhook")
public class RachioWebHookServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServlet.class);
    
    private final ThingRegistry thingRegistry;
    
    @Activate
    public RachioWebHookServlet(@Reference ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.debug("Received webhook request");
        
        // Get bridge UID from request
        String bridgeUID = req.getParameter("bridgeUID");
        if (bridgeUID == null || bridgeUID.isEmpty()) {
            logger.warn("Webhook request missing bridgeUID parameter");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        // Get the bridge handler
        RachioBridgeHandler bridgeHandler = getBridgeHandler(bridgeUID);
        if (bridgeHandler == null) {
            logger.warn("No bridge handler found for UID: {}", bridgeUID);
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // Validate IP if configured
        String remoteAddr = getRemoteAddress(req);
        if (!bridgeHandler.isIpAllowed(remoteAddr, req.getRequestURI())) {
            logger.warn("Webhook request from unauthorized IP: {}", remoteAddr);
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        // Parse the webhook event
        try {
            String body = req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);
            logger.debug("Webhook body: {}", body);
            
            Gson gson = bridgeHandler.getGson();
            if (gson == null) {
                logger.error("No Gson instance available from bridge handler");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
            
            RachioWebHookEvent event = gson.fromJson(body, RachioWebHookEvent.class);
            if (event == null) {
                logger.warn("Failed to parse webhook event");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            // FIXED: Call the correct method signature
            bridgeHandler.handleWebhookEvent(
                event.getId() != null ? event.getId() : "",
                event.getType() != null ? event.getType() : "",
                event.getSubType(),
                event.getData()
            );
            
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            logger.error("Error processing webhook", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    private @Nullable RachioBridgeHandler getBridgeHandler(String bridgeUID) {
        try {
            Thing thing = thingRegistry.get(new ThingUID(bridgeUID));
            if (thing != null && thing.getHandler() instanceof RachioBridgeHandler) {
                return (RachioBridgeHandler) thing.getHandler();
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid bridge UID: {}", bridgeUID, e);
        }
        return null;
    }
    
    private String getRemoteAddress(HttpServletRequest req) {
        String forwardedFor = req.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
    
    // FIXED: Add missing method for line 94
    private String getEventType(RachioWebHookEvent event) {
        return event.getType() != null ? event.getType() : "";
    }
}
