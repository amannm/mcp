package com.amannmalik.mcp;

import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.transport.*;
import com.amannmalik.mcp.config.McpConfiguration;
import com.amannmalik.mcp.roots.*;
import com.amannmalik.mcp.sampling.*;
import com.amannmalik.mcp.elicitation.*;
import com.amannmalik.mcp.util.RootChecker;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class RootsFeatureSteps {
    
    private McpServer server;
    private McpClient client;
    private Transport serverTransport;
    private Transport clientTransport;
    private InMemoryRootsProvider rootsProvider;
    private RootSecurityManager securityManager;
    
    private List<RootSecurityManager.RootConfig> configuredRoots = new ArrayList<>();
    private boolean rootListRequested = false;
    private List<Root> returnedRoots = null;
    private String accessPath;
    private boolean accessGranted = false;
    private boolean accessDenied = false;
    private boolean securityViolationLogged = false;
    private String lastNotification;

    @After
    public void cleanup() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                // Cleanup
            }
        }
        if (server != null) {
            try {
                server.close();
            } catch (Exception e) {
                // Cleanup
            }
        }
    }

    @Given("an MCP client with root management capability")
    public void anMcpClientWithRootManagementCapability() throws Exception {
        securityManager = new RootSecurityManager();
        
        PipedInputStream serverInput = new PipedInputStream();
        PipedOutputStream serverOutput = new PipedOutputStream();
        PipedInputStream clientInput = new PipedInputStream();
        PipedOutputStream clientOutput = new PipedOutputStream();
        
        serverOutput.connect(clientInput);
        clientOutput.connect(serverInput);
        
        serverTransport = new StdioTransport(serverInput, serverOutput);
        server = createServerWithRootsCapability(serverTransport);
        
        Thread serverThread = new Thread(() -> {
            try {
                server.serve();
            } catch (Exception e) {
                fail(e);
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(1000);
        
        clientTransport = new StdioTransport(clientInput, clientOutput);
        rootsProvider = new InMemoryRootsProvider(new ArrayList<>());
        client = createClientWithRootsCapability(clientTransport);
    }

    @And("configured roots:")
    public void configuredRoots(DataTable rootsTable) {
        configuredRoots.clear();
        List<Root> roots = new ArrayList<>();
        
        List<Map<String, String>> rows = rootsTable.asMaps(String.class, String.class);
        for (Map<String, String> row : rows) {
            String uri = row.get("uri");
            String name = row.get("name");
            String permissions = row.get("permissions");
            
            RootSecurityManager.RootConfig config = new RootSecurityManager.RootConfig(uri, name, permissions);
            configuredRoots.add(config);
            
            Root root = new Root(uri, name, null);
            roots.add(root);
        }
        
        securityManager.configure(configuredRoots);
        for (Root root : roots) {
            rootsProvider.add(root);
        }
    }

    @When("the server requests root list")
    public void theServerRequestsRootList() throws Exception {
        client.connect();
        Thread.sleep(500);
        
        returnedRoots = server.listRoots();
        rootListRequested = true;
    }

    @Then("the client returns available roots with proper URIs")
    public void theClientReturnsAvailableRootsWithProperUris() {
        assertTrue(rootListRequested, "Root list should have been requested");
        assertNotNull(returnedRoots, "Roots should be returned");
        assertFalse(returnedRoots.isEmpty(), "Root list should not be empty");
        
        for (Root root : returnedRoots) {
            assertNotNull(root.uri(), "Root URI should not be null");
            assertTrue(root.uri().startsWith("file://"), "Root URI should be a file URI");
        }
    }

    @And("each root includes human-readable names")
    public void eachRootIncludesHumanReadableNames() {
        assertNotNull(returnedRoots, "Roots should be returned");
        
        for (Root root : returnedRoots) {
            assertNotNull(root.name(), "Root name should not be null");
            assertFalse(root.name().trim().isEmpty(), "Root name should not be empty");
        }
    }

    @When("the server attempts to access {string}")
    public void theServerAttemptsToAccess(String path) {
        this.accessPath = path;
        boolean withinRoots = RootChecker.withinRoots(path, returnedRoots);
        boolean securityAllowed = securityManager.checkAccess(path);
        
        if (withinRoots && securityAllowed) {
            accessGranted = true;
            accessDenied = false;
        } else {
            accessGranted = false;
            accessDenied = true;
            securityViolationLogged = securityManager.violationLogged();
        }
    }

    @Then("access is granted as path is within allowed root")
    public void accessIsGrantedAsPathIsWithinAllowedRoot() {
        assertTrue(accessGranted, "Access should be granted for path within allowed root: " + accessPath);
        assertFalse(accessDenied, "Access should not be denied for allowed path");
    }

    @Then("access is denied as path is outside allowed roots")
    public void accessIsDeniedAsPathIsOutsideAllowedRoots() {
        assertTrue(accessDenied, "Access should be denied for path outside allowed roots: " + accessPath);
        assertFalse(accessGranted, "Access should not be granted for disallowed path");
    }

    @And("security violation is logged")
    public void securityViolationIsLogged() {
        assertTrue(securityViolationLogged, "Security violation should be logged");
    }

    @When("root configuration changes \\(new project added)")
    public void rootConfigurationChangesNewProjectAdded() {
        RootSecurityManager.RootConfig newRoot = new RootSecurityManager.RootConfig(
            "file:///home/user/project3", "New Project", "read"
        );
        
        securityManager.addRoot(newRoot);
        
        Root root = new Root(newRoot.uri(), newRoot.name(), null);
        rootsProvider.add(root);
        
        lastNotification = securityManager.lastNotification();
    }

    @Then("{string} is sent to server")
    public void notificationIsSentToServer(String expectedNotification) {
        assertEquals(expectedNotification, lastNotification, 
            "Expected notification should be sent");
    }

    @When("server refreshes root list")
    public void serverRefreshesRootList() {
        try {
            returnedRoots = server.listRoots();
        } catch (Exception e) {
            fail(e);
        }
    }

    @Then("updated roots are returned")
    public void updatedRootsAreReturned() {
        assertNotNull(returnedRoots, "Updated roots should be returned");
        
        boolean newProjectFound = returnedRoots.stream()
            .anyMatch(root -> "file:///home/user/project3".equals(root.uri()) 
                           && "New Project".equals(root.name()));
        
        assertTrue(newProjectFound, "New project should be in updated root list");
    }

    private McpServer createServerWithRootsCapability(Transport transport) {
        return new McpServer(transport, null);
    }

    private McpClient createClientWithRootsCapability(Transport transport) {
        Set<ClientCapability> capabilities = EnumSet.of(ClientCapability.ROOTS);
        
        McpConfiguration config = McpConfiguration.current();
        ClientInfo info = new ClientInfo(
            config.clientName(),
            config.clientDisplayName(),
            config.clientVersion()
        );
        
        return new McpClient(
            info,
            capabilities,
            transport,
            null,
            rootsProvider,
            null,
            null
        );
    }

}