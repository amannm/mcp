module mcp.main {
    requires info.picocli;
    requires jakarta.json;
    requires jakarta.servlet;
    requires java.net.http;
    requires org.eclipse.jetty.ee10.servlet;
    requires org.eclipse.jetty.server;
    exports com.amannmalik.mcp.cli;
    exports com.amannmalik.mcp.api;
    exports com.amannmalik.mcp.api.prompt;
    exports com.amannmalik.mcp.api.resource;
    exports com.amannmalik.mcp.api.tools;
    exports com.amannmalik.mcp.api.sampling;
    exports com.amannmalik.mcp.api.completion;
    exports com.amannmalik.mcp.api.elicitation;
    exports com.amannmalik.mcp.api.roots;
}