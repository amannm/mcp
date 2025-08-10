module mcp.main {
    requires info.picocli;
    requires jakarta.json;
    requires jakarta.servlet;
    requires java.net.http;
    requires org.eclipse.jetty.ee10.servlet;
    requires org.eclipse.jetty.server;
    exports com.amannmalik.mcp;
    exports com.amannmalik.mcp.util;
    exports com.amannmalik.mcp.core;
    exports com.amannmalik.mcp.jsonrpc;
    exports com.amannmalik.mcp.elicitation;
    exports com.amannmalik.mcp.roots;
    exports com.amannmalik.mcp.sampling;
}