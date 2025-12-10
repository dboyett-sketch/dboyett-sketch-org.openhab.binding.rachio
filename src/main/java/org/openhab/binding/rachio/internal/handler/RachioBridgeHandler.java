package org.openhab.binding.rachio.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.RachioSecurity;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
import org.openhab.binding.rachio.internal.discovery.RachioDiscoveryService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

/**
 * The {@link RachioBridgeHandler} is responsible for handling commands for the bridge
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);
    private final Map<String, RachioDevice> deviceCache = new ConcurrentHashMap<>();
    private final Set<RachioStatusListener> statusListeners = ConcurrentHashMap.newKeySet();

    private @Nullable RachioHttp httpClient;
    private @Nullable RachioSecurity security;
    private @Nullable RachioDiscoveryService discoveryService;
    private @Nullable ScheduledFuture<?> pollingHandler;
    private @Nullable RachioBridgeConfiguration config;
    private @Nullable String personId;

    public RachioBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio bridge handler");

        config = getConfigAs(RachioBridgeConfiguration.class);
        if (config.apiKey == null || config.apiKey.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "API key is not configured");
            return;
        }

        // Initialize HTTP client and security
        httpClient = new RachioHttp(config.apiKey);
        security = new RachioSecurity();

        updateStatus(ThingStatus.UNKNOWN);

        // Start initial poll
        scheduler.execute(() -> {
            try {
                // Get person info to retrieve devices
                RachioPerson person = httpClient.getPersonInfo();
                if (person != null && person.getId() != null) {
                    personId = person.getId();
                    logger.debug("Retrieved person ID: {}", personId);
                    
                    // CORRECTION APPLIED HERE (Line 95 area):
                    // Initialize polling handler correctly - store the ScheduledFuture, not cast it
                    pollingHandler = scheduler.scheduleWithFixedDelay(this::pollDevices, 
                            0, config.refresh, TimeUnit.SECONDS);
                    
                    updateStatus(ThingStatus.ONLINE);
                    startDiscovery();
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, 
                            "Failed to retrieve person information");
                }
            } catch (Exception e) {
                logger.error("Error during bridge initialization", e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
        });
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio bridge handler");
        
        // Cancel polling job
        ScheduledFuture<?> localPollingHandler = pollingHandler;
        if (localPollingHandler != null && !localPollingHandler.isCancelled()) {
            localPollingHandler.cancel(true);
            pollingHandler = null;
        }
        
        // Clear caches
        deviceCache.clear();
        statusListeners.clear();
        
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Handling command {} for channel {}", command, channelUID);

        if (command instanceof RefreshType) {
            // Refresh bridge status
            scheduler.execute(this::pollDevices);
        }
        // Bridge doesn't have many commands - most are handled by device/zone handlers
    }

    /**
     * Poll devices from Rachio API
     * CORRECTION: Changed from private to package-private to allow access from device/zone handlers
     */
    void pollDevices() {
        RachioHttp localHttpClient = httpClient;
        if (localHttpClient == null || getThing().getStatus() != ThingStatus.ONLINE) {
            return;
        }

        try {
            logger.debug("Polling Rachio devices");
            
            // Get person info to retrieve devices
            RachioPerson person = localHttpClient.getPersonInfo();
            if (person != null && person.getDevices() != null) {
                // Update device cache
                for (RachioDevice device : person.getDevices()) {
                    deviceCache.put(device.getId(), device);
                }
                
                // Notify status listeners of updates
                notifyDeviceUpdates(person.getDevices());
                
                logger.debug("Successfully polled {} devices", person.getDevices().size());
            }
        } catch (Exception e) {
            logger.error("Error polling devices", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, 
                    "Polling error: " + e.getMessage());
        }
    }

    /**
     * Start device discovery
     */
    private void startDiscovery() {
        // CORRECTION APPLIED HERE (Line 188 area):
        // Use startBackgroundDiscovery() instead of protected startScan()
        RachioDiscoveryService localDiscoveryService = discoveryService;
        if (localDiscoveryService != null) {
            localDiscoveryService.startBackgroundDiscovery();
        } else {
            logger.debug("Discovery service not available");
        }
    }

    /**
     * Set discovery service
     */
    public void setDiscoveryService(RachioDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
        discoveryService.setBridgeHandler(this);
    }

    /**
     * Get device by ID
     */
    public @Nullable RachioDevice getDevice(String deviceId) {
        return deviceCache.get(deviceId);
    }

    /**
     * Get zone by ID (searches through all devices)
     */
    public @Nullable RachioZone getZone(String zoneId) {
        for (RachioDevice device : deviceCache.values()) {
            if (device.getZones() != null) {
                for (RachioZone zone : device.getZones()) {
                    if (zoneId.equals(zone.getId())) {
                        return zone;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get all devices for discovery
     */
    public List<RachioDevice> discoverDevices() {
        return new ArrayList<>(deviceCache.values());
    }

    /**
     * Get HTTP client
     */
    public @Nullable RachioHttp getHttpClient() {
        return httpClient;
    }

    /**
     * Get security utility
     */
    public @Nullable RachioSecurity getSecurity() {
        return security;
    }

    /**
     * Get person ID
     */
    public @Nullable String getPersonId() {
        return personId;
    }

    /**
     * Get bridge configuration
     */
    public @Nullable RachioBridgeConfiguration getBridgeConfig() {
        return config;
    }

    /**
     * Status listener management
     */
    public void registerStatusListener(RachioStatusListener listener) {
        if (listener != null) {
            statusListeners.add(listener);
            logger.debug("Registered status listener: {}", listener.getClass().getSimpleName());
        }
    }

    public void unregisterStatusListener(RachioStatusListener listener) {
        if (listener != null) {
            statusListeners.remove(listener);
            logger.debug("Unregistered status listener: {}", listener.getClass().getSimpleName());
        }
    }

    /**
     * Notify all status listeners of device updates
     */
    private void notifyDeviceUpdates(List<RachioDevice> devices) {
        for (RachioStatusListener listener : statusListeners) {
            try {
                for (RachioDevice device : devices) {
                    listener.onDeviceUpdated(device.getId());
                }
            } catch (Exception e) {
                logger.warn("Error notifying status listener {}", listener.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Notify all status listeners of a status change
     */
    protected void notifyStatusListeners(ThingStatus status, ThingStatusDetail detail, @Nullable String message) {
        for (RachioStatusListener listener : statusListeners) {
            try {
                listener.onStatusChanged(status, detail, message);
            } catch (Exception e) {
                logger.warn("Error notifying status listener {}", listener.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Get all status listeners (for testing/debugging)
     */
    protected Set<RachioStatusListener> getStatusListeners() {
        return new HashSet<>(statusListeners);
    }

    /**
     * Clear device cache (force refresh on next poll)
     */
    public void clearDeviceCache() {
        deviceCache.clear();
    }

    /**
     * Check if webhook is configured
     */
    public boolean isWebhookConfigured() {
        RachioBridgeConfiguration localConfig = config;
        return localConfig != null && localConfig.webhookUrl != null && !localConfig.webhookUrl.isEmpty();
    }

    /**
     * Update bridge status and notify listeners
     */
    @Override
    protected void updateStatus(ThingStatus status, ThingStatusDetail statusDetail, @Nullable String description) {
        super.updateStatus(status, statusDetail, description);
        notifyStatusListeners(status, statusDetail, description);
    }
}
