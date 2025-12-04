package org.openhab.binding.rachio.internal;

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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioHandlerFactory} is responsible for creating things and thing handlers.
 *
 * @author Brian G. - Initial contribution (from 2.5 binding)
 * @author Daniel B. - Major rewrite for OpenHAB 5.x
 */
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.rachio")
@NonNullByDefault
public class RachioHandlerFactory extends BaseThingHandlerFactory {
    private final Logger logger = LoggerFactory.getLogger(RachioHandlerFactory.class);

    // API Client - injected via OSGi
    private final RachioHttp rachioHttp;

    @Activate
    public RachioHandlerFactory(@Reference RachioHttp rachioHttp) {
        this.rachioHttp = rachioHttp;
        logger.debug("Rachio handler factory initialized");
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return RachioBindingConstants.SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (RachioBindingConstants.THING_TYPE_BRIDGE.equals(thingTypeUID)) {
            logger.debug("Creating Rachio bridge handler for {}", thing.getUID());
            return new RachioBridgeHandler(rachioHttp, (Bridge) thing);
        } else if (RachioBindingConstants.THING_TYPE_DEVICE.equals(thingTypeUID)) {
            logger.debug("Creating Rachio device handler for {}", thing.getUID());
            return new RachioDeviceHandler(thing);
        } else if (RachioBindingConstants.THING_TYPE_ZONE.equals(thingTypeUID)) {
            logger.debug("Creating Rachio zone handler for {}", thing.getUID());
            return new RachioZoneHandler(rachioHttp, thing);
        }

        logger.warn("Unsupported thing type: {}", thingTypeUID);
        return null;
    }

    @Override
    public void unregisterHandler(Thing thing) {
        super.unregisterHandler(thing);
        logger.debug("Unregistered handler for {}", thing.getUID());
    }

    @Override
    public void removeThing(Thing thing) {
        super.removeThing(thing);
        logger.debug("Removed thing {}", thing.getUID());
    }

    /**
     * Get the API client instance
     */
    public RachioHttp getRachioHttp() {
        return rachioHttp;
    }
}
