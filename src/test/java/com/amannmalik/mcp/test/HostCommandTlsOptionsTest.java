package com.amannmalik.mcp.test;

import com.amannmalik.mcp.cli.HostCommand;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HostCommandTlsOptionsTest {
    @Test
    public void parsesTlsOptions() {
        CommandLine cmd = new CommandLine(HostCommand.createCommandSpec());
        String[] args = {
                "--client", "a:cmd",
                "--client-truststore", "trust.p12",
                "--client-truststore-password", "tpass",
                "--client-truststore-type", "JKS",
                "--client-keystore", "client.p12",
                "--client-keystore-password", "cpass",
                "--client-keystore-type", "PKCS12",
                "--verify-certificates", "false",
                "--allow-self-signed",
                "--tls-protocols", "TLSv1.3",
                "--certificate-pinning", "pin1,pin2"
        };
        CommandLine.ParseResult pr = cmd.parseArgs(args);
        assertEquals(Path.of("trust.p12"), pr.matchedOptionValue("--client-truststore", null));
        assertEquals("tpass", pr.matchedOptionValue("--client-truststore-password", null));
        assertEquals("JKS", pr.matchedOptionValue("--client-truststore-type", null));
        assertEquals(Path.of("client.p12"), pr.matchedOptionValue("--client-keystore", null));
        assertEquals("cpass", pr.matchedOptionValue("--client-keystore-password", null));
        assertEquals("PKCS12", pr.matchedOptionValue("--client-keystore-type", null));
        assertFalse(pr.matchedOptionValue("--verify-certificates", true));
        assertTrue(pr.matchedOptionValue("--allow-self-signed", false));
        assertEquals(List.of("TLSv1.3"), pr.matchedOptionValue("--tls-protocols", List.of()));
        assertEquals(List.of("pin1", "pin2"), pr.matchedOptionValue("--certificate-pinning", List.of()));
    }
}
