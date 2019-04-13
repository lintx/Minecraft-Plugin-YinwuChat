/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lintx.plugins.yinwuchat.bungee.json;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.lintx.plugins.yinwuchat.bungee.PlayerConfig;
import org.lintx.plugins.yinwuchat.bungee.WSServer;
import org.lintx.plugins.yinwuchat.bungee.YinwuChat;
import org.lintx.plugins.yinwuchat.bungee.util.WsClientHelper;
import org.lintx.plugins.yinwuchat.bungee.util.WsClientUtil;

import java.util.Collection;

/**
 *
 * @author jjcbw01
 */
public class PlayerListJSON {
    public static void sendGamePlayerList(){
        JsonArray jsonArray = new JsonArray();
        for (ServerInfo serverInfo:YinwuChat.getPlugin().getProxy().getServers().values()){
            for (ProxiedPlayer player : serverInfo.getPlayers()){
                PlayerConfig.Player playerConfig = PlayerConfig.getConfig(player);
                if (playerConfig.vanish){
                    continue;
                }
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("player_name", player.getDisplayName());
                String server_name = serverInfo.getName();
                jsonObject.addProperty("server_name", server_name);
                jsonArray.add(jsonObject);
            }
        }
        JsonObject resultJsonObject = new JsonObject();
        resultJsonObject.addProperty("action", "game_player_list");
        resultJsonObject.add("player_list", jsonArray);
        String json = new Gson().toJson(resultJsonObject);
        WSServer server = YinwuChat.getWSServer();
        if (server!=null) {
            server.broadcast(json);
        }
    }
    
    public static void sendWebPlayerList(){
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
        String json = new Gson().toJson(resultJsonObject);
        WSServer server = YinwuChat.getWSServer();
        if (server!=null) {
            server.broadcast(json);
        }
    }
}
