package com.amannmalik.mcp.util;

import java.security.*;
import java.security.cert.*;
import java.util.*;
import java.util.function.Consumer;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public final class CertificateValidator implements X509TrustManager {
    private final X509TrustManager delegate;
    private final Set<String> pins;
    private final boolean revocation;
    private final List<Consumer<X509Certificate>> callbacks;
    private final String hostname;

    private CertificateValidator(X509TrustManager delegate,
                                Set<String> pins,
                                boolean revocation,
                                List<Consumer<X509Certificate>> callbacks,
                                String hostname) {
        this.delegate = delegate;
        this.pins = pins;
        this.revocation = revocation;
        this.callbacks = callbacks;
        this.hostname = hostname;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private X509TrustManager delegate;
        private final Set<String> pins = new HashSet<>();
        private boolean revocation;
        private final List<Consumer<X509Certificate>> callbacks = new ArrayList<>();
        private String hostname;

        public Builder trustManager(X509TrustManager delegate) {
            this.delegate = delegate;
            return this;
        }

        public Builder pin(String fingerprint) {
            pins.add(fingerprint.toUpperCase(Locale.ROOT));
            return this;
        }

        public Builder checkRevocation(boolean revocation) {
            this.revocation = revocation;
            return this;
        }

        public Builder callback(Consumer<X509Certificate> callback) {
            callbacks.add(callback);
            return this;
        }

        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public CertificateValidator build() {
            X509TrustManager tm = delegate;
            if (tm == null) {
                try {
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init((KeyStore) null);
                    tm = Arrays.stream(tmf.getTrustManagers())
                            .filter(X509TrustManager.class::isInstance)
                            .map(X509TrustManager.class::cast)
                            .findFirst()
                            .orElseThrow();
                } catch (GeneralSecurityException e) {
                    throw new IllegalStateException("No default trust manager", e);
                }
            }
            return new CertificateValidator(tm, Set.copyOf(pins), revocation, List.copyOf(callbacks), hostname);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        delegate.checkClientTrusted(chain, authType);
        extraChecks(chain);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        delegate.checkServerTrusted(chain, authType);
        extraChecks(chain);
        if (hostname != null) verifyHostname(chain[0], hostname);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return delegate.getAcceptedIssuers();
    }

    private void extraChecks(X509Certificate[] chain) throws CertificateException {
        for (var cert : chain) cert.checkValidity();
        if (revocation) validateRevocation(chain);
        if (!pins.isEmpty()) {
            String fp = Certificates.fingerprint(chain[0]);
            if (!pins.contains(fp)) throw new CertificateException("Certificate pinning failure");
        }
        for (var cb : callbacks) cb.accept(chain[0]);
    }

    private void validateRevocation(X509Certificate[] chain) throws CertificateException {
        try {
            var anchors = new HashSet<TrustAnchor>();
            for (var issuer : delegate.getAcceptedIssuers()) anchors.add(new TrustAnchor(issuer, null));
            if (anchors.isEmpty()) return;
            CertPath cp = CertificateFactory.getInstance("X.509").generateCertPath(Arrays.asList(chain));
            PKIXParameters params = new PKIXParameters(anchors);
            params.setRevocationEnabled(true);
            CertPathValidator.getInstance("PKIX").validate(cp, params);
        } catch (GeneralSecurityException e) {
            throw new CertificateException(e);
        }
    }

    public static void verifyHostname(X509Certificate cert, String hostname) throws CertificateException {
        var sans = Certificates.subjectAltNames(cert);
        for (var san : sans) {
            if (hostname.equalsIgnoreCase(san)) return;
        }
        String cn = cert.getSubjectX500Principal().getName();
        if (cn.contains("CN=")) {
            String name = cn.substring(cn.indexOf("CN=") + 3).split(",")[0];
            if (hostname.equalsIgnoreCase(name)) return;
        }
        throw new CertificateException("Hostname mismatch");
    }
}

