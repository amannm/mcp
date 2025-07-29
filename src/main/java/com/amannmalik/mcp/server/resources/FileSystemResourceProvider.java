package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.validation.UriValidator;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import com.amannmalik.mcp.util.Pagination;
import java.util.concurrent.atomic.AtomicBoolean;

/** Simple ResourceProvider backed by the filesystem. */
public final class FileSystemResourceProvider implements ResourceProvider {
    private final Path root;

    public FileSystemResourceProvider(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public ResourceList list(String cursor) throws IOException {
        List<Resource> all = new ArrayList<>();
        Files.walk(root).filter(Files::isRegularFile).forEach(p -> {
            String uri = p.toUri().toString();
            String name = p.getFileName().toString();
            String mime = probeMime(p);
            long size;
            try {
                size = Files.size(p);
            } catch (IOException e) {
                size = -1;
            }
            Resource r = new Resource(uri, name, null, null, mime, size < 0 ? null : size, null);
            all.add(r);
        });
        Pagination.Page<Resource> page = Pagination.page(all, cursor, 100);
        return new ResourceList(page.items(), page.nextCursor());
    }

    @Override
    public ResourceBlock read(String uri) throws IOException {
        UriValidator.requireFileUri(uri);
        Path p = Path.of(URI.create(uri));
        p = root.resolve(root.relativize(p)).normalize();
        if (!p.startsWith(root) || !Files.exists(p)) return null;
        String name = p.getFileName().toString();
        String mime = probeMime(p);
        if (mime != null && mime.startsWith("text")) {
            String text = Files.readString(p, StandardCharsets.UTF_8);
            return new ResourceBlock.Text(uri, name, null, mime, text, null);
        }
        byte[] data = Files.readAllBytes(p);
        return new ResourceBlock.Binary(uri, name, null, mime, data, null);
    }

    @Override
    public ResourceTemplatePage listTemplates(String cursor) {
        return new ResourceTemplatePage(List.of(), null);
    }

    @Override
    public ResourceSubscription subscribe(String uri, ResourceListener listener) {
        UriValidator.requireFileUri(uri);
        Path target = root.resolve(root.relativize(Path.of(URI.create(uri)))).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("uri outside of root");
        }
        Path dir = target.getParent();
        try {
            WatchService ws = dir.getFileSystem().newWatchService();
            dir.register(ws, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
            AtomicBoolean running = new AtomicBoolean(true);
            Thread t = Thread.startVirtualThread(() -> {
                try {
                    while (running.get()) {
                        WatchKey key = ws.take();
                        for (WatchEvent<?> e : key.pollEvents()) {
                            Path changed = dir.resolve((Path) e.context()).normalize();
                            if (changed.equals(target)) {
                                listener.updated(new ResourceUpdate(uri, null));
                            }
                        }
                        key.reset();
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    try { ws.close(); } catch (IOException ignored) {}
                }
            });
            return () -> {
                running.set(false);
                t.interrupt();
                try { ws.close(); } catch (IOException ignored) {}
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String probeMime(Path p) {
        try {
            return Files.probeContentType(p);
        } catch (IOException e) {
            return null;
        }
    }
}
