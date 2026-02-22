package org.lintx.plugins.yinwuchat.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.velocity.YinwuChat;
import org.lintx.plugins.yinwuchat.velocity.config.Config;
import org.lintx.plugins.yinwuchat.velocity.config.PlayerConfig;
import org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWebSocketFrameHandler;

import java.util.List;

/**
 * 隐身命令 (/vanish)
 */
public class VanishCommand implements SimpleCommand {
    private final YinwuChat plugin;

    public VanishCommand(YinwuChat plugin) {
        this.plugin = plugin;
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

        settings.vanished = !settings.vanished;
        playerConfig.saveSettings(settings);

        if (settings.vanished) {
            player.sendMessage(Component.text("✓ 已开启隐身模式，你不会出现在玩家列表中").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("✓ 已关闭隐身模式").color(NamedTextColor.GREEN));
        }

        // 广播更新后的玩家列表给所有 Web 客户端
        VelocityWebSocketFrameHandler.broadcastPlayerList(plugin);
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
        return player.hasPermission(Const.PERMISSION_VANISH) || Config.getInstance().isAdmin(player);
    }
}
