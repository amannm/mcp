package com.amannmalik.mcp.client.sampling;

public sealed interface MessageContent permits MessageContent.Text, MessageContent.Image, MessageContent.Audio {
    String type();
    Annotations annotations();
    jakarta.json.JsonObject _meta();

    record Text(String text, Annotations annotations, jakarta.json.JsonObject _meta) implements MessageContent {
        public Text {
            if (text == null) throw new IllegalArgumentException("text is required");
            com.amannmalik.mcp.validation.MetaValidator.requireValid(_meta);
        }

        @Override
        public String type() {
            return "text";
        }
    }

    record Image(byte[] data, String mimeType, Annotations annotations, jakarta.json.JsonObject _meta) implements MessageContent {
        public Image {
            if (data == null || mimeType == null) {
                throw new IllegalArgumentException("data and mimeType are required");
            }
            data = data.clone();
            com.amannmalik.mcp.validation.MetaValidator.requireValid(_meta);
        }

        @Override
        public String type() {
            return "image";
        }
    }

    record Audio(byte[] data, String mimeType, Annotations annotations, jakarta.json.JsonObject _meta) implements MessageContent {
        public Audio {
            if (data == null || mimeType == null) {
                throw new IllegalArgumentException("data and mimeType are required");
            }
            data = data.clone();
            com.amannmalik.mcp.validation.MetaValidator.requireValid(_meta);
        }

        @Override
        public String type() {
            return "audio";
        }
    }
}
