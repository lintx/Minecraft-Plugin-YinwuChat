package org.lintx.plugins.yinwuchat.json;

import org.lintx.plugins.modules.configure.YamlConfig;

import java.util.List;

public class HandleConfig {
    @YamlConfig
    public String placeholder;

    @YamlConfig
    public List<MessageFormat> format;
}
