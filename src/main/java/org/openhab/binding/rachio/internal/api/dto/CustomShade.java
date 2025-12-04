package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for Rachio Custom Shade Configuration
 *
 * @author Daniel B. - Professional irrigation data
 */
@NonNullByDefault
public class CustomShade {
    private String id = "";
    private String name = "";
    private @Nullable Double percentage; // 0-100%
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public @Nullable Double getPercentage() {
        return percentage;
    }
    
    public void setPercentage(@Nullable Double percentage) {
        this.percentage = percentage;
    }
    
    @Override
    public String toString() {
        return "CustomShade{id='" + id + "', name='" + name + "', percentage=" + percentage + "}";
    }
}
