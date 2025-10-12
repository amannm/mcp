package com.amannmalik.mcp.api;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public record TlsConfiguration(
        String keystorePath,
        String keystorePassword,
        String keystoreType,
        String truststorePath,
        String truststorePassword,
        String truststoreType,
        List<String> tlsProtocols,
        List<String> cipherSuites
) {

    private static final Set<String> ALLOWED_PROTOCOLS = Set.of("TLSv1.2", "TLSv1.3");
    private static final Pattern WEAK_CIPHERS = Pattern.compile(".*_(NULL|RC4|DES|3DES|MD5|CBC|EXPORT|anon)_.*", Pattern.CASE_INSENSITIVE);

    public TlsConfiguration {
        tlsProtocols = List.copyOf(tlsProtocols);
        cipherSuites = List.copyOf(cipherSuites);
        if (tlsProtocols.isEmpty() ||
                tlsProtocols.stream().anyMatch(p -> p.isBlank() || !ALLOWED_PROTOCOLS.contains(p))) {
            throw new IllegalArgumentException("Invalid TLS protocols");
        }
        if (cipherSuites.isEmpty() || cipherSuites.stream().anyMatch(s -> s.isBlank() || !isStrongCipher(s))) {
            throw new IllegalArgumentException("Invalid cipher suites");
        }
    }

    public static boolean isStrongCipher(String suite) {
        if (WEAK_CIPHERS.matcher(suite).matches()) {
            return false;
        }
        return suite.contains("TLS_AES") || suite.contains("_ECDHE_") || suite.contains("_DHE_");
    }

    public static TlsConfiguration defaultConfiguration() {
        return new TlsConfiguration(
                "",
                "",
                "PKCS12",
                "",
                "",
                "PKCS12",
                List.of("TLSv1.3", "TLSv1.2"),
                List.of(
                        "TLS_AES_128_GCM_SHA256",
                        "TLS_AES_256_GCM_SHA384",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"
                )
        );
    }

    public boolean hasKeystore() {
        return !keystorePath.isBlank() && !keystorePassword.isBlank() && !keystoreType.isBlank();
    }

    public boolean hasTruststore() {
        return !truststorePath.isBlank() && !truststorePassword.isBlank() && !truststoreType.isBlank();
    }
}