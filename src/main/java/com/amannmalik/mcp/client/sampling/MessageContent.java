package com.amannmalik.mcp.client.sampling;


public sealed interface MessageContent permits MessageContent.Text, MessageContent.Image, MessageContent.Audio {
    String type();

    
    record Text(String text) implements MessageContent {
        public Text {
            if (text == null) throw new IllegalArgumentException("text is required");
        }
        @Override public String type() { return "text"; }
    }

    
    record Image(byte[] data, String mimeType) implements MessageContent {
        public Image {
            if (data == null || mimeType == null) {
                throw new IllegalArgumentException("data and mimeType are required");
            }
            data = data.clone();
        }
        @Override public String type() { return "image"; }
    }

    
    record Audio(byte[] data, String mimeType) implements MessageContent {
        public Audio {
            if (data == null || mimeType == null) {
                throw new IllegalArgumentException("data and mimeType are required");
            }
            data = data.clone();
        }
        @Override public String type() { return "audio"; }
    }
}
