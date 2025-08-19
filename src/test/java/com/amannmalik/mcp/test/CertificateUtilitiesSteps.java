package com.amannmalik.mcp.test;

import com.amannmalik.mcp.util.Certificates;
import io.cucumber.java.en.*;
import javax.security.auth.x500.X500Principal;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

public final class CertificateUtilitiesSteps {
    private KeyPair pair;
    private X509Certificate cert;
    private PKCS10CertificationRequest csr;

    @Given("I generate an RSA key pair")
    public void i_generate_an_rsa_key_pair() {
        pair = Certificates.generateKeyPair(Certificates.Algorithm.RSA);
    }

    @When("I create a self-signed certificate for {string} with SAN {string}")
    public void i_create_a_self_signed_certificate(String cn, String san) {
        cert = Certificates.selfSign(
                pair,
                new X500Principal("CN=" + cn),
                List.of(san),
                Duration.ofDays(1)
        );
    }

    @Then("the certificate subject should be {string}")
    public void the_certificate_subject_should_be(String expected) {
        if (!expected.equals(cert.getSubjectX500Principal().getName())) {
            throw new AssertionError("wrong subject");
        }
    }

    @Then("the certificate should contain SAN {string}")
    public void the_certificate_should_contain_san(String san) throws Exception {
        if (cert.getSubjectAlternativeNames().stream().noneMatch(n -> san.equals(n.get(1)))) {
            throw new AssertionError("san missing");
        }
    }

    @When("I create a CSR for {string} with SAN {string}")
    public void i_create_a_csr(String cn, String san) {
        var data = Certificates.generateCsr(pair, new X500Principal("CN=" + cn), List.of(san));
        try {
            csr = new PKCS10CertificationRequest(data);
        } catch (Exception e) {
            throw new AssertionError("csr parse failed", e);
        }
    }

    @Then("the CSR subject should be {string}")
    public void the_csr_subject_should_be(String expected) {
        if (!expected.equals(csr.getSubject().toString())) {
            throw new AssertionError("wrong csr subject");
        }
    }
}
