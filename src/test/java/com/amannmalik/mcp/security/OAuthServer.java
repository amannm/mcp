package com.amannmalik.mcp.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class OAuthServer {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    private final Map<String, String> codes = new ConcurrentHashMap<>();
    private final String url;

    public OAuthServer(String url) {
        this.url = url;
    }

    public record Client(String id, String secret) {}

    public Client register(String resource) {
        String id = random();
        String secret = random();
        clients.put(id, new Client(id, secret));
        return clients.get(id);
    }

    public String authorize(Client client, String challenge, String resource) {
        String code = random();
        codes.put(code, client.id());
        return code;
    }

    public String token(String code, String verifier, String resource) {
        if (!codes.containsKey(code)) throw new IllegalArgumentException();
        return "aud=" + resource + ";exp=valid;sig=true";
    }

    public String audience(String token) {
        return token.split(";")[0].split("=")[1];
    }

    public String verifier() {
        return random();
    }

    private String random() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
