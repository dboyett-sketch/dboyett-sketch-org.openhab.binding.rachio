package org.openhab.binding.rachio.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiClient;
import org.openhab.binding.rachio.internal.api.RachioWebHookServletService;
import org.openhab.binding.rachio.internal.api.dto.RachioWebHookEvent;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioBridgeHandler} is the central bridge for all Rachio devices and zones.
 * It manages the API connection and coordinates webhook events.
 */
@NonNullByDefault
@Component(service = RachioBridgeHandler.class)
public class RachioBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);

    private @Nullable RachioApiClient apiClient;
    private @Nullable RachioBridgeConfiguration config;
    private @Nullable RachioWebHookServletService webhookService;

    @Activate
    public RachioBridgeHandler(final Bridge bridge, @Reference RachioWebHookServletService webhookService) {
        super(bridge);
        this.webhookService = webhookService;
    }

    @Override
    public void initialize() {
        logger.info("Initializing Rachio bridge handler for {}", getThing().getUID());

        updateStatus(ThingStatus.UNKNOWN);
        config = getConfigAs(RachioBridgeConfiguration.class);

        if (config == null || config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "API key is required");
            return;
        }

        try {
            // Create the modern API client
            apiClient = new RachioApiClient(config.getApiKey());

            // Test the connection by fetching devices
            apiClient.getDevices();
            logger.debug("Successfully connected to Rachio API");

            // Register this bridge handler with the webhook service
            RachioWebHookServletService localService = webhookService;
            if (localService != null) {
                localService.registerBridgeHandler(this);
                logger.debug("Registered bridge with webhook service");
            } else {
                logger.warn("Webhook service not available, real-time updates may be limited");
            }

            // Register webhook with Rachio cloud (if callback URL is configured)
            registerCloudWebhook();

            updateStatus(ThingStatus.ONLINE);

        } catch (Exception e) {
            logger.error("Failed to initialize Rachio bridge: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Failed to connect to Rachio API: " + e.getMessage());
        }
    }

    @Override
    @Deactivate
    public void dispose() {
        logger.debug("Disposing Rachio bridge handler for {}", getThing().getUID());

        // Unregister from webhook service
        RachioWebHookServletService localService = webhookService;
        if (localService != null) {
            localService.unregisterBridgeHandler(this);
        }

        // Dispose API client resources
        RachioApiClient localClient = apiClient;
        if (localClient != null) {
            localClient.dispose();
            apiClient = null;
        }

        super.dispose();
    }

    public @Nullable RachioApiClient getApiClient() {
        return apiClient;
    }

    public @Nullable RachioBridgeConfiguration getBridgeConfig() {
        return config;
    }

    /**
     * Handle a webhook event received from the Rachio cloud.
     * This method is called by RachioWebHookServletService.
     */
    public void handleWebhookEvent(RachioWebHookEvent event) {
        if (event == null) {
            logger.warn("Received null webhook event");
            return;
        }

        logger.debug("Bridge handling webhook event: {}", event.getEventType());

        // Process the event in the API client to clear relevant caches
        RachioApiClient localClient = apiClient;
        if (localClient != null) {
            try {
                localClient.processWebhookEvent(event);
                logger.debug("Processed webhook event for cache invalidation: {}", event.getEventType());
            } catch (Exception e) {
                logger.error("Error processing webhook event in API client", e);
            }
        } else {
            logger.warn("API client not available, cannot process webhook event");
        }

        // TODO: In a more advanced implementation, you could notify registered
        // Device and Zone handlers here for immediate UI updates.
        // For example: notifyStatusListeners(event);
    }

    /**
     * Register a webhook with the Rachio cloud API.
     * This tells Rachio where to send real-time event notifications.
     */
    private void registerCloudWebhook() {
        RachioApiClient localClient = apiClient;
        RachioBridgeConfiguration localConfig = config;

        if (localClient == null || localConfig == null) {
            return;
        }

        // The webhook URL must be externally accessible.
        // In a home setup, this often requires dynamic DNS and port forwarding.
        String webhookUrl = localConfig.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            logger.info("Webhook URL not configured. Real-time events will not be received.");
            return;
        }

        // Use the bridge's Thing UID as a unique external ID for the webhook
        String externalId = getThing().getUID().getId();

        try {
            localClient.registerWebhook(webhookUrl, externalId);
            logger.info("Successfully registered cloud webhook for URL: {}", webhookUrl);
        } catch (Exception e) {
            logger.warn("Failed to register cloud webhook. Real-time events disabled. Error: {}", e.getMessage());
            // This is not a critical failure; polling will still work.
        }
    }

    /**
     * Unregister the webhook from the Rachio cloud (e.g., during disposal).
     */
    private void unregisterCloudWebhook() {
        // Note: To unregister, you need the webhook ID returned during registration.
        // The current RachioApiClient.registerWebhook() would need enhancement
        // to store and later use this ID for deletion.
        // For now, we log a message.
        logger.debug("Cloud webhook unregistration logic would go here.");
    }

    /**
     * Check if the bridge has a webhook URL configured.
     * Used by the webhook servlet service.
     */
    public boolean isWebhookConfigured() {
        RachioBridgeConfiguration localConfig = config;
        return localConfig != null && localConfig.getWebhookUrl() != null && !localConfig.getWebhookUrl().isEmpty();
    }

    /**
     * Get the configured webhook secret for HMAC validation.
     * Used by the webhook servlet service.
     */
    public @Nullable String getWebhookSecret() {
        RachioBridgeConfiguration localConfig = config;
        return (localConfig != null) ? localConfig.getWebhookSecret() : null;
    }

    /**
     * Get the webhook URL for this bridge.
     */
    public @Nullable String getWebhookUrl() {
        RachioBridgeConfiguration localConfig = config;
        return (localConfig != null) ? localConfig.getWebhookUrl() : null;
    }
}
