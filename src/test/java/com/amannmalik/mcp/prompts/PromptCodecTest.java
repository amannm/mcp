package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.server.resources.Resource;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PromptCodecTest {
    @Test
    void instanceSerialization() {
        Resource r = new Resource("file:///a.txt", "a.txt", "A", null, "text/plain", 5L, null);
        PromptInstance inst = new PromptInstance("desc", List.of(
                new PromptMessage(Role.USER, new PromptContent.Text("hi")),
                new PromptMessage(Role.ASSISTANT, new PromptContent.ResourceContent(r))
        ));
        JsonObject json = PromptCodec.toJsonObject(inst);
        assertEquals(2, json.getJsonArray("messages").size());
        assertEquals("hi", json.getJsonArray("messages").getJsonObject(0).getJsonObject("content").getString("text"));
        JsonObject resJson = json.getJsonArray("messages").getJsonObject(1).getJsonObject("content").getJsonObject("resource");
        assertEquals("file:///a.txt", resJson.getString("uri"));
    }
}
