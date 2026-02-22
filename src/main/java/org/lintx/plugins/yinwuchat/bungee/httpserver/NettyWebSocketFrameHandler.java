package org.lintx.plugins.yinwuchat.bungee.httpserver;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.lintx.plugins.yinwuchat.Const;
import com.google.gson.JsonObject;
import org.lintx.plugins.yinwuchat.Util.MessageUtil;
import org.lintx.plugins.yinwuchat.bungee.config.Config;
import org.lintx.plugins.yinwuchat.bungee.MessageManage;
import org.lintx.plugins.yinwuchat.bungee.YinwuChat;
import org.lintx.plugins.yinwuchat.bungee.json.*;
import org.lintx.plugins.yinwuchat.bungee.config.PlayerConfig;
import org.lintx.plugins.yinwuchat.bungee.manage.MuteManage;
import org.lintx.plugins.yinwuchat.common.auth.AuthService;
import org.lintx.plugins.yinwuchat.common.auth.AuthUserStore;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class NettyWebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private final YinwuChat plugin;

    NettyWebSocketFrameHandler(YinwuChat plugin){
        this.plugin = plugin;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            String request = ((TextWebSocketFrame) frame).text();

            Channel channel = ctx.channel();
            plugin.getProxy().getScheduler().runAsync(plugin, () -> {
                InputBase object = InputBase.getObject(request);
                if (object instanceof InputCheckToken) {
                    InputCheckToken o = (InputCheckToken)object;

                    WsClientUtil clientUtil = new WsClientUtil(o.getToken());
                    WsClientHelper.add(channel, clientUtil);

                    NettyChannelMessageHelper.send(channel,o.getJSON());
                    if (!o.getIsvaild()) {
                        NettyChannelMessageHelper.send(channel,o.getTokenJSON());
                    }
                    else{
                        if (o.getIsbind()) {
                            clientUtil.setUUID(o.getUuid());
                            // 不再踢掉其他同账号连接，允许多设备同时在线
                            // WsClientHelper.kickOtherWS(channel, o.getUuid());

                            if (enforceAccountBan(channel, clientUtil)) {
                                return;
                            }

                            sendSelfInfo(channel, o.getUuid());
                            OutputPlayerList.sendGamePlayerList(channel);
                            OutputPlayerList.sendWebPlayerList();
//                            String player_name = PlayerConfig.getConfig(o.getUuid()).name;
//                        YinwuChat.getWSServer().broadcast((new PlayerStatusJSON(player_name,PlayerStatusJSON.PlayerStatus.WEB_JOIN)).getWebStatusJSON());
//                        plugin.getProxy().broadcast(ChatUtil.formatJoinMessage(o.getUuid()));
                        }
                    }
                }
                else if (object instanceof InputMessage) {
                    handleInputMessage(channel, (InputMessage) object);
                }
                else if (object instanceof InputCommand) {
                    InputCommand o = (InputCommand)object;
                    handleCommandAction(channel, o);
                }
                else if (object instanceof InputBindAccount) {
                    handleBindAccount(channel, (InputBindAccount) object);
                }
                else if (object instanceof InputCoolQ){
                    InputCoolQ coolMessage = (InputCoolQ)object;
                    if (coolMessage.getPost_type().equalsIgnoreCase("message")
                            && coolMessage.getMessage_type().equalsIgnoreCase("group")
                            && coolMessage.getSub_type().equalsIgnoreCase("normal")
                            && coolMessage.getGroup_id() == Config.getInstance().coolQConfig.coolQGroup){
                        if (!"".equals(Config.getInstance().coolQConfig.coolqToGameStart)){
                            if (!coolMessage.getMessage().startsWith(Config.getInstance().coolQConfig.coolqToGameStart)){
                                return;
                            }
                        }
                        MessageManage.getInstance().handleQQMessage(coolMessage);
                    }
                }
            });
        } else {
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        //断开连接
        if (WsClientHelper.getCoolQ() == ctx.channel()){
            WsClientHelper.updateCoolQ(null);
        }
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            WsClientUtil util = WsClientHelper.get(ctx.channel());
            if (util != null) {
                WsClientHelper.remove(ctx.channel());
                OutputPlayerList.sendWebPlayerList();
            }
        });
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete){
            WebSocketServerProtocolHandler.HandshakeComplete complete = (WebSocketServerProtocolHandler.HandshakeComplete)evt;
            HttpHeaders headers = complete.requestHeaders();

            String qq = headers.get("X-Self-ID");
            String role = headers.get("X-Client-Role");
            String authorization = headers.get("Authorization");
            if (!"".equals(qq) && "Universal".equals(role)){
                String token = Config.getInstance().coolQConfig.coolQAccessToken;
                if (!token.equals("")){
                    token = "Token " + token;
                    if (!token.equals(authorization)){
                        ctx.close();
                        return;
                    }
                }
                WsClientHelper.updateCoolQ(ctx.channel());
            }
            else {
                OutputPlayerList.sendPlayerStatusList(ctx.channel());
            }
        }
    }

    private void sendSelfInfo(Channel channel, java.util.UUID uuid) {
        // ... (existing code)
    }

    private void handleCommandAction(Channel channel, InputCommand inputCommand) {
        WsClientUtil util = WsClientHelper.get(channel);
        if (util == null || util.getUuid() == null) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("请先绑定账号").getJSON());
            return;
        }

        String fullCommand = inputCommand.getCommand();
        if (fullCommand.isEmpty()) return;

        handleCommand(channel, util, "/" + fullCommand);
    }

    private void handleCommand(Channel channel, WsClientUtil util, String message) {
        String[] commandArgs = message.replaceFirst("^/", "").split("\\s+");
        String label = commandArgs[0].toLowerCase();
        if (label.equals("chatban")) {
            commandArgs[0] = "ban";
            label = "ban";
        }

        if (enforceAccountBan(channel, util)) {
            return;
        }

        if (label.equals("msg") || label.equals("tell") || label.equals("whisper") || label.equals("w") || label.equals("t")) {
            if (commandArgs.length >= 3) {
                String toPlayerName = commandArgs[1];
                List<String> tmpList = new ArrayList<>(Arrays.asList(commandArgs).subList(2, commandArgs.length));
                String msg = String.join(" ", tmpList);
                MessageManage.getInstance().handleWebPrivateMessage(channel, util, toPlayerName, msg);
            }
        } else if (label.equals("yinwuchat") || label.equals("ignore") || label.equals("noat") || label.equals("vanish") || label.equals("ban")) {
            // 对于插件自身指令，无论玩家是否在线都允许执行
            String[] args;
            if (label.equals("yinwuchat")) {
                args = commandArgs.length > 1 ? Arrays.copyOfRange(commandArgs, 1, commandArgs.length) : new String[]{"ws"};
            } else {
                args = commandArgs;
            }
            handleWebPlayerCommand(channel, util, args);
        } else {
            // 其他指令必须在游戏内在线才能执行
            ProxiedPlayer player = plugin.getProxy().getPlayer(util.getUuid());
            if (player != null) {
                plugin.getProxy().getPluginManager().dispatchCommand(player, message.substring(1));
            } else {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 你必须在游戏内在线才能执行此指令").getJSON());
            }
        }
    }

    private void handleBindAccount(Channel channel, InputBindAccount input) {
        WsClientUtil util = WsClientHelper.get(channel);
        if (util == null || util.getUuid() == null) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("请先绑定账号").getJSON());
            return;
        }
        String account = input.getAccount();
        if (account == null || account.trim().isEmpty()) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("Web 账户名不能为空").getJSON());
            return;
        }
        PlayerConfig.Player playerConfig = PlayerConfig.getConfig(util.getUuid());
        if (playerConfig == null || playerConfig.name == null || playerConfig.name.isEmpty()) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("无法获取玩家名称").getJSON());
            return;
        }
        org.lintx.plugins.yinwuchat.common.auth.AuthService authService =
            org.lintx.plugins.yinwuchat.common.auth.AuthService.getInstance(plugin.getDataFolder());
        if (!authService.accountExists(account)) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("Web 账户不存在").getJSON());
            return;
        }
        // 检查账户是否已绑定该玩家，如果已绑定则不发送确认消息
        String existingBound = authService.getBoundPlayerName(account);
        boolean alreadyBound = playerConfig.name.equalsIgnoreCase(existingBound);
        
        authService.bindAccountPlayerName(account, playerConfig.name);
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
     * 处理 Web 玩家的特殊命令（无需在游戏内在线）
     */
    private void handleWebPlayerCommand(Channel channel, WsClientUtil util, String[] args) {
        UUID uuid = util.getUuid();
        PlayerConfig.Player playerConfig = PlayerConfig.getConfig(uuid);
        String playerName = playerConfig.name;
        if (playerName == null || playerName.isEmpty()) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("无法获取玩家名称").getJSON());
            return;
        }

        String cmd = args[0].toLowerCase();
        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);
        boolean isAdmin = (player != null) ? Config.getInstance().isAdmin(player) : Config.getInstance().isAdmin(playerName);

        if (cmd.equals("ws")) {
            if (!isAdmin && (player == null || !player.hasPermission(Const.PERMISSION_WS))) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 权限不足").getJSON());
                return;
            }
            String host = "服务器IP或域名";
            try {
                java.net.InetSocketAddress address = (java.net.InetSocketAddress) plugin.getProxy().getConfig().getListeners().iterator().next().getHost();
                if (address != null && address.getHostString() != null) {
                    host = address.getHostString();
                }
            } catch (Exception ignored) {
            }
            if ("0.0.0.0".equals(host) || "::".equals(host)) {
                host = "服务器IP或域名";
            }
            int port = Config.getInstance().wsport;
            String wsUrl = "ws://" + host + ":" + port + "/ws";
            NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("WebSocket 地址: " + wsUrl + "\n如果网页使用 HTTPS，请将 ws:// 改为 wss://").getJSON());
        } else if (cmd.equals("ignore")) {
            if (!isAdmin && (player == null || !player.hasPermission(Const.PERMISSION_IGNORE))) {
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
            ProxiedPlayer target = plugin.getProxy().getPlayer(targetName);
            if (target != null) {
                if (playerConfig.ignore(target.getUniqueId())) {
                    playerConfig.save();
                    NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("✓ 已忽略玩家 " + targetName).getJSON());
                } else {
                    playerConfig.save();
                    NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("✓ 已取消忽略玩家 " + targetName).getJSON());
                }
            } else {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 该玩家目前不在线，Bungee 版暂不支持离线忽略").getJSON());
            }
        } else if (cmd.equals("noat") || cmd.equals("muteat")) {
            String perm = cmd.equals("noat") ? Const.PERMISSION_NOAT : Const.PERMISSION_MUTEAT;
            if (!isAdmin && (player == null || !player.hasPermission(perm))) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 权限不足").getJSON());
                return;
            }
            playerConfig.muteAt = !playerConfig.muteAt;
            playerConfig.save();
            if (playerConfig.muteAt) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("✓ 已开启免打扰模式").getJSON());
            } else {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("✓ 已关闭免打扰模式").getJSON());
            }
        } else if (cmd.equals("vanish")) {
            if (!isAdmin) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 权限不足（你必须在游戏中且拥有权限，或者使用 Bungee 权限系统）").getJSON());
                return;
            }
            playerConfig.vanish = !playerConfig.vanish;
            playerConfig.save();
            if (playerConfig.vanish) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("✓ 已开启隐身模式").getJSON());
            } else {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("✓ 已关闭隐身模式").getJSON());
            }
            // 广播更新后的玩家列表
            OutputPlayerList.sendPlayerStatusList();
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
        } else if (cmd.equals("webbind")) {
            if (!isAdmin) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 权限不足").getJSON());
                return;
            }
            handleWebBindCommand(channel, args);
        } else if (cmd.equals("mute") || cmd.equals("unmute") || cmd.equals("muteinfo")) {
            if (!isAdmin) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 权限不足").getJSON());
                return;
            }
            
            org.lintx.plugins.yinwuchat.bungee.manage.MuteManage manager = org.lintx.plugins.yinwuchat.bungee.manage.MuteManage.getInstance();
            if (cmd.equals("mute")) {
                if (args.length < 2) {
                    NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("用法: /mute <玩家名> [时间(分钟)] [原因]").getJSON());
                    return;
                }
                String target = args[1];
                long duration = 0;
                if (args.length >= 3) {
                    try {
                        duration = Long.parseLong(args[2]) * 60;
                    } catch (NumberFormatException ignored) {}
                }
                String reason = args.length >= 4 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "";
                if (manager.mutePlayer(target, duration, playerName, reason)) {
                    NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("✓ 已禁言玩家 " + target + (duration > 0 ? " " + (duration/60) + " 分钟" : " (永久)")).getJSON());
                } else {
                    NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 禁言失败，玩家可能不在线").getJSON());
                }
            } else if (cmd.equals("unmute")) {
                if (args.length < 2) {
                    NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("用法: /unmute <玩家名>").getJSON());
                    return;
                }
                String target = args[1];
                if (manager.unmutePlayer(target, playerName)) {
                    NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("✓ 已解除玩家 " + target + " 的禁言").getJSON());
                } else {
                    NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 解除禁言失败，玩家可能不在线").getJSON());
                }
            } else if (cmd.equals("muteinfo")) {
                if (args.length < 2) {
                    NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("用法: /muteinfo <玩家名>").getJSON());
                    return;
                }
                String target = args[1];
                ProxiedPlayer targetPlayer = plugin.getProxy().getPlayer(target);
                if (targetPlayer != null) {
                    PlayerConfig.Player targetConfig = PlayerConfig.getConfig(targetPlayer);
                    if (!targetConfig.isMuted()) {
                        NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("玩家 " + target + " 当前未被禁言").getJSON());
                    } else {
                        long rem = targetConfig.getRemainingMuteTime();
                        String timeStr = rem == -1 ? "永久" : (rem / 60) + " 分钟";
                        NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("玩家 " + target + " 处于禁言状态\n剩余时间: " + timeStr + "\n原因: " + targetConfig.muteReason).getJSON());
                    }
                } else {
                    NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 无法获取该玩家信息").getJSON());
                }
            }
        } else {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("可用指令: /yinwuchat [ws|ignore|muteat|vanish|chatban|webbind|atalladmin|mute|unmute|muteinfo]").getJSON());
        }
    }

    private void handleWebAtAllAdminCommand(Channel channel, WsClientUtil util, String playerName, boolean isAdmin, String[] args) {
        ProxiedPlayer player = util != null && util.getUuid() != null ? plugin.getProxy().getPlayer(util.getUuid()) : null;
        boolean hasPermission = (player != null && player.hasPermission(Const.PERMISSION_AT_ALL_ADMIN)) || isAdmin;
        if (!hasPermission) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 权限不足").getJSON());
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
            boolean canReset = (player != null && player.hasPermission(Const.PERMISSION_AT_ALL_ADMIN_RESET)) || isAdmin;
            if (!canReset) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 权限不足").getJSON());
                return;
            }
            if (args.length < 3) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("用法: /yinwuchat atalladmin confirm <玩家名>").getJSON());
                return;
            }
            String targetName = args[2];
            PlayerConfig.Player targetConfig = PlayerConfig.getPlayerConfigByName(targetName);
            if (targetConfig != null) {
                targetConfig.lastAtAllAdmin = 0;
                targetConfig.save();
                NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("✓ 已重置玩家 " + targetName + " 的突发事件提醒冷却时间").getJSON());
            } else {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 未找到玩家 " + targetName).getJSON());
            }
            return;
        }

        PlayerConfig.Player selfConfig = util != null && util.getUuid() != null ? PlayerConfig.getConfig(util.getUuid()) : null;
        long last = selfConfig != null ? selfConfig.lastAtAllAdmin : 0L;
        long now = System.currentTimeMillis();
        if (last > 0 && now - last < 24 * 60 * 60 * 1000L) {
            long remaining = 24 * 60 * 60 * 1000L - (now - last);
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

    private void handleWebAtAllAdminExecute(Channel channel, WsClientUtil util, String playerName, boolean isAdmin) {
        ProxiedPlayer player = util != null && util.getUuid() != null ? plugin.getProxy().getPlayer(util.getUuid()) : null;
        boolean hasPermission = (player != null && player.hasPermission(Const.PERMISSION_AT_ALL_ADMIN)) || isAdmin;
        if (!hasPermission) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 权限不足").getJSON());
            return;
        }
        PlayerConfig.Player selfConfig = util != null && util.getUuid() != null ? PlayerConfig.getConfig(util.getUuid()) : null;
        long last = selfConfig != null ? selfConfig.lastAtAllAdmin : 0L;
        long now = System.currentTimeMillis();
        if (last > 0 && now - last < 24 * 60 * 60 * 1000L) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("✗ 冷却中").getJSON());
            return;
        }
        if (selfConfig != null) {
            selfConfig.lastAtAllAdmin = now;
            selfConfig.save();
        }

        String senderName = playerName;
        if (senderName == null || senderName.isEmpty()) {
            senderName = player != null ? player.getName() : "Web用户";
        }
        net.md_5.bungee.api.chat.TextComponent gameAlert = new net.md_5.bungee.api.chat.TextComponent("警告！" + senderName + " 报告服务器存在突发事件，请立即查看");
        gameAlert.setColor(ChatColor.RED);
        gameAlert.setBold(true);

        com.google.gson.JsonObject webAlert = new com.google.gson.JsonObject();
        webAlert.addProperty("action", "admin_alert");
        webAlert.addProperty("message", "存在需要立即处理的服务器风险");
        webAlert.addProperty("player", senderName);
        String webAlertJson = webAlert.toString();

        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            if (Config.getInstance().isAdmin(p)) {
                p.sendMessage(gameAlert);
            }
        }
        for (Channel ch : WsClientHelper.channels()) {
            WsClientUtil u = WsClientHelper.get(ch);
            if (u != null && u.getUuid() != null) {
                PlayerConfig.Player pc = PlayerConfig.getConfig(u.getUuid());
                if (pc.name != null && Config.getInstance().isAdmin(pc.name)) {
                    NettyChannelMessageHelper.send(ch, webAlertJson);
                }
            }
        }
        NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("✓ 突发事件报告已发送给所有管理员").getJSON());
    }

    private void handleWebBindCommand(Channel channel, String[] args) {
        if (args.length < 3) {
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("用法: /yinwuchat webbind <query|unbind> <账号名/玩家名>").getJSON());
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        String target = args[2];
        AuthService authService = AuthService.getInstance(plugin.getDataFolder());
        if ("query".equals(action)) {
            if (authService.accountExists(target)) {
                String bound = authService.getBoundPlayerName(target);
                String msg = bound == null || bound.isEmpty()
                    ? "账号 " + target + " 未绑定玩家名"
                    : "账号 " + target + " 绑定玩家名: " + bound;
                NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON(msg).getJSON());
                return;
            }
            String account = authService.resolveAccountByPlayerName(target);
            if (account == null || account.isEmpty()) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("未找到对应的绑定信息").getJSON());
                return;
            }
            NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("玩家 " + target + " 绑定账号: " + account).getJSON());
            return;
        }
        if ("unbind".equals(action)) {
            if (authService.accountExists(target)) {
                boolean ok = authService.unbindAccountPlayerName(target);
                NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON(ok
                    ? "已解绑账号 " + target + " 的玩家名"
                    : "解绑失败").getJSON());
                return;
            }
            String account = authService.unbindAccountByPlayerName(target);
            if (account == null || account.isEmpty()) {
                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("未找到对应的绑定信息").getJSON());
                return;
            }
            NettyChannelMessageHelper.send(channel, OutputServerMessage.infoJSON("已解绑玩家 " + target + " 的账号 " + account).getJSON());
            return;
        }
        NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("用法: /yinwuchat webbind <query|unbind> <账号名/玩家名>").getJSON());
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
        AuthService authService = AuthService.getInstance(plugin.getDataFolder());
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
        notifyBanToAdmins(accountName, playerName, durationText, reason, operatorName, channel);
            kickWebPlayerByName(playerName, accountName, durationText, reason);
    }

    private void notifyBanToAdmins(String accountName, String playerName, String durationText, String reason, String operator, Channel excludeChannel) {
        String message = "账号 " + accountName + " 已被封禁"
            + (playerName != null && !playerName.isEmpty() ? "（玩家: " + playerName + "）" : "")
            + "，时长: " + (durationText == null || durationText.isEmpty() ? "永久" : durationText)
            + (reason == null || reason.isEmpty() ? "" : "，理由: " + reason);

        // 游戏内管理员
        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            if (Config.getInstance().isAdmin(p)) {
                p.sendMessage(MessageUtil.newTextComponent(ChatColor.YELLOW + message));
            }
        }

        // Web 端管理员
        if (Config.getInstance().openwsserver && YinwuChat.getWSServer() != null) {
            String json = OutputServerMessage.infoJSON(message).getJSON();
            for (Channel ch : WsClientHelper.channels()) {
                WsClientUtil util = WsClientHelper.get(ch);
                if (util == null) continue;

                boolean isAdmin = false;
                String name = util.getAccount();
                if (util.getUuid() != null) {
                    ProxiedPlayer pp = plugin.getProxy().getPlayer(util.getUuid());
                    if (pp != null) {
                        isAdmin = Config.getInstance().isAdmin(pp);
                    }
                    if (!isAdmin) {
                    PlayerConfig.Player pc = PlayerConfig.getConfig(util.getUuid());
                        if (pc != null && pc.name != null) {
                            if (name == null || name.isEmpty()) name = pc.name;
                            isAdmin = Config.getInstance().isAdmin(pc.name);
                        }
                    }
                }
                if (!isAdmin && name != null && !name.isEmpty()) {
                    isAdmin = Config.getInstance().isAdmin(name);
                }

                if (isAdmin) {
                    NettyChannelMessageHelper.send(ch, json);
                }
            }
        }
    }

    private void kickWebPlayerByName(String playerName, String accountName, String durationText, String reason) {
        if (!Config.getInstance().openwsserver || YinwuChat.getWSServer() == null) return;
        JsonObject json = new JsonObject();
        json.addProperty("action", "ban_kick");
        json.addProperty("player", playerName == null ? "" : playerName);
        json.addProperty("account", accountName == null ? "" : accountName);
        json.addProperty("durationText", durationText == null || durationText.isEmpty() ? "永久" : durationText);
        json.addProperty("reason", reason == null ? "" : reason);
        String payload = json.toString();

        for (Channel ch : WsClientHelper.channels()) {
            WsClientUtil util = WsClientHelper.get(ch);
            if (util == null) continue;
            boolean matched = false;
            if (accountName != null && !accountName.isEmpty()) {
                String utilAccount = util.getAccount();
                if (utilAccount != null && utilAccount.equalsIgnoreCase(accountName)) {
                    matched = true;
                }
            }
            if (!matched && util.getUuid() != null && playerName != null && !playerName.isEmpty()) {
                PlayerConfig.Player pc = PlayerConfig.getConfig(util.getUuid());
                if (pc != null && pc.name != null && pc.name.equalsIgnoreCase(playerName)) {
                    matched = true;
                }
            }
            if (matched) {
                ch.writeAndFlush(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame(payload)).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    private void handleInputMessage(Channel channel, InputMessage o) {
        if (o.getMessage().equalsIgnoreCase("")) {
            return;
        }

        WsClientUtil util = WsClientHelper.get(channel);
        if (util != null && util.getUuid() != null) {
            if (enforceAccountBan(channel, util)) {
                return;
            }
            if (!util.getLastDate().isEqual(LocalDateTime.MIN)) {
                Duration duration = Duration.between(util.getLastDate(), LocalDateTime.now());
                if (duration.toMillis() < Config.getInstance().wsCooldown) {
                    NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("发送消息过快（当前设置为每条消息之间最少间隔" + (Config.getInstance().wsCooldown) + "毫秒）").getJSON());
                    return;
                }
            }
            util.updateLastDate();

            if (o.getMessage().startsWith("/")) {
                handleCommand(channel, util, o.getMessage());
                return;
            }
            MessageManage.getInstance().handleWebPublicMessage(util.getUuid(), o.getMessage(), channel);
        }
    }

    private boolean enforceAccountBan(Channel channel, WsClientUtil util) {
        if (util == null) {
            return false;
        }
        String accountName = util.getAccount();
        String playerName = "";
        if (util.getUuid() != null) {
            PlayerConfig.Player pc = PlayerConfig.getConfig(util.getUuid());
            if (pc != null && pc.name != null) {
                playerName = pc.name;
            }
        }
        if (accountName == null || accountName.isEmpty()) {
            if (playerName == null || playerName.isEmpty()) {
                return false;
            }
            AuthService authService = AuthService.getInstance(plugin.getDataFolder());
            String mapped = authService.resolveAccountByPlayerName(playerName);
            if (mapped == null || mapped.isEmpty()) {
                return false;
            }
            accountName = mapped;
        }
        AuthService authService = AuthService.getInstance(plugin.getDataFolder());
        AuthUserStore.BanInfo banInfo = authService.getBanInfo(accountName);
        if (!banInfo.banned) {
            return false;
        }
        String durationText = AuthService.formatDuration(banInfo.permanent ? -1L : banInfo.remainingMillis);
        JsonObject json = new JsonObject();
        json.addProperty("action", "ban_kick");
        json.addProperty("player", playerName == null ? "" : playerName);
        json.addProperty("account", accountName == null ? "" : accountName);
        json.addProperty("durationText", durationText == null || durationText.isEmpty() ? "永久" : durationText);
        json.addProperty("reason", banInfo.reason == null ? "" : banInfo.reason);
        json.addProperty("by", banInfo.bannedBy == null ? "" : banInfo.bannedBy);
        channel.writeAndFlush(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame(json.toString())).addListener(ChannelFutureListener.CLOSE);
        return true;
    }
}
