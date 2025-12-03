package org.openhab.binding.rachio.internal.discovery;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.openhab.core.config.discovery.DiscoveryService;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.Bridge;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioDiscoveryService} discovers Rachio controllers from configured bridges.
 *
 * @author Brian Gleason - Initial contribution
 */
@Component(service = DiscoveryService.class, immediate = true, configurationPid = "discovery.rachio")
@NonNullByDefault
public class RachioDiscoveryService extends AbstractDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(RachioDiscoveryService.class);

    private final ThingRegistry thingRegistry;

    private @Nullable ScheduledFuture<?> backgroundDiscoveryJob;

    @Activate
    public RachioDiscoveryService(@Reference ThingRegistry thingRegistry) {
        super(SUPPORTED_THING_TYPES_UIDS, 30, true); // Increased timeout for bridge discovery
        this.thingRegistry = thingRegistry;
    }

    @Activate
    @Override
    public void activate(@Nullable Map<String, @Nullable Object> configProperties) {
        super.activate(configProperties);
        startBackgroundDiscovery();
        logger.info("Rachio discovery service activated");
    }

    @Override
    public void deactivate() {
        stopBackgroundDiscovery();
        super.deactivate();
        logger.info("Rachio discovery service deactivated");
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES_UIDS;
    }

    @Override
    public void startScan() {
        logger.debug("Starting Rachio discovery scan");
        discoverDevicesFromBridges();
    }

    @Override
    public synchronized void stopScan() {
        super.stopScan();
        logger.debug("Stopped Rachio discovery scan");
    }

    /**
     * Discover devices from all configured Rachio bridges
     */
    private void discoverDevicesFromBridges() {
        logger.debug("Discovering devices from configured Rachio bridges");
        
        // Find all configured Rachio bridges and discover their devices
        thingRegistry.getAll().stream()
            .filter(thing -> THING_TYPE_BRIDGE.equals(thing.getThingTypeUID()))
            .filter(thing -> thing instanceof Bridge)
            .map(thing -> (Bridge) thing)
            .forEach(this::discoverDevicesFromBridge);
    }

    /**
     * Discover devices from a specific bridge
     */
    private void discoverDevicesFromBridge(Bridge bridge) {
        logger.debug("Discovering devices from bridge: {}", bridge.getUID());
        
        // Get the bridge handler to access discovered devices
        org.openhab.core.thing.binding.ThingHandler handler = bridge.getHandler();
        if (handler instanceof org.openhab.binding.rachio.internal.handler.RachioBridgeHandler) {
            org.openhab.binding.rachio.internal.handler.RachioBridgeHandler bridgeHandler = 
                (org.openhab.binding.rachio.internal.handler.RachioBridgeHandler) handler;
            
            // Check if bridge is online before attempting discovery
            if (bridge.getStatus() != org.openhab.core.thing.ThingStatus.ONLINE) {
                logger.debug("Bridge {} is not ONLINE, skipping discovery", bridge.getUID());
                return;
            }
            
            // Get devices from the bridge handler
            java.util.List<RachioPerson.Device> devices = bridgeHandler.getDevices();
            
            if (devices != null && !devices.isEmpty()) {
                logger.info("Discovered {} devices from bridge {}", devices.size(), bridge.getUID());
                for (RachioPerson.Device device : devices) {
                    deviceDiscovered(device, bridge.getUID());
                    logger.debug("Discovered device: {} ({}) from bridge {}", device.name, device.id, bridge.getUID());
                    
                    // Also discover zones for this device
                    if (device.zones != null && !device.zones.isEmpty()) {
                        logger.debug("Discovered {} zones for device: {}", device.zones.size(), device.name);
                        for (RachioPerson.Zone zone : device.zones) {
                            zoneDiscovered(zone, device, bridge.getUID());
                        }
                    }
                }
            } else {
                logger.debug("No devices found from bridge {} (devices list: {})", 
                           bridge.getUID(), devices == null ? "null" : "empty");
                
                // If bridge is online but no devices, log warning
                if (bridge.getStatus() == org.openhab.core.thing.ThingStatus.ONLINE) {
                    logger.warn("Bridge {} is ONLINE but returned no devices. Check API key permissions.", bridge.getUID());
                }
            }
        } else {
            logger.debug("Bridge handler not ready or not found for bridge: {}", bridge.getUID());
        }
    }

    /**
     * Create discovery result for a discovered device
     */
    private void deviceDiscovered(RachioPerson.Device device, @Nullable ThingUID bridgeUID) {
        ThingUID thingUID = getDeviceThingUID(device, bridgeUID);

        if (thingUID != null) {
            Map<String, Object> properties = new HashMap<>();
            properties.put(DEVICE_ID, device.id);
            properties.put(Thing.PROPERTY_SERIAL_NUMBER, device.serialNumber);
            properties.put(Thing.PROPERTY_MODEL_ID, device.model);
            properties.put(Thing.PROPERTY_VENDOR, "Rachio");
            properties.put(Thing.PROPERTY_FIRMWARE_VERSION, "Unknown"); // Rachio API doesn't provide this
            properties.put("latitude", String.valueOf(device.latitude));
            properties.put("longitude", String.valueOf(device.longitude));
            // FIX: Removed timeZone property - it doesn't exist in our DTO

            String label = "Rachio " + device.model + " - " + device.name;

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                    .withProperties(properties)
                    .withBridge(bridgeUID)
                    .withLabel(label)
                    .withRepresentationProperty(DEVICE_ID)
                    .build();

            thingDiscovered(discoveryResult);
            logger.debug("Discovery result created for Rachio device: {}", device.name);
        }
    }

    /**
     * Create discovery result for a discovered zone
     */
    private void zoneDiscovered(RachioPerson.Zone zone, RachioPerson.Device device, @Nullable ThingUID bridgeUID) {
        ThingUID thingUID = getZoneThingUID(zone, device, bridgeUID);

        if (thingUID != null) {
            Map<String, Object> properties = new HashMap<>();
            properties.put(ZONE_ID, zone.id);
            properties.put(DEVICE_ID, device.id);
            properties.put("zoneNumber", String.valueOf(zone.zoneNumber));
            properties.put("enabled", String.valueOf(zone.enabled));
            // FIX: Removed maxRuntime property - it doesn't exist in our DTO
            // Use runtime instead if available, or provide default
            properties.put("runtime", String.valueOf(zone.runtime));

            String label = "Rachio Zone - " + zone.name + " (" + device.name + ")";

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                    .withProperties(properties)
                    .withBridge(bridgeUID)
                    .withLabel(label)
                    .withRepresentationProperty(ZONE_ID)
                    .build();

            thingDiscovered(discoveryResult);
            logger.debug("Discovery result created for Rachio zone: {} on device: {}", zone.name, device.name);
        }
    }

    /**
     * Generate ThingUID for a device
     */
    private @Nullable ThingUID getDeviceThingUID(RachioPerson.Device device, @Nullable ThingUID bridgeUID) {
        ThingTypeUID thingTypeUID = THING_TYPE_DEVICE;

        if (bridgeUID != null) {
            // Create device thing as child of bridge
            return new ThingUID(thingTypeUID, bridgeUID, sanitizeId(device.id));
        } else {
            // Should not happen in normal operation, but provide fallback
            logger.warn("Device discovered without bridge UID: {}", device.name);
            return new ThingUID(thingTypeUID, sanitizeId(device.id));
        }
    }

    /**
     * Generate ThingUID for a zone
     */
    private @Nullable ThingUID getZoneThingUID(RachioPerson.Zone zone, RachioPerson.Device device, @Nullable ThingUID bridgeUID) {
        ThingTypeUID thingTypeUID = THING_TYPE_ZONE;

        if (bridgeUID != null) {
            // Create zone thing as child of bridge
            return new ThingUID(thingTypeUID, bridgeUID, sanitizeId(zone.id));
        } else {
            // Should not happen in normal operation, but provide fallback
            logger.warn("Zone discovered without bridge UID: {} on device: {}", zone.name, device.name);
            return new ThingUID(thingTypeUID, sanitizeId(zone.id));
        }
    }

    /**
     * Sanitize ID for use in ThingUID (remove invalid characters)
     */
    private String sanitizeId(String id) {
        // ThingUID only allows alphanumeric and underscore
        return id.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Starting Rachio background discovery");
        ScheduledFuture<?> job = backgroundDiscoveryJob;
        if (job == null || job.isCancelled()) {
            // Scan every 10 minutes in background
            backgroundDiscoveryJob = scheduler.scheduleWithFixedDelay(this::startScan, 1, 10, TimeUnit.MINUTES);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        ScheduledFuture<?> job = backgroundDiscoveryJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(true);
            backgroundDiscoveryJob = null;
        }
        logger.debug("Stopped Rachio background discovery");
    }
}