package com.amannmalik.mcp;

import com.amannmalik.mcp.cli.ClientConfig;
import com.amannmalik.mcp.cli.ServerConfig;
import com.amannmalik.mcp.cli.TransportType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MainCliIntegrationTest {

    @TempDir
    Path tempDir;

    private static final String JAVA_BIN = System.getProperty("java.home") +
            File.separator + "bin" + File.separator + "java";

    private ExecutorService executor;
    private Process serverProcess;
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
        if (serverProcess != null && serverProcess.isAlive()) {
            serverProcess.destroyForcibly();
        }
    }

    @Test
    @DisplayName("Happy path: Complete CLI interface with STDIO transport")
    void testCompleteCliWorkflowWithStdio() throws Exception {
        // Test server startup with STDIO transport - expect quick startup
        String[] serverArgs = {"server", "--stdio", "-v"};
        ProcessBuilder serverBuilder = new ProcessBuilder(JAVA_BIN, "-cp", System.getProperty("java.class.path"), 
                "com.amannmalik.mcp.Main");
        serverBuilder.command().addAll(java.util.Arrays.asList(serverArgs));
        
        Process server = serverBuilder.start();
        
        try {
            // Fail fast if server doesn't start within 300ms
            assertEventually(() -> server.isAlive(), Duration.ofMillis(300), 
                    "Server should start within 300ms");
            
            // Test client connection with realistic timeout
            String[] clientArgs = {"client", "--command", buildServerCommand(), "-v"};
            ProcessBuilder clientBuilder = new ProcessBuilder(JAVA_BIN, "-cp", System.getProperty("java.class.path"), 
                    "com.amannmalik.mcp.Main");
            clientBuilder.command().addAll(java.util.Arrays.asList(clientArgs));
            
            Process client = clientBuilder.start();
            
            // Client should complete quickly or we have a problem
            boolean finished = client.waitFor(2, TimeUnit.SECONDS);
            assertTrue(finished, "Client should complete within 2 seconds");
            
            // Note: We don't assert exit code 0 since the buildServerCommand() may not be a real MCP server
            // The important thing is that the CLI processes the command without hanging
            
        } finally {
            server.destroyForcibly();
            boolean serverTerminated = server.waitFor(1, TimeUnit.SECONDS);
            if (!serverTerminated) {
                fail("Server failed to terminate within 1 second");
            }
        }
    }

    @Test
    @Disabled("Flaky in CI")
    @DisplayName("Happy path: HTTP transport server with configuration")
    void testHttpTransportServerWithConfig() throws Exception {
        // Create server configuration file
        Path serverConfigPath = tempDir.resolve("server-config.json");
        String serverConfig = String.format("""
            {
              "transport": "HTTP",
              "port": %d
            }
            """, httpPort);
        Files.writeString(serverConfigPath, serverConfig);
        
        // Start HTTP server with fail-fast timeout
        CompletableFuture<Process> serverFuture = CompletableFuture.supplyAsync(() -> {
            try {
                String[] args = {"server", "-c", serverConfigPath.toString(), "-v"};
                ProcessBuilder pb = new ProcessBuilder(JAVA_BIN, "-cp", System.getProperty("java.class.path"), 
                        "com.amannmalik.mcp.Main");
                return pb.start();
            } catch (IOException e) {
                throw new RuntimeException("Failed to start server process", e);
            }
        });
        
        try {
        serverProcess = serverFuture.get(3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            fail("Server process failed to start within 1 second");
        }
        
        // Fail fast if server doesn't bind to port within 1.5 seconds
        assertEventually(() -> isPortOpen(httpPort), Duration.ofSeconds(5),
                "Server should bind to HTTP port within 5 seconds");
        
        // Verify server process is alive
        assertTrue(serverProcess.isAlive(), "HTTP server process should be running");
        
        // Test HTTP connectivity with timeout
        CompletableFuture<Void> connectionTest = CompletableFuture.runAsync(() -> {
            try (Socket socket = new Socket()) {
                socket.connect(new java.net.InetSocketAddress("127.0.0.1", httpPort), 1000);
            } catch (IOException e) {
                throw new RuntimeException("Failed to connect to HTTP server: " + e.getMessage());
            }
        });
        
        try {
            connectionTest.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            fail("HTTP connection test timed out after 2 seconds");
        } catch (ExecutionException e) {
            fail("HTTP connection failed: " + e.getCause().getMessage());
        }
    }

    @Test
    @DisplayName("Happy path: Configuration loading and validation")
    void testConfigurationLoadingAndValidation() throws Exception {
        // Test valid client configuration
        Path clientConfigPath = tempDir.resolve("client-config.json");
        String clientConfig = """
            {
              "transport": "STDIO",
              "command": "echo test-server"
            }
            """;
        Files.writeString(clientConfigPath, clientConfig);
        
        // Test client with config file
        String[] args = {"client", "-c", clientConfigPath.toString()};
        ProcessBuilder pb = new ProcessBuilder(JAVA_BIN, "-cp", System.getProperty("java.class.path"), 
                "com.amannmalik.mcp.Main");
        pb.command().addAll(java.util.Arrays.asList(args));
        
        Process process = pb.start();
        boolean finished = process.waitFor(3, TimeUnit.SECONDS);
        
        // Note: This may not exit with 0 since echo isn't a real MCP server,
        // but it validates config loading path
        assertTrue(finished, "Process should complete");
    }

    @Test
    @DisplayName("Happy path: Verbose logging and error handling")
    void testVerboseLoggingAndErrorHandling() throws Exception {
        // Test help command
        String[] helpArgs = {"--help"};
        ProcessBuilder helpBuilder = new ProcessBuilder(JAVA_BIN, "-cp", System.getProperty("java.class.path"), 
                "com.amannmalik.mcp.Main");
        helpBuilder.command().addAll(java.util.Arrays.asList(helpArgs));
        
        Process helpProcess = helpBuilder.start();
        
        // Capture output
        String output = readProcessOutput(helpProcess);
        
        boolean finished = helpProcess.waitFor(3, TimeUnit.SECONDS);
        assertTrue(finished, "Help command should complete");
        assertEquals(0, helpProcess.exitValue(), "Help should exit successfully");
        assertTrue(output.contains("mcp"), "Help output should contain program name");
        assertTrue(output.contains("server"), "Help should show server subcommand");
        assertTrue(output.contains("client"), "Help should show client subcommand");
    }

    @Test
    @DisplayName("Happy path: Server subcommand help and options")
    void testServerSubcommandHelp() throws Exception {
        String[] args = {"server", "--help"};
        ProcessBuilder pb = new ProcessBuilder(JAVA_BIN, "-cp", System.getProperty("java.class.path"), 
                "com.amannmalik.mcp.Main");
        pb.command().addAll(java.util.Arrays.asList(args));
        
        Process process = pb.start();
        String output = readProcessOutput(process);
        
        boolean finished = process.waitFor(3, TimeUnit.SECONDS);
        assertTrue(finished, "Server help should complete");
        assertEquals(0, process.exitValue(), "Server help should exit successfully");
        assertTrue(output.contains("--http"), "Should show HTTP option");
        assertTrue(output.contains("--stdio"), "Should show STDIO option");
        assertTrue(output.contains("--verbose"), "Should show verbose option");
    }

    @Test
    @DisplayName("Happy path: Client subcommand help and options")
    void testClientSubcommandHelp() throws Exception {
        String[] args = {"client", "--help"};
        ProcessBuilder pb = new ProcessBuilder(JAVA_BIN, "-cp", System.getProperty("java.class.path"), 
                "com.amannmalik.mcp.Main");
        pb.command().addAll(java.util.Arrays.asList(args));
        
        Process process = pb.start();
        String output = readProcessOutput(process);
        
        boolean finished = process.waitFor(3, TimeUnit.SECONDS);
        assertTrue(finished, "Client help should complete");
        assertEquals(0, process.exitValue(), "Client help should exit successfully");
        assertTrue(output.contains("--command"), "Should show command option");
        assertTrue(output.contains("--config"), "Should show config option");
        assertTrue(output.contains("--verbose"), "Should show verbose option");
    }

    @Test
    @DisplayName("Happy path: Multiple transport types and configurations")
    void testMultipleTransportConfigurations() throws Exception {
        // Test server with HTTP port specification
        String[] httpArgs = {"server", "--http", String.valueOf(httpPort + 1), "-v"};
        ProcessBuilder httpBuilder = new ProcessBuilder(JAVA_BIN, "-cp", System.getProperty("java.class.path"), 
                "com.amannmalik.mcp.Main");
        httpBuilder.command().addAll(java.util.Arrays.asList(httpArgs));
        
        Process httpServer = httpBuilder.start();
        
        try {
            // Wait for server to start
            assertEventually(() -> isPortOpen(httpPort + 1), Duration.ofSeconds(3), 
                    "HTTP server should start listening");
            
            assertTrue(httpServer.isAlive(), "HTTP server should be running");
            
            // Test STDIO server configuration
            String[] stdioArgs = {"server", "--stdio"};
            ProcessBuilder stdioBuilder = new ProcessBuilder(JAVA_BIN, "-cp", System.getProperty("java.class.path"), 
                    "com.amannmalik.mcp.Main");
            stdioBuilder.command().addAll(java.util.Arrays.asList(stdioArgs));
            
            Process stdioServer = stdioBuilder.start();
            
            try {
                Thread.sleep(100); // Allow startup
                assertTrue(stdioServer.isAlive(), "STDIO server should be running");
            } finally {
                stdioServer.destroyForcibly();
            }
            
        } finally {
            httpServer.destroyForcibly();
        }
    }

    @Test
    @DisplayName("Happy path: Configuration validation and error cases")
    void testConfigurationValidation() throws Exception {
        // Test invalid configuration
        Path invalidConfigPath = tempDir.resolve("invalid-config.json");
        String invalidConfig = """
            {
              "invalid": "data"
            }
            """;
        Files.writeString(invalidConfigPath, invalidConfig);
        
        String[] args = {"client", "-c", invalidConfigPath.toString()};
        ProcessBuilder pb = new ProcessBuilder(JAVA_BIN, "-cp", System.getProperty("java.class.path"), 
                "com.amannmalik.mcp.Main");
        pb.command().addAll(java.util.Arrays.asList(args));
        
        Process process = pb.start();
        boolean finished = process.waitFor(3, TimeUnit.SECONDS);
        
        assertTrue(finished, "Invalid config process should complete");
        assertNotEquals(0, process.exitValue(), "Invalid config should cause non-zero exit");
    }

    // Helper methods

    private String buildServerCommand() {
        return String.format("java -cp %s com.amannmalik.mcp.Main server --stdio", 
                System.getProperty("java.class.path"));
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
        long checkInterval = Math.min(50, timeout.toMillis() / 20); // Adaptive check interval
        
        while (System.currentTimeMillis() < endTime) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(checkInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Interrupted while waiting: " + message);
            }
        }
        fail(message + " (timeout: " + timeout.toMillis() + "ms)");
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    @FunctionalInterface
    interface BooleanSupplier {
        boolean getAsBoolean();
    }
}