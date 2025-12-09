package org.openhab.binding.rachio.internal.discovery;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The {@link RachioDiscoveryService} discovers Rachio devices and zones
 * automatically when a bridge is configured.
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDiscoveryService extends AbstractDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(RachioDiscoveryService.class);
    private final RachioBridgeHandler bridgeHandler;
    private @Nullable ScheduledFuture<?> discoveryJob;
    private static final int DISCOVERY_TIMEOUT = 30;
    private static final int BACKGROUND_DISCOVERY_INTERVAL = 300; // 5 minutes
    
    public RachioDiscoveryService(RachioBridgeHandler bridgeHandler) {
        super(Set.of(
            RachioBindingConstants.THING_TYPE_DEVICE,
            RachioBindingConstants.THING_TYPE_ZONE
        ), DISCOVERY_TIMEOUT, true);
        
        this.bridgeHandler = bridgeHandler;
        this.bridgeHandler.setDiscoveryService(this);
        logger.debug("RachioDiscoveryService created for bridge: {}", bridgeHandler.getThing().getUID());
    }

    @Override
    protected void startScan() {
        logger.debug("Starting Rachio discovery scan");
        discoverDevicesAndZones();
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Starting Rachio background discovery");
        
        if (discoveryJob != null && !discoveryJob.isCancelled()) {
            discoveryJob.cancel(true);
        }
        
        discoveryJob = scheduler.scheduleWithFixedDelay(this::discoverDevicesAndZones, 
            10, BACKGROUND_DISCOVERY_INTERVAL, TimeUnit.SECONDS);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("Stopping Rachio background discovery");
        
        if (discoveryJob != null && !discoveryJob.isCancelled()) {
            discoveryJob.cancel(true);
            discoveryJob = null;
        }
        
        super.stopBackgroundDiscovery();
    }

    @Override
    protected void stopScan() {
        logger.debug("Stopping Rachio discovery scan");
        super.stopScan();
    }

    private void discoverDevicesAndZones() {
        try {
            RachioHttp rachioHttp = bridgeHandler.getRachioHttp();
            if (rachioHttp == null) {
                logger.warn("No HTTP client available for discovery");
                return;
            }
            
            // Discover devices
            List<RachioDevice> devices = rachioHttp.getDevices();
            if (devices != null && !devices.isEmpty()) {
                for (RachioDevice device : devices) {
                    if (device.id != null && device.name != null) {
                        discoverDevice(device);
                        
                        // Discover zones for this device
                        if (device.zones != null) {
                            for (RachioZone zone : device.zones) {
                                if (zone.id != null && zone.name != null) {
                                    discoverZone(device, zone);
                                }
                            }
                        }
                    }
                }
            } else {
                logger.debug("No devices found during discovery");
            }
            
        } catch (Exception e) {
            logger.error("Error during discovery: {}", e.getMessage(), e);
        }
    }

    private void discoverDevice(RachioDevice device) {
        ThingUID bridgeUID = bridgeHandler.getThing().getUID();
        ThingUID thingUID = new ThingUID(RachioBindingConstants.THING_TYPE_DEVICE, bridgeUID, device.id);
        
        // Check if thing already exists
        if (bridgeHandler.getThing().getThing(thingUID) != null) {
            logger.debug("Device {} already exists, skipping discovery", device.id);
            return;
        }
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(RachioBindingConstants.CONFIG_DEVICE_ID, device.id);
        properties.put(RachioBindingConstants.PROPERTY_NAME, device.name);
        properties.put(RachioBindingConstants.PROPERTY_MODEL, device.model != null ? device.model : "Unknown");
        properties.put(RachioBindingConstants.PROPERTY_LOCATION, device.name);
        
        if (device.serialNumber != null) {
            properties.put("serialNumber", device.serialNumber);
        }
        
        if (device.macAddress != null) {
            properties.put("macAddress", device.macAddress);
        }
        
        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
            .withBridge(bridgeUID)
            .withProperties(properties)
            .withLabel(device.name + " (Rachio Controller)")
            .withRepresentationProperty(RachioBindingConstants.CONFIG_DEVICE_ID)
            .build();
        
        thingDiscovered(discoveryResult);
        logger.debug("Discovered Rachio device: {} ({})", device.name, device.id);
    }

    private void discoverZone(RachioDevice device, RachioZone zone) {
        ThingUID bridgeUID = bridgeHandler.getThing().getUID();
        String zoneId = zone.id;
        if (zoneId == null) {
            return;
        }
        
        // Create a unique ID for the zone
        String uniqueZoneId = device.id + "_" + zoneId;
        ThingUID thingUID = new ThingUID(RachioBindingConstants.THING_TYPE_ZONE, bridgeUID, uniqueZoneId);
        
        // Check if thing already exists
        if (bridgeHandler.getThing().getThing(thingUID) != null) {
            logger.debug("Zone {} already exists, skipping discovery", zoneId);
            return;
        }
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(RachioBindingConstants.CONFIG_ZONE_ID, zoneId);
        properties.put(RachioBindingConstants.CONFIG_ZONE_DEVICE_ID, device.id);
        properties.put(RachioBindingConstants.PROPERTY_NAME, zone.name);
        properties.put(RachioBindingConstants.PROPERTY_LOCATION, zone.name);
        
        if (zone.zoneNumber != null) {
            properties.put(RachioBindingConstants.PROPERTY_ZONE_NUMBER, zone.zoneNumber);
        }
        
        String label = zone.name;
        if (zone.zoneNumber != null) {
            label = "Zone " + zone.zoneNumber + ": " + zone.name;
        }
        
        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
            .withBridge(bridgeUID)
            .withProperties(properties)
            .withLabel(label)
            .withRepresentationProperty(RachioBindingConstants.CONFIG_ZONE_ID)
            .build();
        
        thingDiscovered(discoveryResult);
        logger.debug("Discovered Rachio zone: {} ({})", zone.name, zoneId);
    }

    public void triggerDiscovery() {
        scheduler.submit(this::discoverDevicesAndZones);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        
        if (discoveryJob != null && !discoveryJob.isCancelled()) {
            discoveryJob.cancel(true);
            discoveryJob = null;
        }
        
        logger.debug("RachioDiscoveryService deactivated");
    }
}
