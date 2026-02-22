package org.lintx.plugins.yinwuchat.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import io.netty.channel.ChannelFutureListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.velocity.YinwuChat;
import org.lintx.plugins.yinwuchat.velocity.config.Config;
import org.lintx.plugins.yinwuchat.velocity.config.PlayerConfig;
import org.lintx.plugins.yinwuchat.velocity.manage.MuteManage;
import org.lintx.plugins.yinwuchat.common.auth.AuthService;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Velocity 代理命令处理器
 * 支持: reload, bind, unbind, list, ignore, noat, muteat, monitor, vanish, mute, unmute, muteinfo
 */
public class YinwuChatCommand implements SimpleCommand {
    private final YinwuChat plugin;
    private final Config config;

    public YinwuChatCommand(YinwuChat plugin) {
        this.plugin = plugin;
        this.config = Config.getInstance();
    }

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        String label = invocation.alias().toLowerCase();
        
        if (label.equals("chatban")) {
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = "ban";
            System.arraycopy(args, 0, newArgs, 1, args.length);
            args = newArgs;
        }
        
        if (label.equals("chatunban")) {
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = "unban";
            System.arraycopy(args, 0, newArgs, 1, args.length);
            args = newArgs;
        }
        
        // 处理禁言相关命令（可在控制台执行）
        if (args.length >= 1) {
            String cmd = args[0].toLowerCase();
            if (cmd.equals("mute")) {
                handleMute(source, args);
                return;
            }
            if (cmd.equals("unmute")) {
                handleUnmute(source, args);
                return;
            }
            if (cmd.equals("muteinfo")) {
                handleMuteInfo(source, args);
                return;
            }
            if (cmd.equals("reload")) {
                handleReload(source, args);
                return;
            }
        }

        // 非玩家命令
        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("Must use command in-game").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;
        boolean isAdmin = config.isAdmin(player);
        boolean isDefault = config.isDefault(player);
        
        if (args.length == 0) {
            showHelp(player);
            return;
        }

        String command = args[0].toLowerCase();
        switch (command) {
            case "permsync":
                if (player.hasPermission(Const.PERMISSION_ADMIN) || isAdmin) {
                    handlePermissionSync(player);
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            case "ws":
                if (player.hasPermission(Const.PERMISSION_WS) || isDefault) {
                    handleWsAddress(player);
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            case "bind":
                if (player.hasPermission(Const.PERMISSION_BIND) || isDefault) {
                    if (args.length >= 2) {
                        String bindToken = args[1].trim().replaceAll("[^a-zA-Z0-9-]", "");
                        PlayerConfig.TokenManager tokens = PlayerConfig.getInstance().getTokenManager();
                        if (tokens.tokenNotValid(bindToken)) {
                            player.sendMessage(Component.text("✗ Token 无效").color(NamedTextColor.RED));
                            return;
                        }
                        if (tokens.tokenIsBind(bindToken)) {
                            player.sendMessage(Component.text("✗ 该 Token 已被绑定").color(NamedTextColor.RED));
                            return;
                        }
                        tokens.bindToken(bindToken, player.getUniqueId(), player.getUsername());
                        player.sendMessage(Component.text("✓ Token 绑定成功！").color(NamedTextColor.GREEN));
                        
                        // 通知 WebSocket 客户端
                        io.netty.channel.Channel channel = org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.getWebSocket(bindToken);
                        if (channel != null) {
                            org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientUtil util = 
                                org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.get(channel);
                            if (util != null) {
                                util.setUUID(player.getUniqueId());
                            }
                            org.lintx.plugins.yinwuchat.velocity.json.InputCheckToken response = 
                                new org.lintx.plugins.yinwuchat.velocity.json.InputCheckToken(bindToken, false);
                            org.lintx.plugins.yinwuchat.velocity.httpserver.NettyChannelMessageHelper.send(channel, response.getJSON());
                            // 发送绑定成功的系统消息到 Web 客户端
                            org.lintx.plugins.yinwuchat.velocity.httpserver.NettyChannelMessageHelper.send(channel, 
                                org.lintx.plugins.yinwuchat.velocity.json.OutputServerMessage.infoJSON("✓ 已绑定 Web 账户与玩家名").getJSON());
                        }
                    } else {
                        player.sendMessage(Component.text("用法: /yinwuchat bind <Token>").color(NamedTextColor.RED));
                    }
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            case "list":
                if (player.hasPermission(Const.PERMISSION_LIST) || isDefault) {
                    String userToken = PlayerConfig.getInstance().getTokenManager().getToken(player.getUniqueId());
                    if (userToken == null) {
                        player.sendMessage(Component.text("你当前没有绑定的 Token").color(NamedTextColor.YELLOW));
                    } else {
                        player.sendMessage(Component.text("你绑定的 Token: ").color(NamedTextColor.GOLD)
                            .append(Component.text(userToken).color(NamedTextColor.AQUA)));
                    }
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            case "unbind":
                if (player.hasPermission(Const.PERMISSION_UNBIND) || isDefault) {
                    String currentToken = PlayerConfig.getInstance().getTokenManager().getToken(player.getUniqueId());
                    if (currentToken == null) {
                        player.sendMessage(Component.text("✗ 你当前没有绑定的 Token").color(NamedTextColor.RED));
                    } else {
                        PlayerConfig.getInstance().getTokenManager().unbindToken(currentToken);
                        player.sendMessage(Component.text("✓ Token 已解绑").color(NamedTextColor.GREEN));
                    }
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            case "ignore":
                if (player.hasPermission(Const.PERMISSION_IGNORE) || isDefault) {
                    if (args.length >= 2) {
                        handleIgnore(player, args[1]);
                    } else {
                        player.sendMessage(Component.text("用法: /yinwuchat ignore <玩家名>").color(NamedTextColor.RED));
                    }
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            case "noat":
                if (player.hasPermission(Const.PERMISSION_NOAT) || isDefault) {
                    handleNoAt(player);
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            case "muteat":
                if (player.hasPermission(Const.PERMISSION_MUTEAT) || isDefault) {
                    handleMuteAt(player);
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            case "monitor":
                if (player.hasPermission(Const.PERMISSION_MONITOR_PRIVATE_MESSAGE) || isAdmin) {
                    player.sendMessage(Component.text("监听私聊功能施工中").color(NamedTextColor.YELLOW));
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            case "vanish":
                if (player.hasPermission(Const.PERMISSION_VANISH) || isAdmin) {
                    handleVanish(player);
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            case "chatban":
            case "ban":
                if (player.hasPermission(Const.PERMISSION_ADMIN) || isAdmin) {
                    handleBan(player, args);
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            case "chatunban":
            case "unban":
                if (player.hasPermission(Const.PERMISSION_ADMIN) || isAdmin) {
                    handleUnban(player, args);
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            case "webbind":
                if (player.hasPermission(Const.PERMISSION_ADMIN) || isAdmin) {
                    handleWebBind(player, args);
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            case "format":
                if (player.hasPermission(Const.PERMISSION_FORMAT) || isDefault) {
                    handleFormat(player, args);
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            case "atalladmin":
                if (player.hasPermission(Const.PERMISSION_AT_ALL_ADMIN) || isDefault) {
                    handleAtAllAdmin(player, args);
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            case "atalladmin_execute":
                if (player.hasPermission(Const.PERMISSION_AT_ALL_ADMIN) || isDefault) {
                    handleAtAllAdminExecute(player);
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            case "atalladmin_cancel":
                player.sendMessage(Component.text("已取消突发事件报告").color(NamedTextColor.GRAY));
                break;
            case "badword":
                if (player.hasPermission(Const.PERMISSION_BAD_WORD) || isAdmin) {
                    handleBadWord(player, args);
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            case "reset":
                handleResetCode(player, args);
                break;
            case "mute":
                handleMute(player, args);
                break;
            case "unmute":
                handleUnmute(player, args);
                break;
            case "muteinfo":
                handleMuteInfo(player, args);
                break;
            case "itemdisplay":
                if (player.hasPermission(Const.PERMISSION_ITEM_DISPLAY) || isDefault) {
                    plugin.getProxy().getCommandManager().executeAsync(player, "itemdisplay " + String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            case "qq":
                if (player.hasPermission(Const.PERMISSION_QQ) || isDefault) {
                    plugin.getProxy().getCommandManager().executeAsync(player, "qq " + String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            case "msg":
                if (player.hasPermission(Const.PERMISSION_MSG) || isDefault) {
                    plugin.getProxy().getCommandManager().executeAsync(player, "msg " + String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                } else {
                    player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                }
                break;
            default:
                showHelp(player);
        }
    }

    private void handleBan(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("用法: /chatban <账号名/玩家名> [时长] [理由]").color(NamedTextColor.RED));
            player.sendMessage(Component.text("时长支持: 10s/10秒、30m/30分钟、2h/2小时、1d/1天，纯数字默认秒，缺省为永久").color(NamedTextColor.GRAY));
            return;
        }
        String target = args[1];
        String durationArg = args.length >= 3 ? args[2] : "";
        long durationMillis = AuthService.parseDurationMillis(durationArg);
        int reasonStart = 3;
        if (durationMillis == 0L && args.length >= 3) {
            reasonStart = 2;
        }
        String reason = "";
        if (args.length > reasonStart) {
            reason = String.join(" ", java.util.Arrays.copyOfRange(args, reasonStart, args.length));
        }
        AuthService authService = AuthService.getInstance(plugin.getDataFolder().toFile());
        String accountName = target;
        String playerName = "";
        if (!authService.accountExists(target)) {
            String mapped = authService.resolveAccountByPlayerName(target);
            if (mapped == null || mapped.isEmpty()) {
                player.sendMessage(Component.text("✗ 未找到对应的 Web 账户").color(NamedTextColor.RED));
                return;
            }
            accountName = mapped;
            playerName = target;
        } else {
            playerName = authService.getBoundPlayerName(accountName);
        }
        AuthService.BanResult result = authService.banUser(accountName, durationMillis, reason, player.getUsername());
        if (result.notFound) {
            player.sendMessage(Component.text("✗ 未找到该 Web 账户").color(NamedTextColor.RED));
            return;
        }
        if (!result.ok) {
            player.sendMessage(Component.text("✗ 封禁失败，请稍后重试").color(NamedTextColor.RED));
            return;
        }
        if (playerName != null && !playerName.isEmpty()) {
            long muteSeconds = durationMillis <= 0L ? 0L : Math.max(1L, durationMillis / 1000L);
            MuteManage.getInstance().mutePlayer(playerName, muteSeconds, player.getUsername(), reason);
        }
        String durationText = AuthService.formatDuration(durationMillis <= 0L ? -1L : durationMillis);
        String tip = "已封禁账号 " + accountName + "，时长: " + (durationText.isEmpty() ? "永久" : durationText)
            + (reason == null || reason.isEmpty() ? "" : "，理由: " + reason);
        if (playerName != null && !playerName.isEmpty()) {
            tip += "（玩家: " + playerName + "）";
        }
        player.sendMessage(Component.text(tip).color(NamedTextColor.GREEN));
        notifyBanToAdmins(accountName, playerName, durationText, reason, player.getUsername());
        kickWebPlayerByName(playerName, accountName, durationText, reason);
    }

    private void handleUnban(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("用法: /chatunban <账号名/玩家名>").color(NamedTextColor.RED));
            return;
        }
        String target = args[1];
        AuthService authService = AuthService.getInstance(plugin.getDataFolder().toFile());
        String accountName = target;
        String playerName = "";
        if (!authService.accountExists(target)) {
            String mapped = authService.resolveAccountByPlayerName(target);
            if (mapped == null || mapped.isEmpty()) {
                player.sendMessage(Component.text("✗ 未找到对应的 Web 账户").color(NamedTextColor.RED));
                return;
            }
            accountName = mapped;
            playerName = target;
        } else {
            playerName = authService.getBoundPlayerName(accountName);
        }
        AuthService.BanResult result = authService.unbanUser(accountName);
        if (result.notFound) {
            player.sendMessage(Component.text("✗ 未找到该 Web 账户").color(NamedTextColor.RED));
            return;
        }
        if (!result.ok) {
            player.sendMessage(Component.text("✗ 解封失败，请稍后重试").color(NamedTextColor.RED));
            return;
        }
        // 同时解除游戏内禁言
        if (playerName != null && !playerName.isEmpty()) {
            MuteManage.getInstance().unmutePlayer(playerName, player.getUsername());
        }
        String tip = "✓ 已解封账号 " + accountName;
        if (playerName != null && !playerName.isEmpty()) {
            tip += "（玩家: " + playerName + "）";
        }
        player.sendMessage(Component.text(tip).color(NamedTextColor.GREEN));
        notifyUnbanToAdmins(accountName, playerName, player.getUsername());
    }

    private void notifyUnbanToAdmins(String accountName, String targetPlayerName, String operator) {
        String message = "账号 " + accountName + " 已被解封"
            + (targetPlayerName != null && !targetPlayerName.isEmpty() ? "（玩家: " + targetPlayerName + "）" : "")
            + "，操作人: " + operator;

        // 游戏内管理员
        for (Player p : plugin.getProxy().getAllPlayers()) {
            if (config.isAdmin(p)) {
                p.sendMessage(Component.text(message).color(NamedTextColor.GREEN));
            }
        }

        // Web 端管理员
        if (plugin.getConfig().openwsserver && YinwuChat.getWSServer() != null) {
            String json = org.lintx.plugins.yinwuchat.velocity.json.OutputServerMessage.infoJSON(message).getJSON();
            for (io.netty.channel.Channel channel : org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.getChannels()) {
                org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientUtil util =
                    org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.get(channel);
                if (util == null || util.getUuid() == null) continue;
                String name = util.getAccount();
                if (name == null || name.isEmpty()) {
                    name = org.lintx.plugins.yinwuchat.velocity.config.PlayerConfig.getInstance().getTokenManager().getName(util.getUuid());
                }
                if (name != null && config.isAdmin(name)) {
                    org.lintx.plugins.yinwuchat.velocity.httpserver.NettyChannelMessageHelper.send(channel, json);
                }
            }
        }
    }

    private void notifyBanToAdmins(String accountName, String targetPlayerName, String durationText, String reason, String operator) {
        String message = "账号 " + accountName + " 已被封禁"
            + (targetPlayerName != null && !targetPlayerName.isEmpty() ? "（玩家: " + targetPlayerName + "）" : "")
            + "，时长: " + (durationText == null || durationText.isEmpty() ? "永久" : durationText)
            + (reason == null || reason.isEmpty() ? "" : "，理由: " + reason);

        // 游戏内管理员
        for (Player p : plugin.getProxy().getAllPlayers()) {
            if (config.isAdmin(p)) {
                p.sendMessage(Component.text(message).color(NamedTextColor.YELLOW));
            }
        }

        // Web 端管理员
        if (plugin.getConfig().openwsserver && YinwuChat.getWSServer() != null) {
            String json = org.lintx.plugins.yinwuchat.velocity.json.OutputServerMessage.infoJSON(message).getJSON();
            for (io.netty.channel.Channel channel : org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.getChannels()) {
                org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientUtil util =
                    org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.get(channel);
                if (util == null || util.getUuid() == null) continue;
                String name = util.getAccount();
                if (name == null || name.isEmpty()) {
                    name = PlayerConfig.getInstance().getTokenManager().getName(util.getUuid());
                }
                if (name != null && config.isAdmin(name)) {
                    org.lintx.plugins.yinwuchat.velocity.httpserver.NettyChannelMessageHelper.send(channel, json);
                }
            }
        }
    }

    private void kickWebPlayerByName(String playerName, String accountName, String durationText, String reason) {
        if (!plugin.getConfig().openwsserver || YinwuChat.getWSServer() == null) return;
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("action", "ban_kick");
        json.addProperty("player", playerName == null ? "" : playerName);
        json.addProperty("account", accountName == null ? "" : accountName);
        json.addProperty("durationText", durationText == null || durationText.isEmpty() ? "永久" : durationText);
        json.addProperty("reason", reason == null ? "" : reason);
        String payload = json.toString();

        for (io.netty.channel.Channel channel : org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.getChannels()) {
            org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientUtil util =
                org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.get(channel);
            if (util == null) continue;
            boolean matched = false;
            if (accountName != null && !accountName.isEmpty()) {
                String utilAccount = util.getAccount();
                if (utilAccount != null && utilAccount.equalsIgnoreCase(accountName)) {
                    matched = true;
                }
            }
            if (!matched && util.getUuid() != null && playerName != null && !playerName.isEmpty()) {
                String name = PlayerConfig.getInstance().getTokenManager().getName(util.getUuid());
                if (name != null && name.equalsIgnoreCase(playerName)) {
                    matched = true;
                }
            }
            if (matched) {
                channel.writeAndFlush(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame(payload)).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    private void handleWebBind(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("用法: /yinwuchat webbind <query|unbind> <账号名/玩家名>").color(NamedTextColor.RED));
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        String target = args[2];
        AuthService authService = AuthService.getInstance(plugin.getDataFolder().toFile());
        if ("query".equals(action)) {
            if (authService.accountExists(target)) {
                String bound = authService.getBoundPlayerName(target);
                String msg = bound == null || bound.isEmpty()
                    ? "账号 " + target + " 未绑定玩家名"
                    : "账号 " + target + " 绑定玩家名: " + bound;
                player.sendMessage(Component.text(msg).color(NamedTextColor.GREEN));
                return;
            }
            String account = authService.resolveAccountByPlayerName(target);
            if (account == null || account.isEmpty()) {
                player.sendMessage(Component.text("未找到对应的绑定信息").color(NamedTextColor.RED));
                return;
            }
            player.sendMessage(Component.text("玩家 " + target + " 绑定账号: " + account).color(NamedTextColor.GREEN));
            return;
        }
        if ("unbind".equals(action)) {
            if (authService.accountExists(target)) {
                boolean ok = authService.unbindAccountPlayerName(target);
                player.sendMessage(Component.text(ok ? "已解绑账号 " + target + " 的玩家名" : "解绑失败")
                    .color(ok ? NamedTextColor.GREEN : NamedTextColor.RED));
                return;
            }
            String account = authService.unbindAccountByPlayerName(target);
            if (account == null || account.isEmpty()) {
                player.sendMessage(Component.text("未找到对应的绑定信息").color(NamedTextColor.RED));
                return;
            }
            player.sendMessage(Component.text("已解绑玩家 " + target + " 的账号 " + account).color(NamedTextColor.GREEN));
            return;
        }
        player.sendMessage(Component.text("用法: /yinwuchat webbind <query|unbind> <账号名/玩家名>").color(NamedTextColor.RED));
    }

    private void handleAtAllAdmin(Player player, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
            // 管理员重置玩家冷却时间
            if (!config.isAdmin(player) && !player.hasPermission(Const.PERMISSION_AT_ALL_ADMIN_RESET)) {
                player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                return;
            }
            if (args.length < 3) {
                player.sendMessage(Component.text("用法: /yinwuchat atalladmin confirm <玩家名>").color(NamedTextColor.RED));
                return;
            }
            String targetName = args[2];
            PlayerConfig.PlayerSettings settings = PlayerConfig.getInstance().getSettings(targetName);
            if (settings != null) {
                settings.lastAtAllAdmin = 0;
                PlayerConfig.getInstance().saveSettings(settings);
                player.sendMessage(Component.text("✓ 已重置玩家 " + targetName + " 的突发事件提醒冷却时间").color(NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("✗ 未找到玩家 " + targetName).color(NamedTextColor.RED));
            }
            return;
        }

        // 普通玩家使用提醒功能
        if (!player.hasPermission(Const.PERMISSION_AT_ALL_ADMIN) && !config.isAdmin(player)) {
            player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
            return;
        }

        PlayerConfig.PlayerSettings settings = PlayerConfig.getInstance().getSettings(player);
        long now = System.currentTimeMillis();
        if (settings.lastAtAllAdmin > 0 && now - settings.lastAtAllAdmin < 24 * 60 * 60 * 1000L) {
            long remaining = 24 * 60 * 60 * 1000L - (now - settings.lastAtAllAdmin);
            long hours = remaining / (60 * 60 * 1000L);
            long minutes = (remaining % (60 * 60 * 1000L)) / (60 * 1000L);
            player.sendMessage(Component.text("✗ 你今天已经使用过该功能了，请在 " + hours + " 小时 " + minutes + " 分钟后再试").color(NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("⚠ 请确认目前情况是否需要提醒所有管理员，若情况不属实，将承担被封禁的风险！！").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("  点击确认发送：")
            .append(Component.text("[ √ ]")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/yinwuchat atalladmin_execute"))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("确认发送突发事件提醒").color(NamedTextColor.GRAY))))
            .append(Component.text("    "))
            .append(Component.text("[ x ]")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/yinwuchat atalladmin_cancel"))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("取消发送").color(NamedTextColor.GRAY)))));
    }

    private void handleAtAllAdminExecute(Player player) {
        if (!player.hasPermission(Const.PERMISSION_AT_ALL_ADMIN) && !config.isAdmin(player)) {
            player.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
            return;
        }

        PlayerConfig.PlayerSettings settings = PlayerConfig.getInstance().getSettings(player);
        long now = System.currentTimeMillis();
        if (settings.lastAtAllAdmin > 0 && now - settings.lastAtAllAdmin < 24 * 60 * 60 * 1000L) {
            player.sendMessage(Component.text("✗ 冷却中").color(NamedTextColor.RED));
            return;
        }

        settings.lastAtAllAdmin = now;
        PlayerConfig.getInstance().saveSettings(settings);

        String playerName = player.getUsername();
        Component gameAlert = Component.text("警告！").color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
            .append(Component.text(playerName).color(NamedTextColor.YELLOW))
            .append(Component.text(" 报告服务器存在突发事件，请立即查看").color(NamedTextColor.RED).decoration(TextDecoration.BOLD, false));

        com.google.gson.JsonObject webAlert = new com.google.gson.JsonObject();
        webAlert.addProperty("action", "admin_alert");
        webAlert.addProperty("message", "存在需要立即处理的服务器风险");
        webAlert.addProperty("player", playerName);
        String webAlertJson = webAlert.toString();

        // 提醒所有在线管理员
        for (Player p : plugin.getProxy().getAllPlayers()) {
            if (config.isAdmin(p)) {
                p.sendMessage(gameAlert);
            }
        }

        // 提醒所有 Web 在线管理员
        for (io.netty.channel.Channel channel : org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.getChannels()) {
            org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientUtil util = 
                org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.get(channel);
            if (util != null && util.getUuid() != null) {
                String webName = PlayerConfig.getInstance().getTokenManager().getName(util.getUuid());
                if (webName != null && config.isAdmin(webName)) {
                    org.lintx.plugins.yinwuchat.velocity.httpserver.NettyChannelMessageHelper.send(channel, webAlertJson);
                }
            }
        }

        player.sendMessage(Component.text("✓ 突发事件报告已发送给所有管理员").color(NamedTextColor.GREEN));
    }

    private void handleResetCode(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("用法: /yinwuchat reset <账户名> <重置码>").color(NamedTextColor.RED));
            return;
        }
        String accountName = args[1];
        String code = args[2];
        
        org.lintx.plugins.yinwuchat.common.auth.AuthService authService = 
            org.lintx.plugins.yinwuchat.common.auth.AuthService.getInstance(plugin.getDataFolder().toFile());
        
        boolean success = authService.verifyInGameReset(accountName, player.getUsername(), code);
        if (success) {
            player.sendMessage(Component.text("✓ 身份验证成功！请回到 Web 端设置新密码。").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("✗ 验证失败。请检查账户名和重置码是否正确，或重置申请已过期。").color(NamedTextColor.RED));
        }
    }

    @Override
    public boolean hasPermission(SimpleCommand.Invocation invocation) {
        // 始终返回 true，让命令可以执行
        // 具体的权限检查在各个处理方法中进行，以便返回友好的错误消息
        // 这样可以避免 Velocity 权限桥接问题导致的 "Incorrect argument" 错误
        return true;
    }

    @Override
    public List<String> suggest(SimpleCommand.Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        
        if (args.length == 0) return List.of();
        if (!(source instanceof Player)) return List.of();
        
        Player player = (Player) source;
        boolean isAdmin = config.isAdmin(player);
        boolean isDefault = config.isDefault(player);
        
        List<String> completions = new ArrayList<>();
        
        // 管理员指令
        if (player.hasPermission(Const.PERMISSION_RELOAD) || isAdmin) completions.add("reload");
        if (player.hasPermission(Const.PERMISSION_MONITOR_PRIVATE_MESSAGE) || isAdmin) completions.add("monitor");
        if (player.hasPermission(Const.PERMISSION_VANISH) || isAdmin) completions.add("vanish");
        if (player.hasPermission(Const.PERMISSION_MUTE) || isAdmin) {
            completions.add("mute");
            completions.add("unmute");
            completions.add("muteinfo");
        }
        if (player.hasPermission(Const.PERMISSION_ADMIN) || isAdmin) {
            completions.add("chatban");
        }
        
        // 基础指令
        if (player.hasPermission(Const.PERMISSION_WS) || isDefault) completions.add("ws");
        if (player.hasPermission(Const.PERMISSION_BIND) || isDefault) completions.add("bind");
        if (player.hasPermission(Const.PERMISSION_LIST) || isDefault) completions.add("list");
        if (player.hasPermission(Const.PERMISSION_UNBIND) || isDefault) completions.add("unbind");
        if (player.hasPermission(Const.PERMISSION_IGNORE) || isDefault) completions.add("ignore");
        if (player.hasPermission(Const.PERMISSION_NOAT) || isDefault) completions.add("noat");
        if (player.hasPermission(Const.PERMISSION_MUTEAT) || isDefault) completions.add("muteat");
        if (player.hasPermission(Const.PERMISSION_FORMAT) || isDefault) completions.add("format");
        if (player.hasPermission(Const.PERMISSION_AT_ALL_ADMIN) || isDefault) completions.add("atalladmin");
        if (player.hasPermission(Const.PERMISSION_ITEM_DISPLAY) || isDefault) completions.add("itemdisplay");
        if (player.hasPermission(Const.PERMISSION_QQ) || isDefault) completions.add("qq");
        if (player.hasPermission(Const.PERMISSION_MSG) || isDefault) completions.add("msg");
        
        String prefix = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.startsWith(prefix))
            .toList();
    }

    /**
     * 处理屏蔽词管理
     */
    private void handleBadWord(Player player, String[] args) {
        if (args.length >= 3) {
            if (args[1].equalsIgnoreCase("add")) {
                String word = args[2].toLowerCase(Locale.ROOT);
                config.shieldeds.add(word);
                config.save(plugin);
                player.sendMessage(Component.text("✓ 已将关键词 " + word + " 添加到屏蔽库").color(NamedTextColor.GREEN));
                return;
            } else if (args[1].equalsIgnoreCase("remove")) {
                String word = args[2].toLowerCase(Locale.ROOT);
                if (config.shieldeds.contains(word)) {
                    config.shieldeds.remove(word);
                    config.save(plugin);
                    player.sendMessage(Component.text("✓ 已将关键词 " + word + " 从屏蔽库删除").color(NamedTextColor.GREEN));
                    return;
                }
            }
        } else if (args.length == 2 && args[1].equalsIgnoreCase("list")) {
            player.sendMessage(Component.text("=== 屏蔽关键词列表 ===").color(NamedTextColor.GOLD));
            for (String str : config.shieldeds) {
                player.sendMessage(Component.text("- " + str).color(NamedTextColor.GREEN));
            }
            return;
        }
        player.sendMessage(Component.text("用法: /yinwuchat badword <add|remove|list> [word]").color(NamedTextColor.RED));
    }

    private void handleReload(CommandSource source, String[] args) {
        if (source instanceof Player) {
            Player player = (Player) source;
            boolean hasPermission = player.hasPermission(Const.PERMISSION_RELOAD) || config.isAdmin(player);
            if (!hasPermission) {
                source.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
                return;
            }
        }
        
        // 重新加载配置并重启相关服务
        try {
            plugin.reload();
            source.sendMessage(Component.text("✓ 配置已重新加载").color(NamedTextColor.GREEN));
        } catch (Exception e) {
            source.sendMessage(Component.text("✗ 配置加载失败: " + e.getMessage()).color(NamedTextColor.RED));
            plugin.getLogger().error("Failed to reload config", e);
        }
    }

    private void showHelp(Player player) {
        boolean isAdmin = config.isAdmin(player);
        boolean isDefault = config.isDefault(player);
        
        player.sendMessage(Component.text("=== YinwuChat 帮助 ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/yinwuchat").color(NamedTextColor.AQUA)
            .append(Component.text(": 显示帮助信息").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/yinwuchat ws").color(NamedTextColor.AQUA)
            .append(Component.text(": 获取当前 WebSocket 地址").color(NamedTextColor.GRAY)));
        
        if (player.hasPermission(Const.PERMISSION_RELOAD) || isAdmin) {
            player.sendMessage(Component.text("/yinwuchat reload [config|ws]").color(NamedTextColor.AQUA)
                .append(Component.text(": 重新加载配置").color(NamedTextColor.GRAY)));
        }
        
        player.sendMessage(Component.text("—— Web 端配置指令 ——").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/yinwuchat bind <Token>").color(NamedTextColor.AQUA)
            .append(Component.text(": 绑定 Web 账号").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/yinwuchat unbind").color(NamedTextColor.AQUA)
            .append(Component.text(": 解绑当前 Web 账号").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/yinwuchat list").color(NamedTextColor.AQUA)
            .append(Component.text(": 查看已绑定 Token").color(NamedTextColor.GRAY)));
        if (isAdmin) {
            player.sendMessage(Component.text("/yinwuchat webbind query <账号名/玩家名>").color(NamedTextColor.AQUA)
                .append(Component.text(": 查询 Web 绑定关系").color(NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/yinwuchat webbind unbind <账号名/玩家名>").color(NamedTextColor.AQUA)
                .append(Component.text(": 解除 Web 绑定关系").color(NamedTextColor.GRAY)));
        }
        
        player.sendMessage(Component.text("—— Velocity 指令 ——").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/yinwuchat msg <玩家名> <消息>").color(NamedTextColor.AQUA)
            .append(Component.text(": 发送私聊消息").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  别名: /msg /tell /whisper /w /t").color(NamedTextColor.GRAY));
        
        player.sendMessage(Component.text("/yinwuchat noat").color(NamedTextColor.AQUA)
            .append(Component.text(": 禁止/允许被@").color(NamedTextColor.GRAY)));
        
        player.sendMessage(Component.text("/yinwuchat muteat").color(NamedTextColor.AQUA)
            .append(Component.text(": 切换@时的声音").color(NamedTextColor.GRAY)));
        
        player.sendMessage(Component.text("/yinwuchat ignore <玩家名>").color(NamedTextColor.AQUA)
            .append(Component.text(": 忽略/取消忽略玩家").color(NamedTextColor.GRAY)));
        
        if (player.hasPermission(Const.PERMISSION_VANISH) || isAdmin) {
            player.sendMessage(Component.text("/yinwuchat vanish").color(NamedTextColor.AQUA)
                .append(Component.text(": 切换隐身模式").color(NamedTextColor.GRAY)));
        }
        
        player.sendMessage(Component.text("/yinwuchat atalladmin").color(NamedTextColor.AQUA)
            .append(Component.text(": 报告突发事件给所有管理员 (每日限一次)").color(NamedTextColor.GRAY)));
        
        if (isAdmin) {
            player.sendMessage(Component.text("/yinwuchat atalladmin confirm <玩家名>").color(NamedTextColor.AQUA)
                .append(Component.text(": 重置玩家报告冷却时间").color(NamedTextColor.GRAY)));
        }
        
        if (config.allowPlayerFormatPrefixSuffix) {
            player.sendMessage(Component.text("/yinwuchat format edit").color(NamedTextColor.AQUA)
                .append(Component.text(": 编辑聊天前后缀").color(NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/yinwuchat format show").color(NamedTextColor.AQUA)
                .append(Component.text(": 显示当前前后缀").color(NamedTextColor.GRAY)));
        }
        
        if (player.hasPermission(Const.PERMISSION_MUTE) || isAdmin) {
            player.sendMessage(Component.text("/yinwuchat mute <玩家> [时长] [原因]").color(NamedTextColor.AQUA)
                .append(Component.text(": 禁言玩家").color(NamedTextColor.GRAY)));
            player.sendMessage(Component.text("  别名: /mute").color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("/yinwuchat unmute <玩家>").color(NamedTextColor.AQUA)
                .append(Component.text(": 解除禁言").color(NamedTextColor.GRAY)));
            player.sendMessage(Component.text("  别名: /unmute").color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("/yinwuchat muteinfo <玩家>").color(NamedTextColor.AQUA)
                .append(Component.text(": 查看禁言信息").color(NamedTextColor.GRAY)));
            player.sendMessage(Component.text("  别名: /muteinfo").color(NamedTextColor.GRAY));
        }
        
        if (isAdmin) {
            player.sendMessage(Component.text("/yinwuchat permsync").color(NamedTextColor.AQUA)
                .append(Component.text(": 同步 LuckPerms 组权限节点").color(NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/yinwuchat chatban <玩家> [时长] [原因]").color(NamedTextColor.AQUA)
                .append(Component.text(": 封禁 Web 账号").color(NamedTextColor.GRAY)));
            player.sendMessage(Component.text("  别名: /chatban").color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("/yinwuchat chatunban <玩家>").color(NamedTextColor.AQUA)
                .append(Component.text(": 解封 Web 账号").color(NamedTextColor.GRAY)));
            player.sendMessage(Component.text("  别名: /chatunban").color(NamedTextColor.GRAY));
        }

        if (player.hasPermission(Const.PERMISSION_ITEM_DISPLAY) || isDefault) {
            player.sendMessage(Component.text("/yinwuchat itemdisplay").color(NamedTextColor.AQUA)
                .append(Component.text(": 显示物品展示帮助").color(NamedTextColor.GRAY)));
            player.sendMessage(Component.text("  别名: /itemdisplay /showitem /displayitem").color(NamedTextColor.GRAY));
        }
        
        player.sendMessage(Component.text("—— Bukkit 指令（子服） ——").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/msg <玩家名> <消息>").color(NamedTextColor.AQUA)
            .append(Component.text(": 子服私聊指令").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/viewitem <物品ID>").color(NamedTextColor.AQUA)
            .append(Component.text(": 查看物品展示详情").color(NamedTextColor.GRAY)));
    }

    private void handlePermissionSync(Player player) {
        boolean detected = false;
        if (config.permissionPluginIds != null) {
            for (String pluginId : config.permissionPluginIds) {
                if (pluginId != null && !pluginId.isEmpty()
                    && plugin.getProxy().getPluginManager().getPlugin(pluginId).isPresent()) {
                    detected = true;
                    break;
                }
            }
        }
        if (!detected) {
            player.sendMessage(Component.text("⚠ 未检测到权限插件，将尝试直接执行同步命令").color(NamedTextColor.YELLOW));
        }
        
        String adminGroup = config.adminGroup != null && !config.adminGroup.isEmpty()
            ? config.adminGroup
            : "admin";
        String defaultGroup = config.defaultGroup != null && !config.defaultGroup.isEmpty()
            ? config.defaultGroup
            : "default";
        
        String commandPrefix = config.permissionCommandPrefix != null && !config.permissionCommandPrefix.isEmpty()
            ? config.permissionCommandPrefix
            : "lp";
        
        List<String> commands = new ArrayList<>();
        for (String node : Const.DEFAULT_PERMISSION_NODES) {
            commands.add(commandPrefix + " group " + defaultGroup + " permission set " + node + " true");
            commands.add(commandPrefix + " group " + adminGroup + " permission set " + node + " true");
        }
        for (String node : Const.ADMIN_PERMISSION_NODES) {
            commands.add(commandPrefix + " group " + adminGroup + " permission set " + node + " true");
        }
        
        dispatchPermissionCommands(commands);
        
        player.sendMessage(Component.text("✓ 已同步权限节点到组: " + adminGroup + ", " + defaultGroup)
            .color(NamedTextColor.GREEN));
    }

    private void dispatchPermissionCommands(List<String> commands) {
        if (commands == null || commands.isEmpty()) return;
        for (int i = 0; i < commands.size(); i++) {
            String command = commands.get(i);
            long delayMs = i * 50L;
            plugin.getProxy().getScheduler().buildTask(plugin, () -> {
                plugin.getProxy().getCommandManager().executeAsync(
                    plugin.getProxy().getConsoleCommandSource(),
                    command
                );
            }).delay(java.time.Duration.ofMillis(delayMs)).schedule();
        }
    }

    /**
     * 处理忽略玩家命令
     */
    private void handleIgnore(Player player, String targetName) {
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

    /**
     * 处理禁止被@ 命令
     */
    private void handleNoAt(Player player) {
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

    /**
     * 处理@时静音 命令
     */
    private void handleMuteAt(Player player) {
        PlayerConfig playerConfig = PlayerConfig.getInstance();
        PlayerConfig.PlayerSettings settings = playerConfig.getSettings(player);
        
        settings.muteAtMention = !settings.muteAtMention;
        playerConfig.saveSettings(settings);
        
        if (settings.muteAtMention) {
            player.sendMessage(Component.text("✓ 已禁用@声音").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("✓ 已启用@声音").color(NamedTextColor.GREEN));
        }
    }

    /**
     * 处理隐身命令
     */
    private void handleVanish(Player player) {
        // 权限检查：检查 LuckPerms 权限或配置文件中的管理员列表
        boolean hasPermission = player.hasPermission(Const.PERMISSION_VANISH) || config.isAdmin(player);
        
        if (!hasPermission) {
            player.sendMessage(Component.text("✗ 权限不足，需要 ").color(NamedTextColor.RED)
                .append(Component.text(Const.PERMISSION_VANISH).color(NamedTextColor.YELLOW))
                .append(Component.text(" 权限，或在配置文件 admins 列表中").color(NamedTextColor.RED)));
            return;
        }
        
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
        org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWebSocketFrameHandler.broadcastPlayerList(plugin);
    }

    private void handleWsAddress(CommandSource source) {
        String host = "服务器IP或域名";
        try {
            InetSocketAddress address = plugin.getProxy().getBoundAddress();
            if (address != null && address.getHostString() != null) {
                host = address.getHostString();
            }
        } catch (Exception ignored) {
        }
        if ("0.0.0.0".equals(host) || "::".equals(host)) {
            host = "服务器IP或域名";
        }
        int port = config.wsport;
        // 直接连接地址（无SSL）
        String wsUrl = "ws://" + host + ":" + port + "/ws";
        // 反代连接地址（有SSL）
        String wssUrl = "wss://" + host + ":31115/new-ws";
        source.sendMessage(Component.text("直接连接地址: ").color(NamedTextColor.GOLD)
            .append(Component.text(wsUrl).color(NamedTextColor.AQUA)
                .clickEvent(ClickEvent.copyToClipboard(wsUrl))));
        source.sendMessage(Component.text("反代连接地址 (SSL): ").color(NamedTextColor.GOLD)
            .append(Component.text(wssUrl).color(NamedTextColor.GREEN)
                .clickEvent(ClickEvent.copyToClipboard(wssUrl))));
        source.sendMessage(Component.text("提示: 使用反代地址需要配置 Nginx 反向代理").color(NamedTextColor.GRAY));
    }

    /**
     * 处理前后缀命令
     * /yinwuchat format edit - 进入编辑模式
     * /yinwuchat format show - 显示当前前后缀
     * /yinwuchat format public prefix set <内容> - 设置公聊前缀
     * /yinwuchat format public suffix set <内容> - 设置公聊后缀
     * /yinwuchat format private prefix set <内容> - 设置私聊前缀
     * /yinwuchat format private suffix set <内容> - 设置私聊后缀
     * /yinwuchat format <public|private> <prefix|suffix> clear - 清除前后缀
     */
    private void handleFormat(Player player, String[] args) {
        // 检查是否启用了前后缀功能
        if (!config.allowPlayerFormatPrefixSuffix) {
            player.sendMessage(Component.text("✗ 服务器已禁用自定义前后缀功能").color(NamedTextColor.RED));
            return;
        }
        
        if (args.length < 2) {
            showFormatHelp(player);
            return;
        }
        
        String subCommand = args[1].toLowerCase(Locale.ROOT);
        
        switch (subCommand) {
            case "edit":
                showFormatEditMenu(player);
                break;
            case "show":
                showCurrentFormat(player);
                break;
            case "public":
            case "private":
                if (args.length < 4) {
                    showFormatHelp(player);
                    return;
                }
                handleFormatSet(player, args);
                break;
            default:
                showFormatHelp(player);
        }
    }
    
    /**
     * 显示前后缀编辑菜单
     */
    private void showFormatEditMenu(Player player) {
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("=== 聊天前后缀编辑模式 ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("已进入编辑模式，选择编辑前缀还是后缀：").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text(""));
        
        // 公聊前缀
        player.sendMessage(Component.text("公聊前缀 ").color(NamedTextColor.WHITE)
            .append(Component.text("[✔]").color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.suggestCommand("/yinwuchat format public prefix set "))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                    Component.text("点击设置公聊前缀").color(NamedTextColor.GRAY)))));
        
        // 公聊后缀
        player.sendMessage(Component.text("公聊后缀 ").color(NamedTextColor.WHITE)
            .append(Component.text("[✔]").color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.suggestCommand("/yinwuchat format public suffix set "))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                    Component.text("点击设置公聊后缀").color(NamedTextColor.GRAY)))));
        
        // 私聊前缀
        player.sendMessage(Component.text("私聊前缀 ").color(NamedTextColor.WHITE)
            .append(Component.text("[✔]").color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.suggestCommand("/yinwuchat format private prefix set "))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                    Component.text("点击设置私聊前缀").color(NamedTextColor.GRAY)))));
        
        // 私聊后缀
        player.sendMessage(Component.text("私聊后缀 ").color(NamedTextColor.WHITE)
            .append(Component.text("[✔]").color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.suggestCommand("/yinwuchat format private suffix set "))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                    Component.text("点击设置私聊后缀").color(NamedTextColor.GRAY)))));
        
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("提示：点击 [✔] 后在聊天框中输入内容，支持颜色代码 (&a, &b 等)").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("最大前缀长度: " + config.maxPrefixLength + " | 最大后缀长度: " + config.maxSuffixLength).color(NamedTextColor.GRAY));
    }
    
    /**
     * 显示当前前后缀设置
     */
    private void showCurrentFormat(Player player) {
        PlayerConfig playerConfig = PlayerConfig.getInstance();
        PlayerConfig.PlayerSettings settings = playerConfig.getSettings(player);
        
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("=== 你的聊天前后缀 ===").color(NamedTextColor.GOLD));
        
        // 公聊前缀
        String publicPrefix = settings.publicPrefix;
        if (publicPrefix == null || publicPrefix.isEmpty()) {
            player.sendMessage(Component.text("公聊前缀: ").color(NamedTextColor.WHITE)
                .append(Component.text("(未设置)").color(NamedTextColor.GRAY)));
        } else {
            player.sendMessage(Component.text("公聊前缀: ").color(NamedTextColor.WHITE)
                .append(Component.text(publicPrefix.replace("&", "§")).color(NamedTextColor.AQUA))
                .append(Component.text(" [清除]").color(NamedTextColor.RED)
                    .clickEvent(ClickEvent.runCommand("/yinwuchat format public prefix clear"))
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        Component.text("点击清除公聊前缀").color(NamedTextColor.RED)))));
        }
        
        // 公聊后缀
        String publicSuffix = settings.publicSuffix;
        if (publicSuffix == null || publicSuffix.isEmpty()) {
            player.sendMessage(Component.text("公聊后缀: ").color(NamedTextColor.WHITE)
                .append(Component.text("(未设置)").color(NamedTextColor.GRAY)));
        } else {
            player.sendMessage(Component.text("公聊后缀: ").color(NamedTextColor.WHITE)
                .append(Component.text(publicSuffix.replace("&", "§")).color(NamedTextColor.AQUA))
                .append(Component.text(" [清除]").color(NamedTextColor.RED)
                    .clickEvent(ClickEvent.runCommand("/yinwuchat format public suffix clear"))
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        Component.text("点击清除公聊后缀").color(NamedTextColor.RED)))));
        }
        
        // 私聊前缀
        String privatePrefix = settings.privatePrefix;
        if (privatePrefix == null || privatePrefix.isEmpty()) {
            player.sendMessage(Component.text("私聊前缀: ").color(NamedTextColor.WHITE)
                .append(Component.text("(未设置)").color(NamedTextColor.GRAY)));
        } else {
            player.sendMessage(Component.text("私聊前缀: ").color(NamedTextColor.WHITE)
                .append(Component.text(privatePrefix.replace("&", "§")).color(NamedTextColor.AQUA))
                .append(Component.text(" [清除]").color(NamedTextColor.RED)
                    .clickEvent(ClickEvent.runCommand("/yinwuchat format private prefix clear"))
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        Component.text("点击清除私聊前缀").color(NamedTextColor.RED)))));
        }
        
        // 私聊后缀
        String privateSuffix = settings.privateSuffix;
        if (privateSuffix == null || privateSuffix.isEmpty()) {
            player.sendMessage(Component.text("私聊后缀: ").color(NamedTextColor.WHITE)
                .append(Component.text("(未设置)").color(NamedTextColor.GRAY)));
        } else {
            player.sendMessage(Component.text("私聊后缀: ").color(NamedTextColor.WHITE)
                .append(Component.text(privateSuffix.replace("&", "§")).color(NamedTextColor.AQUA))
                .append(Component.text(" [清除]").color(NamedTextColor.RED)
                    .clickEvent(ClickEvent.runCommand("/yinwuchat format private suffix clear"))
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        Component.text("点击清除私聊后缀").color(NamedTextColor.RED)))));
        }
        
        player.sendMessage(Component.text(""));
    }
    
    /**
     * 处理设置前后缀
     * 格式: /yinwuchat format <public|private> <prefix|suffix> <set|clear> [内容]
     */
    private void handleFormatSet(Player player, String[] args) {
        String namespace = args[1].toLowerCase(Locale.ROOT);  // public 或 private
        String position = args[2].toLowerCase(Locale.ROOT);   // prefix 或 suffix
        String action = args[3].toLowerCase(Locale.ROOT);     // set 或 clear
        
        PlayerConfig playerConfig = PlayerConfig.getInstance();
        PlayerConfig.PlayerSettings settings = playerConfig.getSettings(player);
        
        // 验证 position
        if (!position.equals("prefix") && !position.equals("suffix")) {
            showFormatHelp(player);
            return;
        }
        
        // 处理 clear 操作
        if (action.equals("clear")) {
            if (namespace.equals("public")) {
                if (position.equals("prefix")) {
                    settings.publicPrefix = "";
                    player.sendMessage(Component.text("✓ 已清除公聊前缀").color(NamedTextColor.GREEN));
                } else {
                    settings.publicSuffix = "";
                    player.sendMessage(Component.text("✓ 已清除公聊后缀").color(NamedTextColor.GREEN));
                }
            } else {
                if (position.equals("prefix")) {
                    settings.privatePrefix = "";
                    player.sendMessage(Component.text("✓ 已清除私聊前缀").color(NamedTextColor.GREEN));
                } else {
                    settings.privateSuffix = "";
                    player.sendMessage(Component.text("✓ 已清除私聊后缀").color(NamedTextColor.GREEN));
                }
            }
            playerConfig.saveSettings(settings);
            return;
        }
        
        // 处理 set 操作
        if (!action.equals("set")) {
            showFormatHelp(player);
            return;
        }
        
        // 获取设置的内容
        if (args.length < 5) {
            player.sendMessage(Component.text("✗ 请输入要设置的内容").color(NamedTextColor.RED));
            return;
        }
        
        // 合并剩余参数作为内容
        String content = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
        
        // 过滤禁止的样式代码
        content = filterDenyStyle(content, config.playerFormatPrefixSuffixDenyStyle);
        
        // 检查长度
        int maxLength = position.equals("prefix") ? config.maxPrefixLength : config.maxSuffixLength;
        String strippedContent = content.replaceAll("[&§][0-9a-fA-Fk-oK-OrRxX]", "");  // 移除颜色代码计算实际长度
        
        if (strippedContent.length() > maxLength) {
            player.sendMessage(Component.text("✗ 前缀或后缀长度不得超过规定长度！").color(NamedTextColor.RED));
            player.sendMessage(Component.text("  当前长度: " + strippedContent.length() + " | 最大长度: " + maxLength).color(NamedTextColor.GRAY));
            return;
        }
        
        // 设置前后缀
        String typeName;
        if (namespace.equals("public")) {
            if (position.equals("prefix")) {
                settings.publicPrefix = content;
                typeName = "公聊前缀";
            } else {
                settings.publicSuffix = content;
                typeName = "公聊后缀";
            }
        } else {
            if (position.equals("prefix")) {
                settings.privatePrefix = content;
                typeName = "私聊前缀";
            } else {
                settings.privateSuffix = content;
                typeName = "私聊后缀";
            }
        }
        
        playerConfig.saveSettings(settings);
        player.sendMessage(Component.text("✓ 已设置" + typeName + "为: ").color(NamedTextColor.GREEN)
            .append(Component.text(content.replace("&", "§")).color(NamedTextColor.AQUA)));
    }
    
    /**
     * 过滤禁止的样式代码
     */
    private String filterDenyStyle(String content, String denyStyle) {
        if (denyStyle == null || denyStyle.isEmpty()) {
            return content;
        }
        
        for (char c : denyStyle.toCharArray()) {
            content = content.replace("&" + c, "").replace("§" + c, "");
        }
        return content;
    }
    
    /**
     * 显示前后缀帮助
     */
    private void showFormatHelp(Player player) {
        player.sendMessage(Component.text("=== 聊天前后缀帮助 ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/yinwuchat format edit").color(NamedTextColor.AQUA)
            .append(Component.text(": 进入编辑模式").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/yinwuchat format show").color(NamedTextColor.AQUA)
            .append(Component.text(": 显示当前前后缀").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/yinwuchat format public prefix set <内容>").color(NamedTextColor.AQUA)
            .append(Component.text(": 设置公聊前缀").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/yinwuchat format public suffix set <内容>").color(NamedTextColor.AQUA)
            .append(Component.text(": 设置公聊后缀").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/yinwuchat format private prefix set <内容>").color(NamedTextColor.AQUA)
            .append(Component.text(": 设置私聊前缀").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/yinwuchat format private suffix set <内容>").color(NamedTextColor.AQUA)
            .append(Component.text(": 设置私聊后缀").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/yinwuchat format <public|private> <prefix|suffix> clear").color(NamedTextColor.AQUA)
            .append(Component.text(": 清除前后缀").color(NamedTextColor.GRAY)));
    }
    
    /**
     * 显示禁言命令帮助信息
     */
    private void showMuteHelp(CommandSource source) {
        source.sendMessage(Component.text("=== 禁言命令帮助 ===").color(NamedTextColor.GOLD));
        source.sendMessage(Component.empty());
        
        // mute 命令
        source.sendMessage(Component.text("/yinwuchat mute <玩家> [时长] [原因]").color(NamedTextColor.AQUA));
        source.sendMessage(Component.text("  禁言指定玩家").color(NamedTextColor.GRAY));
        source.sendMessage(Component.empty());
        
        // 参数说明
        source.sendMessage(Component.text("参数说明:").color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("  <玩家>").color(NamedTextColor.WHITE)
                .append(Component.text(" - 必填，要禁言的玩家名").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  [时长]").color(NamedTextColor.WHITE)
                .append(Component.text(" - 可选，禁言时长，不填则永久").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  [原因]").color(NamedTextColor.WHITE)
                .append(Component.text(" - 可选，禁言原因").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.empty());
        
        // 时长格式
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
        
        // 示例
        source.sendMessage(Component.text("示例:").color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("  /yinwuchat mute Steve").color(NamedTextColor.GREEN)
                .append(Component.text(" - 永久禁言").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  /yinwuchat mute Steve 1h").color(NamedTextColor.GREEN)
                .append(Component.text(" - 禁言1小时").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  /yinwuchat mute Steve 30m 刷屏").color(NamedTextColor.GREEN)
                .append(Component.text(" - 禁言30分钟，原因：刷屏").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  /yinwuchat mute Steve 违规发言").color(NamedTextColor.GREEN)
                .append(Component.text(" - 永久禁言，原因：违规发言").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.empty());
        
        // 其他命令
        source.sendMessage(Component.text("相关命令:").color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("  /yinwuchat unmute <玩家>").color(NamedTextColor.AQUA)
                .append(Component.text(" - 解除禁言").color(NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  /yinwuchat muteinfo <玩家>").color(NamedTextColor.AQUA)
                .append(Component.text(" - 查看禁言信息").color(NamedTextColor.GRAY)));
    }
    
    /**
     * 处理禁言命令
     * /yinwuchat mute <玩家> [时长] [原因]
     * 时长格式: 1d (天), 2h (小时), 30m (分钟), 60s (秒), 或纯数字（秒）
     * 不指定时长则为永久禁言
     */
    private void handleMute(CommandSource source, String[] args) {
        // 控制台默认拥有权限，玩家需要检查权限
        if (source instanceof Player && !source.hasPermission(Const.PERMISSION_MUTE) && !config.isAdmin((Player)source)) {
            source.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
            return;
        }
        
        if (args.length < 2) {
            showMuteHelp(source);
            return;
        }
        
        String targetName = args[1];
        
        // 验证玩家名格式（只允许字母、数字和下划线，长度1-16）
        if (!targetName.matches("^[a-zA-Z0-9_]{1,16}$")) {
            source.sendMessage(Component.text("✗ 无效的玩家名格式").color(NamedTextColor.RED));
            source.sendMessage(Component.text("  玩家名只能包含字母、数字和下划线，长度1-16位").color(NamedTextColor.GRAY));
            return;
        }
        
        long duration = 0;  // 默认永久
        String reason = "";
        
        // 解析时长
        if (args.length >= 3) {
            duration = org.lintx.plugins.yinwuchat.velocity.manage.MuteManage.parseTime(args[2]);
            if (duration == -1) {
                // 可能是原因而不是时长
                duration = 0;
                reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            } else if (args.length >= 4) {
                // 有时长和原因
                reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
            }
        }
        
        // 获取操作者名称
        String operatorName = source instanceof Player ? ((Player) source).getUsername() : "Console";
        
        // 执行禁言
        boolean success = org.lintx.plugins.yinwuchat.velocity.manage.MuteManage.getInstance()
                .mutePlayer(targetName, duration, operatorName, reason);
        
        if (success) {
            String durationStr = duration > 0 ? 
                    org.lintx.plugins.yinwuchat.velocity.manage.MuteManage.formatTime(duration) : "永久";
            source.sendMessage(Component.text("✓ 已禁言 " + targetName + " (" + durationStr + ")").color(NamedTextColor.GREEN));
            if (!reason.isEmpty()) {
                source.sendMessage(Component.text("  原因: " + reason).color(NamedTextColor.GRAY));
            }
        } else {
            source.sendMessage(Component.text("✗ 禁言失败").color(NamedTextColor.RED));
        }
    }
    
    /**
     * 处理解除禁言命令
     * /yinwuchat unmute <玩家>
     */
    private void handleUnmute(CommandSource source, String[] args) {
        // 控制台默认拥有权限，玩家需要检查权限
        if (source instanceof Player && !source.hasPermission(Const.PERMISSION_MUTE) && !config.isAdmin((Player)source)) {
            source.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
            return;
        }
        
        if (args.length < 2) {
            source.sendMessage(Component.text("✗ 缺少玩家名参数").color(NamedTextColor.RED));
            source.sendMessage(Component.text("用法: /yinwuchat unmute <玩家>").color(NamedTextColor.YELLOW));
            source.sendMessage(Component.text("示例: /yinwuchat unmute Steve").color(NamedTextColor.GRAY));
            return;
        }
        
        String targetName = args[1];
        
        // 验证玩家名格式
        if (!targetName.matches("^[a-zA-Z0-9_]{1,16}$")) {
            source.sendMessage(Component.text("✗ 无效的玩家名格式").color(NamedTextColor.RED));
            source.sendMessage(Component.text("  玩家名只能包含字母、数字和下划线，长度1-16位").color(NamedTextColor.GRAY));
            return;
        }
        
        String operatorName = source instanceof Player ? ((Player) source).getUsername() : "Console";
        
        boolean success = org.lintx.plugins.yinwuchat.velocity.manage.MuteManage.getInstance()
                .unmutePlayer(targetName, operatorName);
        
        if (success) {
            source.sendMessage(Component.text("✓ 已解除 " + targetName + " 的禁言").color(NamedTextColor.GREEN));
        } else {
            source.sendMessage(Component.text("✗ 该玩家未被禁言").color(NamedTextColor.YELLOW));
        }
    }
    
    /**
     * 处理查看禁言信息命令
     * /yinwuchat muteinfo <玩家>
     */
    private void handleMuteInfo(CommandSource source, String[] args) {
        // 控制台默认拥有权限，玩家需要检查权限
        if (source instanceof Player && !source.hasPermission(Const.PERMISSION_MUTE) && !config.isAdmin((Player)source)) {
            source.sendMessage(Component.text("✗ 权限不足").color(NamedTextColor.RED));
            return;
        }
        
        if (args.length < 2) {
            source.sendMessage(Component.text("✗ 缺少玩家名参数").color(NamedTextColor.RED));
            source.sendMessage(Component.text("用法: /yinwuchat muteinfo <玩家>").color(NamedTextColor.YELLOW));
            source.sendMessage(Component.text("示例: /yinwuchat muteinfo Steve").color(NamedTextColor.GRAY));
            return;
        }
        
        String targetName = args[1];
        
        // 验证玩家名格式
        if (!targetName.matches("^[a-zA-Z0-9_]{1,16}$")) {
            source.sendMessage(Component.text("✗ 无效的玩家名格式").color(NamedTextColor.RED));
            source.sendMessage(Component.text("  玩家名只能包含字母、数字和下划线，长度1-16位").color(NamedTextColor.GRAY));
            return;
        }
        
        String info = org.lintx.plugins.yinwuchat.velocity.manage.MuteManage.getInstance()
                .getMuteInfo(targetName);
        
        if (info != null) {
            source.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacyAmpersand().deserialize(info));
        } else {
            source.sendMessage(Component.text("✓ " + targetName + " 未被禁言").color(NamedTextColor.GREEN));
        }
    }
}
