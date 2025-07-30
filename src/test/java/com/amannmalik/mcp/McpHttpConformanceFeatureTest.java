package com.amannmalik.mcp;

import io.cucumber.junit.platform.engine.Cucumber;

@Cucumber
public class McpHttpConformanceFeatureTest {
    static {
        System.setProperty("mcp.test.transport", "http");
    }
}
