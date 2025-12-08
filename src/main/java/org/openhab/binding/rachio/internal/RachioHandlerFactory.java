package org.openhab.binding.rachio.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioSecurity;
import org.openhab.binding.rachio.internal.api.RachioWebHookServletService;
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

import java.util.Set;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

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
    private final RachioSecurity security;
    private final RachioWebHookServletService webhookService;

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(
            THING_TYPE_BRIDGE, THING_TYPE_DEVICE, THING_TYPE_ZONE);

    @Activate
    public RachioHandlerFactory(@Reference HttpClientFactory httpClientFactory,
            @Reference RachioSecurity security,
            @Reference RachioWebHookServletService webhookService) {
        this.httpClientFactory = httpClientFactory;
        this.security = security;
        this.webhookService = webhookService;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_BRIDGE.equals(thingTypeUID)) {
            return new RachioBridgeHandler((Bridge) thing, httpClientFactory, security, webhookService);
        } else if (THING_TYPE_DEVICE.equals(thingTypeUID)) {
            return new RachioDeviceHandler(thing);
        } else if (THING_TYPE_ZONE.equals(thingTypeUID)) {
            return new RachioZoneHandler(thing);
        }

        return null;
    }
}
