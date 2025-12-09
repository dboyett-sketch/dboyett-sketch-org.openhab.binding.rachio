package org.openhab.binding.rachio.internal.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.binding.rachio.internal.api.dto.RachioWebHookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Servlet for handling Rachio webhook callbacks
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioWebHookServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String HEADER_SIGNATURE = "X-Rachio-Signature";
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";

    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServlet.class);
    private final RachioBridgeHandler bridgeHandler;
    private final Gson gson;

    public RachioWebHookServlet(RachioBridgeHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
        this.gson = bridgeHandler.getGson();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.debug("Received webhook POST request");
        
        try {
            // Read request body
            String requestBody = readRequestBody(req);
            if (requestBody == null || requestBody.isEmpty()) {
                logger.warn("Empty webhook request body");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Get client IP address
            String clientIp = getClientIp(req);
            
            // Get X-Forwarded-For header if present
            String forwardedFor = req.getHeader(HEADER_FORWARDED_FOR);
            
            // Validate IP address
            if (!bridgeHandler.isIpAllowed(clientIp, forwardedFor)) {
                logger.warn("Webhook request from unauthorized IP: {} (X-Forwarded-For: {})", clientIp, forwardedFor);
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // Validate signature
            String signature = req.getHeader(HEADER_SIGNATURE);
            if (!isValidSignature(requestBody, signature)) {
                logger.warn("Invalid webhook signature");
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // Parse webhook event
            RachioWebHookEvent event = parseWebhookEvent(requestBody);
            if (event == null) {
                logger.warn("Failed to parse webhook event");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Handle the event
            bridgeHandler.handleWebhookEvent(event);
            
            // Send successful response
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"status\":\"ok\"}");
            
            logger.debug("Successfully processed webhook event: {}", event.type);
            
        } catch (Exception e) {
            logger.error("Error processing webhook request: {}", e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Simple health check endpoint
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.getWriter().write("{\"status\":\"online\",\"service\":\"rachio-webhook\"}");
    }

    /**
     * Read the request body as a string
     */
    private @Nullable String readRequestBody(HttpServletRequest req) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            logger.warn("Failed to read request body: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest req) {
        String ip = req.getRemoteAddr();
        
        // Check for proxy headers
        String xForwardedFor = req.getHeader(HEADER_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, first one is the client
            String[] ips = xForwardedFor.split(",");
            if (ips.length > 0) {
                ip = ips[0].trim();
            }
        }
        
        return ip;
    }

    /**
     * Validate the webhook signature
     */
    private boolean isValidSignature(String requestBody, @Nullable String signature) {
        if (signature == null || signature.isEmpty()) {
            logger.warn("Missing webhook signature");
            return false;
        }

        // Get secret key from bridge handler
        String secretKey = getSecretKey();
        if (secretKey == null || secretKey.isEmpty()) {
            logger.warn("No secret key configured for signature validation");
            return false;
        }

        try {
            // Calculate HMAC-SHA256 of request body
            String calculatedSignature = calculateHmacSha256(requestBody, secretKey);
            
            // Constant-time comparison to prevent timing attacks
            return constantTimeEquals(calculatedSignature, signature);
        } catch (Exception e) {
            logger.warn("Error validating signature: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get the secret key from bridge configuration
     */
    private @Nullable String getSecretKey() {
        // This would come from bridge configuration
        // For now, implement a method in bridge handler to get it
        return bridgeHandler.getBridgeConfiguration() != null ? 
               bridgeHandler.getBridgeConfiguration().secretKey : null;
    }

    /**
     * Calculate HMAC-SHA256 signature
     */
    private String calculateHmacSha256(String data, String key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
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
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC-SHA256", e);
        }
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
     * Parse webhook event from JSON
     */
    private @Nullable RachioWebHookEvent parseWebhookEvent(String json) {
        try {
            return gson.fromJson(json, RachioWebHookEvent.class);
        } catch (JsonSyntaxException e) {
            logger.warn("Failed to parse webhook event JSON: {}", e.getMessage());
            return null;
        }
    }
}
