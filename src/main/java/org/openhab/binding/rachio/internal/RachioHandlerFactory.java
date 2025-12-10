package org.openhab.binding.rachio.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioImageServletService;
import org.openhab.binding.rachio.internal.api.RachioWebHookServletService;
import org.openhab.binding.rachio.internal.discovery.RachioDiscoveryService;
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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

/**
 * The {@link RachioHandlerFactory} is responsible for creating thing handlers
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.rachio")
public class RachioHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(RachioHandlerFactory.class);
    
    private final HttpClientFactory httpClientFactory;
    private final RachioWebHookServletService webHookServletService;
    
    // Optional service reference for image servlet
    private @Nullable RachioImageServletService imageServletService;
    
    // Discovery service instance
    private @Nullable RachioDiscoveryService discoveryService;

    @Activate
    public RachioHandlerFactory(
            @Reference HttpClientFactory httpClientFactory,
            @Reference RachioWebHookServletService webHookServletService) {
        this.httpClientFactory = httpClientFactory;
        this.webHookServletService = webHookServletService;
        logger.debug("RachioHandlerFactory activated");
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        
        logger.debug("Creating handler for thing type: {}", thingTypeUID);
        
        if (THING_TYPE_BRIDGE.equals(thingTypeUID)) {
            return createBridgeHandler(thing);
        } else if (THING_TYPE_DEVICE.equals(thingTypeUID)) {
            return createDeviceHandler(thing);
        } else if (THING_TYPE_ZONE.equals(thingTypeUID)) {
            return createZoneHandler(thing);
        }
        
        logger.warn("Unsupported thing type: {}", thingTypeUID);
        return null;
    }

    private RachioBridgeHandler createBridgeHandler(Thing thing) {
        logger.debug("Creating RachioBridgeHandler for thing: {}", thing.getUID());
        
        // Create bridge handler with required dependencies
        RachioBridgeHandler bridgeHandler = new RachioBridgeHandler((Bridge) thing, httpClientFactory);
        
        // Create and set discovery service
        RachioDiscoveryService discovery = new RachioDiscoveryService();
        bridgeHandler.setDiscoveryService(discovery);
        this.discoveryService = discovery;
        
        // Register bridge with webhook servlet service
        webHookServletService.registerBridgeHandler(bridgeHandler);
        
        logger.info("RachioBridgeHandler created for bridge: {}", thing.getUID());
        return bridgeHandler;
    }

    private RachioDeviceHandler createDeviceHandler(Thing thing) {
        logger.debug("Creating RachioDeviceHandler for thing: {}", thing.getUID());
        
        RachioDeviceHandler deviceHandler = new RachioDeviceHandler(thing);
        logger.info("RachioDeviceHandler created for device: {}", thing.getUID());
        return deviceHandler;
    }

    private RachioZoneHandler createZoneHandler(Thing thing) {
        logger.debug("Creating RachioZoneHandler for thing: {}", thing.getUID());
        
        RachioZoneHandler zoneHandler = new RachioZoneHandler(thing);
        logger.info("RachioZoneHandler created for zone: {}", thing.getUID());
        return zoneHandler;
    }

    @Override
    public void unregisterHandler(Thing thing) {
        logger.debug("Unregistering handler for thing: {}", thing.getUID());
        
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        
        if (THING_TYPE_BRIDGE.equals(thingTypeUID)) {
            // Unregister bridge from webhook servlet service
            ThingHandler handler = thing.getHandler();
            if (handler instanceof RachioBridgeHandler) {
                webHookServletService.unregisterBridgeHandler((RachioBridgeHandler) handler);
            }
            
            // Clear discovery service reference
            this.discoveryService = null;
        }
        
        super.unregisterHandler(thing);
        logger.info("Handler unregistered for thing: {}", thing.getUID());
    }

    @Override
    protected void removeHandler(ThingHandler thingHandler) {
        logger.debug("Removing handler: {}", thingHandler.getClass().getSimpleName());
        
        if (thingHandler instanceof RachioBridgeHandler) {
            // Additional cleanup for bridge handler
            RachioBridgeHandler bridgeHandler = (RachioBridgeHandler) thingHandler;
            
            // Unregister from webhook servlet service
            webHookServletService.unregisterBridgeHandler(bridgeHandler);
            
            // Clear discovery service
            RachioDiscoveryService discovery = discoveryService;
            if (discovery != null) {
                discovery.deactivate();
                this.discoveryService = null;
            }
            
            logger.info("Bridge handler removed: {}", bridgeHandler.getThing().getUID());
        }
        
        super.removeHandler(thingHandler);
    }

    /**
     * Get the discovery service instance
     */
    public @Nullable RachioDiscoveryService getDiscoveryService() {
        return discoveryService;
    }

    /**
     * Get the webhook servlet service
     */
    public RachioWebHookServletService getWebHookServletService() {
        return webHookServletService;
    }

    /**
     * Get the image servlet service (if available)
     */
    public @Nullable RachioImageServletService getImageServletService() {
        return imageServletService;
    }

    /**
     * Set the image servlet service (injected by OSGi)
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setImageServletService(RachioImageServletService imageServletService) {
        this.imageServletService = imageServletService;
        logger.debug("ImageServletService injected: {}", imageServletService != null);
    }

    public void unsetImageServletService(RachioImageServletService imageServletService) {
        if (this.imageServletService == imageServletService) {
            this.imageServletService = null;
            logger.debug("ImageServletService unset");
        }
    }

    /**
     * Get all supported thing types
     */
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES_UIDS;
    }

    /**
     * Get binding ID
     */
    public String getBindingId() {
        return BINDING_ID;
    }

    /**
     * Check if handler factory is ready
     */
    public boolean isReady() {
        return httpClientFactory != null && webHookServletService != null;
    }

    /**
     * Get factory status for debugging
     */
    public String getStatus() {
        StringBuilder status = new StringBuilder();
        status.append("RachioHandlerFactory [");
        status.append("binding=").append(BINDING_ID);
        status.append(", ready=").append(isReady());
        status.append(", httpClientFactory=").append(httpClientFactory != null ? "available" : "null");
        status.append(", webHookServletService=").append(webHookServletService != null ? "available" : "null");
        status.append(", imageServletService=").append(imageServletService != null ? "available" : "null");
        status.append(", discoveryService=").append(discoveryService != null ? "created" : "null");
        status.append("]");
        return status.toString();
    }
}
