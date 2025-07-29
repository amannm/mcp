package com.amannmalik.mcp.server.completion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;


public final class InMemoryCompletionProvider implements CompletionProvider {
    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    public void add(CompleteRequest.Ref ref, String argumentName, Map<String, String> context, List<String> values) {
        entries.add(new Entry(ref, argumentName, context == null ? Map.of() : Map.copyOf(context),
                values == null ? List.of() : List.copyOf(values)));
    }

    @Override
    public CompleteResult complete(CompleteRequest request) {
        List<String> matches = new ArrayList<>();
        for (Entry e : entries) {
            if (refEquals(e.ref, request.ref()) && e.argumentName.equals(request.argument().name())) {
                if (request.context() == null || request.context().arguments().entrySet().containsAll(e.context.entrySet())) {
                    matches.addAll(e.values);
                }
            }
        }
        String prefix = request.argument().value();
        List<String> filtered = matches.stream()
                .filter(v -> v.startsWith(prefix))
                .limit(100)
                .toList();
        int total = (int) matches.stream().filter(v -> v.startsWith(prefix)).count();
        boolean hasMore = total > filtered.size();
        return new CompleteResult(new CompleteResult.Completion(filtered, total, hasMore));
    }

    private static boolean refEquals(CompleteRequest.Ref a, CompleteRequest.Ref b) {
        if (a instanceof CompleteRequest.Ref.PromptRef pa && b instanceof CompleteRequest.Ref.PromptRef pb) {
            return pa.name().equals(pb.name());
        }
        if (a instanceof CompleteRequest.Ref.ResourceRef ra && b instanceof CompleteRequest.Ref.ResourceRef rb) {
            return ra.uri().equals(rb.uri());
        }
        return false;
    }

    private record Entry(CompleteRequest.Ref ref, String argumentName, Map<String, String> context, List<String> values) {}
}
