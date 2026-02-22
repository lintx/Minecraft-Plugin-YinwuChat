package org.lintx.plugins.yinwuchat.velocity.message;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Web 端聊天增量存储与阅读游标存储。
 * 使用 JSONL 增量写入消息，游标单独写入 JSON 文件。
 */
public class WebChatReplayStore {

    public static class PublicMessageRecord {
        public long id;
        public long time;
        public String player;
        public String server;
        public String message;
        public String rawChat;
        public List<String> rawItems;
    }

    public static class PrivateMessageRecord {
        public long id;
        public long time;
        public String from;
        public String to;
        public String message;
    }

    public static class ReplaySnapshot {
        public long publicLastReadId;
        public Map<String, Long> privateLastReadByPeer;
        public List<PublicMessageRecord> publicMessages;
        public List<PrivateMessageRecord> privateMessages;
    }

    private static class CursorState {
        Map<String, Long> publicCursors = new HashMap<>();
        Map<String, Map<String, Long>> privateCursors = new HashMap<>();
    }

    private static final int MAX_PUBLIC_RECORDS = 50000;
    private static final int MAX_PRIVATE_RECORDS = 100000;

    private final Gson gson = new Gson();
    private final File publicLogFile;
    private final File privateLogFile;
    private final File cursorFile;

    private final LinkedList<PublicMessageRecord> publicRecords = new LinkedList<>();
    private final LinkedList<PrivateMessageRecord> privateRecords = new LinkedList<>();
    private CursorState cursorState = new CursorState();
    private long nextId = 1L;

    public WebChatReplayStore(File dataFolder) {
        this.publicLogFile = new File(dataFolder, "web-public-messages.jsonl");
        this.privateLogFile = new File(dataFolder, "web-private-messages.jsonl");
        this.cursorFile = new File(dataFolder, "web-read-cursors.json");
        loadAll();
    }

    public synchronized long appendPublicMessage(String player, String server, String message, String rawChat, List<String> rawItems) {
        PublicMessageRecord record = new PublicMessageRecord();
        record.id = nextId++;
        record.time = System.currentTimeMillis();
        record.player = safe(player);
        record.server = safe(server);
        record.message = safe(message);
        record.rawChat = safe(rawChat);
        record.rawItems = rawItems == null ? Collections.emptyList() : new ArrayList<>(rawItems);
        publicRecords.add(record);
        while (publicRecords.size() > MAX_PUBLIC_RECORDS) {
            publicRecords.removeFirst();
        }
        appendJsonLine(publicLogFile, gson.toJson(record));
        return record.id;
    }

    public synchronized long appendPrivateMessage(String from, String to, String message) {
        PrivateMessageRecord record = new PrivateMessageRecord();
        record.id = nextId++;
        record.time = System.currentTimeMillis();
        record.from = safe(from);
        record.to = safe(to);
        record.message = safe(message);
        privateRecords.add(record);
        while (privateRecords.size() > MAX_PRIVATE_RECORDS) {
            privateRecords.removeFirst();
        }
        appendJsonLine(privateLogFile, gson.toJson(record));
        return record.id;
    }

    public synchronized ReplaySnapshot createSnapshot(String playerName, int publicLimit, int privateLimit) {
        String userKey = normalizeKey(playerName);
        ReplaySnapshot snapshot = new ReplaySnapshot();
        snapshot.publicLastReadId = cursorState.publicCursors.getOrDefault(userKey, 0L);
        snapshot.privateLastReadByPeer = new HashMap<>(cursorState.privateCursors.getOrDefault(userKey, Collections.emptyMap()));
        snapshot.publicMessages = listPublicAfter(snapshot.publicLastReadId, publicLimit);
        snapshot.privateMessages = listUnreadPrivate(userKey, snapshot.privateLastReadByPeer, privateLimit);
        return snapshot;
    }

    public synchronized void updateReadCursor(String playerName, String chat, long messageId) {
        if (messageId <= 0) return;
        String userKey = normalizeKey(playerName);
        if ("public".equalsIgnoreCase(chat)) {
            long current = cursorState.publicCursors.getOrDefault(userKey, 0L);
            if (messageId > current) {
                cursorState.publicCursors.put(userKey, messageId);
                saveCursors();
            }
            return;
        }
        String peerKey = normalizeKey(chat);
        if (peerKey.isEmpty()) return;
        Map<String, Long> privateMap = cursorState.privateCursors.computeIfAbsent(userKey, k -> new HashMap<>());
        long current = privateMap.getOrDefault(peerKey, 0L);
        if (messageId > current) {
            privateMap.put(peerKey, messageId);
            saveCursors();
        }
    }

    private List<PublicMessageRecord> listPublicAfter(long afterId, int limit) {
        List<PublicMessageRecord> list = new ArrayList<>();
        if (limit <= 0) return list;
        for (PublicMessageRecord record : publicRecords) {
            if (record.id > afterId) {
                list.add(record);
                if (list.size() >= limit) break;
            }
        }
        return list;
    }

    private List<PrivateMessageRecord> listUnreadPrivate(String userKey, Map<String, Long> privateCursorMap, int limit) {
        List<PrivateMessageRecord> list = new ArrayList<>();
        if (limit <= 0) return list;
        for (PrivateMessageRecord record : privateRecords) {
            if (!userKey.equals(normalizeKey(record.to))) continue;
            long peerCursor = privateCursorMap.getOrDefault(normalizeKey(record.from), 0L);
            if (record.id > peerCursor) {
                list.add(record);
                if (list.size() >= limit) break;
            }
        }
        return list;
    }

    private void loadAll() {
        long maxId = 0L;
        maxId = Math.max(maxId, loadPublicMessages());
        maxId = Math.max(maxId, loadPrivateMessages());
        loadCursors();
        nextId = Math.max(1L, maxId + 1L);
    }

    private long loadPublicMessages() {
        if (!publicLogFile.exists()) return 0L;
        long maxId = 0L;
        try (BufferedReader reader = new BufferedReader(new FileReader(publicLogFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                PublicMessageRecord record = gson.fromJson(line, PublicMessageRecord.class);
                if (record == null) continue;
                if (record.rawItems == null) record.rawItems = Collections.emptyList();
                publicRecords.add(record);
                maxId = Math.max(maxId, record.id);
            }
            while (publicRecords.size() > MAX_PUBLIC_RECORDS) {
                publicRecords.removeFirst();
            }
        } catch (Exception ignored) {
        }
        return maxId;
    }

    private long loadPrivateMessages() {
        if (!privateLogFile.exists()) return 0L;
        long maxId = 0L;
        try (BufferedReader reader = new BufferedReader(new FileReader(privateLogFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                PrivateMessageRecord record = gson.fromJson(line, PrivateMessageRecord.class);
                if (record == null) continue;
                privateRecords.add(record);
                maxId = Math.max(maxId, record.id);
            }
            while (privateRecords.size() > MAX_PRIVATE_RECORDS) {
                privateRecords.removeFirst();
            }
        } catch (Exception ignored) {
        }
        return maxId;
    }

    private void loadCursors() {
        if (!cursorFile.exists()) {
            cursorState = new CursorState();
            return;
        }
        try (FileReader reader = new FileReader(cursorFile)) {
            Type type = new TypeToken<CursorState>() {}.getType();
            CursorState data = gson.fromJson(reader, type);
            cursorState = data != null ? data : new CursorState();
            if (cursorState.publicCursors == null) cursorState.publicCursors = new HashMap<>();
            if (cursorState.privateCursors == null) cursorState.privateCursors = new HashMap<>();
        } catch (Exception ignored) {
            cursorState = new CursorState();
        }
    }

    private void saveCursors() {
        try (FileWriter writer = new FileWriter(cursorFile)) {
            gson.toJson(cursorState, writer);
        } catch (Exception ignored) {
        }
    }

    private void appendJsonLine(File file, String jsonLine) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write(jsonLine);
            writer.newLine();
        } catch (Exception ignored) {
        }
    }

    private String normalizeKey(String value) {
        return safe(value).trim().toLowerCase();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
