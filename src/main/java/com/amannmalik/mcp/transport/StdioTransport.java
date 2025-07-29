package com.amannmalik.mcp.transport;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class StdioTransport implements Transport {
    private final BufferedReader in;
    private final BufferedWriter out;
    private final Process process;
    private final Thread logReader;
    private static final Duration WAIT = Duration.ofSeconds(2);

    public StdioTransport(InputStream in, OutputStream out) {
        this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        this.process = null;
        this.logReader = null;
    }

    public StdioTransport(ProcessBuilder builder, Consumer<String> logSink) throws IOException {
        Objects.requireNonNull(logSink, "logSink");
        builder.redirectErrorStream(false);
        this.process = builder.start();
        this.in = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        this.logReader = new Thread(() -> readLogs(process.getErrorStream(), logSink));
        this.logReader.setDaemon(true);
        this.logReader.start();
    }

    public StdioTransport(ProcessBuilder builder) throws IOException {
        this(builder, System.err::println);
    }

    @Override
    public synchronized void send(JsonObject message) throws IOException {
        String s = message.toString();
        if (s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("message contains newline");
        }
        out.write(s);
        out.write('\n');
        out.flush();
    }

    @Override
    public JsonObject receive() throws IOException {
        String line = in.readLine();
        if (line == null) throw new EOFException();
        try (JsonReader reader = Json.createReader(new StringReader(line))) {
            return reader.readObject();
        }
    }

    @Override
    public void close() throws IOException {
        IOException ex = null;
        try { out.close(); } catch (IOException e) { ex = e; }
        try { in.close(); } catch (IOException e) { if (ex == null) ex = e; }
        if (process != null) {
            process.destroy();
            try {
                if (!process.waitFor(WAIT.toMillis(), TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(WAIT.toMillis(), TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        }
        if (logReader != null) {
            try { logReader.join(100); } catch (InterruptedException ignore) { Thread.currentThread().interrupt(); }
        }
        if (ex != null) throw ex;
    }

    private static void readLogs(InputStream err, Consumer<String> sink) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(err, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sink.accept(line);
        } catch (IOException ignore) {
        }
    }
}
