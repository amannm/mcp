package com.amannmalik.mcp.test.impl;

import com.amannmalik.mcp.spi.*;
import jakarta.json.JsonObject;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DefaultCompletionProvider implements CompletionProvider {
    private final List<Ref> refs = new CopyOnWriteArrayList<>(DefaultServerFixtures.COMPLETION_REFS);
    private final List<DefaultServerFixtures.CompletionEntry> entries = new CopyOnWriteArrayList<>(DefaultServerFixtures.COMPLETION_ENTRIES);
    private final List<Runnable> listChangedListeners = new CopyOnWriteArrayList<>();

    @Override
    public Pagination.Page<Ref> list(Cursor cursor) {
        return Pagination.page(refs, cursor == null ? Cursor.Start.INSTANCE : cursor, Pagination.DEFAULT_PAGE_SIZE);
    }

    @Override
    public Closeable onListChanged(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        listChangedListeners.add(listener);
        return () -> listChangedListeners.remove(listener);
    }

    @Override
    public boolean supportsListChanged() {
        return true;
    }

    @Override
    public void close() {
        listChangedListeners.clear();
        refs.clear();
        entries.clear();
    }

    @Override
    public CompleteResult execute(String name, JsonObject args) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(args, "args");
        var ref = CompletionProvider.decode(name);
        var argumentObject = args.getJsonObject("argument");
        if (argumentObject == null) {
            throw new IllegalArgumentException("argument required");
        }
        var argument = new Argument(argumentObject.getString("name"), argumentObject.getString("value", ""));
        Context context = null;
        if (args.containsKey("context")) {
            var contextWrapper = args.getJsonObject("context");
            if (contextWrapper != null) {
                var contextObj = contextWrapper.getJsonObject("arguments");
                if (contextObj != null) {
                    var map = new HashMap<String, String>();
                    for (var entry : contextObj.entrySet()) {
                        var value = entry.getValue();
                        if (value instanceof jakarta.json.JsonString js) {
                            map.put(entry.getKey(), js.getString());
                        }
                    }
                    if (!map.isEmpty()) {
                        context = new Context(map);
                    }
                }
            }
        }
        return complete(new CompleteRequest(ref, argument, context, null));
    }

    @Override
    public CompleteResult complete(CompleteRequest request) {
        var matchingEntries = entries.stream()
                .filter(entry -> sameReference(entry.ref(), request.ref()) && entry.argumentName().equals(request.argument().name()))
                .toList();
        if (matchingEntries.isEmpty()) {
            throw new IllegalArgumentException("unknown ref");
        }
        var matches = new ArrayList<String>();
        var contextSatisfied = false;
        for (var entry : matchingEntries) {
            if (entry.context().isEmpty() || contextMatches(entry.context(), request.context())) {
                contextSatisfied = true;
                matches.addAll(entry.values());
            }
        }
        if (!contextSatisfied) {
            throw new IllegalArgumentException("missing arguments");
        }
        var unique = new LinkedHashSet<>(matches);
        var ordered = List.copyOf(unique);
        var limited = DefaultServerFixtures.limitValues(ordered);
        var hasMore = ordered.size() > limited.size();
        var completion = new Completion(limited, ordered.size(), hasMore);
        return new CompleteResult(completion, null);
    }

    private boolean sameReference(Ref a, Ref b) {
        if (a instanceof Ref.PromptRef promptA && b instanceof Ref.PromptRef promptB) {
            return promptA.name().equals(promptB.name());
        }
        if (a instanceof Ref.ResourceRef(String uri) && b instanceof Ref.ResourceRef(String otherUri)) {
            return uri.equals(otherUri);
        }
        return false;
    }

    private boolean contextMatches(Map<String, String> expected, Context actual) {
        if (actual == null) {
            return false;
        }
        return actual.arguments().entrySet().containsAll(expected.entrySet());
    }
}
