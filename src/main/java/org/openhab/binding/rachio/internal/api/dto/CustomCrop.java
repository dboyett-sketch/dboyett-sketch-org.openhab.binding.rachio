package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for Rachio Custom Crop Configuration
 *
 * @author Daniel B. - Professional irrigation data
 */
@NonNullByDefault
public class CustomCrop {
    private String id = "";
    private String name = "";
    private @Nullable Double coefficient; // 0.0-1.0
    private @Nullable Double rootDepth; // inches
    
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
    
    public @Nullable Double getCoefficient() {
        return coefficient;
    }
    
    public void setCoefficient(@Nullable Double coefficient) {
        this.coefficient = coefficient;
    }
    
    public @Nullable Double getRootDepth() {
        return rootDepth;
    }
    
    public void setRootDepth(@Nullable Double rootDepth) {
        this.rootDepth = rootDepth;
    }
    
    @Override
    public String toString() {
        return "CustomCrop{id='" + id + "', name='" + name + "', coefficient=" + coefficient + ", rootDepth=" + rootDepth + "}";
    }
}
