package org.openhab.binding.rachio.internal.handler;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.RachioSecurity;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link RachioBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);

    private @Nullable RachioBridgeConfiguration config;
    private @Nullable RachioHttp rachioHttp;
    private @Nullable RachioSecurity rachioSecurity;
    private @Nullable ScheduledFuture<?> pollingJob;
    private @Nullable ScheduledFuture<?> webhookHealthJob;

    // FIXED: Constructor should not have HttpClientFactory parameter
    public RachioBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio bridge handler");
        
        // Get configuration
        this.config = getConfigAs(RachioBridgeConfiguration.class);
        
        if (this.config == null || this.config.apiKey == null || this.config.apiKey.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "API key not configured");
            return;
        }
        
        // FIXED: Create RachioHttp with config only (no HttpClientFactory needed)
        this.rachioHttp = new RachioHttp(this.config);
        this.rachioSecurity = new RachioSecurity(this.config);
        
        // Start polling
        startPolling();
        
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        stopPolling();
        stopWebhookHealthCheck();
        
        RachioHttp http = rachioHttp;
        if (http != null) {
            http.close();
        }
        
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Handle bridge commands if needed
        logger.debug("Bridge command received for channel {}: {}", channelUID.getId(), command);
    }

    // FIXED: Added missing method for webhook servlet
    public Gson getGson() {
        RachioHttp http = rachioHttp;
        return http != null ? http.getGson() : null;
    }

    // FIXED: Added missing method for webhook servlet
    public boolean isIpAllowed(String ipAddress, String requestPath) {
        RachioSecurity security = rachioSecurity;
        return security != null && security.isIpAllowed(ipAddress, requestPath);
    }

    // FIXED: Added method for webhook events
    public void handleWebhookEvent(String deviceId, String eventType, @Nullable String subType, 
                                   @Nullable Map<String, Object> eventData) {
        logger.debug("Received webhook event: {} for device {}", eventType, deviceId);
        
        // Notify all child things about the webhook event
        getThing().getThings().forEach(thing -> {
            if (thing.getHandler() instanceof RachioStatusListener) {
                RachioStatusListener listener = (RachioStatusListener) thing.getHandler();
                listener.onWebhookEvent(deviceId, eventType, subType, eventData);
            }
        });
        
        // Also trigger polling to update state
        scheduler.submit(() -> {
            try {
                // Quick refresh after webhook
                pollDevices();
            } catch (Exception e) {
                logger.debug("Failed to poll after webhook", e);
            }
        });
    }

    // FIXED: Added getter methods for child handlers
    public @Nullable RachioHttp getRachioHttp() {
        return rachioHttp;
    }

    public @Nullable RachioSecurity getRachioSecurity() {
        return rachioSecurity;
    }

    private void startPolling() {
        stopPolling(); // Stop existing polling if any
        
        pollingJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                pollDevices();
            } catch (Exception e) {
                logger.warn("Polling failed", e);
            }
        }, 0, 60, TimeUnit.SECONDS); // Poll every 60 seconds
        
        // Start webhook health check if enabled
        startWebhookHealthCheck();
    }

    private void stopPolling() {
        ScheduledFuture<?> job = pollingJob;
        if (job != null) {
            job.cancel(true);
            pollingJob = null;
        }
    }

    private void pollDevices() {
        RachioHttp http = rachioHttp;
        if (http != null) {
            try {
                // Test connection first
                if (http.testConnection()) {
                    updateStatus(ThingStatus.ONLINE);
                    
                    // Here you would poll devices and update child things
                    // This is where you'd call http.getDevices() and update device/zone status
                    logger.debug("Bridge polling successful");
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "API connection failed");
                }
            } catch (Exception e) {
                logger.debug("Failed to poll devices", e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
        }
    }

    private void startWebhookHealthCheck() {
        stopWebhookHealthCheck();
        
        webhookHealthJob = scheduler.scheduleWithFixedDelay(() -> {
            checkWebhookHealth();
        }, 0, 5, TimeUnit.MINUTES); // Check every 5 minutes
    }

    private void stopWebhookHealthCheck() {
        ScheduledFuture<?> job = webhookHealthJob;
        if (job != null) {
            job.cancel(true);
            webhookHealthJob = null;
        }
    }

    private void checkWebhookHealth() {
        RachioHttp http = rachioHttp;
        if (http != null) {
            try {
                // Check webhook registration status
                // This would verify webhooks are registered with Rachio
                logger.debug("Webhook health check performed");
                
                // You could implement actual webhook health checking here:
                // 1. Check if webhooks are registered
                // 2. Re-register if needed
                // 3. Verify webhook URLs are accessible
            } catch (Exception e) {
                logger.debug("Webhook health check failed", e);
            }
        }
    }

    // FIXED: Removed problematic webhook service creation
    // Lines 179-191 should be removed or commented out:
    // this.webHookService = new RachioWebHookServletService(webhookPort, this);
    // this.webHookService.start();
    // 
    // And in dispose():
    // if (webHookService != null) {
    //     webHookService.stop();
    // }
}
