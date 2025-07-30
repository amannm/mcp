package com.amannmalik.mcp.server.completion;

import com.amannmalik.mcp.validation.InputSanitizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryCompletionProvider implements CompletionProvider {
    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    public void add(CompleteRequest.Ref ref,
                    String argumentName,
                    Map<String, String> context,
                    List<String> values) {
        argumentName = InputSanitizer.requireClean(argumentName);
        Map<String, String> ctx;
        if (context == null || context.isEmpty()) {
            ctx = Map.of();
        } else {
            Map<String, String> copy = new HashMap<>();
            context.forEach((k, v) -> copy.put(InputSanitizer.requireClean(k), InputSanitizer.requireClean(v)));
            ctx = Map.copyOf(copy);
        }
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
        Comparator<String> cmp = Comparator.comparingInt((String v) -> similarity(prefix, v))
                .thenComparing(String::compareTo);
        List<String> sorted = unique.stream()
                .sorted(cmp)
                .limit(CompleteResult.MAX_VALUES)
                .toList();
        int total = unique.size();
        boolean hasMore = total > sorted.size();
        return new CompleteResult(new CompleteResult.Completion(sorted, total, hasMore), null);
    }

    private static int similarity(String a, String b) {
        int n = Math.min(a.length(), b.length());
        return levenshtein(a.substring(0, n).toLowerCase(), b.substring(0, n).toLowerCase());
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            int[] curr = new int[b.length() + 1];
            curr[0] = i;
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= b.length(); j++) {
                int cost = ca == b.charAt(j - 1) ? 0 : 1;
                int ins = curr[j - 1] + 1;
                int del = prev[j] + 1;
                int sub = prev[j - 1] + cost;
                curr[j] = Math.min(Math.min(ins, del), sub);
            }
            prev = curr;
        }
        return prev[b.length()];
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
