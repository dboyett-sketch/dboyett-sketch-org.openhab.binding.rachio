package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.type.ChannelTypeUID;
// FIX: Correct ChannelBuilder import
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The {@link RachioHandler} is the base class for Rachio thing handlers.
 *
 * @author Damion
 */
@NonNullByDefault
public abstract class RachioHandler extends BaseThingHandler {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    protected RachioHttp rachioHttp;
    protected @Nullable String deviceId;
    private @Nullable ScheduledFuture<?> statusUpdateJob;

    public RachioHandler(Thing thing) {
        super(thing);
        this.rachioHttp = new RachioHttp(); // Fallback - will be overridden if bridge handler is available
        logger.debug("RachioHandler created for thing: {}", thing.getUID());
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio handler for thing: {}", getThing().getUID());
        
        // Try to get bridge handler and use its API instance
        RachioBridgeHandler bridgeHandler = getRachioBridgeHandler();
        if (bridgeHandler != null) {
            this.rachioHttp = bridgeHandler.getApi();
            logger.debug("Using bridge handler API for thing: {}", getThing().getUID());
            
            // Check if bridge is online before proceeding
            if (bridgeHandler.getThing().getStatus() != ThingStatus.ONLINE) {
                logger.warn("Bridge is not ONLINE for thing: {}, waiting for bridge", getThing().getUID());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge is offline");
                return;
            }
        } else {
            logger.warn("No bridge handler found for thing: {}, using standalone API (may not work)", getThing().getUID());
            // FIX: Don't try to register device things with their own API key
            // Device things should always have a bridge
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "No bridge found");
            return;
        }
        
        // Create channels dynamically for OpenHAB 5.x
        createChannels();
        
        // Set initial status to UNKNOWN while we check connectivity
        updateStatus(ThingStatus.UNKNOWN);
        
        // Start status update job after a short delay to ensure thing is registered
        scheduler.schedule(this::startStatusUpdates, 2, TimeUnit.SECONDS);
    }

    /**
     * Create channels dynamically - to be implemented by child classes
     */
    protected abstract void createChannels();

    /**
     * Create a channel with the specified parameters
     */
    protected void createChannel(String channelId, String label, String itemType, @Nullable String category) {
        ThingHandlerCallback callback = getCallback();
        if (callback != null) {
            ChannelUID channelUID = new ChannelUID(getThing().getUID(), channelId);
            ChannelTypeUID channelTypeUID = new ChannelTypeUID("rachio", getChannelTypeId(channelId));
            
            // FIX: ChannelBuilder now correctly imported
            ChannelBuilder channelBuilder = ChannelBuilder.create(channelUID, itemType)
                    .withType(channelTypeUID)
                    .withLabel(label);
            
            // FIX: Category is no longer supported in ChannelBuilder in OpenHAB 5.x
            // Remove the category parameter entirely
            
            Channel channel = channelBuilder.build();
            
            // Update the thing with the new channel
            Thing updatedThing = editThing().withChannel(channel).build();
            updateThing(updatedThing);
            
            logger.debug("Created channel: {} for thing: {}", channelId, getThing().getUID());
        } else {
            logger.warn("Cannot create channel {}, callback not available for thing: {}", channelId, getThing().getUID());
        }
    }

    /**
     * Map channel IDs to channel type IDs
     */
    private String getChannelTypeId(String channelId) {
        switch (channelId) {
            case CHANNEL_START_ALL_ZONES:
            case CHANNEL_START_NEXT_ZONE:
            case CHANNEL_STOP_WATERING:
            case ZONE_ENABLED:
                return "switch";
            case CHANNEL_RAIN_DELAY:
            case CHANNEL_START_ZONE:
            case ZONE_RUNTIME:
                return "number";
            case "device#status":
            case ZONE_NAME:
                return "string";
            default:
                return "string"; // fallback
        }
    }

    /**
     * Get the Rachio bridge handler if available
     */
    @Nullable
    protected RachioBridgeHandler getRachioBridgeHandler() {
        try {
            if (getBridge() != null && getBridge().getHandler() instanceof RachioBridgeHandler) {
                return (RachioBridgeHandler) getBridge().getHandler();
            }
        } catch (Exception e) {
            logger.debug("Error getting bridge handler for thing {}: {}", getThing().getUID(), e.getMessage());
        }
        return null;
    }

    private void startStatusUpdates() {
        // Cancel existing job if any
        ScheduledFuture<?> job = statusUpdateJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(true);
        }

        // Start polling for status updates every 60 seconds
        statusUpdateJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                updateDeviceStatus();
            } catch (Exception e) {
                logger.debug("Error during status update for thing {}: {}", getThing().getUID(), e.getMessage());
            }
        }, 5, 60, TimeUnit.SECONDS);

        logger.debug("Started status update job for thing: {}", getThing().getUID());
    }

    /**
     * Abstract method to update device-specific status
     */
    protected abstract void updateDeviceStatus();

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received command {} for channel {} on thing: {}", command, channelUID.getId(), getThing().getUID());
        
        if (command instanceof RefreshType) {
            // Handle refresh command by updating status
            updateDeviceStatus();
            return;
        }
        
        // Specific command handling is implemented in child classes
    }

    /**
     * Handle webhook events from Rachio - UPDATED WITH CORRECT FIELDS
     */
    public void handleWebhookEvent(RachioWebhookEvent event) {
        logger.debug("Received webhook event: {} for device: {} on thing: {}", 
                    event.eventType, event.deviceId, getThing().getUID());
        
        // Check if this event is for our device
        String localDeviceId = getDeviceId();
        if (localDeviceId != null && localDeviceId.equals(event.deviceId)) {
            // Update status to show we're processing webhooks
            updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, "Webhook: " + event.eventType);
            
            // Log detailed webhook information using actual fields
            logWebhookEventDetails(event);
            
            // Trigger a status update when we receive a relevant webhook
            scheduler.schedule(() -> {
                try {
                    updateDeviceStatus();
                } catch (Exception e) {
                    logger.debug("Error during webhook-triggered status update for thing {}: {}", 
                               getThing().getUID(), e.getMessage());
                }
            }, 1, TimeUnit.SECONDS); // Small delay to allow Rachio API to update
            
            logger.debug("Webhook event processed for thing: {}", getThing().getUID());
        } else {
            logger.trace("Webhook event ignored - device ID mismatch. Our: {}, Event: {}", 
                        localDeviceId, event.deviceId);
        }
    }
    
    /**
     * Log detailed webhook event information - UPDATED WITH CORRECT FIELDS
     */
    private void logWebhookEventDetails(RachioWebhookEvent event) {
        try {
            StringBuilder details = new StringBuilder();
            details.append("Webhook Event - Type: ").append(event.eventType);
            details.append(", Device: ").append(event.deviceId);
            details.append(", Time: ").append(event.timestamp);
            
            if (event.summary != null && !event.summary.isEmpty()) {
                details.append(", Summary: ").append(event.summary);
            }
            
            // Log device details if available
            if (event.device != null) {
                details.append(", Device Name: ").append(event.device.name);
                details.append(", Status: ").append(event.device.status);
                details.append(", On: ").append(event.device.on);
            }
            
            // Log zone details if available
            if (event.zone != null) {
                details.append(", Zone: ").append(event.zone.name);
                details.append(" (#").append(event.zone.zoneNumber).append(")");
                details.append(", Duration: ").append(event.zone.duration).append("s");
                details.append(", Zone Status: ").append(event.zone.status);
            }
            
            // Log notifier details if available
            if (event.notifier != null) {
                details.append(", Notifier: ").append(event.notifier.type);
                if (event.notifier.summary != null && !event.notifier.summary.isEmpty()) {
                    details.append(" (").append(event.notifier.summary).append(")");
                }
            }
            
            logger.info(details.toString());
            
        } catch (Exception e) {
            logger.debug("Error logging webhook details: {}", e.getMessage());
        }
    }

    /**
     * Set the device ID for this handler
     */
    protected void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        logger.debug("Device ID set to: {} for thing: {}", deviceId, getThing().getUID());
    }

    /**
     * Get the device ID for this handler
     */
    @Nullable
    protected String getDeviceId() {
        return deviceId;
    }

    /**
     * Update thing status based on API connectivity
     */
    protected void updateThingStatus(boolean connected, @Nullable String message) {
        if (connected) {
            updateStatus(ThingStatus.ONLINE);
            logger.debug("Thing status set to ONLINE for thing: {}", getThing().getUID());
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, 
                        message != null ? message : "Communication error");
            logger.debug("Thing status set to OFFLINE for thing: {} - {}", getThing().getUID(), message);
        }
    }

    // ===== SERVLET METHODS =====

    /**
     * Check if this handler handles the specified device ID
     * Used by RachioWebHookServlet
     */
    public boolean handlesDevice(String deviceId) {
        String localDeviceId = getDeviceId();
        boolean handles = localDeviceId != null && localDeviceId.equals(deviceId);
        logger.debug("Handler check for device {}: {} on thing: {}", deviceId, handles, getThing().getUID());
        return handles;
    }

    /**
     * Handle webhook call from RachioWebHookServlet - IMPROVED VERSION
     */
    public void handleWebhookCall(javax.servlet.http.HttpServletRequest request) {
        logger.debug("Handling webhook call for device: {} on thing: {}", getDeviceId(), getThing().getUID());
        
        try {
            // Get client IP for logging/security
            String clientIp = request.getRemoteAddr();
            logger.debug("Webhook from IP: {}", clientIp);
            
            // Check if bridge has IP filtering enabled
            RachioBridgeHandler bridgeHandler = getRachioBridgeHandler();
            if (bridgeHandler != null) {
                // Validate IP if IP filtering is configured
                String ipFilter = bridgeHandler.getIpFilter();
                if (ipFilter != null && !ipFilter.isEmpty()) {
                    if (!bridgeHandler.isIpAllowed(clientIp)) {
                        logger.warn("Blocked webhook from unauthorized IP: {} for thing: {}", 
                                   clientIp, getThing().getUID());
                        return;
                    }
                }
            }
            
            // This method is called by the servlet - implementation can be extended as needed
            // The actual webhook processing is done in handleWebhookEvent()
            
            // For now, just trigger a status update
            scheduler.execute(this::updateDeviceStatus);
            
        } catch (Exception e) {
            logger.debug("Error handling webhook call for thing {}: {}", getThing().getUID(), e.getMessage());
        }
    }

    /**
     * Handle image call from RachioImageServlet
     */
    public void handleImageCall(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) {
        logger.debug("Handling image call for device: {} on thing: {}", getDeviceId(), getThing().getUID());
        // This is a placeholder - image handling can be implemented if needed
        try {
            response.setStatus(javax.servlet.http.HttpServletResponse.SC_NOT_IMPLEMENTED);
            response.getWriter().write("Image handling not implemented for thing: " + getThing().getUID());
        } catch (java.io.IOException e) {
            logger.debug("Error handling image call for thing {}: {}", getThing().getUID(), e.getMessage());
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio handler for thing: {}", getThing().getUID());
        
        // Cancel status update job
        ScheduledFuture<?> job = statusUpdateJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(true);
        }
        statusUpdateJob = null;
        
        this.deviceId = null;
        super.dispose();
    }
    
    // ===== HELPER METHODS FOR WEBHOOK PROCESSING =====
    
    /**
     * Check if webhooks are enabled on the bridge
     */
    protected boolean areWebhooksEnabled() {
        RachioBridgeHandler bridgeHandler = getRachioBridgeHandler();
        if (bridgeHandler != null) {
            return bridgeHandler.isWebhooksEnabled();
        }
        return false;
    }
    
    /**
     * Get webhook status for debugging
     */
    protected String getWebhookStatus() {
        RachioBridgeHandler bridgeHandler = getRachioBridgeHandler();
        if (bridgeHandler != null) {
            String webhookId = bridgeHandler.getWebhookId();
            String deviceId = bridgeHandler.getWebhookDeviceId();
            if (webhookId != null && deviceId != null) {
                return String.format("Webhook ID: %s, Device: %s", webhookId, deviceId);
            }
        }
        return "Webhooks not configured";
    }
}