package com.amannmalik.mcp.client.sampling;

/** Content of a sampling message. */
public sealed interface MessageContent permits MessageContent.Text, MessageContent.Image, MessageContent.Audio {
    String type();

    /** Text content. */
    record Text(String text) implements MessageContent {
        public Text {
            if (text == null) throw new IllegalArgumentException("text is required");
        }
        @Override public String type() { return "text"; }
    }

    /** Image content. */
    record Image(byte[] data, String mimeType) implements MessageContent {
        public Image {
            if (data == null || mimeType == null) {
                throw new IllegalArgumentException("data and mimeType are required");
            }
        }
        @Override public String type() { return "image"; }
    }

    /** Audio content. */
    record Audio(byte[] data, String mimeType) implements MessageContent {
        public Audio {
            if (data == null || mimeType == null) {
                throw new IllegalArgumentException("data and mimeType are required");
            }
        }
        @Override public String type() { return "audio"; }
    }
}
