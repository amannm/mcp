open module mcp.test {
    requires mcp.main;
    requires io.cucumber.java;
    requires io.cucumber.datatable;
    requires io.cucumber.junit.platform.engine;
    requires org.junit.platform.suite.api;
    requires org.junit.jupiter.api;
    requires jakarta.json;
    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    requires java.net.http;
    provides com.amannmalik.mcp.spi.ResourceProvider with com.amannmalik.mcp.test.impl.DefaultResourceProvider;
    provides com.amannmalik.mcp.spi.ToolProvider with com.amannmalik.mcp.test.impl.DefaultToolProvider;
    provides com.amannmalik.mcp.spi.PromptProvider with com.amannmalik.mcp.test.impl.DefaultPromptProvider;
    provides com.amannmalik.mcp.spi.CompletionProvider with com.amannmalik.mcp.test.impl.DefaultCompletionProvider;
    provides com.amannmalik.mcp.spi.SamplingProvider with com.amannmalik.mcp.test.impl.DefaultSamplingProvider;
    provides com.amannmalik.mcp.spi.RootsProvider with com.amannmalik.mcp.test.impl.DefaultRootsProvider;
    provides com.amannmalik.mcp.spi.ElicitationProvider with com.amannmalik.mcp.test.impl.DefaultElicitationProvider;
}
