package org.lintx.plugins.yinwuchat.velocity.json;

/**
 * 物品请求数据类
 * 用于 Velocity 向后端服务器请求玩家物品信息
 */
public class ItemRequest {
    public String playerName;
    public String requestType; // "hand", "inventory", "enderchest"
    public String targetPlayer; // 如果是查看其他玩家的物品

    public ItemRequest() {}

    public ItemRequest(String playerName, String requestType) {
        this.playerName = playerName;
        this.requestType = requestType;
    }

    public ItemRequest(String playerName, String requestType, String targetPlayer) {
        this.playerName = playerName;
        this.requestType = requestType;
        this.targetPlayer = targetPlayer;
    }
}
