package com.amannmalik.mcp.util;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.cert.CertificateException;

/// - [Security Best Practices](specification/2025-06-18/basic/security_best_practices.mdx)
public final class TlsErrors {
    private TlsErrors() {
    }

    public static IOException ioException(SSLException e) {
        return new IOException(message(e), e);
    }

    private static String message(SSLException e) {
        if (e instanceof SSLHandshakeException h) {
            Throwable c = h.getCause();
            if (c instanceof CertificateException) return "Certificate validation failed";
            if (c instanceof SSLPeerUnverifiedException) return "Client certificate authentication failed";
            if (c instanceof SocketTimeoutException) return "TLS handshake timed out";
            if (c instanceof SSLProtocolException) return "TLS protocol negotiation failed";
            String m = h.getMessage();
            if (m != null && m.contains("no cipher suites in common")) return "Cipher suite negotiation failed";
            return "TLS handshake failed";
        }
        if (e instanceof SSLProtocolException) return "TLS protocol negotiation failed";
        return "TLS error";
    }
}

