package org.openhab.binding.rachio.internal.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Servlet for receiving Rachio webhook callbacks
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioWebHookServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServlet.class);

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String HEADER_SIGNATURE = "X-RACHIO-SIGNATURE";
    private static final String HEADER_WEBHOOK_ID = "X-RACHIO-WEBHOOK-ID";
    private static final String HEADER_WEBHOOK_TIMESTAMP = "X-RACHIO-WEBHOOK-TIMESTAMP";
    
    private final RachioBridgeHandler bridgeHandler;
    private final RachioSecurity security;
    private final Gson gson;
    private final ExecutorService executorService;
    
    private @Nullable String webhookSecret;
    private boolean ipFilteringEnabled = true;
    private boolean requireValidSignature = true;

    /**
     * Constructor
     */
    public RachioWebHookServlet(RachioBridgeHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
        this.security = new RachioSecurity();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
        
        // Create a thread pool for async processing
        this.executorService = Executors.newFixedThreadPool(5);
        
        logger.debug("Rachio webhook servlet initialized");
    }

    /**
     * Initialize servlet with configuration
     */
    public void initialize(@Nullable String webhookSecret, boolean ipFilteringEnabled, boolean requireValidSignature) {
        this.webhookSecret = webhookSecret;
        this.ipFilteringEnabled = ipFilteringEnabled;
        this.requireValidSignature = requireValidSignature;
        
        logger.debug("Webhook servlet configured - IP filtering: {}, Signature validation: {}", 
                ipFilteringEnabled, requireValidSignature);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String clientIp = getClientIp(req);
        String webhookId = req.getHeader(HEADER_WEBHOOK_ID);
        String signature = req.getHeader(HEADER_SIGNATURE);
        String timestamp = req.getHeader(HEADER_WEBHOOK_TIMESTAMP);
        
        logger.debug("Webhook received from IP: {}, Webhook ID: {}", clientIp, webhookId);
        
        // Log headers for debugging
        if (logger.isTraceEnabled()) {
            logger.trace("Webhook headers:");
            req.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                logger.trace("  {}: {}", headerName, req.getHeader(headerName));
            });
        }
        
        // Step 1: Validate request
        if (!validateRequest(req, resp, clientIp, signature)) {
            return; // Response already sent
        }
        
        // Step 2: Read request body
        String payload = readRequestBody(req);
        if (payload == null || payload.isEmpty()) {
            logger.warn("Empty webhook payload received");
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Empty payload");
            return;
        }
        
        logger.trace("Webhook payload: {}", payload);
        
        // Step 3: Validate signature if required
        if (requireValidSignature && !validateSignature(payload, signature, timestamp)) {
            logger.warn("Invalid webhook signature from IP: {}", clientIp);
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
            return;
        }
        
        // Step 4: Parse and validate webhook event
        RachioWebhookEvent event;
        try {
            event = gson.fromJson(payload, RachioWebhookEvent.class);
            if (event == null || event.type == null || event.deviceId == null) {
                logger.warn("Invalid webhook event structure");
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid event structure");
                return;
            }
        } catch (JsonSyntaxException e) {
            logger.warn("Failed to parse webhook JSON: {}", e.getMessage());
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON");
            return;
        }
        
        // Step 5: Process event asynchronously
        executorService.submit(() -> processWebhookEvent(event, clientIp, webhookId));
        
        // Step 6: Send success response
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(CONTENT_TYPE_JSON);
        resp.getWriter().write("{\"status\":\"received\"}");
        resp.getWriter().flush();
        
        logger.debug("Webhook processed successfully - Event: {}, Device: {}", 
                event.type, event.deviceId);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        // GET requests can be used for health checks or webhook verification
        String clientIp = getClientIp(req);
        logger.debug("GET request received from IP: {}", clientIp);
        
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(CONTENT_TYPE_JSON);
        resp.getWriter().write("{\"status\":\"online\",\"service\":\"rachio-webhook\"}");
        resp.getWriter().flush();
    }

    @Override
    public void destroy() {
        // Shutdown executor service
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.debug("Rachio webhook servlet destroyed");
        super.destroy();
    }

    /**
     * Validate incoming request
     */
    private boolean validateRequest(HttpServletRequest req, HttpServletResponse resp, 
                                   String clientIp, @Nullable String signature) throws IOException {
        
        // Check content type
        String contentType = req.getContentType();
        if (contentType == null || !contentType.startsWith(CONTENT_TYPE_JSON)) {
            logger.warn("Invalid content type: {}", contentType);
            sendError(resp, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, 
                    "Content type must be " + CONTENT_TYPE_JSON);
            return false;
        }
        
        // IP filtering
        if (ipFilteringEnabled && !security.isAllowedIp(clientIp)) {
            logger.warn("IP address not allowed: {}", clientIp);
            sendError(resp, HttpServletResponse.SC_FORBIDDEN, "IP address not allowed");
            return false;
        }
        
        // Signature validation configuration check
        if (requireValidSignature && (signature == null || signature.isEmpty())) {
            logger.warn("Missing signature header");
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Missing signature");
            return false;
        }
        
        return true;
    }

    /**
     * Read request body as string
     */
    private @Nullable String readRequestBody(HttpServletRequest req) throws IOException {
        StringBuilder buffer = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IOException e) {
            logger.error("Error reading request body: {}", e.getMessage());
            throw e;
        }
        
        return buffer.toString();
    }

    /**
     * Validate webhook signature
     */
    private boolean validateSignature(String payload, @Nullable String signature, 
                                     @Nullable String timestamp) {
        if (signature == null || signature.isEmpty()) {
            logger.warn("Signature is null or empty");
            return false;
        }
        
        String localSecret = webhookSecret;
        if (localSecret == null || localSecret.isEmpty()) {
            logger.warn("Webhook secret not configured");
            return false;
        }
        
        try {
            // Rachio uses HMAC-SHA256 with payload + timestamp
            String dataToSign = payload;
            if (timestamp != null && !timestamp.isEmpty()) {
                dataToSign = timestamp + payload;
            }
            
            return security.validateWebhookSignature(dataToSign, signature, localSecret);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error validating signature: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error validating signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process webhook event asynchronously
     */
    private void processWebhookEvent(RachioWebhookEvent event, String clientIp, @Nullable String webhookId) {
        try {
            logger.info("Processing webhook event - Type: {}, Device: {}, Zone: {}", 
                    event.type, event.deviceId, event.zoneId != null ? event.zoneId : "N/A");
            
            // Log event details at debug level
            if (logger.isDebugEnabled()) {
                logger.debug("Event details: {}", gson.toJson(event));
            }
            
            // Update bridge handler with event
            bridgeHandler.handleWebhookEvent(event);
            
            // Log successful processing
            logger.debug("Webhook event processed successfully - ID: {}, From: {}", 
                    webhookId, clientIp);
            
        } catch (Exception e) {
            logger.error("Error processing webhook event: {}", e.getMessage(), e);
        }
    }

    /**
     * Get client IP address with proxy support
     */
    private String getClientIp(HttpServletRequest req) {
        // Check for X-Forwarded-For header (common with proxies)
        String xForwardedFor = req.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            String[] ips = xForwardedFor.split(",");
            return ips[0].trim();
        }
        
        // Fall back to remote address
        return req.getRemoteAddr();
    }

    /**
     * Send error response
     */
    private void sendError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType(CONTENT_TYPE_JSON);
        resp.getWriter().write(String.format("{\"error\":\"%s\"}", message));
        resp.getWriter().flush();
        
        logger.debug("Sent error response: {} - {}", statusCode, message);
    }

    /**
     * Update webhook secret
     */
    public void updateWebhookSecret(@Nullable String newSecret) {
        this.webhookSecret = newSecret;
        logger.debug("Webhook secret updated");
    }

    /**
     * Enable/disable IP filtering
     */
    public void setIpFilteringEnabled(boolean enabled) {
        this.ipFilteringEnabled = enabled;
        logger.debug("IP filtering {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Enable/disable signature validation
     */
    public void setRequireValidSignature(boolean required) {
        this.requireValidSignature = required;
        logger.debug("Signature validation {}", required ? "required" : "optional");
    }

    /**
     * Get current configuration
     */
    public String getConfiguration() {
        return String.format("IP filtering: %s, Signature validation: %s, Secret configured: %s",
                ipFilteringEnabled ? "enabled" : "disabled",
                requireValidSignature ? "required" : "optional",
                webhookSecret != null && !webhookSecret.isEmpty() ? "yes" : "no");
    }

    /**
     * Health check
     */
    public boolean isHealthy() {
        return bridgeHandler != null && 
               bridgeHandler.getThing().getStatus() == org.openhab.core.thing.ThingStatus.ONLINE;
    }

    /**
     * Get processing statistics
     */
    public String getStatistics() {
        return String.format("Thread pool: %d/%d threads, Bridge status: %s",
                ((java.util.concurrent.ThreadPoolExecutor) executorService).getActiveCount(),
                ((java.util.concurrent.ThreadPoolExecutor) executorService).getPoolSize(),
                bridgeHandler.getThing().getStatus().toString());
    }
}
