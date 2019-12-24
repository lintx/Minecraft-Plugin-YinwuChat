package org.lintx.plugins.yinwuchat.bungee.json;

import net.md_5.bungee.api.chat.BaseComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RedisMessage {
    public RedisMessageType type = RedisMessageType.UNKNOWN;
    public String toPlayerName = "";
    public String message = "";
    public transient BaseComponent chat;
    public String fromServer = "";
    public UUID fromPlayerUUID;
    public String toServer = "";
    public String toMCServer = "";
    public List<String> playerList = new ArrayList<>();
}
