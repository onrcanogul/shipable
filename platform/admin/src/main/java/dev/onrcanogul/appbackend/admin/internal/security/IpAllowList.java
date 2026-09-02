package dev.onrcanogul.appbackend.admin.internal.security;

import java.util.List;

/**
 * Matches a caller address against an allowlist of exact addresses and CIDR ranges.
 *
 * <p>Supports IPv4 CIDR ({@code 203.0.113.0/24}) and exact matches for both IPv4 and IPv6.
 * An IPv6 address with a prefix length is treated as an exact match rather than silently
 * matching too much — quietly widening an allowlist is the wrong way to be wrong.
 *
 * <p><b>Only as trustworthy as the proxy in front of you.</b> The address comes from
 * {@code X-Forwarded-For}, which a client can forge if it can reach the app directly. In
 * this template Caddy is the only route in and sets it, so it holds. Expose the app port
 * and this check becomes decoration.
 */
public final class IpAllowList {

    private IpAllowList() {
    }

    /** An empty list allows everything: the API key is then the only lock. */
    public static boolean permits(List<String> allowed, String callerIp) {
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }
        if (callerIp == null || callerIp.isBlank()) {
            return false;
        }
        String caller = normalise(callerIp);
        return allowed.stream().anyMatch(entry -> matches(entry.trim(), caller));
    }

    private static boolean matches(String entry, String caller) {
        if (entry.isEmpty()) {
            return false;
        }
        int slash = entry.indexOf('/');
        if (slash < 0) {
            return normalise(entry).equalsIgnoreCase(caller);
        }

        String network = entry.substring(0, slash);
        if (!isIpv4(network) || !isIpv4(caller)) {
            // IPv6 ranges are not parsed here. Fall back to an exact comparison rather
            // than guessing at a prefix and allowing more than intended.
            return normalise(network).equalsIgnoreCase(caller);
        }

        int prefixLength;
        try {
            prefixLength = Integer.parseInt(entry.substring(slash + 1));
        } catch (NumberFormatException e) {
            return false;
        }
        if (prefixLength < 0 || prefixLength > 32) {
            return false;
        }
        if (prefixLength == 0) {
            return true;
        }

        long mask = 0xFFFFFFFFL << (32 - prefixLength);
        return (toLong(caller) & mask) == (toLong(network) & mask);
    }

    /** IPv4-mapped IPv6 loopback and the like, so "::1" and "127.0.0.1" compare sensibly. */
    private static String normalise(String ip) {
        String value = ip.trim();
        if (value.startsWith("::ffff:")) {
            return value.substring("::ffff:".length());
        }
        return value;
    }

    private static boolean isIpv4(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            try {
                int octet = Integer.parseInt(part);
                if (octet < 0 || octet > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    private static long toLong(String ipv4) {
        String[] parts = ipv4.split("\\.");
        long value = 0;
        for (String part : parts) {
            value = (value << 8) | Integer.parseInt(part);
        }
        return value;
    }
}
