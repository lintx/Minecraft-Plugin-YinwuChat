package org.lintx.plugins.yinwuchat.Util;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.lintx.plugins.yinwuchat.velocity.config.PlayerConfig;

/**
 * 私聊视监菜单：可点击快捷命令（最小实现：指定玩家 / 历史 为 SUGGEST_COMMAND）。
 */
public final class MonitorMenuUtil {

    private MonitorMenuUtil() {}

    public static void sendMenu(Player player, PlayerConfig.PlayerSettings settings) {
        PlayerConfig.PlayerSettings s = settings != null
                ? settings
                : PlayerConfig.getInstance().getSettings(player);

        player.sendMessage(
                Component.text("=== YinwuChat视监菜单 ===").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));

        if (s.isPrivateMonitorAll()) {
            player.sendMessage(Component.text("正在视监所有玩家").color(NamedTextColor.GREEN));
        } else if (s.isPrivateMonitorTarget()) {
            String disp = (s.monitorTargetDisplay != null && !s.monitorTargetDisplay.isEmpty())
                    ? s.monitorTargetDisplay
                    : (s.monitorTargetCanonical != null ? s.monitorTargetCanonical : "");
            player.sendMessage(Component.text("正在视监" + disp + "玩家").color(NamedTextColor.GREEN));
        }

        player.sendMessage(clickableLine("监听全部", "/yinwuchat monitor all",
                ClickEvent.runCommand("/yinwuchat monitor all")));
        player.sendMessage(clickableLine("监听指定玩家", "/yinwuchat monitor ",
                ClickEvent.suggestCommand("/yinwuchat monitor ")));
        player.sendMessage(clickableLine("私聊历史", "/yinwuchat monitor history ",
                ClickEvent.suggestCommand("/yinwuchat monitor history ")));
        player.sendMessage(clickableLine("关闭监听", "/yinwuchat monitor off",
                ClickEvent.runCommand("/yinwuchat monitor off")));
    }

    private static Component clickableLine(String label, String showCommand, ClickEvent click) {
        return Component.text("[ " + label + " ]")
                .color(NamedTextColor.AQUA)
                .clickEvent(click)
                .hoverEvent(HoverEvent.showText(
                        Component.text(showCommand).color(NamedTextColor.WHITE)));
    }
}
