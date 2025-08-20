package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.api.CertificateValidationMode;
import com.amannmalik.mcp.api.McpClientConfiguration;
import com.amannmalik.mcp.api.McpServerConfiguration;
import com.amannmalik.mcp.api.Transport;
import com.amannmalik.mcp.auth.AuthorizationManager;
import com.amannmalik.mcp.auth.BearerTokenAuthorizationStrategy;
import com.amannmalik.mcp.auth.JwtTokenValidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.net.URI;
import java.util.List;
import java.util.Set;

public final class TransportFactory {
    private TransportFactory() {
    }

    public static Transport server(McpServerConfiguration config) throws Exception {
        return switch (config.transportType()) {
            case "stdio" -> new StdioTransport(System.in, System.out, config.defaultTimeoutMs());
            case "http" -> {
                if (!config.insecure() && config.authServers().isEmpty()) {
                    throw new IllegalArgumentException("auth server must be specified");
                }
                AuthorizationManager authManager = null;
                if (config.expectedAudience() != null && !config.expectedAudience().isBlank()) {
                    var secretEnv = System.getenv("MCP_JWT_SECRET");
                    var tokenValidator = secretEnv == null || secretEnv.isBlank()
                            ? new JwtTokenValidator(config.expectedAudience())
                            : new JwtTokenValidator(config.expectedAudience(), secretEnv.getBytes(StandardCharsets.UTF_8));
                    authManager = new AuthorizationManager(List.of(new BearerTokenAuthorizationStrategy(tokenValidator)));
                }
                var ht = new StreamableHttpServerTransport(
                        config,
                        authManager);
                if (config.verbose()) {
                    if (config.serverPort() > 0) {
                        System.err.println("Listening on http://127.0.0.1:" + ht.port());
                    }
                    if (config.httpsPort() > 0) {
                        System.err.println("Listening on https://127.0.0.1:" + ht.httpsPort());
                    }
                }
                yield ht;
            }
            default -> throw new IllegalArgumentException("Unknown transport type: " + config.transportType());
        };
    }

    public static Transport client(McpClientConfiguration config, boolean globalVerbose) throws IOException {
        var spec = config.commandSpec();
        if (spec != null && !spec.isBlank()) {
            if (spec.startsWith("http://") || spec.startsWith("https://")) {
                var uri = URI.create(spec);
                if (spec.startsWith("https://")) {
                    var ts = config.truststorePath().isBlank() ? null : Path.of(config.truststorePath());
                    var ks = config.keystorePath().isBlank() ? null : Path.of(config.keystorePath());
                    var pins = Set.copyOf(config.certificatePins());
                    var validate = config.certificateValidationMode() != CertificateValidationMode.PERMISSIVE;
                    return new StreamableHttpClientTransport(
                            uri,
                            config.defaultReceiveTimeout(),
                            config.defaultOriginHeader(),
                            ts,
                            config.truststorePassword().toCharArray(),
                            ks,
                            config.keystorePassword().toCharArray(),
                            validate,
                            pins,
                            config.verifyHostname());
                }
                return new StreamableHttpClientTransport(
                        uri,
                        config.defaultReceiveTimeout(),
                        config.defaultOriginHeader());
            }
            var cmds = spec.split(" ");
            var verbose = config.verbose() || globalVerbose;
            return new StdioTransport(cmds, verbose ? System.err::println : s -> {
            }, config.defaultReceiveTimeout());
        }
        return new StdioTransport(System.in, System.out, config.defaultReceiveTimeout());
    }
}
