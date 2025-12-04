package org.openhab.binding.rachio.internal.discovery;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioDiscoveryService} discovers Rachio devices and zones
 *
 * @author Brian G. - Initial contribution (from 2.5 binding)
 * @author Daniel B. - Major rewrite for OpenHAB 5.x
 */
@Component(service = DiscoveryService.class, configurationPid = "discovery.rachio")
@NonNullByDefault
public class RachioDiscoveryService extends AbstractDiscoveryService implements ThingHandlerService {
    private final Logger logger = LoggerFactory.getLogger(RachioDiscoveryService.class);

    // API Client
    private final RachioHttp rachioHttp;

    // Bridge Handler
    private @Nullable RachioBridgeHandler bridgeHandler;

    // Background discovery
    private @Nullable ScheduledFuture<?> backgroundDiscoveryJob;
    private static final int BACKGROUND_DISCOVERY_INTERVAL = 3600; // 1 hour
    private static final int INITIAL_DELAY = 10; // seconds

    // Track discovered things to avoid duplicates
    private final Set<String> discoveredDevices = new HashSet<>();
    private final Set<String> discoveredZones = new HashSet<>();

    @Activate
    public RachioDiscoveryService(@Reference RachioHttp rachioHttp) {
        super(SUPPORTED_THING_TYPES, 30, true);
        this.rachioHttp = rachioHttp;
        logger.debug("Rachio discovery service initialized");
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof RachioBridgeHandler) {
            this.bridgeHandler = (RachioBridgeHandler) handler;
            logger.debug("Discovery service attached to bridge: {}", bridgeHandler.getThing().getUID());
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler;
    }

    @Override
    public void deactivate() {
        stopBackgroundDiscovery();
        super.deactivate();
    }

    @Override
    protected void startScan() {
        logger.debug("Starting manual Rachio discovery scan");
        discoverDevicesAndZones();
    }

    @Override
    protected synchronized void stopScan() {
        logger.debug("Stopping Rachio discovery scan");
        super.stopScan();
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Starting Rachio background discovery");
        ScheduledFuture<?> job = backgroundDiscoveryJob;
        if (job == null || job.isCancelled()) {
            backgroundDiscoveryJob = scheduler.scheduleWithFixedDelay(this::backgroundDiscoveryScan,
                    INITIAL_DELAY, BACKGROUND_DISCOVERY_INTERVAL, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("Stopping Rachio background discovery");
        ScheduledFuture<?> job = backgroundDiscoveryJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(true);
            backgroundDiscoveryJob = null;
        }
    }

    /**
     * Background discovery scan
     */
    private void backgroundDiscoveryScan() {
        logger.debug("Running background discovery scan");
        try {
            discoverDevicesAndZones();
        } catch (Exception e) {
            logger.debug("Error in background discovery: {}", e.getMessage(), e);
        }
    }

    /**
     * Discover devices and zones from Rachio API
     */
    private void discoverDevicesAndZones() {
        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler == null) {
            logger.warn("Bridge handler not available for discovery");
            return;
        }

        String apiKey = localBridgeHandler.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("API key not available for discovery");
            return;
        }

        try {
            // Get devices from Rachio API
            var devices = rachioHttp.getDevices(apiKey);
            logger.debug("Found {} Rachio devices", devices.size());

            // Discover each device
            for (RachioDevice device : devices) {
                discoverDevice(device, localBridgeHandler.getThing().getUID());
                
                // Discover zones for this device
                discoverZones(device, localBridgeHandler.getThing().getUID());
            }

            // Remove things that are no longer present
            removeStaleThings();

        } catch (RachioApiException e) {
            logger.warn("Failed to discover Rachio devices: {}", e.getMessage());
        }
    }

    /**
     * Discover a single device
     */
    private void discoverDevice(RachioDevice device, ThingUID bridgeUID) {
        String deviceId = device.getId();
        String deviceName = device.getName();
        
        // Check if already discovered
        String deviceKey = "device:" + deviceId;
        if (discoveredDevices.contains(deviceKey)) {
            logger.trace("Device {} already discovered", deviceId);
            return;
        }

        logger.debug("Discovering device: {} ({})", deviceName, deviceId);

        // Create discovery result
        ThingUID thingUID = new ThingUID(THING_TYPE_DEVICE, bridgeUID, deviceId);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(PROPERTY_DEVICE_ID, deviceId);
        properties.put(PROPERTY_DEVICE_NAME, deviceName);
        properties.put(PROPERTY_SERIAL_NUMBER, device.getSerialNumber());
        properties.put(PROPERTY_MODEL, device.getModel());
        properties.put(PROPERTY_MAC_ADDRESS, device.getMacAddress());
        properties.put(PROPERTY_FIRMWARE_VERSION, device.getFirmwareVersion());
        
        if (device.getCreatedDate() != null) {
            properties.put(PROPERTY_CREATED_DATE, device.getCreatedDate().toString());
        }
        
        if (device.getUpdatedDate() != null) {
            properties.put(PROPERTY_UPDATED_DATE, device.getUpdatedDate().toString());
        }

        // Add professional device properties
        properties.put("zoneCount", String.valueOf(device.getZones().size()));
        properties.put("status", device.getStatus());
        properties.put("online", String.valueOf(device.isOnline()));
        properties.put("paused", String.valueOf(device.isPaused()));

        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                .withBridge(bridgeUID)
                .withProperties(properties)
                .withRepresentationProperty(PROPERTY_DEVICE_ID)
                .withLabel(deviceName)
                .build();

        thingDiscovered(discoveryResult);
        discoveredDevices.add(deviceKey);
        
        logger.info("Discovered Rachio device: {} ({})", deviceName, deviceId);
    }

    /**
     * Discover zones for a device
     */
    private void discoverZones(RachioDevice device, ThingUID bridgeUID) {
        String deviceId = device.getId();
        String apiKey = bridgeHandler != null ? bridgeHandler.getApiKey() : null;

        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("API key not available for zone discovery");
            return;
        }

        try {
            // Get zones for this device
            var zones = rachioHttp.getZones(apiKey, deviceId);
            logger.debug("Found {} zones for device {}", zones.size(), deviceId);

            // Discover each zone
            for (RachioZone zone : zones) {
                discoverZone(zone, device, bridgeUID);
            }

        } catch (RachioApiException e) {
            logger.warn("Failed to discover zones for device {}: {}", deviceId, e.getMessage());
        }
    }

    /**
     * Discover a single zone with professional irrigation data
     */
    private void discoverZone(RachioZone zone, RachioDevice device, ThingUID bridgeUID) {
        String zoneId = zone.getId();
        String zoneName = zone.getName();
        String deviceId = device.getId();
        
        // Create unique key for this zone
        String zoneKey = "zone:" + deviceId + ":" + zoneId;
        if (discoveredZones.contains(zoneKey)) {
            logger.trace("Zone {} already discovered", zoneId);
            return;
        }

        logger.debug("Discovering zone: {} ({}) on device {}", zoneName, zoneId, deviceId);

        // Create zone thing UID (use deviceId in UID to ensure uniqueness)
        String safeZoneId = zoneId.replaceAll("[^a-zA-Z0-9_]", "_");
        String safeDeviceId = deviceId.replaceAll("[^a-zA-Z0-9_]", "_");
        ThingUID thingUID = new ThingUID(THING_TYPE_ZONE, bridgeUID, safeDeviceId + "_" + safeZoneId);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(PROPERTY_ZONE_ID, zoneId);
        properties.put(PROPERTY_ZONE_NAME, zoneName);
        properties.put(PROPERTY_DEVICE_ID, deviceId);
        properties.put(PROPERTY_DEVICE_NAME, device.getName());
        
        // Determine zone number (extract from name or use position)
        int zoneNumber = extractZoneNumber(zoneName, zoneId);
        properties.put(PROPERTY_ZONE_NUMBER, String.valueOf(zoneNumber));

        // Add professional irrigation properties
        addProfessionalZoneProperties(zone, properties);

        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                .withBridge(bridgeUID)
                .withProperties(properties)
                .withRepresentationProperty(PROPERTY_ZONE_ID)
                .withLabel(zoneName + " (Zone " + zoneNumber + ")")
                .build();

        thingDiscovered(discoveryResult);
        discoveredZones.add(zoneKey);
        
        logger.info("Discovered Rachio zone: {} (Zone {}) on device {}", zoneName, zoneNumber, device.getName());
    }

    /**
     * Extract zone number from zone name or ID
     */
    private int extractZoneNumber(String zoneName, String zoneId) {
        try {
            // Try to extract number from name (e.g., "Zone 1", "Front Yard 2")
            String[] parts = zoneName.split(" ");
            for (int i = parts.length - 1; i >= 0; i--) {
                try {
                    return Integer.parseInt(parts[i]);
                } catch (NumberFormatException e) {
                    // Not a number, continue
                }
            }
            
            // Try to extract from zone ID
            String numericPart = zoneId.replaceAll("[^0-9]", "");
            if (!numericPart.isEmpty()) {
                return Integer.parseInt(numericPart);
            }
        } catch (Exception e) {
            logger.debug("Could not extract zone number from {} or {}: {}", zoneName, zoneId, e.getMessage());
        }
        
        // Fallback: Use hash of zone ID
        return Math.abs(zoneId.hashCode() % 16) + 1;
    }

    /**
     * Add professional irrigation properties to discovery result
     */
    private void addProfessionalZoneProperties(RachioZone zone, Map<String, Object> properties) {
        // Soil properties
        if (zone.getCustomSoil() != null) {
            properties.put(PROPERTY_SOIL_TYPE, zone.getCustomSoil().getName());
        }
        
        // Crop properties
        if (zone.getCustomCrop() != null) {
            properties.put(PROPERTY_CROP_TYPE, zone.getCustomCrop().getName());
            if (zone.getCustomCrop().getCoefficient() != null) {
                properties.put(PROPERTY_CROP_COEFFICIENT, 
                        String.valueOf(zone.getCustomCrop().getCoefficient()));
            }
        }
        
        // Nozzle properties
        if (zone.getCustomNozzle() != null) {
            properties.put(PROPERTY_NOZZLE_TYPE, zone.getCustomNozzle().getName());
            if (zone.getCustomNozzle().getInchesPerHour() != null) {
                properties.put(PROPERTY_NOZZLE_RATE, 
                        String.valueOf(zone.getCustomNozzle().getInchesPerHour()));
            }
        }
        
        // Slope and shade
        if (zone.getCustomSlope() != null) {
            properties.put(PROPERTY_SLOPE_TYPE, zone.getCustomSlope().getName());
        }
        if (zone.getCustomShade() != null) {
            properties.put(PROPERTY_SHADE_TYPE, zone.getCustomShade().getName());
        }
        
        // Numeric properties
        if (zone.getRootZoneDepth() != null) {
            properties.put(PROPERTY_ROOT_DEPTH, String.valueOf(zone.getRootZoneDepth()));
        }
        if (zone.getEfficiency() != null) {
            properties.put(PROPERTY_EFFICIENCY, String.valueOf(zone.getEfficiency()));
        }
        if (zone.getYardAreaSquareFeet() != null) {
            properties.put(PROPERTY_AREA, String.valueOf(zone.getYardAreaSquareFeet()));
        }
        
        // Water adjustment runtimes
        if (zone.getWateringAdjustmentRuntimes() != null) {
            StringBuilder adjustments = new StringBuilder();
            for (int i = 0; i < zone.getWateringAdjustmentRuntimes().length; i++) {
                if (i > 0) adjustments.append(",");
                adjustments.append(zone.getWateringAdjustmentRuntimes()[i]);
            }
            properties.put(PROPERTY_WATER_ADJUSTMENTS, adjustments.toString());
        }
        
        // Zone status and configuration
        properties.put("enabled", String.valueOf(zone.isEnabled()));
        properties.put("duration", String.valueOf(zone.getDuration()));
        properties.put("status", zone.getStatus() != null ? zone.getStatus().name() : "UNKNOWN");
        
        if (zone.getLastWateredDate() != null) {
            properties.put("lastWatered", zone.getLastWateredDate().toString());
        }
        
        if (zone.getWaterBudget() != null) {
            properties.put("waterBudget", String.valueOf(zone.getWaterBudget()));
        }
        
        if (zone.getAvailableWater() != null) {
            properties.put("availableWater", String.valueOf(zone.getAvailableWater()));
        }
    }

    /**
     * Remove things that are no longer present
     */
    private void removeStaleThings() {
        // Get current timestamp for staleness check
        Instant now = Instant.now();
        Instant staleThreshold = now.minusSeconds(BACKGROUND_DISCOVERY_INTERVAL * 2);
        
        // In a real implementation, you would:
        // 1. Track when things were last seen
        // 2. Remove things not seen in a while
        // 3. Call thingRemoved() for stale things
        
        // For now, we'll just log that this would happen
        logger.debug("Stale thing removal would run (not implemented in this version)");
    }

    /**
     * Manually trigger discovery from bridge handler
     */
    public void triggerDiscovery() {
        logger.debug("Manual discovery triggered");
        scheduler.submit(this::startScan);
    }

    /**
     * Clear discovery cache (for testing or re-discovery)
     */
    public void clearDiscoveryCache() {
        discoveredDevices.clear();
        discoveredZones.clear();
        logger.debug("Discovery cache cleared");
    }

    /**
     * Get number of discovered devices
     */
    public int getDiscoveredDeviceCount() {
        return discoveredDevices.size();
    }

    /**
     * Get number of discovered zones
     */
    public int getDiscoveredZoneCount() {
        return discoveredZones.size();
    }

    @Override
    public void activate() {
        super.activate(null);
        logger.debug("Rachio discovery service activated");
    }

    @Override
    public void deactivate() {
        super.deactivate();
        logger.debug("Rachio discovery service deactivated");
    }
}
