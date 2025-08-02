package com.amannmalik.mcp.transport;

import jakarta.json.JsonObject;
import jakarta.servlet.http.*;

import java.io.IOException;

final class MetadataServlet extends HttpServlet {
    private final StreamableHttpTransport transport;

    MetadataServlet(StreamableHttpTransport transport) {
        this.transport = transport;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ResourceMetadata meta = new ResourceMetadata(transport.canonicalResource, transport.authorizationServers);
        JsonObject body = ResourceMetadataCodec.toJsonObject(meta);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(body.toString());
    }
}

