package org.openhab.binding.rachio.internal.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.RachioWebHookEvent;
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

/**
 * Servlet for handling Rachio webhook events using OSGi HTTP Whiteboard pattern
 */
@NonNullByDefault
@Component(
    service = javax.servlet.Servlet.class,
    property = {
        "osgi.http.whiteboard.servlet.pattern=/rachio/webhook/*",
        "osgi.http.whiteboard.servlet.name=Rachio Webhook Servlet",
        "osgi.http.whiteboard.servlet.asyncSupported=true",
        "osgi.http.whiteboard.context.select=(osgi.http.whiteboard.context.name=openhab)"
    }
)
public class RachioWebHookServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServlet.class);

    private final ThingRegistry thingRegistry;

    @Activate
    public RachioWebHookServlet(@Reference ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
        logger.debug("RachioWebHookServlet activated with Whiteboard pattern");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        logger.debug("Received webhook request from: {}", getRemoteAddress(req));

        // Get bridge UID from path rather than parameter for better REST design
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            logger.warn("Webhook request missing bridge UID in path");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Missing bridge UID in path");
            return;
        }

        // Extract bridgeUID from path (e.g., /rachio/webhook/bridge:rachio:abc123)
        String bridgeUID = pathInfo.substring(1); // Remove leading slash
        if (bridgeUID.isEmpty()) {
            logger.warn("Webhook request has empty bridge UID in path");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Empty bridge UID in path");
            return;
        }

        // Get the bridge handler
        RachioBridgeHandler bridgeHandler = getBridgeHandler(bridgeUID);
        if (bridgeHandler == null) {
            logger.warn("No bridge handler found for UID: {}", bridgeUID);
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("Bridge not found: " + bridgeUID);
            return;
        }

        // Validate IP if configured
        String remoteAddr = getRemoteAddress(req);
        if (!bridgeHandler.isIpAllowed(remoteAddr, req.getRequestURI())) {
            logger.warn("Webhook request from unauthorized IP: {}", remoteAddr);
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("IP not authorized: " + remoteAddr);
            return;
        }

        // Parse the webhook event
        try {
            String body = req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);
            logger.trace("Webhook body: {}", body);

            Gson gson = bridgeHandler.getGson();
            if (gson == null) {
                logger.error("No Gson instance available from bridge handler");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Internal server error: Gson not available");
                return;
            }

            RachioWebHookEvent event = gson.fromJson(body, RachioWebHookEvent.class);
            if (event == null) {
                logger.warn("Failed to parse webhook event");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Failed to parse JSON event");
                return;
            }

            // Call the bridge handler with event data
            bridgeHandler.handleWebhookEvent(
                event.getId() != null ? event.getId() : "",
                event.getType() != null ? event.getType() : "",
                event.getSubType(),
                event.getData()
            );

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Webhook processed successfully");
        } catch (Exception e) {
            logger.error("Error processing webhook", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal server error: " + e.getMessage());
        }
    }

    private @Nullable RachioBridgeHandler getBridgeHandler(String bridgeUID) {
        try {
            Thing thing = thingRegistry.get(new ThingUID(bridgeUID));
            if (thing != null && thing.getHandler() instanceof RachioBridgeHandler) {
                return (RachioBridgeHandler) thing.getHandler();
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid bridge UID format: {}", bridgeUID, e);
        }
        return null;
    }

    private String getRemoteAddress(HttpServletRequest req) {
        // Support proxy headers for proper IP detection
        String forwardedFor = req.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            // Take the first IP in the chain (client's original IP)
            return forwardedFor.split(",")[0].trim();
        }
        
        // Fall back to standard remote address
        return req.getRemoteAddr();
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        // Provide a simple health/readiness check
        resp.setContentType("text/plain");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write("Rachio Webhook Servlet is active");
        logger.debug("Health check request from: {}", getRemoteAddress(req));
    }
}
