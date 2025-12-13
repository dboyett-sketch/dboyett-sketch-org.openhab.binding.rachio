package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

/**
 * The {@link RachioSecurity} class handles security operations for webhook validation and IP filtering
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioSecurity {
    private final Logger logger = LoggerFactory.getLogger(RachioSecurity.class);

    private final Set<String> trustedIpRanges = ConcurrentHashMap.newKeySet();
    private final Set<String> allowedIps = ConcurrentHashMap.newKeySet();
    private final Set<String> blockedIps = ConcurrentHashMap.newKeySet();

    private final Map<String, Instant> failedAttempts = new ConcurrentHashMap<>();
    private final Map<String, Integer> attemptCounters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final Pattern CIDR_PATTERN = Pattern.compile(
            "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}" +
            "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])" +
            "(/([0-9]|[1-2][0-9]|3[0-2]))$");

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}" +
            "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");

    private static final long FAILED_ATTEMPT_TIMEOUT = Duration.ofMinutes(15).toMillis();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long RATE_LIMIT_WINDOW = Duration.ofMinutes(1).toMillis();
    private static final int MAX_REQUESTS_PER_WINDOW = 100;
    private static final String WEBHOOK_HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Default constructor
     */
    public RachioSecurity() {
        logger.debug("RachioSecurity initialized");
        loadDefaultTrustedRanges();
    }

    /**
     * Load default trusted IP ranges (AWS ranges would be loaded from external source)
     */
    private void loadDefaultTrustedRanges() {
        // Add localhost ranges
        addTrustedIpRange("127.0.0.0/8");
        addTrustedIpRange("::1/128");

        // Add private network ranges
        addTrustedIpRange("10.0.0.0/8");
        addTrustedIpRange("172.16.0.0/12");
        addTrustedIpRange("192.168.0.0/16");

        // Add link-local
        addTrustedIpRange("169.254.0.0/16");
        addTrustedIpRange("fe80::/10");

        logger.debug("Loaded {} default trusted IP ranges", trustedIpRanges.size());
    }

    // ==================== STATIC WEBHOOK VALIDATION METHODS ====================

    /**
     * STATIC METHOD: Validate webhook signature using HMAC-SHA256.
     * Used by RachioWebHookServletService. Uses constant-time comparison to prevent timing attacks.
     */
    public static boolean validateWebhookSignature(String payload, String signature, String secret) {
        Logger logger = LoggerFactory.getLogger(RachioSecurity.class);
        
        if (payload == null || payload.isEmpty() || signature == null || signature.isEmpty() || 
            secret == null || secret.isEmpty()) {
            logger.warn("Invalid parameters for webhook signature validation");
            return false;
        }

        try {
            // Decode the signature from hex
            byte[] receivedSignature = hexStringToByteArray(signature);
            if (receivedSignature == null || receivedSignature.length != 32) { // SHA-256 is 32 bytes
                logger.warn("Invalid signature format or length");
                return false;
            }

            // Compute expected signature
            byte[] expectedSignature = computeHmacSha256(payload, secret);
            if (expectedSignature == null) {
                logger.error("Failed to compute HMAC-SHA256");
                return false;
            }

            // Constant-time comparison to prevent timing attacks
            boolean isValid = constantTimeEquals(receivedSignature, expectedSignature);

            if (isValid) {
                logger.debug("Webhook signature validated successfully");
            } else {
                logger.warn("Webhook signature validation failed");
            }

            return isValid;

        } catch (Exception e) {
            logger.error("Error validating webhook signature", e);
            return false;
        }
    }

    /**
     * STATIC HELPER: Compute HMAC-SHA256 signature
     */
    private static byte @Nullable [] computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(WEBHOOK_HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), 
                    WEBHOOK_HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            LoggerFactory.getLogger(RachioSecurity.class).error("HMAC-SHA256 algorithm not available", e);
            return null;
        } catch (InvalidKeyException e) {
            LoggerFactory.getLogger(RachioSecurity.class).error("Invalid key for HMAC-SHA256", e);
            return null;
        } catch (Exception e) {
            LoggerFactory.getLogger(RachioSecurity.class).error("Error computing HMAC-SHA256", e);
            return null;
        }
    }

    /**
     * STATIC HELPER: Constant-time byte array comparison to prevent timing attacks
     */
    private static boolean constantTimeEquals(byte @Nullable [] a, byte @Nullable [] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    /**
     * STATIC HELPER: Convert hex string to byte array
     */
    private static byte @Nullable [] hexStringToByteArray(String hex) {
        try {
            int len = hex.length();
            if (len % 2 != 0) {
                return null;
            }

            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                     + Character.digit(hex.charAt(i + 1), 16));
            }
            return data;
        } catch (Exception e) {
            LoggerFactory.getLogger(RachioSecurity.class).warn("Invalid hex string: {}", hex);
            return null;
        }
    }

    // ==================== INSTANCE-BASED METHODS (IP Filtering) ====================

    /**
     * Check if IP address is valid
     */
    public boolean isValidIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }

        // Check if it's a valid IP address format
        if (!IP_PATTERN.matcher(ipAddress).matches()) {
            // Check IPv6 (simplified check)
            if (ipAddress.contains(":")) {
                try {
                    InetAddress.getByName(ipAddress);
                    return true;
                } catch (UnknownHostException e) {
                    return false;
                }
            }
            return false;
        }

        return true;
    }

    /**
     * Check if IP is allowed (with rate limiting and blocking)
     */
    public boolean isAllowedIp(String ipAddress, @Nullable String forwardedFor) {
        if (!isValidIp(ipAddress)) {
            logger.warn("Invalid IP address format: {}", ipAddress);
            return false;
        }

        // Check if IP is blocked
        if (blockedIps.contains(ipAddress)) {
            logger.debug("IP {} is blocked", ipAddress);
            return false;
        }

        // Check rate limiting
        if (!checkRateLimit(ipAddress)) {
            logger.warn("Rate limit exceeded for IP: {}", ipAddress);
            blockIpTemporarily(ipAddress, Duration.ofMinutes(5));
            return false;
        }

        // Check failed attempts
        if (!checkFailedAttempts(ipAddress)) {
            logger.warn("Too many failed attempts from IP: {}", ipAddress);
            return false;
        }

        // Extract real IP from X-Forwarded-For header if present
        String actualIp = extractRealIp(ipAddress, forwardedFor);

        // Check if IP is in allowed list
        if (allowedIps.contains(actualIp)) {
            return true;
        }

        // Check if IP is in trusted ranges
        for (String cidr : trustedIpRanges) {
            if (isIpInCidrRange(actualIp, cidr)) {
                return true;
            }
        }

        logger.debug("IP {} not in allowed ranges", actualIp);
        return false;
    }

    /**
     * Extract real IP from X-Forwarded-For header
     */
    private String extractRealIp(String remoteIp, @Nullable String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isEmpty()) {
            return remoteIp;
        }

        // X-Forwarded-For can contain multiple IPs: client, proxy1, proxy2
        String[] ips = forwardedFor.split(",");

        // The client IP is the first one in the list
        String clientIp = ips[0].trim();

        if (isValidIp(clientIp)) {
            logger.debug("Extracted client IP {} from X-Forwarded-For: {}", clientIp, forwardedFor);
            return clientIp;
        }

        // Fall back to remote IP if extraction fails
        logger.debug("Failed to extract valid IP from X-Forwarded-For: {}, using remote IP: {}", 
                forwardedFor, remoteIp);
        return remoteIp;
    }

    /**
     * Check if IP is in CIDR range
     */
    public boolean isIpInCidrRange(String ipAddress, String cidr) {
        if (!isValidIp(ipAddress) || !isValidCidr(cidr)) {
            return false;
        }

        try {
            String[] parts = cidr.split("/");
            String network = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            // Convert IPs to byte arrays
            byte[] ipBytes = InetAddress.getByName(ipAddress).getAddress();
            byte[] networkBytes = InetAddress.getByName(network).getAddress();

            // Handle IPv4 and IPv6
            if (ipBytes.length != networkBytes.length) {
                return false;
            }

            // Calculate mask
            int mask = 0xffffffff;
            if (prefixLength < 32) {
                mask = mask << (32 - prefixLength);
            }

            // Compare network portions
            for (int i = 0; i < Math.ceil(prefixLength / 8.0); i++) {
                int ipByte = ipBytes[i] & 0xff;
                int networkByte = networkBytes[i] & 0xff;
                int maskByte = (mask >> (24 - i * 8)) & 0xff;

                if ((ipByte & maskByte) != (networkByte & maskByte)) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            logger.debug("Error checking CIDR range {} for IP {}", cidr, ipAddress, e);
            return false;
        }
    }

    /**
     * Check if CIDR notation is valid
     */
    public boolean isValidCidr(String cidr) {
        if (cidr == null || cidr.isEmpty()) {
            return false;
        }
        return CIDR_PATTERN.matcher(cidr).matches();
    }

    /**
     * Add trusted IP range (CIDR notation)
     */
    public boolean addTrustedIpRange(String cidr) {
        if (!isValidCidr(cidr)) {
            logger.warn("Invalid CIDR notation: {}", cidr);
            return false;
        }

        boolean added = trustedIpRanges.add(cidr);
        if (added) {
            logger.debug("Added trusted IP range: {}", cidr);
        }
        return added;
    }

    /**
     * Remove trusted IP range
     */
    public boolean removeTrustedIpRange(String cidr) {
        boolean removed = trustedIpRanges.remove(cidr);
        if (removed) {
            logger.debug("Removed trusted IP range: {}", cidr);
        }
        return removed;
    }

    /**
     * Add allowed IP
     */
    public boolean addAllowedIp(String ipAddress) {
        if (!isValidIp(ipAddress)) {
            logger.warn("Invalid IP address: {}", ipAddress);
            return false;
        }

        boolean added = allowedIps.add(ipAddress);
        if (added) {
            logger.debug("Added allowed IP: {}", ipAddress);
        }
        return added;
    }

    /**
     * Remove allowed IP
     */
    public boolean removeAllowedIp(String ipAddress) {
        boolean removed = allowedIps.remove(ipAddress);
        if (removed) {
            logger.debug("Removed allowed IP: {}", ipAddress);
        }
        return removed;
    }

    /**
     * Block IP temporarily
     */
    public void blockIpTemporarily(String ipAddress, Duration duration) {
        if (!isValidIp(ipAddress)) {
            return;
        }

        blockedIps.add(ipAddress);
        logger.warn("Blocked IP {} for {}", ipAddress, duration);

        // Schedule unblock
        scheduler.schedule(() -> {
            blockedIps.remove(ipAddress);
            logger.debug("Unblocked IP: {}", ipAddress);
        }, duration.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Check rate limiting for IP
     */
    private boolean checkRateLimit(String ipAddress) {
        String key = "rate:" + ipAddress;
        Instant now = Instant.now();

        synchronized (failedAttempts) {
            Instant lastAttempt = failedAttempts.get(key);

            if (lastAttempt != null) {
                long timeSinceLast = Duration.between(lastAttempt, now).toMillis();

                if (timeSinceLast < RATE_LIMIT_WINDOW) {
                    int count = attemptCounters.getOrDefault(key, 0);
                    if (count >= MAX_REQUESTS_PER_WINDOW) {
                        return false;
                    }
                    attemptCounters.put(key, count + 1);
                } else {
                    // Reset counter if outside window
                    attemptCounters.put(key, 1);
                    failedAttempts.put(key, now);
                }
            } else {
                // First request
                attemptCounters.put(key, 1);
                failedAttempts.put(key, now);
            }
        }
        return true;
    }

    /**
     * Check failed attempts for IP
     */
    private boolean checkFailedAttempts(String ipAddress) {
        String key = "failed:" + ipAddress;
        Instant now = Instant.now();

        synchronized (failedAttempts) {
            Instant lastFailed = failedAttempts.get(key);

            if (lastFailed != null) {
                long timeSinceLast = Duration.between(lastFailed, now).toMillis();

                if (timeSinceLast < FAILED_ATTEMPT_TIMEOUT) {
                    int count = attemptCounters.getOrDefault(key, 0);
                    if (count >= MAX_FAILED_ATTEMPTS) {
                        return false;
                    }
                    attemptCounters.put(key, count + 1);
                } else {
                    // Reset counter if outside timeout
                    attemptCounters.put(key, 1);
                    failedAttempts.put(key, now);
                }
            } else {
                // First failed attempt
                attemptCounters.put(key, 1);
                failedAttempts.put(key, now);
            }
        }
        return true;
    }

    /**
     * Clean up resources
     */
    public void dispose() {
        scheduler.shutdown();
        logger.debug("RachioSecurity disposed");
    }
}
