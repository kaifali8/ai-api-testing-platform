package com.kaif.apitestplatform.security;

import com.kaif.apitestplatform.exception.UnsafeUrlException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.net.URISyntaxException;

@Slf4j
@Component
public class UrlGuard {
    // NAT64 prefix 64:ff9b::/96 can smuggle IPv4 addresses through IPv6
    private static final byte[] NAT64_PREFIX =
            {0x00, 0x64, (byte) 0xFF, (byte) 0x9B, 0, 0, 0, 0, 0, 0, 0, 0};

    private final boolean allowPrivateNetwork;

    public UrlGuard(@Value("${app.ssrf.allow-private-network:false}") boolean allowPrivateNetwork) {
        this.allowPrivateNetwork = allowPrivateNetwork;
        if (allowPrivateNetwork) {
            log.warn("SSRF protection is RELAXED: private and internal targets are allowed. "
                    + "Use this for local development only.");
        }
    }

    /** Validates the URL and returns it parsed. Throws UnsafeUrlException if unsafe. */
    public URI validate(String url) {
        if (url == null || url.isBlank()) {
            throw new UnsafeUrlException("URL is empty");
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new UnsafeUrlException("URL is malformed");
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new UnsafeUrlException("Only http:// and https:// URLs are allowed");
        }
        if (uri.getUserInfo() != null) {
            throw new UnsafeUrlException("URL must not contain credentials");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new UnsafeUrlException("URL has no valid host");
        }
        int port = uri.getPort();
        if (port != -1 && (port < 1 || port > 65535)) {
            throw new UnsafeUrlException("URL has an invalid port");
        }

        resolvePublic(host);
        return uri;
    }

    /** Resolves a host and rejects it if ANY resulting address is internal. */
    public InetAddress[] resolvePublic(String host) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new UnsafeUrlException("Host cannot be resolved");
        }
        if (!allowPrivateNetwork) {
            for (InetAddress a : addresses) {
                if (isBlocked(a)) {
                    // Deliberately vague: don't tell an attacker which address we saw
                    throw new UnsafeUrlException("Target is a private or internal address");
                }
            }
        }
        return addresses;
    }

    private static boolean isBlocked(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                || a.isSiteLocalAddress() || a.isMulticastAddress()) {
            return true;   // 127/8, ::1, 169.254/16 (cloud metadata), 10/8, 172.16/12, 192.168/16, ...
        }
        byte[] b = a.getAddress();
        if (b.length == 4) {
            int b0 = b[0] & 0xFF, b1 = b[1] & 0xFF, b2 = b[2] & 0xFF;
            if (b0 == 0) return true;                                // 0.0.0.0/8
            if (b0 == 100 && b1 >= 64 && b1 <= 127) return true;     // 100.64/10 carrier-grade NAT
            if (b0 == 192 && b1 == 0 && b2 == 0) return true;        // 192.0.0.0/24
            if (b0 == 198 && (b1 == 18 || b1 == 19)) return true;    // 198.18/15 benchmarking
            if (b0 >= 240) return true;                              // reserved + broadcast
        } else if (b.length == 16) {
            if ((b[0] & 0xFE) == 0xFC) return true;                  // fc00::/7 unique-local
            if (Arrays.equals(Arrays.copyOf(b, 12), NAT64_PREFIX)) return true;
        }
        return false;
    }
}
