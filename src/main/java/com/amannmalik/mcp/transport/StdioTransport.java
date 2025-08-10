package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.api.Transport;
import com.amannmalik.mcp.config.McpConfiguration;
import jakarta.json.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/// - [Transports](specification/2025-06-18/basic/transports.mdx)
public final class StdioTransport implements Transport {
    private final BufferedReader in;
    private final BufferedWriter out;
    private final Process process;
    private final Thread logReader;
    private static final Duration WAIT = Duration.ofSeconds(
            McpConfiguration.current().processWaitSeconds());

    public StdioTransport(InputStream in, OutputStream out) {
        this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        this.process = null;
        this.logReader = null;
    }

    public StdioTransport(String[] args, Consumer<String> logSink) throws IOException {
        Objects.requireNonNull(logSink, "logSink");
        var builder = new ProcessBuilder(args);
        builder.redirectErrorStream(false);
        this.process = builder.start();
        this.in = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        this.logReader = new Thread(() -> readLogs(process.getErrorStream(), logSink));
        this.logReader.setDaemon(true);
        this.logReader.start();
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
        return receive(McpConfiguration.current().defaultMs());
    }
    
    @Override
    public JsonObject receive(long timeoutMillis) throws IOException {
        long endTime = System.currentTimeMillis() + timeoutMillis;
        
        while (System.currentTimeMillis() < endTime) {
            if (in.ready()) {
                String line = in.readLine();
                if (line == null) throw new EOFException();
                try (JsonReader reader = Json.createReader(new StringReader(line))) {
                    return reader.readObject();
                }
            }
            
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for input", e);
            }
        }
        
        throw new IOException("Timeout after " + timeoutMillis + "ms waiting for input");
    }

    @Override
    public void close() throws IOException {
        IOException ex = null;
        try {
            out.close();
        } catch (IOException e) {
            ex = e;
        }
        try {
            in.close();
        } catch (IOException e) {
            if (ex == null) ex = e;
        }
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
            try {
                logReader.join(100);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
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
