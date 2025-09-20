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
import java.util.Objects;
import java.util.Optional;

final class AuthorizationUtil {
    private AuthorizationUtil() {
    }

    static Optional<Principal> authorize(AuthorizationManager manager,
                                         HttpServletRequest req,
                                         HttpServletResponse resp,
                                         String resourceMetadataUrl,
                                         Principal defaultPrincipal) throws IOException {
        Objects.requireNonNull(req, "req");
        Objects.requireNonNull(resp, "resp");
        Objects.requireNonNull(defaultPrincipal, "defaultPrincipal");

        if (manager == null) {
            return Optional.of(defaultPrincipal);
        }
        try {
            var header = req.getHeader(TransportHeaders.AUTHORIZATION);
            return Optional.of(manager.authorize(header));
        } catch (AuthorizationException e) {
            handleAuthorizationFailure(resp, resourceMetadataUrl, e);
            return Optional.empty();
        }
    }

    static void checkUnauthorized(HttpResponse<InputStream> response) throws IOException {
        Objects.requireNonNull(response, "response");
        if (response.statusCode() == HttpServletResponse.SC_UNAUTHORIZED) {
            var header = response.headers().firstValue(TransportHeaders.WWW_AUTHENTICATE).orElse("");
            var body = response.body();
            if (body != null) {
                body.close();
            }
            throw new UnauthorizedException(header);
        }
    }

    private static void handleAuthorizationFailure(HttpServletResponse resp,
                                                   String resourceMetadataUrl,
                                                   AuthorizationException failure) throws IOException {
        if (shouldAdvertiseMetadata(resourceMetadataUrl, failure.status())) {
            resp.setHeader(TransportHeaders.WWW_AUTHENTICATE, "Bearer resource=" + resourceMetadataUrl);
        }
        resp.sendError(failure.status());
    }

    private static boolean shouldAdvertiseMetadata(String resourceMetadataUrl, int status) {
        return status == HttpServletResponse.SC_UNAUTHORIZED
                && resourceMetadataUrl != null
                && !resourceMetadataUrl.isBlank();
    }
}
