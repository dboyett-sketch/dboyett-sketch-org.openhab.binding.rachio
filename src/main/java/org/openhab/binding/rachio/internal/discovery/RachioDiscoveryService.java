package org.openhab.binding.rachio.internal.discovery;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioDeviceConfiguration;
import org.openhab.binding.rachio.internal.config.RachioZoneConfiguration;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The {@link RachioDiscoveryService} discovers Rachio devices and zones
 * associated with a bridge.
 *
 * @author Dave Boyett - Initial contribution
 */
@Component(scope = ServiceScope.PROTOTYPE, service = DiscoveryService.class)
@NonNullByDefault
public class RachioDiscoveryService extends AbstractDiscoveryService implements ThingHandlerService {

    private final Logger logger = LoggerFactory.getLogger(RachioDiscoveryService.class);
    
    private static final int DISCOVERY_TIMEOUT_SECONDS = 30;
    private static final int BACKGROUND_DISCOVERY_INTERVAL_MINUTES = 60;
    
    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable ScheduledFuture<?> backgroundDiscoveryJob;
    private final HttpClientFactory httpClientFactory;
    
    private final Set<ThingUID> discoveredDevices = new HashSet<>();
    private final Set<ThingUID> discoveredZones = new HashSet<>();

    public RachioDiscoveryService(@Reference HttpClientFactory httpClientFactory) {
        super(Collections.singleton(RachioBindingConstants.THING_TYPE_DEVICE), DISCOVERY_TIMEOUT_SECONDS, true);
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof RachioBridgeHandler) {
            this.bridgeHandler = (RachioBridgeHandler) handler;
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
        super.deactivate();
        stopBackgroundDiscovery();
    }

    @Override
    public void startScan() {
        logger.debug("Starting Rachio discovery scan");
        removeOlderResults(getTimestampOfLastScan());
        
        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler == null) {
            logger.warn("Bridge handler not available for discovery");
            return;
        }
        
        try {
            discoverDevices(localBridgeHandler);
            logger.info("Rachio discovery scan completed");
        } catch (Exception e) {
            logger.error("Error during discovery scan: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Starting Rachio background discovery");
        
        ScheduledFuture<?> backgroundDiscoveryJob = this.backgroundDiscoveryJob;
        if (backgroundDiscoveryJob != null && !backgroundDiscoveryJob.isCancelled()) {
            backgroundDiscoveryJob.cancel(true);
        }
        
        this.backgroundDiscoveryJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                startScan();
            } catch (Exception e) {
                logger.debug("Error in background discovery: {}", e.getMessage());
            }
        }, 5, BACKGROUND_DISCOVERY_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("Stopping Rachio background discovery");
        
        ScheduledFuture<?> backgroundDiscoveryJob = this.backgroundDiscoveryJob;
        if (backgroundDiscoveryJob != null && !backgroundDiscoveryJob.isCancelled()) {
            backgroundDiscoveryJob.cancel(true);
            this.backgroundDiscoveryJob = null;
        }
    }

    private void discoverDevices(RachioBridgeHandler bridgeHandler) {
        try {
            RachioHttp httpHandler = bridgeHandler.getHttpHandler();
            if (httpHandler == null) {
                logger.warn("HTTP handler not available for discovery");
                return;
            }
            
            // Get person info to find devices
            RachioPerson person = httpHandler.getPerson();
            if (person == null || person.devices == null) {
                logger.info("No devices found for Rachio account");
                return;
            }
            
            logger.debug("Found {} devices for Rachio account", person.devices.size());
            
            // Discover each device
            for (RachioDevice device : person.devices) {
                discoverDevice(bridgeHandler, device);
            }
            
        } catch (RachioApiException e) {
            logger.error("Failed to discover devices: {}", e.getMessage(), e);
        }
    }

    private void discoverDevice(RachioBridgeHandler bridgeHandler, RachioDevice device) {
        ThingUID bridgeUID = bridgeHandler.getThing().getUID();
        ThingUID deviceThingUID = new ThingUID(RachioBindingConstants.THING_TYPE_DEVICE, bridgeUID, 
                sanitizeId(device.id));
        
        // Check if this device was already discovered
        if (discoveredDevices.contains(deviceThingUID)) {
            logger.debug("Device {} already discovered, skipping", device.name);
            return;
        }
        
        logger.info("Discovered Rachio device: {} (ID: {})", device.name, device.id);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(RachioBindingConstants.PROPERTY_DEVICE_ID, device.id);
        properties.put(RachioBindingConstants.PROPERTY_DEVICE_NAME, device.name);
        properties.put(RachioBindingConstants.PROPERTY_DEVICE_SERIAL, device.serialNumber);
        properties.put(RachioBindingConstants.PROPERTY_DEVICE_MODEL, device.model);
        properties.put(RachioBindingConstants.PROPERTY_ZONES, String.valueOf(device.zones != null ? device.zones.size() : 0));
        
        if (device.macAddress != null && !device.macAddress.isEmpty()) {
            properties.put(RachioBindingConstants.PROPERTY_MAC, device.macAddress);
        }
        
        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(deviceThingUID)
                .withBridge(bridgeUID)
                .withProperties(properties)
                .withRepresentationProperty(RachioBindingConstants.PROPERTY_DEVICE_ID)
                .withLabel(device.name + " (Rachio Controller)")
                .build();
        
        thingDiscovered(discoveryResult);
        discoveredDevices.add(deviceThingUID);
        
        // Discover zones for this device
        if (device.zones != null && !device.zones.isEmpty()) {
            discoverZones(bridgeHandler, device);
        }
    }

    private void discoverZones(RachioBridgeHandler bridgeHandler, RachioDevice device) {
        ThingUID bridgeUID = bridgeHandler.getThing().getUID();
        
        // First create device thing UID for zone bridge reference
        ThingUID deviceThingUID = new ThingUID(RachioBindingConstants.THING_TYPE_DEVICE, bridgeUID, 
                sanitizeId(device.id));
        
        if (device.zones == null) {
            return;
        }
        
        logger.debug("Discovering {} zones for device {}", device.zones.size(), device.name);
        
        for (RachioZone zone : device.zones) {
            discoverZone(bridgeHandler, deviceThingUID, device, zone);
        }
    }

    private void discoverZone(RachioBridgeHandler bridgeHandler, ThingUID deviceThingUID, 
                              RachioDevice device, RachioZone zone) {
        ThingUID zoneThingUID = new ThingUID(RachioBindingConstants.THING_TYPE_ZONE, deviceThingUID, 
                sanitizeId(zone.id));
        
        // Check if this zone was already discovered
        if (discoveredZones.contains(zoneThingUID)) {
            logger.debug("Zone {} already discovered, skipping", zone.name);
            return;
        }
        
        logger.info("Discovered Rachio zone: {} (Device: {})", zone.name, device.name);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(RachioBindingConstants.PROPERTY_ZONE_ID, zone.id);
        properties.put(RachioBindingConstants.PROPERTY_ZONE_NAME, zone.name);
        properties.put(RachioBindingConstants.PROPERTY_ZONE_NUMBER, String.valueOf(zone.zoneNumber));
        properties.put(RachioBindingConstants.PROPERTY_DEVICE_ID, device.id);
        properties.put(RachioBindingConstants.PROPERTY_DEVICE_NAME, device.name);
        
        // Add professional irrigation data as properties
        if (zone.customSoil != null) {
            properties.put("soilType", zone.customSoil.name);
        }
        if (zone.customCrop != null) {
            properties.put("cropType", zone.customCrop.name);
            properties.put("cropCoefficient", String.valueOf(zone.customCrop.coefficient));
        }
        if (zone.customNozzle != null) {
            properties.put("nozzleType", zone.customNozzle.name);
            properties.put("nozzleRate", String.valueOf(zone.customNozzle.inchesPerHour));
        }
        if (zone.efficiency > 0) {
            properties.put("efficiency", String.valueOf(zone.efficiency));
        }
        if (zone.rootZoneDepth > 0) {
            properties.put("rootDepth", String.valueOf(zone.rootZoneDepth));
        }
        if (zone.yardAreaSquareFeet > 0) {
            properties.put("area", String.valueOf(zone.yardAreaSquareFeet));
        }
        
        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(zoneThingUID)
                .withBridge(deviceThingUID)
                .withProperties(properties)
                .withRepresentationProperty(RachioBindingConstants.PROPERTY_ZONE_ID)
                .withLabel(zone.name + " (Rachio Zone)")
                .build();
        
        thingDiscovered(discoveryResult);
        discoveredZones.add(zoneThingUID);
    }

    /**
     * Manually trigger discovery for a specific bridge
     */
    public void triggerDiscovery() {
        logger.debug("Manual discovery triggered");
        startScan();
    }

    /**
     * Remove discovered things when bridge is removed
     */
    public void removeDiscoveredThings() {
        logger.debug("Removing discovered things");
        
        // Remove discovered devices
        for (ThingUID deviceUID : discoveredDevices) {
            thingRemoved(deviceUID);
        }
        discoveredDevices.clear();
        
        // Remove discovered zones
        for (ThingUID zoneUID : discoveredZones) {
            thingRemoved(zoneUID);
        }
        discoveredZones.clear();
    }

    /**
     * Update discovery results when things are manually added/removed
     */
    public void updateDiscoveryResults() {
        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler == null) {
            return;
        }
        
        // Get currently managed things
        Set<ThingUID> managedDevices = new HashSet<>();
        Set<ThingUID> managedZones = new HashSet<>();
        
        // Check bridge for child things
        Thing bridgeThing = localBridgeHandler.getThing();
        for (Thing thing : bridgeThing.getThings()) {
            ThingTypeUID thingType = thing.getThingTypeUID();
            
            if (RachioBindingConstants.THING_TYPE_DEVICE.equals(thingType)) {
                managedDevices.add(thing.getUID());
                
                // Check for zone children
                for (Thing childThing : thing.getThings()) {
                    if (RachioBindingConstants.THING_TYPE_ZONE.equals(childThing.getThingTypeUID())) {
                        managedZones.add(childThing.getUID());
                    }
                }
            }
        }
        
        // Remove discovery results for things that are now managed
        for (ThingUID deviceUID : managedDevices) {
            if (discoveredDevices.contains(deviceUID)) {
                thingRemoved(deviceUID);
                discoveredDevices.remove(deviceUID);
            }
        }
        
        for (ThingUID zoneUID : managedZones) {
            if (discoveredZones.contains(zoneUID)) {
                thingRemoved(zoneUID);
                discoveredZones.remove(zoneUID);
            }
        }
    }

    /**
     * Sanitize ID for use in ThingUID (remove special characters)
     */
    private String sanitizeId(String id) {
        // Remove any characters that aren't alphanumeric or hyphen
        return id.replaceAll("[^a-zA-Z0-9_-]", "-");
    }

    /**
     * Get discovered device count
     */
    public int getDiscoveredDeviceCount() {
        return discoveredDevices.size();
    }

    /**
     * Get discovered zone count
     */
    public int getDiscoveredZoneCount() {
        return discoveredZones.size();
    }

    /**
     * Check if a specific device has been discovered
     */
    public boolean isDeviceDiscovered(String deviceId) {
        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler == null) {
            return false;
        }
        
        ThingUID bridgeUID = localBridgeHandler.getThing().getUID();
        ThingUID deviceUID = new ThingUID(RachioBindingConstants.THING_TYPE_DEVICE, bridgeUID, 
                sanitizeId(deviceId));
        
        return discoveredDevices.contains(deviceUID);
    }

    /**
     * Check if a specific zone has been discovered
     */
    public boolean isZoneDiscovered(String zoneId) {
        // We need to check all discovered zones
        for (ThingUID zoneUID : discoveredZones) {
            if (zoneUID.getId().contains(sanitizeId(zoneId))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        Set<ThingTypeUID> supportedThingTypes = new HashSet<>();
        supportedThingTypes.add(RachioBindingConstants.THING_TYPE_DEVICE);
        supportedThingTypes.add(RachioBindingConstants.THING_TYPE_ZONE);
        return supportedThingTypes;
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        logger.debug("Rachio discovery scan stopped");
    }
}
