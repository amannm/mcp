package com.amannmalik.mcp.test;

import com.amannmalik.mcp.cli.ServerCommand;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ServerCommandTlsOptionsTest {
    @Test
    public void parsesTlsOptions() {
        CommandLine cmd = new CommandLine(ServerCommand.createCommandSpec());
        String[] args = {
                "--https-port", "4443",
                "--keystore", "server.p12",
                "--keystore-password", "pass",
                "--keystore-type", "PKCS12",
                "--truststore", "trust.p12",
                "--truststore-password", "tpass",
                "--truststore-type", "JKS",
                "--tls-protocols", "TLSv1.3,TLSv1.2",
                "--cipher-suites", "TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384",
                "--require-client-auth",
                "--https-only"
        };
        CommandLine.ParseResult pr = cmd.parseArgs(args);
        assertEquals(4443, pr.matchedOptionValue("--https-port", 0));
        assertEquals(Path.of("server.p12"), pr.matchedOptionValue("--keystore", null));
        assertEquals("pass", pr.matchedOptionValue("--keystore-password", null));
        assertEquals("PKCS12", pr.matchedOptionValue("--keystore-type", null));
        assertEquals(Path.of("trust.p12"), pr.matchedOptionValue("--truststore", null));
        assertEquals("tpass", pr.matchedOptionValue("--truststore-password", null));
        assertEquals("JKS", pr.matchedOptionValue("--truststore-type", null));
        assertEquals(List.of("TLSv1.3", "TLSv1.2"), pr.matchedOptionValue("--tls-protocols", List.of()));
        assertEquals(List.of("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384"), pr.matchedOptionValue("--cipher-suites", List.of()));
        assertTrue(pr.matchedOptionValue("--require-client-auth", false));
        assertTrue(pr.matchedOptionValue("--https-only", false));
    }
}
