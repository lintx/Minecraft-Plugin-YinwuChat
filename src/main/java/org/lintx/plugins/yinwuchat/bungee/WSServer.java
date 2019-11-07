/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lintx.plugins.yinwuchat.bungee;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.lintx.plugins.yinwuchat.bungee.json.*;
import org.lintx.plugins.yinwuchat.bungee.util.*;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 *
 * @author jjcbw01
 */
public class WSServer extends WebSocketServer {
    private final YinwuChat plugin;

    public WSServer(int port,YinwuChat plugin) {
        super(new InetSocketAddress(port));
        this.plugin = plugin;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String qq = handshake.getFieldValue("X-Self-ID");
        String role = handshake.getFieldValue("X-Client-Role");
        String authorization = handshake.getFieldValue("Authorization");
        if (!qq.equals("") && role.equals("Universal")){
            String token = Config.getInstance().coolQAccessToken;
            if (!token.equals("")){
                token = "Token " + token;
                if (!token.equals(authorization)){
                    conn.close();
                    return;
                }
            }
            WsClientHelper.updateCoolQ(conn);
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        //Yinwuchat.getPlugin().getLogger().info("ws on close");
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            WsClientUtil util = WsClientHelper.get(conn);
//            if (util != null && util.getUuid() != null) {
//                YinwuChat.getWSServer().broadcast((new PlayerStatusJSON(util.getPlayerName(),PlayerStatusJSON.PlayerStatus.WEB_JOIN)).getWebStatusJSON());
//                plugin.getProxy().broadcast(ChatUtil.formatLeaveMessage(util.getUuid()));
//            }
            WsClientHelper.remove(conn);
            PlayerListJSON.sendWebPlayerList();
        });
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            BaseInputJSON object = BaseInputJSON.getObject(message);
            if (object instanceof InputCheckToken) {
                InputCheckToken o = (InputCheckToken)object;

                WsClientUtil clientUtil = new WsClientUtil(o.getToken());

                conn.send(o.getJSON());
                if (!o.getIsvaild()) {
                    conn.send(o.getTokenJSON());
                }
                else{
                    if (o.getIsbind()) {
                        PlayerListJSON.sendGamePlayerList();
                        PlayerListJSON.sendWebPlayerList();

                        clientUtil.setUUID(o.getUuid());
                        WsClientHelper.kickOtherWS(conn, o.getUuid());
                        String player_name = PlayerConfig.getConfig(o.getUuid()).name;
//                        YinwuChat.getWSServer().broadcast((new PlayerStatusJSON(player_name,PlayerStatusJSON.PlayerStatus.WEB_JOIN)).getWebStatusJSON());
//                        plugin.getProxy().broadcast(ChatUtil.formatJoinMessage(o.getUuid()));
                    }
                }
                WsClientHelper.add(conn, clientUtil);
            }
            else if (object instanceof InputMessage) {
                InputMessage o = (InputMessage)object;
                if (o.getMessage().equalsIgnoreCase("")) {
                    return;
                }

                WsClientUtil util = WsClientHelper.get(conn);
                if (util != null && util.getUuid() != null) {
                    if (!util.getLastDate().isEqual(LocalDateTime.MIN)){
                        Duration duration = Duration.between(util.getLastDate(), LocalDateTime.now());
                        if (duration.toMillis() < Config.getInstance().wsCooldown) {
                            conn.send(ServerMessageJSON.errorJSON("发送消息过快（当前设置为每条消息之间最少间隔"+(Config.getInstance().wsCooldown)+"毫秒）").getJSON());
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

                                MessageManage.getInstance().webSendPrivateMessage(conn,util,to_player_name,msg);
                            }
                            else{
                                conn.send(ServerMessageJSON.errorJSON("发送私聊消息的正确格式为/msg 玩家名 消息").getJSON());
                                conn.send(ServerMessageJSON.errorJSON("其他命令暂不支持").getJSON());
                            }
                        }
                        else{
                            conn.send(ServerMessageJSON.errorJSON("发送私聊消息的正确格式为/msg 玩家名 消息").getJSON());
                            conn.send(ServerMessageJSON.errorJSON("其他命令暂不支持").getJSON());
                        }
                        return;
                    }
                    MessageManage.getInstance().webBroadcastMessage(util.getUuid(),o.getMessage(),conn);
                }
            }
            else if (object instanceof CoolQInputJSON){
                CoolQInputJSON coolMessage = (CoolQInputJSON)object;
                if (coolMessage.getPost_type().equalsIgnoreCase("message")
                && coolMessage.getMessage_type().equalsIgnoreCase("group")
                && coolMessage.getSub_type().equalsIgnoreCase("normal")
                && coolMessage.getGroup_id() == Config.getInstance().coolQGroup){
                    MessageManage.getInstance().qqMessage(coolMessage);
                }
            }
        });

    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        plugin.getLogger().info("ws on error");
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        plugin.getLogger().info("ws on start");
    }
    
}