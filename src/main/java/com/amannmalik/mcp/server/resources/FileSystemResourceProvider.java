package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.validation.UriValidator;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Simple ResourceProvider backed by the filesystem. */
public final class FileSystemResourceProvider implements ResourceProvider {
    private final Path root;

    public FileSystemResourceProvider(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public ResourceList list(String cursor) throws IOException {
        List<Resource> list = new ArrayList<>();
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
            list.add(r);
        });
        return new ResourceList(list, null);
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
    public List<ResourceTemplate> templates() {
        return List.of();
    }

    @Override
    public ResourceSubscription subscribe(String uri, ResourceListener listener) {
        return () -> {};
    }

    private static String probeMime(Path p) {
        try {
            return Files.probeContentType(p);
        } catch (IOException e) {
            return null;
        }
    }
}
