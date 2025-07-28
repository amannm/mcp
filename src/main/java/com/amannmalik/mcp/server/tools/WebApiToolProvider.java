package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.validation.SchemaValidator;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/** Minimal ToolProvider performing HTTP GET requests. */
public final class WebApiToolProvider implements ToolProvider {
    private final Tool tool;
    private final HttpClient client = HttpClient.newHttpClient();

    public WebApiToolProvider() {
        JsonObject schema = Json.createObjectBuilder()
                .add("type", "object")
                .add("properties", Json.createObjectBuilder().add("url", Json.createObjectBuilder().add("type", "string")))
                .add("required", Json.createArrayBuilder().add("url"))
                .build();
        this.tool = new Tool("http_get", "HTTP GET", "Fetch URL", schema, null, null);
    }

    @Override
    public ToolPage list(String cursor) {
        return new ToolPage(List.of(tool), null);
    }

    @Override
    public ToolResult call(String name, JsonObject arguments) {
        if (!tool.name().equals(name)) throw new IllegalArgumentException("Unknown tool");
        SchemaValidator.validate(tool.inputSchema(), arguments);
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(arguments.getString("url"))).GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            JsonArray content = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder().add("type", "text").add("text", resp.body()).build())
                    .build();
            return new ToolResult(content, null, false);
        } catch (IOException | InterruptedException e) {
            return new ToolResult(JsonValue.EMPTY_JSON_ARRAY, null, true);
        }
    }
}
