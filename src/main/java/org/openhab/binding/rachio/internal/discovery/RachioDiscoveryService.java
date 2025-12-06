package org.openhab.binding.rachio.internal.discovery;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovery service for Rachio devices and zones
 *
 * @author Dave Boyett - Initial contribution
 */
@Component(service = ThingHandlerService.class)
@NonNullByDefault
public class RachioDiscoveryService extends AbstractDiscoveryService implements ThingHandlerService {

    private final Logger logger = LoggerFactory.getLogger(RachioDiscoveryService.class);

    private static final int DISCOVERY_TIMEOUT_SECONDS = 30;
    private static final int BACKGROUND_DISCOVERY_INTERVAL_SECONDS = 300; // 5 minutes

    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable ScheduledFuture<?> backgroundDiscoveryJob;
    private final Set<String> discoveredDeviceIds = new HashSet<>();
    private final Set<String> discoveredZoneIds = new HashSet<>();

    public RachioDiscoveryService() {
        super(Set.of(RachioBindingConstants.THING_TYPE_DEVICE, RachioBindingConstants.THING_TYPE_ZONE),
                DISCOVERY_TIMEOUT_SECONDS, true);
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
        stopBackgroundDiscovery();
        super.deactivate();
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
        
        stopBackgroundDiscovery(); // Ensure no existing job
        
        backgroundDiscoveryJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                discoverDevices();
                discoverZones();
            } catch (Exception e) {
                logger.error("Error during background discovery: {}", e.getMessage(), e);
            }
        }, 10, BACKGROUND_DISCOVERY_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        ScheduledFuture<?> localJob = backgroundDiscoveryJob;
        if (localJob != null) {
            localJob.cancel(true);
            backgroundDiscoveryJob = null;
        }
        logger.debug("Stopped Rachio background discovery");
    }

    /**
     * Discover Rachio devices
     */
    private void discoverDevices() {
        RachioBridgeHandler localHandler = bridgeHandler;
        if (localHandler == null) {
            logger.warn("Bridge handler not available for discovery");
            return;
        }

        try {
            // Get all devices from the bridge
            var devices = localHandler.getDevices();
            if (devices == null || devices.isEmpty()) {
                logger.debug("No devices found during discovery");
                return;
            }

            logger.debug("Found {} devices during discovery", devices.size());

            for (RachioDevice device : devices) {
                if (device.id == null || device.id.isEmpty()) {
                    logger.debug("Skipping device with null/empty ID");
                    continue;
                }

                // Skip if already discovered
                if (discoveredDeviceIds.contains(device.id)) {
                    logger.debug("Device {} already discovered, skipping", device.id);
                    continue;
                }

                ThingUID bridgeUID = localHandler.getThing().getUID();
                ThingUID thingUID = new ThingUID(RachioBindingConstants.THING_TYPE_DEVICE, bridgeUID, device.id);

                // Check if thing already exists
                if (isThingExisting(thingUID)) {
                    logger.debug("Thing already exists for device {}", device.id);
                    discoveredDeviceIds.add(device.id);
                    continue;
                }

                // Create discovery result
                Map<String, Object> properties = new HashMap<>();
                properties.put(RachioBindingConstants.PARAM_DEVICE_ID, device.id);
                properties.put(RachioBindingConstants.PROPERTY_DEVICE_ID, device.id);
                
                if (device.name != null) {
                    properties.put(RachioBindingConstants.PROPERTY_DEVICE_NAME, device.name);
                }
                
                if (device.model != null) {
                    properties.put(RachioBindingConstants.PROPERTY_DEVICE_MODEL, device.model);
                }
                
                if (device.serialNumber != null) {
                    properties.put(RachioBindingConstants.PROPERTY_DEVICE_SERIAL, device.serialNumber);
                }

                String label = device.name != null ? device.name : "Rachio Device " + device.id;
                
                DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                        .withBridge(bridgeUID)
                        .withProperties(properties)
                        .withLabel(label)
                        .withRepresentationProperty(RachioBindingConstants.PROPERTY_DEVICE_ID)
                        .build();

                thingDiscovered(discoveryResult);
                discoveredDeviceIds.add(device.id);
                
                logger.info("Discovered Rachio device: {} (ID: {})", label, device.id);
                
                // Discover zones for this device
                discoverZonesForDevice(device, bridgeUID);
            }

        } catch (RachioApiException e) {
            logger.error("Error discovering devices: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during device discovery: {}", e.getMessage(), e);
        }
    }

    /**
     * Discover zones for a specific device
     */
    private void discoverZonesForDevice(RachioDevice device, ThingUID bridgeUID) {
        if (device.id == null || device.zones == null || device.zones.isEmpty()) {
            logger.debug("Device {} has no zones to discover", device.id);
            return;
        }

        logger.debug("Found {} zones for device {}", device.zones.size(), device.id);

        for (RachioZone zone : device.zones) {
            if (zone.id == null || zone.id.isEmpty()) {
                logger.debug("Skipping zone with null/empty ID");
                continue;
            }

            // Skip if already discovered
            if (discoveredZoneIds.contains(zone.id)) {
                logger.debug("Zone {} already discovered, skipping", zone.id);
                continue;
            }

            // Create zone thing UID (use device ID as part of zone UID to ensure uniqueness)
            String zoneThingId = device.id + "_" + zone.id;
            ThingUID zoneUID = new ThingUID(RachioBindingConstants.THING_TYPE_ZONE, bridgeUID, zoneThingId);

            // Check if thing already exists
            if (isThingExisting(zoneUID)) {
                logger.debug("Thing already exists for zone {}", zone.id);
                discoveredZoneIds.add(zone.id);
                continue;
            }

            // Create discovery result for zone
            Map<String, Object> properties = new HashMap<>();
            properties.put(RachioBindingConstants.PARAM_ZONE_ID, zone.id);
            properties.put(RachioBindingConstants.PARAM_DEVICE_ID, device.id);
            properties.put(RachioBindingConstants.PROPERTY_ZONE_ID, zone.id);
            properties.put(RachioBindingConstants.PROPERTY_DEVICE_ID, device.id);
            
            if (zone.name != null) {
                properties.put(RachioBindingConstants.PROPERTY_ZONE_NAME, zone.name);
            }
            
            if (zone.zoneNumber != null) {
                properties.put("zoneNumber", zone.zoneNumber);
            }

            String label = zone.name != null ? zone.name : "Zone " + (zone.zoneNumber != null ? zone.zoneNumber : "?");
            if (device.name != null) {
                label += " (" + device.name + ")";
            }

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(zoneUID)
                    .withBridge(bridgeUID)
                    .withProperties(properties)
                    .withLabel(label)
                    .withRepresentationProperty(RachioBindingConstants.PROPERTY_ZONE_ID)
                    .build();

            thingDiscovered(discoveryResult);
            discoveredZoneIds.add(zone.id);
            
            logger.info("Discovered Rachio zone: {} (Device: {}, Zone ID: {})", 
                    zone.name != null ? zone.name : "Unknown", 
                    device.name != null ? device.name : device.id, 
                    zone.id);
        }
    }

    /**
     * Discover all zones (for manual discovery)
     */
    private void discoverZones() {
        RachioBridgeHandler localHandler = bridgeHandler;
        if (localHandler == null) {
            return;
        }

        try {
            var devices = localHandler.getDevices();
            if (devices == null || devices.isEmpty()) {
                return;
            }

            ThingUID bridgeUID = localHandler.getThing().getUID();
            
            for (RachioDevice device : devices) {
                if (device.id != null) {
                    discoverZonesForDevice(device, bridgeUID);
                }
            }

        } catch (RachioApiException e) {
            logger.error("Error discovering zones: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if a thing already exists
     */
    private boolean isThingExisting(ThingUID thingUID) {
        if (bridgeHandler == null) {
            return false;
        }
        
        // Get the bridge's thing registry (via handler)
        var bridge = bridgeHandler.getThing();
        if (bridge == null) {
            return false;
        }
        
        // In OpenHAB, we would typically use ThingRegistry, but in discovery service
        // we can check via the bridge handler context
        // For now, we'll rely on the discovery service's own tracking
        // A more robust implementation would check the ThingRegistry
        
        return false; // Let discovery service manage duplicates
    }

    /**
     * Remove thing from discovered cache
     */
    public void thingRemoved(ThingUID thingUID) {
        String thingId = thingUID.getId();
        
        // Check if it's a device
        if (thingId != null && discoveredDeviceIds.contains(thingId)) {
            discoveredDeviceIds.remove(thingId);
            logger.debug("Removed device {} from discovery cache", thingId);
        }
        
        // Check if it's a zone (zones have deviceId_zoneId format)
        for (String zoneId : discoveredZoneIds) {
            if (thingId != null && thingId.contains(zoneId)) {
                discoveredZoneIds.remove(zoneId);
                logger.debug("Removed zone {} from discovery cache", zoneId);
                break;
            }
        }
    }

    /**
     * Clear discovery cache
     */
    public void clearDiscoveryCache() {
        discoveredDeviceIds.clear();
        discoveredZoneIds.clear();
        logger.debug("Cleared discovery cache");
    }

    /**
     * Get discovered device count
     */
    public int getDiscoveredDeviceCount() {
        return discoveredDeviceIds.size();
    }

    /**
     * Get discovered zone count
     */
    public int getDiscoveredZoneCount() {
        return discoveredZoneIds.size();
    }

    /**
     * Manual discovery trigger (can be called from rules or UI)
     */
    public void triggerDiscovery() {
        logger.info("Manual discovery triggered");
        startScan();
    }
}
