package org.lintx.plugins.yinwuchat.velocity.config;

import org.lintx.plugins.modules.configure.YamlConfig;

/**
 * AQQBot (OneBot标准) 配置类
 * 支持 AQQBot、Lagrange、LLoneBot 等基于 OneBot 标准的 QQ 机器人框架
 */
public class AQQBotConfig {

    /**
     * QQ群有新消息时是否发送到游戏中
     */
    @YamlConfig
    public boolean qqToGame = true;

    /**
     * QQ群有新消息时，只有开头跟这里一样才发送到游戏中
     */
    @YamlConfig
    public String qqToGameStart = "";

    /**
     * 游戏中有新消息时是否发送到QQ群中
     */
    @YamlConfig
    public boolean gameToQQ = true;

    /**
     * 游戏中有新消息时，只有开头跟这里一样才发送到QQ群中
     */
    @YamlConfig
    public String gameToQQStart = "";

    /**
     * 转发QQ群消息到游戏时禁用的样式代码
     */
    @YamlConfig
    public String qqDenyStyle = "0-9a-fA-Fk-oK-OrRxX";

    /**
     * 监听的QQ群的群号，AQQBot接收到消息时，如果是QQ群，且群号和这里一致，就会转发到游戏中
     */
    @YamlConfig
    public long qqGroup = 0;

    /**
     * 和 AQQBot WebSocket 通信时使用的 Access Token，为空时不验证，强烈建议设置为一个复杂的字符串
     */
    @YamlConfig
    public String accessToken = "";

    /**
     * QQ群中群员发送的@信息将被替换为这个文本
     * {qq}将被替换为被@的人的QQ号
     */
    @YamlConfig
    public String qqAtText = "&7[@{qq}]&r";

    /**
     * QQ群中群员发送的图片将被替换为这个文本
     */
    @YamlConfig
    public String qqImageText = "&7[图片]&r";

    /**
     * QQ群中群员发送的语音将被替换为这个文本
     */
    @YamlConfig
    public String qqRecordText = "&7[语音]&r";
}

