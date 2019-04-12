package org.lintx.plugins.yinwuchat.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.java_websocket.WebSocket;

public class PrivateMessageConf {
    public ProxiedPlayer player = null;
    public String name = null;
    public WebSocket webSocket = null;
    public PlayerConfig.Player config = null;
}
