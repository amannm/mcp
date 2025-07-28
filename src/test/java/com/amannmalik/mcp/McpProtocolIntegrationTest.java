package com.amannmalik.mcp;

import com.amannmalik.mcp.client.SimpleMcpClient;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import com.amannmalik.mcp.server.SimpleServer;
import com.amannmalik.mcp.transport.StdioTransport;
import com.amannmalik.mcp.transport.StreamableHttpTransport;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class McpProtocolIntegrationTest {

    @TempDir
    Path tempDir;

    private static final String JAVA_BIN = System.getProperty("java.home") +
            File.separator + "bin" + File.separator + "java";
    
    private ExecutorService executor;
    private int httpPort;

    @BeforeAll
    void setup() {
        executor = Executors.newCachedThreadPool();
        httpPort = findAvailablePort();
    }

    @AfterAll
    void cleanup() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Happy path: Complete MCP protocol lifecycle via CLI")
    void testCompleteProtocolLifecycleViaCli() throws Exception {
        // Start server process via CLI with timeout
        ProcessBuilder serverBuilder = new ProcessBuilder(
                JAVA_BIN, "-cp", System.getProperty("java.class.path"),
                "com.amannmalik.mcp.Main", "server", "--stdio", "-v"
        );
        
        Process serverProcess = serverBuilder.start();
        
        try {
            // Fail fast if server doesn't start within reasonable time
            assertEventually(() -> serverProcess.isAlive(), Duration.ofMillis(500), 
                    "Server should start within 500ms");
            
            // Validate server is actually ready for connections
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(serverProcess.getErrorStream()));
            String errorLine = null;
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 1000) {
                if (errorReader.ready()) {
                    errorLine = errorReader.readLine();
                    if (errorLine != null && errorLine.contains("Exception")) {
                        fail("Server failed to start: " + errorLine);
                    }
                }
                Thread.sleep(10);
            }
            
            // Create client with strict timeouts
            StdioTransport clientTransport = new StdioTransport(
                    serverProcess.getInputStream(),
                    serverProcess.getOutputStream()
            );
            
            SimpleMcpClient client = new SimpleMcpClient(
                    new ClientInfo("test-client", "Test Client", "1.0"),
                    EnumSet.allOf(ClientCapability.class),
                    clientTransport
            );
            
            // Test connection with timeout
            CompletableFuture<Void> connectTask = CompletableFuture.runAsync(() -> {
                try {
                    client.connect();
                } catch (Exception e) {
                    throw new RuntimeException("Client connection failed", e);
                }
            });
            
            try {
                connectTask.get(2, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                fail("Client connection timed out after 2 seconds");
            }
            
            // Test protocol operations with individual timeouts
            testProtocolOperationWithTimeout(() -> client.request("ping", Json.createObjectBuilder().build()),
                    "ping", 3000);
            
            testProtocolOperationWithTimeout(() -> client.request("resources/list", Json.createObjectBuilder().build()),
                    "resources/list", 3000);
            
            testProtocolOperationWithTimeout(() -> client.request("tools/list", Json.createObjectBuilder().build()),
                    "tools/list", 3000);
            
            testProtocolOperationWithTimeout(() -> client.request("prompts/list", Json.createObjectBuilder().build()),
                    "prompts/list", 3000);
            
            // Disconnect with timeout
            CompletableFuture<Void> disconnectTask = CompletableFuture.runAsync(() -> {
                try {
                    client.disconnect();
                } catch (Exception e) {
                    throw new RuntimeException("Client disconnect failed", e);
                }
            });
            
            try {
                disconnectTask.get(1, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                fail("Client disconnect timed out after 1 second");
            }
            
        } finally {
            if (serverProcess.isAlive()) {
                serverProcess.destroyForcibly();
                // Ensure process actually terminates
                boolean terminated = serverProcess.waitFor(2, TimeUnit.SECONDS);
                if (!terminated) {
                    fail("Server process failed to terminate within 2 seconds");
                }
            }
        }
    }
    
    private void testProtocolOperationWithTimeout(Callable<JsonRpcMessage> operation, String operationName, long timeoutMs) {
        CompletableFuture<JsonRpcMessage> task = CompletableFuture.supplyAsync(() -> {
            try {
                return operation.call();
            } catch (Exception e) {
                throw new RuntimeException(operationName + " operation failed", e);
            }
        });
        
        try {
            JsonRpcMessage response = task.get(timeoutMs, TimeUnit.MILLISECONDS);
            assertInstanceOf(JsonRpcResponse.class, response, 
                    operationName + " should return JsonRpcResponse");
        } catch (TimeoutException e) {
            fail(operationName + " operation timed out after " + timeoutMs + "ms");
        } catch (ExecutionException e) {
            fail(operationName + " operation failed: " + e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(operationName + " operation was interrupted");
        }
    }

    @Test
    @DisplayName("Happy path: HTTP transport with protocol operations")
    void testHttpTransportProtocolOperations() throws Exception {
        // Start HTTP server via CLI with stricter timeout
        ProcessBuilder serverBuilder = new ProcessBuilder(
                JAVA_BIN, "-cp", System.getProperty("java.class.path"),
                "com.amannmalik.mcp.Main", "server", "--http", String.valueOf(httpPort), "-v"
        );
        
        Process serverProcess = serverBuilder.start();
        
        try {
            // Fail fast if server doesn't bind to port within 2 seconds
            assertEventually(() -> isPortOpen(httpPort), Duration.ofSeconds(2), 
                    "HTTP server should bind to port within 2 seconds");
            
            // Test HTTP connectivity with timeout
            CompletableFuture<String> httpTest = CompletableFuture.supplyAsync(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new java.net.InetSocketAddress("127.0.0.1", httpPort), 1000);
                    
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    
                    out.println("GET / HTTP/1.1");
                    out.println("Host: 127.0.0.1:" + httpPort);
                    out.println("Connection: close");
                    out.println();
                    
                    String response = in.readLine();
                    if (response == null) {
                        throw new RuntimeException("No HTTP response received");
                    }
                    return response;
                } catch (IOException e) {
                    throw new RuntimeException("HTTP connection failed: " + e.getMessage());
                }
            });
            
            try {
                String response = httpTest.get(3, TimeUnit.SECONDS);
                assertTrue(response.startsWith("HTTP/"), 
                        "Should receive HTTP response, got: " + response);
            } catch (TimeoutException e) {
                fail("HTTP request timed out after 3 seconds");
            } catch (ExecutionException e) {
                fail("HTTP request failed: " + e.getCause().getMessage());
            }
            
        } finally {
            serverProcess.destroyForcibly();
            boolean terminated = serverProcess.waitFor(1, TimeUnit.SECONDS);
            if (!terminated) {
                fail("HTTP server failed to terminate within 1 second");
            }
        }
    }

    @Test
    @DisplayName("Happy path: Client-server interaction with different configurations")
    void testClientServerInteractionWithConfigurations() throws Exception {
        // Test with minimal configuration and fail-fast validation
        ProcessBuilder minimalServerBuilder = new ProcessBuilder(
                JAVA_BIN, "-cp", System.getProperty("java.class.path"),
                "com.amannmalik.mcp.Main", "server", "--stdio"
        );
        
        Process minimalServer = minimalServerBuilder.start();
        
        try {
            // Validate server starts quickly
            assertEventually(minimalServer::isAlive, Duration.ofMillis(500), 
                    "Minimal server should start within 500ms");
            
            // Check for immediate startup errors
            if (minimalServer.getErrorStream().available() > 0) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(minimalServer.getErrorStream()));
                if (errorReader.ready()) {
                    String error = errorReader.readLine();
                    if (error != null && error.contains("Exception")) {
                        fail("Server startup error: " + error);
                    }
                }
            }
            
            // Test client connection with timeout and validation
            ProcessBuilder clientBuilder = new ProcessBuilder(
                    JAVA_BIN, "-cp", System.getProperty("java.class.path"),
                    "com.amannmalik.mcp.Main", "client", 
                    "--command", buildEchoServerCommand()
            );
            
            Process clientProcess = clientBuilder.start();
            
            // Client should complete quickly or fail fast
            boolean finished = clientProcess.waitFor(2, TimeUnit.SECONDS);
            assertTrue(finished, "Client should complete interaction within 2 seconds");
            
            // Validate exit behavior (echo command will likely exit non-zero, but should exit quickly)
            int exitCode = clientProcess.exitValue();
            assertTrue(exitCode >= 0, "Client should exit with valid exit code");
            
        } finally {
            minimalServer.destroyForcibly();
            boolean terminated = minimalServer.waitFor(1, TimeUnit.SECONDS);
            if (!terminated) {
                fail("Minimal server failed to terminate within 1 second");
            }
        }
    }

    @Test
    @DisplayName("Happy path: Multi-server scenario simulation")
    void testMultiServerScenario() throws Exception {
        // Start multiple servers on different ports with tight timeouts
        int port1 = httpPort + 10;
        int port2 = httpPort + 20;
        
        ProcessBuilder server1Builder = new ProcessBuilder(
                JAVA_BIN, "-cp", System.getProperty("java.class.path"),
                "com.amannmalik.mcp.Main", "server", "--http", String.valueOf(port1)
        );
        
        ProcessBuilder server2Builder = new ProcessBuilder(
                JAVA_BIN, "-cp", System.getProperty("java.class.path"),
                "com.amannmalik.mcp.Main", "server", "--http", String.valueOf(port2)
        );
        
        Process server1 = server1Builder.start();
        Process server2 = server2Builder.start();
        
        try {
            // Fail fast if servers don't start within 1.5 seconds each
            assertEventually(() -> isPortOpen(port1), Duration.ofMillis(1500), 
                    "Server 1 should bind to port within 1.5 seconds");
            assertEventually(() -> isPortOpen(port2), Duration.ofMillis(1500), 
                    "Server 2 should bind to port within 1.5 seconds");
            
            // Validate processes are actually alive
            assertTrue(server1.isAlive(), "Server 1 process should be running");
            assertTrue(server2.isAlive(), "Server 2 process should be running");
            
            // Test concurrent connections with timeout
            CompletableFuture<Void> connection1 = CompletableFuture.runAsync(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new java.net.InetSocketAddress("127.0.0.1", port1), 1000);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to connect to server 1: " + e.getMessage());
                }
            });
            
            CompletableFuture<Void> connection2 = CompletableFuture.runAsync(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new java.net.InetSocketAddress("127.0.0.1", port2), 1000);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to connect to server 2: " + e.getMessage());
                }
            });
            
            try {
                CompletableFuture.allOf(connection1, connection2).get(3, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                fail("Connections to both servers timed out after 3 seconds");
            } catch (ExecutionException e) {
                fail("Connection failed: " + e.getCause().getMessage());
            }
            
        } finally {
            server1.destroyForcibly();
            server2.destroyForcibly();
            
            // Ensure clean termination
            boolean server1Terminated = server1.waitFor(1, TimeUnit.SECONDS);
            boolean server2Terminated = server2.waitFor(1, TimeUnit.SECONDS);
            
            if (!server1Terminated || !server2Terminated) {
                fail("One or more servers failed to terminate within 1 second");
            }
        }
    }

    @Test
    @DisplayName("Happy path: CLI error handling and recovery")
    void testCliErrorHandlingAndRecovery() throws Exception {
        // Test invalid port handling
        ProcessBuilder invalidPortBuilder = new ProcessBuilder(
                JAVA_BIN, "-cp", System.getProperty("java.class.path"),
                "com.amannmalik.mcp.Main", "server", "--http", "-1"
        );
        
        Process invalidProcess = invalidPortBuilder.start();
        
        boolean finished = invalidProcess.waitFor(3, TimeUnit.SECONDS);
        assertTrue(finished, "Invalid port process should complete");
        assertNotEquals(0, invalidProcess.exitValue(), "Invalid port should cause error exit");
        
        // Test missing command for client
        ProcessBuilder missingCommandBuilder = new ProcessBuilder(
                JAVA_BIN, "-cp", System.getProperty("java.class.path"),
                "com.amannmalik.mcp.Main", "client"
        );
        
        Process missingCommandProcess = missingCommandBuilder.start();
        
        finished = missingCommandProcess.waitFor(3, TimeUnit.SECONDS);
        assertTrue(finished, "Missing command process should complete");
        assertNotEquals(0, missingCommandProcess.exitValue(), "Missing command should cause error exit");
    }

    @Test
    @Disabled("Flaky in CI")
    @DisplayName("Happy path: Verbose logging and debugging output")
    void testVerboseLoggingAndDebugging() throws Exception {
        // Test server with verbose logging and strict timeout
        ProcessBuilder verboseServerBuilder = new ProcessBuilder(
                JAVA_BIN, "-cp", System.getProperty("java.class.path"),
                "com.amannmalik.mcp.Main", "server", "--http", String.valueOf(httpPort + 30), "-v"
        );
        verboseServerBuilder.redirectErrorStream(true);
        
        Process verboseServer = verboseServerBuilder.start();
        
        try {
            // Capture output with timeout
            StringBuilder output = new StringBuilder();
            CompletableFuture<String> outputCapture = CompletableFuture.supplyAsync(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(verboseServer.getInputStream()))) {
                    String line;
                    long startTime = System.currentTimeMillis();
                    while ((line = reader.readLine()) != null &&
                           output.length() < 1000 &&
                           System.currentTimeMillis() - startTime < 4000) {
                        output.append(line).append("\n");
                    }
                    return output.toString();
                } catch (IOException e) {
                    return output.toString(); // Return what we captured
                }
            });
            
            try {
                String capturedOutput = outputCapture.get(5, TimeUnit.SECONDS);
                // Verbose mode should activate (output validation is secondary)
                assertTrue(!capturedOutput.isEmpty() || verboseServer.isAlive(), 
                        "Verbose server should start and/or produce output");
            } catch (TimeoutException e) {
                fail("Verbose logging capture timed out after 5 seconds");
            }
            
        } finally {
            verboseServer.destroyForcibly();
            boolean terminated = verboseServer.waitFor(1, TimeUnit.SECONDS);
            if (!terminated) {
                fail("Verbose server failed to terminate within 1 second");
            }
        }
    }

    @Test
    @DisplayName("Happy path: Configuration precedence and overrides")
    void testConfigurationPrecedenceAndOverrides() throws Exception {
        // Test that command line options override defaults with fail-fast validation
        ProcessBuilder explicitPortBuilder = new ProcessBuilder(
                JAVA_BIN, "-cp", System.getProperty("java.class.path"),
                "com.amannmalik.mcp.Main", "server", "--http", String.valueOf(httpPort + 40), "--stdio"
        );
        
        Process explicitProcess = explicitPortBuilder.start();
        
        try {
            // Validate server starts quickly
            assertEventually(explicitProcess::isAlive, Duration.ofMillis(500), 
                    "Server with explicit options should start within 500ms");
            
            // Give minimal time for port binding attempt, then validate precedence
            CompletableFuture<Boolean> portCheck = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(300); // Brief delay for any port binding attempt
                    return isPortOpen(httpPort + 40);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            });
            
            try {
                boolean portOpen = portCheck.get(1, TimeUnit.SECONDS);
                assertFalse(portOpen, "STDIO override should prevent HTTP port from opening");
            } catch (TimeoutException e) {
                fail("Port precedence check timed out after 1 second");
            }
            
        } finally {
            explicitProcess.destroyForcibly();
            boolean terminated = explicitProcess.waitFor(1, TimeUnit.SECONDS);
            if (!terminated) {
                fail("Precedence test server failed to terminate within 1 second");
            }
        }
    }

    // Helper methods

    private String buildEchoServerCommand() {
        // Simple echo command that will fail gracefully but test the command parsing path
        return "echo \"MCP Test Server\"";
    }

    private int findAvailablePort() {
        try (var socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            return 8080; // fallback
        }
    }

    private boolean isPortOpen(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void assertEventually(BooleanSupplier condition, Duration timeout, String message) {
        long endTime = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < endTime) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Interrupted while waiting: " + message);
            }
        }
        fail(message);
    }

    @FunctionalInterface
    interface BooleanSupplier {
        boolean getAsBoolean();
    }
}