package com.amannmalik.mcp.test;

import com.amannmalik.mcp.api.McpServerConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class UrlSchemeValidationTest {
    @Test
    public void rejectsHttpMetadataUrlWhenHttpsEnabled() {
        assertThrows(IllegalArgumentException.class, () ->
                McpServerConfiguration.defaultConfiguration().withTransport(
                        "http",
                        3000,
                        List.of("http://localhost"),
                        "https://mcp.example.com",
                        "http://meta.example.com",
                        List.of("https://auth.example.com"),
                        false,
                        false));
    }

    @Test
    public void rejectsHttpAuthServerWhenHttpsEnabled() {
        assertThrows(IllegalArgumentException.class, () ->
                McpServerConfiguration.defaultConfiguration().withTransport(
                        "http",
                        3000,
                        List.of("http://localhost"),
                        "https://mcp.example.com",
                        "",
                        List.of("http://auth.example.com"),
                        false,
                        false));
    }
}
