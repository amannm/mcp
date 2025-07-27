package com.amannmalik.mcp.transport;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.*;
import java.nio.charset.StandardCharsets;

public final class StdioTransport implements Transport {
    private final BufferedReader in;
    private final BufferedWriter out;

    public StdioTransport(InputStream in, OutputStream out) {
        this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
    }

    @Override
    public synchronized void send(JsonObject message) throws IOException {
        out.write(message.toString());
        out.write('\n');
        out.flush();
    }

    @Override
    public synchronized JsonObject receive() throws IOException {
        String line = in.readLine();
        if (line == null) throw new EOFException();
        try (JsonReader reader = Json.createReader(new StringReader(line))) {
            return reader.readObject();
        }
    }

    @Override
    public void close() throws IOException {
        in.close();
        out.close();
    }
}
