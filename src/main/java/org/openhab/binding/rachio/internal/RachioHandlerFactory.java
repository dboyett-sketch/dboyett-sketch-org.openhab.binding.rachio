package org.openhab.binding.rachio.internal;

import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;

@Component(service = ThingHandlerFactory.class,
           configurationPid = "binding.rachio",
           property = {
               "service.config.description.uri=binding:rachio",
               "service.config.label=Rachio Binding",
               "service.config.category=iot"
           })
public class RachioHandlerFactory extends BaseThingHandlerFactory {
    
    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return RachioBindingConstants.SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }
    
    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        
        if (thingTypeUID.equals(RachioBindingConstants.THING_TYPE_BRIDGE)) {
            // 🎯 CLEAN: No factory parameter needed
            return new RachioBridgeHandler((Bridge) thing);
        } else if (thingTypeUID.equals(RachioBindingConstants.THING_TYPE_DEVICE)) {
            return new RachioDeviceHandler(thing);
        } else if (thingTypeUID.equals(RachioBindingConstants.THING_TYPE_ZONE)) {
            return new RachioZoneHandler(thing);
        }
        
        return null;
    }
}
