package com.amannmalik.mcp.test;

import com.amannmalik.mcp.api.*;
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;

import java.util.*;
import java.util.stream.Collectors;

public final class ProtocolLifecycleSteps {
    private final Set<ClientCapability> clientCapabilities = EnumSet.noneOf(ClientCapability.class);
    private final Set<ServerCapability> serverCapabilities = EnumSet.noneOf(ServerCapability.class);
    private final Set<ServerCapability> availableFeatures = EnumSet.noneOf(ServerCapability.class);
    private final Set<ServerCapability> unavailableFeatures = EnumSet.noneOf(ServerCapability.class);
    private final Set<String> serverVersions = new LinkedHashSet<>();
    private final Set<RequestId> usedIds = new HashSet<>();
    private RequestId lastRequestId = RequestId.parse("noop");
    private RequestId lastResponseId = RequestId.parse("noop");
    private String lastNotificationMethod = "";
    private String clientVersion = "";
    private String negotiatedVersion = "";
    private boolean transportAvailable;
    private boolean operational;

    @Given("a clean MCP environment")
    public void cleanEnvironment() {
        clientCapabilities.clear();
        serverCapabilities.clear();
        availableFeatures.clear();
        unavailableFeatures.clear();
        serverVersions.clear();
        usedIds.clear();
        clientVersion = "";
        negotiatedVersion = "";
        transportAvailable = false;
        operational = false;
    }

    @Given("valid JSON-RPC transport is available")
    public void validTransport() {
        transportAvailable = true;
    }

    @Given("a client with protocol version {string}")
    public void clientWithProtocolVersion(String version) {
        clientVersion = version;
    }

    @Given("client capabilities include {string}")
    @Given("a client declaring {string}")
    public void clientCapabilities(String caps) {
        clientCapabilities.addAll(parseClientCapabilities(caps));
    }

    @Given("a server supporting versions {string} and {string}")
    public void serverSupportingVersions(String v1, String v2) {
        serverVersions.addAll(List.of(v1, v2));
    }

    @Given("a server declaring {string}")
    public void serverDeclares(String caps) {
        serverCapabilities.addAll(parseServerCapabilities(caps));
    }

    @When("client sends initialize request")
    public void clientSendsInitializeRequest() {
        // TODO: send initialize
        throw new UnsupportedOperationException("TODO");
    }

    @Then("server responds with compatible protocol version")
    public void serverRespondsWithCompatibleProtocolVersion() {
        // TODO: verify version
        throw new UnsupportedOperationException("TODO");
    }

    @Then("server declares supported capabilities")
    public void serverDeclaresSupportedCapabilities() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("server provides implementation info")
    public void serverProvidesImplementationInfo() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @When("client sends initialized notification")
    public void clientSendsInitializedNotification() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("connection enters operational state")
    public void connectionEntersOperationalState() {
        operational = true;
    }

    @Then("both parties can exchange messages")
    public void bothPartiesCanExchangeMessages() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @When("client requests version {string}")
    public void clientRequestsVersion(String version) {
        negotiatedVersion = version;
    }

    @Then("server accepts requested version")
    public void serverAcceptsRequestedVersion() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @When("client requests unsupported version {string}")
    public void clientRequestsUnsupportedVersion(String version) {
        negotiatedVersion = version;
    }

    @Then("server responds with latest supported version")
    public void serverRespondsWithLatestSupportedVersion() {
        negotiatedVersion = Protocol.LATEST_VERSION;
    }

    @Then("client should decide on compatibility")
    public void clientShouldDecideOnCompatibility() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @When("initialization completes")
    public void initializationCompletes() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("available features are {string}")
    public void availableFeaturesAre(String features) {
        availableFeatures.addAll(parseServerCapabilities(features));
    }

    @Then("unavailable features are {string}")
    public void unavailableFeaturesAre(String features) {
        unavailableFeatures.addAll(parseServerCapabilities(features));
    }

    @Given("an operational MCP connection")
    public void anOperationalMcpConnection() {
        operational = true;
    }

    @Given("an operational MCP connection with {word} transport")
    public void operationalConnectionWithTransport(String transport) {
        operational = true;
    }

    @Given("an operational MCP connection with {int}-second timeout")
    public void operationalConnectionWithTimeout(int seconds) {
        operational = true;
    }

    @When("client sends request with id {string}")
    public void clientSendsRequestWithId(String id) {
        RequestId rid = RequestId.parse(id);
        lastRequestId = rid;
        if (!usedIds.add(rid)) {
            Assertions.fail("id reused: " + id);
        }
    }

    @Then("request includes jsonrpc \"2.0\", method, and id")
    public void requestIncludesJsonrpcMethodAndId() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("id is not null")
    public void idIsNotNull() {
        Assertions.assertFalse(lastRequestId instanceof RequestId.NullId);
    }

    @Then("id has not been used before")
    public void idHasNotBeenUsedBefore() {
        // already checked when sending
    }

    @When("server responds")
    public void serverResponds() {
        lastResponseId = lastRequestId;
    }

    @Then("response includes matching id {string}")
    public void responseIncludesMatchingId(String id) {
        Assertions.assertEquals(id, lastResponseId.toString());
    }

    @Then("response has either result or error, not both")
    public void responseHasEitherResultOrError() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @When("server sends notification")
    public void serverSendsNotification() {
        lastNotificationMethod = "method";
    }

    @Then("notification lacks id field")
    public void notificationLacksIdField() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("notification includes method and optional params")
    public void notificationIncludesMethodAndOptionalParams() {
        Assertions.assertFalse(lastNotificationMethod.isEmpty());
    }

    @When("client sends malformed request")
    public void clientSendsMalformedRequest() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("server returns parse error (-32700)")
    public void serverReturnsParseError() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @When("client sends invalid method request")
    public void clientSendsInvalidMethodRequest() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("server returns method not found (-32601)")
    public void serverReturnsMethodNotFound() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @When("client sends request with invalid params")
    public void clientSendsRequestWithInvalidParams() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("server returns invalid params (-32602)")
    public void serverReturnsInvalidParams() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @When("server encounters internal error")
    public void serverEncountersInternalError() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("server returns internal error (-32603)")
    public void serverReturnsInternalError() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @When("client closes input stream to server")
    public void clientClosesInputStreamToServer() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @When("waits for server to exit gracefully")
    public void waitsForServerToExitGracefully() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("connection terminates cleanly")
    public void connectionTerminatesCleanly() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @When("client closes HTTP connection")
    public void clientClosesHttpConnection() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @When("client sends request with _meta field")
    public void clientSendsRequestWithMetaField() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("server preserves _meta without assumptions")
    public void serverPreservesMetaWithoutAssumptions() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @When("server sends response with reserved _meta prefix")
    public void serverSendsResponseWithReservedMetaPrefix() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("client handles MCP-reserved keys appropriately")
    public void clientHandlesMcpReservedKeysAppropriately() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @When("server sends _meta with custom prefix")
    public void serverSendsMetaWithCustomPrefix() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("client treats as implementation-specific")
    public void clientTreatsAsImplementationSpecific() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @When("client sends request that takes {int} seconds")
    public void clientSendsRequestThatTakesSeconds(int seconds) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("client issues cancellation notification")
    public void clientIssuesCancellationNotification() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("client stops waiting for response")
    public void clientStopsWaitingForResponse() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @When("client sends request with progress notifications")
    public void clientSendsRequestWithProgressNotifications() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("timeout may be extended based on progress")
    public void timeoutMayBeExtendedBasedOnProgress() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("maximum timeout is still enforced")
    public void maximumTimeoutIsStillEnforced() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @When("server only supports incompatible versions")
    public void serverOnlySupportsIncompatibleVersions() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("server returns unsupported protocol error")
    public void serverReturnsUnsupportedProtocolError() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("includes supported versions in error data")
    public void includesSupportedVersionsInErrorData() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @When("required capabilities cannot be negotiated")
    public void requiredCapabilitiesCannotBeNegotiated() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("initialization fails with capability error")
    public void initializationFailsWithCapabilityError() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @When("client sends requests before initialized notification")
    public void clientSendsRequestsBeforeInitializedNotification() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @Then("server may reject or queue non-ping requests")
    public void serverMayRejectOrQueueNonPingRequests() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    private Set<ClientCapability> parseClientCapabilities(String raw) {
        if (raw.equals("none")) return EnumSet.noneOf(ClientCapability.class);
        if (raw.equals("all")) return EnumSet.allOf(ClientCapability.class);
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(ClientCapability::from)
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ClientCapability.class)));
    }

    private Set<ServerCapability> parseServerCapabilities(String raw) {
        if (raw.equals("none")) return EnumSet.noneOf(ServerCapability.class);
        if (raw.equals("all")) return EnumSet.allOf(ServerCapability.class);
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(ServerCapability::from)
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ServerCapability.class)));
    }
}

