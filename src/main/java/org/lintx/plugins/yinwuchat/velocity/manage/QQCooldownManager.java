package org.lintx.plugins.yinwuchat.velocity.manage;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QQ消息发送冷却时间管理器
 * 每个玩家独立管理，5秒冷却时间
 */
public class QQCooldownManager {
    private static final long COOLDOWN_SECONDS = 5;
    private static final ConcurrentHashMap<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    /**
     * 检查玩家是否可以发送QQ消息
     * @param playerUUID 玩家UUID
     * @return true表示可以发送，false表示仍在冷却中
     */
    public static boolean canSend(UUID playerUUID) {
        Long lastSendTime = cooldowns.get(playerUUID);
        if (lastSendTime == null) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsedSeconds = (currentTime - lastSendTime) / 1000;
        
        return elapsedSeconds >= COOLDOWN_SECONDS;
    }

    /**
     * 获取剩余的冷却时间（秒）
     * @param playerUUID 玩家UUID
     * @return 剩余冷却时间（秒），如果不在冷却中则返回0
     */
    public static long getRemainingCooldown(UUID playerUUID) {
        Long lastSendTime = cooldowns.get(playerUUID);
        if (lastSendTime == null) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsedSeconds = (currentTime - lastSendTime) / 1000;
        
        if (elapsedSeconds >= COOLDOWN_SECONDS) {
            return 0;
        }
        
        return COOLDOWN_SECONDS - elapsedSeconds;
    }

    /**
     * 记录玩家发送QQ消息的时间
     * @param playerUUID 玩家UUID
     */
    public static void recordSend(UUID playerUUID) {
        cooldowns.put(playerUUID, System.currentTimeMillis());
    }

    /**
     * 清除玩家的冷却时间（用于测试或特殊情况）
     * @param playerUUID 玩家UUID
     */
    public static void clearCooldown(UUID playerUUID) {
        cooldowns.remove(playerUUID);
    }

    /**
     * 清除所有冷却时间
     */
    public static void clearAll() {
        cooldowns.clear();
    }
}








