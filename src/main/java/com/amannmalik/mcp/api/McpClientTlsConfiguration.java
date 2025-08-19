package com.amannmalik.mcp.api;

import java.util.List;

public record McpClientTlsConfiguration(
        String truststorePath,
        String truststorePassword,
        String truststoreType,
        String keystorePath,
        String keystorePassword,
        String keystoreType,
        CertificateValidationMode certificateValidationMode,
        List<String> tlsProtocols,
        List<String> cipherSuites,
        List<String> certificatePins,
        boolean verifyHostname
) {
    public McpClientTlsConfiguration {
        if (truststorePath == null || truststorePassword == null || truststoreType == null)
            throw new IllegalArgumentException("truststore configuration required");
        if (keystorePath == null || keystorePassword == null || keystoreType == null)
            throw new IllegalArgumentException("keystore configuration required");
        if (certificateValidationMode == null)
            throw new IllegalArgumentException("certificate validation mode required");
        if (tlsProtocols == null || tlsProtocols.isEmpty() || tlsProtocols.stream().anyMatch(String::isBlank))
            throw new IllegalArgumentException("tls protocols required");
        if (cipherSuites == null || cipherSuites.isEmpty() || cipherSuites.stream().anyMatch(String::isBlank))
            throw new IllegalArgumentException("cipher suites required");
        if (certificatePins == null)
            throw new IllegalArgumentException("certificate pins required");
        tlsProtocols = List.copyOf(tlsProtocols);
        cipherSuites = List.copyOf(cipherSuites);
        certificatePins = List.copyOf(certificatePins);
    }

    public static McpClientTlsConfiguration defaultConfiguration() {
        return new McpClientTlsConfiguration(
                "",
                "",
                "PKCS12",
                "",
                "",
                "PKCS12",
                CertificateValidationMode.STRICT,
                List.of("TLSv1.3", "TLSv1.2"),
                List.of("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384"),
                List.of(),
                true
        );
    }
}
