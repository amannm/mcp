package com.amannmalik.mcp.server.completion;

import com.amannmalik.mcp.util.StringMetrics;
import com.amannmalik.mcp.server.roots.validation.InputSanitizer;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryCompletionProvider implements CompletionProvider {
    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    public void add(CompleteRequest.Ref ref,
                    String argumentName,
                    Map<String, String> context,
                    List<String> values) {
        argumentName = InputSanitizer.requireClean(argumentName);
        Map<String, String> ctx = InputSanitizer.requireCleanMap(context);
        List<String> vals = values == null ? List.of() : values.stream()
                .map(InputSanitizer::requireClean)
                .toList();
        entries.add(new Entry(ref, argumentName, ctx, vals));
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
        return new CompleteResult(new CompleteResult.Completion(sorted, total, hasMore), null);
    }

    private static boolean refEquals(CompleteRequest.Ref a, CompleteRequest.Ref b) {
        if (a instanceof CompleteRequest.Ref.PromptRef(var aName, var _, var _) &&
                b instanceof CompleteRequest.Ref.PromptRef(var bName, var _, var _)) {
            return aName.equals(bName);
        }
        if (a instanceof CompleteRequest.Ref.ResourceRef(String aUri) && b instanceof CompleteRequest.Ref.ResourceRef(String bUri)) {
            return aUri.equals(bUri);
        }
        return false;
    }

    private record Entry(CompleteRequest.Ref ref, String argumentName, Map<String, String> context, List<String> values) {
    }
}
