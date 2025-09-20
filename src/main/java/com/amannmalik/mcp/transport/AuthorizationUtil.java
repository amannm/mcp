package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.auth.AuthorizationException;
import com.amannmalik.mcp.auth.AuthorizationManager;
import com.amannmalik.mcp.core.UnauthorizedException;
import com.amannmalik.mcp.spi.Principal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.Optional;

final class AuthorizationUtil {
    private AuthorizationUtil() {
    }

    static Optional<Principal> authorize(AuthorizationManager manager,
                                         HttpServletRequest req,
                                         HttpServletResponse resp,
                                         String resourceMetadataUrl,
                                         Principal defaultPrincipal) throws IOException {
        if (manager == null) {
            return Optional.of(defaultPrincipal);
        }
        try {
            return Optional.of(manager.authorize(req.getHeader(TransportHeaders.AUTHORIZATION)));
        } catch (AuthorizationException e) {
            if (resourceMetadataUrl != null
                    && !resourceMetadataUrl.isBlank()
                    && e.status() == HttpServletResponse.SC_UNAUTHORIZED) {
                resp.setHeader(TransportHeaders.WWW_AUTHENTICATE, "Bearer resource=" + resourceMetadataUrl);
            }
            resp.sendError(e.status());
            return Optional.empty();
        }
    }

    static void checkUnauthorized(HttpResponse<InputStream> response) throws IOException {
        if (response.statusCode() == HttpServletResponse.SC_UNAUTHORIZED) {
            var header = response.headers().firstValue(TransportHeaders.WWW_AUTHENTICATE).orElse("");
            response.body().close();
            throw new UnauthorizedException(header);
        }
    }
}
