package org.openhab.binding.rachio.internal.api;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Security utilities for Rachio binding including webhook signature validation and IP filtering
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioSecurity {

    private final Logger logger = LoggerFactory.getLogger(RachioSecurity.class);

    // Security algorithms
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SHA256 = "SHA-256";
    
    // AWS IP ranges for Rachio webhooks (these are the actual Rachio server IPs)
    private static final String[] RACHIO_IP_RANGES = {
        "52.25.110.0/24",    // US West (Oregon)
        "52.32.110.0/24",    // US West (Oregon)
        "54.70.0.0/16",      // US West (Oregon)
        "54.149.0.0/16",     // US West (Oregon)
        "54.184.0.0/13",     // US West (Oregon)
        "54.200.0.0/15",     // US West (Oregon)
        "54.218.0.0/16",     // US West (Oregon)
        "54.244.0.0/16",     // US West (Oregon)
    };
    
    // AWS IP ranges cache
    private final List<String> awsIpRanges = new CopyOnWriteArrayList<>();
    private final List<IpNetwork> allowedNetworks = new CopyOnWriteArrayList<>();
    
    // Configuration
    private boolean useAwsIpRanges = true;
    private final List<String> customAllowedIps = new CopyOnWriteArrayList<>();
    private Instant lastIpRangeUpdate;
    
    /**
     * Constructor
     */
    public RachioSecurity() {
        initializeDefaultNetworks();
        loadAwsIpRanges();
    }

    /**
     * Initialize default allowed networks
     */
    private void initializeDefaultNetworks() {
        // Add localhost addresses
        addCustomIp("127.0.0.1");
        addCustomIp("::1");
        
        // Add private network ranges
        addCustomIp("10.0.0.0/8");
        addCustomIp("172.16.0.0/12");
        addCustomIp("192.168.0.0/16");
        
        logger.debug("Initialized default IP networks");
    }

    /**
     * Load AWS IP ranges for Rachio
     */
    private void loadAwsIpRanges() {
        if (!useAwsIpRanges) {
            return;
        }
        
        try {
            awsIpRanges.clear();
            
            // Add predefined Rachio IP ranges
            for (String range : RACHIO_IP_RANGES) {
                awsIpRanges.add(range);
                try {
                    allowedNetworks.add(new IpNetwork(range));
                    logger.trace("Added AWS IP range: {}", range);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid IP range format: {}", range);
                }
            }
            
            lastIpRangeUpdate = Instant.now();
            logger.info("Loaded {} AWS IP ranges for Rachio webhooks", awsIpRanges.size());
            
        } catch (Exception e) {
            logger.error("Error loading AWS IP ranges: {}", e.getMessage(), e);
        }
    }

    /**
     * Validate webhook signature using HMAC-SHA256
     */
    public boolean validateWebhookSignature(String payload, String signature, String secret) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        
        if (payload == null || payload.isEmpty()) {
            logger.warn("Payload is null or empty");
            return false;
        }
        
        if (signature == null || signature.isEmpty()) {
            logger.warn("Signature is null or empty");
            return false;
        }
        
        if (secret == null || secret.isEmpty()) {
            logger.warn("Secret is null or empty");
            return false;
        }
        
        // Generate HMAC-SHA256 signature
        String calculatedSignature = calculateHmacSha256(payload, secret);
        
        // Constant-time comparison to prevent timing attacks
        boolean isValid = constantTimeEquals(calculatedSignature, signature);
        
        if (logger.isDebugEnabled()) {
            logger.debug("Signature validation: {} (expected: {}, calculated: {})", 
                    isValid ? "VALID" : "INVALID", 
                    signature, 
                    calculatedSignature);
        }
        
        return isValid;
    }

    /**
     * Calculate HMAC-SHA256 signature
     */
    public String calculateHmacSha256(String data, String secret) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), 
                HMAC_SHA256
        );
        
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(secretKeySpec);
        
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        // Convert to hexadecimal
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }

    /**
     * Constant-time string comparison to prevent timing attacks
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }

    /**
     * Check if IP address is allowed
     */
    public boolean isAllowedIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            logger.warn("IP address is null or empty");
            return false;
        }
        
        // Parse IP address
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            logger.warn("Invalid IP address format: {}", ipAddress);
            return false;
        }
        
        // Check against allowed networks
        for (IpNetwork network : allowedNetworks) {
            if (network.contains(inetAddress)) {
                logger.debug("IP {} allowed by network {}", ipAddress, network);
                return true;
            }
        }
        
        // Check custom IPs
        for (String customIp : customAllowedIps) {
            try {
                if (customIp.contains("/")) {
                    // It's a network
                    IpNetwork network = new IpNetwork(customIp);
                    if (network.contains(inetAddress)) {
                        logger.debug("IP {} allowed by custom network {}", ipAddress, customIp);
                        return true;
                    }
                } else {
                    // It's a single IP
                    InetAddress customInetAddress = InetAddress.getByName(customIp);
                    if (customInetAddress.equals(inetAddress)) {
                        logger.debug("IP {} allowed by custom IP {}", ipAddress, customIp);
                        return true;
                    }
                }
            } catch (Exception e) {
                logger.warn("Invalid custom IP format: {}", customIp);
            }
        }
        
        logger.debug("IP {} not allowed", ipAddress);
        return false;
    }

    /**
     * Add custom IP address or network to allowed list
     */
    public void addCustomIp(String ipOrNetwork) {
        if (ipOrNetwork == null || ipOrNetwork.isEmpty()) {
            return;
        }
        
        try {
            if (ipOrNetwork.contains("/")) {
                // It's a network
                allowedNetworks.add(new IpNetwork(ipOrNetwork));
                customAllowedIps.add(ipOrNetwork);
                logger.info("Added custom IP network: {}", ipOrNetwork);
            } else {
                // It's a single IP
                InetAddress inetAddress = InetAddress.getByName(ipOrNetwork);
                allowedNetworks.add(new IpNetwork(ipOrNetwork + "/32"));
                customAllowedIps.add(ipOrNetwork);
                logger.info("Added custom IP address: {}", ipOrNetwork);
            }
        } catch (Exception e) {
            logger.error("Error adding custom IP {}: {}", ipOrNetwork, e.getMessage());
        }
    }

    /**
     * Remove custom IP address or network
     */
    public void removeCustomIp(String ipOrNetwork) {
        customAllowedIps.remove(ipOrNetwork);
        
        // Rebuild allowed networks
        allowedNetworks.clear();
        initializeDefaultNetworks();
        loadAwsIpRanges();
        
        // Re-add remaining custom IPs
        for (String customIp : customAllowedIps) {
            addCustomIp(customIp);
        }
        
        logger.info("Removed custom IP: {}", ipOrNetwork);
    }

    /**
     * Clear all custom IPs
     */
    public void clearCustomIps() {
        customAllowedIps.clear();
        
        // Rebuild allowed networks
        allowedNetworks.clear();
        initializeDefaultNetworks();
        loadAwsIpRanges();
        
        logger.info("Cleared all custom IPs");
    }

    /**
     * Enable/disable AWS IP ranges
     */
    public void setUseAwsIpRanges(boolean useAwsIpRanges) {
        this.useAwsIpRanges = useAwsIpRanges;
        
        if (useAwsIpRanges) {
            loadAwsIpRanges();
        } else {
            // Remove AWS networks
            allowedNetworks.removeIf(network -> {
                for (String awsRange : RACHIO_IP_RANGES) {
                    if (network.toString().equals(awsRange)) {
                        return true;
                    }
                }
                return false;
            });
        }
        
        logger.info("AWS IP ranges {}", useAwsIpRanges ? "enabled" : "disabled");
    }

    /**
     * Calculate SHA-256 hash
     */
    public String calculateSha256(String data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(SHA256);
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        
        // Convert to hexadecimal
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }

    /**
     * Validate API key format (basic validation)
     */
    public boolean validateApiKeyFormat(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }
        
        // Rachio API keys are UUIDs (32 hex characters with hyphens)
        String pattern = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$";
        return apiKey.matches(pattern);
    }

    /**
     * Mask sensitive data for logging
     */
    public String maskSensitiveData(String data) {
        if (data == null || data.length() <= 8) {
            return "***";
        }
        
        // Show first 4 and last 4 characters
        return data.substring(0, 4) + "..." + data.substring(data.length() - 4);
    }

    /**
     * Get current IP filtering configuration
     */
    public List<String> getAllowedIps() {
        List<String> result = new ArrayList<>();
        
        // Add AWS ranges if enabled
        if (useAwsIpRanges) {
            result.addAll(awsIpRanges);
        }
        
        // Add custom IPs
        result.addAll(customAllowedIps);
        
        return result;
    }

    /**
     * Get number of allowed networks
     */
    public int getAllowedNetworkCount() {
        return allowedNetworks.size();
    }

    /**
     * Get last IP range update time
     */
    @Nullable
    public Instant getLastIpRangeUpdate() {
        return lastIpRangeUpdate;
    }

    /**
     * Check if security is properly configured
     */
    public boolean isConfigured() {
        return !allowedNetworks.isEmpty();
    }

    /**
     * Inner class for IP network representation
     */
    private static class IpNetwork {
        private final byte[] address;
        private final byte[] mask;
        private final int prefixLength;
        
        public IpNetwork(String cidr) throws IllegalArgumentException {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid CIDR format: " + cidr);
            }
            
            try {
                this.address = InetAddress.getByName(parts[0]).getAddress();
                this.prefixLength = Integer.parseInt(parts[1]);
                
                // Validate prefix length
                int maxPrefix = address.length * 8;
                if (prefixLength < 0 || prefixLength > maxPrefix) {
                    throw new IllegalArgumentException("Invalid prefix length: " + prefixLength);
                }
                
                // Create mask
                this.mask = createMask(address.length, prefixLength);
                
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid CIDR: " + cidr, e);
            }
        }
        
        private byte[] createMask(int addressLength, int prefixLength) {
            byte[] mask = new byte[addressLength];
            for (int i = 0; i < addressLength; i++) {
                int bits = Math.min(8, prefixLength - i * 8);
                if (bits > 0) {
                    mask[i] = (byte) (0xff << (8 - bits));
                }
            }
            return mask;
        }
        
        public boolean contains(InetAddress inetAddress) {
            byte[] testAddress = inetAddress.getAddress();
            
            if (testAddress.length != address.length) {
                return false; // IPv4 vs IPv6 mismatch
            }
            
            for (int i = 0; i < address.length; i++) {
                if ((testAddress[i] & mask[i]) != (address[i] & mask[i])) {
                    return false;
                }
            }
            
            return true;
        }
        
        @Override
        public String toString() {
            try {
                return InetAddress.getByAddress(address).getHostAddress() + "/" + prefixLength;
            } catch (UnknownHostException e) {
                return "Invalid";
            }
        }
    }
}
