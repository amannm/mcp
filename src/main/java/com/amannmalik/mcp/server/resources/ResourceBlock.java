package com.amannmalik.mcp.server.resources;

public sealed interface ResourceBlock permits ResourceBlock.Text, ResourceBlock.Binary {
    String uri();
    String name();
    String title();
    String mimeType();
    ResourceAnnotations annotations();

    record Text(String uri, String name, String title, String mimeType, String text, ResourceAnnotations annotations) implements ResourceBlock {
        public Text {
            if (uri == null || name == null || text == null) {
                throw new IllegalArgumentException("uri, name and text are required");
            }
        }
    }

    record Binary(String uri, String name, String title, String mimeType, byte[] blob, ResourceAnnotations annotations) implements ResourceBlock {
        public Binary {
            if (uri == null || name == null || blob == null) {
                throw new IllegalArgumentException("uri, name and blob are required");
            }
        }
    }
}
