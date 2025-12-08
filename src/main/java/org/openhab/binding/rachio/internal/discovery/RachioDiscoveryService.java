package org.openhab.binding.rachio.internal.discovery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovery service for Rachio devices and zones
 *
 * @author Damion Boyett - Enhanced with professional features
 */
@NonNullByDefault
public class RachioDiscoveryService extends AbstractDiscoveryService implements ThingHandlerService {
    private final Logger logger = LoggerFactory.getLogger(RachioDiscoveryService.class);
    
    private static final int DISCOVERY_TIMEOUT_SECONDS = 30;
    private static final int BACKGROUND_DISCOVERY_INTERVAL_MINUTES = 10;
    
    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable ScheduledFuture<?> backgroundDiscoveryJob;
    
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
        cancelBackgroundDiscovery();
        super.deactivate();
    }
    
    @Override
    protected void startScan() {
        logger.debug("Starting Rachio discovery scan");
        discoverDevicesAndZones();
    }
    
    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Starting Rachio background discovery");
        if (backgroundDiscoveryJob == null || backgroundDiscoveryJob.isCancelled()) {
            backgroundDiscoveryJob = scheduler.scheduleWithFixedDelay(this::discoverDevicesAndZones, 0,
                    BACKGROUND_DISCOVERY_INTERVAL_MINUTES, TimeUnit.MINUTES);
        }
    }
    
    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("Stopping Rachio background discovery");
        cancelBackgroundDiscovery();
    }
    
    private void cancelBackgroundDiscovery() {
        if (backgroundDiscoveryJob != null && !backgroundDiscoveryJob.isCancelled()) {
            backgroundDiscoveryJob.cancel(true);
            backgroundDiscoveryJob = null;
        }
    }
    
    private void discoverDevicesAndZones() {
        RachioBridgeHandler handler = bridgeHandler;
        if (handler == null) {
            logger.debug("Bridge handler not available for discovery");
            return;
        }
        
        RachioHttp http = handler.getHttp();
        if (http == null) {
            logger.debug("HTTP client not available for discovery");
            return;
        }
        
        try {
            // Get person info to discover all devices
            RachioPerson person = http.getPersonInfo(); // Fixed: Use getter method
            if (person != null && person.getDevices() != null) {
                List<RachioDevice> devices = person.getDevices();
                for (RachioDevice device : devices) {
                    discoverDevice(device, handler.getThing().getUID());
                    
                    // Discover zones for this device
                    List<RachioZone> zones = device.getZones(); // Fixed: Use getter method
                    if (zones != null) {
                        for (RachioZone zone : zones) {
                            discoverZone(zone, device, handler.getThing().getUID());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error during discovery: {}", e.getMessage(), e);
        }
    }
    
    private void discoverDevice(RachioDevice device, ThingUID bridgeUID) {
        String deviceId = device.getId(); // Fixed: Use getter method
        if (deviceId == null || deviceId.isEmpty()) {
            logger.debug("Device ID is null or empty, skipping discovery");
            return;
        }
        
        ThingUID thingUID = new ThingUID(RachioBindingConstants.THING_TYPE_DEVICE, bridgeUID, deviceId);
        
        // Check if device is already discovered
        if (getDiscoveryResults().stream().anyMatch(result -> result.getThingUID().equals(thingUID))) {
            logger.debug("Device {} already discovered", deviceId);
            return;
        }
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(RachioBindingConstants.PARAM_DEVICE_ID, deviceId); // Fixed: Use constant
        
        String deviceName = device.getName(); // Fixed: Use getter method
        if (deviceName != null && !deviceName.isEmpty()) {
            properties.put(RachioBindingConstants.PROPERTY_DEVICE_NAME, deviceName);
        }
        
        String model = device.getModel(); // Fixed: Use getter method
        if (model != null && !model.isEmpty()) {
            properties.put(RachioBindingConstants.PROPERTY_DEVICE_MODEL, model);
        }
        
        String serialNumber = device.getSerialNumber(); // Fixed: Use getter method
        if (serialNumber != null && !serialNumber.isEmpty()) {
            properties.put(RachioBindingConstants.PROPERTY_DEVICE_SERIAL, serialNumber);
        }
        
        String macAddress = device.getMacAddress();
        if (macAddress != null && !macAddress.isEmpty()) {
            properties.put(RachioBindingConstants.PROPERTY_DEVICE_MAC, macAddress);
        }
        
        List<RachioZone> zones = device.getZones(); // Fixed: Use getter method
        if (zones != null) {
            properties.put(RachioBindingConstants.PROPERTY_ZONE_COUNT, zones.size());
        }
        
        // Professional properties
        properties.put(RachioBindingConstants.PROPERTY_LATITUDE, device.getLatitude());
        properties.put(RachioBindingConstants.PROPERTY_LONGITUDE, device.getLongitude());
        properties.put(RachioBindingConstants.PROPERTY_ELEVATION, device.getElevation());
        properties.put(RachioBindingConstants.PROPERTY_TIMEZONE, device.getTimeZone());
        properties.put(RachioBindingConstants.PROPERTY_FLEX_SCHEDULE, device.isFlexScheduleRules());
        
        String label = String.format("Rachio %s: %s", 
                deviceName != null ? deviceName : "Controller",
                model != null ? model : "Smart Sprinkler");
        
        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                .withBridge(bridgeUID)
                .withProperties(properties)
                .withLabel(label)
                .withRepresentationProperty(RachioBindingConstants.PARAM_DEVICE_ID)
                .build();
        
        thingDiscovered(discoveryResult);
        logger.debug("Discovered Rachio device: {} ({})", deviceName, deviceId);
    }
    
    private void discoverZone(RachioZone zone, RachioDevice device, ThingUID bridgeUID) {
        String zoneId = zone.getId(); // Fixed: Use getter method
        String deviceId = device.getId(); // Fixed: Use getter method
        
        if (zoneId == null || zoneId.isEmpty() || deviceId == null || deviceId.isEmpty()) {
            logger.debug("Zone ID or Device ID is null or empty, skipping discovery");
            return;
        }
        
        // Create unique zone UID: deviceId_zoneId
        String uniqueZoneId = String.format("%s_%s", deviceId, zoneId);
        ThingUID thingUID = new ThingUID(RachioBindingConstants.THING_TYPE_ZONE, bridgeUID, uniqueZoneId);
        
        // Check if zone is already discovered
        if (getDiscoveryResults().stream().anyMatch(result -> result.getThingUID().equals(thingUID))) {
            logger.debug("Zone {} already discovered", zoneId);
            return;
        }
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(RachioBindingConstants.PARAM_ZONE_ID, zoneId);
        properties.put(RachioBindingConstants.PARAM_DEVICE_ID, deviceId);
        
        String zoneName = zone.getName();
        if (zoneName != null && !zoneName.isEmpty()) {
            properties.put(RachioBindingConstants.PROPERTY_ZONE_NAME, zoneName);
        }
        
        properties.put(RachioBindingConstants.PROPERTY_ZONE_NUMBER, zone.getZoneNumber());
        properties.put(RachioBindingConstants.PROPERTY_ZONE_ENABLED, zone.isEnabled());
        properties.put(RachioBindingConstants.PROPERTY_ZONE_RUNTIME, zone.getRuntime());
        properties.put(RachioBindingConstants.PROPERTY_ZONE_MAX_RUNTIME, zone.getMaxRuntime());
        properties.put(RachioBindingConstants.PROPERTY_ZONE_AREA, zone.getArea());
        
        // Professional irrigation properties
        properties.put(RachioBindingConstants.PROPERTY_SOIL_TYPE, zone.getSoilType());
        properties.put(RachioBindingConstants.PROPERTY_CROP_TYPE, zone.getCropType());
        properties.put(RachioBindingConstants.PROPERTY_NOZZLE_TYPE, zone.getNozzleType());
        properties.put(RachioBindingConstants.PROPERTY_SLOPE_TYPE, zone.getSlopeType());
        properties.put(RachioBindingConstants.PROPERTY_SHADE_TYPE, zone.getShadeType());
        properties.put(RachioBindingConstants.PROPERTY_ROOT_DEPTH, zone.getRootZoneDepth());
        properties.put(RachioBindingConstants.PROPERTY_IRRIGATION_EFFICIENCY, zone.getEfficiency());
        properties.put(RachioBindingConstants.PROPERTY_AVAILABLE_WATER, zone.getAvailableWater());
        
        // Water adjustment levels
        int[] adjustmentRuntimes = zone.getWateringAdjustmentRuntimes();
        for (int i = 0; i < Math.min(adjustmentRuntimes.length, 5); i++) {
            properties.put(RachioBindingConstants.PROPERTY_ADJUSTMENT_LEVEL_PREFIX + (i + 1), 
                          adjustmentRuntimes[i]);
        }
        
        String deviceName = device.getName(); // Fixed: Use getter method
        String label = String.format("Zone %d: %s (%s)", 
                zone.getZoneNumber(), 
                zoneName != null ? zoneName : "Unnamed Zone",
                deviceName != null ? deviceName : "Controller");
        
        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                .withBridge(bridgeUID)
                .withProperties(properties)
                .withLabel(label)
                .withRepresentationProperty(RachioBindingConstants.PARAM_ZONE_ID)
                .build();
        
        thingDiscovered(discoveryResult);
        logger.debug("Discovered Rachio zone: {} (Device: {}, Zone: {})", zoneName, deviceId, zoneId);
    }
    
    /**
     * Force rediscovery of all devices and zones
     */
    public void rediscoverAll() {
        logger.debug("Forcing rediscovery of all Rachio devices and zones");
        removeOlderResults(getTimestampOfLastScan());
        discoverDevicesAndZones();
    }
    
    /**
     * Discover a specific device by ID
     * 
     * @param deviceId the device ID to discover
     */
    public void discoverDeviceById(String deviceId) {
        RachioBridgeHandler handler = bridgeHandler;
        if (handler == null || handler.getHttp() == null) {
            return;
        }
        
        try {
            // Get person info to find the specific device
            RachioPerson person = handler.getHttp().getPersonInfo(); // Fixed: Use getter method
            if (person != null && person.getDevices() != null) {
                for (RachioDevice device : person.getDevices()) {
                    if (deviceId.equals(device.getId())) { // Fixed: Use getter method
                        discoverDevice(device, handler.getThing().getUID());
                        
                        // Discover zones for this device
                        List<RachioZone> zones = device.getZones(); // Fixed: Use getter method
                        if (zones != null) {
                            for (RachioZone zone : zones) {
                                discoverZone(zone, device, handler.getThing().getUID());
                            }
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error discovering device {}: {}", deviceId, e.getMessage(), e);
        }
    }
    
    /**
     * Remove discovery results for a specific device and its zones
     * 
     * @param deviceId the device ID to remove
     */
    public void removeDeviceDiscovery(String deviceId) {
        ThingUID bridgeUID = bridgeHandler != null ? bridgeHandler.getThing().getUID() : null;
        if (bridgeUID == null) {
            return;
        }
        
        // Remove device discovery
        ThingUID deviceThingUID = new ThingUID(RachioBindingConstants.THING_TYPE_DEVICE, bridgeUID, deviceId);
        thingRemoved(deviceThingUID);
        
        // Remove all zones for this device
        getDiscoveryResults().stream()
            .filter(result -> result.getThingUID().getBridgeUID() != null &&
                             result.getThingUID().getBridgeUID().equals(bridgeUID))
            .filter(result -> result.getProperties().containsKey(RachioBindingConstants.PARAM_DEVICE_ID))
            .filter(result -> deviceId.equals(result.getProperties().get(RachioBindingConstants.PARAM_DEVICE_ID)))
            .forEach(result -> thingRemoved(result.getThingUID()));
        
        logger.debug("Removed discovery results for device: {}", deviceId);
    }
    
    @Override
    protected synchronized void stopScan() {
        logger.debug("Stopping Rachio discovery scan");
        super.stopScan();
    }
}
