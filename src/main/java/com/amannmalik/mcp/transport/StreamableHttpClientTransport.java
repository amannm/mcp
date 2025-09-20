package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.api.Protocol;
import com.amannmalik.mcp.api.Transport;
import com.amannmalik.mcp.util.*;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.net.URI;
import java.net.http.*;
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
    private static final Logger LOG = PlatformLog.get(StreamableHttpClientTransport.class);

    private final HttpClient client;
    private final URI endpoint;
    private final BlockingQueue<JsonObject> incoming = new LinkedBlockingQueue<>();
    private final Set<SseReader> streams = ConcurrentHashMap.newKeySet();
    private final AtomicReference<String> sessionId = new AtomicReference<>();
    private final AtomicReference<String> protocolVersion = new AtomicReference<>(Protocol.LATEST_VERSION);
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final Duration defaultReceiveTimeout;
    private final String defaultOriginHeader;

    public StreamableHttpClientTransport(URI endpoint,
                                         Duration defaultReceiveTimeout,
                                         String defaultOriginHeader) {
        this(endpoint, defaultReceiveTimeout, defaultOriginHeader, defaultClient(endpoint, true));
    }

    public StreamableHttpClientTransport(URI endpoint,
                                         Duration defaultReceiveTimeout,
                                         String defaultOriginHeader,
                                         Path trustStore,
                                         char[] trustStorePassword,
                                         Path keyStore,
                                         char[] keyStorePassword,
                                         boolean validateCertificates,
                                         Set<String> pinnedFingerprints,
                                         boolean verifyHostname) {
        this(endpoint,
                defaultReceiveTimeout,
                defaultOriginHeader,
                buildClient(endpoint,
                        trustStore,
                        trustStorePassword,
                        keyStore,
                        keyStorePassword,
                        validateCertificates,
                        pinnedFingerprints,
                        verifyHostname));
    }

    private StreamableHttpClientTransport(URI endpoint,
                                          Duration defaultReceiveTimeout,
                                          String defaultOriginHeader,
                                          HttpClient client) {
        var scheme = endpoint.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Endpoint must use http or https");
        }
        this.endpoint = endpoint;
        this.defaultReceiveTimeout = ValidationUtil.requirePositive(defaultReceiveTimeout, "Default receive timeout");
        if (defaultOriginHeader == null || defaultOriginHeader.isBlank()) {
            throw new IllegalArgumentException("Default origin header is required");
        }
        this.defaultOriginHeader = defaultOriginHeader;
        this.client = client;
    }

    private static HttpClient defaultClient(URI endpoint, boolean verifyHostname) {
        return buildClient(endpoint, null, null, null, null, true, Set.of(), verifyHostname);
    }

    public static HttpClient buildClient(URI endpoint,
                                         Path trustStore,
                                         char[] trustStorePassword,
                                         Path keyStore,
                                         char[] keyStorePassword,
                                         boolean validateCertificates,
                                         Set<String> pinnedFingerprints,
                                         boolean verifyHostname) {
        try {
            var kms = loadKeyManagers(keyStore, keyStorePassword);
            var tms = validateCertificates
                    ? loadTrustManagers(trustStore, trustStorePassword, pinnedFingerprints)
                    : new TrustManager[]{new InsecureTrustManager()};
            var ctx = SSLContext.getInstance("TLS");
            ctx.init(kms, tms, null);
            var params = new SSLParameters();
            params.setServerNames(List.of(new SNIHostName(endpoint.getHost())));
            if (verifyHostname) {
                params.setEndpointIdentificationAlgorithm("HTTPS");
            }
            return HttpClient.newBuilder()
                    .sslContext(ctx)
                    .sslParameters(params)
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalArgumentException("TLS configuration failed", e);
        }
    }

    private static KeyManager[] loadKeyManagers(Path keyStore, char[] password) throws GeneralSecurityException, IOException {
        if (keyStore == null) {
            return null;
        }
        var ks = KeyStore.getInstance(KeyStore.getDefaultType());
        try (var in = Files.newInputStream(keyStore)) {
            ks.load(in, password);
        }
        var kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password);
        return kmf.getKeyManagers();
    }

    private static TrustManager[] loadTrustManagers(Path trustStore,
                                                    char[] password,
                                                    Set<String> pins) throws GeneralSecurityException, IOException {
        var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        if (trustStore == null) {
            tmf.init((KeyStore) null);
        } else {
            var ts = KeyStore.getInstance(KeyStore.getDefaultType());
            try (var in = Files.newInputStream(trustStore)) {
                ts.load(in, password);
            }
            tmf.init(ts);
        }
        var tms = tmf.getTrustManagers();
        if (pins.isEmpty()) {
            return tms;
        }
        for (var i = 0; i < tms.length; i++) {
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
        var status = response.statusCode();
        var ct = response.headers().firstValue("Content-Type").orElse("");
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
        var status = response.statusCode();
        var ct = response.headers().firstValue("Content-Type").orElse("");
        if (status == 202) {
            response.body().close();
            return;
        }
        if (ct.startsWith("application/json")) {
            try (var reader = Json.createReader(response.body())) {
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
    public JsonObject receive(Duration timeout) throws IOException {
        var duration = ValidationUtil.requirePositive(timeout, "timeout");
        var waitMillis = duration.toMillis();
        try {
            var result = incoming.poll(waitMillis, TimeUnit.MILLISECONDS);
            if (result == null) {
                throw new IOException("Timeout after " + waitMillis + "ms waiting for message");
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
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.log(Logger.Level.WARNING, "Session termination interrupted", e);
            }
        }
        streams.forEach(SseReader::close);
        streams.clear();
    }

    private void startReader(InputStream body) {
        var reader = new SseReader(body, incoming, streams);
        streams.add(reader);
        var t = new Thread(reader);
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
            if (pins.isEmpty()) {
                return;
            }
            var fp = Certificates.fingerprint(chain[0]);
            if (!pins.contains(fp)) {
                throw new CertificateException("Certificate pinning failure");
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return delegate.getAcceptedIssuers();
        }
    }
}
