package org.openhab.binding.rachio.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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

/**
 * The {@link RachioHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.rachio", service = ThingHandlerFactory.class)
public class RachioHandlerFactory extends BaseThingHandlerFactory {
    private final HttpClientFactory httpClientFactory;

    @Activate
    public RachioHandlerFactory(@Reference HttpClientFactory httpClientFactory) {
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return RachioBindingConstants.SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (RachioBindingConstants.THING_TYPE_BRIDGE.equals(thingTypeUID)) {
            return new RachioBridgeHandler((Bridge) thing, httpClientFactory);
        } else if (RachioBindingConstants.THING_TYPE_DEVICE.equals(thingTypeUID)) {
            return new RachioDeviceHandler(thing);
        } else if (RachioBindingConstants.THING_TYPE_ZONE.equals(thingTypeUID)) {
            return new RachioZoneHandler(thing);
        }

        return null;
    }
}
