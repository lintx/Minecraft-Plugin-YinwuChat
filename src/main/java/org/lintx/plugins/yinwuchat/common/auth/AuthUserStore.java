package org.lintx.plugins.yinwuchat.common.auth;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
        save();
        return true;
    }

    public synchronized boolean bindPlayerName(String username, String playerName) {
        UserRecord record = users.get(normalize(username));
        if (record == null) {
            return false;
        }
        record.boundPlayerName = playerName == null ? "" : playerName.trim();
        save();
        return true;
    }

    public synchronized boolean unbindPlayerName(String username) {
        UserRecord record = users.get(normalize(username));
        if (record == null) {
            return false;
        }
        record.boundPlayerName = "";
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
            String bound = normalizePlayerName(record.boundPlayerName);
            if (!bound.isEmpty() && bound.equals(key)) {
                account = record.username == null ? "" : record.username;
                record.boundPlayerName = "";
                break;
            }
        }
        if (!account.isEmpty()) {
            save();
        }
        return account;
    }

    public synchronized String getBoundPlayerName(String username) {
        UserRecord record = users.get(normalize(username));
        if (record == null) {
            return "";
        }
        return record.boundPlayerName == null ? "" : record.boundPlayerName;
    }

    public synchronized String getAccountByPlayerName(String playerName) {
        String key = normalizePlayerName(playerName);
        if (key.isEmpty()) {
            return "";
        }
        for (UserRecord record : users.values()) {
            String bound = normalizePlayerName(record.boundPlayerName);
            if (!bound.isEmpty() && bound.equals(key)) {
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

    public static class UserRecord {
        public String username = "";
        public String salt = "";
        public String saltedHash = "";
        public long createdAt = 0L;
        public long bannedUntil = 0L;
        public String banReason = "";
        public String bannedBy = "";
        public long bannedAt = 0L;
        public String boundPlayerName = "";
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
