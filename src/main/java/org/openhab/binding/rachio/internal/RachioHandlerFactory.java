package org.openhab.binding.rachio.internal;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiClient;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.binding.rachio.internal.handler.RachioDeviceHandler;
import org.openhab.binding.rachio.internal.handler.RachioZoneHandler;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.config.core.Configuration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Dave Boyett - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, 
           configurationPid = "binding.rachio",
           property = {
               "service.config.description.uri=binding:rachio",
               "service.config.label=Rachio Binding",
               "service.config.category=iot"
           })
@NonNullByDefault
public class RachioHandlerFactory extends BaseThingHandlerFactory {
    private final Logger logger = LoggerFactory.getLogger(RachioHandlerFactory.class);
    
    // REQUIRED for OpenHAB 5.x compatibility - HTTP client via factory
    private final HttpClientFactory httpClientFactory;
    
    // REQUIRED - Rachio API client service
    private final RachioApiClient rachioApiClient;
    
    /**
     * Constructor with dependency injection (OSGi DS pattern for OpenHAB 5.x)
     */
    @Activate
    public RachioHandlerFactory(@Reference HttpClientFactory httpClientFactory,
                               @Reference RachioApiClient rachioApiClient) {
        this.httpClientFactory = httpClientFactory;
        this.rachioApiClient = rachioApiClient;
        logger.debug("RachioHandlerFactory created with HttpClientFactory and RachioApiClient");
    }
    
    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        logger.trace("Checking support for thing type: {}", thingTypeUID);
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }
    
    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        logger.debug("Creating handler for thing type: {} with ID: {}", thingTypeUID, thing.getUID());
        
        if (thingTypeUID.equals(THING_TYPE_BRIDGE)) {
            // CRITICAL: Use the new constructor with HttpClientFactory and RachioApiClient
            // This is REQUIRED for OpenHAB 5.x compatibility
            return createBridgeHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_DEVICE)) {
            return createDeviceHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_ZONE)) {
            return createZoneHandler(thing);
        }
        
        logger.warn("Unsupported thing type: {}", thingTypeUID);
        return null;
    }
    
    /**
     * Create bridge handler with injected dependencies
     */
    private RachioBridgeHandler createBridgeHandler(Thing thing) {
        logger.debug("Creating RachioBridgeHandler for bridge: {}", thing.getUID());
        
        if (!(thing instanceof Bridge)) {
            logger.error("Thing {} is not a Bridge, but THING_TYPE_BRIDGE was requested", thing.getUID());
            throw new IllegalArgumentException("Thing must be a Bridge for THING_TYPE_BRIDGE");
        }
        
        try {
            // Use constructor injection with HttpClientFactory and RachioApiClient
            RachioBridgeHandler handler = new RachioBridgeHandler(
                (Bridge) thing, 
                rachioApiClient, 
                httpClientFactory
            );
            
            logger.debug("Successfully created RachioBridgeHandler for {}", thing.getUID());
            return handler;
            
        } catch (Exception e) {
            logger.error("Failed to create RachioBridgeHandler for {}", thing.getUID(), e);
            throw new RuntimeException("Failed to create bridge handler", e);
        }
    }
    
    /**
     * Create device handler
     */
    private RachioDeviceHandler createDeviceHandler(Thing thing) {
        logger.debug("Creating RachioDeviceHandler for device: {}", thing.getUID());
        
        try {
            RachioDeviceHandler handler = new RachioDeviceHandler(thing);
            logger.debug("Successfully created RachioDeviceHandler for {}", thing.getUID());
            return handler;
        } catch (Exception e) {
            logger.error("Failed to create RachioDeviceHandler for {}", thing.getUID(), e);
            throw new RuntimeException("Failed to create device handler", e);
        }
    }
    
    /**
     * Create zone handler
     */
    private RachioZoneHandler createZoneHandler(Thing thing) {
        logger.debug("Creating RachioZoneHandler for zone: {}", thing.getUID());
        
        try {
            RachioZoneHandler handler = new RachioZoneHandler(thing);
            logger.debug("Successfully created RachioZoneHandler for {}", thing.getUID());
            return handler;
        } catch (Exception e) {
            logger.error("Failed to create RachioZoneHandler for {}", thing.getUID(), e);
            throw new RuntimeException("Failed to create zone handler", e);
        }
    }
    
    @Override
    public @Nullable Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, 
                                      @Nullable ThingUID thingUID, @Nullable ThingUID bridgeUID) {
        logger.debug("Creating thing of type {} with configuration {}", thingTypeUID, configuration);
        
        ThingUID actualThingUID = thingUID;
        if (actualThingUID == null) {
            actualThingUID = getThingUID(thingTypeUID, configuration, bridgeUID);
        }
        
        if (actualThingUID == null) {
            logger.error("Cannot create thing - thingUID is null");
            return null;
        }
        
        return super.createThing(thingTypeUID, configuration, actualThingUID, bridgeUID);
    }
    
    @Override
    protected @Nullable ThingUID getThingUID(ThingTypeUID thingTypeUID, Configuration configuration, 
                                            @Nullable ThingUID bridgeUID) {
        logger.trace("Getting ThingUID for type {} with bridge {}", thingTypeUID, bridgeUID);
        
        if (thingTypeUID.equals(THING_TYPE_BRIDGE)) {
            // Bridge doesn't need a bridge UID
            return new ThingUID(thingTypeUID, "rachio");
        } else if (thingTypeUID.equals(THING_TYPE_DEVICE)) {
            String deviceId = (String) configuration.get("deviceId");
            if (deviceId != null && bridgeUID != null) {
                return new ThingUID(thingTypeUID, bridgeUID, deviceId);
            }
        } else if (thingTypeUID.equals(THING_TYPE_ZONE)) {
            String zoneId = (String) configuration.get("zoneId");
            if (zoneId != null && bridgeUID != null) {
                return new ThingUID(thingTypeUID, bridgeUID, zoneId);
            }
        }
        
        logger.warn("Cannot determine ThingUID for type {} with configuration {}", thingTypeUID, configuration);
        return null;
    }
    
    /**
     * Validate thing configuration before creation
     */
    @Override
    protected void validateThingType(ThingTypeUID thingTypeUID, Configuration configuration) {
        logger.debug("Validating configuration for thing type: {}", thingTypeUID);
        
        if (thingTypeUID.equals(THING_TYPE_BRIDGE)) {
            validateBridgeConfiguration(configuration);
        } else if (thingTypeUID.equals(THING_TYPE_DEVICE)) {
            validateDeviceConfiguration(configuration);
        } else if (thingTypeUID.equals(THING_TYPE_ZONE)) {
            validateZoneConfiguration(configuration);
        } else {
            logger.warn("Unknown thing type for validation: {}", thingTypeUID);
        }
        
        super.validateThingType(thingTypeUID, configuration);
    }
    
    /**
     * Validate bridge configuration
     */
    private void validateBridgeConfiguration(Configuration configuration) {
        String apiKey = (String) configuration.get("apiKey");
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("Bridge configuration missing API key");
            // Note: We warn but don't fail - user can configure later via UI
        } else if (!isValidApiKeyFormat(apiKey)) {
            logger.warn("Bridge API key format appears invalid: {}", maskApiKey(apiKey));
        } else {
            logger.debug("Bridge configuration validated successfully");
        }
    }
    
    /**
     * Validate device configuration
     */
    private void validateDeviceConfiguration(Configuration configuration) {
        String deviceId = (String) configuration.get("deviceId");
        
        if (deviceId == null || deviceId.trim().isEmpty()) {
            logger.warn("Device configuration missing deviceId");
        } else {
            logger.debug("Device configuration validated for deviceId: {}", deviceId);
        }
    }
    
    /**
     * Validate zone configuration
     */
    private void validateZoneConfiguration(Configuration configuration) {
        String zoneId = (String) configuration.get("zoneId");
        String deviceId = (String) configuration.get("deviceId");
        
        if (zoneId == null || zoneId.trim().isEmpty()) {
            logger.warn("Zone configuration missing zoneId");
        }
        
        if (deviceId == null || deviceId.trim().isEmpty()) {
            logger.warn("Zone configuration missing deviceId");
        }
        
        if (zoneId != null && deviceId != null) {
            logger.debug("Zone configuration validated for zoneId: {}, deviceId: {}", zoneId, deviceId);
        }
    }
    
    /**
     * Check if API key has valid format (basic validation)
     */
    private boolean isValidApiKeyFormat(String apiKey) {
        // Rachio API keys are typically UUID format
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
    
    @Deactivate
    protected void deactivate() {
        logger.debug("RachioHandlerFactory deactivated");
    }
    
    /**
     * Get the HttpClientFactory (for potential use by other components)
     */
    public HttpClientFactory getHttpClientFactory() {
        return httpClientFactory;
    }
    
    /**
     * Get the RachioApiClient (for potential use by other components)
     */
    public RachioApiClient getRachioApiClient() {
        return rachioApiClient;
    }
    
    /**
     * Register a thing with the handler factory
     * This can be used by discovery service to dynamically add things
     */
    public void registerThing(Thing thing) {
        logger.debug("Registering thing: {}", thing.getUID());
        // Implementation would add thing to thing registry
        // This is typically handled by OpenHAB framework
    }
    
    /**
     * Unregister a thing from the handler factory
     */
    public void unregisterThing(ThingUID thingUID) {
        logger.debug("Unregistering thing: {}", thingUID);
        // Implementation would remove thing from thing registry
        // This is typically handled by OpenHAB framework
    }
}
