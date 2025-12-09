package org.openhab.binding.rachio.internal.api;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Security utility for IP filtering and validation
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioSecurity {
    private final Logger logger = LoggerFactory.getLogger(RachioSecurity.class);
    
    private @Nullable String secretKey;
    private final List<String> allowedIpRanges = new ArrayList<>();
    private boolean allowAwsIps = false;
    
    // AWS IP ranges (simplified - in reality should fetch from AWS)
    private static final String[] AWS_IP_RANGES = {
        "3.80.0.0/12", "3.208.0.0/12", "13.248.118.0/24", "18.204.0.0/14",
        "35.168.0.0/13", "52.0.0.0/15", "52.4.0.0/14", "52.20.0.0/14"
    };

    public void setSecretKey(@Nullable String secretKey) {
        this.secretKey = secretKey;
    }
    
    public void setAllowedIpAddresses(@Nullable String ipAddresses) {
        allowedIpRanges.clear();
        if (ipAddresses != null && !ipAddresses.isEmpty()) {
            String[] ips = ipAddresses.split(",");
            for (String ip : ips) {
                String trimmedIp = ip.trim();
                if (!trimmedIp.isEmpty()) {
                    allowedIpRanges.add(trimmedIp);
                }
            }
        }
    }
    
    public void setAllowAwsIps(@Nullable Boolean allowAwsIps) {
        this.allowAwsIps = allowAwsIps != null ? allowAwsIps : false;
    }
    
    public boolean isIpAllowed(@Nullable String ipAddress, @Nullable String forwardedFor) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }
        
        // Check X-Forwarded-For if present
        String clientIp = ipAddress;
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            String[] ips = forwardedFor.split(",");
            if (ips.length > 0) {
                clientIp = ips[0].trim();
            }
        }
        
        // Check against allowed IP ranges
        for (String range : allowedIpRanges) {
            if (isIpInRange(clientIp, range)) {
                return true;
            }
        }
        
        // Check AWS IP ranges if enabled
        if (allowAwsIps) {
            for (String awsRange : AWS_IP_RANGES) {
                if (isIpInRange(clientIp, awsRange)) {
                    return true;
                }
            }
        }
        
        logger.debug("IP {} not in allowed ranges", clientIp);
        return false;
    }
    
    private boolean isIpInRange(String ip, String range) {
        try {
            if (range.contains("/")) {
                // CIDR notation
                String[] parts = range.split("/");
                String network = parts[0];
                int prefix;
                try {
                    prefix = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    return false;
                }
                
                InetAddress ipAddr = InetAddress.getByName(ip);
                InetAddress networkAddr = InetAddress.getByName(network);
                
                byte[] ipBytes = ipAddr.getAddress();
                byte[] networkBytes = networkAddr.getAddress();
                
                if (ipBytes.length != networkBytes.length) {
                    return false;
                }
                
                int mask = 0xffffffff << (32 - prefix);
                int ipInt = ((ipBytes[0] & 0xFF) << 24) |
                           ((ipBytes[1] & 0xFF) << 16) |
                           ((ipBytes[2] & 0xFF) << 8) |
                           (ipBytes[3] & 0xFF);
                int networkInt = ((networkBytes[0] & 0xFF) << 24) |
                                ((networkBytes[1] & 0xFF) << 16) |
                                ((networkBytes[2] & 0xFF) << 8) |
                                (networkBytes[3] & 0xFF);
                
                return (ipInt & mask) == (networkInt & mask);
            } else {
                // Exact IP match
                return ip.equals(range);
            }
        } catch (UnknownHostException e) {
            logger.debug("Invalid IP address or range: {} - {}", ip, range);
            return false;
        }
    }
    
    public @Nullable String getSecretKey() {
        return secretKey;
    }
}
