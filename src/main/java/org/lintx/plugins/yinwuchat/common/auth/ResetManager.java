package org.lintx.plugins.yinwuchat.common.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class ResetManager {
    private final Map<String, ResetSession> sessions = new ConcurrentHashMap<>();
    private final long ttlMillis = 10 * 60 * 1000; // 10 分钟有效期

    public static class ResetSession {
        public String accountName;
        public String playerName;
        public String resetCode;
        public boolean verified = false;
        public long expiresAt;

        ResetSession(String accountName, String playerName, String resetCode, long expiresAt) {
            this.accountName = accountName;
            this.playerName = playerName;
            this.resetCode = resetCode;
            this.expiresAt = expiresAt;
        }
    }

    public String createSession(String accountName, String playerName) {
        String resetCode = "RESET-" + (int)((Math.random() * 900000) + 100000);
        long expiresAt = System.currentTimeMillis() + ttlMillis;
        sessions.put(accountName.toLowerCase(), new ResetSession(accountName, playerName, resetCode, expiresAt));
        return resetCode;
    }

    public boolean verifyCode(String accountName, String playerName, String code) {
        ResetSession session = sessions.get(accountName.toLowerCase());
        if (session == null || System.currentTimeMillis() > session.expiresAt) {
            sessions.remove(accountName.toLowerCase());
            return false;
        }
        if (session.playerName.equalsIgnoreCase(playerName) && session.resetCode.equalsIgnoreCase(code)) {
            session.verified = true;
            return true;
        }
        return false;
    }

    public ResetSession getSession(String accountName) {
        ResetSession session = sessions.get(accountName.toLowerCase());
        if (session != null && System.currentTimeMillis() > session.expiresAt) {
            sessions.remove(accountName.toLowerCase());
            return null;
        }
        return session;
    }

    public void removeSession(String accountName) {
        sessions.remove(accountName.toLowerCase());
    }
}
