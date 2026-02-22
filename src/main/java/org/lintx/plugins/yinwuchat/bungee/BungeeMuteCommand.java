package org.lintx.plugins.yinwuchat.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.Util.MessageUtil;
import org.lintx.plugins.yinwuchat.bungee.config.Config;
import org.lintx.plugins.yinwuchat.bungee.config.PlayerConfig;
import org.lintx.plugins.yinwuchat.bungee.manage.MuteManage;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BungeeCord 独立禁言命令 (/mute, /unmute, /muteinfo)
 */
public class BungeeMuteCommand extends Command {
    private final YinwuChat plugin;
    private final String commandType;

    public BungeeMuteCommand(YinwuChat plugin, String commandType) {
        super(commandType);
        this.plugin = plugin;
        this.commandType = commandType;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        boolean hasPermission;
        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            hasPermission = player.hasPermission(Const.PERMISSION_MUTE) || Config.getInstance().isAdmin(player);
        } else {
            hasPermission = true;
        }

        if (!hasPermission) {
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "权限不足"));
            return;
        }

        switch (commandType) {
            case "mute":
                handleMute(sender, args);
                break;
            case "unmute":
                handleUnmute(sender, args);
                break;
            case "muteinfo":
                handleMuteInfo(sender, args);
                break;
        }
    }

    private void handleMute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GOLD + "=== 禁言命令帮助 ==="));
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.AQUA + "/mute <玩家> [时长] [原因]"));
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.YELLOW + "时长格式: 1d(天) 2h(时) 30m(分) 60s(秒) 可组合"));
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GRAY + "不填时长则永久禁言"));
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.AQUA + "/unmute <玩家>" + ChatColor.GRAY + " - 解除禁言"));
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.AQUA + "/muteinfo <玩家>" + ChatColor.GRAY + " - 查看禁言信息"));
            return;
        }

        String targetName = args[0];
        long duration = 0;
        String reason = "";

        if (args.length >= 2) {
            duration = parseTime(args[1]);
            if (duration == -1) {
                duration = 0;
                reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            } else if (args.length >= 3) {
                reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            }
        }

        String operatorName = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getName() : "Console";
        boolean success = MuteManage.getInstance().mutePlayer(targetName, duration, operatorName, reason);

        if (success) {
            String durationStr = duration > 0 ? MuteManage.formatTime(duration) : "永久";
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "✓ 已禁言 " + targetName + " (" + durationStr + ")"));
            if (!reason.isEmpty()) {
                sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GRAY + "  原因: " + reason));
            }
        } else {
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "✗ 禁言失败，未找到该玩家"));
        }
    }

    private void handleUnmute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "用法: /unmute <玩家>"));
            return;
        }

        String targetName = args[0];
        String operatorName = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getName() : "Console";
        boolean success = MuteManage.getInstance().unmutePlayer(targetName, operatorName);

        if (success) {
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "✓ 已解除 " + targetName + " 的禁言"));
        } else {
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.YELLOW + "✗ 该玩家未被禁言或不存在"));
        }
    }

    private void handleMuteInfo(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "用法: /muteinfo <玩家>"));
            return;
        }

        String targetName = args[0];
        ProxiedPlayer target = plugin.getProxy().getPlayer(targetName);
        PlayerConfig.Player settings = null;
        if (target != null) {
            settings = PlayerConfig.getConfig(target);
        } else {
            settings = PlayerConfig.getPlayerConfigByName(targetName);
        }

        if (settings == null) {
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "✗ 未找到该玩家"));
            return;
        }

        if (!settings.isMuted()) {
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "✓ " + targetName + " 未被禁言"));
            return;
        }

        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GOLD + "=== " + targetName + " 的禁言信息 ==="));
        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.YELLOW + "状态: " + ChatColor.RED + "已禁言"));

        if (settings.mutedBy != null && !settings.mutedBy.isEmpty()) {
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.YELLOW + "操作人: " + ChatColor.WHITE + settings.mutedBy));
        }
        if (settings.muteReason != null && !settings.muteReason.isEmpty()) {
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.YELLOW + "原因: " + ChatColor.WHITE + settings.muteReason));
        }

        long remaining = settings.getRemainingMuteTime();
        if (remaining == -1) {
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.YELLOW + "时长: " + ChatColor.RED + "永久"));
        } else if (remaining > 0) {
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.YELLOW + "剩余: " + ChatColor.WHITE + MuteManage.formatTime(remaining)));
        }
    }

    private static final Pattern TIME_PATTERN = Pattern.compile("(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s?)?", Pattern.CASE_INSENSITIVE);

    /**
     * 解析时长字符串，返回秒数。返回 -1 表示无法解析。
     */
    public static long parseTime(String input) {
        if (input == null || input.isEmpty()) return -1;

        // 纯数字视为秒
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException ignored) {}

        Matcher matcher = TIME_PATTERN.matcher(input);
        if (!matcher.matches()) return -1;

        long total = 0;
        boolean matched = false;
        if (matcher.group(1) != null) { total += Long.parseLong(matcher.group(1)) * 86400; matched = true; }
        if (matcher.group(2) != null) { total += Long.parseLong(matcher.group(2)) * 3600; matched = true; }
        if (matcher.group(3) != null) { total += Long.parseLong(matcher.group(3)) * 60; matched = true; }
        if (matcher.group(4) != null) { total += Long.parseLong(matcher.group(4)); matched = true; }

        return matched ? total : -1;
    }
}
