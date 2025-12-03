package org.openhab.binding.rachio.internal;

import org.apache.commons.net.util.SubnetUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Security utilities for Rachio binding including IP filtering.
 *
 * @author Michael Lobstein - Initial contribution (original)
 * @author Damion Boyett - OpenHAB 5.x migration
 */
@NonNullByDefault
public class RachioSecurity {
    private static final Logger logger = LoggerFactory.getLogger(RachioSecurity.class);

    /**
     * AWS IP address range entry
     */
    public static class AwsIpAddressRange {
        public String ip_prefix = "";
        public String region = "";
        public String service = "";
    }

    /**
     * Check if client IP is in allowed subnet/IP list
     * 
     * @param clientIp IP address to check (e.g., "192.168.1.100")
     * @param ipFilter Semicolon-separated list of IPs/subnets (e.g., "192.168.0.0/16;10.0.0.0/8")
     * @return true if IP is allowed, false if not allowed (or false if no filter configured)
     */
    public static boolean isIpInAllowedSubnet(String clientIp, String ipFilter) {
        if (ipFilter == null || ipFilter.trim().isEmpty()) {
            logger.trace("No IP filter configured, allowing all IPs");
            return true; // No filtering configured
        }

        logger.debug("Checking IP {} against filter: {}", clientIp, ipFilter);
        
        String[] subnetMasks = ipFilter.split(";");
        for (String subnetMask : subnetMasks) {
            subnetMask = subnetMask.trim();
            if (subnetMask.isEmpty()) {
                continue;
            }

            // Exact IP match
            if (clientIp.equals(subnetMask)) {
                logger.debug("IP {} matches exact filter entry: {}", clientIp, subnetMask);
                return true;
            }

            // Subnet range match (CIDR notation)
            if (subnetMask.contains("/")) {
                try {
                    SubnetUtils utils = new SubnetUtils(subnetMask);
                    utils.setInclusiveHostCount(true); // Include network and broadcast addresses
                    
                    if (utils.getInfo().isInRange(clientIp)) {
                        logger.debug("IP {} is in subnet range: {}", clientIp, subnetMask);
                        return true;
                    }
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid subnet range in IP filter '{}': {}", subnetMask, e.getMessage());
                    // Continue checking other entries
                }
            }
        }

        logger.warn("IP {} is NOT in allowed IP filter list", clientIp);
        return false;
    }

    /**
     * Check if IP is in AWS IP range list
     * This is used to verify webhooks from Rachio's AWS infrastructure
     * 
     * @param clientIp IP address to check
     * @param awsRanges List of AWS IP address ranges
     * @return true if IP is in AWS ranges
     */
    public static boolean isIpInAwsRange(String clientIp, java.util.List<AwsIpAddressRange> awsRanges) {
        if (awsRanges.isEmpty()) {
            logger.trace("No AWS ranges configured, skipping AWS verification");
            return false;
        }

        for (AwsIpAddressRange range : awsRanges) {
            if (isIpInAllowedSubnet(clientIp, range.ip_prefix)) {
                logger.debug("IP {} is in AWS range: {} (region: {}, service: {})", 
                           clientIp, range.ip_prefix, range.region, range.service);
                return true;
            }
        }

        logger.debug("IP {} is NOT in configured AWS IP ranges", clientIp);
        return false;
    }

    /**
     * Validate an IP address format
     * 
     * @param ip IP address to validate
     * @return true if valid IPv4 address
     */
    public static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }
            
            for (String part : parts) {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            }
            
            // Check for trailing characters
            if (ip.endsWith(".")) {
                return false;
            }
            
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Simple test method to verify IP filtering logic
     */
    public static void testIpFiltering() {
        String ipFilter = "192.168.0.0/16;10.0.0.0/8;172.16.0.0/12;127.0.0.1";
        
        // Test cases
        String[] testIps = {
            "192.168.1.100",  // Should pass (in 192.168.0.0/16)
            "10.1.2.3",       // Should pass (in 10.0.0.0/8)
            "172.16.0.5",     // Should pass (in 172.16.0.0/12)
            "127.0.0.1",      // Should pass (exact match)
            "8.8.8.8",        // Should fail (not in filter)
            "192.169.1.1",    // Should fail (outside 192.168.0.0/16)
        };
        
        logger.info("Testing IP filtering with filter: {}", ipFilter);
        for (String testIp : testIps) {
            boolean allowed = isIpInAllowedSubnet(testIp, ipFilter);
            logger.info("IP {}: {}", testIp, allowed ? "ALLOWED" : "DENIED");
        }
    }
}