package org.lintx.plugins.yinwuchat.velocity.json;

import java.util.List;

/**
 * 物品响应数据类
 * 用于后端服务器向 Velocity 返回物品信息
 */
public class ItemResponse {
    public String playerName;
    public String requestType; // "hand", "inventory", "enderchest"
    public boolean success;
    public String errorMessage;
    public List<String> items; // 物品的 JSON 字符串列表

    public ItemResponse() {}

    public ItemResponse(String playerName, String requestType, boolean success) {
        this.playerName = playerName;
        this.requestType = requestType;
        this.success = success;
    }

    public ItemResponse(String playerName, String requestType, String errorMessage) {
        this.playerName = playerName;
        this.requestType = requestType;
        this.success = false;
        this.errorMessage = errorMessage;
    }

    public ItemResponse(String playerName, String requestType, List<String> items) {
        this.playerName = playerName;
        this.requestType = requestType;
        this.success = true;
        this.items = items;
    }
}
