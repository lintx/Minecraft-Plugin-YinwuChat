package org.lintx.plugins.yinwuchat.bungee.manage;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.lintx.plugins.yinwuchat.bungee.YinwuChat;
import org.lintx.plugins.yinwuchat.bungee.config.Config;
import org.lintx.plugins.yinwuchat.bungee.config.PlayerConfig;
import org.lintx.plugins.yinwuchat.Util.MessageUtil;

import java.util.Optional;

/**
 * BungeeCord 禁言管理系统
 */
public class MuteManage {
    private static final MuteManage instance = new MuteManage();

    private MuteManage() {}

    public static MuteManage getInstance() {
        return instance;
    }

    public boolean isMuted(ProxiedPlayer player) {
        PlayerConfig.Player settings = PlayerConfig.getConfig(player);
        return settings.isMuted();
    }

    public boolean checkMutedAndNotify(ProxiedPlayer player) {
        PlayerConfig.Player settings = PlayerConfig.getConfig(player);
        if (!settings.isMuted()) {
            return false;
        }

        Config config = Config.getInstance();
        String tipMessage = config.tipsConfig.youismuteTip;

        long remaining = settings.getRemainingMuteTime();
        if (remaining == -1) {
            tipMessage += " (永久禁言)";
        } else if (remaining > 0) {
            tipMessage += " (剩余: " + formatTime(remaining) + ")";
        }

        if (settings.muteReason != null && !settings.muteReason.isEmpty()) {
            tipMessage += "\n&7原因: &f" + settings.muteReason;
        }

        player.sendMessage(MessageUtil.newTextComponent(tipMessage));
        return true;
    }

    public boolean mutePlayer(String targetName, long duration, String operatorName, String reason) {
        ProxiedPlayer player = YinwuChat.getPlugin().getProxy().getPlayer(targetName);
        PlayerConfig.Player settings = null;
        if (player != null) {
            settings = PlayerConfig.getConfig(player);
        } else {
            settings = PlayerConfig.getPlayerConfigByName(targetName);
        }
        
        if (settings == null) {
            // 如果离线且未找到配置，目前无法禁言
            return false;
        }

        settings.muted = true;
        settings.mutedBy = operatorName;
        settings.muteReason = reason != null ? reason : "";

        if (duration > 0) {
            settings.mutedUntil = System.currentTimeMillis() + (duration * 1000);
        } else {
            settings.mutedUntil = 0;
        }

        settings.save();

        if (player != null) {
            String tip = "&c你已被 &e" + operatorName + " &c禁言";
            if (duration > 0) tip += " &e" + (duration / 60) + " &c分钟";
            else tip += " &e永久";
            if (reason != null && !reason.isEmpty()) tip += "\n&7原因: &f" + reason;
            player.sendMessage(MessageUtil.newTextComponent(tip));
        }

        return true;
    }

    public boolean unmutePlayer(String targetName, String operatorName) {
        ProxiedPlayer player = YinwuChat.getPlugin().getProxy().getPlayer(targetName);
        if (player != null) {
            PlayerConfig.Player settings = PlayerConfig.getConfig(player);
            settings.muted = false;
            settings.mutedUntil = 0;
            settings.mutedBy = "";
            settings.muteReason = "";
            settings.save();
            player.sendMessage(MessageUtil.newTextComponent("&a你已被 &e" + operatorName + " &a解除禁言"));
            return true;
        }
        return false;
    }

    public static String formatTime(long seconds) {
        if (seconds < 60) return seconds + "秒";
        if (seconds < 3600) return (seconds / 60) + "分" + (seconds % 60) + "秒";
        long hours = seconds / 3600;
        long mins = (seconds % 3600) / 60;
        return hours + "小时" + mins + "分";
    }
}
