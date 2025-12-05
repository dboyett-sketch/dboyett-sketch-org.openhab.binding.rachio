package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Servlet for handling Rachio webhook callbacks with HMAC-SHA256 security
 * 
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
@Component(service = RachioWebHookServlet.class, immediate = true)
public class RachioWebHookServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServlet.class);
    
    private static final String WEBHOOK_PATH = "/rachio/webhook";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_HEADER = "X-Rachio-Signature";
    
    private final Gson gson;
    private final Map<String, RachioBridgeHandler> bridgeHandlers = new ConcurrentHashMap<>();
    
    private @Nullable HttpService httpService;
    private boolean servletRegistered = false;
    
    @Activate
    public RachioWebHookServlet(@Reference HttpService httpService) {
        this.httpService = httpService;
        
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Instant.class, new InstantTypeAdapter());
        this.gson = gsonBuilder.create();
        
        registerServlet();
    }
    
    private void registerServlet() {
        HttpService localHttpService = httpService;
        if (localHttpService == null) {
            logger.warn("HttpService not available, cannot register webhook servlet");
            return;
        }
        
        try {
            localHttpService.registerServlet(WEBHOOK_PATH, this, null, null);
            servletRegistered = true;
            logger.info("Rachio webhook servlet registered at {}", WEBHOOK_PATH);
        } catch (ServletException | NamespaceException e) {
            logger.error("Failed to register webhook servlet: {}", e.getMessage(), e);
            servletRegistered = false;
        }
    }
    
    @Deactivate
    public void deactivate() {
        HttpService localHttpService = httpService;
        if (localHttpService != null && servletRegistered) {
            try {
                localHttpService.unregister(WEBHOOK_PATH);
                servletRegistered = false;
                logger.info("Rachio webhook servlet unregistered");
            } catch (IllegalArgumentException e) {
                logger.debug("Servlet already unregistered: {}", e.getMessage());
            }
        }
        
        bridgeHandlers.clear();
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String requestBody = getRequestBody(req);
        String signature = req.getHeader(SIGNATURE_HEADER);
        
        // Log request details for debugging
        logRequestDetails(req, requestBody);
        
        if (requestBody == null || requestBody.isEmpty()) {
            logger.warn("Received empty webhook request");
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Empty request body");
            return;
        }
        
        if (signature == null || signature.isEmpty()) {
            logger.warn("Webhook request missing signature header");
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Missing signature");
            return;
        }
        
        // Parse the request to get device ID
        String deviceId = extractDeviceId(requestBody);
        if (deviceId == null || deviceId.isEmpty()) {
            logger.warn("Could not extract device ID from webhook request");
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid request format");
            return;
        }
        
        // Find the appropriate bridge handler for this device
        RachioBridgeHandler handler = findBridgeHandlerForDevice(deviceId);
        if (handler == null) {
            logger.warn("No bridge handler found for device ID: {}", deviceId);
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "No handler for device");
            return;
        }
        
        // Get API key from handler for signature verification
        String apiKey = getApiKeyFromHandler(handler);
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("No API key available for device ID: {}", deviceId);
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "No API key configured");
            return;
        }
        
        // Verify HMAC signature
        if (!verifySignature(requestBody, signature, apiKey)) {
            logger.warn("Invalid signature for webhook request from device: {}", deviceId);
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
            return;
        }
        
        // Process the webhook event
        try {
            processWebhookEvent(handler, requestBody);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("OK");
            logger.debug("Webhook processed successfully for device: {}", deviceId);
        } catch (Exception e) {
            logger.error("Error processing webhook: {}", e.getMessage(), e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Processing error");
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // GET requests can be used for health checks
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain");
        resp.getWriter().write("Rachio Webhook Servlet Active");
        resp.getWriter().write("\nRegistered handlers: " + bridgeHandlers.size());
        resp.getWriter().write("\nPath: " + WEBHOOK_PATH);
        logger.debug("Health check request received");
    }
    
    private String getRequestBody(HttpServletRequest req) throws IOException {
        return new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
    
    private void logRequestDetails(HttpServletRequest req, @Nullable String body) {
        if (logger.isDebugEnabled()) {
            logger.debug("Webhook request received:");
            logger.debug("  Method: {}", req.getMethod());
            logger.debug("  Path: {}", req.getPathInfo());
            logger.debug("  Remote: {}:{}", req.getRemoteAddr(), req.getRemotePort());
            
            Enumeration<String> headerNames = req.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                logger.debug("  Header {}: {}", headerName, req.getHeader(headerName));
            }
            
            if (body != null && !body.isEmpty()) {
                logger.debug("  Body: {}", body);
                
                // Try to parse and log structured data
                try {
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    if (json.has("deviceId")) {
                        logger.debug("  Device ID: {}", json.get("deviceId").getAsString());
                    }
                    if (json.has("eventType")) {
                        logger.debug("  Event Type: {}", json.get("eventType").getAsString());
                    }
                    if (json.has("timestamp")) {
                        logger.debug("  Timestamp: {}", json.get("timestamp").getAsString());
                    }
                } catch (Exception e) {
                    logger.debug("  Could not parse JSON body: {}", e.getMessage());
                }
            }
        }
    }
    
    private @Nullable String extractDeviceId(String requestBody) {
        try {
            JsonObject json = JsonParser.parseString(requestBody).getAsJsonObject();
            if (json.has("deviceId") && json.get("deviceId").isJsonPrimitive()) {
                return json.get("deviceId").getAsString();
            }
            
            // Some events might have deviceId nested
            if (json.has("device") && json.get("device").isJsonObject()) {
                JsonObject device = json.get("device").getAsJsonObject();
                if (device.has("id")) {
                    return device.get("id").getAsString();
                }
            }
            
            // Check for zone events that might reference device
            if (json.has("zone") && json.get("zone").isJsonObject()) {
                JsonObject zone = json.get("zone").getAsJsonObject();
                if (zone.has("deviceId")) {
                    return zone.get("deviceId").getAsString();
                }
            }
            
        } catch (Exception e) {
            logger.debug("Error extracting device ID: {}", e.getMessage());
        }
        
        return null;
    }
    
    private @Nullable RachioBridgeHandler findBridgeHandlerForDevice(String deviceId) {
        for (RachioBridgeHandler handler : bridgeHandlers.values()) {
            // Check if this handler manages the specified device
            if (handler.getBridgeConfiguration() != null) {
                String handlerDeviceId = handler.getBridgeConfiguration().deviceId;
                if (deviceId.equals(handlerDeviceId)) {
                    return handler;
                }
            }
        }
        
        // If no exact match, return first handler (for single-device setups)
        if (!bridgeHandlers.isEmpty()) {
            return bridgeHandlers.values().iterator().next();
        }
        
        return null;
    }
    
    private @Nullable String getApiKeyFromHandler(RachioBridgeHandler handler) {
        if (handler.getBridgeConfiguration() != null) {
            return handler.getBridgeConfiguration().apiKey;
        }
        return null;
    }
    
    private boolean verifySignature(String requestBody, String signature, String apiKey) {
        try {
            // Create HMAC-SHA256 key
            SecretKeySpec secretKey = new SecretKeySpec(apiKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(secretKey);
            
            // Compute HMAC
            byte[] hmacBytes = mac.doFinal(requestBody.getBytes(StandardCharsets.UTF_8));
            String computedSignature = bytesToHex(hmacBytes);
            
            // Constant-time comparison to prevent timing attacks
            return constantTimeEquals(computedSignature, signature);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error verifying signature: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
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
    
    private void processWebhookEvent(RachioBridgeHandler handler, String requestBody) {
        try {
            JsonObject json = JsonParser.parseString(requestBody).getAsJsonObject();
            
            String eventType = json.has("eventType") ? json.get("eventType").getAsString() : "UNKNOWN";
            String deviceId = extractDeviceId(requestBody);
            
            if (deviceId == null) {
                logger.warn("Could not extract device ID from webhook event");
                return;
            }
            
            logger.info("Processing webhook event: {} for device: {}", eventType, deviceId);
            
            // Notify bridge handler
            handler.notifyWebhookEventReceived(deviceId, eventType, requestBody);
            
            // Parse specific event types for additional processing
            parseSpecificEventTypes(handler, json, eventType, deviceId);
            
        } catch (Exception e) {
            logger.error("Error processing webhook event: {}", e.getMessage(), e);
        }
    }
    
    private void parseSpecificEventTypes(RachioBridgeHandler handler, JsonObject json, 
                                         String eventType, String deviceId) {
        try {
            switch (eventType) {
                case "ZONE_STATUS_EVENT":
                    if (json.has("zone") && json.get("zone").isJsonObject()) {
                        JsonObject zoneJson = json.get("zone").getAsJsonObject();
                        String zoneId = zoneJson.has("id") ? zoneJson.get("id").getAsString() : null;
                        String zoneStatus = zoneJson.has("state") ? zoneJson.get("state").getAsString() : null;
                        
                        if (zoneId != null && zoneStatus != null) {
                            logger.debug("Zone {} status: {}", zoneId, zoneStatus);
                            // Could create zone DTO and notify handlers
                        }
                    }
                    break;
                    
                case "DEVICE_STATUS_EVENT":
                    if (json.has("device") && json.get("device").isJsonObject()) {
                        JsonObject deviceJson = json.get("device").getAsJsonObject();
                        String status = deviceJson.has("status") ? deviceJson.get("status").getAsString() : null;
                        
                        if (status != null) {
                            logger.info("Device {} status: {}", deviceId, status);
                        }
                    }
                    break;
                    
                case "RAIN_DELAY_EVENT":
                    if (json.has("rainDelay") && json.get("rainDelay").isJsonObject()) {
                        JsonObject rainDelayJson = json.get("rainDelay").getAsJsonObject();
                        int duration = rainDelayJson.has("duration") ? rainDelayJson.get("duration").getAsInt() : 0;
                        logger.info("Rain delay set: {} seconds", duration);
                    }
                    break;
                    
                case "WEATHER_INTELLIGENCE_EVENT":
                    logger.debug("Weather intelligence event received");
                    break;
                    
                case "WATER_BUDGET_EVENT":
                    logger.debug("Water budget event received");
                    break;
                    
                case "SCHEDULE_STATUS_EVENT":
                    logger.debug("Schedule status event received");
                    break;
                    
                case "RAIN_SENSOR_DETECTION_EVENT":
                    logger.debug("Rain sensor detection event received");
                    break;
                    
                default:
                    logger.debug("Unknown event type: {}", eventType);
                    break;
            }
        } catch (Exception e) {
            logger.debug("Error parsing event type {}: {}", eventType, e.getMessage());
        }
    }
    
    private void sendError(HttpServletResponse resp, int code, String message) throws IOException {
        resp.setStatus(code);
        resp.setContentType("text/plain");
        resp.getWriter().write(message);
        logger.debug("Sent error response: {} - {}", code, message);
    }
    
    // OSGi Service References
    
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addBridgeHandler(RachioBridgeHandler handler) {
        String thingId = handler.getThing().getUID().getId();
        bridgeHandlers.put(thingId, handler);
        logger.debug("Added bridge handler: {}", thingId);
    }
    
    public void removeBridgeHandler(RachioBridgeHandler handler) {
        String thingId = handler.getThing().getUID().getId();
        bridgeHandlers.remove(thingId);
        logger.debug("Removed bridge handler: {}", thingId);
    }
    
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setHttpService(HttpService httpService) {
        this.httpService = httpService;
        if (!servletRegistered) {
            registerServlet();
        }
    }
    
    public void unsetHttpService(HttpService httpService) {
        if (this.httpService == httpService) {
            this.httpService = null;
        }
    }
    
    // Getters for testing and monitoring
    
    public boolean isServletRegistered() {
        return servletRegistered;
    }
    
    public String getWebhookPath() {
        return WEBHOOK_PATH;
    }
    
    public int getRegisteredHandlerCount() {
        return bridgeHandlers.size();
    }
    
    public List<String> getRegisteredHandlerIds() {
        return Arrays.asList(bridgeHandlers.keySet().toArray(new String[0]));
    }
}
