package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.api.Principal;
import com.amannmalik.mcp.auth.AuthorizationException;
import com.amannmalik.mcp.auth.AuthorizationManager;
import com.amannmalik.mcp.core.UnauthorizedException;
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
        if (manager == null) return Optional.of(defaultPrincipal);
        try {
            return Optional.of(manager.authorize(req.getHeader("Authorization")));
        } catch (AuthorizationException e) {
            if (resourceMetadataUrl != null) {
                resp.setHeader("WWW-Authenticate", "Bearer resource_metadata=\"" + resourceMetadataUrl + "\"");
            }
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return Optional.empty();
        }
    }

    static void checkUnauthorized(HttpResponse<InputStream> response) throws IOException {
        if (response.statusCode() == 401) {
            String header = response.headers().firstValue("WWW-Authenticate").orElse("");
            response.body().close();
            throw new UnauthorizedException(header);
        }
    }
}
