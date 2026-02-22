package org.lintx.plugins.yinwuchat.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.lintx.plugins.yinwuchat.velocity.YinwuChat;
import org.lintx.plugins.yinwuchat.velocity.config.PlayerConfig;

import java.util.List;

/**
 * 禁止@命令 (/noat)
 */
public class NoAtCommand implements SimpleCommand {
    public NoAtCommand(YinwuChat plugin) {
        // no-op
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("该命令只能在游戏内执行").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;
        PlayerConfig playerConfig = PlayerConfig.getInstance();
        PlayerConfig.PlayerSettings settings = playerConfig.getSettings(player);

        settings.disableAtMention = !settings.disableAtMention;
        playerConfig.saveSettings(settings);

        if (settings.disableAtMention) {
            player.sendMessage(Component.text("✓ 已禁止其他玩家@你").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("✓ 已允许其他玩家@你").color(NamedTextColor.GREEN));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player)) {
            return true;
        }
        Player player = (Player) source;
        return player.hasPermission(org.lintx.plugins.yinwuchat.Const.PERMISSION_NOAT) || org.lintx.plugins.yinwuchat.velocity.config.Config.getInstance().isDefault(player);
    }
}
