package org.lintx.plugins.yinwuchat.common.auth;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AuthUserStoreRememberLoginTest {

    @Test
    public void issuedRememberTokenCanBeResolvedBackToUserBeforeExpiry() {
        File dataFolder = new File("target/test-data/auth-store-remember-1");
        deleteRecursively(dataFolder);
        AuthUserStore store = new AuthUserStore(dataFolder);
        store.register("Alice", repeat('a', 64));

        String token = store.issueRememberLoginToken("Alice", 1_000L, 100L);

        assertFalse(token.isEmpty());
        assertEquals("Alice", store.findUserByRememberLoginToken(token, 500L));
    }

    @Test
    public void expiredRememberTokenCanNoLongerBeUsed() {
        File dataFolder = new File("target/test-data/auth-store-remember-2");
        deleteRecursively(dataFolder);
        AuthUserStore store = new AuthUserStore(dataFolder);
        store.register("Alice", repeat('b', 64));

        String token = store.issueRememberLoginToken("Alice", 1_000L, 100L);

        assertEquals("", store.findUserByRememberLoginToken(token, 1_101L));
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
