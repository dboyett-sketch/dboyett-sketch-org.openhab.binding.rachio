package org.openhab.binding.rachio.internal;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.binding.rachio.internal.handler.RachioDeviceHandler;
import org.openhab.binding.rachio.internal.handler.RachioZoneHandler;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TranslationProvider;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Damion Boyett - Refactor contribution
 */
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.rachio")
@NonNullByDefault
public class RachioHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(RachioHandlerFactory.class);

    // Services for internationalization
    private final TranslationProvider translationProvider;
    private final LocaleProvider localeProvider;

    private final HttpClientFactory httpClientFactory;
    private final ScheduledExecutorService scheduler;

    // Supported thing types from binding constants
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(RachioBindingConstants.THING_TYPE_BRIDGE,
            RachioBindingConstants.THING_TYPE_DEVICE, RachioBindingConstants.THING_TYPE_ZONE);

    /**
     * ACTIVATE CONSTRUCTOR - OpenHAB 5.x OSGi injection pattern
     *
     * @param translationProvider Translation service for i18n
     * @param localeProvider Locale service for region-specific content
     * @param httpClientFactory HTTP client factory (REQUIRED for BridgeHandler)
     * @param scheduler Scheduled executor service (REQUIRED for all handlers)
     */
    @Activate
    public RachioHandlerFactory(@Reference TranslationProvider translationProvider,
            @Reference LocaleProvider localeProvider, @Reference HttpClientFactory httpClientFactory,
            @Reference ScheduledExecutorService scheduler) { // REMOVED: ThingHandlerCallback parameter
        this.translationProvider = translationProvider;
        this.localeProvider = localeProvider;
        this.httpClientFactory = httpClientFactory;
        this.scheduler = scheduler;
        logger.debug("RachioHandlerFactory activated with corrected services");
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        try {
            if (RachioBindingConstants.THING_TYPE_BRIDGE.equals(thingTypeUID)) {
                if (thing instanceof Bridge) {
                    RachioBridgeHandler handler = new RachioBridgeHandler((Bridge) thing, httpClientFactory, scheduler); // REMOVED:
                                                                                                                         // thingHandlerCallback
                                                                                                                         // parameter
                    logger.debug("Created RachioBridgeHandler for thing: {}", thing.getUID());
                    return handler;
                } else {
                    logger.error("Failed to create RachioBridgeHandler: thing is not a Bridge");
                    return null;
                }
            } else if (RachioBindingConstants.THING_TYPE_DEVICE.equals(thingTypeUID)) {
                // Create device handler with scheduler service
                RachioDeviceHandler handler = new RachioDeviceHandler(thing, scheduler);
                logger.debug("Created RachioDeviceHandler for thing: {}", thing.getUID());
                return handler;
            } else if (RachioBindingConstants.THING_TYPE_ZONE.equals(thingTypeUID)) {
                // Create zone handler with scheduler service
                RachioZoneHandler handler = new RachioZoneHandler(thing, scheduler);
                logger.debug("Created RachioZoneHandler for thing: {}", thing.getUID());
                return handler;
            }
        } catch (Exception e) {
            logger.error("Error creating handler for thing {}: {}", thing.getUID(), e.getMessage(), e);
        }

        return null;
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof RachioBridgeHandler) {
            logger.debug("Removing RachioBridgeHandler: {}", thingHandler.getThing().getUID());
        } else if (thingHandler instanceof RachioDeviceHandler) {
            logger.debug("Removing RachioDeviceHandler: {}", thingHandler.getThing().getUID());
        } else if (thingHandler instanceof RachioZoneHandler) {
            logger.debug("Removing RachioZoneHandler: {}", thingHandler.getThing().getUID());
        }

        super.removeHandler(thingHandler);
    }

    /**
     * Get the translation provider for internationalization
     *
     * @return The translation provider
     */
    public TranslationProvider getTranslationProvider() {
        return translationProvider;
    }

    /**
     * Get the locale provider for region-specific content
     *
     * @return The locale provider
     */
    public LocaleProvider getLocaleProvider() {
        return localeProvider;
    }
}
