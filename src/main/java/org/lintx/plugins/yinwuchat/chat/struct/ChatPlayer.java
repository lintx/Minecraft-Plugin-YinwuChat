package org.lintx.plugins.yinwuchat.chat.struct;

import io.netty.channel.Channel;
import org.lintx.plugins.yinwuchat.bungee.config.PlayerConfig;

public class ChatPlayer {
    public PlayerConfig.Player config;
    public String playerName;
    public Channel channel;
}
