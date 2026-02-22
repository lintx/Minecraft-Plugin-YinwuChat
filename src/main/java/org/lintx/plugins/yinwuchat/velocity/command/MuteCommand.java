package org.lintx.plugins.yinwuchat.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.lintx.plugins.yinwuchat.velocity.YinwuChat;
import org.lintx.plugins.yinwuchat.velocity.config.Config;
import org.lintx.plugins.yinwuchat.velocity.manage.MuteManage;

import java.util.Arrays;
import java.util.List;

/**
 * 独立的禁言命令 (/mute, /unmute, /muteinfo)
 */
public class MuteCommand implements SimpleCommand {
    private final YinwuChat plugin;
    private final ProxyServer proxy;
    private final String commandType; // "mute", "unmute", "muteinfo"

    public MuteCommand(YinwuChat plugin, String commandType) {
        this.plugin = plugin;
        this.proxy = plugin.getProxy();
        this.commandType = commandType;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        switch (commandType) {
            case "mute":
                handleMute(source, args);
                break;
            case "unmute":
                handleUnmute(source, args);
                break;
            case "muteinfo":
                handleMuteInfo(source, args);
                break;
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        CommandSource source = invocation.source();
        // 控制台默认拥有权限
        if (!(source instanceof Player)) {
            return true;
        }
        // 玩家检查权限或管理员列表
        Player player = (Player) source;
        return source.hasPermission(org.lintx.plugins.yinwuchat.Const.PERMISSION_MUTE) || Config.getInstance().isAdmin(player);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        
        // 第一个参数：玩家名补全
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase();
            return proxy.getAllPlayers().stream()
                .map(Player::getUsername)
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .toList();
        }
        
        // mute 命令第二个参数：时长建议
        if (commandType.equals("mute") && args.length == 2) {
            String prefix = args[1].toLowerCase();
            return List.of("1h", "30m", "1d", "7d", "30d").stream()
                .filter(s -> s.startsWith(prefix))
                .toList();
        }
        
        return List.of();
    }

    /**
     * 显示禁言命令帮助信息
     */
    private void showMuteHelp(CommandSource source) {
        source.sendMessage(Component.text("=== 禁言命令帮助 ===").color(NamedTextColor.GOLD));
        source.sendMessage(Component.empty());
        
        source.sendMessage(Component.text("/mute <玩家> [时长] [原因]").color(NamedTextColor.AQUA));
        source.sendMessage(Component.text("  禁言指定玩家").color(NamedTextColor.GRAY));
        source.sendMessage(Component.empty());
        
        source.sendMessage(Component.text("参数说明:").color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("  <玩家>").color(NamedTextColor.WHITE)
                .append(Component.text(" - 必填，要禁言的玩家名").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  [时长]").color(NamedTextColor.WHITE)
                .append(Component.text(" - 可选，禁言时长，不填则永久").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  [原因]").color(NamedTextColor.WHITE)
                .append(Component.text(" - 可选，禁言原因").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.empty());
        
        source.sendMessage(Component.text("时长格式:").color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("  1d").color(NamedTextColor.WHITE)
                .append(Component.text(" - 1天").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  2h").color(NamedTextColor.WHITE)
                .append(Component.text(" - 2小时").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  30m").color(NamedTextColor.WHITE)
                .append(Component.text(" - 30分钟").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  60s 或 60").color(NamedTextColor.WHITE)
                .append(Component.text(" - 60秒").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  1d2h30m").color(NamedTextColor.WHITE)
                .append(Component.text(" - 可组合使用").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.empty());
        
        source.sendMessage(Component.text("示例:").color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("  /mute Steve").color(NamedTextColor.GREEN)
                .append(Component.text(" - 永久禁言").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  /mute Steve 1h").color(NamedTextColor.GREEN)
                .append(Component.text(" - 禁言1小时").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  /mute Steve 30m 刷屏").color(NamedTextColor.GREEN)
                .append(Component.text(" - 禁言30分钟，原因：刷屏").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.empty());
        
        source.sendMessage(Component.text("相关命令:").color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("  /unmute <玩家>").color(NamedTextColor.AQUA)
                .append(Component.text(" - 解除禁言").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  /muteinfo <玩家>").color(NamedTextColor.AQUA)
                .append(Component.text(" - 查看禁言信息").color(NamedTextColor.GRAY)));
    }

    private void handleMute(CommandSource source, String[] args) {
        if (args.length < 1) {
            showMuteHelp(source);
            return;
        }

        String targetName = args[0];

        // 验证玩家名格式
        if (!targetName.matches("^[a-zA-Z0-9_]{1,16}$")) {
            source.sendMessage(Component.text("✗ 无效的玩家名格式").color(NamedTextColor.RED));
            source.sendMessage(Component.text("  玩家名只能包含字母、数字和下划线，长度1-16位").color(NamedTextColor.GRAY));
            return;
        }

        long duration = 0;  // 默认永久
        String reason = "";

        // 解析时长和原因
        if (args.length >= 2) {
            duration = MuteManage.parseTime(args[1]);
            if (duration == -1) {
                // 可能是原因而不是时长
                duration = 0;
                reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            } else if (args.length >= 3) {
                // 有时长和原因
                reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            }
        }

        // 获取操作者名称
        String operatorName = source instanceof Player ? ((Player) source).getUsername() : "Console";

        // 执行禁言
        boolean success = MuteManage.getInstance().mutePlayer(targetName, duration, operatorName, reason);

        if (success) {
            String durationStr = duration > 0 ? MuteManage.formatTime(duration) : "永久";
            source.sendMessage(Component.text("✓ 已禁言 " + targetName + " (" + durationStr + ")").color(NamedTextColor.GREEN));
            if (!reason.isEmpty()) {
                source.sendMessage(Component.text("  原因: " + reason).color(NamedTextColor.GRAY));
            }
        } else {
            source.sendMessage(Component.text("✗ 禁言失败").color(NamedTextColor.RED));
        }
    }

    private void handleUnmute(CommandSource source, String[] args) {
        if (args.length < 1) {
            source.sendMessage(Component.text("✗ 缺少玩家名参数").color(NamedTextColor.RED));
            source.sendMessage(Component.text("用法: /unmute <玩家>").color(NamedTextColor.YELLOW));
            source.sendMessage(Component.text("示例: /unmute Steve").color(NamedTextColor.GRAY));
            return;
        }

        String targetName = args[0];

        // 验证玩家名格式
        if (!targetName.matches("^[a-zA-Z0-9_]{1,16}$")) {
            source.sendMessage(Component.text("✗ 无效的玩家名格式").color(NamedTextColor.RED));
            source.sendMessage(Component.text("  玩家名只能包含字母、数字和下划线，长度1-16位").color(NamedTextColor.GRAY));
            return;
        }

        String operatorName = source instanceof Player ? ((Player) source).getUsername() : "Console";

        boolean success = MuteManage.getInstance().unmutePlayer(targetName, operatorName);

        if (success) {
            source.sendMessage(Component.text("✓ 已解除 " + targetName + " 的禁言").color(NamedTextColor.GREEN));
        } else {
            source.sendMessage(Component.text("✗ 该玩家未被禁言").color(NamedTextColor.YELLOW));
        }
    }

    private void handleMuteInfo(CommandSource source, String[] args) {
        if (args.length < 1) {
            source.sendMessage(Component.text("✗ 缺少玩家名参数").color(NamedTextColor.RED));
            source.sendMessage(Component.text("用法: /muteinfo <玩家>").color(NamedTextColor.YELLOW));
            source.sendMessage(Component.text("示例: /muteinfo Steve").color(NamedTextColor.GRAY));
            return;
        }

        String targetName = args[0];

        // 验证玩家名格式
        if (!targetName.matches("^[a-zA-Z0-9_]{1,16}$")) {
            source.sendMessage(Component.text("✗ 无效的玩家名格式").color(NamedTextColor.RED));
            source.sendMessage(Component.text("  玩家名只能包含字母、数字和下划线，长度1-16位").color(NamedTextColor.GRAY));
            return;
        }

        String info = MuteManage.getInstance().getMuteInfo(targetName);

        if (info != null) {
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(info));
        } else {
            source.sendMessage(Component.text("✓ " + targetName + " 未被禁言").color(NamedTextColor.GREEN));
        }
    }
}
