package com.amannmalik.mcp.completion;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.codec.ArgumentJsonCodec;
import com.amannmalik.mcp.codec.ContextJsonCodec;
import com.amannmalik.mcp.core.InMemoryProvider;
import com.amannmalik.mcp.util.StringMetrics;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryCompletionProvider extends InMemoryProvider<Ref> implements CompletionProvider {

    private static final ArgumentJsonCodec ARGUMENT_CODEC = new ArgumentJsonCodec();
    private static final ContextJsonCodec CONTEXT_CODEC = new ContextJsonCodec();

    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    private static boolean refEquals(Ref a, Ref b) {
        if (a instanceof Ref.PromptRef(var aName, var _, var _) &&
                b instanceof Ref.PromptRef(var bName, var _, var _)) {
            return aName.equals(bName);
        }
        if (a instanceof Ref.ResourceRef(String aUri) && b instanceof Ref.ResourceRef(String bUri)) {
            return aUri.equals(bUri);
        }
        return false;
    }

    public void add(Ref ref,
                    String argumentName,
                    Map<String, String> context,
                    List<String> values) {
        argumentName = ValidationUtil.requireClean(argumentName);
        Map<String, String> ctx = ValidationUtil.requireCleanMap(context);
        List<String> vals = values == null ? List.of() : values.stream()
                .map(ValidationUtil::requireClean)
                .toList();
        entries.add(new Entry(ref, argumentName, ctx, vals));
        if (items.stream().noneMatch(r -> refEquals(r, ref))) {
            items.add(ref);
            notifyListeners();
        }
    }

    @Override
    public CompleteResult execute(String name, JsonObject args) {
        Ref ref = CompletionProvider.decode(name);
        var arg = ARGUMENT_CODEC.fromJson(args.getJsonObject("argument"));
        Context ctx = args.containsKey("context")
                ? CONTEXT_CODEC.fromJson(args.getJsonObject("context"))
                : null;
        return complete(new CompleteRequest(ref, arg, ctx, null));
    }

    @Override
    public CompleteResult complete(CompleteRequest request) {
        List<Entry> candidates = new ArrayList<>();
        for (Entry e : entries) {
            if (refEquals(e.ref, request.ref()) && e.argumentName.equals(request.argument().name())) {
                candidates.add(e);
            }
        }
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("unknown ref");
        }

        List<String> matches = new ArrayList<>();
        boolean contextSatisfied = false;
        for (Entry e : candidates) {
            if (request.context() == null || request.context().arguments().entrySet().containsAll(e.context.entrySet())) {
                contextSatisfied = true;
                matches.addAll(e.values);
            }
        }
        if (!contextSatisfied) {
            throw new IllegalArgumentException("missing arguments");
        }

        String prefix = request.argument().value();
        Set<String> unique = new LinkedHashSet<>(matches);
        Comparator<String> cmp = Comparator.comparingInt((String v) -> StringMetrics.prefixDistance(prefix, v))
                .thenComparing(String::compareTo);
        List<String> sorted = unique.stream()
                .sorted(cmp)
                .limit(CompleteResult.MAX_VALUES)
                .toList();
        int total = unique.size();
        boolean hasMore = total > sorted.size();
        return new CompleteResult(new Completion(sorted, total, hasMore), null);
    }

    private record Entry(Ref ref, String argumentName, Map<String, String> context, List<String> values) {
    }
}
