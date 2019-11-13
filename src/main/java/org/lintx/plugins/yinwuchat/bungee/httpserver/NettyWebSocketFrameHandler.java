package org.lintx.plugins.yinwuchat.bungee.httpserver;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.lintx.plugins.yinwuchat.bungee.config.Config;
import org.lintx.plugins.yinwuchat.bungee.MessageManage;
import org.lintx.plugins.yinwuchat.bungee.YinwuChat;
import org.lintx.plugins.yinwuchat.bungee.json.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                    NettyChannelMessageHelper.send(channel,o.getJSON());
                    if (!o.getIsvaild()) {
                        NettyChannelMessageHelper.send(channel,o.getTokenJSON());
                    }
                    else{
                        if (o.getIsbind()) {
                            OutputPlayerList.sendGamePlayerList();
                            OutputPlayerList.sendWebPlayerList();

                            clientUtil.setUUID(o.getUuid());
                            WsClientHelper.kickOtherWS(channel, o.getUuid());
//                            String player_name = PlayerConfig.getConfig(o.getUuid()).name;
//                        YinwuChat.getWSServer().broadcast((new PlayerStatusJSON(player_name,PlayerStatusJSON.PlayerStatus.WEB_JOIN)).getWebStatusJSON());
//                        plugin.getProxy().broadcast(ChatUtil.formatJoinMessage(o.getUuid()));
                        }
                    }
                    WsClientHelper.add(channel, clientUtil);
                }
                else if (object instanceof InputMessage) {
                    InputMessage o = (InputMessage)object;
                    if (o.getMessage().equalsIgnoreCase("")) {
                        return;
                    }

                    WsClientUtil util = WsClientHelper.get(channel);
                    if (util != null && util.getUuid() != null) {
                        if (!util.getLastDate().isEqual(LocalDateTime.MIN)){
                            Duration duration = Duration.between(util.getLastDate(), LocalDateTime.now());
                            if (duration.toMillis() < Config.getInstance().wsCooldown) {
                                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("发送消息过快（当前设置为每条消息之间最少间隔"+(Config.getInstance().wsCooldown)+"毫秒）").getJSON());
                                return;
                            }
                        }
                        util.updateLastDate();

                        if (o.getMessage().startsWith("/")) {
                            String[] command = o.getMessage().replaceFirst("^/", "").split("\\s");
                            if (command.length>=3) {
                                if (command[0].equalsIgnoreCase("msg")) {
                                    String to_player_name = command[1];
                                    List<String> tmpList = new ArrayList<>(Arrays.asList(command).subList(2, command.length));
                                    String msg = String.join(" ", tmpList);

                                    MessageManage.getInstance().handleWebPrivateMessage(channel,util,to_player_name,msg);
                                }
                                else{
                                    NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("发送私聊消息的正确格式为/msg 玩家名 消息").getJSON());
                                    NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("其他命令暂不支持").getJSON());
                                }
                            }
                            else{
                                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("发送私聊消息的正确格式为/msg 玩家名 消息").getJSON());
                                NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON("其他命令暂不支持").getJSON());
                            }
                            return;
                        }
                        MessageManage.getInstance().handleWebPublicMessage(util.getUuid(),o.getMessage(),channel);
                    }
                }
                else if (object instanceof InputCoolQ){
                    InputCoolQ coolMessage = (InputCoolQ)object;
                    if (coolMessage.getPost_type().equalsIgnoreCase("message")
                            && coolMessage.getMessage_type().equalsIgnoreCase("group")
                            && coolMessage.getSub_type().equalsIgnoreCase("normal")
                            && coolMessage.getGroup_id() == Config.getInstance().coolQGroup){
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
//                YinwuChat.getWSServer().broadcast((new PlayerStatusJSON(util.getPlayerName(),PlayerStatusJSON.PlayerStatus.WEB_JOIN)).getWebStatusJSON());
//                plugin.getProxy().broadcast(ChatUtil.formatLeaveMessage(util.getUuid()));
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
                String token = Config.getInstance().coolQAccessToken;
                if (!token.equals("")){
                    token = "Token " + token;
                    if (!token.equals(authorization)){
                        ctx.close();
                        return;
                    }
                }
                WsClientHelper.updateCoolQ(ctx.channel());
            }
        }
    }
}
