package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.binding.rachio.internal.handler.RachioHandler;
import org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Servlet for handling Rachio webhook calls with full security
 * Now includes IP filtering, HMAC validation, and bridge integration
 */
@Component(service = HttpServlet.class, property = {"alias=/rachio/webhook", "servlet-name=RachioWebHookServlet"})
@NonNullByDefault
public class RachioWebHookServlet extends HttpServlet {
    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServlet.class);
    private final Gson gson = new Gson();
    private final Set<RachioHandler> handlers = ConcurrentHashMap.newKeySet();
    private final Set<RachioBridgeHandler> bridgeHandlers = ConcurrentHashMap.newKeySet();

    @Activate
    public RachioWebHookServlet() {
        logger.debug("RachioWebHookServlet activated with security features");
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addRachioHandler(RachioHandler handler) {
        handlers.add(handler);
        logger.debug("RachioHandler registered: {}", handler.getThing().getUID());
    }

    public void removeRachioHandler(RachioHandler handler) {
        handlers.remove(handler);
        logger.debug("RachioHandler unregistered: {}", handler.getThing().getUID());
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addRachioBridgeHandler(RachioBridgeHandler bridgeHandler) {
        bridgeHandlers.add(bridgeHandler);
        logger.debug("RachioBridgeHandler registered: {}", bridgeHandler.getThing().getUID());
    }

    public void removeRachioBridgeHandler(RachioBridgeHandler bridgeHandler) {
        bridgeHandlers.remove(bridgeHandler);
        logger.debug("RachioBridgeHandler unregistered: {}", bridgeHandler.getThing().getUID());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String clientIp = getClientIp(req);
        String payload = null;
        
        try {
            // Read the payload
            payload = req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);
            
            if (payload == null || payload.trim().isEmpty()) {
                logger.warn("Empty webhook payload from IP: {}", clientIp);
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Empty webhook payload");
                return;
            }
            
            logger.debug("Received webhook from {}: {} bytes", clientIp, payload.length());
            
            // ===== SECURITY CHECKS =====
            
            // 1. Validate webhook signature (HMAC)
            if (!validateWebhookSignature(req, payload)) {
                logger.error("WEBHOOK SECURITY VIOLATION: Invalid HMAC signature from IP: {}", clientIp);
                sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid webhook signature");
                return;
            }
            
            // 2. Parse the event to get device ID for bridge lookup
            RachioWebhookEvent event = parseWebhookEvent(payload);
            if (event == null || event.deviceId == null || event.deviceId.isEmpty()) {
                logger.warn("Invalid webhook payload or missing device ID from IP: {}", clientIp);
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid webhook payload - missing device ID");
                return;
            }
            
            // 3. Find the appropriate bridge handler for this device
            RachioBridgeHandler bridgeHandler = findBridgeHandlerForDevice(event.deviceId);
            if (bridgeHandler == null) {
                logger.warn("No bridge handler found for device: {} from IP: {}", event.deviceId, clientIp);
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "No bridge found for device: " + event.deviceId);
                return;
            }
            
            // 4. Validate client IP with bridge security
            if (!bridgeHandler.isIpAllowed(clientIp)) {
                logger.error("WEBHOOK SECURITY VIOLATION: IP {} blocked by bridge security for device: {}", 
                           clientIp, event.deviceId);
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "IP address not authorized");
                return;
            }
            
            // ===== PROCESS WEBHOOK =====
            
            logger.info("Processing webhook from {}: {} for device: {}", 
                       clientIp, event.eventType, event.deviceId);
            
            // Forward to bridge handler (which will forward to device/zone handlers)
            bridgeHandler.webHookEvent(clientIp, event);
            
            // Send success response
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain");
            resp.getWriter().write("Webhook processed successfully");
            resp.getWriter().flush();
            
            logger.debug("Webhook processed successfully for device: {}", event.deviceId);
            
        } catch (Exception e) {
            logger.error("Error processing webhook from {}: {}", clientIp, e.getMessage(), e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                     "Error processing webhook: " + e.getMessage());
        }
    }
    
    /**
     * Validate webhook HMAC signature
     */
    private boolean validateWebhookSignature(HttpServletRequest req, String payload) {
        try {
            // Get webhook ID from header (Rachio sends this)
            String webhookId = req.getHeader("X-Rachio-Webhook-ID");
            if (webhookId == null || webhookId.trim().isEmpty()) {
                logger.warn("Missing X-Rachio-Webhook-ID header");
                return false; // Can't validate without webhook ID
            }
            
            // Get signature from header
            String receivedSignature = req.getHeader("X-Rachio-Signature");
            if (receivedSignature == null || receivedSignature.trim().isEmpty()) {
                logger.warn("Missing X-Rachio-Signature header");
                return false; // Can't validate without signature
            }
            
            logger.debug("Validating signature for webhook ID: {}", webhookId);
            
            // Check each bridge handler's RachioHttp instance for validation
            for (RachioBridgeHandler bridgeHandler : bridgeHandlers) {
                try {
                    RachioHttp rachioHttp = bridgeHandler.getApi();
                    if (rachioHttp != null && rachioHttp.validateWebhookSignature(payload, receivedSignature, webhookId)) {
                        logger.debug("Signature validated successfully by bridge: {}", 
                                   bridgeHandler.getThing().getUID());
                        return true;
                    }
                } catch (Exception e) {
                    logger.debug("Bridge {} failed to validate signature: {}", 
                               bridgeHandler.getThing().getUID(), e.getMessage());
                    // Try next bridge
                }
            }
            
            logger.warn("No bridge could validate webhook signature for ID: {}", webhookId);
            return false;
            
        } catch (Exception e) {
            logger.error("Error during signature validation: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Find bridge handler that manages the specified device
     */
    @Nullable
    private RachioBridgeHandler findBridgeHandlerForDevice(String deviceId) {
        for (RachioBridgeHandler bridgeHandler : bridgeHandlers) {
            try {
                // Check if this bridge has the device
                List<org.openhab.binding.rachio.internal.api.dto.RachioPerson.Device> devices = 
                    bridgeHandler.getDevices();
                if (devices != null) {
                    for (org.openhab.binding.rachio.internal.api.dto.RachioPerson.Device device : devices) {
                        if (deviceId.equals(device.id)) {
                            logger.debug("Found bridge {} for device: {}", 
                                       bridgeHandler.getThing().getUID(), deviceId);
                            return bridgeHandler;
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Error checking bridge {} for device {}: {}", 
                           bridgeHandler.getThing().getUID(), deviceId, e.getMessage());
            }
        }
        
        // If not found by device list, try any bridge (fallback for initial setup)
        if (!bridgeHandlers.isEmpty()) {
            RachioBridgeHandler firstBridge = bridgeHandlers.iterator().next();
            logger.debug("Using first available bridge {} for device: {}", 
                       firstBridge.getThing().getUID(), deviceId);
            return firstBridge;
        }
        
        return null;
    }
    
    /**
     * Parse webhook payload into RachioWebhookEvent
     */
    @Nullable
    private RachioWebhookEvent parseWebhookEvent(String payload) {
        try {
            RachioWebhookEvent event = gson.fromJson(payload, RachioWebhookEvent.class);
            if (event != null) {
                logger.debug("Parsed webhook event: {} for device: {}", event.eventType, event.deviceId);
                
                // Log additional event details for debugging
                if (event.zone != null) {
                    logger.debug("Zone info: {} ({}), duration: {}s, status: {}", 
                               event.zone.name, event.zone.zoneNumber, 
                               event.zone.duration, event.zone.status);
                }
                if (event.device != null) {
                    logger.debug("Device info: {}, status: {}, on: {}", 
                               event.device.name, event.device.status, event.device.on);
                }
            }
            return event;
        } catch (JsonSyntaxException e) {
            logger.warn("Failed to parse webhook payload: {}", e.getMessage());
            logger.debug("Malformed payload: {}", payload.length() > 500 ? 
                       payload.substring(0, 500) + "..." : payload);
            return null;
        }
    }
    
    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest req) {
        // Try X-Forwarded-For header first (for proxies)
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getRemoteAddr();
        }
        
        // Handle multiple IPs in X-Forwarded-For
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip != null ? ip : "unknown";
    }
    
    /**
     * Send error response
     */
    private void sendError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        if (!resp.isCommitted()) {
            resp.setStatus(statusCode);
            resp.setContentType("text/plain");
            resp.getWriter().write(message);
            resp.getWriter().flush();
        }
    }

    /**
     * Get the number of registered handlers (for debugging)
     */
    public int getHandlerCount() {
        return handlers.size();
    }

    /**
     * Get the number of registered bridge handlers (for debugging)
     */
    public int getBridgeHandlerCount() {
        return bridgeHandlers.size();
    }

    /**
     * Health check endpoint with security info
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.setStatus(HttpServletResponse.SC_OK);
        
        StringBuilder info = new StringBuilder();
        info.append("Rachio Webhook Servlet Status\n");
        info.append("=============================\n");
        info.append("Active: Yes\n");
        info.append("Endpoint: /rachio/webhook\n");
        info.append("Registered handlers: ").append(getHandlerCount()).append("\n");
        info.append("Registered bridges: ").append(getBridgeHandlerCount()).append("\n");
        info.append("\nSecurity Features:\n");
        info.append("- IP Filtering: Enabled (via bridge configuration)\n");
        info.append("- HMAC Validation: Enabled\n");
        info.append("- Bridge Integration: Enabled\n");
        
        // List registered bridges
        if (!bridgeHandlers.isEmpty()) {
            info.append("\nRegistered Bridges:\n");
            for (RachioBridgeHandler bridge : bridgeHandlers) {
                info.append("- ").append(bridge.getThing().getUID()).append("\n");
            }
        }
        
        resp.getWriter().write(info.toString());
        resp.getWriter().flush();
    }
    
    /**
     * Test endpoint for debugging (can be removed in production)
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // This is a test endpoint that simulates a webhook for debugging
        // It bypasses security for testing purposes only
        
        String testPayload = "{\n" +
            "  \"eventType\": \"ZONE_STARTED\",\n" +
            "  \"deviceId\": \"test-device-123\",\n" +
            "  \"timestamp\": \"2024-01-01T12:00:00Z\",\n" +
            "  \"summary\": \"Test zone started\",\n" +
            "  \"device\": {\n" +
            "    \"id\": \"test-device-123\",\n" +
            "    \"name\": \"Test Controller\",\n" +
            "    \"on\": true,\n" +
            "    \"status\": \"ONLINE\"\n" +
            "  },\n" +
            "  \"zone\": {\n" +
            "    \"id\": \"test-zone-456\",\n" +
            "    \"name\": \"Test Zone\",\n" +
            "    \"zoneNumber\": 1,\n" +
            "    \"duration\": 300,\n" +
            "    \"status\": \"RUNNING\"\n" +
            "  }\n" +
            "}";
        
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write("{\"status\":\"test\",\"payload\":" + testPayload + "}");
        resp.getWriter().flush();
        
        logger.info("Test endpoint called - returned sample webhook payload");
    }
}