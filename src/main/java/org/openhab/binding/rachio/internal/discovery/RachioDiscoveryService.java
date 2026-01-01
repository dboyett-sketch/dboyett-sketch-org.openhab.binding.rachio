package org.openhab.binding.rachio.internal.discovery;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiClient;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.binding.rachio.internal.handler.RachioStatusListener;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioDiscoveryService} discovers Rachio devices and zones.
 *
 * @author Damion Boyett - Refactor contribution
 */
@NonNullByDefault
public class RachioDiscoveryService extends AbstractDiscoveryService
        implements DiscoveryService, ThingHandlerService, RachioStatusListener {

    private final Logger logger = LoggerFactory.getLogger(RachioDiscoveryService.class);

    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable ScheduledFuture<?> discoveryJob;

    /**
     * Constructor for RachioDiscoveryService
     */
    public RachioDiscoveryService() {
        super(Set.of(THING_TYPE_DEVICE, THING_TYPE_ZONE), 30, true);
        logger.debug("RachioDiscoveryService created");
    }

    /**
     * Set the bridge handler for this discovery service
     * 
     * @param handler The bridge handler
     */
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof RachioBridgeHandler) {
            this.bridgeHandler = (RachioBridgeHandler) handler;
            this.bridgeHandler.registerStatusListener(this);
            logger.debug("Bridge handler set for discovery service");
        } else if (handler != null) {
            logger.debug("Handler is not a RachioBridgeHandler: {}", handler.getClass().getName());
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler;
    }

    @Override
    public void activate() {
        super.activate(null);
        logger.debug("RachioDiscoveryService activated");
    }

    @Override
    public void deactivate() {
        super.deactivate();

        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler != null) {
            localBridgeHandler.unregisterStatusListener(this);
            logger.debug("Unregistered from bridge handler");
        }

        stopBackgroundDiscovery();
        logger.debug("RachioDiscoveryService deactivated");
    }

    @Override
    protected void startScan() {
        logger.debug("Starting Rachio discovery scan");
        discoverDevices();
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Starting Rachio background discovery");
        if (discoveryJob == null || discoveryJob.isCancelled()) {
            discoveryJob = scheduler.scheduleWithFixedDelay(this::discoverDevices, 0, 5, TimeUnit.MINUTES);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("Stopping Rachio background discovery");
        if (discoveryJob != null && !discoveryJob.isCancelled()) {
            discoveryJob.cancel(true);
            discoveryJob = null;
        }
    }

    /**
     * Stops the discovery service
     */
    public void stopDiscovery() {
        stopBackgroundDiscovery();
    }

    private void discoverDevices() {

        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler == null) {
            logger.debug("Bridge handler not available for discovery");
            return;
        }

        RachioApiClient apiClient = localBridgeHandler.getApiClient();
        if (apiClient == null) {
            logger.debug("API client not available for discovery");
            return;
        }

        try {
            // Get person info directly from API
            var personInfo = apiClient.getPersonInfo();
            if (personInfo == null || personInfo.id == null || personInfo.id.isEmpty()) {
                logger.debug("Person info not available for discovery");
                return;
            }

            // Get devices
            List<RachioDevice> devices = apiClient.getDevices();
            if (devices != null && !devices.isEmpty()) {
                for (RachioDevice device : devices) {
                    if (device != null && device.id != null && !device.id.isEmpty()) {
                        deviceDiscovered(device);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error during discovery: {}", e.getMessage(), e);
        }
    }

    public void onDeviceDiscovered(RachioDevice device) {
        logger.debug("Bridge reported device discovered: {}", device.id);
        deviceDiscovered(device);
    }

    public void onZoneDiscovered(String deviceId, RachioZone zone) {
        logger.debug("Bridge reported zone discovered: device={}, zone={}", deviceId, zone.id);
        zoneDiscovered(deviceId, zone);
    }

    private void deviceDiscovered(RachioDevice device) {

        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler == null) {
            logger.debug("Bridge handler not available for device discovery");
            return;
        }

        ThingUID bridgeUID = localBridgeHandler.getThing().getUID();
        ThingUID thingUID = new ThingUID(THING_TYPE_DEVICE, bridgeUID, device.id);

        Map<String, Object> properties = new HashMap<>();
        properties.put(DEVICE_PROP_ID, device.id);

        String deviceName = device.name;
        if (deviceName != null && !deviceName.isEmpty()) {
            properties.put(DEVICE_PROP_NAME, deviceName);
        }

        String deviceStatus = device.status;
        if (deviceStatus != null) {
            properties.put(DEVICE_PROP_STATUS, deviceStatus);
        }

        String deviceSerial = device.getSerialNumberSafe();
        if (deviceSerial != null && !deviceSerial.isEmpty()) {
            properties.put(DEVICE_PROP_SERIAL, deviceSerial);
        }

        String deviceModel = device.model;
        if (deviceModel != null) {
            properties.put(DEVICE_PROP_MODEL, deviceModel);
        }

        String deviceMac = device.getMacAddressNonNull();
        if (deviceMac != null && !deviceMac.isEmpty()) {
            properties.put(DEVICE_PROP_MAC, deviceMac);
        }

        List<RachioZone> deviceZones = device.zones;
        if (deviceZones != null) {
            properties.put(DEVICE_PROP_ZONECOUNT, Integer.toString(deviceZones.size()));

            // Discover zones for this device
            for (RachioZone zone : deviceZones) {
                if (zone != null && zone.id != null && !zone.id.isEmpty()) {
                    zoneDiscovered(device.id, zone);
                }
            }
        }

        thingDiscovered(
                DiscoveryResultBuilder.create(thingUID).withThingType(THING_TYPE_DEVICE).withProperties(properties)
                        .withBridge(bridgeUID).withLabel(deviceName != null ? deviceName : "Rachio Device")
                        .withRepresentationProperty(DEVICE_PROP_ID).build());
    }

    private void zoneDiscovered(String deviceId, RachioZone zone) {

        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler == null) {
            logger.debug("Bridge handler not available for zone discovery");
            return;
        }

        ThingUID bridgeUID = localBridgeHandler.getThing().getUID();
        ThingUID thingUID = new ThingUID(THING_TYPE_ZONE, bridgeUID, zone.id);

        Map<String, Object> properties = new HashMap<>();
        properties.put(ZONE_PROP_ID, zone.id);
        properties.put(PROPERTY_DEVICE_ID, deviceId);

        String zoneName = zone.name;
        if (zoneName != null && !zoneName.isEmpty()) {
            properties.put(ZONE_PROP_NAME, zoneName);
        }

        properties.put(ZONE_PROP_NUMBER, Integer.toString(zone.zoneNumber));
        properties.put(ZONE_PROP_ENABLED, Boolean.toString(zone.enabled));

        properties.put(ZONE_PROP_SOIL, Boolean.toString(zone.customSoil));
        properties.put(ZONE_PROP_CROP, Boolean.toString(zone.customCrop));
        properties.put(ZONE_PROP_NOZZLE, Boolean.toString(zone.customNozzle));

        properties.put(ZONE_PROP_EFFICIENCY, Double.toString(zone.efficiency));

        String label = (zoneName != null ? zoneName : "Zone") + " " + zone.zoneNumber;
        thingDiscovered(
                DiscoveryResultBuilder.create(thingUID).withThingType(THING_TYPE_ZONE).withProperties(properties)
                        .withBridge(bridgeUID).withLabel(label).withRepresentationProperty(ZONE_PROP_ID).build());
    }

    @Override
    public String getListenerId() {
        return "discovery-service";
    }

    @Override
    public RachioStatusListener.ListenerType getListenerType() {
        return RachioStatusListener.ListenerType.DISCOVERY;
    }

    @Override
    public void onWebhookEventReceived(@Nullable String eventJson) {
        logger.trace("Discovery service received webhook event");
    }

    @Override
    public void onRateLimitStatusChanged(int remaining, int limit, @Nullable String status) {
        logger.trace("Discovery service received rate limit status: {}", status);
    }

    @Override
    public void onDeviceStatusChanged(@Nullable RachioDevice device) {
        if (device != null && device.id != null) {
            logger.debug("Device status changed: {}", device.id);
        }
    }

    @Override
    public void onDeviceStatusChanged(@Nullable String deviceId, @Nullable String status) {
        if (deviceId != null) {
            logger.debug("Device {} status changed to: {}", deviceId, status);
        }
    }

    @Override
    public void onDeviceUpdated(@Nullable RachioDevice device) {
        if (device != null && device.id != null) {
            logger.debug("Device updated: {}", device.id);
            deviceDiscovered(device);
        }
    }

    @Override
    public void onZoneUpdated(@Nullable String deviceId, @Nullable RachioZone zone) {
        if (deviceId != null && zone != null && zone.id != null) {
            logger.debug("Zone updated: device={}, zone={}", deviceId, zone.id);
            zoneDiscovered(deviceId, zone);
        }
    }

    @Override
    public void onNewDevice(@Nullable String deviceId) {
        logger.debug("New device notification received: {}", deviceId);
        if (deviceId == null)
            return;

        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler == null)
            return;

        try {
            RachioApiClient apiClient = localBridgeHandler.getApiClient();
            if (apiClient == null)
                return;

            RachioDevice device = apiClient.getDevice(deviceId);
            if (device != null)
                deviceDiscovered(device);
        } catch (Exception e) {
            logger.debug("Error discovering new device {}: {}", deviceId, e.getMessage(), e);
        }
    }

    @Override
    public void onNewZone(@Nullable String deviceId, @Nullable String zoneId) {
        logger.debug("New zone notification received: device={}, zone={}", deviceId, zoneId);
        if (deviceId == null || zoneId == null)
            return;

        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler == null)
            return;

        try {
            RachioApiClient apiClient = localBridgeHandler.getApiClient();
            if (apiClient == null)
                return;

            List<RachioZone> zones = apiClient.getZones(deviceId);
            if (zones != null) {
                for (RachioZone zone : zones) {
                    if (zone != null && zoneId.equals(zone.id)) {
                        zoneDiscovered(deviceId, zone);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error discovering new zone {}: {}", zoneId, e.getMessage(), e);
        }
    }

    @Override
    public void onZoneStatusChanged(@Nullable String deviceId, @Nullable String zoneId, @Nullable String status) {
        if (deviceId != null && zoneId != null) {
            logger.trace("Zone status changed: device={}, zone={}, status={}", deviceId, zoneId, status);
        }
    }

    public void triggerDiscovery() {
        startScan();
    }

    public boolean isDiscoveryActive() {
        return discoveryJob != null && !discoveryJob.isCancelled();
    }
}
