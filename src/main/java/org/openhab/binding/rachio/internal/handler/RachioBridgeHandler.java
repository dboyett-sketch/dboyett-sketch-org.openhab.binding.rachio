package org.openhab.binding.rachio.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.RachioSecurity;
import org.openhab.binding.rachio.internal.api.RachioWebHookServletService;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
import org.openhab.binding.rachio.internal.discovery.RachioDiscoveryService;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The {@link RachioBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
@Component(service = RachioBridgeHandler.class)
public class RachioBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);
    private final HttpClientFactory httpClientFactory;
    private final Map<String, RachioStatusListener> statusListeners = new ConcurrentHashMap<>();
    
    private @Nullable RachioHttp rachioHttp;
    private @Nullable RachioDiscoveryService discoveryService;
    private @Nullable RachioWebHookServletService webHookService;
    private @Nullable RachioSecurity security;
    private @Nullable ScheduledFuture<?> refreshJob;
    
    private RachioBridgeConfiguration config = new RachioBridgeConfiguration();

    @Activate
    public RachioBridgeHandler(Bridge bridge, @Reference HttpClientFactory httpClientFactory) {
        super(bridge);
        this.httpClientFactory = httpClientFactory;
        logger.debug("RachioBridgeHandler created for bridge: {}", bridge.getUID());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received command {} for channel {}", command, channelUID);
        
        if (command instanceof RefreshType) {
            // Handle refresh command
            updateBridgeStatus();
        } else {
            logger.warn("Unsupported command {} for channel {}", command, channelUID);
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio bridge handler");
        
        // Update configuration
        config = getConfigAs(RachioBridgeConfiguration.class);
        
        if (config.apiKey == null || config.apiKey.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, 
                "API Key is required");
            return;
        }
        
        logger.debug("Bridge configuration: apiKey={}, refresh={}, webhookPort={}, webhookEnabled={}",
            config.apiKey, config.refresh, config.webhookPort, config.webhookEnabled);
        
        // Initialize HTTP client
        try {
            rachioHttp = new RachioHttp(config.apiKey, httpClientFactory);
            security = new RachioSecurity();
            
            // Start webhook service if enabled
            if (config.webhookEnabled) {
                startWebHookService();
            }
            
            // Schedule refresh job
            scheduleRefresh();
            
            // Initialize bridge status
            updateBridgeStatus();
            
            updateStatus(ThingStatus.ONLINE);
            logger.info("Rachio bridge initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize Rachio bridge: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, 
                "Failed to initialize: " + e.getMessage());
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio bridge handler");
        
        // Stop refresh job
        if (refreshJob != null && !refreshJob.isCancelled()) {
            refreshJob.cancel(true);
            refreshJob = null;
        }
        
        // Stop webhook service
        stopWebHookService();
        
        // Clear status listeners
        statusListeners.clear();
        
        super.dispose();
    }

    private void scheduleRefresh() {
        if (refreshJob != null && !refreshJob.isCancelled()) {
            refreshJob.cancel(true);
        }
        
        int refreshInterval = config.refresh != null ? config.refresh : RachioBindingConstants.DEFAULT_REFRESH_INTERVAL;
        logger.debug("Scheduling refresh every {} seconds", refreshInterval);
        
        refreshJob = scheduler.scheduleWithFixedDelay(this::refresh, 5, refreshInterval, TimeUnit.SECONDS);
    }

    private void refresh() {
        try {
            logger.debug("Refreshing bridge status");
            updateBridgeStatus();
            
            // Refresh all child things
            getThing().getThings().forEach(thing -> {
                RachioStatusListener listener = statusListeners.get(thing.getUID().getId());
                if (listener != null) {
                    listener.refresh();
                }
            });
            
        } catch (Exception e) {
            logger.error("Error during refresh: {}", e.getMessage(), e);
        }
    }

    private void updateBridgeStatus() {
        // Update bridge status channels if needed
        // For now, just log
        logger.debug("Updating bridge status");
        
        // TODO: Update bridge-specific channels like heartbeat
    }

    private void startWebHookService() {
        logger.debug("Starting webhook service on port {}", config.webhookPort);
        
        try {
            int port = config.webhookPort != null ? config.webhookPort : RachioBindingConstants.DEFAULT_WEBHOOK_PORT;
            webHookService = new RachioWebHookServletService(port, this);
            webHookService.start();
            
            logger.info("Webhook service started on port {}", port);
        } catch (Exception e) {
            logger.error("Failed to start webhook service: {}", e.getMessage(), e);
        }
    }

    private void stopWebHookService() {
        if (webHookService != null) {
            try {
                webHookService.stop();
                logger.debug("Webhook service stopped");
            } catch (Exception e) {
                logger.error("Error stopping webhook service: {}", e.getMessage(), e);
            }
            webHookService = null;
        }
    }

    public void registerStatusListener(RachioStatusListener listener) {
        statusListeners.put(listener.getThing().getUID().getId(), listener);
        logger.debug("Registered status listener for thing: {}", listener.getThing().getUID());
    }

    public void unregisterStatusListener(RachioStatusListener listener) {
        statusListeners.remove(listener.getThing().getUID().getId());
        logger.debug("Unregistered status listener for thing: {}", listener.getThing().getUID());
    }

    public @Nullable RachioHttp getRachioHttp() {
        return rachioHttp;
    }

    public @Nullable RachioSecurity getSecurity() {
        return security;
    }

    public RachioBridgeConfiguration getBridgeConfiguration() {
        return config;
    }

    public void handleWebhookEvent(String eventType, String deviceId, @Nullable String zoneId, 
                                   @Nullable Map<String, Object> data) {
        logger.debug("Received webhook event: type={}, deviceId={}, zoneId={}", 
            eventType, deviceId, zoneId);
        
        // Notify relevant status listeners
        statusListeners.values().forEach(listener -> {
            listener.onWebhookEvent(eventType, deviceId, zoneId, data);
        });
    }

    public void setDiscoveryService(RachioDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    public @Nullable RachioDiscoveryService getDiscoveryService() {
        return discoveryService;
    }
}
