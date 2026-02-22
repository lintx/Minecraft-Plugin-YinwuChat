package org.lintx.plugins.yinwuchat.velocity.config;

import org.lintx.plugins.modules.configure.YamlConfig;

/**
 * CoolQ/OneBot 配置类（Velocity 版本）
 */
public class CoolQConfig {

    @YamlConfig
    public boolean coolQQQToGame = true;

    @YamlConfig
    public String coolqToGameStart = "";

    @YamlConfig
    public boolean coolQGameToQQ = true;

    @YamlConfig
    public String gameToCoolqStart = "";

    @YamlConfig
    public String qqDenyStyle = "0-9a-fA-Fk-oK-OrRxX";

    @YamlConfig
    public long coolQGroup = 0;

    @YamlConfig
    public String coolQAccessToken = "";

    @YamlConfig
    public String qqAtText = "&7[@{qq}]&r";

    @YamlConfig
    public String qqImageText = "&7[图片]&r";

    @YamlConfig
    public String qqRecordText = "&7[语音]&r";
}








