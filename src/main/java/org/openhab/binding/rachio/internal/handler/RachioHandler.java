package org.openhab.binding.rachio.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

@NonNullByDefault
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.rachio")
public class RachioHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(RachioHandlerFactory.class);
    private final HttpClientFactory httpClientFactory;

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Set.of(
        THING_TYPE_BRIDGE,
        THING_TYPE_DEVICE,
        THING_TYPE_ZONE
    );

    @Activate
    public RachioHandlerFactory(@Reference HttpClientFactory httpClientFactory) {
        this.httpClientFactory = httpClientFactory;
        logger.debug("RachioHandlerFactory activated");
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        boolean supported = SUPPORTED_THING_TYPES.contains(thingTypeUID);
        logger.debug("Checking support for thing type {}: {}", thingTypeUID, supported);
        return supported;
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        logger.debug("Creating handler for thing type: {}", thingTypeUID);

        if (THING_TYPE_BRIDGE.equals(thingTypeUID)) {
            logger.debug("Creating RachioBridgeHandler");
            return new org.openhab.binding.rachio.internal.handler.RachioBridgeHandler((Bridge) thing, httpClientFactory);
        } else if (THING_TYPE_DEVICE.equals(thingTypeUID)) {
            logger.debug("Creating RachioDeviceHandler");
            return new org.openhab.binding.rachio.internal.handler.RachioDeviceHandler(thing);
        } else if (THING_TYPE_ZONE.equals(thingTypeUID)) {
            logger.debug("Creating RachioZoneHandler");
            return new org.openhab.binding.rachio.internal.handler.RachioZoneHandler(thing);
        }

        logger.warn("Unknown thing type: {}", thingTypeUID);
        return null;
    }
}
