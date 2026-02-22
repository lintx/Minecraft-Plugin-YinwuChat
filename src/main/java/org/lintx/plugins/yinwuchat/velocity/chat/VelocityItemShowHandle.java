package org.lintx.plugins.yinwuchat.velocity.chat;

import net.kyori.adventure.text.Component;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.chat.struct.ChatSource;
import org.lintx.plugins.yinwuchat.velocity.util.VelocityItemUtil;

/**
 * Velocity 版本的物品显示处理器
 * 处理聊天中的 [i]、[inv]、[ec] 占位符
 * 参考 InteractiveChat 的实现方式
 */
public class VelocityItemShowHandle extends VelocityChatHandle {

    @Override
    public void handle(VelocityChat chat) {
        if (chat.source != ChatSource.GAME) return;

        // 处理物品显示 [i] - 使用简单的正则表达式
        handle(chat, "\\[i\\]", (matcher) -> {
            if (chat.items != null && !chat.items.isEmpty()) {
                Component itemComponent = chat.items.remove(0);
                return itemComponent;
            } else {
                // 没有物品时显示提示信息
                return Component.text("[物品]").color(net.kyori.adventure.text.format.NamedTextColor.GRAY);
            }
        });

        // 处理背包显示 [inv:playername]
        handle(chat, Const.INVENTORY_PLACEHOLDER, (matcher) -> {
            String playerName = matcher.group(2);
            if (playerName == null || playerName.trim().isEmpty()) {
                playerName = chat.fromPlayer.playerName;
            } else {
                playerName = playerName.trim();
            }

            // 这里需要检查玩家是否有查看背包的权限
            // 暂时创建一个简单的背包显示组件
            return VelocityItemUtil.createInventoryComponent("", playerName);
        });

        // 处理末影箱显示 [ec:playername]
        handle(chat, Const.ENDER_CHEST_PLACEHOLDER, (matcher) -> {
            String playerName = matcher.group(2);
            if (playerName == null || playerName.trim().isEmpty()) {
                playerName = chat.fromPlayer.playerName;
            } else {
                playerName = playerName.trim();
            }

            // 这里需要检查玩家是否有查看末影箱的权限
            // 暂时创建一个简单的末影箱显示组件
            return VelocityItemUtil.createEnderChestComponent("", playerName);
        });
    }
}
