import com.amannmalik.mcp.auth.AuthorizationManager;
import com.amannmalik.mcp.auth.AuthorizationStrategy;
import com.amannmalik.mcp.transport.StreamableHttpTransport;
import com.amannmalik.mcp.security.OriginValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StreamableHttpTransportAuthTest {
    private StreamableHttpTransport transport;

    @AfterEach
    void cleanup() throws Exception {
        if (transport != null) {
            transport.close();
        }
    }

    @Test
    void unauthorizedIncludesWwwAuthenticateHeader() throws Exception {
        AuthorizationStrategy strat = header -> java.util.Optional.empty();
        AuthorizationManager auth = new AuthorizationManager(List.of(strat));
        OriginValidator validator = new OriginValidator(Set.of("http://localhost"));
        String metaUrl = "https://example.com/.well-known/oauth-protected-resource";
        transport = new StreamableHttpTransport(0, validator, auth, metaUrl);

        HttpClient client = HttpClient.newHttpClient();
        String body = "{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":1}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://127.0.0.1:" + transport.port() + "/"))
                .header("Accept", "application/json,text/event-stream")
                .header("Origin", "http://localhost")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, resp.statusCode());
        assertEquals("Bearer resource_metadata=\"" + metaUrl + "\"", resp.headers().firstValue("WWW-Authenticate").orElse(null));
    }
}
