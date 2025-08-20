package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.codec.ResourceMetadataJsonCodec;
import com.amannmalik.mcp.core.ResourceMetadata;
import jakarta.servlet.http.*;

import java.io.IOException;

final class MetadataServlet extends HttpServlet {
    private final StreamableHttpServerTransport transport;

    MetadataServlet(StreamableHttpServerTransport transport) {
        this.transport = transport;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!transport.enforceHttps(req, resp)) {
            return;
        }
        var meta = new ResourceMetadata(transport.canonicalResource, transport.authorizationServers);
        var body = new ResourceMetadataJsonCodec().toJson(meta);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(body.toString());
    }
}

