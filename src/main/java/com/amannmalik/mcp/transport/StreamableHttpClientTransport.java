package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.api.Protocol;
import com.amannmalik.mcp.api.Transport;
import com.amannmalik.mcp.util.*;
import jakarta.json.*;

import javax.net.ssl.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/// - [Transports](specification/2025-06-18/basic/transports.mdx)
public final class StreamableHttpClientTransport implements Transport {
    private final HttpClient client;
    private final URI endpoint;
    private final BlockingQueue<JsonObject> incoming = new LinkedBlockingQueue<>();
    private final Set<SseReader> streams = ConcurrentHashMap.newKeySet();
    private final AtomicReference<String> sessionId = new AtomicReference<>();
    private final AtomicReference<String> protocolVersion = new AtomicReference<>(Protocol.LATEST_VERSION);
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final Duration defaultReceiveTimeout;
    private final String defaultOriginHeader;

    public StreamableHttpClientTransport(URI endpoint) {
        this(endpoint, Duration.ofSeconds(10), "http://127.0.0.1");
    }

    public StreamableHttpClientTransport(URI endpoint, Duration defaultReceiveTimeout, String defaultOriginHeader) {
        this(endpoint, defaultReceiveTimeout, defaultOriginHeader, defaultClient(endpoint));
    }

    public StreamableHttpClientTransport(URI endpoint,
                                         Duration defaultReceiveTimeout,
                                         String defaultOriginHeader,
                                         Path trustStore,
                                         char[] trustStorePassword,
                                         Path keyStore,
                                         char[] keyStorePassword,
                                         boolean validateCertificates,
                                         Set<String> pinnedFingerprints) {
        this(endpoint,
                defaultReceiveTimeout,
                defaultOriginHeader,
                buildClient(endpoint, trustStore, trustStorePassword, keyStore, keyStorePassword, validateCertificates, pinnedFingerprints));
    }

    private StreamableHttpClientTransport(URI endpoint,
                                          Duration defaultReceiveTimeout,
                                          String defaultOriginHeader,
                                          HttpClient client) {
        String scheme = endpoint.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
            throw new IllegalArgumentException("Endpoint must use http or https");
        this.endpoint = endpoint;
        if (defaultReceiveTimeout == null || defaultReceiveTimeout.isNegative() || defaultReceiveTimeout.isZero())
            throw new IllegalArgumentException("Default receive timeout must be positive");
        if (defaultOriginHeader == null || defaultOriginHeader.isBlank())
            throw new IllegalArgumentException("Default origin header is required");
        this.defaultReceiveTimeout = defaultReceiveTimeout;
        this.defaultOriginHeader = defaultOriginHeader;
        this.client = client;
    }

    private static HttpClient defaultClient(URI endpoint) {
        return buildClient(endpoint, null, null, null, null, true, Set.of());
    }

    private static HttpClient buildClient(URI endpoint,
                                          Path trustStore,
                                          char[] trustStorePassword,
                                          Path keyStore,
                                          char[] keyStorePassword,
                                          boolean validateCertificates,
                                          Set<String> pinnedFingerprints) {
        try {
            KeyManager[] kms = loadKeyManagers(keyStore, keyStorePassword);
            TrustManager[] tms = validateCertificates
                    ? loadTrustManagers(trustStore, trustStorePassword, pinnedFingerprints)
                    : new TrustManager[]{new InsecureTrustManager()};
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kms, tms, null);
            SSLParameters params = new SSLParameters();
            params.setServerNames(List.of(new SNIHostName(endpoint.getHost())));
            return HttpClient.newBuilder()
                    .sslContext(ctx)
                    .sslParameters(params)
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalArgumentException("TLS configuration failed", e);
        }
    }

    private static KeyManager[] loadKeyManagers(Path keyStore, char[] password) throws GeneralSecurityException, IOException {
        if (keyStore == null) return null;
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream in = Files.newInputStream(keyStore)) {
            ks.load(in, password);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password);
        return kmf.getKeyManagers();
    }

    private static TrustManager[] loadTrustManagers(Path trustStore,
                                                    char[] password,
                                                    Set<String> pins) throws GeneralSecurityException, IOException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        if (trustStore == null) {
            tmf.init((KeyStore) null);
        } else {
            KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream in = Files.newInputStream(trustStore)) {
                ts.load(in, password);
            }
            tmf.init(ts);
        }
        TrustManager[] tms = tmf.getTrustManagers();
        if (pins.isEmpty()) return tms;
        for (int i = 0; i < tms.length; i++) {
            if (tms[i] instanceof X509TrustManager x509) {
                tms[i] = new PinnedTrustManager(x509, pins);
            }
        }
        return tms;
    }

    public void setProtocolVersion(String version) {
        protocolVersion.set(ValidationUtil.requireNonBlank(version));
    }

    public void setAuthorization(String token) {
        authorization.set(ValidationUtil.requireNonBlank(token));
    }

    public void clearAuthorization() {
        authorization.set(null);
    }

    public void listen() throws IOException {
        var request = builder()
                .header("Accept", "text/event-stream")
                .GET()
                .build();
        var response = exchange(request);
        AuthorizationUtil.checkUnauthorized(response);
        int status = response.statusCode();
        String ct = response.headers().firstValue("Content-Type").orElse("");
        if (status != 200 || !ct.startsWith("text/event-stream")) {
            response.body().close();
            throw new IOException("Unexpected response: " + status + " " + ct);
        }
        startReader(response.body());
    }

    @Override
    public void send(JsonObject message) throws IOException {
        var request = builder()
                .header("Accept", "application/json, text/event-stream")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(message.toString()))
                .build();
        var response = exchange(request);
        AuthorizationUtil.checkUnauthorized(response);
        int status = response.statusCode();
        String ct = response.headers().firstValue("Content-Type").orElse("");
        if (status == 202) {
            response.body().close();
            return;
        }
        if (ct.startsWith("application/json")) {
            try (JsonReader reader = Json.createReader(response.body())) {
                incoming.add(reader.readObject());
            }
            return;
        }
        if (ct.startsWith("text/event-stream")) {
            startReader(response.body());
            return;
        }
        response.body().close();
        throw new IOException("Unexpected response: " + status + " " + ct);
    }

    @Override
    public JsonObject receive() throws IOException {
        return receive(defaultReceiveTimeout);
    }

    @Override
    public JsonObject receive(Duration timeoutMillis) throws IOException {
        try {
            JsonObject result = incoming.poll(timeoutMillis.toMillis(), TimeUnit.MILLISECONDS);
            if (result == null) {
                throw new IOException("Timeout after " + timeoutMillis.toMillis() + "ms waiting for message");
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for message", e);
        }
    }

    @Override
    public void close() throws IOException {
        if (sessionId.get() != null) {
            var request = builder().DELETE().build();
            try {
                client.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        }
        streams.forEach(SseReader::close);
        streams.clear();
    }

    private void startReader(InputStream body) {
        SseReader reader = new SseReader(body, incoming, streams);
        streams.add(reader);
        Thread t = new Thread(reader);
        t.setDaemon(true);
        t.start();
    }

    private HttpRequest.Builder builder() {
        var b = HttpRequest.newBuilder(endpoint)
                .header("Origin", defaultOriginHeader)
                .header(TransportHeaders.PROTOCOL_VERSION, protocolVersion.get());
        Optional.ofNullable(authorization.get())
                .ifPresent(t -> b.header(TransportHeaders.AUTHORIZATION, "Bearer " + t));
        Optional.ofNullable(sessionId.get())
                .ifPresent(id -> b.header(TransportHeaders.SESSION_ID, id));
        return b;
    }

    private HttpResponse<InputStream> exchange(HttpRequest request) throws IOException {
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            sessionId.updateAndGet(old -> response.headers()
                    .firstValue(TransportHeaders.SESSION_ID)
                    .orElse(old));
            protocolVersion.updateAndGet(old -> response.headers()
                    .firstValue(TransportHeaders.PROTOCOL_VERSION)
                    .orElse(old));
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (SSLException e) {
            throw TlsErrors.ioException(e);
        }
    }

    private static final class InsecureTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    private record PinnedTrustManager(X509TrustManager delegate, Set<String> pins) implements X509TrustManager {

        @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                delegate.checkClientTrusted(chain, authType);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                delegate.checkServerTrusted(chain, authType);
                if (pins.isEmpty()) return;
                String fp = Certificates.fingerprint(chain[0]);
                if (!pins.contains(fp)) throw new CertificateException("Certificate pinning failure");
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return delegate.getAcceptedIssuers();
            }
        }

    static class SseReader implements Runnable {
        private final InputStream input;
        private final BlockingQueue<JsonObject> queue;
        private final Set<SseReader> container;
        private final EventBuffer buffer = new EventBuffer();
        private volatile boolean closed;
        private String lastEventId;

        SseReader(InputStream input, BlockingQueue<JsonObject> queue, Set<SseReader> container) {
            this.input = input;
            this.queue = queue;
            this.container = container;
        }

        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while (!closed && (line = br.readLine()) != null) {
                    if (line.isEmpty()) {
                        buffer.flush();
                        continue;
                    }
                    int idx = line.indexOf(':');
                    if (idx < 0) continue;
                    buffer.field(line.substring(0, idx), line.substring(idx + 1).trim());
                }
                buffer.flush();
            } catch (IOException ignore) {
            } finally {
                if (container != null) container.remove(this);
                close();
            }
        }

        private void dispatch(String payload, String eventId) {
            try (JsonReader jr = Json.createReader(new StringReader(payload))) {
                queue.add(jr.readObject());
            } catch (Exception ignore) {
            }
            if (eventId != null) lastEventId = eventId;
        }

        void close() {
            closed = true;
            try {
                input.close();
            } catch (IOException ignore) {
            }
        }

        String lastEventId() {
            return lastEventId;
        }

        private final class EventBuffer {
            private final StringBuilder data = new StringBuilder();
            private String eventId;

            void field(String name, String value) {
                switch (name) {
                    case "id" -> eventId = value;
                    case "data" -> {
                        if (!data.isEmpty()) data.append('\n');
                        data.append(value);
                    }
                }
            }

            void flush() {
                if (data.isEmpty()) return;
                dispatch(data.toString(), eventId);
                data.setLength(0);
                eventId = null;
            }
        }
    }
}
