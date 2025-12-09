package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@link RachioSecurity} class handles security features for the Rachio binding
 * including HMAC validation, IP filtering, and constant-time comparison.
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioSecurity {

    private final Logger logger = LoggerFactory.getLogger(RachioSecurity.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HEX_CHARS = "0123456789abcdef";
    
    // IP filtering
    private final Set<String> allowedIpRanges = ConcurrentHashMap.newKeySet();
    private final Set<String> allowedIps = ConcurrentHashMap.newKeySet();
    private boolean useAwsRanges = false;
    
    // Nonce tracking for replay protection (optional)
    private final Map<String, Long> usedNonces = new ConcurrentHashMap<>();
    private static final long NONCE_TIMEOUT = 5 * 60 * 1000; // 5 minutes
    
    // Thread-safe SecureRandom
    private final SecureRandom secureRandom = new SecureRandom();

    public RachioSecurity() {
        logger.debug("RachioSecurity initialized with HMAC-SHA256 and IP filtering");
    }

    // ===== HMAC Validation =====
    
    /**
     * Validate HMAC signature from Rachio webhook
     * Uses constant-time comparison to prevent timing attacks
     */
    public boolean validateHmacSignature(@Nullable String payload, @Nullable String receivedSignature, 
                                         @Nullable String secret) {
        if (payload == null || receivedSignature == null || secret == null || 
            payload.isEmpty() || receivedSignature.isEmpty() || secret.isEmpty()) {
            logger.warn("HMAC validation failed: missing parameters");
            return false;
        }
        
        try {
            String calculatedSignature = calculateHmac(payload, secret);
            boolean valid = constantTimeEquals(calculatedSignature, receivedSignature);
            
            if (valid) {
                logger.debug("HMAC validation successful");
            } else {
                logger.warn("HMAC validation failed: signature mismatch");
                logger.debug("Calculated: {}, Received: {}", calculatedSignature, receivedSignature);
            }
            
            return valid;
            
        } catch (Exception e) {
            logger.error("HMAC validation error: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Calculate HMAC-SHA256 signature
     */
    public String calculateHmac(String payload, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(secretKeySpec);
        
        byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(rawHmac);
    }
    
    /**
     * Constant-time string comparison to prevent timing attacks
     */
    public boolean constantTimeEquals(String a, String b) {
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
    
    // ===== IP Filtering =====
    
    /**
     * Check if an IP address is allowed based on configured filters
     * Supports CIDR ranges, exact IPs, and AWS ranges
     */
    public boolean isIpAllowed(@Nullable String ipAddress, @Nullable RachioBridgeHandler bridgeHandler) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            logger.warn("IP validation failed: no IP address provided");
            return false;
        }
        
        // Extract IP from X-Forwarded-For header if behind proxy
        String actualIp = extractActualIp(ipAddress);
        logger.debug("Checking IP access for: {} (original: {})", actualIp, ipAddress);
        
        // Check exact IP matches
        if (allowedIps.contains(actualIp)) {
            logger.debug("IP allowed (exact match): {}", actualIp);
            return true;
        }
        
        // Check CIDR ranges
        for (String cidr : allowedIpRanges) {
            if (isIpInCidrRange(actualIp, cidr)) {
                logger.debug("IP allowed (CIDR range {}): {}", cidr, actualIp);
                return true;
            }
        }
        
        // Check AWS ranges if enabled
        if (useAwsRanges && isAwsIp(actualIp)) {
            logger.debug("IP allowed (AWS range): {}", actualIp);
            return true;
        }
        
        logger.warn("IP denied: {}", actualIp);
        return false;
    }
    
    /**
     * Extract actual IP from X-Forwarded-For header (proxy support)
     */
    public String extractActualIp(String ipHeader) {
        if (ipHeader == null || ipHeader.isEmpty()) {
            return "";
        }
        
        // Handle X-Forwarded-For: client, proxy1, proxy2
        String[] ips = ipHeader.split(",");
        if (ips.length > 0) {
            // First IP in chain is the client
            return ips[0].trim();
        }
        
        return ipHeader.trim();
    }
    
    /**
     * Check if IP is in CIDR range
     */
    public boolean isIpInCidrRange(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }
            
            String network = parts[0];
            int prefix;
            try {
                prefix = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return false;
            }
            
            if (prefix < 0 || prefix > 32) {
                return false;
            }
            
            long ipLong = ipToLong(ip);
            long networkLong = ipToLong(network);
            long mask = prefix == 0 ? 0 : 0xFFFFFFFFL << (32 - prefix);
            
            return (ipLong & mask) == (networkLong & mask);
            
        } catch (Exception e) {
            logger.debug("Error checking CIDR range {} for IP {}: {}", cidr, ip, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if IP is in AWS range (simplified check)
     */
    public boolean isAwsIp(String ip) {
        // Simplified AWS IP range check
        // In production, you would load the actual AWS IP ranges from their JSON
        try {
            long ipLong = ipToLong(ip);
            
            // Common AWS ranges (simplified)
            // 3.0.0.0/8 - 3.255.255.255
            // 52.0.0.0/8 - 52.255.255.255
            // 54.0.0.0/8 - 54.255.255.255
            // etc.
            
            long awsRange1Start = ipToLong("3.0.0.0");
            long awsRange1End = ipToLong("3.255.255.255");
            
            long awsRange2Start = ipToLong("52.0.0.0");
            long awsRange2End = ipToLong("52.255.255.255");
            
            long awsRange3Start = ipToLong("54.0.0.0");
            long awsRange3End = ipToLong("54.255.255.255");
            
            return (ipLong >= awsRange1Start && ipLong <= awsRange1End) ||
                   (ipLong >= awsRange2Start && ipLong <= awsRange2End) ||
                   (ipLong >= awsRange3Start && ipLong <= awsRange3End);
            
        } catch (Exception e) {
            logger.debug("Error checking AWS IP: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Convert IP address to long
     */
    private long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        if (octets.length != 4) {
            throw new IllegalArgumentException("Invalid IP address: " + ip);
        }
        
        long result = 0;
        for (int i = 0; i < 4; i++) {
            int octet = Integer.parseInt(octets[i]);
            if (octet < 0 || octet > 255) {
                throw new IllegalArgumentException("Invalid IP octet: " + octet);
            }
            result |= (long) octet << (24 - (8 * i));
        }
        return result;
    }
    
    // ===== Configuration Methods =====
    
    public void addAllowedIp(String ip) {
        if (ip != null && !ip.isEmpty()) {
            allowedIps.add(ip.trim());
            logger.debug("Added allowed IP: {}", ip);
        }
    }
    
    public void addAllowedIpRange(String cidr) {
        if (cidr != null && !cidr.isEmpty()) {
            allowedIpRanges.add(cidr.trim());
            logger.debug("Added allowed IP range: {}", cidr);
        }
    }
    
    public void setUseAwsRanges(boolean useAwsRanges) {
        this.useAwsRanges = useAwsRanges;
        logger.debug("AWS IP range checking: {}", useAwsRanges);
    }
    
    public void clearIpFilters() {
        allowedIps.clear();
        allowedIpRanges.clear();
        logger.debug("Cleared all IP filters");
    }
    
    public Set<String> getAllowedIps() {
        return new HashSet<>(allowedIps);
    }
    
    public Set<String> getAllowedIpRanges() {
        return new HashSet<>(allowedIpRanges);
    }
    
    // ===== Utility Methods =====
    
    /**
     * Generate a secure random nonce for replay protection
     */
    public String generateNonce() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return bytesToHex(bytes);
    }
    
    /**
     * Check and record nonce to prevent replay attacks
     */
    public boolean validateNonce(String nonce) {
        long now = System.currentTimeMillis();
        
        // Clean up old nonces
        usedNonces.entrySet().removeIf(entry -> now - entry.getValue() > NONCE_TIMEOUT);
        
        // Check if nonce was already used
        if (usedNonces.containsKey(nonce)) {
            logger.warn("Replay attack detected: nonce {} already used", nonce);
            return false;
        }
        
        // Record nonce
        usedNonces.put(nonce, now);
        return true;
    }
    
    /**
     * Calculate SHA-256 hash (for API key validation, etc.)
     */
    public String calculateSha256(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }
    
    /**
     * Convert byte array to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(HEX_CHARS.charAt((b & 0xF0) >> 4));
            hex.append(HEX_CHARS.charAt(b & 0x0F));
        }
        return hex.toString();
    }
    
    /**
     * Validate API key format (basic validation)
     */
    public boolean isValidApiKeyFormat(@Nullable String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }
        
        // Rachio API keys are UUIDs
        return apiKey.matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");
    }
}
