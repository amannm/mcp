# KT.md - MCP Progress Tracking Test Failure

Problem Summary

The MCP progress tracking conformance test fails with a "Duplicate token" error (-32602) when requesting resource list with progress tracking.
Error:
```
JsonRpcError[id=3, error=ErrorDetail[code=-32602, message=Duplicate token: <uuid>, data=null]]
```

Failing Location: `McpConformanceSteps.java:442` in `requestResourceListWithProgress()`

## Root Cause Analysis

### The Issue

The test generates a fresh UUID token (UUID.randomUUID().toString()) but the server's ProgressManager rejects it as a duplicate, indicating server state is persisting between test scenarios.

### Key Evidence

1. Fresh Token Generation: Test creates new UUID token each time: McpConformanceSteps.java:437
2. Server Rejection: ProgressManager.register() throws "Duplicate token" from line 27
3. Test Isolation: Comment in feature file says "single instance to avoid token conflicts" - but it's not working

### Critical Files & Locations

Progress Token Processing Pipeline:
- ProgressManager.java:23-31 - register() method that throws the error
- ProgressManager.java:26-27 - Duplicate detection: putIfAbsent() returns non-null
- JsonRpcRequestProcessor.java:50 - Calls progressManager.register()
- McpConformanceSteps.java:435-443 - Test method that fails

Server Lifecycle:
- McpConformanceSteps.java:368-374 - Server setup per scenario
- McpConformanceSteps.java:357-366 - Cleanup logic (may be insufficient)
- McpServer.java - Contains single ProgressManager instance

### Most Likely Root Cause

Server state is NOT being reset between test scenarios. The ProgressManager instance in the server contains a ConcurrentHashMap<ProgressToken, Double> progress that retains tokens from
previous test runs.

### Why This Happens

1. Test runs multiple scenarios sequentially
2. Each scenario should get a fresh server, but the ProgressManager state persists
3. Previous scenarios may have used the same UUID (unlikely) OR
4. More likely: The server process/instance is being reused between scenarios

### Investigation Path for Successor

1. Check Server Instance Reuse

- File: McpConformanceSteps.java:368 (setupTransport)
- Question: Is the same server process/instance used across multiple test scenarios?
- Look for: Server creation vs reuse logic

2. Examine ProgressManager State Management

- File: ProgressManager.java:33-36 (release() method)
- Question: Is progress token cleanup working correctly?
- Test: Add logging to see if tokens are being properly released

3. Check Test Cleanup

- File: McpConformanceSteps.java:357-366 (cleanup() method)
- Question: Is cleanup being called between scenarios?
- Check: @After annotation timing vs Cucumber scenario lifecycle

### Quick Fix Options

Option 1: Force Server Reset (Safest)

Ensure each test scenario gets a completely fresh server instance:
```java
@Before // Add this annotation
public void freshServerSetup() {
// Force cleanup of any existing server state
cleanup();
}
```

Option 2: Clear ProgressManager State

Add a method to reset ProgressManager state:
```java
// In ProgressManager.java
public void reset() {
progress.clear();
tokens.clear();
}
```

Option 3: Make Tokens More Unique

Add test scenario context to token generation to guarantee uniqueness across all test runs.

Files to Modify (Likely)

1. McpConformanceSteps.java - Improve server lifecycle management
2. ProgressManager.java - Add state reset capability if needed
3. Consider: McpServer.java - Check if ProgressManager needs instance isolation

### Success Criteria

- Progress tracking test passes consistently
- No impact on other test scenarios
- Clean server state between test runs