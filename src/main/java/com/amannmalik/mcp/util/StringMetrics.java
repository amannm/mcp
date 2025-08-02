package com.amannmalik.mcp.util;

public final class StringMetrics {
    private StringMetrics() {
    }

    public static int levenshtein(String a, String b) {
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

    public static int prefixDistance(String a, String b) {
        int n = Math.min(a.length(), b.length());
        return levenshtein(
                a.substring(0, n).toLowerCase(java.util.Locale.ROOT),
                b.substring(0, n).toLowerCase(java.util.Locale.ROOT)
        );
    }
}
