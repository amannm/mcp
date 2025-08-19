package com.amannmalik.mcp.test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;
import org.junit.jupiter.api.Test;

import com.amannmalik.mcp.util.CertificateValidator;
import com.amannmalik.mcp.util.Certificates;

final class CertificateValidatorTest {
    @Test
    void validatesHostname() throws Exception {
        KeyPair pair = Certificates.generateKeyPair(Certificates.Algorithm.RSA);
        X509Certificate cert = Certificates.selfSign(pair, new X500Principal("CN=Test"), List.of("example.com"), Duration.ofDays(1));
        CertificateValidator.verifyHostname(cert, "example.com");
        assertThrows(CertificateException.class, () -> CertificateValidator.verifyHostname(cert, "other.com"));
    }

    @Test
    void enforcesPinning() throws Exception {
        KeyPair pair = Certificates.generateKeyPair(Certificates.Algorithm.RSA);
        X509Certificate cert = Certificates.selfSign(pair, new X500Principal("CN=Test"), List.of("example.com"), Duration.ofDays(1));
        String pin = Certificates.fingerprint(cert);
        X509TrustManager delegate = new X509TrustManager() {
            @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
            @Override public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                Certificates.verifySelfSigned(chain[0]);
            }
            @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        };
        CertificateValidator.builder().trustManager(delegate).pin(pin).build()
                .checkServerTrusted(new X509Certificate[]{cert}, "RSA");
        assertThrows(CertificateException.class, () ->
                CertificateValidator.builder().trustManager(delegate).pin("00").build()
                        .checkServerTrusted(new X509Certificate[]{cert}, "RSA"));
    }
}

