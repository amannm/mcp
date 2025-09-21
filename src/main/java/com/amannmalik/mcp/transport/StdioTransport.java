package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.api.Transport;
import com.amannmalik.mcp.util.*;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.*;
import java.lang.System.Logger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;

/// - [Transports](specification/2025-06-18/basic/transports.mdx)
public final class StdioTransport implements Transport {
    private static final Executor READER = command -> Thread.ofVirtual().start(command);
    private static final Logger LOG = PlatformLog.get(StdioTransport.class);
    private final BufferedReader in;
    private final BufferedWriter out;
    private final ProcessResources resources;
    private final Duration receiveTimeout;

    public StdioTransport(InputStream in, OutputStream out, Duration receiveTimeout) {
        this.resources = Detached.INSTANCE;
        this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        this.receiveTimeout = ValidationUtil.requirePositive(receiveTimeout, "receiveTimeout");
    }

    public StdioTransport(String[] command,
                          Consumer<String> logSink,
                          Duration receiveTimeout,
                          Duration processShutdownWait) throws IOException {
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
        this.resources = new Spawned(process, logReader, ValidationUtil.requirePositive(processShutdownWait, "processShutdownWait"));
        this.receiveTimeout = ValidationUtil.requirePositive(receiveTimeout, "receiveTimeout");
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
    public void send(JsonObject message) throws IOException {
        var s = message.toString();
        if (s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("message contains newline");
        }
        synchronized (out) {
            out.write(s);
            out.write('\n');
            out.flush();
        }
    }

    @Override
    public JsonObject receive() throws IOException {
        return receive(receiveTimeout);
    }

    @Override
    public JsonObject receive(Duration timeout) throws IOException {
        var duration = ValidationUtil.requirePositive(timeout, "timeout");
        var waitMillis = duration.toMillis();
        var future = CompletableFuture.supplyAsync(() -> {
            try {
                return in.readLine();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, READER);
        String line;
        try {
            line = future.get(waitMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            resources.checkAlive();
            throw new IOException("Timeout after " + waitMillis + "ms waiting for input", e);
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

    @Override
    public void close() throws IOException {
        CloseUtil.closeAll(out, in, resources);
    }

    @Override
    public void listen() {
        // do nothing
    }

    @Override
    public void setProtocolVersion(String version) {
        // do nothing
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

    private sealed interface ProcessResources extends AutoCloseable permits Detached, Spawned {
        void checkAlive() throws IOException;

        @Override
        void close() throws IOException;
    }

    private record Spawned(Process process, Thread logReader, Duration shutdownWait) implements ProcessResources {
        @Override
        public void checkAlive() throws IOException {
            if (!process.isAlive()) {
                int code;
                try {
                    code = process.exitValue();
                } catch (IllegalThreadStateException e) {
                    code = -1;
                    LOG.log(Logger.Level.DEBUG, "Process still running when checking exit value", e);
                }
                throw new IOException("Process exited with code " + code);
            }
        }

        @Override
        public void close() throws IOException {
            process.destroy();
            try {
                if (!process.waitFor(shutdownWait.toMillis(), TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    if (!process.waitFor(shutdownWait.toMillis(), TimeUnit.MILLISECONDS)) {
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
