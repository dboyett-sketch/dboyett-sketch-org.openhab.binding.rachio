package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.ActionOutput;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * The {@link RachioActions} class defines rule actions for the Rachio binding.
 *
 * @author Dave Boyett - Initial contribution
 */
@ThingActionsScope(name = "rachio")
@NonNullByDefault
public class RachioActions implements ThingActions {

    private final Logger logger = LoggerFactory.getLogger(RachioActions.class);
    private @Nullable RachioBridgeHandler handler;

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof RachioBridgeHandler) {
            this.handler = (RachioBridgeHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return handler;
    }

    /**
     * Start a zone for a specific duration
     */
    @RuleAction(label = "Start Zone", description = "Start a Rachio zone for specified duration")
    @ActionOutput(name = "success", label = "Success", type = "java.lang.Boolean", description = "True if the command was successful")
    public Map<String, Object> startZone(
            @ActionInput(name = "zoneId", label = "Zone ID", required = true, description = "The ID of the zone to start") String zoneId,
            @ActionInput(name = "duration", label = "Duration", required = true, description = "Duration in seconds") int duration) {
        
        logger.debug("Action: startZone called with zoneId={}, duration={}", zoneId, duration);
        boolean success = false;
        
        try {
            RachioBridgeHandler localHandler = handler;
            if (localHandler != null) {
                localHandler.getHttpHandler().startZone(zoneId, duration);
                success = true;
                logger.info("Started zone {} for {} seconds", zoneId, duration);
            } else {
                logger.warn("Handler not available for startZone action");
            }
        } catch (RachioApiException e) {
            logger.error("Failed to start zone: {}", e.getMessage(), e);
        }
        
        return Map.of("success", success);
    }

    /**
     * Run all zones sequentially
     */
    @RuleAction(label = "Run All Zones", description = "Run all enabled zones sequentially for specified duration")
    @ActionOutput(name = "success", label = "Success", type = "java.lang.Boolean", description = "True if the command was successful")
    public Map<String, Object> runAllZones(
            @ActionInput(name = "duration", label = "Duration", required = true, description = "Duration in seconds for each zone") int duration) {
        
        logger.debug("Action: runAllZones called with duration={}", duration);
        boolean success = false;
        
        try {
            RachioBridgeHandler localHandler = handler;
            if (localHandler != null && localHandler.getBridgeConfiguration() != null) {
                String thingId = localHandler.getThing().getUID().getId();
                String deviceId = localHandler.getBridgeConfiguration().deviceId;
                
                if (deviceId != null && !deviceId.isEmpty()) {
                    localHandler.getHttpHandler().runAllZones(thingId, duration, deviceId);
                    success = true;
                    logger.info("Running all zones for {} seconds each", duration);
                } else {
                    logger.warn("Device ID not configured for runAllZones action");
                }
            } else {
                logger.warn("Handler not available for runAllZones action");
            }
        } catch (RachioApiException e) {
            logger.error("Failed to run all zones: {}", e.getMessage(), e);
        }
        
        return Map.of("success", success);
    }

    /**
     * Run the next available zone
     */
    @RuleAction(label = "Run Next Zone", description = "Run the next available zone for specified duration")
    @ActionOutput(name = "success", label = "Success", type = "java.lang.Boolean", description = "True if the command was successful")
    public Map<String, Object> runNextZone(
            @ActionInput(name = "duration", label = "Duration", required = true, description = "Duration in seconds") int duration) {
        
        logger.debug("Action: runNextZone called with duration={}", duration);
        boolean success = false;
        
        try {
            RachioBridgeHandler localHandler = handler;
            if (localHandler != null && localHandler.getBridgeConfiguration() != null) {
                String thingId = localHandler.getThing().getUID().getId();
                String deviceId = localHandler.getBridgeConfiguration().deviceId;
                
                if (deviceId != null && !deviceId.isEmpty()) {
                    localHandler.getHttpHandler().runNextZone(thingId, duration, deviceId);
                    success = true;
                    logger.info("Running next zone for {} seconds", duration);
                } else {
                    logger.warn("Device ID not configured for runNextZone action");
                }
            } else {
                logger.warn("Handler not available for runNextZone action");
            }
        } catch (RachioApiException e) {
            logger.error("Failed to run next zone: {}", e.getMessage(), e);
        }
        
        return Map.of("success", success);
    }

    /**
     * Set rain delay
     */
    @RuleAction(label = "Set Rain Delay", description = "Set rain delay for specified hours (0-168)")
    @ActionOutput(name = "success", label = "Success", type = "java.lang.Boolean", description = "True if the command was successful")
    public Map<String, Object> rainDelay(
            @ActionInput(name = "hours", label = "Hours", required = true, description = "Hours to delay (0-168, 0 to cancel)") int hours) {
        
        logger.debug("Action: rainDelay called with hours={}", hours);
        boolean success = false;
        
        if (hours < 0 || hours > 168) {
            logger.warn("Invalid rain delay hours: {} (must be 0-168)", hours);
            return Map.of("success", false);
        }
        
        try {
            RachioBridgeHandler localHandler = handler;
            if (localHandler != null && localHandler.getBridgeConfiguration() != null) {
                String thingId = localHandler.getThing().getUID().getId();
                String deviceId = localHandler.getBridgeConfiguration().deviceId;
                
                if (deviceId != null && !deviceId.isEmpty()) {
                    localHandler.getHttpHandler().rainDelay(thingId, hours, deviceId);
                    success = true;
                    logger.info("Set rain delay to {} hours", hours);
                } else {
                    logger.warn("Device ID not configured for rainDelay action");
                }
            } else {
                logger.warn("Handler not available for rainDelay action");
            }
        } catch (RachioApiException e) {
            logger.error("Failed to set rain delay: {}", e.getMessage(), e);
        }
        
        return Map.of("success", success);
    }

    /**
     * Stop all watering
     */
    @RuleAction(label = "Stop Watering", description = "Stop all watering immediately")
    @ActionOutput(name = "success", label = "Success", type = "java.lang.Boolean", description = "True if the command was successful")
    public Map<String, Object> stopWatering() {
        
        logger.debug("Action: stopWatering called");
        boolean success = false;
        
        try {
            RachioBridgeHandler localHandler = handler;
            if (localHandler != null && localHandler.getBridgeConfiguration() != null) {
                String deviceId = localHandler.getBridgeConfiguration().deviceId;
                
                if (deviceId != null && !deviceId.isEmpty()) {
                    localHandler.getHttpHandler().stopWatering(deviceId);
                    success = true;
                    logger.info("Stopped all watering");
                } else {
                    logger.warn("Device ID not configured for stopWatering action");
                }
            } else {
                logger.warn("Handler not available for stopWatering action");
            }
        } catch (RachioApiException e) {
            logger.error("Failed to stop watering: {}", e.getMessage(), e);
        }
        
        return Map.of("success", success);
    }

    /**
     * Enable or disable a zone
     */
    @RuleAction(label = "Set Zone Enabled", description = "Enable or disable a zone")
    @ActionOutput(name = "success", label = "Success", type = "java.lang.Boolean", description = "True if the command was successful")
    public Map<String, Object> setZoneEnabled(
            @ActionInput(name = "zoneId", label = "Zone ID", required = true, description = "The ID of the zone") String zoneId,
            @ActionInput(name = "enabled", label = "Enabled", required = true, description = "True to enable, false to disable") boolean enabled) {
        
        logger.debug("Action: setZoneEnabled called with zoneId={}, enabled={}", zoneId, enabled);
        boolean success = false;
        
        try {
            RachioBridgeHandler localHandler = handler;
            if (localHandler != null) {
                localHandler.getHttpHandler().setZoneEnabled(zoneId, enabled);
                success = true;
                logger.info("Set zone {} enabled to {}", zoneId, enabled);
            } else {
                logger.warn("Handler not available for setZoneEnabled action");
            }
        } catch (RachioApiException e) {
            logger.error("Failed to set zone enabled: {}", e.getMessage(), e);
        }
        
        return Map.of("success", success);
    }

    /**
     * Get zone information
     */
    @RuleAction(label = "Get Zone Info", description = "Get information about a zone")
    @ActionOutput(name = "zoneName", label = "Zone Name", type = "java.lang.String", description = "Name of the zone")
    @ActionOutput(name = "zoneEnabled", label = "Zone Enabled", type = "java.lang.Boolean", description = "Whether the zone is enabled")
    @ActionOutput(name = "zoneRuntime", label = "Zone Runtime", type = "java.lang.Integer", description = "Runtime in seconds")
    @ActionOutput(name = "success", label = "Success", type = "java.lang.Boolean", description = "True if the command was successful")
    public Map<String, Object> getZoneInfo(
            @ActionInput(name = "zoneId", label = "Zone ID", required = true, description = "The ID of the zone") String zoneId) {
        
        logger.debug("Action: getZoneInfo called with zoneId={}", zoneId);
        
        String zoneName = "";
        boolean zoneEnabled = false;
        int zoneRuntime = 0;
        boolean success = false;
        
        try {
            RachioBridgeHandler localHandler = handler;
            if (localHandler != null && localHandler.getBridgeConfiguration() != null) {
                String deviceId = localHandler.getBridgeConfiguration().deviceId;
                
                if (deviceId != null && !deviceId.isEmpty()) {
                    // Get device data to find zone
                    var device = localHandler.getHttpHandler().getDevice(deviceId);
                    if (device != null && device.zones != null) {
                        for (var zone : device.zones) {
                            if (zone.id.equals(zoneId)) {
                                zoneName = zone.name;
                                zoneEnabled = zone.enabled;
                                zoneRuntime = zone.runtime;
                                success = true;
                                logger.debug("Found zone: {}, enabled: {}, runtime: {}", zoneName, zoneEnabled, zoneRuntime);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (RachioApiException e) {
            logger.error("Failed to get zone info: {}", e.getMessage(), e);
        }
        
        if (!success) {
            logger.warn("Could not find zone with ID: {}", zoneId);
        }
        
        return Map.of(
            "zoneName", zoneName,
            "zoneEnabled", zoneEnabled,
            "zoneRuntime", zoneRuntime,
            "success", success
        );
    }

    /**
     * Get device status
     */
    @RuleAction(label = "Get Device Status", description = "Get the status of the Rachio device")
    @ActionOutput(name = "deviceName", label = "Device Name", type = "java.lang.String", description = "Name of the device")
    @ActionOutput(name = "deviceStatus", label = "Device Status", type = "java.lang.String", description = "Status (ONLINE/OFFLINE/SLEEP)")
    @ActionOutput(name = "zoneCount", label = "Zone Count", type = "java.lang.Integer", description = "Number of zones")
    @ActionOutput(name = "success", label = "Success", type = "java.lang.Boolean", description = "True if the command was successful")
    public Map<String, Object> getDeviceStatus() {
        
        logger.debug("Action: getDeviceStatus called");
        
        String deviceName = "";
        String deviceStatus = "UNKNOWN";
        int zoneCount = 0;
        boolean success = false;
        
        try {
            RachioBridgeHandler localHandler = handler;
            if (localHandler != null && localHandler.getBridgeConfiguration() != null) {
                String deviceId = localHandler.getBridgeConfiguration().deviceId;
                
                if (deviceId != null && !deviceId.isEmpty()) {
                    var device = localHandler.getHttpHandler().getDevice(deviceId);
                    if (device != null) {
                        deviceName = device.name;
                        deviceStatus = device.status;
                        zoneCount = (device.zones != null) ? device.zones.size() : 0;
                        success = true;
                        logger.debug("Device status: {}, zones: {}", deviceStatus, zoneCount);
                    }
                }
            }
        } catch (RachioApiException e) {
            logger.error("Failed to get device status: {}", e.getMessage(), e);
        }
        
        return Map.of(
            "deviceName", deviceName,
            "deviceStatus", deviceStatus,
            "zoneCount", zoneCount,
            "success", success
        );
    }

    /**
     * Check if device is online
     */
    @RuleAction(label = "Is Device Online", description = "Check if the Rachio device is online")
    @ActionOutput(name = "online", label = "Online", type = "java.lang.Boolean", description = "True if the device is online")
    @ActionOutput(name = "success", label = "Success", type = "java.lang.Boolean", description = "True if the command was successful")
    public Map<String, Object> isDeviceOnline() {
        
        logger.debug("Action: isDeviceOnline called");
        
        boolean online = false;
        boolean success = false;
        
        try {
            RachioBridgeHandler localHandler = handler;
            if (localHandler != null && localHandler.getBridgeConfiguration() != null) {
                String deviceId = localHandler.getBridgeConfiguration().deviceId;
                
                if (deviceId != null && !deviceId.isEmpty()) {
                    var device = localHandler.getHttpHandler().getDevice(deviceId);
                    if (device != null) {
                        online = "ONLINE".equals(device.status);
                        success = true;
                        logger.debug("Device online status: {}", online);
                    }
                }
            }
        } catch (RachioApiException e) {
            logger.error("Failed to check device online status: {}", e.getMessage(), e);
        }
        
        return Map.of(
            "online", online,
            "success", success
        );
    }

    // Static methods for rule DSL compatibility
    
    public static Map<String, Object> startZone(@Nullable ThingActions actions, String zoneId, int duration) {
        if (actions instanceof RachioActions) {
            return ((RachioActions) actions).startZone(zoneId, duration);
        }
        logger.warn("Actions instance is not RachioActions");
        return Map.of("success", false);
    }
    
    public static Map<String, Object> runAllZones(@Nullable ThingActions actions, int duration) {
        if (actions instanceof RachioActions) {
            return ((RachioActions) actions).runAllZones(duration);
        }
        logger.warn("Actions instance is not RachioActions");
        return Map.of("success", false);
    }
    
    public static Map<String, Object> runNextZone(@Nullable ThingActions actions, int duration) {
        if (actions instanceof RachioActions) {
            return ((RachioActions) actions).runNextZone(duration);
        }
        logger.warn("Actions instance is not RachioActions");
        return Map.of("success", false);
    }
    
    public static Map<String, Object> rainDelay(@Nullable ThingActions actions, int hours) {
        if (actions instanceof RachioActions) {
            return ((RachioActions) actions).rainDelay(hours);
        }
        logger.warn("Actions instance is not RachioActions");
        return Map.of("success", false);
    }
    
    public static Map<String, Object> stopWatering(@Nullable ThingActions actions) {
        if (actions instanceof RachioActions) {
            return ((RachioActions) actions).stopWatering();
        }
        logger.warn("Actions instance is not RachioActions");
        return Map.of("success", false);
    }
    
    public static Map<String, Object> setZoneEnabled(@Nullable ThingActions actions, String zoneId, boolean enabled) {
        if (actions instanceof RachioActions) {
            return ((RachioActions) actions).setZoneEnabled(zoneId, enabled);
        }
        logger.warn("Actions instance is not RachioActions");
        return Map.of("success", false);
    }
    
    public static Map<String, Object> getZoneInfo(@Nullable ThingActions actions, String zoneId) {
        if (actions instanceof RachioActions) {
            return ((RachioActions) actions).getZoneInfo(zoneId);
        }
        logger.warn("Actions instance is not RachioActions");
        return Map.of("zoneName", "", "zoneEnabled", false, "zoneRuntime", 0, "success", false);
    }
    
    public static Map<String, Object> getDeviceStatus(@Nullable ThingActions actions) {
        if (actions instanceof RachioActions) {
            return ((RachioActions) actions).getDeviceStatus();
        }
        logger.warn("Actions instance is not RachioActions");
        return Map.of("deviceName", "", "deviceStatus", "UNKNOWN", "zoneCount", 0, "success", false);
    }
    
    public static Map<String, Object> isDeviceOnline(@Nullable ThingActions actions) {
        if (actions instanceof RachioActions) {
            return ((RachioActions) actions).isDeviceOnline();
        }
        logger.warn("Actions instance is not RachioActions");
        return Map.of("online", false, "success", false);
    }
}
