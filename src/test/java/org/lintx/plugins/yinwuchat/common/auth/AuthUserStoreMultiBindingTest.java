package org.lintx.plugins.yinwuchat.common.auth;

import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AuthUserStoreMultiBindingTest {

    @Test
    public void multipleBoundPlayersCanBeAddedAndRemoved() {
        File dataFolder = new File("target/test-data/auth-store-multi-1");
        deleteRecursively(dataFolder);
        AuthUserStore store = new AuthUserStore(dataFolder);
        store.register("Alice", repeat('a', 64));
        store.addOrUpdateBoundPlayerFromGame("Alice", "P1", 1000L, false);
        store.addOrUpdateBoundPlayerFromGame("Alice", "P2", 2000L, false);
        assertEquals(2, store.getBoundPlayers("Alice").size());
        assertTrue(store.removeBoundPlayer("Alice", "P1"));
        List<AuthUserStore.BoundPlayerRecord> left = store.getBoundPlayers("Alice");
        assertEquals(1, left.size());
        assertEquals("P2", left.get(0).playerName);
    }

    @Test
    public void legacyBoundPlayerNameMigratesIntoBoundPlayersList() throws Exception {
        File dataFolder = new File("target/test-data/auth-store-multi-legacy");
        deleteRecursively(dataFolder);
        assertTrue(dataFolder.mkdirs());
        File json = new File(dataFolder, "web-users.json");
        String jsonBody = "{"
                + "\"alice\":{"
                + "\"username\":\"Alice\","
                + "\"salt\":\"s\","
                + "\"saltedHash\":\"h\","
                + "\"createdAt\":0,"
                + "\"boundPlayerName\":\"OldPlayer\","
                + "\"boundPlayers\":[]"
                + "}}";
        try (FileWriter w = new FileWriter(json, StandardCharsets.UTF_8)) {
            w.write(jsonBody);
        }
        AuthUserStore store = new AuthUserStore(dataFolder);
        List<AuthUserStore.BoundPlayerRecord> list = store.getBoundPlayers("Alice");
        assertEquals(1, list.size());
        assertEquals("OldPlayer", list.get(0).playerName);
    }

    @Test
    public void pendingBindTokenFindsAccount() {
        File dataFolder = new File("target/test-data/auth-store-multi-pending");
        deleteRecursively(dataFolder);
        AuthUserStore store = new AuthUserStore(dataFolder);
        store.register("Alice", repeat('b', 64));
        assertTrue(store.registerPendingBindToken("Alice", "rawtoken", 1000L, 3_600_000L));
        assertEquals("Alice", store.findAccountByPendingBindToken("rawtoken", 1000L));
    }

    @Test
    public void markNeedsRebindKeepsPlayerInList() {
        File dataFolder = new File("target/test-data/auth-store-multi-rebind");
        deleteRecursively(dataFolder);
        AuthUserStore store = new AuthUserStore(dataFolder);
        store.register("Alice", repeat('c', 64));
        store.addOrUpdateBoundPlayerFromGame("Alice", "P1", 1000L, false);
        store.markBoundPlayerNeedsRebind("Alice", "P1", true);
        List<AuthUserStore.BoundPlayerRecord> list = store.getBoundPlayers("Alice");
        assertEquals(1, list.size());
        assertTrue(list.get(0).needsRebind);
    }

    private static String repeat(char c, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(c);
        }
        return builder.toString();
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
