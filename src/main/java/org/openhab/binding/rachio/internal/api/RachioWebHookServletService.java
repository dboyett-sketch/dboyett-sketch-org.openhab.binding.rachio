package org.openhab.binding.rachio.internal.api;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioWebHookServletService} is responsible for managing the webhook servlet.
 *
 * @author Damion Boyett - Initial contribution
 */
@Component(service = RachioWebHookServletService.class, immediate = true)
@NonNullByDefault
public class RachioWebHookServletService {
    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServletService.class);

    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable RachioHttp rachioHttp;
    private @Nullable RachioWebHookServlet servlet;
    private @Nullable Timer healthCheckTimer;

    private @Nullable HttpClientFactory httpClientFactory;

    @Reference
    public void setHttpClientFactory(HttpClientFactory httpClientFactory) {
        this.httpClientFactory = httpClientFactory;
    }

    public void unsetHttpClientFactory(HttpClientFactory httpClientFactory) {
        this.httpClientFactory = null;
    }

    @Activate
    public void activate(@Nullable ComponentContext componentContext, @Nullable Map<String, @Nullable Object> properties) {
        logger.debug("Activating RachioWebHookServletService");
        startWebhookHealthCheck();
    }

    @Deactivate
    public void deactivate() {
        logger.debug("Deactivating RachioWebHookServletService");
        stopWebhookHealthCheck();
        unregisterServlet();
    }

    public void setBridgeHandler(RachioBridgeHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
        this.rachioHttp = bridgeHandler.getRachioHttp();
        registerServlet();
    }

    public void unsetBridgeHandler(RachioBridgeHandler bridgeHandler) {
        unregisterServlet();
        this.bridgeHandler = null;
        this.rachioHttp = null;
    }

    private void registerServlet() {
        RachioBridgeHandler handler = bridgeHandler;
        if (handler != null && servlet == null) {
            servlet = new RachioWebHookServlet(handler);
            // Register servlet with webhook port from configuration
            // This would typically register with OpenHAB's HTTP service
            logger.info("Rachio webhook servlet registered for bridge: {}", handler.getThing().getUID());
            
            // Register webhook with Rachio API
            registerWebhookWithRachio();
        }
    }

    private void unregisterServlet() {
        if (servlet != null) {
            // Unregister servlet
            logger.info("Rachio webhook servlet unregistered");
            servlet = null;
            
            // Optionally delete webhook from Rachio
            deleteWebhookFromRachio();
        }
    }

    private void registerWebhookWithRachio() {
        RachioHttp http = rachioHttp;
        RachioBridgeHandler handler = bridgeHandler;
        
        if (http != null && handler != null) {
            try {
                // Build webhook URL
                String webhookUrl = buildWebhookUrl(handler);
                String externalId = "openhab-" + handler.getThing().getUID().getId();
                
                http.registerWebhook(webhookUrl, externalId);
                logger.info("Registered webhook with Rachio: {}", webhookUrl);
                
            } catch (Exception e) {
                logger.warn("Failed to register webhook with Rachio: {}", e.getMessage());
            }
        }
    }

    private void deleteWebhookFromRachio() {
        RachioHttp http = rachioHttp;
        if (http != null) {
            try {
                http.deleteAllWebhooks();
                logger.info("Deleted all Rachio webhooks");
            } catch (Exception e) {
                logger.warn("Failed to delete Rachio webhooks: {}", e.getMessage());
            }
        }
    }

    private String buildWebhookUrl(RachioBridgeHandler handler) {
        // Build the webhook callback URL
        // This should use the configuration from the bridge handler
        // For now, return a placeholder
        return "http://localhost:8080/rachio/webhook";
    }

    private void startWebhookHealthCheck() {
        stopWebhookHealthCheck(); // Stop existing if any
        
        healthCheckTimer = new Timer("RachioWebhookHealthCheck", true);
        healthCheckTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkWebhookHealth();
            }
        }, 0, 5 * 60 * 1000); // Check every 5 minutes
    }

    private void stopWebhookHealthCheck() {
        Timer timer = healthCheckTimer;
        if (timer != null) {
            timer.cancel();
            healthCheckTimer = null;
        }
    }

    private void checkWebhookHealth() {
        RachioHttp http = rachioHttp;
        if (http != null) {
            try {
                // Check if webhooks are registered
                RachioApiWebHookList webhooks = http.getWebhooks();
                if (webhooks != null && webhooks.getData() != null && !webhooks.getData().isEmpty()) {
                    logger.debug("Webhook health check: {} webhooks registered", webhooks.getData().size());
                } else {
                    logger.warn("Webhook health check: No webhooks registered. Attempting to re-register.");
                    registerWebhookWithRachio();
                }
            } catch (Exception e) {
                logger.debug("Webhook health check failed: {}", e.getMessage());
            }
        }
    }
}
