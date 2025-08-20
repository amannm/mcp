package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.api.McpHostConfiguration;
import com.amannmalik.mcp.api.Transport;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/// - [Transports](specification/2025-06-18/basic/transports.mdx)
public final class StdioTransport implements Transport {
    // TODO: externalize
    private static final Duration WAIT = McpHostConfiguration.defaultConfiguration().processWaitSeconds();
    private static final Duration RECEIVE = Duration.ofSeconds(5);
    private final BufferedReader in;
    private final BufferedWriter out;
    private final ProcessResources resources;
    private final Duration receiveTimeout;

    public StdioTransport(InputStream in, OutputStream out) {
        this(in, out, RECEIVE);
    }

    public StdioTransport(InputStream in, OutputStream out, Duration receiveTimeout) {
        this.resources = Detached.INSTANCE;
        this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        this.receiveTimeout = receiveTimeout;
    }

    public StdioTransport(String[] command, Consumer<String> logSink) throws IOException {
        this(command, logSink, RECEIVE);
    }

    public StdioTransport(String[] command, Consumer<String> logSink, Duration receiveTimeout) throws IOException {
        Objects.requireNonNull(logSink, "logSink");
        if (command.length == 0) {
            throw new IllegalArgumentException("command");
        }
        var builder = new ProcessBuilder(command);
        builder.redirectErrorStream(false);
        var process = builder.start();
        this.in = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        var logReader = new Thread(() -> readLogs(process.getErrorStream(), logSink));
        logReader.setDaemon(true);
        logReader.start();
        this.resources = new Spawned(process, logReader);
        this.receiveTimeout = receiveTimeout;
    }

    private static void readLogs(InputStream err, Consumer<String> sink) {
        try (var r = new BufferedReader(new InputStreamReader(err, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                sink.accept(line);
            }
        } catch (IOException e) {
            sink.accept("error reading log stream: " + e.getMessage());
        }
    }

    @Override
    public synchronized void send(JsonObject message) throws IOException {
        var s = message.toString();
        if (s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("message contains newline");
        }
        out.write(s);
        out.write('\n');
        out.flush();
    }

    @Override
    public JsonObject receive() throws IOException {
        return receive(receiveTimeout);
    }

    @Override
    public JsonObject receive(Duration timeout) throws IOException {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var future = executor.submit(in::readLine);
            String line;
            try {
                line = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                resources.checkAlive();
                throw new IOException("Timeout after " + timeout + " waiting for input", e);
            } catch (ExecutionException e) {
                var cause = e.getCause();
                if (cause instanceof IOException io) {
                    throw io;
                }
                throw new IOException("Failed to read input", cause);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for input", e);
            }
            if (line == null) {
                throw new EOFException();
            }
            try (var reader = Json.createReader(new StringReader(line))) {
                return reader.readObject();
            }
        }
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
            if (ex == null) {
                ex = e;
            }
        }
        try {
            resources.close();
        } catch (IOException e) {
            if (ex == null) {
                ex = e;
            }
        }
        if (ex != null) {
            throw ex;
        }
    }

    private sealed interface ProcessResources extends AutoCloseable permits Detached, Spawned {
        void checkAlive() throws IOException;
        @Override
        void close() throws IOException;
    }

    private enum Detached implements ProcessResources {
        INSTANCE;
        @Override
        public void checkAlive() {
        }
        @Override
        public void close() {
        }
    }

    private record Spawned(Process process, Thread logReader) implements ProcessResources {
        @Override
        public void checkAlive() throws IOException {
            if (!process.isAlive()) {
                int code;
                try {
                    code = process.exitValue();
                } catch (IllegalThreadStateException ignore) {
                    code = -1;
                }
                throw new IOException("Process exited with code " + code);
            }
        }

        @Override
        public void close() throws IOException {
            process.destroy();
            try {
                if (!process.waitFor(WAIT.toMillis(), TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    if (!process.waitFor(WAIT.toMillis(), TimeUnit.MILLISECONDS)) {
                        throw new IOException("Process did not terminate");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for process", e);
            }
            try {
                logReader.join(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for log reader", e);
            }
        }
    }
}
