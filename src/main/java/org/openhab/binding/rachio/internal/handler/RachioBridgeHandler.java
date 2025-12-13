package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiClient;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.exception.RachioApiException;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Dave Boyett - Initial contribution
 */
@Component(service = RachioBridgeHandler.class)
@NonNullByDefault
public class RachioBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);
    
    // Configuration
    private RachioBridgeConfiguration config = new RachioBridgeConfiguration();
    
    // API Client (OpenHAB 5.x compatible)
    private final RachioApiClient apiClient;
    
    // HTTP Client Factory (REQUIRED for OpenHAB 5.x)
    private final HttpClientFactory httpClientFactory;
    
    // Scheduler for periodic updates
    private @Nullable ScheduledFuture<?> refreshJob;
    
    // Status listeners
    private final List<RachioStatusListener> statusListeners = new ArrayList<>();
    
    // Device cache
    private final Map<String, RachioDevice> deviceCache = new ConcurrentHashMap<>();
    
    // Bridge state
    private boolean initialized = false;
    
    /**
     * Constructor with dependency injection for OpenHAB 5.x compatibility
     */
    @Activate
    public RachioBridgeHandler(Bridge bridge, 
                              @Reference RachioApiClient apiClient,
                              @Reference HttpClientFactory httpClientFactory) {
        super(bridge);
        this.apiClient = apiClient;
        this.httpClientFactory = httpClientFactory; // Store for potential future use
        logger.debug("RachioBridgeHandler created with API client and HttpClientFactory");
    }
    
    @Override
    public void initialize() {
        logger.debug("Initializing Rachio bridge handler");
        
        // Start with UNKNOWN status (OpenHAB 5.x pattern)
        updateStatus(ThingStatus.UNKNOWN);
        
        // Load configuration
        config = getConfigAs(RachioBridgeConfiguration.class);
        
        if (config.apiKey == null || config.apiKey.trim().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, 
                        "API key is missing. Please configure the bridge.");
            logger.error("Rachio bridge API key is not configured");
            return;
        }
        
        // Validate configuration
        if (!isValidApiKey(config.apiKey)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "API key appears invalid. Please check configuration.");
            logger.error("Rachio bridge API key appears invalid");
            return;
        }
        
        // Schedule initialization in background (OpenHAB 5.x best practice)
        scheduler.execute(() -> {
            try {
                initializeBridge();
            } catch (Exception e) {
                logger.error("Failed to initialize Rachio bridge", e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Initialization failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Initialize bridge connection to Rachio API
     */
    private void initializeBridge() {
        logger.debug("Starting bridge initialization");
        
        try {
            // Test API connection
            if (!apiClient.testConnection(config.apiKey)) {
                throw new RachioApiException("API connection test failed");
            }
            
            // Fetch initial device data
            refreshDevices();
            
            // Start periodic refresh
            startRefreshJob();
            
            // Mark as initialized
            initialized = true;
            
            // Update status to ONLINE
            updateStatus(ThingStatus.ONLINE);
            logger.info("Rachio bridge initialized successfully with API key: {}", 
                       maskApiKey(config.apiKey));
            
        } catch (RachioApiException e) {
            logger.error("Failed to initialize Rachio bridge: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "API error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during bridge initialization", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Unexpected error: " + e.getMessage());
        }
    }
    
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Bridge received command {} for channel {}", command, channelUID);
        
        // Bridge typically doesn't handle commands directly
        // Commands are handled by device/zone handlers
        logger.warn("Bridge received unexpected command {} for channel {}", command, channelUID);
    }
    
    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        logger.debug("Handling configuration update");
        
        // Stop existing refresh job
        stopRefreshJob();
        
        // Update configuration
        super.handleConfigurationUpdate(configurationParameters);
        config = getConfigAs(RachioBridgeConfiguration.class);
        
        // Re-initialize with new configuration
        scheduler.execute(() -> {
            try {
                refreshDevices();
                startRefreshJob();
                updateStatus(ThingStatus.ONLINE);
                logger.info("Bridge configuration updated successfully");
            } catch (Exception e) {
                logger.error("Failed to update bridge configuration", e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "Configuration update failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Start periodic refresh job
     */
    private void startRefreshJob() {
        if (refreshJob != null && !refreshJob.isCancelled()) {
            refreshJob.cancel(true);
        }
        
        int refreshInterval = config.refreshInterval > 0 ? config.refreshInterval : 60;
        
        refreshJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                refreshDevices();
            } catch (Exception e) {
                logger.warn("Failed to refresh devices during scheduled job", e);
            }
        }, refreshInterval, refreshInterval, TimeUnit.SECONDS);
        
        logger.debug("Started refresh job with interval {} seconds", refreshInterval);
    }
    
    /**
     * Stop refresh job
     */
    private void stopRefreshJob() {
        if (refreshJob != null) {
            refreshJob.cancel(true);
            refreshJob = null;
            logger.debug("Stopped refresh job");
        }
    }
    
    /**
     * Refresh device data from Rachio API
     */
    public void refreshDevices() throws RachioApiException {
        if (config.apiKey == null || config.apiKey.trim().isEmpty()) {
            logger.warn("Cannot refresh devices: API key not configured");
            return;
        }
        
        try {
            List<RachioDevice> devices = apiClient.getDevices(config.apiKey);
            
            synchronized (deviceCache) {
                deviceCache.clear();
                for (RachioDevice device : devices) {
                    if (device.id != null) {
                        deviceCache.put(device.id, device);
                    }
                }
            }
            
            logger.debug("Refreshed {} devices from Rachio API", devices.size());
            
            // Notify status listeners
            notifyStatusListeners(devices);
            
        } catch (RachioApiException e) {
            logger.warn("Failed to refresh devices: {}", e.getMessage());
            
            // Update bridge status if API call fails
            if (getThing().getStatus() == ThingStatus.ONLINE) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Failed to refresh devices: " + e.getMessage());
            }
            throw e;
        }
    }
    
    /**
     * Get API client for device/zone handlers
     */
    public RachioApiClient getApiClient() {
        return apiClient;
    }
    
    /**
     * Get API key from configuration
     */
    public @Nullable String getApiKey() {
        return config.apiKey;
    }
    
    /**
     * Get device by ID from cache
     */
    public @Nullable RachioDevice getDevice(String deviceId) {
        return deviceCache.get(deviceId);
    }
    
    /**
     * Get all devices from cache
     */
    public Collection<RachioDevice> getDevices() {
        return deviceCache.values();
    }
    
    /**
     * Check if device exists
     */
    public boolean hasDevice(String deviceId) {
        return deviceCache.containsKey(deviceId);
    }
    
    /**
     * Add status listener
     */
    public void addStatusListener(RachioStatusListener listener) {
        synchronized (statusListeners) {
            if (!statusListeners.contains(listener)) {
                statusListeners.add(listener);
                logger.debug("Added status listener: {}", listener);
            }
        }
    }
    
    /**
     * Remove status listener
     */
    public void removeStatusListener(RachioStatusListener listener) {
        synchronized (statusListeners) {
            statusListeners.remove(listener);
            logger.debug("Removed status listener: {}", listener);
        }
    }
    
    /**
     * Notify all status listeners
     */
    private void notifyStatusListeners(List<RachioDevice> devices) {
        synchronized (statusListeners) {
            for (RachioStatusListener listener : statusListeners) {
                try {
                    listener.onDevicesUpdated(devices);
                } catch (Exception e) {
                    logger.warn("Status listener notification failed", e);
                }
            }
        }
    }
    
    /**
     * Handle webhook event from Rachio
     */
    public void onWebhookEvent(String deviceId, String eventType, String eventData) {
        logger.debug("Received webhook event for device {}: {} - {}", deviceId, eventType, eventData);
        
        // Update device cache if needed
        scheduler.execute(() -> {
            try {
                if (config.apiKey != null) {
                    RachioDevice device = apiClient.getDevice(config.apiKey, deviceId);
                    if (device != null) {
                        deviceCache.put(deviceId, device);
                        logger.debug("Updated device cache from webhook: {}", deviceId);
                    }
                }
            } catch (Exception e) {
                logger.debug("Failed to update device from webhook", e);
            }
            
            // Notify listeners about webhook event
            synchronized (statusListeners) {
                for (RachioStatusListener listener : statusListeners) {
                    try {
                        listener.onWebhookEvent(deviceId, eventType, eventData);
                    } catch (Exception e) {
                        logger.warn("Webhook notification failed for listener", e);
                    }
                }
            }
        });
    }
    
    /**
     * Validate API key format
     */
    private boolean isValidApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        
        // Rachio API keys are UUID format
        return apiKey.matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");
    }
    
    /**
     * Mask API key for logging
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }
    
    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        // Return any services this handler provides
        // For example, if this handler provides a discovery service:
        // return Set.of(RachioDiscoveryService.class);
        return List.of();
    }
    
    @Override
    public void dispose() {
        logger.debug("Disposing Rachio bridge handler");
        
        // Stop refresh job
        stopRefreshJob();
        
        // Clear cache
        deviceCache.clear();
        
        // Clear listeners
        synchronized (statusListeners) {
            statusListeners.clear();
        }
        
        initialized = false;
        
        super.dispose();
        logger.debug("Rachio bridge handler disposed");
    }
    
    /**
     * Check if bridge is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Get refresh interval from configuration
     */
    public int getRefreshInterval() {
        return config.refreshInterval > 0 ? config.refreshInterval : 60;
    }
    
    @Deactivate
    protected void deactivate() {
        logger.debug("RachioBridgeHandler deactivated");
        dispose();
    }
    
    @Modified
    protected void modified() {
        logger.debug("RachioBridgeHandler configuration modified");
        // Re-initialize with potentially new configuration
        scheduler.execute(this::initialize);
    }
    
    /**
     * Get HTTP client factory (for potential future use by other components)
     */
    public HttpClientFactory getHttpClientFactory() {
        return httpClientFactory;
    }
}
