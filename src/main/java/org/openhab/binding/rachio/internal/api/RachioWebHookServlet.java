package org.openhab.binding.rachio.internal.api;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.binding.rachio.internal.handler.RachioStatusListener;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@link RachioWebHookServlet} handles incoming webhook events from Rachio Cloud
 *
 * @author Brian G. - Initial contribution (from 2.5 binding)
 * @author Daniel B. - Major rewrite for OpenHAB 5.x
 */
@Component(service = RachioWebHookServlet.class, immediate = true)
@NonNullByDefault
public class RachioWebHookServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServlet.class);

    // Services
    private final HttpService httpService;
    private final ThingRegistry thingRegistry;
    private final HttpClientFactory httpClientFactory;

    // Security
    private final Map<String, String> bridgeSecrets = new ConcurrentHashMap<>();
    private final Map<String, List<String>> allowedIPs = new ConcurrentHashMap<>();
    private final Map<String, Boolean> useAWSIPs = new ConcurrentHashMap<>();

    // JSON parser
    private final Gson gson;

    // Webhook statistics
    private final Map<String, WebhookStats> webhookStats = new ConcurrentHashMap<>();
    private Instant lastWebhookTime = Instant.now();

    @Activate
    public RachioWebHookServlet(@Reference HttpService httpService, @Reference ThingRegistry thingRegistry,
            @Reference HttpClientFactory httpClientFactory, BundleContext bundleContext) {
        this.httpService = httpService;
        this.thingRegistry = thingRegistry;
        this.httpClientFactory = httpClientFactory;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();

        try {
            // Register servlet
            Dictionary<String, String> initParams = new Hashtable<>();
            initParams.put("alias", WEBHOOK_PATH);
            httpService.registerServlet(WEBHOOK_PATH, this, initParams, null);
            logger.info("Rachio webhook servlet registered at {}", WEBHOOK_PATH);
        } catch (ServletException | NamespaceException e) {
            logger.error("Failed to register Rachio webhook servlet: {}", e.getMessage(), e);
        }
    }

    @Deactivate
    public void deactivate() {
        try {
            httpService.unregister(WEBHOOK_PATH);
            logger.info("Rachio webhook servlet unregistered");
        } catch (IllegalArgumentException e) {
            logger.debug("Servlet already unregistered");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String clientIP = getClientIP(request);
        String requestBody = readRequestBody(request);
        String signature = request.getHeader("x-signature");
        
        logger.debug("Webhook received from IP: {}, Body length: {}, Signature: {}", 
                clientIP, requestBody.length(), signature != null ? "present" : "missing");

        // Validate request
        ValidationResult validation = validateRequest(clientIP, requestBody, signature);
        if (!validation.isValid()) {
            logger.warn("Webhook validation failed from {}: {}", clientIP, validation.getErrorMessage());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, validation.getErrorMessage());
            return;
        }

        // Parse and process event
        try {
            RachioWebhookEvent event = parseWebhookEvent(requestBody);
            if (event == null) {
                logger.warn("Failed to parse webhook event");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid event data");
                return;
            }

            // Process event based on type
            boolean processed = processWebhookEvent(event, validation.getBridgeId());
            
            if (processed) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("OK");
                
                // Update statistics
                updateWebhookStats(validation.getBridgeId(), event.getEventType());
                lastWebhookTime = Instant.now();
                
                logger.debug("Webhook processed successfully: {} for bridge {}", 
                        event.getEventType(), validation.getBridgeId());
            } else {
                response.setStatus(HttpServletResponse.SC_ACCEPTED);
                response.getWriter().write("Event not handled");
                logger.debug("Webhook event not handled: {}", event.getEventType());
            }
        } catch (Exception e) {
            logger.error("Error processing webhook: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Provide webhook status information
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        
        JsonObject status = new JsonObject();
        status.addProperty("status", "active");
        status.addProperty("path", WEBHOOK_PATH);
        status.addProperty("lastWebhook", lastWebhookTime.toString());
        status.addProperty("totalBridges", bridgeSecrets.size());
        
        JsonObject stats = new JsonObject();
        webhookStats.forEach((bridgeId, stat) -> {
            JsonObject bridgeStats = new JsonObject();
            bridgeStats.addProperty("totalEvents", stat.getTotalEvents());
            bridgeStats.addProperty("lastEvent", stat.getLastEventTime().toString());
            bridgeStats.addProperty("lastEventType", stat.getLastEventType());
            stats.add(bridgeId, bridgeStats);
        });
        status.add("statistics", stats);
        
        response.getWriter().write(gson.toJson(status));
    }

    /**
     * Read request body as string
     */
    private String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder buffer = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        }
        return buffer.toString();
    }

    /**
     * Get client IP address with proxy support
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // Handle multiple IPs in X-Forwarded-For
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip != null ? ip : "unknown";
    }

    /**
     * Validate webhook request
     */
    private ValidationResult validateRequest(String clientIP, String requestBody, @Nullable String signature) {
        // Find bridge that matches the webhook
        String bridgeId = findBridgeBySignature(requestBody, signature);
        if (bridgeId == null) {
            return ValidationResult.invalid("No matching bridge found");
        }

        // Validate IP address
        if (!isIPAllowed(clientIP, bridgeId)) {
            return ValidationResult.invalid("IP address not allowed: " + clientIP);
        }

        // Validate HMAC signature if secret is configured
        String secret = bridgeSecrets.get(bridgeId);
        if (secret != null && !secret.isEmpty()) {
            if (signature == null || signature.isEmpty()) {
                return ValidationResult.invalid("Missing HMAC signature");
            }
            if (!validateHMAC(requestBody, signature, secret)) {
                return ValidationResult.invalid("Invalid HMAC signature");
            }
        }

        return ValidationResult.valid(bridgeId);
    }

    /**
     * Find bridge by attempting to parse event and match device
     */
    private @Nullable String findBridgeBySignature(String requestBody, @Nullable String signature) {
        try {
            // Try to parse event to get device ID
            RachioWebhookEvent event = parseWebhookEvent(requestBody);
            if (event != null && event.getDeviceId() != null) {
                // Find bridge that manages this device
                for (Thing thing : thingRegistry.getAll()) {
                    if (THING_TYPE_BRIDGE.equals(thing.getThingTypeUID())) {
                        ThingHandler handler = thing.getHandler();
                        if (handler instanceof RachioBridgeHandler) {
                            RachioBridgeHandler bridgeHandler = (RachioBridgeHandler) handler;
                            List<RachioDevice> devices = bridgeHandler.getDevices();
                            for (RachioDevice device : devices) {
                                if (device.getId().equals(event.getDeviceId())) {
                                    return thing.getUID().getId();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error parsing webhook for bridge identification: {}", e.getMessage());
        }

        // Fallback: Use first bridge with matching signature
        if (signature != null) {
            for (Map.Entry<String, String> entry : bridgeSecrets.entrySet()) {
                if (validateHMAC(requestBody, signature, entry.getValue())) {
                    return entry.getKey();
                }
            }
        }

        // Last resort: Return first bridge if only one exists
        if (bridgeSecrets.size() == 1) {
            return bridgeSecrets.keySet().iterator().next();
        }

        return null;
    }

    /**
     * Check if IP address is allowed for bridge
     */
    private boolean isIPAllowed(String clientIP, String bridgeId) {
        List<String> allowed = allowedIPs.get(bridgeId);
        Boolean useAWS = useAWSIPs.get(bridgeId);

        // If no restrictions configured, allow all
        if ((allowed == null || allowed.isEmpty()) && (useAWS == null || !useAWS)) {
            return true;
        }

        // Check exact IP matches
        if (allowed != null && allowed.contains(clientIP)) {
            return true;
        }

        // Check CIDR ranges
        if (allowed != null) {
            for (String range : allowed) {
                if (isIPInRange(clientIP, range)) {
                    return true;
                }
            }
        }

        // Check AWS IP ranges if enabled
        if (useAWS != null && useAWS && isAWSIP(clientIP)) {
            return true;
        }

        return false;
    }

    /**
     * Check if IP is in AWS range
     */
    private boolean isAWSIP(String ip) {
        // Simplified AWS IP check - in production, use AWS published IP ranges
        // https://docs.aws.amazon.com/general/latest/gr/aws-ip-ranges.html
        return ip.startsWith("52.27.") || ip.startsWith("52.39.") || 
               ip.startsWith("34.208.") || ip.startsWith("54.148.");
    }

    /**
     * Check if IP is in CIDR range
     */
    private boolean isIPInRange(String ip, String range) {
        if (!range.contains("/")) {
            return ip.equals(range);
        }

        try {
            String[] parts = range.split("/");
            String network = parts[0];
            int prefix = Integer.parseInt(parts[1]);

            long ipLong = ipToLong(ip);
            long networkLong = ipToLong(network);
            long mask = ~((1L << (32 - prefix)) - 1);

            return (ipLong & mask) == (networkLong & mask);
        } catch (Exception e) {
            logger.debug("Error checking IP range {} for IP {}: {}", range, ip, e.getMessage());
            return false;
        }
    }

    /**
     * Convert IP address to long
     */
    private long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        return (Long.parseLong(octets[0]) << 24) +
               (Long.parseLong(octets[1]) << 16) +
               (Long.parseLong(octets[2]) << 8) +
               Long.parseLong(octets[3]);
    }

    /**
     * Validate HMAC signature
     */
    private boolean validateHMAC(String body, String signature, String secret) {
        try {
            String computedSignature = computeHMAC(body, secret);
            
            // Constant-time comparison to prevent timing attacks
            return constantTimeEquals(computedSignature, signature);
        } catch (Exception e) {
            logger.debug("Error validating HMAC: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Compute HMAC-SHA256 signature
     */
    private String computeHMAC(String data, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Constant-time string comparison
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
    private @Nullable RachioWebhookEvent parseWebhookEvent(String json) {
        try {
            return gson.fromJson(json, RachioWebhookEvent.class);
        } catch (Exception e) {
            logger.debug("Error parsing webhook event: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Process webhook event based on type
     */
    private boolean processWebhookEvent(RachioWebhookEvent event, String bridgeId) {
        String eventType = event.getEventType();
        logger.debug("Processing webhook event: {} for device {}", eventType, event.getDeviceId());

        // Find bridge handler
        RachioBridgeHandler bridgeHandler = findBridgeHandler(bridgeId);
        if (bridgeHandler == null) {
            logger.warn("Bridge handler not found for bridge ID: {}", bridgeId);
            return false;
        }

        // Process based on event type
        switch (eventType) {
            case EVENT_ZONE_STATUS:
                return processZoneStatusEvent(event, bridgeHandler);
                
            case EVENT_DEVICE_STATUS:
                return processDeviceStatusEvent(event, bridgeHandler);
                
            case EVENT_RAIN_DELAY:
                return processRainDelayEvent(event, bridgeHandler);
                
            case EVENT_WEATHER_INTEL:
                return processWeatherIntelEvent(event, bridgeHandler);
                
            case EVENT_WATER_BUDGET:
                return processWaterBudgetEvent(event, bridgeHandler);
                
            case EVENT_SCHEDULE_STATUS:
                return processScheduleStatusEvent(event, bridgeHandler);
                
            case EVENT_RAIN_SENSOR:
                return processRainSensorEvent(event, bridgeHandler);
                
            case EVENT_DEVICE_ALERT:
                return processDeviceAlertEvent(event, bridgeHandler);
                
            case EVENT_SCHEDULE_COMPLETE:
                return processScheduleCompleteEvent(event, bridgeHandler);
                
            default:
                logger.debug("Unhandled event type: {}", eventType);
                return false;
        }
    }

    /**
     * Process ZONE_STATUS event
     */
    private boolean processZoneStatusEvent(RachioWebhookEvent event, RachioBridgeHandler bridgeHandler) {
        try {
            // Extract zone information
            String zoneId = event.getZoneId();
            String status = event.getStatus();
            Integer duration = event.getDuration();
            
            if (zoneId == null || status == null) {
                logger.warn("Invalid ZONE_STATUS event: missing zoneId or status");
                return false;
            }
            
            // Create zone DTO from event data
            RachioZone zone = new RachioZone();
            zone.setId(zoneId);
            zone.setName(event.getZoneName() != null ? event.getZoneName() : "Zone " + zoneId);
            zone.setStatus(org.openhab.binding.rachio.internal.api.dto.ZoneRunStatus.valueOf(status));
            
            if (duration != null) {
                zone.setDuration(duration);
            }
            
            // Update zone state via bridge
            bridgeHandler.notifyZoneUpdated(zone);
            
            // Update zone thing if it exists
            updateZoneThing(zone, bridgeHandler);
            
            logger.debug("Zone {} status updated to {}", zoneId, status);
            return true;
        } catch (Exception e) {
            logger.error("Error processing ZONE_STATUS event: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process DEVICE_STATUS event
     */
    private boolean processDeviceStatusEvent(RachioWebhookEvent event, RachioBridgeHandler bridgeHandler) {
        try {
            String deviceId = event.getDeviceId();
            String status = event.getStatus();
            
            if (deviceId == null || status == null) {
                logger.warn("Invalid DEVICE_STATUS event: missing deviceId or status");
                return false;
            }
            
            // Trigger device refresh
            bridgeHandler.notifyDeviceUpdated(null);
            
            // Update device thing if it exists
            updateDeviceThing(deviceId, status, bridgeHandler);
            
            logger.debug("Device {} status updated to {}", deviceId, status);
            return true;
        } catch (Exception e) {
            logger.error("Error processing DEVICE_STATUS event: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process RAIN_DELAY event
     */
    private boolean processRainDelayEvent(RachioWebhookEvent event, RachioBridgeHandler bridgeHandler) {
        try {
            String deviceId = event.getDeviceId();
            Instant endTime = event.getRainDelayEndTime();
            
            if (deviceId == null || endTime == null) {
                logger.warn("Invalid RAIN_DELAY event: missing deviceId or endTime");
                return false;
            }
            
            // Calculate remaining hours
            long remainingSeconds = java.time.Duration.between(Instant.now(), endTime).getSeconds();
            int remainingHours = (int) Math.max(0, remainingSeconds / 3600);
            
            // Update device thing
            updateDeviceRainDelay(deviceId, remainingHours, bridgeHandler);
            
            logger.debug("Device {} rain delay set: {} hours remaining", deviceId, remainingHours);
            return true;
        } catch (Exception e) {
            logger.error("Error processing RAIN_DELAY event: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process WEATHER_INTEL event (smart skip)
     */
    private boolean processWeatherIntelEvent(RachioWebhookEvent event, RachioBridgeHandler bridgeHandler) {
        try {
            String deviceId = event.getDeviceId();
            Boolean smartSkipEnabled = event.getSmartSkipEnabled();
            String skipReason = event.getSkipReason();
            
            if (deviceId == null) {
                logger.warn("Invalid WEATHER_INTEL event: missing deviceId");
                return false;
            }
            
            // Update device thing with weather intelligence
            updateDeviceWeatherIntel(deviceId, smartSkipEnabled, skipReason, bridgeHandler);
            
            logger.debug("Device {} weather intelligence: skipEnabled={}, reason={}", 
                    deviceId, smartSkipEnabled, skipReason);
            return true;
        } catch (Exception e) {
            logger.error("Error processing WEATHER_INTEL event: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process WATER_BUDGET event
     */
    private boolean processWaterBudgetEvent(RachioWebhookEvent event, RachioBridgeHandler bridgeHandler) {
        try {
            String deviceId = event.getDeviceId();
            Integer waterBudget = event.getWaterBudgetPercent();
            
            if (deviceId == null || waterBudget == null) {
                logger.warn("Invalid WATER_BUDGET event: missing deviceId or waterBudget");
                return false;
            }
            
            // Update device thing with water budget
            updateDeviceWaterBudget(deviceId, waterBudget, bridgeHandler);
            
            logger.debug("Device {} water budget updated: {}%", deviceId, waterBudget);
            return true;
        } catch (Exception e) {
            logger.error("Error processing WATER_BUDGET event: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process SCHEDULE_STATUS event
     */
    private boolean processScheduleStatusEvent(RachioWebhookEvent event, RachioBridgeHandler bridgeHandler) {
        try {
            String deviceId = event.getDeviceId();
            String scheduleId = event.getScheduleId();
            String status = event.getStatus();
            
            if (deviceId == null || scheduleId == null || status == null) {
                logger.warn("Invalid SCHEDULE_STATUS event: missing data");
                return false;
            }
            
            // Update device thing with schedule status
            updateDeviceScheduleStatus(deviceId, scheduleId, status, bridgeHandler);
            
            logger.debug("Device {} schedule {} status: {}", deviceId, scheduleId, status);
            return true;
        } catch (Exception e) {
            logger.error("Error processing SCHEDULE_STATUS event: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process RAIN_SENSOR event
     */
    private boolean processRainSensorEvent(RachioWebhookEvent event, RachioBridgeHandler bridgeHandler) {
        try {
            String deviceId = event.getDeviceId();
            Boolean rainDetected = event.getRainDetected();
            
            if (deviceId == null || rainDetected == null) {
                logger.warn("Invalid RAIN_SENSOR event: missing deviceId or rainDetected");
                return false;
            }
            
            // Update device thing with rain sensor status
            updateDeviceRainSensor(deviceId, rainDetected, bridgeHandler);
            
            logger.debug("Device {} rain sensor: detected={}", deviceId, rainDetected);
            return true;
        } catch (Exception e) {
            logger.error("Error processing RAIN_SENSOR event: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process DEVICE_ALERT event
     */
    private boolean processDeviceAlertEvent(RachioWebhookEvent event, RachioBridgeHandler bridgeHandler) {
        try {
            String deviceId = event.getDeviceId();
            String alertType = event.getAlertType();
            String alertMessage = event.getAlertMessage();
            
            if (deviceId == null || alertType == null) {
                logger.warn("Invalid DEVICE_ALERT event: missing deviceId or alertType");
                return false;
            }
            
            // Update device thing with alert
            updateDeviceAlert(deviceId, alertType, alertMessage, bridgeHandler);
            
            logger.debug("Device {} alert: {} - {}", deviceId, alertType, alertMessage);
            return true;
        } catch (Exception e) {
            logger.error("Error processing DEVICE_ALERT event: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process SCHEDULE_COMPLETE event
     */
    private boolean processScheduleCompleteEvent(RachioWebhookEvent event, RachioBridgeHandler bridgeHandler) {
        try {
            String deviceId = event.getDeviceId();
            String scheduleId = event.getScheduleId();
            
            if (deviceId == null || scheduleId == null) {
                logger.warn("Invalid SCHEDULE_COMPLETE event: missing deviceId or scheduleId");
                return false;
            }
            
            // Update device thing with schedule completion
            updateDeviceScheduleComplete(deviceId, scheduleId, bridgeHandler);
            
            logger.debug("Device {} schedule {} completed", deviceId, scheduleId);
            return true;
        } catch (Exception e) {
            logger.error("Error processing SCHEDULE_COMPLETE event: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Update zone thing with new state
     */
    private void updateZoneThing(RachioZone zone, RachioBridgeHandler bridgeHandler) {
        for (Thing thing : thingRegistry.getAll()) {
            if (THING_TYPE_ZONE.equals(thing.getThingTypeUID())) {
                ThingHandler handler = thing.getHandler();
                if (handler instanceof RachioStatusListener) {
                    RachioStatusListener zoneHandler = (RachioStatusListener) handler;
                    
                    // Check if this zone belongs to the same bridge
                    org.openhab.core.thing.Bridge bridge = thing.getBridge();
                    if (bridge != null && bridge.getUID().equals(bridgeHandler.getThing().getUID())) {
                        // Check if zone ID matches
                        Object zoneId = thing.getConfiguration().get(CONFIG_ZONE_ID);
                        if (zoneId != null && zoneId.toString().equals(zone.getId())) {
                            zoneHandler.onZoneStateUpdated(zone);
                        }
                    }
                }
            }
        }
    }

    /**
     * Update device thing with status
     */
    private void updateDeviceThing(String deviceId, String status, RachioBridgeHandler bridgeHandler) {
        for (Thing thing : thingRegistry.getAll()) {
            if (THING_TYPE_DEVICE.equals(thing.getThingTypeUID())) {
                ThingHandler handler = thing.getHandler();
                if (handler instanceof RachioStatusListener) {
                    // Check if this device belongs to the same bridge
                    org.openhab.core.thing.Bridge bridge = thing.getBridge();
                    if (bridge != null && bridge.getUID().equals(bridgeHandler.getThing().getUID())) {
                        // Check if device ID matches
                        Object configDeviceId = thing.getConfiguration().get(CONFIG_DEVICE_ID);
                        if (configDeviceId != null && configDeviceId.toString().equals(deviceId)) {
                            ((RachioStatusListener) handler).onDeviceStateUpdated();
                        }
                    }
                }
            }
        }
    }

    /**
     * Update device rain delay
     */
    private void updateDeviceRainDelay(String deviceId, int hours, RachioBridgeHandler bridgeHandler) {
        // Similar implementation to updateDeviceThing, but updates rain delay channel
        // This would require extending the device handler to handle rain delay updates
    }

    /**
     * Update device weather intelligence
     */
    private void updateDeviceWeatherIntel(String deviceId, @Nullable Boolean enabled, 
                                        @Nullable String reason, RachioBridgeHandler bridgeHandler) {
        // Update weather intelligence channel on device thing
    }

    /**
     * Update device water budget
     */
    private void updateDeviceWaterBudget(String deviceId, int percent, RachioBridgeHandler bridgeHandler) {
        // Update water budget channel on device thing
    }

    /**
     * Update device schedule status
     */
    private void updateDeviceScheduleStatus(String deviceId, String scheduleId, 
                                          String status, RachioBridgeHandler bridgeHandler) {
        // Update schedule status channel on device thing
    }

    /**
     * Update device rain sensor
     */
    private void updateDeviceRainSensor(String deviceId, boolean detected, RachioBridgeHandler bridgeHandler) {
        // Update rain sensor channel on device thing
    }

    /**
     * Update device alert
     */
    private void updateDeviceAlert(String deviceId, String type, @Nullable String message, 
                                 RachioBridgeHandler bridgeHandler) {
        // Update alert channel on device thing
    }

    /**
     * Update device schedule completion
     */
    private void updateDeviceScheduleComplete(String deviceId, String scheduleId, RachioBridgeHandler bridgeHandler) {
        // Update schedule completion on device thing
    }

    /**
     * Find bridge handler by bridge ID
     */
    private @Nullable RachioBridgeHandler findBridgeHandler(String bridgeId) {
        for (Thing thing : thingRegistry.getAll()) {
            if (THING_TYPE_BRIDGE.equals(thing.getThingTypeUID()) && 
                thing.getUID().getId().equals(bridgeId)) {
                ThingHandler handler = thing.getHandler();
                if (handler instanceof RachioBridgeHandler) {
                    return (RachioBridgeHandler) handler;
                }
            }
        }
        return null;
    }

    /**
     * Update webhook statistics
     */
    private void updateWebhookStats(String bridgeId, String eventType) {
        WebhookStats stats = webhookStats.computeIfAbsent(bridgeId, k -> new WebhookStats());
        stats.recordEvent(eventType);
    }

    /**
     * Register bridge configuration for webhook validation
     */
    public void registerBridge(String bridgeId, @Nullable String secret, 
                              @Nullable List<String> allowedIPs, boolean useAWSIPs) {
        if (secret != null) {
            bridgeSecrets.put(bridgeId, secret);
        }
        if (allowedIPs != null) {
            this.allowedIPs.put(bridgeId, allowedIPs);
        }
        this.useAWSIPs.put(bridgeId, useAWSIPs);
        
        logger.debug("Registered bridge {} for webhook processing", bridgeId);
    }

    /**
     * Unregister bridge configuration
     */
    public void unregisterBridge(String bridgeId) {
        bridgeSecrets.remove(bridgeId);
        allowedIPs.remove(bridgeId);
        useAWSIPs.remove(bridgeId);
        webhookStats.remove(bridgeId);
        
        logger.debug("Unregistered bridge {} from webhook processing", bridgeId);
    }

    /**
     * Get webhook statistics for a bridge
     */
    public @Nullable WebhookStats getWebhookStats(String bridgeId) {
        return webhookStats.get(bridgeId);
    }

    /**
     * Get last webhook time
     */
    public Instant getLastWebhookTime() {
        return lastWebhookTime;
    }

    /**
     * Validation result class
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final @Nullable String bridgeId;

        private ValidationResult(boolean valid, @Nullable String errorMessage, @Nullable String bridgeId) {
            this.valid = valid;
            this.errorMessage = errorMessage != null ? errorMessage : "";
            this.bridgeId = bridgeId;
        }

        public static ValidationResult valid(String bridgeId) {
            return new ValidationResult(true, null, bridgeId);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage, null);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public @Nullable String getBridgeId() {
            return bridgeId;
        }
    }

    /**
     * Webhook statistics class
     */
    public static class WebhookStats {
        private int totalEvents = 0;
        private Instant lastEventTime = Instant.now();
        private String lastEventType = "NONE";

        public void recordEvent(String eventType) {
            totalEvents++;
            lastEventTime = Instant.now();
            lastEventType = eventType;
        }

        public int getTotalEvents() {
            return totalEvents;
        }

        public Instant getLastEventTime() {
            return lastEventTime;
        }

        public String getLastEventType() {
            return lastEventType;
        }
    }
}
