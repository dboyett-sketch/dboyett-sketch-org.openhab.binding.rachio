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
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

/**
 * The {@link RachioHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
@Component(service = ThingHandlerFactory.class, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class RachioHandlerFactory extends BaseThingHandlerFactory {
    
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(
        RachioBindingConstants.THING_TYPE_BRIDGE,
        RachioBindingConstants.THING_TYPE_DEVICE,
        RachioBindingConstants.THING_TYPE_ZONE
    );
    
    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }
    
    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        
        if (RachioBindingConstants.THING_TYPE_BRIDGE.equals(thingTypeUID)) {
            return new RachioBridgeHandler((Bridge) thing);
        } else if (RachioBindingConstants.THING_TYPE_DEVICE.equals(thingTypeUID)) {
            return new RachioDeviceHandler(thing);
        } else if (RachioBindingConstants.THING_TYPE_ZONE.equals(thingTypeUID)) {
            return new RachioZoneHandler(thing);
        }
        
        return null;
    }
    
    // FIXED: Removed the incorrect @Override annotation from these methods
    // They were trying to override methods that don't exist in the parent class
    
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES_UIDS;
    }
    
    public boolean isBridge(ThingTypeUID thingTypeUID) {
        return RachioBindingConstants.THING_TYPE_BRIDGE.equals(thingTypeUID);
    }
    
    public boolean isDevice(ThingTypeUID thingTypeUID) {
        return RachioBindingConstants.THING_TYPE_DEVICE.equals(thingTypeUID);
    }
    
    public boolean isZone(ThingTypeUID thingTypeUID) {
        return RachioBindingConstants.THING_TYPE_ZONE.equals(thingTypeUID);
    }
    
    public String getThingTypeLabel(ThingTypeUID thingTypeUID) {
        if (RachioBindingConstants.THING_TYPE_BRIDGE.equals(thingTypeUID)) {
            return "Rachio Bridge";
        } else if (RachioBindingConstants.THING_TYPE_DEVICE.equals(thingTypeUID)) {
            return "Rachio Device";
        } else if (RachioBindingConstants.THING_TYPE_ZONE.equals(thingTypeUID)) {
            return "Rachio Zone";
        }
        return "Unknown";
    }
    
    public String getThingTypeDescription(ThingTypeUID thingTypeUID) {
        if (RachioBindingConstants.THING_TYPE_BRIDGE.equals(thingTypeUID)) {
            return "Represents a connection to the Rachio cloud service";
        } else if (RachioBindingConstants.THING_TYPE_DEVICE.equals(thingTypeUID)) {
            return "Represents a Rachio irrigation controller";
        } else if (RachioBindingConstants.THING_TYPE_ZONE.equals(thingTypeUID)) {
            return "Represents an individual irrigation zone";
        }
        return "";
    }
    
    // Helper methods for thing type strings (used in the original code)
    public String getBridgeTypeString() {
        return RachioBindingConstants.BRIDGE_TYPE;
    }
    
    public String getDeviceTypeString() {
        return RachioBindingConstants.DEVICE_TYPE;
    }
    
    public String getZoneTypeString() {
        return RachioBindingConstants.ZONE_TYPE;
    }
    
    // Factory methods for testing
    public RachioBridgeHandler createBridgeHandler(Bridge bridge) {
        return new RachioBridgeHandler(bridge);
    }
    
    public RachioDeviceHandler createDeviceHandler(Thing thing) {
        return new RachioDeviceHandler(thing);
    }
    
    public RachioZoneHandler createZoneHandler(Thing thing) {
        return new RachioZoneHandler(thing);
    }
    
    // Compatibility methods
    public static RachioHandlerFactory getInstance() {
        // Note: This is not the standard OSGi way, but provided for compatibility
        return new RachioHandlerFactory();
    }
    
    @Override
    public String toString() {
        return "RachioHandlerFactory[supportedTypes=" + SUPPORTED_THING_TYPES_UIDS.size() + "]";
    }
}
