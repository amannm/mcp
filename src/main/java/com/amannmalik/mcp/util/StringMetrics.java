package com.amannmalik.mcp.util;

import java.util.Locale;

public final class StringMetrics {
    private StringMetrics() {
    }

    public static int levenshtein(String a, String b) {
        var prev = new int[b.length() + 1];
        for (var j = 0; j <= b.length(); j++) prev[j] = j;
        for (var i = 1; i <= a.length(); i++) {
            var curr = new int[b.length() + 1];
            curr[0] = i;
            var ca = a.charAt(i - 1);
            for (var j = 1; j <= b.length(); j++) {
                var cost = ca == b.charAt(j - 1) ? 0 : 1;
                var ins = curr[j - 1] + 1;
                var del = prev[j] + 1;
                var sub = prev[j - 1] + cost;
                curr[j] = Math.min(Math.min(ins, del), sub);
            }
            prev = curr;
        }
        return prev[b.length()];
    }

    public static int prefixDistance(String a, String b) {
        var n = Math.min(a.length(), b.length());
        return levenshtein(
                a.substring(0, n).toLowerCase(Locale.ROOT),
                b.substring(0, n).toLowerCase(Locale.ROOT)
        );
    }
}
