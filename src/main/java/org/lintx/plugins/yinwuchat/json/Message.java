package org.lintx.plugins.yinwuchat.json;

import java.util.ArrayList;
import java.util.List;

public class Message {
    public String player = "";
    public String chat = "";
    public String serverName = ""; // 玩家所在的服务器名称
    public List<String> items = null;
    public List<HandleConfig> handles = new ArrayList<>();
}
