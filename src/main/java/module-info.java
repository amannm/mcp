module mcp.main {
    requires transitive info.picocli;
    requires transitive jakarta.json;
    requires jakarta.servlet;
    requires java.net.http;
    requires org.eclipse.jetty.ee10.servlet;
    requires org.eclipse.jetty.server;
    exports com.amannmalik.mcp.cli;
    exports com.amannmalik.mcp.api;
    exports com.amannmalik.mcp.spi;
}