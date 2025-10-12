package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.codec.ResourceMetadataJsonCodec;
import com.amannmalik.mcp.core.ResourceMetadata;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.Serial;
import java.util.Objects;

final class MetadataServlet extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 133742069L;
    private static final ResourceMetadataJsonCodec METADATA_CODEC = new ResourceMetadataJsonCodec();
    private transient final StreamableHttpServerTransport transport;

    MetadataServlet(StreamableHttpServerTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!transport.enforceHttps(req, resp)) {
            return;
        }
        var meta = new ResourceMetadata(transport.canonicalResource(), transport.authorizationServers());
        var body = METADATA_CODEC.toJson(meta);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(body.toString());
    }
}
