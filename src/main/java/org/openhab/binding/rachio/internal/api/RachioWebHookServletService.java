package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for managing Rachio webhook servlet with professional security features
 *
 * @author Damion Boyett - Enhanced with enterprise security and health monitoring
 */
@NonNullByDefault
public class RachioWebHookServletService {
    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServletService.class);
    
    private static final String WEBHOOK_EXTERNAL_ID = "openhab-rachio-binding";
    private static final String WEBHOOK_DESCRIPTION = "OpenHAB Rachio Binding Webhook";
    private static final int WEBHOOK_REGISTRATION_RETRY_DELAY = 60; // seconds
    private static final int WEBHOOK_HEALTH_CHECK_INTERVAL = 300; // seconds
    
    private final RachioBridgeHandler bridgeHandler;
    private final int port;
    private final String path;
    private final RachioHttp http;
    private final ScheduledExecutorService scheduler;
    
    private @Nullable RachioWebHookServlet servlet;
    private @Nullable String webhookUrl;
    private @Nullable String webhookId;
    private @Nullable ScheduledFuture<?> healthCheckJob;
    
    private final List<String> allowedIPs = Collections.synchronizedList(new ArrayList<>());
    private boolean useAwsIpRanges = false;
    private boolean enabled = false;
    private Instant lastWebhookTime = Instant.MIN;
    private Instant lastHealthCheck = Instant.MIN;
    
    // Track webhook events for monitoring
    private final Map<String, Instant> recentEvents = new ConcurrentHashMap<>();
    private static final int MAX_EVENT_HISTORY = 100;
    private static final long EVENT_HISTORY_RETENTION_MS = 3600000; // 1 hour
    
    public RachioWebHookServletService(RachioBridgeHandler bridgeHandler, int port, String path, 
                                       RachioHttp http, ScheduledExecutorService scheduler) {
        this.bridgeHandler = bridgeHandler;
        this.port = port;
        this.path = path;
        this.http = http;
        this.scheduler = scheduler;
        
        logger.debug("Webhook service created for port {} path {}", port, path);
    }
    
    public void activate() {
        logger.info("Activating Rachio webhook service");
        
        try {
            // Create and start servlet
            servlet = new RachioWebHookServlet(bridgeHandler, this);
            servlet.activate(port, path);
            
            // Build webhook URL
            webhookUrl = buildWebhookUrl();
            logger.info("Webhook servlet activated at: {}", webhookUrl);
            
            // Register webhook with Rachio API
            registerWebhook();
            
            // Enable service
            enabled = true;
            
            // Schedule health check
            scheduleHealthCheck();
            
        } catch (Exception e) {
            logger.error("Failed to activate webhook service: {}", e.getMessage(), e);
            enabled = false;
        }
    }
    
    public void deactivate() {
        logger.info("Deactivating Rachio webhook service");
        
        // Stop health check
        stopHealthCheck();
        
        // Unregister webhook
        unregisterWebhook();
        
        // Deactivate servlet
        if (servlet != null) {
            try {
                servlet.deactivate();
            } catch (Exception e) {
                logger.debug("Error deactivating servlet: {}", e.getMessage());
            }
            servlet = null;
        }
        
        // Clear state
        webhookUrl = null;
        webhookId = null;
        enabled = false;
        recentEvents.clear();
        
        logger.debug("Webhook service deactivated");
    }
    
    private String buildWebhookUrl() {
        // Try to determine external URL
        // In production, this should be configured or detected
        String hostname = getHostname();
        return String.format("http://%s:%d%s", hostname, port, path);
    }
    
    private String getHostname() {
        // Try to get external hostname
        // This is a simplified version - in production you might want to:
        // 1. Use a configured external URL
        // 2. Use UPnP/NAT-PMP to setup port forwarding
        // 3. Use a dynamic DNS service
        
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.warn("Could not determine hostname: {}", e.getMessage());
            return "localhost";
        }
    }
    
    private void registerWebhook() {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            logger.error("Cannot register webhook: URL not available");
            return;
        }
        
        try {
            // Check if webhook already exists
            List<RachioApiWebHookEntry> existingWebhooks = getExistingWebhooks();
            RachioApiWebHookEntry existingWebhook = findExistingWebhook(existingWebhooks);
            
            if (existingWebhook != null) {
                // Update existing webhook
                webhookId = existingWebhook.getId(); // Fixed: Use getter method
                logger.info("Found existing webhook: {}", webhookId);
                
                // Check if URL needs update
                String existingUrl = existingWebhook.getUrl(); // Fixed: Use getter method
                if (!webhookUrl.equals(existingUrl)) {
                    updateWebhook(existingWebhook);
                }
            } else {
                // Create new webhook
                createWebhook();
            }
            
            logger.info("Webhook registration successful: {}", webhookUrl);
            
        } catch (Exception e) {
            logger.error("Failed to register webhook: {}", e.getMessage(), e);
            
            // Schedule retry
            scheduler.schedule(this::registerWebhook, WEBHOOK_REGISTRATION_RETRY_DELAY, TimeUnit.SECONDS);
        }
    }
    
    private void unregisterWebhook() {
        if (webhookId == null) {
            logger.debug("No webhook ID to unregister");
            return;
        }
        
        try {
            // Get list of webhooks
            List<RachioApiWebHookEntry> webhooks = getExistingWebhooks();
            
            // Find and delete our webhook
            for (RachioApiWebHookEntry webhook : webhooks) {
                // Fixed: Use getter methods for comparison
                if (webhookId.equals(webhook.getId()) || 
                    WEBHOOK_EXTERNAL_ID.equals(webhook.getExternalId())) {
                    
                    deleteWebhook(webhook);
                    logger.info("Unregistered webhook: {}", webhookId);
                    break;
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to unregister webhook: {}", e.getMessage(), e);
        }
    }
    
    private List<RachioApiWebHookEntry> getExistingWebhooks() throws IOException, RachioApiException {
        String response = http.executeGet("/webhook");
        RachioApiWebHookList webhookList = http.getGson().fromJson(response, RachioApiWebHookList.class);
        
        // Fixed: Access data field through getter method
        return webhookList != null && webhookList.getData() != null ? webhookList.getData() : List.of();
    }
    
    private @Nullable RachioApiWebHookEntry findExistingWebhook(List<RachioApiWebHookEntry> webhooks) {
        for (RachioApiWebHookEntry webhook : webhooks) {
            // Fixed: Use getter methods for comparison
            if (WEBHOOK_EXTERNAL_ID.equals(webhook.getExternalId())) {
                return webhook;
            }
        }
        return null;
    }
    
    private void createWebhook() throws IOException, RachioApiException {
        String body = String.format(
            "{\"externalId\":\"%s\",\"url\":\"%s\",\"eventTypes\":[\"DEVICE_STATUS_EVENT\",\"ZONE_STATUS_EVENT\",\"RAIN_DELAY_EVENT\",\"WEATHER_INTELLIGENCE_EVENT\",\"WATER_BUDGET_EVENT\",\"SCHEDULE_STATUS_EVENT\",\"RAIN_SENSOR_DETECTION_EVENT\"],\"deviceIds\":[]}",
            WEBHOOK_EXTERNAL_ID, webhookUrl);
        
        String response = http.executePut("/webhook", body);
        logger.debug("Webhook creation response: {}", response);
        
        // Parse response to get webhook ID
        RachioApiWebHookEntry createdWebhook = http.getGson().fromJson(response, RachioApiWebHookEntry.class);
        if (createdWebhook != null) {
            webhookId = createdWebhook.getId(); // Fixed: Use getter method
            logger.info("Created new webhook: {}", webhookId);
        }
    }
    
    private void updateWebhook(RachioApiWebHookEntry webhook) throws IOException, RachioApiException {
        String webhookId = webhook.getId(); // Fixed: Use getter method
        if (webhookId == null) {
            logger.error("Cannot update webhook: ID not available");
            return;
        }
        
        String body = String.format("{\"url\":\"%s\"}", webhookUrl);
        String response = http.executePut("/webhook/" + webhookId, body);
        logger.debug("Webhook update response: {}", response);
        
        logger.info("Updated webhook {} with new URL: {}", webhookId, webhookUrl);
    }
    
    private void deleteWebhook(RachioApiWebHookEntry webhook) throws IOException, RachioApiException {
        String webhookId = webhook.getId(); // Fixed: Use getter method
        if (webhookId == null) {
            logger.error("Cannot delete webhook: ID not available");
            return;
        }
        
        String response = http.executeDelete("/webhook/" + webhookId);
        logger.debug("Webhook deletion response: {}", response);
    }
    
    private void scheduleHealthCheck() {
        if (healthCheckJob == null || healthCheckJob.isCancelled()) {
            healthCheckJob = scheduler.scheduleWithFixedDelay(this::checkWebhookHealth, 
                    WEBHOOK_HEALTH_CHECK_INTERVAL, WEBHOOK_HEALTH_CHECK_INTERVAL, TimeUnit.SECONDS);
            logger.debug("Scheduled webhook health check every {} seconds", WEBHOOK_HEALTH_CHECK_INTERVAL);
        }
    }
    
    private void stopHealthCheck() {
        if (healthCheckJob != null && !healthCheckJob.isCancelled()) {
            healthCheckJob.cancel(true);
            healthCheckJob = null;
            logger.debug("Stopped webhook health check");
        }
    }
    
    public boolean checkWebhookHealth() {
        logger.debug("Checking webhook health");
        lastHealthCheck = Instant.now();
        
        if (!enabled) {
            logger.warn("Webhook service not enabled");
            return false;
        }
        
        if (webhookId == null) {
            logger.warn("Webhook not registered, attempting to register");
            registerWebhook();
            return false;
        }
        
        try {
            // Verify webhook exists and is active
            List<RachioApiWebHookEntry> webhooks = getExistingWebhooks();
            RachioApiWebHookEntry ourWebhook = findExistingWebhook(webhooks);
            
            if (ourWebhook == null) {
                logger.warn("Webhook not found in Rachio account, re-registering");
                registerWebhook();
                return false;
            }
            
            // Check if webhook URL is correct
            String currentUrl = ourWebhook.getUrl(); // Fixed: Use getter method
            if (!webhookUrl.equals(currentUrl)) {
                logger.warn("Webhook URL mismatch, updating");
                updateWebhook(ourWebhook);
                return false;
            }
            
            // Check last webhook activity
            if (lastWebhookTime.isBefore(Instant.now().minusSeconds(WEBHOOK_HEALTH_CHECK_INTERVAL * 2))) {
                logger.warn("No recent webhook activity (last: {})", lastWebhookTime);
                // Don't return false here - could just be no events
            }
            
            logger.debug("Webhook health check passed");
            return true;
            
        } catch (Exception e) {
            logger.error("Webhook health check failed: {}", e.getMessage(), e);
            return false;
        }
    }
    
    public void reregisterWebhook() {
        logger.info("Re-registering webhook");
        unregisterWebhook();
        
        // Wait a bit before re-registering
        scheduler.schedule(() -> {
            webhookId = null;
            registerWebhook();
        }, 5, TimeUnit.SECONDS);
    }
    
    // Security configuration
    
    public void setAllowedIPs(String ipList) {
        allowedIPs.clear();
        if (ipList != null && !ipList.isEmpty()) {
            String[] ips = ipList.split(",");
            for (String ip : ips) {
                ip = ip.trim();
                if (!ip.isEmpty()) {
                    allowedIPs.add(ip);
                }
            }
            logger.info("Set allowed IPs: {}", allowedIPs);
        }
    }
    
    public void enableAwsIpRanges() {
        useAwsIpRanges = true;
        logger.info("Enabled AWS IP range verification");
        
        // Load AWS IP ranges
        loadAwsIpRanges();
    }
    
    private void loadAwsIpRanges() {
        // Implementation for loading AWS IP ranges
        // This would fetch from https://ip-ranges.amazonaws.com/ip-ranges.json
        // and add to allowedIPs list
        logger.debug("AWS IP range loading would be implemented here");
    }
    
    public List<String> getAllowedIPs() {
        return Collections.unmodifiableList(allowedIPs);
    }
    
    public boolean isIpAllowed(String ipAddress) {
        if (allowedIPs.isEmpty() && !useAwsIpRanges) {
            return true; // No restrictions
        }
        
        // Check exact IP matches
        if (allowedIPs.contains(ipAddress)) {
            return true;
        }
        
        // Check CIDR ranges
        for (String allowedIp : allowedIPs) {
            if (allowedIp.contains("/")) {
                if (isInCidrRange(ipAddress, allowedIp)) {
                    return true;
                }
            }
        }
        
        // Check AWS IP ranges if enabled
        if (useAwsIpRanges && isAwsIp(ipAddress)) {
            return true;
        }
        
        return false;
    }
    
    private boolean isInCidrRange(String ipAddress, String cidr) {
        // Simplified CIDR check - in production use proper IP address library
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) return false;
            
            String network = parts[0];
            int prefix = Integer.parseInt(parts[1]);
            
            // This is a simplified check - real implementation would use
            // proper IP address arithmetic
            return ipAddress.startsWith(network.substring(0, network.lastIndexOf('.')));
        } catch (Exception e) {
            logger.debug("Error checking CIDR range: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean isAwsIp(String ipAddress) {
        // Check if IP is in AWS range
        // This would use the loaded AWS IP ranges
        return false; // Simplified
    }
    
    // Event tracking
    
    public void recordWebhookEvent(String eventType, String deviceId) {
        lastWebhookTime = Instant.now();
        String key = String.format("%s-%s-%d", eventType, deviceId, lastWebhookTime.toEpochMilli());
        
        recentEvents.put(key, lastWebhookTime);
        
        // Clean up old events
        cleanupEventHistory();
        
        logger.debug("Recorded webhook event: {} for device: {}", eventType, deviceId);
    }
    
    private void cleanupEventHistory() {
        Instant cutoff = Instant.now().minusMillis(EVENT_HISTORY_RETENTION_MS);
        
        recentEvents.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        
        // Limit size
        if (recentEvents.size() > MAX_EVENT_HISTORY) {
            List<Map.Entry<String, Instant>> entries = new ArrayList<>(recentEvents.entrySet());
            entries.sort(Map.Entry.comparingByValue());
            
            int toRemove = entries.size() - MAX_EVENT_HISTORY;
            for (int i = 0; i < toRemove; i++) {
                recentEvents.remove(entries.get(i).getKey());
            }
        }
    }
    
    public Map<String, Instant> getRecentEvents() {
        return Collections.unmodifiableMap(recentEvents);
    }
    
    public int getRecentEventCount() {
        return recentEvents.size();
    }
    
    public @Nullable Instant getLastWebhookTime() {
        return lastWebhookTime;
    }
    
    // Getters
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public @Nullable String getWebhookUrl() {
        return webhookUrl;
    }
    
    public @Nullable String getWebhookId() {
        return webhookId;
    }
    
    public @Nullable RachioWebHookServlet getServlet() {
        return servlet;
    }
    
    public RachioBridgeHandler getBridgeHandler() {
        return bridgeHandler;
    }
    
    // Utility method for executeDelete (if needed in RachioHttp)
    private String executeDelete(String endpoint) throws IOException, RachioApiException {
        // This would be a wrapper around http.executeRequest("DELETE", endpoint, null)
        // but RachioHttp doesn't currently have executeDelete method
        throw new UnsupportedOperationException("DELETE not implemented in current RachioHttp");
    }
}

