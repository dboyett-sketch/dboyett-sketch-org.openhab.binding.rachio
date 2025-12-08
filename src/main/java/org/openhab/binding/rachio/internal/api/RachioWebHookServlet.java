package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.RachioWebHookEvent;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.core.io.net.http.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Servlet for handling incoming Rachio webhook events.
 * This servlet validates HMAC signatures, filters by IP, and processes events.
 */
@NonNullByDefault
public class RachioWebHookServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServlet.class);
    
    private final RachioBridgeHandler bridgeHandler;
    private final Gson gson;
    
    // Cache for HMAC validation to prevent timing attacks
    private final Map<String, Boolean> hmacCache = new ConcurrentHashMap<>();
    
    public RachioWebHookServlet(RachioBridgeHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
    }
    
    /**
     * Activate the servlet with port and secret
     */
    public void activate(int port, String secret) {
        logger.debug("Rachio WebHook Servlet activated on port {} with secret configured", port);
    }
    
    /**
     * Deactivate the servlet
     */
    public void deactivate() {
        logger.debug("Rachio WebHook Servlet deactivated");
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.debug("GET request received for webhook verification");
        resp.setContentType("text/plain");
        resp.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = resp.getWriter();
        writer.write("Rachio OpenHAB Binding WebHook Endpoint Active");
        writer.flush();
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String clientIp = getClientIp(req);
        String contentType = req.getContentType();
        String signature = req.getHeader("X-RACHIO-SIGNATURE");
        String timestamp = req.getHeader("X-RACHIO-TIMESTAMP");
        
        logger.debug("WebHook POST received from IP: {}", clientIp);
        logger.debug("Content-Type: {}, Signature: {}, Timestamp: {}", contentType, 
                signature != null ? "[PRESENT]" : "[MISSING]", timestamp);
        
        // Validate basic requirements
        if (!validateBasicRequirements(req, resp, clientIp, signature, timestamp)) {
            return;
        }
        
        // Read request body
        String content = HttpUtil.readData(req);
        if (content == null || content.isEmpty()) {
            logger.warn("Empty webhook content received from {}", clientIp);
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Empty request body");
            return;
        }
        
        logger.trace("Webhook content: {}", content);
        
        // Validate HMAC signature
        if (!validateHmacSignature(content, signature, timestamp)) {
            logger.warn("HMAC validation failed for webhook from {}", clientIp);
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
            return;
        }
        
        // Parse and process the event
        try {
            RachioWebHookEvent event = gson.fromJson(content, RachioWebHookEvent.class);
            if (event == null) {
                logger.warn("Failed to parse webhook event from JSON: {}", content);
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON");
                return;
            }
            
            logger.debug("Webhook event parsed: type={}, deviceId={}, zoneId={}", 
                    event.getType(), event.getDeviceId(), event.getZoneId());
            
            // Process the event asynchronously
            processEventAsync(event);
            
            // Send success response
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            PrintWriter writer = resp.getWriter();
            writer.write("{\"status\":\"success\",\"message\":\"Event processed\"}");
            writer.flush();
            
        } catch (JsonSyntaxException e) {
            logger.warn("Invalid JSON in webhook from {}: {}", clientIp, e.getMessage());
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON format");
        } catch (Exception e) {
            logger.error("Error processing webhook from {}: {}", clientIp, e.getMessage(), e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
    
    /**
     * Validate basic requirements for the webhook request
     */
    private boolean validateBasicRequirements(HttpServletRequest req, HttpServletResponse resp, 
            String clientIp, @Nullable String signature, @Nullable String timestamp) throws IOException {
        
        // Check if webhooks are enabled
        if (!bridgeHandler.isWebhookEnabled()) {
            logger.debug("Webhooks disabled, rejecting request from {}", clientIp);
            sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Webhooks disabled");
            return false;
        }
        
        // Check IP filtering
        if (!bridgeHandler.isIpAllowed(clientIp)) {
            logger.warn("IP {} not allowed for webhook access", clientIp);
            sendError(resp, HttpServletResponse.SC_FORBIDDEN, "IP not allowed");
            return false;
        }
        
        // Check required headers
        if (signature == null || signature.isEmpty()) {
            logger.warn("Missing X-RACHIO-SIGNATURE header from {}", clientIp);
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing signature header");
            return false;
        }
        
        if (timestamp == null || timestamp.isEmpty()) {
            logger.warn("Missing X-RACHIO-TIMESTAMP header from {}", clientIp);
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing timestamp header");
            return false;
        }
        
        // Validate timestamp (prevent replay attacks)
        try {
            Instant eventTime = Instant.parse(timestamp);
            Instant now = Instant.now();
            long difference = Math.abs(now.getEpochSecond() - eventTime.getEpochSecond());
            
            if (difference > 300) { // 5 minutes tolerance
                logger.warn("Timestamp too far from current time: {} (difference: {}s)", timestamp, difference);
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid timestamp");
                return false;
            }
        } catch (DateTimeParseException e) {
            logger.warn("Invalid timestamp format from {}: {}", clientIp, timestamp);
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid timestamp format");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate HMAC signature using SHA-256
     */
    private boolean validateHmacSignature(String content, String signature, String timestamp) {
        String cacheKey = content + "|" + signature + "|" + timestamp;
        Boolean cached = hmacCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        try {
            String secret = bridgeHandler.getWebhookSecret();
            if (secret == null || secret.isEmpty()) {
                logger.warn("Webhook secret not configured");
                hmacCache.put(cacheKey, false);
                return false;
            }
            
            String payload = timestamp + content;
            String expectedSignature = calculateHmac(payload, secret);
            
            // Constant-time comparison to prevent timing attacks
            boolean isValid = constantTimeEquals(signature, expectedSignature);
            
            hmacCache.put(cacheKey, isValid);
            
            if (!isValid) {
                logger.debug("HMAC validation failed. Expected: {}, Received: {}", expectedSignature, signature);
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.error("Error validating HMAC: {}", e.getMessage(), e);
            hmacCache.put(cacheKey, false);
            return false;
        }
    }
    
    /**
     * Calculate HMAC-SHA256
     */
    private String calculateHmac(String payload, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(payload.getBytes());
        
        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Constant-time string comparison to prevent timing attacks
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
    
    /**
     * Get client IP address, handling proxies
     */
    private String getClientIp(HttpServletRequest req) {
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
        
        return Objects.requireNonNullElse(ip, "unknown");
    }
    
    /**
     * Send error response
     */
    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        PrintWriter writer = resp.getWriter();
        writer.write("{\"error\":\"" + message + "\"}");
        writer.flush();
    }
    
    /**
     * Process event asynchronously to avoid blocking the servlet thread
     */
    private void processEventAsync(RachioWebHookEvent event) {
        bridgeHandler.getScheduler().execute(() -> {
            try {
                bridgeHandler.handleWebhookEvent(event);
                logger.debug("Webhook event processed asynchronously: {}", event.getType());
            } catch (Exception e) {
                logger.error("Error processing webhook event asynchronously: {}", e.getMessage(), e);
            }
        });
    }
    
    /**
     * Clean up HMAC cache periodically
     */
    public void cleanupCache() {
        int maxSize = 1000;
        if (hmacCache.size() > maxSize) {
            hmacCache.clear();
            logger.debug("Cleared HMAC cache (size exceeded {})", maxSize);
        }
    }
}
