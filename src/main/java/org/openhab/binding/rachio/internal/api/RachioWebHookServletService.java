package org.openhab.binding.rachio.internal.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The {@link RachioWebHookServletService} manages the webhook servlet for receiving
 * real-time events from Rachio with async processing and health monitoring.
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioWebHookServletService {

    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServletService.class);
    private final int port;
    private final RachioBridgeHandler bridgeHandler;
    private final Gson gson;
    private final ExecutorService executorService;
    
    private @Nullable org.eclipse.jetty.servlet.ServletContextHandler contextHandler;
    private @Nullable org.eclipse.jetty.server.Server server;
    private volatile boolean isRunning = false;
    
    // Health monitoring
    private long lastEventReceived = 0;
    private long totalEventsReceived = 0;
    private long totalEventsProcessed = 0;

    // FIXED: Removed @Nullable annotations from constructor parameters
    public RachioWebHookServletService(int port, RachioBridgeHandler bridgeHandler) {
        this.port = port;
        this.bridgeHandler = bridgeHandler;
        
        // Configure Gson
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Instant.class, new InstantTypeAdapter());
        this.gson = gsonBuilder.create();
        
        // Thread pool for async processing
        this.executorService = Executors.newFixedThreadPool(2);
        
        logger.debug("WebHookServletService initialized on port {}", port);
    }

    public void start() throws Exception {
        if (isRunning) {
            logger.warn("Webhook service already running on port {}", port);
            return;
        }
        
        try {
            server = new org.eclipse.jetty.server.Server(port);
            contextHandler = new org.eclipse.jetty.servlet.ServletContextHandler(org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS);
            contextHandler.setContextPath("/");
            
            // Register servlet
            contextHandler.addServlet(new org.eclipse.jetty.servlet.ServletHolder(new RachioWebHookServlet()), RachioBindingConstants.WEBHOOK_PATH);
            
            server.setHandler(contextHandler);
            server.start();
            
            isRunning = true;
            logger.info("Webhook service started on port {}", port);
            
            // Register webhook with Rachio API
            registerWebhookWithRachio();
            
        } catch (Exception e) {
            logger.error("Failed to start webhook service on port {}: {}", port, e.getMessage(), e);
            throw e;
        }
    }

    public void stop() {
        if (!isRunning) {
            return;
        }
        
        try {
            // Unregister webhook from Rachio API
            unregisterWebhookFromRachio();
            
            if (server != null) {
                server.stop();
                server = null;
            }
            
            if (contextHandler != null) {
                contextHandler.stop();
                contextHandler = null;
            }
            
            executorService.shutdown();
            
            isRunning = false;
            logger.info("Webhook service stopped");
            
        } catch (Exception e) {
            logger.error("Error stopping webhook service: {}", e.getMessage(), e);
        }
    }

    private void registerWebhookWithRachio() {
        try {
            String callbackUrl = getCallbackUrl();
            String externalId = "openhab-" + System.currentTimeMillis();
            
            RachioHttp rachioHttp = bridgeHandler.getRachioHttp();
            if (rachioHttp != null) {
                // Clear existing webhooks if configured
                if (bridgeHandler.getBridgeConfiguration().clearAllCallbacks != null && 
                    bridgeHandler.getBridgeConfiguration().clearAllCallbacks) {
                    rachioHttp.deleteAllWebhooks();
                    logger.debug("Cleared existing webhooks");
                }
                
                // Register new webhook
                boolean success = rachioHttp.registerWebhook(callbackUrl, externalId);
                if (success) {
                    logger.info("Registered webhook with Rachio: {}", callbackUrl);
                } else {
                    logger.warn("Failed to register webhook with Rachio");
                }
            }
            
        } catch (Exception e) {
            logger.error("Error registering webhook with Rachio: {}", e.getMessage(), e);
        }
    }

    private void unregisterWebhookFromRachio() {
        try {
            RachioHttp rachioHttp = bridgeHandler.getRachioHttp();
            if (rachioHttp != null) {
                rachioHttp.deleteAllWebhooks();
                logger.debug("Unregistered webhooks from Rachio");
            }
        } catch (Exception e) {
            logger.error("Error unregistering webhook from Rachio: {}", e.getMessage(), e);
        }
    }

    private String getCallbackUrl() {
        String callbackUrl = bridgeHandler.getBridgeConfiguration().callbackUrl;
        if (callbackUrl != null && !callbackUrl.isEmpty()) {
            return callbackUrl;
        }
        
        // Auto-detect callback URL
        // In production, this would use network interfaces to determine external IP
        return "http://" + getLocalIp() + ":" + port + RachioBindingConstants.WEBHOOK_PATH;
    }

    private String getLocalIp() {
        try {
            // Simplified - in production, use proper network detection
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getPort() {
        return port;
    }

    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", isRunning);
        status.put("port", port);
        status.put("lastEventReceived", lastEventReceived > 0 ? Instant.ofEpochMilli(lastEventReceived).toString() : "never");
        status.put("totalEventsReceived", totalEventsReceived);
        status.put("totalEventsProcessed", totalEventsProcessed);
        status.put("callbackUrl", getCallbackUrl());
        return status;
    }

    // Inner Servlet class
    @NonNullByDefault
    private class RachioWebHookServlet extends HttpServlet {
        
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            totalEventsReceived++;
            lastEventReceived = System.currentTimeMillis();
            
            // Get client IP (with proxy support)
            String clientIp = req.getHeader("X-Forwarded-For");
            if (clientIp == null || clientIp.isEmpty()) {
                clientIp = req.getRemoteAddr();
            }
            
            // Get HMAC signature
            String signature = req.getHeader("X-Rachio-Signature");
            
            // Read request body
            StringBuilder payloadBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    payloadBuilder.append(line);
                }
            }
            
            String payload = payloadBuilder.toString();
            logger.debug("Received webhook from {}: {}", clientIp, payload.length() > 100 ? payload.substring(0, 100) + "..." : payload);
            
            // Validate HMAC signature (if security enabled)
            RachioSecurity security = bridgeHandler.getSecurity();
            if (security != null) {
                String apiKey = bridgeHandler.getBridgeConfiguration().apiKey;
                if (!security.validateHmacSignature(payload, signature, apiKey)) {
                    logger.warn("Invalid HMAC signature from {}", clientIp);
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
                    return;
                }
            }
            
            // Validate IP (if IP filtering enabled)
            if (security != null && bridgeHandler.getBridgeConfiguration().ipFilter != null) {
                // Note: Fixed method call - use bridgeHandler as parameter
                if (!security.isIpAllowed(clientIp, bridgeHandler)) {
                    logger.warn("IP denied: {}", clientIp);
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "IP not allowed");
                    return;
                }
            }
            
            // Process event asynchronously
            executorService.submit(() -> processWebhookEvent(payload));
            
            // Send immediate response
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"status\":\"received\"}");
            resp.getWriter().flush();
            
            logger.debug("Webhook processed successfully");
        }
        
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Health check endpoint
            Map<String, Object> health = getHealthStatus();
            resp.setContentType("application/json");
            resp.getWriter().write(gson.toJson(health));
            resp.getWriter().flush();
        }
        
        private void processWebhookEvent(String payload) {
            try {
                totalEventsProcessed++;
                
                // Parse the webhook event as a Map instead of specific DTO
                Map<String, Object> eventData = gson.fromJson(payload, Map.class);
                if (eventData == null) {
                    logger.warn("Failed to parse webhook event");
                    return;
                }
                
                // Extract event data
                String eventType = (String) eventData.get("eventType");
                String deviceId = (String) eventData.get("deviceId");
                String zoneId = (String) eventData.get("zoneId");
                
                if (eventType == null || deviceId == null) {
                    logger.warn("Invalid webhook event: missing required fields");
                    return;
                }
                
                logger.debug("Processing webhook event: type={}, deviceId={}", eventType, deviceId);
                
                // Forward to bridge handler
                bridgeHandler.handleWebhookEvent(eventType, deviceId, zoneId, eventData);
                
                logger.debug("Webhook event processed successfully");
                
            } catch (Exception e) {
                logger.error("Error processing webhook event: {}", e.getMessage(), e);
            }
        }
    }
}
