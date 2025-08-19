package com.amannmalik.mcp.util;

import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.util.*;

public final class Certificates {
    private Certificates() {
    }

    public static KeyPair generateKeyPair(Algorithm algorithm) {
        try {
            return switch (algorithm) {
                case RSA -> {
                    var g = KeyPairGenerator.getInstance("RSA");
                    g.initialize(2048);
                    yield g.generateKeyPair();
                }
                case ECDSA -> {
                    var g = KeyPairGenerator.getInstance("EC");
                    g.initialize(new ECGenParameterSpec("secp256r1"));
                    yield g.generateKeyPair();
                }
            };
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Key pair generation failed", e);
        }
    }

    public static X509Certificate selfSign(KeyPair pair, X500Principal subject, List<String> subjectAltNames, Duration lifetime) {
        try {
            var now = new Date();
            var end = new Date(now.getTime() + lifetime.toMillis());
            var builder = new JcaX509v3CertificateBuilder(
                    new X500Name(subject.getName()),
                    new BigInteger(64, new SecureRandom()),
                    now,
                    end,
                    new X500Name(subject.getName()),
                    pair.getPublic()
            );
            var sanBuilder = new GeneralNamesBuilder();
            for (var name : subjectAltNames) sanBuilder.addName(new GeneralName(GeneralName.dNSName, name));
            builder.addExtension(Extension.subjectAlternativeName, false, sanBuilder.build());
            ContentSigner signer = new JcaContentSignerBuilder(switch (pair.getPrivate()) {
                case RSAPrivateKey ignored -> "SHA256withRSA";
                default -> "SHA256withECDSA";
            }).build(pair.getPrivate());
            var holder = builder.build(signer);
            return new JcaX509CertificateConverter().getCertificate(holder);
        } catch (CertificateException | OperatorCreationException | IOException e) {
            throw new IllegalStateException("Certificate generation failed", e);
        }
    }

    public static byte[] generateCsr(KeyPair pair, X500Principal subject, List<String> subjectAltNames) {
        try {
            var builder = new JcaPKCS10CertificationRequestBuilder(new X500Name(subject.getName()), pair.getPublic());
            var sanBuilder = new GeneralNamesBuilder();
            for (var name : subjectAltNames) sanBuilder.addName(new GeneralName(GeneralName.dNSName, name));
            var extensions = new ExtensionsGenerator();
            extensions.addExtension(Extension.subjectAlternativeName, false, sanBuilder.build());
            builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensions.generate());
            ContentSigner signer = new JcaContentSignerBuilder(switch (pair.getPrivate()) {
                case RSAPrivateKey ignored -> "SHA256withRSA";
                default -> "SHA256withECDSA";
            }).build(pair.getPrivate());
            PKCS10CertificationRequest csr = builder.build(signer);
            return csr.getEncoded();
        } catch (OperatorCreationException | IOException e) {
            throw new IllegalStateException("CSR generation failed", e);
        }
    }

    public static X509Certificate parse(byte[] data) {
        try {
            var cert = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(data));
            if (cert instanceof X509Certificate x509) return x509;
            throw new IllegalArgumentException("Not an X.509 certificate");
        } catch (CertificateException e) {
            throw new IllegalArgumentException("Invalid certificate data", e);
        }
    }

    public static void verifySelfSigned(X509Certificate cert) {
        try {
            cert.checkValidity();
            cert.verify(cert.getPublicKey());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Certificate verification failed", e);
        }
    }

    public static String fingerprint(X509Certificate cert) throws CertificateException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(cert.getEncoded());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02X", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            throw new CertificateException(e);
        }
    }

    public static List<String> subjectAltNames(X509Certificate cert) {
        try {
            Collection<List<?>> sans = cert.getSubjectAlternativeNames();
            if (sans == null) return List.of();
            List<String> out = new ArrayList<>();
            for (var san : sans) {
                int type = (Integer) san.get(0);
                if (type == 2 || type == 7) out.add(san.get(1).toString());
            }
            return out;
        } catch (CertificateParsingException e) {
            throw new IllegalStateException("SAN parsing failed", e);
        }
    }

    public static String toPem(X509Certificate cert) {
        try (var sw = new StringWriter(); var pw = new JcaPEMWriter(sw)) {
            pw.writeObject(cert);
            pw.flush();
            return sw.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Encoding failed", e);
        }
    }

    public static X509Certificate fromPem(String pem) {
        var sanitized = pem
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
        return parse(Base64.getDecoder().decode(sanitized));
    }

    public static String csrToPem(byte[] csr) {
        var base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(csr);
        return "-----BEGIN CERTIFICATE REQUEST-----\n" + base64 + "\n-----END CERTIFICATE REQUEST-----\n";
    }

    public enum Algorithm {RSA, ECDSA}
}
