package org.lintx.plugins.yinwuchat.velocity.httpserver;

import io.netty.channel.Channel;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket客户端连接管理器（Velocity版本）
 * 用于管理WebSocket连接，包括 AQQBot (OneBot标准) 连接
 * 支持 AQQBot、Lagrange、LLoneBot 等基于 OneBot 标准的 QQ 机器人框架
 */
public class VelocityWsClientHelper {
    private static final Set<Channel> clients = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<Channel, VelocityWsClientUtil> clientUtils = new ConcurrentHashMap<>();
    private static Channel aqqBot = null; // AQQBot (OneBot标准) 连接
    private static Channel coolQ = null; // 保留以向后兼容

    /**
     * 更新 AQQBot WebSocket 连接
     * @param socket AQQBot 的 WebSocket 连接
     */
    public static void updateAQQBot(Channel socket) {
        if (aqqBot != null) {
            try {
                aqqBot.close();
            } catch (Exception ignored) {
                // 忽略关闭异常
            }
        }
        aqqBot = socket;
        // 同时更新 coolQ 引用以保持向后兼容
        coolQ = socket;
    }

    /**
     * 获取 AQQBot WebSocket 连接
     * @return AQQBot 的 WebSocket 连接，如果未连接则返回null
     */
    public static Channel getAQQBot() {
        return aqqBot != null ? aqqBot : coolQ; // 向后兼容
    }

    /**
     * 更新CoolQ WebSocket连接（向后兼容方法）
     * @param socket CoolQ的WebSocket连接
     * @deprecated 请使用 updateAQQBot() 方法
     */
    @Deprecated
    public static void updateCoolQ(Channel socket) {
        updateAQQBot(socket);
    }

    /**
     * 获取CoolQ WebSocket连接（向后兼容方法）
     * @return CoolQ的WebSocket连接，如果未连接则返回null
     * @deprecated 请使用 getAQQBot() 方法
     */
    @Deprecated
    public static Channel getCoolQ() {
        return getAQQBot();
    }

    /**
     * 添加WebSocket客户端连接
     * @param channel WebSocket连接
     */
    public static void add(Channel channel) {
        clients.add(channel);
    }
    
    /**
     * 添加WebSocket客户端连接和工具
     * @param channel WebSocket连接
     * @param util 客户端工具
     */
    public static void add(Channel channel, VelocityWsClientUtil util) {
        clients.add(channel);
        clientUtils.put(channel, util);
    }

    /**
     * 移除WebSocket客户端连接
     * @param channel WebSocket连接
     */
    public static void remove(Channel channel) {
        clients.remove(channel);
        clientUtils.remove(channel);
    }
    
    /**
     * 获取客户端工具
     * @param channel WebSocket连接
     * @return 客户端工具
     */
    public static VelocityWsClientUtil get(Channel channel) {
        return clientUtils.get(channel);
    }
    
    /**
     * 踢出同UUID的其他连接
     * @param currentChannel 当前连接
     * @param uuid 玩家UUID
     */
    public static void kickOtherWS(Channel currentChannel, UUID uuid) {
        for (Map.Entry<Channel, VelocityWsClientUtil> entry : clientUtils.entrySet()) {
            if (entry.getKey() != currentChannel && entry.getValue().getUuid() != null 
                    && entry.getValue().getUuid().equals(uuid)) {
                entry.getKey().close();
            }
        }
    }

    /**
     * 根据Token获取WebSocket连接
     * @param token Token
     * @return WebSocket连接，如果未找到则返回null
     */
    public static Channel getWebSocket(String token) {
        if (token == null || token.isEmpty()) return null;
        for (Map.Entry<Channel, VelocityWsClientUtil> entry : clientUtils.entrySet()) {
            if (token.equals(entry.getValue().getToken())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 获取所有WebSocket客户端连接
     * @return 所有连接的集合
     */
    public static Set<Channel> getChannels() {
        return Collections.unmodifiableSet(clients);
    }

    /**
     * 清空所有连接
     */
    public static void clear() {
        clients.clear();
        clientUtils.clear();
        aqqBot = null;
        coolQ = null;
    }
    
    /**
     * 获取所有客户端工具映射
     * @return 客户端工具映射
     */
    public static Map<Channel, VelocityWsClientUtil> getClientUtils() {
        return Collections.unmodifiableMap(clientUtils);
    }
}

