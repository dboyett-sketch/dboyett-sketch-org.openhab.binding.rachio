package org.openhab.binding.rachio.internal.api.dto;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Data transfer object for Rachio webhook events.
 */
@NonNullByDefault
public class RachioWebHookEvent {
    private String id = "";
    private String type = "";
    private @Nullable String subType;
    private @Nullable Map<String, Object> data;
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public @Nullable String getSubType() {
        return subType;
    }
    
    public void setSubType(@Nullable String subType) {
        this.subType = subType;
    }
    
    public @Nullable Map<String, Object> getData() {
        return data;
    }
    
    public void setData(@Nullable Map<String, Object> data) {
        this.data = data;
    }
    
    @Override
    public String toString() {
        return "RachioWebHookEvent [id=" + id + ", type=" + type + ", subType=" + subType + ", data=" + data + "]";
    }
}
