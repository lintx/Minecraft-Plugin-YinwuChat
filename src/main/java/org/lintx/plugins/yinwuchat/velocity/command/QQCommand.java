package org.lintx.plugins.yinwuchat.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.lintx.plugins.yinwuchat.velocity.YinwuChat;
import org.lintx.plugins.yinwuchat.velocity.config.Config;
import org.lintx.plugins.yinwuchat.velocity.manage.QQCooldownManager;
import org.lintx.plugins.yinwuchat.velocity.message.MessageManage;

import java.util.List;

/**
 * QQ消息发送命令处理器 (/qq)
 * 允许玩家通过命令将消息发送到QQ群
 * 每条消息之间有5秒冷却时间
 */
public class QQCommand implements SimpleCommand {
    private final YinwuChat plugin;

    public QQCommand(YinwuChat plugin) {
        this.plugin = plugin;
    }
    
    @SuppressWarnings("unused")
    private YinwuChat getPlugin() {
        return plugin; // 保留以备将来使用
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        // 只能由玩家执行
        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("此命令只能由玩家执行").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;

        // 检查是否启用了QQ群同步（优先使用 AQQBot 配置）
        Config config = Config.getInstance();
        boolean enabled;
        if (config.aqqBotConfig != null) {
            enabled = config.aqqBotConfig.gameToQQ;
        } else {
            enabled = config.coolQConfig != null && config.coolQConfig.coolQGameToQQ;
        }
        
        if (!enabled) {
            player.sendMessage(Component.text("QQ群消息同步功能未启用").color(NamedTextColor.RED));
            return;
        }

        // 检查是否有消息内容
        if (args.length == 0) {
            player.sendMessage(Component.text("用法: /qq <消息>").color(NamedTextColor.RED));
            player.sendMessage(Component.text("示例: /qq 大家好！").color(NamedTextColor.GRAY));
            return;
        }

        // 拼接消息内容
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) messageBuilder.append(" ");
            messageBuilder.append(args[i]);
        }
        String message = messageBuilder.toString();

        // 检查冷却时间
        if (!QQCooldownManager.canSend(player.getUniqueId())) {
            long remaining = QQCooldownManager.getRemainingCooldown(player.getUniqueId());
            player.sendMessage(Component.text("发送消息到QQ群需要等待 " + remaining + " 秒").color(NamedTextColor.RED));
            return;
        }

        // 检查是否需要特定的消息前缀（优先使用 AQQBot 配置）
        String gameToQQStart = null;
        if (config.aqqBotConfig != null) {
            gameToQQStart = config.aqqBotConfig.gameToQQStart;
        } else if (config.coolQConfig != null) {
            gameToQQStart = config.coolQConfig.gameToCoolqStart;
        }
        
        if (gameToQQStart != null && !gameToQQStart.isEmpty()) {
            if (!message.startsWith(gameToQQStart)) {
                player.sendMessage(Component.text("发送到QQ群的消息必须以 \"" + gameToQQStart + "\" 开头").color(NamedTextColor.RED));
                return;
            }
            // 移除前缀后再发送
            message = message.substring(gameToQQStart.length()).trim();
        }

        // 发送消息到QQ群
        boolean success = MessageManage.getInstance().sendMessageToQQ(player, message);
        
        if (success) {
            // 记录发送时间
            QQCooldownManager.recordSend(player.getUniqueId());
            player.sendMessage(Component.text("消息已发送到QQ群").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("消息发送失败，QQ群连接可能未建立").color(NamedTextColor.RED));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        // 不提供自动补全
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player)) {
            return true;
        }
        Player player = (Player) source;
        // 权限检查
        return player.hasPermission(org.lintx.plugins.yinwuchat.Const.PERMISSION_QQ) || Config.getInstance().isDefault(player);
    }
}

