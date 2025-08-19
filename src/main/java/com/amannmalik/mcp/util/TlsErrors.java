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
        return switch (e) {
            case SSLHandshakeException h -> {
                var c = h.getCause();
                yield switch (c) {
                    case CertificateException __ -> "Certificate validation failed";
                    case SSLPeerUnverifiedException __ -> "Client certificate authentication failed";
                    case SocketTimeoutException __ -> "TLS handshake timed out";
                    case SSLProtocolException __ -> "TLS protocol negotiation failed";
                    case null, default -> {
                        var m = h.getMessage();
                        yield m != null && m.contains("no cipher suites in common")
                                ? "Cipher suite negotiation failed"
                                : "TLS handshake failed";
                    }
                };
            }
            case SSLProtocolException __ -> "TLS protocol negotiation failed";
            default -> "TLS error";
        };
    }
}

