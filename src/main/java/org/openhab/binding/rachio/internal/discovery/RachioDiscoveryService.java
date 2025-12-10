package org.openhab.binding.rachio.internal.discovery;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioDiscoveryService} discovers Rachio devices and zones.
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
@Component(service = DiscoveryService.class, configurationPid = "discovery.rachio")
public class RachioDiscoveryService extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(RachioDiscoveryService.class);

    private @Nullable RachioBridgeHandler bridgeHandler;

    public RachioDiscoveryService() {
        super(Set.of(RachioBindingConstants.THING_TYPE_DEVICE, RachioBindingConstants.THING_TYPE_ZONE), 30, false);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setBridgeHandler(RachioBridgeHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
        bridgeHandler.setDiscoveryService(this);
    }

    public void unsetBridgeHandler(RachioBridgeHandler bridgeHandler) {
        RachioBridgeHandler handler = this.bridgeHandler;
        if (handler != null && handler.equals(bridgeHandler)) {
            this.bridgeHandler = null;
        }
    }

    @Override
    protected void startScan() {
        logger.debug("Starting Rachio discovery scan");
        discoverDevices();
    }

    @Override
    protected void stopScan() {
        logger.debug("Stopping Rachio discovery scan");
        super.stopScan();
    }

    public void discoverDevices() {
        RachioBridgeHandler handler = bridgeHandler;
        if (handler == null) {
            logger.debug("No bridge handler available for discovery");
            return;
        }

        try {
            // In a real implementation, this would call the Rachio API via bridgeHandler
            // to get devices and zones, then create discovery results
            // For now, we'll log that discovery was triggered
            logger.debug("Discovery triggered via bridge handler");
            
            // Example structure if you had actual device data:
            // List<RachioDevice> devices = handler.getDevices();
            // if (devices != null) {
            //     for (RachioDevice device : devices) {
            //         discoverDevice(device);
            //     }
            // }
            
        } catch (Exception e) {
            logger.debug("Error during discovery", e);
        }
    }

    public void discoverDevice(@Nullable RachioDevice device) {
        if (device == null) {
            return;
        }
        
        String deviceId = device.getId();
        String deviceName = device.getName();
        
        if (deviceId == null || deviceName == null) {
            logger.debug("Device missing ID or name, skipping discovery");
            return;
        }
        
        ThingUID bridgeUID = getBridgeUID();
        if (bridgeUID == null) {
            logger.debug("No bridge UID available for device discovery");
            return;
        }
        
        ThingUID thingUID = new ThingUID(RachioBindingConstants.THING_TYPE_DEVICE, bridgeUID, deviceId);
        
        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                .withLabel(deviceName)
                .withBridge(bridgeUID)
                .withProperty(RachioBindingConstants.PROPERTY_ID, deviceId)
                .withProperty(RachioBindingConstants.PROPERTY_NAME, deviceName)
                .withProperty(RachioBindingConstants.PROPERTY_MODEL, device.getModel() != null ? device.getModel() : "")
                .withRepresentationProperty(RachioBindingConstants.PROPERTY_ID)
                .build();
        
        thingDiscovered(discoveryResult);
        logger.debug("Discovered device: {} ({})", deviceName, deviceId);
        
        // Discover zones for this device
        discoverZones(device, bridgeUID, deviceId);
    }

    private void discoverZones(RachioDevice device, ThingUID bridgeUID, String deviceId) {
        if (device.getZones() == null) {
            return;
        }
        
        for (RachioZone zone : device.getZones()) {
            String zoneId = zone.getId();
            String zoneName = zone.getName();
            
            if (zoneId == null || zoneName == null) {
                logger.debug("Zone missing ID or name, skipping discovery");
                continue;
            }
            
            ThingUID thingUID = new ThingUID(RachioBindingConstants.THING_TYPE_ZONE, bridgeUID, zoneId);
            
            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                    .withLabel(zoneName + " (" + device.getName() + ")")
                    .withBridge(bridgeUID)
                    .withProperty(RachioBindingConstants.PROPERTY_ID, zoneId)
                    .withProperty(RachioBindingConstants.PROPERTY_NAME, zoneName)
                    .withProperty(RachioBindingConstants.CONFIG_ZONE_ID, zoneId)
                    .withProperty(RachioBindingConstants.CONFIG_ZONE_DEVICE_ID, deviceId)
                    .withProperty(RachioBindingConstants.PROPERTY_ZONE_NUMBER, String.valueOf(zone.getZoneNumber()))
                    .withRepresentationProperty(RachioBindingConstants.PROPERTY_ID)
                    .build();
            
            thingDiscovered(discoveryResult);
            logger.debug("Discovered zone: {} ({}) for device {}", zoneName, zoneId, device.getName());
        }
    }

    public void deviceDiscovered(@Nullable RachioDevice device) {
        if (device == null) {
            return;
        }
        
        String deviceId = device.getId();
        if (deviceId == null) {
            return;
        }
        
        ThingUID bridgeUID = getBridgeUID();
        if (bridgeUID == null) {
            return;
        }
        
        ThingUID thingUID = new ThingUID(RachioBindingConstants.THING_TYPE_DEVICE, bridgeUID, deviceId);
        
        // Check if thing already exists
        if (getThingByUID(thingUID) != null) {
            logger.debug("Device {} already exists, skipping discovery", deviceId);
            return;
        }
        
        // Create discovery result
        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                .withLabel(device.getName() != null ? device.getName() : "Rachio Device")
                .withBridge(bridgeUID)
                .withProperty(RachioBindingConstants.PROPERTY_ID, deviceId)
                .withProperty(RachioBindingConstants.PROPERTY_NAME, device.getName() != null ? device.getName() : "")
                .withProperty(RachioBindingConstants.PROPERTY_MODEL, device.getModel() != null ? device.getModel() : "")
                .withRepresentationProperty(RachioBindingConstants.PROPERTY_ID)
                .build();
        
        thingDiscovered(discoveryResult);
        logger.debug("Discovered new device: {} ({})", device.getName(), deviceId);
    }

    public void zoneDiscovered(String deviceId, @Nullable RachioZone zone) {
        if (zone == null) {
            return;
        }
        
        String zoneId = zone.getId();
        if (zoneId == null) {
            return;
        }
        
        ThingUID bridgeUID = getBridgeUID();
        if (bridgeUID == null) {
            return;
        }
        
        ThingUID thingUID = new ThingUID(RachioBindingConstants.THING_TYPE_ZONE, bridgeUID, zoneId);
        
        // Check if thing already exists
        if (getThingByUID(thingUID) != null) {
            logger.debug("Zone {} already exists, skipping discovery", zoneId);
            return;
        }
        
        // Create discovery result
        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                .withLabel((zone.getName() != null ? zone.getName() : "Zone") + " (Zone)")
                .withBridge(bridgeUID)
                .withProperty(RachioBindingConstants.PROPERTY_ID, zoneId)
                .withProperty(RachioBindingConstants.PROPERTY_NAME, zone.getName() != null ? zone.getName() : "")
                .withProperty(RachioBindingConstants.CONFIG_ZONE_ID, zoneId)
                .withProperty(RachioBindingConstants.CONFIG_ZONE_DEVICE_ID, deviceId)
                .withProperty(RachioBindingConstants.PROPERTY_ZONE_NUMBER, 
                    zone.getZoneNumber() > 0 ? String.valueOf(zone.getZoneNumber()) : "")
                .withRepresentationProperty(RachioBindingConstants.PROPERTY_ID)
                .build();
        
        thingDiscovered(discoveryResult);
        logger.debug("Discovered new zone: {} ({}) for device {}", zone.getName(), zoneId, deviceId);
    }

    private @Nullable ThingUID getBridgeUID() {
        RachioBridgeHandler handler = bridgeHandler;
        if (handler != null) {
            return handler.getThing().getUID();
        }
        return null;
    }

    private @Nullable org.openhab.core.thing.Thing getThingByUID(ThingUID uid) {
        // This would need access to ThingRegistry
        // For compilation, returning null
        return null;
    }
}
