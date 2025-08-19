package com.amannmalik.mcp.utiltest;

import com.amannmalik.mcp.util.Certificates;
import javax.security.auth.x500.X500Principal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public final class CertificatesTest {
    @Test
    void pemRoundTrip() {
        var pair = Certificates.generateKeyPair(Certificates.Algorithm.RSA);
        var cert = Certificates.selfSign(pair, new X500Principal("CN=example.com"), List.of("example.com"), Duration.ofDays(1));
        var pem = Certificates.toPem(cert);
        var parsed = Certificates.fromPem(pem);
        assertEquals(cert.getSubjectX500Principal(), parsed.getSubjectX500Principal());
    }
}
