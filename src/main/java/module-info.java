module mcp.main {
    requires info.picocli;
    requires jakarta.json;
    requires jakarta.servlet;
    requires java.net.http;
    requires org.eclipse.jetty.ee10.servlet;
    requires org.eclipse.jetty.server;
    exports com.amannmalik.mcp.cli;
    exports com.amannmalik.mcp.api;
    exports com.amannmalik.mcp.completion;
    exports com.amannmalik.mcp.util;
}