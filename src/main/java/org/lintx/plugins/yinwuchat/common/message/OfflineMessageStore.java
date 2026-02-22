package org.lintx.plugins.yinwuchat.common.message;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 离线私聊消息存储（服务端持久化）
 */
public class OfflineMessageStore {
    public static class OfflineMessage {
        public String from;
        public String to;
        public String message;
        public long time;
    }

    private final File storeFile;
    private final Gson gson = new Gson();
    private Map<String, List<OfflineMessage>> cache;

    public OfflineMessageStore(File dataFolder) {
        this.storeFile = new File(dataFolder, "offline-messages.json");
    }

    public synchronized void addMessage(String toPlayer, OfflineMessage message) {
        if (toPlayer == null || toPlayer.isEmpty() || message == null) return;
        Map<String, List<OfflineMessage>> data = load();
        String key = normalizeKey(toPlayer);
        data.computeIfAbsent(key, k -> new ArrayList<>()).add(message);
        save(data);
    }

    public synchronized List<OfflineMessage> consumeMessages(String toPlayer) {
        if (toPlayer == null || toPlayer.isEmpty()) return new ArrayList<>();
        Map<String, List<OfflineMessage>> data = load();
        String key = normalizeKey(toPlayer);
        List<OfflineMessage> list = data.remove(key);
        save(data);
        return list != null ? list : new ArrayList<>();
    }

    public synchronized Map<String, Integer> getCounts(String toPlayer) {
        Map<String, Integer> result = new HashMap<>();
        if (toPlayer == null || toPlayer.isEmpty()) return result;
        Map<String, List<OfflineMessage>> data = load();
        String key = normalizeKey(toPlayer);
        List<OfflineMessage> list = data.get(key);
        if (list == null) return result;
        for (OfflineMessage message : list) {
            if (message == null || message.from == null) continue;
            String fromKey = normalizeKey(message.from);
            result.put(fromKey, result.getOrDefault(fromKey, 0) + 1);
        }
        return result;
    }

    private String normalizeKey(String playerName) {
        return playerName.trim().toLowerCase();
    }

    private Map<String, List<OfflineMessage>> load() {
        if (cache != null) return cache;
        if (!storeFile.exists()) {
            cache = new HashMap<>();
            return cache;
        }
        try (Reader reader = new FileReader(storeFile)) {
            Type type = new TypeToken<Map<String, List<OfflineMessage>>>() {}.getType();
            Map<String, List<OfflineMessage>> data = gson.fromJson(reader, type);
            cache = data != null ? data : new HashMap<>();
            return cache;
        } catch (Exception e) {
            cache = new HashMap<>();
            return cache;
        }
    }

    private void save(Map<String, List<OfflineMessage>> data) {
        cache = data;
        try (FileWriter writer = new FileWriter(storeFile)) {
            gson.toJson(data, writer);
        } catch (Exception ignored) {
        }
    }
}
