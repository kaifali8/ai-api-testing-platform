package com.kaif.apitestplatform.execution;

import com.kaif.apitestplatform.security.UrlGuard;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class HttpTestClient {

    private final CloseableHttpClient client;
    private final UrlGuard urlGuard;
    private final int maxBytes;
    private final long maxTotalMs;

    public HttpTestClient(CloseableHttpClient client, UrlGuard urlGuard,
                          @Value("${app.execution.max-response-bytes:1048576}") int maxBytes,
                          @Value("${app.execution.max-total-time-ms:30000}") long maxTotalMs) {
        this.client = client;
        this.urlGuard = urlGuard;
        this.maxBytes = maxBytes;
        this.maxTotalMs = maxTotalMs;
    }

    public HttpResponseData send(BuiltRequest req) throws IOException {
        // Re-check at use time. The DNS resolver guards the actual connection as well.
        URI uri = urlGuard.validate(req.uri().toString());

        HttpUriRequestBase request = new HttpUriRequestBase(req.method(), uri);
        req.headers().forEach(request::setHeader);
        if (req.body() != null) {
            request.setEntity(new StringEntity(req.body(), StandardCharsets.UTF_8));
        }

        long start = System.nanoTime();
        ClassicHttpResponse response = client.executeOpen(HttpHost.create(uri), request, null);
        try {
            Map<String, String> headers = new LinkedHashMap<>();
            for (Header h : response.getHeaders()) {
                headers.merge(h.getName().toLowerCase(Locale.ROOT), h.getValue(), (a, b) -> a + ", " + b);
            }

            Body body = readBody(response.getEntity(), request, start);
            long timeMs = (System.nanoTime() - start) / 1_000_000;
            return new HttpResponseData(response.getCode(), headers, body.text(), body.truncated(), timeMs);
        } finally {
            try {
                response.close();
            } catch (Exception ignored) {
                // closing a connection we already aborted may complain; nothing to do
            }
        }
    }

    private record Body(String text, boolean truncated) {}

    private Body readBody(HttpEntity entity, HttpUriRequestBase request, long startNanos) throws IOException {
        if (entity == null) return new Body("", false);

        InputStream in = entity.getContent();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            boolean truncated = false;
            int n;
            while ((n = in.read(buf)) != -1) {
                if (out.size() + n > maxBytes) {
                    out.write(buf, 0, maxBytes - out.size());
                    truncated = true;
                    request.abort();     // stop the transfer; closing normally would try to drain it
                    break;
                }
                out.write(buf, 0, n);
                if ((System.nanoTime() - startNanos) / 1_000_000 > maxTotalMs) {
                    request.abort();
                    throw new IOException("Response took longer than " + maxTotalMs + " ms");
                }
            }
            return new Body(out.toString(StandardCharsets.UTF_8), truncated);
        } finally {
            try {
                in.close();
            } catch (Exception ignored) {
                // see above
            }
        }
    }
}