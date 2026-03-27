package org.lintx.plugins.yinwuchat.common.auth;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AuthUserStore {
    private static final String FILE_NAME = "web-users.json";
    private final File file;
    private final Gson gson = new Gson();
    private final Map<String, UserRecord> users = new HashMap<>();

    public AuthUserStore(File dataFolder) {
        this.file = new File(dataFolder, FILE_NAME);
        load();
    }

    public synchronized boolean exists(String username) {
        return users.containsKey(normalize(username));
    }

    public synchronized boolean register(String username, String passwordHash) {
        String key = normalize(username);
        if (users.containsKey(key)) {
            return false;
        }
        String salt = CryptoUtil.randomHex(16);
        String saltedHash = CryptoUtil.sha256Hex(passwordHash + salt);
        UserRecord record = new UserRecord();
        record.username = username.trim();
        record.salt = salt;
        record.saltedHash = saltedHash;
        record.createdAt = System.currentTimeMillis();
        record.bannedUntil = 0L;
        record.banReason = "";
        record.bannedBy = "";
        record.bannedAt = 0L;
        record.boundPlayerName = "";
        record.boundPlayers = new ArrayList<>();
        record.pendingBindTokens = new ArrayList<>();
        record.rememberLoginTokens = new ArrayList<>();
        users.put(key, record);
        save();
        return true;
    }

    public synchronized boolean verify(String username, String passwordHash) {
        UserRecord record = users.get(normalize(username));
        if (record == null) {
            return false;
        }
        String saltedHash = CryptoUtil.sha256Hex(passwordHash + record.salt);
        return saltedHash.equalsIgnoreCase(record.saltedHash);
    }

    public synchronized String getDisplayName(String username) {
        UserRecord record = users.get(normalize(username));
        return record == null ? "" : record.username;
    }

    public synchronized boolean delete(String username) {
        String key = normalize(username);
        if (users.containsKey(key)) {
            clearRememberLoginTokens(username);
            users.remove(key);
            save();
            return true;
        }
        return false;
    }

    public synchronized boolean updatePassword(String username, String newPasswordHash) {
        UserRecord record = users.get(normalize(username));
        if (record == null) {
            return false;
        }
        String salt = CryptoUtil.randomHex(16);
        String saltedHash = CryptoUtil.sha256Hex(newPasswordHash + salt);
        record.salt = salt;
        record.saltedHash = saltedHash;
        normalizeRecord(record);
        record.rememberLoginTokens.clear();
        save();
        return true;
    }

    public synchronized String issueRememberLoginToken(String username, long ttlMillis) {
        return issueRememberLoginToken(username, ttlMillis, System.currentTimeMillis());
    }

    synchronized String issueRememberLoginToken(String username, long ttlMillis, long now) {
        UserRecord record = users.get(normalize(username));
        if (record == null) {
            return "";
        }
        normalizeRecord(record);
        removeExpiredRememberTokens(record, now);
        RememberLoginToken token = new RememberLoginToken();
        String rawToken = CryptoUtil.randomHex(16) + "." + CryptoUtil.randomHex(32);
        token.tokenHash = CryptoUtil.sha256Hex(rawToken);
        token.createdAt = now;
        token.lastUsedAt = now;
        token.expiresAt = now + Math.max(1L, ttlMillis);
        record.rememberLoginTokens.add(token);
        save();
        return rawToken;
    }

    public synchronized String findUserByRememberLoginToken(String rawToken) {
        return findUserByRememberLoginToken(rawToken, System.currentTimeMillis());
    }

    synchronized String findUserByRememberLoginToken(String rawToken, long now) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            return "";
        }
        String tokenHash = CryptoUtil.sha256Hex(rawToken.trim());
        boolean changed = false;
        for (UserRecord record : users.values()) {
            normalizeRecord(record);
            if (removeExpiredRememberTokens(record, now)) {
                changed = true;
            }
            for (RememberLoginToken token : record.rememberLoginTokens) {
                if (tokenHash.equalsIgnoreCase(token.tokenHash)) {
                    token.lastUsedAt = now;
                    save();
                    return record.username == null ? "" : record.username;
                }
            }
        }
        if (changed) {
            save();
        }
        return "";
    }

    public synchronized boolean revokeRememberLoginToken(String rawToken) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            return false;
        }
        String tokenHash = CryptoUtil.sha256Hex(rawToken.trim());
        for (UserRecord record : users.values()) {
            normalizeRecord(record);
            Iterator<RememberLoginToken> iterator = record.rememberLoginTokens.iterator();
            while (iterator.hasNext()) {
                RememberLoginToken token = iterator.next();
                if (tokenHash.equalsIgnoreCase(token.tokenHash)) {
                    iterator.remove();
                    save();
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized void clearRememberLoginTokens(String username) {
        UserRecord record = users.get(normalize(username));
        if (record == null) {
            return;
        }
        normalizeRecord(record);
        if (!record.rememberLoginTokens.isEmpty()) {
            record.rememberLoginTokens.clear();
            save();
        }
    }

    /**
     * Legacy single-field bind: sets primary bound name and merges into {@link UserRecord#boundPlayers}.
     */
    public synchronized boolean bindPlayerName(String username, String playerName) {
        UserRecord record = users.get(normalize(username));
        if (record == null) {
            return false;
        }
        normalizeRecord(record);
        String trimmed = playerName == null ? "" : playerName.trim();
        record.boundPlayerName = trimmed;
        if (!trimmed.isEmpty()) {
            upsertBoundPlayerRecord(record, trimmed, System.currentTimeMillis(), false);
        }
        save();
        return true;
    }

    public synchronized boolean unbindPlayerName(String username) {
        UserRecord record = users.get(normalize(username));
        if (record == null) {
            return false;
        }
        normalizeRecord(record);
        record.boundPlayerName = "";
        record.boundPlayers.clear();
        save();
        return true;
    }

    public synchronized String unbindByPlayerName(String playerName) {
        String key = normalizePlayerName(playerName);
        if (key.isEmpty()) {
            return "";
        }
        String account = "";
        for (UserRecord record : users.values()) {
            normalizeRecord(record);
            if (removeBoundPlayerInternal(record, key)) {
                account = record.username == null ? "" : record.username;
                syncLegacyBoundPlayerName(record);
                break;
            }
        }
        if (!account.isEmpty()) {
            save();
        }
        return account;
    }

    /**
     * First non-rebind player name for legacy callers (reset token, shielded, etc.), else first in list, else legacy field.
     */
    public synchronized String getBoundPlayerName(String username) {
        UserRecord record = users.get(normalize(username));
        if (record == null) {
            return "";
        }
        normalizeRecord(record);
        for (BoundPlayerRecord b : record.boundPlayers) {
            if (b != null && b.playerName != null && !b.playerName.isEmpty() && !b.needsRebind) {
                return b.playerName.trim();
            }
        }
        if (!record.boundPlayers.isEmpty() && record.boundPlayers.get(0).playerName != null) {
            return record.boundPlayers.get(0).playerName.trim();
        }
        return record.boundPlayerName == null ? "" : record.boundPlayerName.trim();
    }

    public synchronized java.util.List<BoundPlayerRecord> getBoundPlayers(String username) {
        UserRecord record = users.get(normalize(username));
        if (record == null) {
            return new ArrayList<>();
        }
        normalizeRecord(record);
        return new ArrayList<>(record.boundPlayers);
    }

    public synchronized boolean removeBoundPlayer(String username, String playerName) {
        UserRecord record = users.get(normalize(username));
        if (record == null) {
            return false;
        }
        normalizeRecord(record);
        boolean removed = removeBoundPlayerInternal(record, normalizePlayerName(playerName));
        if (removed) {
            syncLegacyBoundPlayerName(record);
            save();
        }
        return removed;
    }

    public synchronized void addOrUpdateBoundPlayerFromGame(String username, String playerName, long nowMillis, boolean needsRebind) {
        UserRecord record = users.get(normalize(username));
        if (record == null) {
            return;
        }
        normalizeRecord(record);
        String trimmed = playerName == null ? "" : playerName.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        upsertBoundPlayerRecord(record, trimmed, nowMillis, needsRebind);
        syncLegacyBoundPlayerName(record);
        save();
    }

    public synchronized void markBoundPlayerNeedsRebind(String username, String playerName, boolean needsRebind) {
        UserRecord record = users.get(normalize(username));
        if (record == null) {
            return;
        }
        normalizeRecord(record);
        String key = normalizePlayerName(playerName);
        for (BoundPlayerRecord b : record.boundPlayers) {
            if (b != null && normalizePlayerName(b.playerName).equals(key)) {
                b.needsRebind = needsRebind;
                save();
                return;
            }
        }
    }

    public synchronized boolean registerPendingBindToken(String username, String rawToken, long nowMillis, long ttlMillis) {
        UserRecord record = users.get(normalize(username));
        if (record == null || rawToken == null || rawToken.trim().isEmpty()) {
            return false;
        }
        normalizeRecord(record);
        String hash = CryptoUtil.sha256Hex(rawToken.trim());
        removeExpiredPendingBinds(record, nowMillis);
        PendingBindEntry entry = new PendingBindEntry();
        entry.tokenHash = hash;
        entry.createdAt = nowMillis;
        entry.expiresAt = nowMillis + Math.max(60_000L, ttlMillis);
        record.pendingBindTokens.add(entry);
        save();
        return true;
    }

    /**
     * If rawToken matches a pending bind for this user, returns true and removes the pending entry.
     */
    public synchronized boolean verifyPendingBindToken(String username, String rawToken, long nowMillis) {
        UserRecord record = users.get(normalize(username));
        if (record == null || rawToken == null || rawToken.trim().isEmpty()) {
            return false;
        }
        normalizeRecord(record);
        String hash = CryptoUtil.sha256Hex(rawToken.trim());
        removeExpiredPendingBinds(record, nowMillis);
        Iterator<PendingBindEntry> it = record.pendingBindTokens.iterator();
        while (it.hasNext()) {
            PendingBindEntry e = it.next();
            if (e != null && hash.equalsIgnoreCase(e.tokenHash)) {
                it.remove();
                save();
                return true;
            }
        }
        return false;
    }

    /**
     * Finds web account that has a pending bind matching this token hash (any user).
     */
    public synchronized String findAccountByPendingBindToken(String rawToken, long nowMillis) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            return "";
        }
        String hash = CryptoUtil.sha256Hex(rawToken.trim());
        for (UserRecord record : users.values()) {
            normalizeRecord(record);
            removeExpiredPendingBinds(record, nowMillis);
            for (PendingBindEntry e : record.pendingBindTokens) {
                if (e != null && hash.equalsIgnoreCase(e.tokenHash)) {
                    return record.username == null ? "" : record.username;
                }
            }
        }
        return "";
    }

    public synchronized boolean consumePendingBindToken(String rawToken, long nowMillis) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            return false;
        }
        String hash = CryptoUtil.sha256Hex(rawToken.trim());
        boolean changed = false;
        for (UserRecord record : users.values()) {
            normalizeRecord(record);
            Iterator<PendingBindEntry> it = record.pendingBindTokens.iterator();
            while (it.hasNext()) {
                PendingBindEntry e = it.next();
                if (e != null && hash.equalsIgnoreCase(e.tokenHash)) {
                    it.remove();
                    changed = true;
                }
            }
        }
        if (changed) {
            save();
        }
        return changed;
    }

    public synchronized String getAccountByPlayerName(String playerName) {
        String key = normalizePlayerName(playerName);
        if (key.isEmpty()) {
            return "";
        }
        for (UserRecord record : users.values()) {
            normalizeRecord(record);
            for (BoundPlayerRecord b : record.boundPlayers) {
                if (b != null && normalizePlayerName(b.playerName).equals(key)) {
                    return record.username == null ? "" : record.username;
                }
            }
            String legacy = normalizePlayerName(record.boundPlayerName);
            if (!legacy.isEmpty() && legacy.equals(key)) {
                return record.username == null ? "" : record.username;
            }
        }
        return "";
    }

    public synchronized BanInfo getBanInfo(String username) {
        UserRecord record = users.get(normalize(username));
        if (record == null) {
            return BanInfo.notBanned();
        }
        if (record.bannedUntil == 0L) {
            return BanInfo.notBanned();
        }
        long now = System.currentTimeMillis();
        if (record.bannedUntil > 0 && now > record.bannedUntil) {
            clearBan(record);
            save();
            return BanInfo.notBanned();
        }
        boolean permanent = record.bannedUntil < 0;
        long remaining = permanent ? -1L : Math.max(0L, record.bannedUntil - now);
        BanInfo info = new BanInfo();
        info.banned = true;
        info.permanent = permanent;
        info.remainingMillis = remaining;
        info.reason = record.banReason == null ? "" : record.banReason;
        info.bannedBy = record.bannedBy == null ? "" : record.bannedBy;
        info.bannedAt = record.bannedAt;
        return info;
    }

    public synchronized boolean ban(String username, long durationMillis, String reason, String bannedBy) {
        UserRecord record = users.get(normalize(username));
        if (record == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        record.bannedAt = now;
        record.bannedBy = bannedBy == null ? "" : bannedBy;
        record.banReason = reason == null ? "" : reason;
        if (durationMillis <= 0L) {
            record.bannedUntil = -1L;
        } else {
            record.bannedUntil = now + durationMillis;
        }
        save();
        return true;
    }

    public synchronized void unban(String username) {
        UserRecord record = users.get(normalize(username));
        if (record == null) {
            return;
        }
        clearBan(record);
        save();
    }

    private void clearBan(UserRecord record) {
        record.bannedUntil = 0L;
        record.banReason = "";
        record.bannedBy = "";
        record.bannedAt = 0L;
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, UserRecord>>() {}.getType();
            Map<String, UserRecord> data = gson.fromJson(reader, type);
            if (data != null) {
                users.clear();
                users.putAll(data);
                boolean migrated = false;
                for (UserRecord record : users.values()) {
                    if (normalizeRecord(record)) {
                        migrated = true;
                    }
                }
                if (migrated) {
                    save();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void save() {
        try {
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8, false)) {
                gson.toJson(users, writer);
            }
        } catch (Exception ignored) {
        }
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private String normalizePlayerName(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }

    /** @return true if legacy single-player field was migrated into {@link UserRecord#boundPlayers} */
    private boolean normalizeRecord(UserRecord record) {
        if (record == null) {
            return false;
        }
        if (record.rememberLoginTokens == null) {
            record.rememberLoginTokens = new ArrayList<>();
        }
        if (record.boundPlayers == null) {
            record.boundPlayers = new ArrayList<>();
        }
        if (record.pendingBindTokens == null) {
            record.pendingBindTokens = new ArrayList<>();
        }
        return migrateLegacyBoundPlayer(record);
    }

    /** @return true if data was migrated and should be persisted */
    private boolean migrateLegacyBoundPlayer(UserRecord record) {
        if (record == null || record.boundPlayers == null) {
            return false;
        }
        if (!record.boundPlayers.isEmpty()) {
            return false;
        }
        String legacy = record.boundPlayerName == null ? "" : record.boundPlayerName.trim();
        if (legacy.isEmpty()) {
            return false;
        }
        BoundPlayerRecord b = new BoundPlayerRecord();
        b.playerName = legacy;
        b.lastBoundAt = record.createdAt > 0L ? record.createdAt : System.currentTimeMillis();
        b.needsRebind = false;
        record.boundPlayers.add(b);
        return true;
    }

    private void upsertBoundPlayerRecord(UserRecord record, String playerName, long nowMillis, boolean needsRebind) {
        String key = normalizePlayerName(playerName);
        for (BoundPlayerRecord b : record.boundPlayers) {
            if (b != null && normalizePlayerName(b.playerName).equals(key)) {
                b.playerName = playerName.trim();
                b.lastBoundAt = nowMillis;
                b.needsRebind = needsRebind;
                return;
            }
        }
        BoundPlayerRecord b = new BoundPlayerRecord();
        b.playerName = playerName.trim();
        b.lastBoundAt = nowMillis;
        b.needsRebind = needsRebind;
        record.boundPlayers.add(b);
    }

    private boolean removeBoundPlayerInternal(UserRecord record, String normalizedPlayerKey) {
        if (record.boundPlayers == null || normalizedPlayerKey.isEmpty()) {
            return false;
        }
        Iterator<BoundPlayerRecord> it = record.boundPlayers.iterator();
        while (it.hasNext()) {
            BoundPlayerRecord b = it.next();
            if (b != null && normalizePlayerName(b.playerName).equals(normalizedPlayerKey)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    private void syncLegacyBoundPlayerName(UserRecord record) {
        if (record.boundPlayers == null || record.boundPlayers.isEmpty()) {
            record.boundPlayerName = "";
            return;
        }
        for (BoundPlayerRecord b : record.boundPlayers) {
            if (b != null && b.playerName != null && !b.needsRebind) {
                record.boundPlayerName = b.playerName.trim();
                return;
            }
        }
        record.boundPlayerName = record.boundPlayers.get(0).playerName == null
                ? ""
                : record.boundPlayers.get(0).playerName.trim();
    }

    private void removeExpiredPendingBinds(UserRecord record, long nowMillis) {
        if (record.pendingBindTokens == null) {
            return;
        }
        Iterator<PendingBindEntry> it = record.pendingBindTokens.iterator();
        while (it.hasNext()) {
            PendingBindEntry e = it.next();
            if (e == null || e.expiresAt <= nowMillis) {
                it.remove();
            }
        }
    }

    private boolean removeExpiredRememberTokens(UserRecord record, long now) {
        normalizeRecord(record);
        boolean changed = false;
        Iterator<RememberLoginToken> iterator = record.rememberLoginTokens.iterator();
        while (iterator.hasNext()) {
            RememberLoginToken token = iterator.next();
            if (token == null || token.expiresAt <= now || token.tokenHash == null || token.tokenHash.isEmpty()) {
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    public static class UserRecord {
        public String username = "";
        public String salt = "";
        public String saltedHash = "";
        public long createdAt = 0L;
        public long bannedUntil = 0L;
        public String banReason = "";
        public String bannedBy = "";
        public long bannedAt = 0L;
        /** @deprecated Prefer {@link #boundPlayers}; kept for Gson migration */
        public String boundPlayerName = "";
        public List<BoundPlayerRecord> boundPlayers = new ArrayList<>();
        public List<PendingBindEntry> pendingBindTokens = new ArrayList<>();
        public List<RememberLoginToken> rememberLoginTokens = new ArrayList<>();
    }

    public static class BoundPlayerRecord {
        public String playerName = "";
        public long lastBoundAt = 0L;
        public boolean needsRebind = false;
    }

    public static class PendingBindEntry {
        public String tokenHash = "";
        public long createdAt = 0L;
        public long expiresAt = 0L;
    }

    public static class RememberLoginToken {
        public String tokenHash = "";
        public long createdAt = 0L;
        public long lastUsedAt = 0L;
        public long expiresAt = 0L;
    }

    public static class BanInfo {
        public boolean banned = false;
        public boolean permanent = false;
        public long remainingMillis = 0L;
        public String reason = "";
        public String bannedBy = "";
        public long bannedAt = 0L;

        static BanInfo notBanned() {
            return new BanInfo();
        }
    }
}
