package org.openhab.binding.rachio.internal.discovery;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.CustomCrop;
import org.openhab.binding.rachio.internal.api.dto.CustomNozzle;
import org.openhab.binding.rachio.internal.api.dto.CustomShade;
import org.openhab.binding.rachio.internal.api.dto.CustomSlope;
import org.openhab.binding.rachio.internal.api.dto.CustomSoil;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.i18n.LocationProvider;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovery service for Rachio devices and zones
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDiscoveryService extends AbstractDiscoveryService implements ThingHandlerService, DiscoveryService {
    private static final int DISCOVERY_TIMEOUT_SECONDS = 30;
    private static final int BACKGROUND_DISCOVERY_INTERVAL_SECONDS = 300; // 5 minutes
    
    private final Logger logger = LoggerFactory.getLogger(RachioDiscoveryService.class);
    private final Map<String, DiscoveryResult> discoveryResults = new ConcurrentHashMap<>();
    
    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable LocationProvider locationProvider;
    private @Nullable ScheduledFuture<?> backgroundDiscoveryJob;

    public RachioDiscoveryService() {
        super(SUPPORTED_THING_TYPES_UIDS, DISCOVERY_TIMEOUT_SECONDS, false);
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof RachioBridgeHandler) {
            bridgeHandler = (RachioBridgeHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler;
    }

    @Override
    public void activate() {
        super.activate(null);
    }

    @Override
    public void deactivate() {
        stopBackgroundDiscovery();
        super.deactivate();
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES_UIDS;
    }

    @Override
    protected void startScan() {
        logger.debug("Starting Rachio discovery scan");
        discoverDevices();
        discoverZones();
    }

    @Override
    protected synchronized void stopScan() {
        logger.debug("Stopping Rachio discovery scan");
        super.stopScan();
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Starting Rachio background discovery");
        stopBackgroundDiscovery();
        
        backgroundDiscoveryJob = scheduler.scheduleWithFixedDelay(() -> {
            discoverDevices();
            discoverZones();
        }, 0, BACKGROUND_DISCOVERY_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        ScheduledFuture<?> localBackgroundDiscoveryJob = backgroundDiscoveryJob;
        if (localBackgroundDiscoveryJob != null && !localBackgroundDiscoveryJob.isCancelled()) {
            localBackgroundDiscoveryJob.cancel(true);
            backgroundDiscoveryJob = null;
            logger.debug("Stopped Rachio background discovery");
        }
    }

    /**
     * Discover Rachio devices
     */
    private void discoverDevices() {
        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler == null) {
            logger.debug("Bridge handler not available for discovery");
            return;
        }

        try {
            Collection<RachioDevice> devices = localBridgeHandler.getDevices();
            if (devices == null || devices.isEmpty()) {
                logger.debug("No devices found for discovery");
                return;
            }

            for (RachioDevice device : devices) {
                if (device != null && device.id != null) {
                    discoverDevice(device);
                }
            }
        } catch (Exception e) {
            logger.warn("Error discovering devices: {}", e.getMessage(), e);
        }
    }

    /**
     * Discover a single device
     */
    private void discoverDevice(RachioDevice device) {
        if (device.id == null || device.name == null) {
            logger.debug("Skipping device with null ID or name");
            return;
        }

        ThingUID bridgeUID = bridgeHandler != null ? bridgeHandler.getThing().getUID() : null;
        if (bridgeUID == null) {
            logger.debug("Bridge UID not available");
            return;
        }

        ThingUID thingUID = new ThingUID(THING_TYPE_DEVICE, bridgeUID, device.id);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(PROPERTY_ID, device.id);
        properties.put(PROPERTY_NAME, device.name);
        
        if (device.model != null) {
            properties.put(PROPERTY_MODEL, device.model);
        }
        
        if (device.serialNumber != null) {
            properties.put(PROPERTY_SERIAL_NUMBER, device.serialNumber);
        }
        
        if (device.macAddress != null) {
            properties.put(PROPERTY_MAC_ADDRESS, device.macAddress);
        }
        
        if (device.status != null) {
            properties.put(PROPERTY_STATUS, device.status);
        }
        
        if (device.zones != null) {
            properties.put(PROPERTY_ZONE_COUNT, device.zones.size());
        }
        
        // Add professional data properties
        if (device.elevation != null) {
            properties.put(PROPERTY_ELEVATION, device.elevation);
        }
        
        if (device.flexScheduleRules != null) {
            properties.put(PROPERTY_FLEX_SCHEDULE, device.flexScheduleRules);
        }
        
        if (device.createdDate != null) {
            properties.put(PROPERTY_CREATED_DATE, device.createdDate.toString());
        }
        
        if (device.updatedDate != null) {
            properties.put(PROPERTY_UPDATED_DATE, device.updatedDate.toString());
        }

        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
            .withBridge(bridgeUID)
            .withProperties(properties)
            .withLabel(device.name + " (Rachio Sprinkler Controller)")
            .withRepresentationProperty(PROPERTY_ID)
            .build();

        thingDiscovered(discoveryResult);
        discoveryResults.put(device.id, discoveryResult);
        
        logger.debug("Discovered Rachio device: {} - {}", device.id, device.name);
    }

    /**
     * Discover Rachio zones
     */
    private void discoverZones() {
        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler == null) {
            logger.debug("Bridge handler not available for zone discovery");
            return;
        }

        try {
            Collection<RachioDevice> devices = localBridgeHandler.getDevices();
            if (devices == null || devices.isEmpty()) {
                logger.debug("No devices found for zone discovery");
                return;
            }

            for (RachioDevice device : devices) {
                if (device != null && device.id != null && device.zones != null) {
                    for (RachioZone zone : device.zones) {
                        if (zone != null && zone.id != null) {
                            discoverZone(device, zone);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error discovering zones: {}", e.getMessage(), e);
        }
    }

    /**
     * Discover a single zone
     */
    private void discoverZone(RachioDevice device, RachioZone zone) {
        if (zone.id == null || zone.name == null) {
            logger.debug("Skipping zone with null ID or name");
            return;
        }

        ThingUID bridgeUID = bridgeHandler != null ? bridgeHandler.getThing().getUID() : null;
        if (bridgeUID == null) {
            logger.debug("Bridge UID not available");
            return;
        }

        ThingUID thingUID = new ThingUID(THING_TYPE_ZONE, bridgeUID, zone.id);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(PROPERTY_ID, zone.id);
        properties.put(PROPERTY_NAME, zone.name);
        properties.put(PROPERTY_DEVICE_ID, device.id);
        properties.put(PROPERTY_ZONE_NUMBER, zone.zoneNumber != null ? zone.zoneNumber : 0);
        
        if (zone.enabled != null) {
            properties.put(PROPERTY_ENABLED, zone.enabled);
        }
        
        if (zone.runtime != null) {
            properties.put(PROPERTY_RUNTIME, zone.runtime);
        }
        
        // Add professional data properties
        if (zone.area != null) {
            properties.put(PROPERTY_AREA, zone.area);
        }
        
        if (zone.soil != null) {
            properties.put(PROPERTY_SOIL_TYPE, zone.soil.name != null ? zone.soil.name : "UNKNOWN");
        }
        
        if (zone.crop != null) {
            properties.put(PROPERTY_CROP_TYPE, zone.crop.name != null ? zone.crop.name : "UNKNOWN");
        }
        
        if (zone.nozzle != null) {
            properties.put(PROPERTY_NOZZLE_TYPE, zone.nozzle.name != null ? zone.nozzle.name : "UNKNOWN");
        }
        
        if (zone.slope != null) {
            properties.put(PROPERTY_SLOPE_TYPE, zone.slope.name != null ? zone.slope.name : "UNKNOWN");
        }
        
        if (zone.shade != null) {
            properties.put(PROPERTY_SHADE_TYPE, zone.shade.name != null ? zone.shade.name : "UNKNOWN");
        }
        
        if (zone.rootZoneDepth != null) {
            properties.put(PROPERTY_ROOT_ZONE_DEPTH, zone.rootZoneDepth);
        }
        
        if (zone.efficiency != null) {
            properties.put(PROPERTY_EFFICIENCY, zone.efficiency);
        }
        
        if (zone.availableWater != null) {
            properties.put(PROPERTY_AVAILABLE_WATER, zone.availableWater);
        }
        
        if (zone.wateringAdjustmentRuntimes != null && !zone.wateringAdjustmentRuntimes.isEmpty()) {
            for (int i = 0; i < Math.min(zone.wateringAdjustmentRuntimes.size(), 5); i++) {
                properties.put(PROPERTY_ADJUSTMENT_LEVEL_PREFIX + (i + 1), 
                    zone.wateringAdjustmentRuntimes.get(i));
            }
        }

        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
            .withBridge(bridgeUID)
            .withProperties(properties)
            .withLabel(zone.name + " (Rachio Zone)")
            .withRepresentationProperty(PROPERTY_ID)
            .build();

        thingDiscovered(discoveryResult);
        discoveryResults.put(zone.id, discoveryResult);
        
        logger.debug("Discovered Rachio zone: {} - {} (Device: {})", zone.id, zone.name, device.name);
    }

    /**
     * Remove a discovery result
     */
    public void removeDiscoveryResult(String thingId) {
        DiscoveryResult result = discoveryResults.remove(thingId);
        if (result != null) {
            thingRemoved(result.getThingUID());
            logger.debug("Removed discovery result for thing: {}", thingId);
        }
    }

    /**
     * Get all discovery results
     */
    public Collection<DiscoveryResult> getDiscoveryResults() {
        return discoveryResults.values();
    }

    /**
     * Set location provider (optional)
     */
    public void setLocationProvider(@Nullable LocationProvider locationProvider) {
        this.locationProvider = locationProvider;
    }

    /**
     * Unset location provider
     */
    public void unsetLocationProvider(@Nullable LocationProvider locationProvider) {
        if (this.locationProvider == locationProvider) {
            this.locationProvider = null;
        }
    }
}
