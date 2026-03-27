package org.lintx.plugins.yinwuchat.Util;

public final class AdminAlertCommandUtil {

    private AdminAlertCommandUtil() {
    }

    public static String buildAtAllAdminConfirmCommand(String playerName) {
        return "/yinwuchat atalladmin confirm " + (playerName == null ? "" : playerName.trim());
    }
}
