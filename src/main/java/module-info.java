module mcp.main {
    requires transitive info.picocli;
    requires transitive jakarta.json;
    requires jakarta.servlet;
    requires java.net.http;
    requires org.eclipse.jetty.ee10.servlet;
    requires org.eclipse.jetty.server;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.pkix;
    exports com.amannmalik.mcp.cli;
    exports com.amannmalik.mcp.api;
    exports com.amannmalik.mcp.spi;
    exports com.amannmalik.mcp.api.config;
    uses com.amannmalik.mcp.spi.ResourceProvider;
    uses com.amannmalik.mcp.spi.ToolProvider;
    uses com.amannmalik.mcp.spi.PromptProvider;
    uses com.amannmalik.mcp.spi.CompletionProvider;
    uses com.amannmalik.mcp.spi.SamplingProvider;
    uses com.amannmalik.mcp.spi.RootsProvider;
    uses com.amannmalik.mcp.spi.ElicitationProvider;
}
