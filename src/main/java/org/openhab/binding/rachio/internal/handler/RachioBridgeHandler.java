package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.RachioSecurity;
import org.openhab.binding.rachio.internal.api.RachioWebHookServlet;
import org.openhab.binding.rachio.internal.api.RachioWebHookServletService;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioWebHookEvent;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
import org.openhab.binding.rachio.internal.discovery.RachioDiscoveryService;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link RachioBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);
    private final HttpClientFactory httpClientFactory;
    private final Gson gson;
    private final Map<String, RachioDevice> devices = new ConcurrentHashMap<>();
    private final Map<String, RachioStatusListener> statusListeners = new ConcurrentHashMap<>();

    private @Nullable RachioHttp http;
    private @Nullable RachioSecurity security;
    private @Nullable RachioWebHookServletService webhookService;
    private @Nullable RachioWebHookServlet webhookServlet;
    private @Nullable ScheduledFuture<?> pollingJob;
    private @Nullable RachioBridgeConfiguration config;
    private @Nullable ScheduledFuture<?> webhookHealthJob;

    public RachioBridgeHandler(Bridge bridge, HttpClientFactory httpClientFactory) {
        super(bridge);
        this.httpClientFactory = httpClientFactory;
        this.gson = new GsonBuilder()
            .registerTypeAdapter(java.time.Instant.class, new org.openhab.binding.rachio.internal.api.InstantTypeAdapter())
            .create();
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio bridge handler");
        config = getConfigAs(RachioBridgeConfiguration.class);

        if (config == null || config.accessToken == null || config.accessToken.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Access token must be configured");
            return;
        }

        scheduler.submit(() -> {
            try {
                // Initialize HTTP client
                RachioHttp localHttp = new RachioHttp(config.accessToken, httpClientFactory.getCommonHttpClient(), gson);
                this.http = localHttp;

                // Initialize security
                RachioSecurity localSecurity = new RachioSecurity();
                localSecurity.setSecretKey(config.secretKey != null ? config.secretKey : "");
                localSecurity.setAllowedIpAddresses(config.allowedIps != null ? config.allowedIps : "");
                localSecurity.setAllowAwsIps(config.allowAwsIps != null ? config.allowAwsIps : false);
                this.security = localSecurity;

                // Test API connection
                localHttp.testConnection();
                
                // Initialize webhook service
                RachioWebHookServletService localWebhookService = new RachioWebHookServletService(this, gson);
                this.webhookService = localWebhookService;

                // Initialize webhook servlet
                RachioWebHookServlet localWebhookServlet = new RachioWebHookServlet(this);
                this.webhookServlet = localWebhookServlet;

                // Start polling
                startPolling();

                // Register webhook if enabled
                if (config.webhookEnabled != null && config.webhookEnabled) {
                    registerWebhook();
                }

                updateStatus(ThingStatus.ONLINE);
                logger.debug("Rachio bridge initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize Rachio bridge: {}", e.getMessage(), e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
        });
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio bridge handler");
        
        // Stop polling
        stopPolling();
        
        // Stop webhook health checks
        stopWebhookHealthChecks();
        
        // Unregister webhook
        unregisterWebhook();
        
        // Dispose webhook service
        RachioWebHookServletService localWebhookService = webhookService;
        if (localWebhookService != null) {
            localWebhookService.dispose();
        }
        
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Bridge doesn't handle commands directly
    }

    /**
     * Get the RachioHttp client
     */
    public @Nullable RachioHttp getRachioHttp() {
        return http;
    }

    /**
     * Get the Gson instance
     */
    public Gson getGson() {
        return gson;
    }

    /**
     * Get the bridge configuration
     */
    public @Nullable RachioBridgeConfiguration getBridgeConfiguration() {
        return config;
    }

    /**
     * Get the device ID from configuration
     */
    public @Nullable String getDeviceId() {
        RachioBridgeConfiguration localConfig = config;
        return localConfig != null ? localConfig.deviceId : null;
    }

    /**
     * Get the webhook URL from configuration
     */
    public @Nullable String getWebhookUrl() {
        RachioBridgeConfiguration localConfig = config;
        return localConfig != null ? localConfig.webhookUrl : null;
    }

    /**
     * Start polling for device updates
     */
    private void startPolling() {
        stopPolling(); // Ensure no existing polling job
        
        pollingJob = scheduler.scheduleWithFixedDelay(this::pollDevices, 0, 60, TimeUnit.SECONDS);
        logger.debug("Started device polling");
    }

    /**
     * Stop polling for device updates
     */
    private void stopPolling() {
        ScheduledFuture<?> localPollingJob = pollingJob;
        if (localPollingJob != null && !localPollingJob.isCancelled()) {
            localPollingJob.cancel(true);
            pollingJob = null;
            logger.debug("Stopped device polling");
        }
    }

    /**
     * Poll devices from Rachio API
     */
    private void pollDevices() {
        try {
            RachioHttp localHttp = http;
            if (localHttp == null) {
                logger.warn("HTTP client not initialized");
                return;
            }

            // Get person info to get device list
            String personJson = localHttp.executeGet("/person/info");
            if (personJson != null && !personJson.isEmpty()) {
                org.openhab.binding.rachio.internal.api.dto.RachioPerson person = gson.fromJson(personJson, org.openhab.binding.rachio.internal.api.dto.RachioPerson.class);
                
                if (person != null && person.devices != null) {
                    devices.clear();
                    for (RachioDevice device : person.devices) {
                        if (device != null && device.id != null) {
                            devices.put(device.id, device);
                            
                            // Notify listeners
                            for (RachioStatusListener listener : statusListeners.values()) {
                                listener.deviceUpdated(device);
                            }
                        }
                    }
                    
                    // Notify all listeners about device list update
                    for (RachioStatusListener listener : statusListeners.values()) {
                        listener.deviceListUpdated(person.devices);
                    }
                    
                    logger.debug("Polled {} devices from Rachio API", devices.size());
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to poll devices: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.warn("Error polling devices: {}", e.getMessage(), e);
        }
    }

    /**
     * Get a device by ID
     */
    public @Nullable RachioDevice getDevice(String deviceId) {
        return devices.get(deviceId);
    }

    /**
     * Get all devices
     */
    public Collection<RachioDevice> getDevices() {
        return devices.values();
    }

    /**
     * Register a webhook with Rachio
     */
    private void registerWebhook() {
        RachioHttp localHttp = http;
        RachioWebHookServletService localWebhookService = webhookService;
        RachioBridgeConfiguration localConfig = config;
        
        if (localHttp == null || localWebhookService == null || localConfig == null) {
            logger.warn("Cannot register webhook: required components not initialized");
            return;
        }

        String deviceId = localConfig.deviceId;
        String webhookUrl = localConfig.webhookUrl;
        String externalId = getThing().getUID().getId();
        
        if (deviceId == null || deviceId.isBlank() || webhookUrl == null || webhookUrl.isBlank()) {
            logger.warn("Cannot register webhook: deviceId or webhookUrl not configured");
            return;
        }

        localWebhookService.registerWebhook(localHttp, deviceId, webhookUrl, externalId);
        
        // Start webhook health checks if enabled
        if (localConfig.webhookCheckInterval != null && localConfig.webhookCheckInterval > 0) {
            startWebhookHealthChecks(localConfig.webhookCheckInterval);
        }
    }

    /**
     * Unregister webhook from Rachio
     */
    private void unregisterWebhook() {
        RachioWebHookServletService localWebhookService = webhookService;
        if (localWebhookService != null) {
            localWebhookService.unregisterWebhook();
        }
    }

    /**
     * Start webhook health checks
     */
    private void startWebhookHealthChecks(int intervalSeconds) {
        stopWebhookHealthChecks(); // Ensure no existing job
        
        webhookHealthJob = scheduler.scheduleWithFixedDelay(this::checkWebhookHealth, 
            intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        logger.debug("Started webhook health checks every {} seconds", intervalSeconds);
    }

    /**
     * Stop webhook health checks
     */
    private void stopWebhookHealthChecks() {
        ScheduledFuture<?> localWebhookHealthJob = webhookHealthJob;
        if (localWebhookHealthJob != null && !localWebhookHealthJob.isCancelled()) {
            localWebhookHealthJob.cancel(true);
            webhookHealthJob = null;
            logger.debug("Stopped webhook health checks");
        }
    }

    /**
     * Check webhook health
     */
    private void checkWebhookHealth() {
        RachioHttp localHttp = http;
        RachioWebHookServletService localWebhookService = webhookService;
        
        if (localHttp != null && localWebhookService != null) {
            localWebhookService.checkWebhookHealth(localHttp);
        }
    }

    /**
     * Handle incoming webhook event
     */
    public void handleWebhookEvent(RachioWebHookEvent event) {
        if (event == null) {
            return;
        }

        logger.debug("Received webhook event: {}", event.type);
        
        // Route event to appropriate handlers
        if (event.deviceId != null) {
            RachioDeviceHandler deviceHandler = getDeviceHandler(event.deviceId);
            if (deviceHandler != null) {
                deviceHandler.handleWebhookEvent(event);
            }
        }
        
        if (event.zoneId != null) {
            RachioZoneHandler zoneHandler = getZoneHandler(event.zoneId);
            if (zoneHandler != null) {
                zoneHandler.handleWebhookEvent(event);
            }
        }
        
        // Notify all status listeners
        for (RachioStatusListener listener : statusListeners.values()) {
            listener.webhookEventReceived(event);
        }
    }

    /**
     * Get device handler by device ID
     */
    private @Nullable RachioDeviceHandler getDeviceHandler(String deviceId) {
        // This would need to be implemented to find the device handler
        // For now, return null and implement properly when thing registry is available
        return null;
    }

    /**
     * Get zone handler by zone ID
     */
    private @Nullable RachioZoneHandler getZoneHandler(String zoneId) {
        // This would need to be implemented to find the zone handler
        // For now, return null and implement properly when thing registry is available
        return null;
    }

    /**
     * Validate IP address for webhook requests
     */
    public boolean isIpAllowed(String ipAddress, String forwardedFor) {
        RachioSecurity localSecurity = security;
        if (localSecurity != null) {
            return localSecurity.isIpAllowed(ipAddress, forwardedFor);
        }
        return false;
    }

    /**
     * Register a status listener
     */
    public void registerStatusListener(RachioStatusListener listener) {
        statusListeners.put(listener.toString(), listener);
    }

    /**
     * Unregister a status listener
     */
    public void unregisterStatusListener(RachioStatusListener listener) {
        statusListeners.remove(listener.toString());
    }

    /**
     * Get weather enabled status
     */
    public boolean isWeatherEnabled() {
        RachioBridgeConfiguration localConfig = config;
        return localConfig != null && localConfig.weatherEnabled != null && localConfig.weatherEnabled;
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(RachioDiscoveryService.class);
    }
}
