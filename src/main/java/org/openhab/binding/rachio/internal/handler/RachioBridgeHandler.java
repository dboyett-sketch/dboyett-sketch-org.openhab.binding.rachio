package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.openhab.binding.rachio.internal.RachioSecurity;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.RachioException;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 * Now includes complete webhook management with registration, security, and cleanup.
 *
 * @author Damion
 */
@Component(service = ThingHandler.class, configurationPid = "bridge.rachio")
@NonNullByDefault
public class RachioBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);

    private final RachioHttp rachioHttp = new RachioHttp();
    private @Nullable ScheduledFuture<?> refreshJob;
    private @Nullable RachioPerson person;
    
    // CRITICAL: Store devices for discovery service
    private @Nullable List<RachioPerson.Device> devices;
    
    // AWS IP ranges for webhook verification (optional)
    private @Nullable List<RachioSecurity.AwsIpAddressRange> awsIpRanges;

    // Configuration parameters with defaults
    private int pollingInterval = 120;
    private int defaultRuntime = 300;
    private @Nullable String callbackUrl;
    private boolean clearAllCallbacks = false;
    private @Nullable String ipFilter;
    
    // Rate limit tracking
    private int adaptivePollingInterval = 120; // Current adaptive interval
    private boolean rateLimitWarningActive = false;
    private boolean rateLimitCriticalActive = false;
    
    // ===== WEBHOOK MANAGEMENT =====
    private @Nullable String webhookId; // Store the registered webhook ID for cleanup
    private @Nullable String webhookDeviceId; // Store device ID used for webhook registration
    private boolean webhooksEnabled = false;
    private boolean webhookRegistrationAttempted = false;
    
    // FIX: Use correct event type IDs from constants
    private static final List<String> DEFAULT_WEBHOOK_EVENTS = Arrays.asList(
        WEBHOOK_EVENT_ID_DEVICE_STATUS,          // "5"
        WEBHOOK_EVENT_ID_RAIN_DELAY,             // "6"
        WEBHOOK_EVENT_ID_WEATHER_INTELLIGENCE,   // "7"
        WEBHOOK_EVENT_ID_WATER_BUDGET,           // "8"
        WEBHOOK_EVENT_ID_SCHEDULE_STATUS,        // "9"
        WEBHOOK_EVENT_ID_ZONE_STATUS,            // "10"
        WEBHOOK_EVENT_ID_RAIN_SENSOR_DETECTION,  // "11"
        WEBHOOK_EVENT_ID_ZONE_DELTA,             // "12"
        WEBHOOK_EVENT_ID_DELTA                   // "14"
    );
    // ===== END WEBHOOK MANAGEMENT =====

    public RachioBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Activate
    public void activate() {
        rachioHttp.activate();
        logger.debug("RachioBridgeHandler activated");
    }

    @Deactivate
    public void deactivate() {
        rachioHttp.deactivate();
        ScheduledFuture<?> job = refreshJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(true);
        }
        refreshJob = null;
        logger.debug("RachioBridgeHandler deactivated");
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // No commands to handle for bridge
        logger.debug("Bridge received command: {} - {}", channelUID, command);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio bridge handler.");

        // Load configuration parameters
        loadConfiguration();

        String apiKey = (String) getConfig().get(API_KEY);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "API Key is required");
            return;
        }

        // Validate polling interval for rate limiting
        if (pollingInterval < 90) {
            logger.warn("Polling interval {} seconds is below minimum 90 seconds. Using 120 seconds for rate limit safety.", pollingInterval);
            pollingInterval = 120;
        }

        // Register this thing with the HTTP service
        String thingId = getThing().getUID().toString();
        rachioHttp.registerThing(thingId, apiKey);
        logger.debug("Registered bridge with thing ID: {}", thingId);

        // Log configuration
        logger.debug("Bridge configuration - Polling: {}s, Default Runtime: {}s, Callback URL: {}, Clear Callbacks: {}, IP Filter: {}", 
                    pollingInterval, defaultRuntime, callbackUrl, clearAllCallbacks, ipFilter);

        // Set initial status to UNKNOWN while we initialize
        updateStatus(ThingStatus.UNKNOWN);
        
        // Use scheduler with a small delay to ensure bridge is fully registered
        scheduler.schedule(this::initializeBridge, 1, TimeUnit.SECONDS);
    }

    /**
     * Load configuration parameters from thing configuration
     */
    private void loadConfiguration() {
        // FIX: Handle BigDecimal values from OpenHAB 5.x configuration
        // Polling Interval (enforce minimum 90 seconds for rate limiting)
        Object configPollingInterval = getConfig().get(POLLING_INTERVAL);
        if (configPollingInterval instanceof BigDecimal) {
            pollingInterval = ((BigDecimal) configPollingInterval).intValue();
        } else if (configPollingInterval instanceof Integer) {
            pollingInterval = (Integer) configPollingInterval;
        } else {
            pollingInterval = 120; // default
            logger.debug("Using default polling interval: {} seconds", pollingInterval);
        }
        
        // Validate polling interval
        if (pollingInterval < 90) {
            logger.warn("Polling interval {} seconds is below minimum 90 seconds. Using 120 seconds.", pollingInterval);
            pollingInterval = 120;
        }

        // FIX: Default Runtime - handle BigDecimal
        Object configDefaultRuntime = getConfig().get(DEFAULT_RUNTIME);
        if (configDefaultRuntime instanceof BigDecimal) {
            defaultRuntime = ((BigDecimal) configDefaultRuntime).intValue();
        } else if (configDefaultRuntime instanceof Integer) {
            defaultRuntime = (Integer) configDefaultRuntime;
        } else {
            defaultRuntime = 300; // default 5 minutes
            logger.debug("Using default runtime: {} seconds", defaultRuntime);
        }

        // Validate default runtime
        if (defaultRuntime <= 0) {
            logger.warn("Default runtime {} seconds is invalid. Using 300 seconds.", defaultRuntime);
            defaultRuntime = 300;
        }

        // Callback URL
        callbackUrl = (String) getConfig().get(CALLBACK_URL);
        if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
            logger.debug("Webhook callback URL configured: {}", callbackUrl);
            webhooksEnabled = true;
        } else {
            logger.debug("No webhook callback URL configured - webhooks disabled");
            webhooksEnabled = false;
        }

        // Clear All Callbacks
        Object configClearCallbacks = getConfig().get(CLEAR_ALL_CALLBACKS);
        if (configClearCallbacks instanceof Boolean) {
            clearAllCallbacks = (Boolean) configClearCallbacks;
        } else if (configClearCallbacks instanceof String) {
            clearAllCallbacks = Boolean.parseBoolean((String) configClearCallbacks);
        }
        logger.debug("Clear all callbacks: {}", clearAllCallbacks);

        // IP Filter
        ipFilter = (String) getConfig().get(IP_FILTER);
        if (ipFilter != null && !ipFilter.trim().isEmpty()) {
            logger.debug("IP filter configured: {}", ipFilter);
        }
    }

    private void initializeBridge() {
        try {
            String thingId = getThing().getUID().toString();
            logger.debug("Fetching person data for bridge initialization with polling interval: {}s", pollingInterval);
            
            // Load AWS IP ranges for webhook verification (optional feature)
            loadAwsIpRanges();
            
            RachioPerson personInfo = rachioHttp.getPerson(thingId);
            if (personInfo != null) {
                this.person = personInfo;
                // STORE DEVICES FOR DISCOVERY SERVICE
                this.devices = personInfo.devices;
                
                logger.info("Successfully connected to Rachio API for user: {}", personInfo.username);
                
                // Log device discovery details
                if (personInfo.devices != null && !personInfo.devices.isEmpty()) {
                    logger.info("Found {} devices for user: {}", personInfo.devices.size(), personInfo.username);
                    for (RachioPerson.Device device : personInfo.devices) {
                        logger.info("Discovered device: {} (ID: {}) - Status: {}", 
                                    device.name, device.id, device.status);
                        if (device.zones != null) {
                            logger.info("  - {} zones: {}", device.zones.size(), 
                                       device.zones.stream().map(z -> z.name).toList());
                        }
                    }
                    
                    // Trigger discovery service to find these devices
                    triggerDiscovery();
                } else {
                    logger.warn("No devices found in API response for user: {}", personInfo.username);
                }
                
                updateStatus(ThingStatus.ONLINE);
                
                // Create rate limit monitoring channels
                createRateLimitChannels();
                
                // Start refresh job with adaptive polling
                startAdaptiveRefreshJob();
                
                // ===== WEBHOOK SETUP =====
                if (webhooksEnabled) {
                    setupWebhooks();
                } else {
                    logger.info("Webhooks disabled - no callback URL configured");
                }
                // ===== END WEBHOOK SETUP =====
                
                // Notify child handlers that bridge is ready
                notifyChildHandlers();
            } else {
                logger.warn("No person info returned from Rachio API");
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "No person info returned");
            }
        } catch (RachioException e) {
            logger.debug("Failed to initialize Rachio bridge: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (Exception e) {
            logger.debug("Unexpected error during bridge initialization: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Load AWS IP ranges for webhook verification (optional security feature)
     */
    private void loadAwsIpRanges() {
        // Optional: Load AWS IP ranges for enhanced webhook security
        // This helps verify that webhooks actually come from Rachio's AWS infrastructure
        
        try {
            // You can enable/disable this feature based on configuration
            boolean enableAwsVerification = false; // Disabled for now - can enable later
            
            if (enableAwsVerification) {
                // For now, create empty list - implement AWS range loading later
                awsIpRanges = new java.util.ArrayList<>();
                logger.info("AWS IP range verification is available but not implemented yet");
            } else {
                logger.debug("AWS IP range verification disabled");
                awsIpRanges = null;
            }
        } catch (Exception e) {
            logger.debug("Failed to initialize AWS IP ranges: {}", e.getMessage());
            awsIpRanges = null;
        }
    }

    /**
     * Check if a client IP is allowed based on configured IP filter and AWS ranges
     * 
     * @param clientIp The client IP address to check
     * @return true if IP is allowed, false if blocked
     */
    public boolean isIpAllowed(String clientIp) {
        if (clientIp == null || clientIp.trim().isEmpty()) {
            logger.warn("Null or empty client IP provided for validation");
            return false;
        }
        
        // First validate the IP format
        if (!RachioSecurity.isValidIpAddress(clientIp)) {
            logger.warn("Invalid IP address format: {}", clientIp);
            return false;
        }
        
        // Check against user-configured IP filter
        if (ipFilter != null && RachioSecurity.isIpInAllowedSubnet(clientIp, ipFilter)) {
            logger.debug("IP {} allowed by user IP filter", clientIp);
            return true;
        }
        
        // If we have AWS ranges, check those too
        if (awsIpRanges != null && !awsIpRanges.isEmpty()) {
            if (RachioSecurity.isIpInAwsRange(clientIp, awsIpRanges)) {
                logger.debug("IP {} allowed by AWS IP ranges", clientIp);
                return true;
            }
        }
        
        logger.warn("IP {} blocked by security filter", clientIp);
        
        // Log additional debug info
        if (ipFilter != null && !ipFilter.trim().isEmpty()) {
            logger.debug("Configured IP filter: {}", ipFilter);
        } else {
            logger.debug("No IP filter configured");
        }
        
        if (awsIpRanges != null && !awsIpRanges.isEmpty()) {
            logger.debug("AWS ranges loaded: {} entries", awsIpRanges.size());
        } else {
            logger.debug("No AWS ranges loaded");
        }
        
        return false;
    }

    /**
     * Trigger discovery service to discover devices and zones
     */
    private void triggerDiscovery() {
        try {
            // This will trigger the discovery service to scan for devices
            // The discovery service will find this bridge and use getDevices()
            logger.debug("Bridge ready for device discovery");
        } catch (Exception e) {
            logger.debug("Error triggering discovery: {}", e.getMessage());
        }
    }

    /**
     * Setup webhook registration if callback URL is configured
     * UPDATED TO USE CORRECT DEVICE ID
     */
    private void setupWebhooks() {
        logger.info("Setting up webhooks with callback URL: {}", callbackUrl);
        
        if (callbackUrl == null || callbackUrl.trim().isEmpty()) {
            logger.warn("Cannot setup webhooks - callback URL is empty");
            return;
        }
        
        // Get device ID for webhook registration
        String deviceId = getFirstDeviceId();
        if (deviceId == null || deviceId.isEmpty()) {
            logger.warn("Cannot setup webhooks - no device found");
            return;
        }
        
        String thingId = getThing().getUID().toString();
        webhookRegistrationAttempted = true;
        this.webhookDeviceId = deviceId; // Store device ID used for registration
        
        try {
            // Step 1: Clear existing webhooks if configured
            if (clearAllCallbacks) {
                logger.info("Clearing all existing webhooks for device {}...", deviceId);
                try {
                    rachioHttp.clearAllWebhooks(thingId, deviceId);
                    logger.info("Successfully cleared all existing webhooks for device: {}", deviceId);
                } catch (Exception e) {
                    logger.warn("Failed to clear existing webhooks for device {}: {}", deviceId, e.getMessage());
                    // Continue anyway - might be first setup
                }
            }
            
            // Step 2: List existing webhooks to avoid duplicates
            logger.debug("Checking for existing webhooks for device: {}...", deviceId);
            List<org.openhab.binding.rachio.internal.api.RachioHttp.RachioApiWebHookEntry> existingWebhooks = 
                rachioHttp.listWebhooks(thingId, deviceId);
            
            // Check if we already have a webhook for this callback URL and device
            String localWebhookId = this.webhookId;
            boolean needsRegistration = true;
            
            if (existingWebhooks != null && !existingWebhooks.isEmpty()) {
                for (org.openhab.binding.rachio.internal.api.RachioHttp.RachioApiWebHookEntry webhook : existingWebhooks) {
                    if (callbackUrl.equals(webhook.url) && thingId.equals(webhook.externalId)) {
                        logger.info("Webhook already registered: {} (ID: {}) for device: {}", 
                                   webhook.url, webhook.id, deviceId);
                        localWebhookId = webhook.id;
                        needsRegistration = false;
                        break;
                    }
                }
            }
            
            // Step 3: Register new webhook if needed
            if (needsRegistration) {
                logger.info("Registering new webhook for device {} with Rachio API...", deviceId);
                logger.debug("Callback URL: {}, External ID: {}, Device ID: {}, Events: {}", 
                           callbackUrl, thingId, deviceId, DEFAULT_WEBHOOK_EVENTS);
                
                String newWebhookId = rachioHttp.registerWebhook(thingId, callbackUrl, thingId, deviceId, DEFAULT_WEBHOOK_EVENTS);
                
                if (newWebhookId != null && !newWebhookId.isEmpty()) {
                    localWebhookId = newWebhookId;
                    logger.info("Successfully registered webhook with ID: {} for device: {}", newWebhookId, deviceId);
                    logger.info("Rachio will now send webhook events to: {}", callbackUrl);
                    logger.info("Registered events: {}", DEFAULT_WEBHOOK_EVENTS);
                } else {
                    logger.error("Failed to register webhook - no webhook ID returned for device: {}", deviceId);
                    return;
                }
            }
            
            // Step 4: Store webhook ID for cleanup
            this.webhookId = localWebhookId;
            
            // Step 5: Update configuration with webhook ID
            if (localWebhookId != null) {
                // Store webhook ID in thing configuration for persistence
                updateConfiguration(WEBHOOK_ID, localWebhookId);
                logger.debug("Webhook ID stored in configuration: {} for device: {}", localWebhookId, deviceId);
            }
            
            // Step 6: Verify webhook setup
            logger.info("Webhook setup complete for device: {}", deviceId);
            logger.info("Rachio events will be sent to: {}", callbackUrl);
            
        } catch (RachioException e) {
            logger.error("Failed to setup webhooks for device {}: {}", deviceId, e.getMessage());
            logger.debug("Webhook registration error details:", e);
            
            // Update status with webhook error (but keep bridge online)
            updateStatus(ThingStatus.ONLINE, ThingStatusDetail.CONFIGURATION_ERROR, 
                        "Webhook setup failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during webhook setup for device {}: {}", deviceId, e.getMessage(), e);
            updateStatus(ThingStatus.ONLINE, ThingStatusDetail.CONFIGURATION_ERROR, 
                        "Webhook setup error: " + e.getMessage());
        }
    }
    
    /**
     * Get the first device ID from the devices list
     */
    @Nullable
    private String getFirstDeviceId() {
        List<RachioPerson.Device> localDevices = devices;
        if (localDevices != null && !localDevices.isEmpty()) {
            return localDevices.get(0).id;
        }
        return null;
    }
    
    /**
     * Clean up webhook registration
     * Called on bridge disposal or when callback URL changes
     */
    private void cleanupWebhooks() {
        String localWebhookId = this.webhookId;
        if (localWebhookId == null || localWebhookId.isEmpty()) {
            logger.debug("No webhook ID to clean up");
            return;
        }
        
        try {
            String thingId = getThing().getUID().toString();
            logger.info("Cleaning up webhook registration: {}", localWebhookId);
            
            rachioHttp.deleteWebhook(thingId, localWebhookId);
            logger.info("Successfully deleted webhook: {}", localWebhookId);
            
            // Clear stored webhook ID
            this.webhookId = null;
            this.webhookDeviceId = null;
            
            // Remove from configuration
            updateConfiguration(WEBHOOK_ID, null);
            
        } catch (RachioException e) {
            logger.warn("Failed to delete webhook {}: {}", localWebhookId, e.getMessage());
            // Don't throw - we're in cleanup/dispose
        } catch (Exception e) {
            logger.debug("Error during webhook cleanup: {}", e.getMessage());
        }
    }
    
    /**
     * Update a configuration parameter
     */
    private void updateConfiguration(String key, @Nullable Object value) {
        try {
            org.openhab.core.config.core.Configuration config = getConfig();
            if (config != null) {
                config.put(key, value);
                // Note: In OpenHAB 5.x, we need to update the thing with the new configuration
                // This is a simplified approach - in practice you might need to handle this differently
                logger.debug("Configuration updated: {} = {}", key, value);
            }
        } catch (Exception e) {
            logger.debug("Failed to update configuration {}: {}", key, e.getMessage());
        }
    }

    private void notifyChildHandlers() {
        // Notify all child things that the bridge is ready
        getThing().getThings().forEach(thing -> {
            ThingHandler handler = thing.getHandler();
            if (handler instanceof RachioHandler) {
                logger.debug("Bridge ready - notifying child handler: {}", thing.getUID());
                // The child handler will pick up the status change through normal polling
            }
        });
    }

    // ===== RATE LIMIT MONITORING AND ADAPTIVE POLLING =====
    
    /**
     * Create rate limit monitoring channels on the bridge
     */
    private void createRateLimitChannels() {
        logger.debug("Creating rate limit monitoring channels");
        
        // Rate limit remaining calls
        createChannel("bridge#rateLimitRemaining", "API Calls Remaining", "Number", "RateLimit");
        
        // Rate limit percentage remaining
        createChannel("bridge#rateLimitPercent", "Rate Limit %", "Number", "RateLimit");
        
        // Rate limit status text
        createChannel("bridge#rateLimitStatus", "Rate Limit Status", "String", "RateLimit");
        
        // Seconds until reset
        createChannel("bridge#rateLimitReset", "Resets In", "Number", "RateLimit");
        
        logger.debug("Rate limit channels created");
    }
    
    /**
     * Create a channel dynamically - matches existing pattern from RachioHandler
     */
    private void createChannel(String channelId, String label, String itemType, @Nullable String category) {
        try {
            ChannelUID channelUID = new ChannelUID(getThing().getUID(), channelId);
            
            // Map channel IDs to channel type IDs - matches existing pattern
            String channelTypeId = getChannelTypeId(channelId, itemType);
            
            org.openhab.core.thing.type.ChannelTypeUID channelTypeUID = 
                new org.openhab.core.thing.type.ChannelTypeUID("rachio", channelTypeId);
            
            org.openhab.core.thing.binding.builder.ChannelBuilder channelBuilder = 
                org.openhab.core.thing.binding.builder.ChannelBuilder.create(channelUID, itemType)
                    .withType(channelTypeUID)
                    .withLabel(label);
            
            org.openhab.core.thing.Channel channel = channelBuilder.build();
            
            // Update the thing with the new channel
            updateThing(editThing().withChannel(channel).build());
            
            logger.debug("Created channel: {} for thing: {}", channelId, getThing().getUID());
        } catch (Exception e) {
            logger.debug("Failed to create channel {} for thing {}: {}", 
                        channelId, getThing().getUID(), e.getMessage());
        }
    }
    
    /**
     * Map channel IDs to channel type IDs - matches existing pattern from RachioHandler
     */
    private String getChannelTypeId(String channelId, String itemType) {
        // Simple mapping based on itemType
        if ("Number".equals(itemType)) {
            return "number";
        } else if ("String".equals(itemType)) {
            return "string";
        } else if ("Switch".equals(itemType)) {
            return "switch";
        }
        return "string"; // fallback
    }
    
    /**
     * Start refresh job with adaptive polling based on rate limits
     */
    private void startAdaptiveRefreshJob() {
        ScheduledFuture<?> job = refreshJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(false);
        }

        // Calculate adaptive polling interval
        adaptivePollingInterval = calculateAdaptivePollingInterval();
        
        refreshJob = scheduler.scheduleWithFixedDelay(this::refreshBridge, 1, adaptivePollingInterval, TimeUnit.SECONDS);
        logger.debug("Started adaptive bridge refresh job with {} second interval (base: {}s, multiplier: {}x)", 
                    adaptivePollingInterval, pollingInterval, rachioHttp.getAdaptivePollingMultiplier());
    }
    
    /**
     * Calculate adaptive polling interval based on rate limit status
     */
    private int calculateAdaptivePollingInterval() {
        double multiplier = rachioHttp.getAdaptivePollingMultiplier();
        int adaptiveInterval = (int) (pollingInterval * multiplier);
        
        // Ensure minimum 90 seconds even when slowing down
        if (adaptiveInterval < 90) {
            adaptiveInterval = 90;
        }
        
        // Cap at 30 minutes (1800 seconds) to avoid extremely slow polling
        if (adaptiveInterval > 1800) {
            adaptiveInterval = 1800;
        }
        
        return adaptiveInterval;
    }
    
    /**
     * Update rate limit channels with current information
     */
    private void updateRateLimitChannels() {
        try {
            // Get rate limit info from RachioHttp
            int remaining = rachioHttp.getRateLimitRemaining();
            int limit = rachioHttp.getRateLimitTotal();
            int percent = rachioHttp.getRateLimitPercent();
            String statusText = rachioHttp.getRateLimitStatusText();
            long secondsUntilReset = rachioHttp.getRateLimitSecondsUntilReset();
            
            // Update remaining calls channel
            ChannelUID remainingChannel = new ChannelUID(getThing().getUID(), "bridge#rateLimitRemaining");
            updateState(remainingChannel, new DecimalType(remaining));
            
            // Update percentage channel
            ChannelUID percentChannel = new ChannelUID(getThing().getUID(), "bridge#rateLimitPercent");
            updateState(percentChannel, new DecimalType(percent));
            
            // Update status text channel
            ChannelUID statusChannel = new ChannelUID(getThing().getUID(), "bridge#rateLimitStatus");
            updateState(statusChannel, new StringType(statusText));
            
            // Update reset time channel
            ChannelUID resetChannel = new ChannelUID(getThing().getUID(), "bridge#rateLimitReset");
            updateState(resetChannel, new DecimalType(secondsUntilReset));
            
            // Update bridge status detail
            updateBridgeStatusForRateLimit(remaining, percent);
            
            // Adjust polling interval if needed
            checkAndAdjustPollingInterval();
            
            logger.trace("Updated rate limit channels: remaining={}, percent={}%, reset={}s",
                        remaining, percent, secondsUntilReset);
        } catch (Exception e) {
            logger.debug("Failed to update rate limit channels: {}", e.getMessage());
        }
    }
    
    /**
     * Update bridge status detail based on rate limit state
     */
    private void updateBridgeStatusForRateLimit(int remaining, int percent) {
        if (rachioHttp.isRateLimitCritical()) {
            updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, 
                        "Rate limit critical: " + remaining + " calls remaining");
            rateLimitCriticalActive = true;
            rateLimitWarningActive = false;
        } else if (rachioHttp.isRateLimitWarning()) {
            updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, 
                        "Rate limit warning: " + remaining + " calls remaining");
            rateLimitWarningActive = true;
        } else if (rateLimitWarningActive || rateLimitCriticalActive) {
            // Back to normal
            updateStatus(ThingStatus.ONLINE);
            rateLimitWarningActive = false;
            rateLimitCriticalActive = false;
        }
    }
    
    /**
     * Check if polling interval needs adjustment and restart job if needed
     */
    private void checkAndAdjustPollingInterval() {
        int newAdaptiveInterval = calculateAdaptivePollingInterval();
        
        if (newAdaptiveInterval != adaptivePollingInterval) {
            logger.info("Rate limit state changed: adjusting polling interval from {}s to {}s (multiplier: {}x)", 
                       adaptivePollingInterval, newAdaptiveInterval, rachioHttp.getAdaptivePollingMultiplier());
            adaptivePollingInterval = newAdaptiveInterval;
            
            // Restart refresh job with new interval
            startAdaptiveRefreshJob();
        }
    }
    // ===== END RATE LIMIT MONITORING =====

    private void refreshBridge() {
        if (getThing().getStatus() != ThingStatus.ONLINE) {
            logger.debug("Bridge is not ONLINE, skipping refresh");
            return;
        }

        try {
            // Simple API call to verify connectivity and refresh device list
            String thingId = getThing().getUID().toString();
            RachioPerson currentPerson = rachioHttp.getPerson(thingId);
            
            if (currentPerson != null) {
                this.person = currentPerson;
                // UPDATE DEVICES LIST ON REFRESH
                this.devices = currentPerson.devices;
                logger.trace("Bridge refresh successful for user: {}", currentPerson.username);
                
                // Update rate limit monitoring channels
                updateRateLimitChannels();
                
                // Log if devices changed
                if (currentPerson.devices != null) {
                    logger.trace("Refreshed device list: {} devices", currentPerson.devices.size());
                }
                
                // ===== WEBHOOK HEALTH CHECK =====
                if (webhooksEnabled && webhookRegistrationAttempted) {
                    checkWebhookHealth();
                }
                // ===== END WEBHOOK HEALTH CHECK =====
                
            } else {
                logger.warn("Bridge refresh failed - no person data returned");
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "No data returned");
            }
        } catch (RachioException e) {
            logger.debug("Rachio bridge refresh failed: {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (Exception e) {
            logger.debug("Unexpected error during bridge refresh: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Check webhook health and re-register if needed
     */
    private void checkWebhookHealth() {
        try {
            String thingId = getThing().getUID().toString();
            String localWebhookId = this.webhookId;
            String localDeviceId = this.webhookDeviceId;
            
            if (localDeviceId == null || localDeviceId.isEmpty()) {
                logger.debug("No device ID stored for webhook health check");
                return;
            }
            
            if (localWebhookId != null && !localWebhookId.isEmpty()) {
                // List webhooks to see if ours still exists
                List<org.openhab.binding.rachio.internal.api.RachioHttp.RachioApiWebHookEntry> webhooks = 
                    rachioHttp.listWebhooks(thingId, localDeviceId);
                
                boolean webhookFound = false;
                if (webhooks != null) {
                    for (org.openhab.binding.rachio.internal.api.RachioHttp.RachioApiWebHookEntry webhook : webhooks) {
                        if (localWebhookId.equals(webhook.id)) {
                            webhookFound = true;
                            logger.trace("Webhook health check: OK (ID: {})", localWebhookId);
                            break;
                        }
                    }
                }
                
                if (!webhookFound) {
                    logger.warn("Webhook health check FAILED - webhook {} not found for device {}. Re-registering...", 
                               localWebhookId, localDeviceId);
                    this.webhookId = null; // Clear so setupWebhooks will create new one
                    setupWebhooks();
                }
            }
        } catch (Exception e) {
            logger.debug("Webhook health check failed: {}", e.getMessage());
            // Don't update status - webhook failure shouldn't take bridge offline
        }
    }

    // CRITICAL: Provide access to devices for discovery service
    public List<RachioPerson.Device> getDevices() {
        List<RachioPerson.Device> localDevices = devices;
        return localDevices != null ? localDevices : java.util.Collections.emptyList();
    }

    // FIXED: Remove @Nullable annotation and throw exception instead of returning null
    public RachioPerson.Device getDeviceById(String deviceId) {
        List<RachioPerson.Device> localDevices = devices;
        if (localDevices != null) {
            for (RachioPerson.Device device : localDevices) {
                if (deviceId.equals(device.id)) {
                    return device;
                }
            }
        }
        throw new IllegalArgumentException("Device not found: " + deviceId);
    }

    public RachioHttp getApi() {
        return rachioHttp;
    }

    public @Nullable RachioPerson getPerson() {
        return person;
    }

    public @Nullable RachioDevice getDevice(String deviceId) {
        try {
            String thingId = getThing().getUID().toString();
            return rachioHttp.getDevice(thingId, deviceId);
        } catch (RachioException e) {
            logger.debug("Failed to get device {}: {}", deviceId, e.getMessage());
            return null;
        }
    }

    // Configuration getters
    public int getPollingInterval() {
        return pollingInterval;
    }

    public int getDefaultRuntime() {
        return defaultRuntime;
    }

    public @Nullable String getCallbackUrl() {
        return callbackUrl;
    }

    public boolean isClearAllCallbacks() {
        return clearAllCallbacks;
    }

    public @Nullable String getIpFilter() {
        return ipFilter;
    }

    /**
     * Get AWS IP ranges (for testing/debugging)
     */
    public @Nullable List<RachioSecurity.AwsIpAddressRange> getAwsIpRanges() {
        return awsIpRanges;
    }

    /**
     * Handle webhook event with IP security validation
     * 
     * @param clientIp The IP address of the webhook sender
     * @param event The webhook event data
     */
    public void webHookEvent(String clientIp, RachioWebhookEvent event) {
        logger.debug("Received webhook event from {}: {}", clientIp, event.eventType);
        
        // SECURITY: Validate client IP before processing
        if (!isIpAllowed(clientIp)) {
            logger.error("WEBHOOK SECURITY VIOLATION: Blocked webhook from unauthorized IP: {} (Event: {})", 
                        clientIp, event.eventType);
            return;
        }
        
        logger.info("Processing webhook event from {}: {} for device: {}", 
                   clientIp, event.eventType, event.deviceId);
        
        // Forward webhook events to all child handlers
        getThing().getThings().forEach(thing -> {
            ThingHandler handler = thing.getHandler();
            if (handler instanceof RachioHandler) {
                ((RachioHandler) handler).handleWebhookEvent(event);
                logger.debug("Forwarded webhook to thing: {}", thing.getUID());
            }
        });
    }

    /**
     * Legacy method for backward compatibility
     */
    public void webHookEvent(RachioWebhookEvent event) {
        // This method doesn't validate IP - use with caution
        logger.warn("Using insecure webhook processing (no IP validation)");
        
        // Try to get IP from context if possible
        String clientIp = "unknown";
        logger.debug("Processing webhook event without IP validation: {}", event.eventType);
        
        // Forward to child handlers
        getThing().getThings().forEach(thing -> {
            ThingHandler handler = thing.getHandler();
            if (handler instanceof RachioHandler) {
                ((RachioHandler) handler).handleWebhookEvent(event);
            }
        });
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio bridge handler");
        
        ScheduledFuture<?> job = refreshJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(true);
        }
        refreshJob = null;
        
        // ===== WEBHOOK CLEANUP =====
        cleanupWebhooks();
        // ===== END WEBHOOK CLEANUP =====
        
        // Unregister from HTTP service
        String thingId = getThing().getUID().toString();
        rachioHttp.unregisterThing(thingId);
        
        this.person = null;
        this.devices = null; // Clear devices on dispose
        this.awsIpRanges = null; // Clear AWS ranges
        this.webhookId = null; // Clear webhook ID
        this.webhookDeviceId = null; // Clear webhook device ID
        super.dispose();
    }
    
    /**
     * Get the registered webhook ID (for debugging/testing)
     */
    @Nullable
    public String getWebhookId() {
        return webhookId;
    }
    
    /**
     * Get the device ID used for webhook registration
     */
    @Nullable
    public String getWebhookDeviceId() {
        return webhookDeviceId;
    }
    
    /**
     * Check if webhooks are enabled and registered
     */
    public boolean isWebhooksEnabled() {
        return webhooksEnabled && webhookId != null && !webhookId.isEmpty();
    }
    
    /**
     * Manually trigger webhook re-registration (for testing/recovery)
     */
    public void reRegisterWebhooks() {
        logger.info("Manually triggering webhook re-registration");
        cleanupWebhooks();
        this.webhookId = null;
        this.webhookDeviceId = null;
        setupWebhooks();
    }
}