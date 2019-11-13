package org.lintx.plugins.yinwuchat.json;

import java.util.ArrayList;
import java.util.List;

public class Message {
    public String player = "";
    public String chat = "";
    public List<String> items = null;
    public List<HandleConfig> handles = new ArrayList<>();
}
