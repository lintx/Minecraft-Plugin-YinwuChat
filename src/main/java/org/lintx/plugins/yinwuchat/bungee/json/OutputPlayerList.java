/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lintx.plugins.yinwuchat.bungee.json;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.channel.Channel;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.lintx.plugins.yinwuchat.bungee.config.PlayerConfig;
import org.lintx.plugins.yinwuchat.bungee.YinwuChat;
import org.lintx.plugins.yinwuchat.bungee.httpserver.NettyChannelMessageHelper;
import org.lintx.plugins.yinwuchat.bungee.httpserver.NettyHttpServer;
import org.lintx.plugins.yinwuchat.bungee.httpserver.WsClientHelper;
import org.lintx.plugins.yinwuchat.bungee.httpserver.WsClientUtil;

/**
 *
 * @author LinTx
 */
public class OutputPlayerList {
    private static String getGamePlayerList(){
        JsonArray jsonArray = new JsonArray();
        for (ServerInfo serverInfo:YinwuChat.getPlugin().getProxy().getServers().values()){
            for (ProxiedPlayer player : serverInfo.getPlayers()){
                PlayerConfig.Player playerConfig = PlayerConfig.getConfig(player);
                if (playerConfig.vanish){
                    continue;
                }
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("player_name", player.getName());
                String server_name = serverInfo.getName();
                jsonObject.addProperty("server_name", server_name);
                jsonArray.add(jsonObject);
            }
        }
        JsonObject resultJsonObject = new JsonObject();
        resultJsonObject.addProperty("action", "game_player_list");
        resultJsonObject.add("player_list", jsonArray);
        return new Gson().toJson(resultJsonObject);
    }

    public static void sendGamePlayerList(Channel channel){
        NettyHttpServer server = YinwuChat.getWSServer();
        if (server!=null) {
            NettyChannelMessageHelper.send(channel,getGamePlayerList());
        }
    }

    public static void sendGamePlayerList(){
        NettyHttpServer server = YinwuChat.getWSServer();
        if (server!=null) {
            NettyChannelMessageHelper.broadcast(getGamePlayerList());
        }
    }

    private static String getWebPlayerList(){
        JsonArray jsonArray = new JsonArray();
        for (WsClientUtil util : WsClientHelper.utils()) {
            if (util.getUuid()==null){
                continue;
            }
            PlayerConfig.Player config = PlayerConfig.getConfig(util.getUuid());
            if (config.name==null || config.name.equals("")){
                continue;
            }
            jsonArray.add(config.name);
        }
        JsonObject resultJsonObject = new JsonObject();
        resultJsonObject.addProperty("action", "web_player_list");
        resultJsonObject.add("player_list", jsonArray);
        return new Gson().toJson(resultJsonObject);
    }
    
    public static void sendWebPlayerList(Channel channel){
        NettyHttpServer server = YinwuChat.getWSServer();
        if (server!=null) {
            NettyChannelMessageHelper.send(channel,getWebPlayerList());
        }
    }

    public static void sendWebPlayerList(){
        NettyHttpServer server = YinwuChat.getWSServer();
        if (server!=null) {
            NettyChannelMessageHelper.broadcast(getWebPlayerList());
        }
    }
}
