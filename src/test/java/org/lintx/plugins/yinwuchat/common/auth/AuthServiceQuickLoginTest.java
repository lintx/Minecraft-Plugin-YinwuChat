package org.lintx.plugins.yinwuchat.common.auth;

import com.google.gson.JsonObject;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuthServiceQuickLoginTest {

    @Test
    public void quickLoginReturnsUserAndRefreshesRememberToken() {
        File dataFolder = new File("target/test-data/auth-service-quick-login-success");
        deleteRecursively(dataFolder);
        AuthUserStore store = new AuthUserStore(dataFolder);
        store.register("QuickUser", repeat('c', 64));
        String rememberToken = store.issueRememberLoginToken("QuickUser", 60_000L);

        AuthService service = AuthService.getInstance(dataFolder);
        JsonObject result = service.handleQuickLogin("{\"rememberToken\":\"" + rememberToken + "\"}");

        assertTrue(result.get("ok").getAsBoolean());
        assertEquals("QuickUser", result.get("username").getAsString());
        assertTrue(result.has("rememberToken"));
        assertFalse(result.get("rememberToken").getAsString().isEmpty());
        assertFalse(rememberToken.equals(result.get("rememberToken").getAsString()));
    }

    @Test
    public void quickLoginRejectsUnknownRememberToken() {
        File dataFolder = new File("target/test-data/auth-service-quick-login-failure");
        deleteRecursively(dataFolder);
        AuthUserStore store = new AuthUserStore(dataFolder);
        store.register("QuickUser", repeat('d', 64));

        AuthService service = AuthService.getInstance(dataFolder);
        JsonObject result = service.handleQuickLogin("{\"rememberToken\":\"missing-token\"}");

        assertFalse(result.get("ok").getAsBoolean());
        assertEquals("登录凭证已失效，请重新输入账号密码", result.get("message").getAsString());
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
