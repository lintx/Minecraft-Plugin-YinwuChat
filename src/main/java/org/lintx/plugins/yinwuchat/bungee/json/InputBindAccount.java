package org.lintx.plugins.yinwuchat.bungee.json;

/**
 * Web 端绑定账号与玩家名
 */
public class InputBindAccount extends InputBase {
    private final String account;

    public String getAccount() {
        return account;
    }

    InputBindAccount(String account) {
        this.account = account == null ? "" : account;
    }
}
