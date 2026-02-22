package org.lintx.plugins.yinwuchat.common.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HandshakeManager {
    private static final long TTL_MS = 5 * 60 * 1000L;
    private final Map<String, Handshake> handshakes = new HashMap<>();

    public synchronized Handshake createHandshake() {
        cleanupExpired();
        Handshake handshake = new Handshake();
        handshake.id = UUID.randomUUID().toString();
        handshake.nonce = CryptoUtil.randomHex(16);
        handshake.hmacKey = CryptoUtil.randomBytes(32);
        handshake.createdAt = System.currentTimeMillis();
        handshakes.put(handshake.id, handshake);
        return handshake;
    }

    public synchronized Handshake getValidHandshake(String id) {
        cleanupExpired();
        if (id == null) {
            return null;
        }
        return handshakes.get(id);
    }

    public synchronized void consume(String id) {
        if (id != null) {
            handshakes.remove(id);
        }
    }

    public long getTtlMillis() {
        return TTL_MS;
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        handshakes.entrySet().removeIf(e -> now - e.getValue().createdAt > TTL_MS);
    }

    public static class Handshake {
        public String id;
        public String nonce;
        public byte[] hmacKey;
        public long createdAt;
    }
}
