package com.kaif.apitestplatform.execution;

import com.kaif.apitestplatform.exception.UnsafeUrlException;
import com.kaif.apitestplatform.security.UrlGuard;
import org.apache.hc.client5.http.DnsResolver;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** Every address the HTTP client connects to goes through UrlGuard first. */
public class GuardedDnsResolver implements DnsResolver {

    private final UrlGuard urlGuard;

    public GuardedDnsResolver(UrlGuard urlGuard) {
        this.urlGuard = urlGuard;
    }

    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        try {
            return urlGuard.resolvePublic(host);
        } catch (UnsafeUrlException e) {
            throw new UnknownHostException("Blocked by SSRF guard: " + e.getMessage());
        }
    }

    @Override
    public String resolveCanonicalHostname(String host) {
        return host;   // no reverse lookups
    }
}