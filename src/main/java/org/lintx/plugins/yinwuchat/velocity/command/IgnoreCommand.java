package org.lintx.plugins.yinwuchat.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.lintx.plugins.yinwuchat.velocity.YinwuChat;
import org.lintx.plugins.yinwuchat.velocity.config.PlayerConfig;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 忽略玩家命令 (/ignore <玩家名>)
 */
public class IgnoreCommand implements SimpleCommand {
    private final ProxyServer proxy;

    public IgnoreCommand(YinwuChat plugin) {
        this.proxy = plugin.getProxy();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("该命令只能在游戏内执行").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;

        if (args.length < 1) {
            player.sendMessage(Component.text("用法: /ignore <玩家名>").color(NamedTextColor.RED));
            return;
        }

        String targetName = args[0];
        if (targetName.equalsIgnoreCase(player.getUsername())) {
            player.sendMessage(Component.text("✗ 你不能忽略你自己").color(NamedTextColor.RED));
            return;
        }

        PlayerConfig playerConfig = PlayerConfig.getInstance();
        PlayerConfig.PlayerSettings settings = playerConfig.getSettings(player);

        if (settings.isIgnored(targetName)) {
            settings.unignorePlayer(targetName);
            playerConfig.saveSettings(settings);
            player.sendMessage(Component.text("✓ 已取消忽略 " + targetName).color(NamedTextColor.GREEN));
        } else {
            settings.ignorePlayer(targetName);
            playerConfig.saveSettings(settings);
            player.sendMessage(Component.text("✓ 已忽略 " + targetName).color(NamedTextColor.GREEN));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase();
            return proxy.getAllPlayers().stream()
                .map(Player::getUsername)
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player)) {
            return true;
        }
        Player player = (Player) source;
        return player.hasPermission(org.lintx.plugins.yinwuchat.Const.PERMISSION_IGNORE) || org.lintx.plugins.yinwuchat.velocity.config.Config.getInstance().isDefault(player);
    }
}
