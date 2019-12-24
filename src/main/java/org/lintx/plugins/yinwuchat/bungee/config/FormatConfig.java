package org.lintx.plugins.yinwuchat.bungee.config;

import org.lintx.plugins.modules.configure.YamlConfig;
import org.lintx.plugins.yinwuchat.json.MessageFormat;

import java.util.List;

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
