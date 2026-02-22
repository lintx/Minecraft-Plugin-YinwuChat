package org.lintx.plugins.yinwuchat.chat.struct;

import com.velocitypowered.api.proxy.Player;

/**
 * Velocity proxy player representation for chat system
 */
public class VelocityChatPlayer extends ChatPlayer {
    private final Player player;
    private String serverName;

    public VelocityChatPlayer(Player player) {
        this.player = player;
        this.playerName = player.getUsername();
        this.serverName = null; // 自动检测
    }

    public VelocityChatPlayer(Player player, String serverName) {
        this.player = player;
        this.playerName = player.getUsername();
        this.serverName = serverName; // 使用预设的服务器名称
    }

    public VelocityChatPlayer(java.util.UUID uuid, String name, String serverName) {
        this.player = null;
        this.playerName = name;
        this.serverName = serverName;
    }

    public Player getPlayer() {
        return player;
    }

    public String getServerName() {
        // 如果有预设的服务器名称，使用它；否则自动检测
        if (serverName != null && !serverName.trim().isEmpty()) {
            return serverName.trim();
        }
        return player.getCurrentServer()
            .map(server -> server.getServerInfo().getName())
            .orElse("unknown");
    }
}
