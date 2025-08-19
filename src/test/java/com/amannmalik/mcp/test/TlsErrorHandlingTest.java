package com.amannmalik.mcp.test;

import com.amannmalik.mcp.util.TlsErrors;
import org.junit.jupiter.api.Test;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.cert.CertificateException;

import static org.junit.jupiter.api.Assertions.*;

public class TlsErrorHandlingTest {
    @Test
    public void mapsCertificateValidationFailure() {
        var ex = new SSLHandshakeException("handshake");
        ex.initCause(new CertificateException("bad cert"));
        IOException io = TlsErrors.ioException(ex);
        assertEquals("Certificate validation failed", io.getMessage());
    }

    @Test
    public void mapsProtocolMismatch() {
        var ex = new SSLProtocolException("version");
        IOException io = TlsErrors.ioException(ex);
        assertEquals("TLS protocol negotiation failed", io.getMessage());
    }

    @Test
    public void mapsCipherSuiteNegotiationFailure() {
        var ex = new SSLHandshakeException("no cipher suites in common");
        IOException io = TlsErrors.ioException(ex);
        assertEquals("Cipher suite negotiation failed", io.getMessage());
    }

    @Test
    public void mapsClientCertificateAuthenticationFailure() {
        var ex = new SSLHandshakeException("peer not verified");
        ex.initCause(new SSLPeerUnverifiedException("missing"));
        IOException io = TlsErrors.ioException(ex);
        assertEquals("Client certificate authentication failed", io.getMessage());
    }

    @Test
    public void mapsHandshakeTimeout() {
        var ex = new SSLHandshakeException("timeout");
        ex.initCause(new SocketTimeoutException("timeout"));
        IOException io = TlsErrors.ioException(ex);
        assertEquals("TLS handshake timed out", io.getMessage());
    }

    @Test
    public void mapsGenericHandshakeFailure() {
        var ex = new SSLHandshakeException("other");
        IOException io = TlsErrors.ioException(ex);
        assertEquals("TLS handshake failed", io.getMessage());
    }
}

