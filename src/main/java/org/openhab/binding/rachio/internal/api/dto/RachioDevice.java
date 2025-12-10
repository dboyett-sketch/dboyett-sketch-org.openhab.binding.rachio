package org.openhab.binding.rachio.internal.api.dto;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Data transfer object for Rachio devices.
 */
@NonNullByDefault
public class RachioDevice {
    private String id = "";
    private String name = "";
    private String model = "";
    private String serialNumber = "";
    private String macAddress = "";
    private String status = "";
    private List<RachioZone> zones;
    
    // Missing fields from compilation errors
    private @Nullable Boolean online;
    private @Nullable Long rainDelayExpiration;
    private @Nullable Boolean paused;
    private @Nullable String firmwareVersion;
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public List<RachioZone> getZones() { return zones; }
    public void setZones(List<RachioZone> zones) { this.zones = zones; }
    
    // Missing getters and setters from compilation errors
    public @Nullable Boolean getOnline() { return online; }
    public void setOnline(@Nullable Boolean online) { this.online = online; }
    
    public @Nullable Long getRainDelayExpiration() { return rainDelayExpiration; }
    public void setRainDelayExpiration(@Nullable Long rainDelayExpiration) { this.rainDelayExpiration = rainDelayExpiration; }
    
    public @Nullable Boolean getPaused() { return paused; }
    public void setPaused(@Nullable Boolean paused) { this.paused = paused; }
    
    public @Nullable String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(@Nullable String firmwareVersion) { this.firmwareVersion = firmwareVersion; }
    
    @Override
    public String toString() {
        return "RachioDevice [id=" + id + ", name=" + name + ", model=" + model + 
               ", serialNumber=" + serialNumber + ", macAddress=" + macAddress + 
               ", status=" + status + ", online=" + online + 
               ", rainDelayExpiration=" + rainDelayExpiration + ", paused=" + paused + 
               ", firmwareVersion=" + firmwareVersion + ", zones=" + zones + "]";
    }
}
