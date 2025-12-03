package org.openhab.binding.rachio.internal;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.binding.rachio.internal.handler.RachioDeviceHandler;
import org.openhab.binding.rachio.internal.handler.RachioZoneHandler;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.ThingRegistry;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

/**
 * The {@link RachioHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Michael Lobstein - Initial contribution
 */

@NonNullByDefault
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.rachio")
public class RachioHandlerFactory extends BaseThingHandlerFactory {
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(
            THING_TYPE_BRIDGE,
            THING_TYPE_DEVICE,
            THING_TYPE_ZONE);

    private final Logger logger = LoggerFactory.getLogger(RachioHandlerFactory.class);
    private final ThingRegistry thingRegistry;

    @Activate
    public RachioHandlerFactory(@Reference ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    @Activate
    protected void activate(ComponentContext componentContext) {
        logger.debug("RachioHandlerFactory activated");
        super.activate(componentContext);
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        logger.debug("RachioHandlerFactory deactivated");
        super.deactivate(componentContext);
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    // FIX: REMOVED the unsupported supportsBridge method
    // The method 'supportsBridge(ThingTypeUID)' doesn't exist in BaseThingHandlerFactory
    // Bridge support is handled automatically by the framework

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_BRIDGE.equals(thingTypeUID)) {
            logger.debug("Creating bridge handler for thing: {}", thing.getUID());
            return new RachioBridgeHandler((Bridge) thing);
        } else if (THING_TYPE_DEVICE.equals(thingTypeUID)) {
            logger.debug("Creating device handler for thing: {} with bridge UID: {}", 
                        thing.getUID(), thing.getBridgeUID());
            
            // For device things, require a bridge
            ThingUID bridgeUID = thing.getBridgeUID();
            if (bridgeUID == null) {
                logger.warn("Cannot create device handler without a bridge. Thing: {}", thing.getUID());
                return null;
            }
            
            // Use the simpler constructor - bridge will be accessed via getBridge() in handler
            logger.debug("Creating device handler for thing: {}", thing.getUID());
            return new RachioDeviceHandler(thing);
            
        } else if (THING_TYPE_ZONE.equals(thingTypeUID)) {
            logger.debug("Creating zone handler for thing: {} with bridge UID: {}", 
                        thing.getUID(), thing.getBridgeUID());
            
            // For zone things, require a bridge
            ThingUID bridgeUID = thing.getBridgeUID();
            if (bridgeUID == null) {
                logger.warn("Cannot create zone handler without a bridge. Thing: {}", thing.getUID());
                return null;
            }
            
            // Use the simpler constructor - bridge will be accessed via getBridge() in handler
            logger.debug("Creating zone handler for thing: {}", thing.getUID());
            return new RachioZoneHandler(thing);
        }

        return null;
    }
}