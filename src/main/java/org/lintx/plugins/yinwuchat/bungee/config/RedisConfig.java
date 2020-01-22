package org.lintx.plugins.yinwuchat.bungee.config;

import org.lintx.plugins.modules.configure.YamlConfig;
import org.lintx.plugins.yinwuchat.json.MessageFormat;

import java.util.ArrayList;
import java.util.List;

public class RedisConfig {
    @YamlConfig
    public boolean openRedis = false;

    @YamlConfig
    public String ip = "";

    @YamlConfig
    public int port = 0;

    @YamlConfig
    public int maxConnection = 8;

    @YamlConfig
    public String password = "";

    @YamlConfig
    public String selfName = "bc1";

    @YamlConfig
    public boolean forwardBcTask = true;

    @YamlConfig
    public boolean forwardBcMessageToQQ = true;

    @YamlConfig
    public boolean forwardBcMessageToWeb = true;

    @YamlConfig
    public boolean forwardBcAtAll = true;

    @YamlConfig
    public boolean forwardBcAtOne = true;

    @YamlConfig
    public List<MessageFormat> selfPrefixFormat = new ArrayList<>();
}
