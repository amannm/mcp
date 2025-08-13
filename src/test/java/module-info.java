module mcp.test {
    requires mcp.main;
    requires io.cucumber.java;
    requires io.cucumber.junit.platform.engine;
    requires org.junit.platform.suite.api;

    exports com.amannmalik.mcp.test;
    opens com.amannmalik.mcp.test to io.cucumber.junit.platform.engine;
}
