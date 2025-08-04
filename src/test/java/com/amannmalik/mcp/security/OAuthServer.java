package com.amannmalik.mcp.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class OAuthServer {
    private static final SecureRandom RANDOM = new SecureRandom();
    private record Grant(String challenge, String resource) {}

    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    private final Map<String, Grant> codes = new ConcurrentHashMap<>();
    public OAuthServer(String url) {
    }

    public record Client(String id, String secret) {}

    public Client register(String resource) {
        String id = random();
        String secret = random();
        clients.put(id, new Client(id, secret));
        return clients.get(id);
    }

    public String authorize(Client client, String verifier, String resource) {
        if (!clients.containsKey(client.id())) throw new IllegalArgumentException();
        String code = random();
        codes.put(code, new Grant(challenge(verifier), resource));
        return code;
    }

    public String token(String code, String verifier, String resource) {
        Grant grant = codes.remove(code);
        if (grant == null) throw new IllegalArgumentException();
        if (!grant.challenge().equals(challenge(verifier)) || !grant.resource().equals(resource)) {
            throw new IllegalArgumentException();
        }
        return "aud=" + resource + ";exp=valid;sig=true";
    }

    public String audience(String token) {
        return token.split(";")[0].split("=")[1];
    }

    public String verifier() {
        return random();
    }

    private String challenge(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String random() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
