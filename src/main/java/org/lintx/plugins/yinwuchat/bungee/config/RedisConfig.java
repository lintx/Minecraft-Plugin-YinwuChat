package org.lintx.plugins.yinwuchat.bungee.config;

import org.lintx.plugins.modules.configure.YamlConfig;

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
}
