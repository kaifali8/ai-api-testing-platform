package com.kaif.apitestplatform.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaif.apitestplatform.testgen.RequestData;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure logic (no network, no database). Every problem is reported as an
 * IllegalArgumentException with a message that is safe to show to the user.
 */
@Component
public class TestRequestBuilder {

    private static final Set<String> METHODS =
            Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");
    private static final Set<String> BODY_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Set<String> FORBIDDEN_HEADERS = Set.of(
            "host", "content-length", "transfer-encoding", "connection",
            "upgrade", "expect", "te", "trailer", "proxy-authorization");
    private static final Pattern HEADER_NAME = Pattern.compile("^[A-Za-z0-9-]{1,100}$");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^/{}]+)}");

    private final ObjectMapper mapper = new ObjectMapper();

    public BuiltRequest build(String baseUrl, String httpMethod, String pathTemplate,
                              RequestData data, boolean includeAuth, ResolvedCredential credential) {

        String method = httpMethod == null ? "" : httpMethod.toUpperCase(Locale.ROOT);
        if (!METHODS.contains(method)) {
            throw new IllegalArgumentException("HTTP method '" + httpMethod + "' is not supported");
        }

        // 1. URL = base + path (placeholders filled and encoded) + query
        String path = fillPath(pathTemplate, nz(data.getPathParams()));
        String query = buildQuery(nz(data.getQueryParams()));
        String base = baseUrl.replaceAll("/+$", "");
        String full = base + (path.startsWith("/") ? path : "/" + path) + query;

        URI uri;
        URI baseUri;
        try {
            uri = new URI(full);
            baseUri = new URI(base);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Resulting URL is not valid: " + e.getReason());
        }
        // Whatever the test data contains, the request must stay on the project's own host
        if (!Objects.equals(uri.getRawAuthority(), baseUri.getRawAuthority())) {
            throw new IllegalArgumentException("Resulting URL points to a different host than the project base URL");
        }

        // 2. Headers (case-insensitive, de-duplicated)
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, Object> e : nz(data.getHeaders()).entrySet()) {
            String name = e.getKey();
            if (name == null || !HEADER_NAME.matcher(name).matches()) {
                throw new IllegalArgumentException("Invalid header name: '" + name + "'");
            }
            if (FORBIDDEN_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Header '" + name + "' is not allowed");
            }
            if (e.getValue() == null) continue;
            String value = String.valueOf(e.getValue());
            if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {   // header injection
                throw new IllegalArgumentException("Header '" + name + "' contains a line break");
            }
            headers.put(name, value);
        }
        if (!headers.containsKey("Accept")) {
            headers.put("Accept", "application/json");
        }

        // 3. Credentials (set last, so they win over any same-named header)
        if (includeAuth && credential != null) {
            headers.put(credential.name(), credential.value());
        }

        // 4. Body
        String body = null;
        if (data.getBody() != null && BODY_METHODS.contains(method)) {
            try {
                body = mapper.writeValueAsString(data.getBody());
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Request body could not be serialized");
            }
            if (!headers.containsKey("Content-Type")) {
                headers.put("Content-Type", "application/json");
            }
        }

        // 5. Masked copy for storage
        Map<String, String> masked = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.forEach((k, v) -> masked.put(k,
                SecretMasker.isSensitiveHeader(k)
                        || (credential != null && k.equalsIgnoreCase(credential.name()))
                        ? "***" : v));

        return new BuiltRequest(method, uri, headers, masked, body);
    }

    private String fillPath(String template, Map<String, Object> params) {
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            Object v = params.get(m.group(1));
            if (v == null) {
                throw new IllegalArgumentException("Missing value for path parameter '" + m.group(1) + "'");
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(encodeSegment(String.valueOf(v))));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // A path value must never be able to add or remove path segments
    private String encodeSegment(String v) {
        if (v.equals(".") || v.equals("..")) {
            return v.replace(".", "%2E");
        }
        return URLEncoder.encode(v, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String buildQuery(Map<String, Object> params) {
        List<String> parts = new ArrayList<>();
        params.forEach((k, v) -> {
            if (v == null) return;
            if (v instanceof Collection<?> items) {
                items.forEach(item -> {
                    if (item != null) parts.add(enc(k) + "=" + enc(String.valueOf(item)));
                });
            } else {
                parts.add(enc(k) + "=" + enc(String.valueOf(v)));
            }
        });
        return parts.isEmpty() ? "" : "?" + String.join("&", parts);
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static <K, V> Map<K, V> nz(Map<K, V> m) {
        return m == null ? Map.of() : m;
    }
}