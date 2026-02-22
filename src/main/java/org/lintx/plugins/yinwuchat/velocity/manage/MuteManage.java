package org.lintx.plugins.yinwuchat.velocity.manage;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.lintx.plugins.yinwuchat.velocity.YinwuChat;
import org.lintx.plugins.yinwuchat.velocity.config.Config;
import org.lintx.plugins.yinwuchat.velocity.config.PlayerConfig;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Velocity 禁言管理系统
 * 管理玩家的禁言状态
 */
public class MuteManage {
    private static final MuteManage instance = new MuteManage();
    
    private MuteManage() {
    }
    
    public static MuteManage getInstance() {
        return instance;
    }
    
    /**
     * 检查玩家是否被禁言
     * @param player 玩家
     * @return true 表示被禁言，false 表示未被禁言
     */
    public boolean isMuted(Player player) {
        PlayerConfig.PlayerSettings settings = PlayerConfig.getInstance().getSettings(player);
        return settings.isMuted();
    }
    
    /**
     * 检查玩家是否被禁言，并发送提示消息
     * @param player 玩家
     * @return true 表示被禁言（消息被阻止），false 表示未被禁言
     */
    public boolean checkMutedAndNotify(Player player) {
        PlayerConfig.PlayerSettings settings = PlayerConfig.getInstance().getSettings(player);
        
        if (!settings.isMuted()) {
            return false;
        }
        
        // 发送禁言提示
        Config config = Config.getInstance();
        String tipMessage = config.tipsConfig.youismuteTip;
        
        // 添加剩余时间信息
        long remaining = settings.getRemainingMuteTime();
        if (remaining == -1) {
            tipMessage += " (永久禁言)";
        } else if (remaining > 0) {
            tipMessage += " (剩余: " + formatTime(remaining) + ")";
        }
        
        // 添加禁言原因
        if (settings.muteReason != null && !settings.muteReason.isEmpty()) {
            tipMessage += "\n&7原因: &f" + settings.muteReason;
        }
        
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(tipMessage);
        player.sendMessage(component);
        
        return true;
    }
    
    /**
     * 禁言玩家
     * @param targetName 目标玩家名
     * @param duration 禁言时长（秒），0 表示永久
     * @param operatorName 操作者名称
     * @param reason 禁言原因
     * @return true 表示成功，false 表示失败
     */
    public boolean mutePlayer(String targetName, long duration, String operatorName, String reason) {
        PlayerConfig.PlayerSettings settings = PlayerConfig.getInstance().getSettings(targetName);
        
        settings.muted = true;
        settings.mutedBy = operatorName;
        settings.muteReason = reason != null ? reason : "";
        
        if (duration > 0) {
            settings.mutedUntil = System.currentTimeMillis() + (duration * 1000);
        } else {
            settings.mutedUntil = 0;  // 永久禁言
        }
        
        // 保存配置
        PlayerConfig.getInstance().saveSettings(settings);
        
        // 通知被禁言的玩家（如果在线）
        YinwuChat plugin = YinwuChat.getInstance();
        if (plugin != null) {
            Optional<Player> targetPlayer = plugin.getProxy().getPlayer(targetName);
            targetPlayer.ifPresent(player -> {
                String message = "&c你已被 &e" + operatorName + " &c禁言";
                if (duration > 0) {
                    message += " &7(" + formatTime(duration) + ")";
                } else {
                    message += " &7(永久)";
                }
                if (reason != null && !reason.isEmpty()) {
                    message += "\n&7原因: &f" + reason;
                }
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
            });
            
            plugin.getLogger().info("[禁言] " + operatorName + " 禁言了 " + targetName + 
                    " (" + (duration > 0 ? formatTime(duration) : "永久") + ")" +
                    (reason != null && !reason.isEmpty() ? " 原因: " + reason : ""));
        }
        
        return true;
    }
    
    /**
     * 解除玩家禁言
     * @param targetName 目标玩家名
     * @param operatorName 操作者名称
     * @return true 表示成功，false 表示玩家未被禁言
     */
    public boolean unmutePlayer(String targetName, String operatorName) {
        PlayerConfig.PlayerSettings settings = PlayerConfig.getInstance().getSettings(targetName);
        
        if (!settings.muted) {
            return false;
        }
        
        settings.muted = false;
        settings.mutedUntil = 0;
        settings.mutedBy = "";
        settings.muteReason = "";
        
        // 保存配置
        PlayerConfig.getInstance().saveSettings(settings);
        
        // 通知被解除禁言的玩家（如果在线）
        YinwuChat plugin = YinwuChat.getInstance();
        if (plugin != null) {
            Optional<Player> targetPlayer = plugin.getProxy().getPlayer(targetName);
            targetPlayer.ifPresent(player -> {
                String message = "&a你的禁言已被 &e" + operatorName + " &a解除";
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
            });
            
            plugin.getLogger().info("[解禁] " + operatorName + " 解除了 " + targetName + " 的禁言");
        }
        
        return true;
    }
    
    /**
     * 获取玩家的禁言信息
     * @param targetName 目标玩家名
     * @return 禁言信息字符串，如果未被禁言返回 null
     */
    public String getMuteInfo(String targetName) {
        PlayerConfig.PlayerSettings settings = PlayerConfig.getInstance().getSettings(targetName);
        
        if (!settings.isMuted()) {
            return null;
        }
        
        StringBuilder info = new StringBuilder();
        info.append("&e").append(targetName).append(" &c的禁言信息:");
        info.append("\n&7状态: &c禁言中");
        
        long remaining = settings.getRemainingMuteTime();
        if (remaining == -1) {
            info.append("\n&7时长: &c永久");
        } else {
            info.append("\n&7剩余: &e").append(formatTime(remaining));
        }
        
        if (settings.mutedBy != null && !settings.mutedBy.isEmpty()) {
            info.append("\n&7操作者: &f").append(settings.mutedBy);
        }
        
        if (settings.muteReason != null && !settings.muteReason.isEmpty()) {
            info.append("\n&7原因: &f").append(settings.muteReason);
        }
        
        return info.toString();
    }
    
    /**
     * 格式化时间
     * @param seconds 秒数
     * @return 格式化的时间字符串
     */
    public static String formatTime(long seconds) {
        if (seconds <= 0) {
            return "0秒";
        }
        
        long days = TimeUnit.SECONDS.toDays(seconds);
        seconds -= TimeUnit.DAYS.toSeconds(days);
        long hours = TimeUnit.SECONDS.toHours(seconds);
        seconds -= TimeUnit.HOURS.toSeconds(hours);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        seconds -= TimeUnit.MINUTES.toSeconds(minutes);
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("天");
        }
        if (hours > 0) {
            sb.append(hours).append("小时");
        }
        if (minutes > 0) {
            sb.append(minutes).append("分钟");
        }
        if (seconds > 0 || sb.length() == 0) {
            sb.append(seconds).append("秒");
        }
        
        return sb.toString();
    }
    
    /**
     * 解析时间字符串
     * @param timeStr 时间字符串（如 "1d", "2h", "30m", "60s", "1d2h30m"）
     * @return 秒数，-1 表示解析失败
     */
    public static long parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return -1;
        }
        
        // 尝试解析为纯数字（秒）
        try {
            return Long.parseLong(timeStr);
        } catch (NumberFormatException ignored) {
        }
        
        long totalSeconds = 0;
        StringBuilder currentNumber = new StringBuilder();
        
        for (char c : timeStr.toLowerCase().toCharArray()) {
            if (Character.isDigit(c)) {
                currentNumber.append(c);
            } else {
                if (currentNumber.length() == 0) {
                    continue;
                }
                
                long value = Long.parseLong(currentNumber.toString());
                currentNumber = new StringBuilder();
                
                switch (c) {
                    case 'd':  // 天
                        totalSeconds += value * 86400;
                        break;
                    case 'h':  // 小时
                        totalSeconds += value * 3600;
                        break;
                    case 'm':  // 分钟
                        totalSeconds += value * 60;
                        break;
                    case 's':  // 秒
                        totalSeconds += value;
                        break;
                    default:
                        return -1;  // 未知单位
                }
            }
        }
        
        // 处理末尾的数字（默认为秒）
        if (currentNumber.length() > 0) {
            totalSeconds += Long.parseLong(currentNumber.toString());
        }
        
        return totalSeconds > 0 ? totalSeconds : -1;
    }
}
