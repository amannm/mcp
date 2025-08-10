package com.amannmalik.mcp.test;

import org.junit.platform.suite.api.*;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("com/amannmalik/mcp/test")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.amannmalik.mcp.test")
public class McpConformanceSuite {
}