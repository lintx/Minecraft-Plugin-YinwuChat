package org.lintx.plugins.yinwuchat.velocity.httpserver;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.lintx.plugins.yinwuchat.velocity.YinwuChat;
import org.lintx.plugins.yinwuchat.velocity.config.Config;
import org.lintx.plugins.yinwuchat.velocity.json.*;
import org.lintx.plugins.yinwuchat.velocity.message.MessageManage;
import org.lintx.plugins.yinwuchat.velocity.config.PlayerConfig;
import org.lintx.plugins.yinwuchat.common.auth.AuthService;
import org.lintx.plugins.yinwuchat.common.auth.AuthUserStore;
import org.lintx.plugins.yinwuchat.velocity.manage.MuteManage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * WebSocket frame handler for Velocity
 * Processes incoming WebSocket frames from web clients and AQQBot (OneBot标准)
 * 支持 AQQBot、Lagrange、LLoneBot 等基于 OneBot 标准的 QQ 机器人框架
 */
public class VelocityWebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private final YinwuChat plugin;
    
    public VelocityWebSocketFrameHandler(YinwuChat plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            String request = ((TextWebSocketFrame) frame).text();
            Channel channel = ctx.channel();
            
            // Handle WebSocket message
            plugin.getLogger().debug("WebSocket message received: " + request);
            
            // 异步处理消息
            plugin.getProxy().getScheduler().buildTask(plugin, () -> {
                InputBase object = InputBase.getObject(request);
                
                if (object instanceof InputCheckToken) {
                    // Token 验证
                    handleTokenCheck(channel, (InputCheckToken) object);
                } else if (object instanceof InputMessage) {
                    // 消息处理
                    handleMessage(channel, (InputMessage) object);
                } else if (object instanceof InputCommand) {
                    // 命令处理
                    handleCommandAction(channel, (InputCommand) object);
                } else if (object instanceof InputPrivateMessage) {
                    // 私聊消息处理
                    handlePrivateMessageAction(channel, (InputPrivateMessage) object);
                } else if (object instanceof InputBindAccount) {
                    handleBindAccount(channel, (InputBindAccount) object);
                } else if (object instanceof InputReadCursor) {
                    handleReadCursorAction(channel, (InputReadCursor) object);
                } else if (object instanceof InputAQQBot) {
                    // AQQBot (OneBot标准) 消息
                    handleAQQBotMessage(request);
                }
            }).schedule();
        }
    }
    
    /**
     * 处理 Token 验证
     */
    private void handleTokenCheck(Channel channel, InputCheckToken tokenCheck) {
        VelocityWsClientUtil clientUtil = new VelocityWsClientUtil(tokenCheck.getToken());
        VelocityWsClientHelper.add(channel, clientUtil);
        
        NettyChannelMessageHelper.send(channel, tokenCheck.getJSON());
        
        if (!tokenCheck.getIsvaild()) {
            NettyChannelMessageHelper.send(channel, tokenCheck.getTokenJSON());
        } else {
            if (tokenCheck.getIsbind()) {
                clientUtil.setUUID(tokenCheck.getUuid());
                            // 不再踢掉其他同账号连接，允许多设备同时在线
                            // VelocityWsClientHelper.kickOtherWS(channel, tokenCheck.getUuid());
                if (enforceAccountBan(channel, clientUtil)) {
                    return;
                }
                sendSelfInfo(channel, tokenCheck.getUuid());
                // 发送离线消息
                String playerName = PlayerConfig.getInstance().getTokenManager().getName(tokenCheck.getUuid());
                if (playerName != null && !playerName.isEmpty()) {
                    org.lintx.plugins.yinwuchat.velocity.message.MessageManage.getInstance()
                        .replayMessagesToWeb(channel, playerName);
                }
                
                // 发送玩家列表
                sendPlayerList(channel);
            }
        }
    }
    
    /**
     * 处理消息发送
     */
    private void handleMessage(Channel channel, InputMessage inputMessage) {
        if (inputMessage.getMessage().isEmpty()) {
            return;
        }
        
        VelocityWsClientUtil util = VelocityWsClientHelper.get(channel);
        if (util == null || util.getUuid() == null) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("请先绑定账号").getJSON());
            return;
        }
        if (enforceAccountBan(channel, util)) {
            return;
        }
        
        // 检查冷却时间
        Config config = plugin.getConfig();
        if (!util.getLastDate().isEqual(LocalDateTime.MIN)) {
            Duration duration = Duration.between(util.getLastDate(), LocalDateTime.now());
            if (duration.toMillis() < config.wsCooldown) {
                NettyChannelMessageHelper.send(channel, 
                    OutputServerMessage.errorJSON("发送消息过快（当前设置为每条消息之间最少间隔" + config.wsCooldown + "毫秒）").getJSON());
                return;
            }
        }
        util.updateLastDate();
        
        String message = inputMessage.getMessage();
        
        // 处理命令
        if (message.startsWith("/")) {
            handleCommand(channel, util, message);
            return;
        }
        
        // 发送公屏消息
        MessageManage.getInstance().handleWebPublicMessage(util.getUuid(), message, channel);
    }

    /**
     * 处理命令动作
     */
    private void handleCommandAction(Channel channel, InputCommand inputCommand) {
        VelocityWsClientUtil util = VelocityWsClientHelper.get(channel);
        if (util == null || util.getUuid() == null) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("请先绑定账号").getJSON());
            return;
        }

        String fullCommand = inputCommand.getCommand();
        if (fullCommand.isEmpty()) return;

        // 统一调用 handleCommand 逻辑
        handleCommand(channel, util, "/" + fullCommand);
    }

    /**
     * 处理私聊消息动作
     */
    private void handlePrivateMessageAction(Channel channel, InputPrivateMessage inputPrivateMessage) {
        VelocityWsClientUtil util = VelocityWsClientHelper.get(channel);
        if (util == null || util.getUuid() == null) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("请先绑定账号").getJSON());
            return;
        }
        if (enforceAccountBan(channel, util)) {
            return;
        }

        if (inputPrivateMessage.getMessage().isEmpty() || inputPrivateMessage.getTo().isEmpty()) {
            return;
        }

        MessageManage.getInstance().handleWebPrivateMessage(channel, util, inputPrivateMessage.getTo(), inputPrivateMessage.getMessage());
    }

    private void handleReadCursorAction(Channel channel, InputReadCursor inputReadCursor) {
        VelocityWsClientUtil util = VelocityWsClientHelper.get(channel);
        if (util == null || util.getUuid() == null) {
            return;
        }
        String playerName = PlayerConfig.getInstance().getTokenManager().getName(util.getUuid());
        if (playerName == null || playerName.isEmpty()) {
            return;
        }
        MessageManage.getInstance().updateWebReadCursor(playerName, inputReadCursor.getChat(), inputReadCursor.getMessageId());
    }

    private void handleBindAccount(Channel channel, InputBindAccount input) {
        VelocityWsClientUtil util = VelocityWsClientHelper.get(channel);
        if (util == null || util.getUuid() == null) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("请先绑定账号").getJSON());
            return;
        }
        String account = input.getAccount();
        if (account == null || account.trim().isEmpty()) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("Web 账户名不能为空").getJSON());
            return;
        }
        String playerName = PlayerConfig.getInstance().getTokenManager().getName(util.getUuid());
        if (playerName == null || playerName.isEmpty()) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("无法获取玩家名称").getJSON());
            return;
        }
        AuthService authService = AuthService.getInstance(plugin.getDataFolder().toFile());
        if (!authService.accountExists(account)) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("Web 账户不存在").getJSON());
            return;
        }
        // 检查账户是否已绑定该玩家，如果已绑定则不发送确认消息
        String existingBound = authService.getBoundPlayerName(account);
        boolean alreadyBound = playerName.equalsIgnoreCase(existingBound);
        
        authService.bindAccountPlayerName(account, playerName);
        util.setAccount(account);
        if (enforceAccountBan(channel, util)) {
            return;
        }
        // 只在首次绑定时发送确认消息
        if (!alreadyBound) {
        NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("✓ 已绑定 Web 账户与玩家名").getJSON());
        }
    }
    
    /**
     * 处理命令
     */
    private void handleCommand(Channel channel, VelocityWsClientUtil util, String message) {
        String[] command = message.replaceFirst("^/", "").split("\\s");
        String label = command[0].toLowerCase();
        if (label.equals("chatban")) {
            command[0] = "ban";
            label = "ban";
        }

        if (enforceAccountBan(channel, util)) {
            return;
        }
        
        if (command.length >= 3 && (label.equals("msg") || label.equals("tell") || label.equals("whisper") || label.equals("w") || label.equals("t"))) {
            String toPlayerName = command[1];
            List<String> tmpList = new ArrayList<>(Arrays.asList(command).subList(2, command.length));
            String msg = String.join(" ", tmpList);
            
            MessageManage.getInstance().handleWebPrivateMessage(channel, util, toPlayerName, msg);
        } else if (label.equals("yinwuchat") || label.equals("ignore") || label.equals("noat") || label.equals("vanish")
            || label.equals("ban") || label.equals("atalladmin") || label.equals("atalladmin_execute")
            || label.equals("mute") || label.equals("unmute") || label.equals("muteinfo")) {
            // 对于插件自身指令，无论玩家是否在线都允许执行
            String[] args;
            if (label.equals("yinwuchat")) {
                args = command.length > 1 ? Arrays.copyOfRange(command, 1, command.length) : new String[]{"ws"};
            } else {
                args = command;
            }
            handleWebPlayerCommand(channel, util, args);
        } else {
            // 其他指令必须在游戏内在线才能执行
            plugin.getProxy().getPlayer(util.getUuid()).ifPresentOrElse(player -> {
                plugin.getProxy().getCommandManager().executeAsync(player, message.substring(1));
            }, () -> {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 你必须在游戏内在线才能执行此指令").getJSON());
            });
        }
    }

    private static boolean isAdmin(YinwuChat plugin, String playerName) {
        if (playerName == null) return false;
        java.util.Optional<com.velocitypowered.api.proxy.Player> player = plugin.getProxy().getPlayer(playerName);
        if (player.isPresent()) {
            return plugin.getConfig().isAdmin(player.get());
        }
        return plugin.getConfig().isAdmin(playerName);
    }

    /**
     * 处理 Web 玩家的特殊命令（无需在游戏内在线）
     */
    private void handleWebPlayerCommand(Channel channel, VelocityWsClientUtil util, String[] args) {
        UUID uuid = util.getUuid();
        String playerName = PlayerConfig.getInstance().getTokenManager().getName(uuid);
        if (playerName == null || playerName.isEmpty()) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("无法获取玩家名称").getJSON());
            return;
        }

        String cmd = args[0].toLowerCase();
        PlayerConfig playerConfig = PlayerConfig.getInstance();
        PlayerConfig.PlayerSettings settings = playerConfig.getSettings(playerName);
        boolean isAdmin = isAdmin(plugin, playerName);

        if (cmd.equals("ws")) {
            if (!isAdmin && !plugin.getProxy().getPlayer(uuid).map(p -> p.hasPermission(org.lintx.plugins.yinwuchat.Const.PERMISSION_WS)).orElse(false)) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 权限不足").getJSON());
                return;
            }
            String host = "服务器IP或域名";
            try {
                java.net.InetSocketAddress address = plugin.getProxy().getBoundAddress();
                if (address != null && address.getHostString() != null) {
                    host = address.getHostString();
                }
            } catch (Exception ignored) {
            }
            if ("0.0.0.0".equals(host) || "::".equals(host)) {
                host = "服务器IP或域名";
            }
            int port = plugin.getConfig().wsport;
            String wsUrl = "ws://" + host + ":" + port + "/ws";
            NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("WebSocket 地址: " + wsUrl + "\n如果网页使用 HTTPS，请将 ws:// 改为 wss://").getJSON());
        } else if (cmd.equals("ignore")) {
            if (!isAdmin && !plugin.getProxy().getPlayer(uuid).map(p -> p.hasPermission(org.lintx.plugins.yinwuchat.Const.PERMISSION_IGNORE)).orElse(false)) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 权限不足").getJSON());
                return;
            }
            if (args.length < 2) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("用法: /ignore <玩家名>").getJSON());
                return;
            }
            String targetName = args[1];
            if (targetName.equalsIgnoreCase(playerName)) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("你不能忽略你自己").getJSON());
                return;
            }
            if (settings.isIgnored(targetName)) {
                settings.unignorePlayer(targetName);
                playerConfig.saveSettings(settings);
                NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("✓ 已取消忽略 " + targetName).getJSON());
            } else {
                settings.ignorePlayer(targetName);
                playerConfig.saveSettings(settings);
                NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("✓ 已忽略 " + targetName).getJSON());
            }
        } else if (cmd.equals("noat")) {
            if (!isAdmin && !plugin.getProxy().getPlayer(uuid).map(p -> p.hasPermission(org.lintx.plugins.yinwuchat.Const.PERMISSION_NOAT)).orElse(false)) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 权限不足").getJSON());
                return;
            }
            settings.disableAtMention = !settings.disableAtMention;
            playerConfig.saveSettings(settings);
            if (settings.disableAtMention) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("✓ 已禁止其他玩家@你").getJSON());
            } else {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("✓ 已允许其他玩家@你").getJSON());
            }
        } else if (cmd.equals("vanish")) {
            if (!isAdmin) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 权限不足").getJSON());
                return;
            }
            settings.vanished = !settings.vanished;
            playerConfig.saveSettings(settings);
            if (settings.vanished) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("✓ 已开启隐身模式").getJSON());
            } else {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("✓ 已关闭隐身模式").getJSON());
            }
            // 更新客户端的 self_info，包含新的隐身状态
            sendSelfInfo(channel, uuid);
            // 广播更新后的玩家列表给所有人
            broadcastPlayerList(plugin);
        } else if (cmd.equals("ban") || cmd.equals("chatban")) {
            if (!isAdmin) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 权限不足").getJSON());
                return;
            }
            handleWebBanCommand(channel, playerName, args);
        } else if (cmd.equals("atalladmin")) {
            handleWebAtAllAdminCommand(channel, util, playerName, isAdmin, args);
        } else if (cmd.equals("atalladmin_execute")) {
            handleWebAtAllAdminExecute(channel, util, playerName, isAdmin);
        } else if (cmd.equals("mute") || cmd.equals("unmute") || cmd.equals("muteinfo")) {
            if (!isAdmin) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 权限不足").getJSON());
                return;
            }
            // 转发给 MessageManage 处理禁言逻辑
            String fullCmd = String.join(" ", args);
            MessageManage.getInstance().handleWebPlayerMuteCommand(channel, playerName, fullCmd);
        } else {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("可用指令: /yinwuchat [ws|ignore|noat|vanish|chatban|atalladmin|mute|unmute|muteinfo]").getJSON());
        }
    }

    private void handleWebAtAllAdminCommand(Channel channel, VelocityWsClientUtil util, String playerName, boolean isAdmin, String[] args) {
        boolean hasPermission = isAdmin || plugin.getProxy().getPlayer(util.getUuid())
            .map(p -> p.hasPermission(org.lintx.plugins.yinwuchat.Const.PERMISSION_AT_ALL_ADMIN))
            .orElse(false);
        if (!hasPermission) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 权限不足").getJSON());
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
            boolean canReset = isAdmin || plugin.getProxy().getPlayer(util.getUuid())
                .map(p -> p.hasPermission(org.lintx.plugins.yinwuchat.Const.PERMISSION_AT_ALL_ADMIN_RESET))
                .orElse(false);
            if (!canReset) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 权限不足").getJSON());
                return;
            }
            if (args.length < 3) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("用法: /yinwuchat atalladmin confirm <玩家名>").getJSON());
                return;
            }
            String targetName = args[2];
            PlayerConfig.PlayerSettings settings = PlayerConfig.getInstance().getSettings(targetName);
            if (settings != null) {
                settings.lastAtAllAdmin = 0;
                PlayerConfig.getInstance().saveSettings(settings);
                NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("✓ 已重置玩家 " + targetName + " 的突发事件提醒冷却时间").getJSON());
            } else {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 未找到玩家 " + targetName).getJSON());
            }
            return;
        }

        PlayerConfig.PlayerSettings settings = PlayerConfig.getInstance().getSettings(playerName);
        long now = System.currentTimeMillis();
        if (settings.lastAtAllAdmin > 0 && now - settings.lastAtAllAdmin < 24 * 60 * 60 * 1000L) {
            long remaining = 24 * 60 * 60 * 1000L - (now - settings.lastAtAllAdmin);
            long hours = remaining / (60 * 60 * 1000L);
            long minutes = (remaining % (60 * 60 * 1000L)) / (60 * 1000L);
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 你今天已经使用过该功能了，请在 " + hours + " 小时 " + minutes + " 分钟后再试").getJSON());
            return;
        }

        com.google.gson.JsonObject confirm = new com.google.gson.JsonObject();
        confirm.addProperty("action", "atalladmin_confirm");
        confirm.addProperty("message", "⚠ 请确认目前情况是否需要提醒所有管理员，若情况不属实，将承担被封禁的风险！！");
        NettyChannelMessageHelper.send(channel, confirm.toString());
    }

    private void handleWebAtAllAdminExecute(Channel channel, VelocityWsClientUtil util, String playerName, boolean isAdmin) {
        boolean hasPermission = isAdmin || plugin.getProxy().getPlayer(util.getUuid())
            .map(p -> p.hasPermission(org.lintx.plugins.yinwuchat.Const.PERMISSION_AT_ALL_ADMIN))
            .orElse(false);
        if (!hasPermission) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 权限不足").getJSON());
            return;
        }
        PlayerConfig.PlayerSettings settings = PlayerConfig.getInstance().getSettings(playerName);
        long now = System.currentTimeMillis();
        if (settings.lastAtAllAdmin > 0 && now - settings.lastAtAllAdmin < 24 * 60 * 60 * 1000L) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 冷却中").getJSON());
            return;
        }
        settings.lastAtAllAdmin = now;
        PlayerConfig.getInstance().saveSettings(settings);

        String senderName = playerName == null || playerName.isEmpty() ? "Web用户" : playerName;
        com.google.gson.JsonObject webAlert = new com.google.gson.JsonObject();
        webAlert.addProperty("action", "admin_alert");
        webAlert.addProperty("message", "存在需要立即处理的服务器风险");
        webAlert.addProperty("player", senderName);
        String webAlertJson = webAlert.toString();

        for (com.velocitypowered.api.proxy.Player p : plugin.getProxy().getAllPlayers()) {
            if (plugin.getConfig().isAdmin(p)) {
                p.sendMessage(net.kyori.adventure.text.Component.text("警告！" + senderName + " 报告服务器存在突发事件，请立即查看")
                    .color(net.kyori.adventure.text.format.NamedTextColor.RED)
                    .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
            }
        }
        for (io.netty.channel.Channel wsChannel : VelocityWsClientHelper.getChannels()) {
            VelocityWsClientUtil wsUtil = VelocityWsClientHelper.get(wsChannel);
            if (wsUtil != null && wsUtil.getUuid() != null) {
                String webName = PlayerConfig.getInstance().getTokenManager().getName(wsUtil.getUuid());
                if (webName != null && plugin.getConfig().isAdmin(webName)) {
                    NettyChannelMessageHelper.send(wsChannel, webAlertJson);
                }
            }
        }
        NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("✓ 突发事件报告已发送给所有管理员").getJSON());
    }

    private void handleWebBanCommand(Channel channel, String operatorName, String[] args) {
        if (args.length < 2) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("用法: /yinwuchat ban <账号名/玩家名> [时长] [理由]").getJSON());
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
            reason = String.join(" ", Arrays.copyOfRange(args, reasonStart, args.length));
        }
        AuthService authService = AuthService.getInstance(plugin.getDataFolder().toFile());
        String accountName = target;
        String playerName = "";
        if (!authService.accountExists(target)) {
            String mapped = authService.resolveAccountByPlayerName(target);
            if (mapped == null || mapped.isEmpty()) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 未找到对应的 Web 账户").getJSON());
                return;
            }
            accountName = mapped;
            playerName = target;
        } else {
            playerName = authService.getBoundPlayerName(accountName);
        }
        AuthService.BanResult result = authService.banUser(accountName, durationMillis, reason, operatorName);
        if (result.notFound) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 未找到该 Web 账户").getJSON());
            return;
        }
        if (!result.ok) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 封禁失败，请稍后重试").getJSON());
            return;
        }
        if (playerName != null && !playerName.isEmpty()) {
            long muteSeconds = durationMillis <= 0L ? 0L : Math.max(1L, durationMillis / 1000L);
            MuteManage.getInstance().mutePlayer(playerName, muteSeconds, operatorName, reason);
        }
        String durationText = AuthService.formatDuration(durationMillis <= 0L ? -1L : durationMillis);
        String info = "✓ 已封禁账号 " + accountName
            + "，时长: " + (durationText.isEmpty() ? "永久" : durationText)
            + (reason == null || reason.isEmpty() ? "" : "，理由: " + reason);
        if (playerName != null && !playerName.isEmpty()) {
            info += "（玩家: " + playerName + "）";
        }
        NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON(info).getJSON());
        notifyBanToAdmins(accountName, playerName, durationText, reason, channel);
        kickWebPlayerByName(playerName, accountName, durationText, reason);
    }

    private void notifyBanToAdmins(String accountName, String playerName, String durationText, String reason, Channel excludeChannel) {
        String message = "账号 " + accountName + " 已被封禁"
            + (playerName != null && !playerName.isEmpty() ? "（玩家: " + playerName + "）" : "")
            + "，时长: " + (durationText == null || durationText.isEmpty() ? "永久" : durationText)
            + (reason == null || reason.isEmpty() ? "" : "，理由: " + reason);

        // 游戏内管理员
        for (com.velocitypowered.api.proxy.Player p : plugin.getProxy().getAllPlayers()) {
            if (plugin.getConfig().isAdmin(p)) {
                p.sendMessage(net.kyori.adventure.text.Component.text(message)
                    .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            }
        }

        // Web 端管理员
        if (plugin.getConfig().openwsserver && YinwuChat.getWSServer() != null) {
            String json = OutputServerMessage.infoJSON(message).getJSON();
            for (io.netty.channel.Channel wsChannel : VelocityWsClientHelper.getChannels()) {
                VelocityWsClientUtil util = VelocityWsClientHelper.get(wsChannel);
                if (util == null || util.getUuid() == null) continue;
                String name = util.getAccount();
                if (name == null || name.isEmpty()) {
                    name = PlayerConfig.getInstance().getTokenManager().getName(util.getUuid());
                }
                if (name != null && plugin.getConfig().isAdmin(name)) {
                    NettyChannelMessageHelper.send(wsChannel, json);
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

        for (io.netty.channel.Channel wsChannel : VelocityWsClientHelper.getChannels()) {
            VelocityWsClientUtil util = VelocityWsClientHelper.get(wsChannel);
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
                wsChannel.writeAndFlush(new TextWebSocketFrame(payload)).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    private boolean enforceAccountBan(Channel channel, VelocityWsClientUtil util) {
        if (util == null) {
            return false;
        }
        String accountName = util.getAccount();
        String playerName = "";
        if (util.getUuid() != null) {
            String name = PlayerConfig.getInstance().getTokenManager().getName(util.getUuid());
            if (name != null) {
                playerName = name;
            }
        }
        if (accountName == null || accountName.isEmpty()) {
            if (playerName == null || playerName.isEmpty()) {
                return false;
            }
            AuthService authService = AuthService.getInstance(plugin.getDataFolder().toFile());
            String mapped = authService.resolveAccountByPlayerName(playerName);
            if (mapped == null || mapped.isEmpty()) {
                return false;
            }
            accountName = mapped;
        }
        AuthService authService = AuthService.getInstance(plugin.getDataFolder().toFile());
        AuthUserStore.BanInfo banInfo = authService.getBanInfo(accountName);
        if (!banInfo.banned) {
            return false;
        }
        String durationText = AuthService.formatDuration(banInfo.permanent ? -1L : banInfo.remainingMillis);
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("action", "ban_kick");
        json.addProperty("player", playerName == null ? "" : playerName);
        json.addProperty("account", accountName == null ? "" : accountName);
        json.addProperty("durationText", durationText == null || durationText.isEmpty() ? "永久" : durationText);
        json.addProperty("reason", banInfo.reason == null ? "" : banInfo.reason);
        json.addProperty("by", banInfo.bannedBy == null ? "" : banInfo.bannedBy);
        channel.writeAndFlush(new TextWebSocketFrame(json.toString())).addListener(ChannelFutureListener.CLOSE);
        return true;
    }
    
    /**
     * 向所有已连接的客户端广播最新的玩家列表
     */
    public static void broadcastPlayerList(YinwuChat plugin) {
        for (io.netty.channel.Channel channel : VelocityWsClientHelper.getChannels()) {
            VelocityWsClientUtil util = VelocityWsClientHelper.get(channel);
            if (util != null && util.getUuid() != null) {
                // 只有已登录（绑定）的 Web 客户端才发送列表
                sendPlayerList(channel, plugin);
            }
        }
    }

    /**
     * 发送玩家列表给特定通道
     */
    private void sendPlayerList(Channel channel) {
        sendPlayerList(channel, plugin);
    }

    private static void sendPlayerList(Channel channel, YinwuChat plugin) {
        VelocityWsClientUtil util = VelocityWsClientHelper.get(channel);
        if (util == null || util.getUuid() == null) {
            return;
        }
        String webPlayerName = PlayerConfig.getInstance().getTokenManager().getName(util.getUuid());
        if (webPlayerName == null || webPlayerName.isEmpty()) {
            return;
        }
        PlayerConfig.PlayerSettings webSettings = PlayerConfig.getInstance().getSettings(webPlayerName);
        if (webSettings.vanished) {
            return;
        }
        sendPlayerStatusList(channel, plugin, webSettings);
    }

    private static void sendPlayerStatusList(Channel channel, YinwuChat plugin, PlayerConfig.PlayerSettings webSettings) {
        com.google.gson.JsonArray players = new com.google.gson.JsonArray();
        java.util.Map<String, String> gameServers = new java.util.HashMap<>();
        java.util.Set<String> gameNames = new java.util.HashSet<>();
        java.util.Set<String> webNames = new java.util.HashSet<>();
        java.util.Map<String, Integer> offlineCounts = new java.util.HashMap<>();
        boolean isAdmin = isAdmin(plugin, webSettings.playerName);

        for (com.velocitypowered.api.proxy.Player player : plugin.getProxy().getAllPlayers()) {
            PlayerConfig.PlayerSettings ps = PlayerConfig.getInstance().getSettings(player.getUsername());
            // 如果玩家隐身且查看者不是管理员，则该玩家不计入游戏在线
            if (ps.vanished && !isAdmin) continue;
            if (webSettings.isIgnored(player.getUsername())) continue;
            String name = player.getUsername();
            gameNames.add(name);
            player.getCurrentServer().ifPresent(server -> gameServers.put(name, server.getServerInfo().getName()));
        }

        for (io.netty.channel.Channel wsChannel : VelocityWsClientHelper.getChannels()) {
            VelocityWsClientUtil wsUtil = VelocityWsClientHelper.get(wsChannel);
            if (wsUtil == null || wsUtil.getUuid() == null) continue;
            String name = PlayerConfig.getInstance().getTokenManager().getName(wsUtil.getUuid());
            if (name == null || name.isEmpty()) continue;
            if (webSettings.isIgnored(name)) continue;
            
            PlayerConfig.PlayerSettings ps = PlayerConfig.getInstance().getSettings(name);
            // 如果玩家隐身且查看者不是管理员，则该玩家不计入 Web 在线
            if (ps.vanished && !isAdmin) continue;
            
            webNames.add(name);
        }

        java.util.Set<String> allNames = new java.util.HashSet<>();
        allNames.addAll(PlayerConfig.getInstance().getTokenManager().getAllNames());
        allNames.addAll(gameNames);
        allNames.addAll(webNames);

        java.util.List<String> sorted = new java.util.ArrayList<>(allNames);
        java.util.Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);

        String viewerName = webSettings.playerName;
        org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore store =
            new org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore(plugin.getDataFolder().toFile());
        if (viewerName != null && !viewerName.isEmpty()) {
            offlineCounts = store.getCounts(viewerName);
        }

        for (String name : sorted) {
            if (name == null || name.isEmpty()) continue;
            PlayerConfig.PlayerSettings ps = PlayerConfig.getInstance().getSettings(name);
            // 忽略列表中被忽略的玩家（除非是管理员查看）
            if (webSettings.isIgnored(name) && !isAdmin) continue;
            
            // 状态逻辑：如果玩家隐身且查看者不是管理员，强制显示为 offline
            String status;
            if (ps.vanished && !isAdmin) {
                status = "offline";
            } else {
                status = gameNames.contains(name) ? "game" : (webNames.contains(name) ? "web" : "offline");
            }
            
            com.google.gson.JsonObject playerJson = new com.google.gson.JsonObject();
            playerJson.addProperty("name", name);
            playerJson.addProperty("status", status);
            if ("game".equals(status)) {
                playerJson.addProperty("server", gameServers.getOrDefault(name, ""));
            }
            Integer count = offlineCounts.get(name.toLowerCase());
            if (count != null && count > 0) {
                playerJson.addProperty("offlineCount", count);
            }
            players.add(playerJson);
        }

        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("action", "player_status_list");
        json.add("players", players);
        NettyChannelMessageHelper.send(channel, json.toString());
    }

    /**
     * 处理 AQQBot (OneBot标准) 消息
     * @param jsonMessage JSON格式的消息
     */
    private void handleAQQBotMessage(String jsonMessage) {
        try {
            Config config = plugin.getConfig();
            InputAQQBot inputAQQBot = org.lintx.plugins.yinwuchat.Util.Gson.gson()
                .fromJson(jsonMessage, InputAQQBot.class);
            
            if (inputAQQBot == null) {
                return;
            }

            // 检查是否是群消息
            if ("message".equalsIgnoreCase(inputAQQBot.getPost_type()) 
                && "group".equalsIgnoreCase(inputAQQBot.getMessage_type())
                && "normal".equalsIgnoreCase(inputAQQBot.getSub_type())) {
                
                // 获取配置的群号（优先使用 AQQBot 配置）
                long targetGroupId;
                if (config.aqqBotConfig != null && config.aqqBotConfig.qqGroup > 0) {
                    targetGroupId = config.aqqBotConfig.qqGroup;
                } else if (config.coolQConfig != null && config.coolQConfig.coolQGroup > 0) {
                    targetGroupId = config.coolQConfig.coolQGroup;
                } else {
                    return; // 未配置群号
                }

                // 检查群号是否匹配
                if (inputAQQBot.getGroup_id() != targetGroupId) {
                    return;
                }

                // 检查是否需要特定的消息前缀
                String qqToGameStart = config.aqqBotConfig != null 
                    ? config.aqqBotConfig.qqToGameStart 
                    : (config.coolQConfig != null ? config.coolQConfig.coolqToGameStart : "");
                
                if (qqToGameStart != null && !qqToGameStart.isEmpty()) {
                    String message = inputAQQBot.getRaw_message();
                    if (message == null || !message.startsWith(qqToGameStart)) {
                        return;
                    }
                }

                // TODO: 处理QQ消息转发到游戏
                // MessageManage.getInstance().handleQQMessage(inputAQQBot);
            }
        } catch (Exception e) {
            plugin.getLogger().warn("[WebSocket] QQ消息解析失败: " + e.getMessage());
        }
    }

    private void sendSelfInfo(Channel channel, java.util.UUID uuid) {
        if (uuid == null) {
            return;
        }
        String name = plugin.getProxy().getPlayer(uuid)
            .map(com.velocitypowered.api.proxy.Player::getUsername)
            .orElse("");
        boolean isAdmin = false;
        
        if (!name.isEmpty()) {
            isAdmin = isAdmin(plugin, name);
        } else {
            name = org.lintx.plugins.yinwuchat.velocity.config.PlayerConfig
                .getInstance()
                .getTokenManager()
                .getName(uuid);
            if (name == null) {
                name = "";
            } else {
                isAdmin = isAdmin(plugin, name);
            }
        }
        
        if (name.isEmpty()) {
            return;
        }
        
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("action", "self_info");
        json.addProperty("player", name);
        json.addProperty("isAdmin", isAdmin);
        json.addProperty("vanished", PlayerConfig.getInstance().getSettings(name).vanished);
        NettyChannelMessageHelper.send(channel, json.toString());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            WebSocketServerProtocolHandler.HandshakeComplete complete = 
                (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            HttpHeaders headers = complete.requestHeaders();

            String qq = headers.get("X-Self-ID");
            String role = headers.get("X-Client-Role");
            String authorization = headers.get("Authorization");

            // 识别 AQQBot (OneBot标准) 连接
            // AQQBot 通常使用 X-Self-ID 和 X-Client-Role 头来标识
            // 支持 "Universal" (通用客户端) 和 "Event" (事件上报) 等角色
            if (qq != null && !qq.isEmpty() && ("Universal".equals(role) || "Event".equals(role))) {
                Config config = plugin.getConfig();
                
                // 优先使用 AQQBot 配置，向后兼容 CoolQ 配置
                String token = null;
                if (config.aqqBotConfig != null) {
                    token = config.aqqBotConfig.accessToken;
                } else if (config.coolQConfig != null) {
                    token = config.coolQConfig.coolQAccessToken;
                }
                
                // 验证访问令牌
                if (token != null && !token.isEmpty()) {
                    String expectedAuth = "Token " + token;
                    if (!expectedAuth.equals(authorization)) {
                        plugin.getLogger().warn("[WebSocket] QQ机器人连接被拒绝: Token无效");
                        ctx.close();
                        return;
                    }
                }
                
                // 注册 AQQBot 连接
                VelocityWsClientHelper.updateAQQBot(ctx.channel());
                plugin.getLogger().info("[WebSocket] QQ机器人已连接 (QQ: " + qq + ")");
            } else {
                // 普通Web客户端连接
                VelocityWsClientHelper.add(ctx.channel());
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        
        // 检查是否是 AQQBot 连接断开
        Channel aqqBotChannel = VelocityWsClientHelper.getAQQBot();
        if (aqqBotChannel != null && aqqBotChannel == ctx.channel()) {
            VelocityWsClientHelper.updateAQQBot(null);
            plugin.getLogger().info("[WebSocket] QQ机器人连接已断开");
        } else {
            VelocityWsClientHelper.remove(ctx.channel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        plugin.getLogger().warn("[WebSocket] 错误: " + cause.getMessage());
        ctx.close();
    }
}
