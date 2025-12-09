package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;

/**
 * DTO for Rachio Person (account)
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioPerson {
    @Nullable
    public String id;
    
    @Nullable
    public String username;
    
    @Nullable
    public String email;
    
    @Nullable
    public String fullName;
    
    @Nullable
    public List<RachioDevice> devices;
    
    @Override
    public String toString() {
        return "RachioPerson [id=" + id + ", username=" + username + ", devices=" + 
               (devices != null ? devices.size() : 0) + "]";
    }
}
