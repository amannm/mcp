package com.amannmalik.mcp.api;

import com.amannmalik.mcp.core.ServerRuntime;
import com.amannmalik.mcp.spi.*;

import java.io.IOException;

public interface McpServer extends AutoCloseable {
    static McpServer create(McpServerConfiguration config,
                            ResourceProvider resources,
                            ToolProvider tools,
                            PromptProvider prompts,
                            CompletionProvider completions,
                            SamplingProvider sampling,
                            ResourceAccessPolicy resourceAccess,
                            ToolAccessPolicy toolAccessPolicy,
                            SamplingAccessPolicy samplingAccessPolicy,
                            Principal principal,
                            String instructions) throws Exception {
        return new ServerRuntime(
                config,
                resources,
                tools,
                prompts,
                completions,
                sampling,
                resourceAccess,
                toolAccessPolicy,
                samplingAccessPolicy,
                principal,
                instructions);
    }

    void serve() throws IOException;

    @Override
    void close() throws IOException;
}
