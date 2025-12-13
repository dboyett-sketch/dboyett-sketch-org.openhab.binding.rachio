package org.openhab.binding.rachio.internal.handler;

import org.openhab.binding.rachio.internal.api.RachioApiClient;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(service = RachioBridgeHandler.class)
public class RachioBridgeHandler extends BaseBridgeHandler {
    private RachioApiClient apiClient;
    private RachioBridgeConfiguration config;
    
    @Activate
    public RachioBridgeHandler(Bridge bridge) {
        super(bridge);
    }
    
    @Override
    public void initialize() {
        config = getConfigAs(RachioBridgeConfiguration.class);
        
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                "API key is required");
            return;
        }
        
        try {
            // 🎯 MODERN: Create API client directly, no factory dependency
            apiClient = new RachioApiClient(config.getApiKey());
            
            // Test connection
            apiClient.getDevices();
            
            updateStatus(ThingStatus.ONLINE);
            
            // Register webhook
            registerWebhook();
            
        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "Failed to connect to Rachio API: " + e.getMessage());
        }
    }
    
    @Override
    @Deactivate
    public void dispose() {
        if (apiClient != null) {
            apiClient.dispose();
            apiClient = null;
        }
        super.dispose();
    }
    
    public RachioApiClient getApiClient() {
        return apiClient;
    }
    
    private void registerWebhook() {
        // Webhook registration logic
        // Uses apiClient.registerWebhook()
    }
}
