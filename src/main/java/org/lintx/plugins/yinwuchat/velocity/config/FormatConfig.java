package org.lintx.plugins.yinwuchat.velocity.config;

import org.lintx.plugins.modules.configure.YamlConfig;
import org.lintx.plugins.yinwuchat.json.MessageFormat;

import java.util.List;

/**
 * 消息格式配置类（Velocity 版本）
 */
public class FormatConfig {

    @YamlConfig
    public List<MessageFormat> format = null;

    @YamlConfig
    public List<MessageFormat> qqFormat = null;

    @YamlConfig
    public List<MessageFormat> toFormat = null;

    @YamlConfig
    public List<MessageFormat> fromFormat = null;

    @YamlConfig
    public List<MessageFormat> monitorFormat = null;
}








