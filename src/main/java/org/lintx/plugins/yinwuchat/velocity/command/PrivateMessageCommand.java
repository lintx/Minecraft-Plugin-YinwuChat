package org.lintx.plugins.yinwuchat.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.lintx.plugins.yinwuchat.velocity.message.MessageManage;

import java.util.List;
import java.util.Optional;

/**
 * Velocity 私聊命令处理器 (/msg)
 */
public class PrivateMessageCommand implements SimpleCommand {
    private final ProxyServer proxy;

    public PrivateMessageCommand(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        
        // 需要两个参数：玩家名 和 消息
        if (args.length < 2) {
            source.sendMessage(Component.text("用法: /msg <玩家名> <消息>").color(NamedTextColor.RED));
            return;
        }

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("此命令只能由玩家执行").color(NamedTextColor.RED));
            return;
        }

        Player sender = (Player) source;
        String targetName = args[0];
        
        // 拼接消息（将剩余参数用空格连接）
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) messageBuilder.append(" ");
            messageBuilder.append(args[i]);
        }
        String message = messageBuilder.toString();

        // 直接交给 MessageManage 处理，它会检查游戏在线和 Web 在线
        MessageManage.getInstance().handlePrivateMessage(sender, targetName, message);
    }

    @Override
    public List<String> suggest(SimpleCommand.Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        
        if (args.length == 1 && source instanceof Player) {
            // 补全在线玩家列表
            String prefix = args[0].toLowerCase();
            return proxy.getAllPlayers().stream()
                .map(Player::getUsername)
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .toList();
        }
        return List.of();
    }

    @Override
    public boolean hasPermission(SimpleCommand.Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player)) {
            return true;
        }
        Player player = (Player) source;
        return player.hasPermission(org.lintx.plugins.yinwuchat.Const.PERMISSION_MSG) || org.lintx.plugins.yinwuchat.velocity.config.Config.getInstance().isDefault(player);
    }
}
