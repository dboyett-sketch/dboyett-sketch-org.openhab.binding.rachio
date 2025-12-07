package org.openhab.binding.rachio.internal;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.binding.rachio.internal.handler.RachioDeviceHandler;
import org.openhab.binding.rachio.internal.handler.RachioZoneHandler;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioHandlerFactory} is responsible for creating things and thing handlers.
 *
 * @author Michael Lobstein - Initial contribution
 * @author Your Name - Enhanced for OpenHAB 5.x
 */
@NonNullByDefault
@Component(service = ThingHandlerFactory.class, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class RachioHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(RachioHandlerFactory.class);

    // Supported thing types
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Set.of(
            RachioBindingConstants.BRIDGE_TYPE,
            RachioBindingConstants.DEVICE_TYPE,
            RachioBindingConstants.ZONE_TYPE
    );

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        try {
            if (RachioBindingConstants.BRIDGE_TYPE.equals(thingTypeUID)) {
                logger.debug("Creating RachioBridgeHandler for thing: {}", thing.getUID());
                return new RachioBridgeHandler((Bridge) thing);
            } else if (RachioBindingConstants.DEVICE_TYPE.equals(thingTypeUID)) {
                logger.debug("Creating RachioDeviceHandler for thing: {}", thing.getUID());
                return new RachioDeviceHandler(thing);
            } else if (RachioBindingConstants.ZONE_TYPE.equals(thingTypeUID)) {
                logger.debug("Creating RachioZoneHandler for thing: {}", thing.getUID());
                return new RachioZoneHandler(thing);
            }
        } catch (Exception e) {
            logger.error("Error creating handler for thing {}: {}", thing.getUID(), e.getMessage(), e);
        }

        return null;
    }

    @Override
    public void unregisterHandler(Thing thing) {
        try {
            ThingHandler handler = thing.getHandler();
            if (handler != null) {
                // Clean up any resources before unregistering
                if (handler instanceof RachioBridgeHandler) {
                    RachioBridgeHandler bridgeHandler = (RachioBridgeHandler) handler;
                    // Bridge-specific cleanup if needed
                } else if (handler instanceof RachioDeviceHandler) {
                    RachioDeviceHandler deviceHandler = (RachioDeviceHandler) handler;
                    // Device-specific cleanup if needed
                } else if (handler instanceof RachioZoneHandler) {
                    RachioZoneHandler zoneHandler = (RachioZoneHandler) handler;
                    // Zone-specific cleanup if needed
                }
            }
        } catch (Exception e) {
            logger.error("Error during handler unregistration for thing {}: {}", thing.getUID(), e.getMessage(), e);
        }

        super.unregisterHandler(thing);
    }

    @Override
    protected void removeHandler(ThingHandler thingHandler) {
        try {
            // Perform any necessary cleanup before removing the handler
            if (thingHandler instanceof RachioBridgeHandler) {
                RachioBridgeHandler bridgeHandler = (RachioBridgeHandler) thingHandler;
                logger.debug("Removing RachioBridgeHandler for bridge: {}", bridgeHandler.getThing().getUID());
            } else if (thingHandler instanceof RachioDeviceHandler) {
                RachioDeviceHandler deviceHandler = (RachioDeviceHandler) thingHandler;
                logger.debug("Removing RachioDeviceHandler for device: {}", deviceHandler.getThing().getUID());
            } else if (thingHandler instanceof RachioZoneHandler) {
                RachioZoneHandler zoneHandler = (RachioZoneHandler) thingHandler;
                logger.debug("Removing RachioZoneHandler for zone: {}", zoneHandler.getThing().getUID());
            }
        } catch (Exception e) {
            logger.error("Error during handler removal: {}", e.getMessage(), e);
        }

        super.removeHandler(thingHandler);
    }

    /**
     * Validate thing configuration before creation
     *
     * @param thing the thing to validate
     * @return true if the thing configuration is valid
     */
    @Override
    public void validateThingType(ThingTypeUID thingTypeUID) {
        if (!supportsThingType(thingTypeUID)) {
            throw new IllegalArgumentException("Unsupported thing type: " + thingTypeUID);
        }
    }

    /**
     * Get the set of supported thing types
     *
     * @return set of supported thing type UIDs
     */
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES;
    }

    /**
     * Create a handler with additional context (for testing)
     *
     * @param thing the thing
     * @param rachioHttp the RachioHttp instance (for testing)
     * @return the thing handler
     */
    protected @Nullable ThingHandler createHandler(Thing thing, @Nullable RachioHttp rachioHttp) {
        // This method is primarily for testing purposes
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        try {
            if (RachioBindingConstants.BRIDGE_TYPE.equals(thingTypeUID)) {
                logger.debug("Creating RachioBridgeHandler with custom RachioHttp for thing: {}", thing.getUID());
                RachioBridgeHandler handler = new RachioBridgeHandler((Bridge) thing);
                // Note: In a real test scenario, you'd need to inject the RachioHttp
                return handler;
            } else if (RachioBindingConstants.DEVICE_TYPE.equals(thingTypeUID)) {
                logger.debug("Creating RachioDeviceHandler for thing: {}", thing.getUID());
                return new RachioDeviceHandler(thing);
            } else if (RachioBindingConstants.ZONE_TYPE.equals(thingTypeUID)) {
                logger.debug("Creating RachioZoneHandler for thing: {}", thing.getUID());
                return new RachioZoneHandler(thing);
            }
        } catch (Exception e) {
            logger.error("Error creating handler for thing {}: {}", thing.getUID(), e.getMessage(), e);
        }

        return null;
    }

    /**
     * Check if a thing is a bridge
     *
     * @param thingTypeUID the thing type UID
     * @return true if the thing is a bridge
     */
    public boolean isBridge(ThingTypeUID thingTypeUID) {
        return RachioBindingConstants.BRIDGE_TYPE.equals(thingTypeUID);
    }

    /**
     * Check if a thing is a device
     *
     * @param thingTypeUID the thing type UID
     * @return true if the thing is a device
     */
    public boolean isDevice(ThingTypeUID thingTypeUID) {
        return RachioBindingConstants.DEVICE_TYPE.equals(thingTypeUID);
    }

    /**
     * Check if a thing is a zone
     *
     * @param thingTypeUID the thing type UID
     * @return true if the thing is a zone
     */
    public boolean isZone(ThingTypeUID thingTypeUID) {
        return RachioBindingConstants.ZONE_TYPE.equals(thingTypeUID);
    }

    /**
     * Get the parent thing type for a thing type
     *
     * @param thingTypeUID the thing type UID
     * @return the parent thing type UID, or null if no parent
     */
    public @Nullable ThingTypeUID getParentThingType(ThingTypeUID thingTypeUID) {
        if (RachioBindingConstants.DEVICE_TYPE.equals(thingTypeUID) || 
            RachioBindingConstants.ZONE_TYPE.equals(thingTypeUID)) {
            return RachioBindingConstants.BRIDGE_TYPE;
        }
        return null;
    }

    /**
     * Get the hierarchy level for a thing type
     *
     * @param thingTypeUID the thing type UID
     * @return 0 for bridge, 1 for device, 2 for zone, -1 for unknown
     */
    public int getHierarchyLevel(ThingTypeUID thingTypeUID) {
        if (RachioBindingConstants.BRIDGE_TYPE.equals(thingTypeUID)) {
            return 0;
        } else if (RachioBindingConstants.DEVICE_TYPE.equals(thingTypeUID)) {
            return 1;
        } else if (RachioBindingConstants.ZONE_TYPE.equals(thingTypeUID)) {
            return 2;
        }
        return -1;
    }
}
