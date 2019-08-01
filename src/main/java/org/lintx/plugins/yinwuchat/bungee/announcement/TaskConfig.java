package org.lintx.plugins.yinwuchat.bungee.announcement;

import org.lintx.plugins.modules.configure.YamlConfig;
import org.lintx.plugins.yinwuchat.json.MessageFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@YamlConfig
public class TaskConfig {
    @YamlConfig
    public boolean enable = false;
    @YamlConfig
    public int interval = 30;
    @YamlConfig
    public List<MessageFormat> list = new ArrayList<>();
    @YamlConfig
    public String server = "all";

    public LocalDateTime lastTime = null;
}
