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

    private static boolean isAdmin(String playerName) {
        if (playerName == null) return false;
        net.md_5.bungee.api.connection.ProxiedPlayer player = YinwuChat.getPlugin().getProxy().getPlayer(playerName);
        if (player != null) {
            return org.lintx.plugins.yinwuchat.bungee.config.Config.getInstance().isAdmin(player);
        }
        return org.lintx.plugins.yinwuchat.bungee.config.Config.getInstance().isAdmin(playerName);
    }

    private static String getPlayerStatusList(String viewerName){
        JsonArray jsonArray = new JsonArray();
        java.util.Map<String, String> gameServers = new java.util.HashMap<>();
        java.util.Set<String> gameNames = new java.util.HashSet<>();
        java.util.Set<String> webNames = new java.util.HashSet<>();
        java.util.Map<String, Integer> offlineCounts = new java.util.HashMap<>();
        boolean isAdmin = isAdmin(viewerName);

        for (ServerInfo serverInfo:YinwuChat.getPlugin().getProxy().getServers().values()){
            for (ProxiedPlayer player : serverInfo.getPlayers()){
                PlayerConfig.Player playerConfig = PlayerConfig.getConfig(player);
                // 如果玩家隐身且查看者不是管理员，则该玩家不计入游戏在线
                if (playerConfig.vanish && !isAdmin) {
                    continue;
                }
                String name = player.getName();
                gameNames.add(name);
                gameServers.put(name, serverInfo.getName());
            }
        }

        for (WsClientUtil util : WsClientHelper.utils()) {
            if (util.getUuid()==null){
                continue;
            }
            PlayerConfig.Player config = PlayerConfig.getConfig(util.getUuid());
            if (config.name==null || config.name.equals("")){
                continue;
            }
            // 如果玩家隐身且查看者不是管理员，则该玩家不计入 Web 在线
            if (config.vanish && !isAdmin){
                continue;
            }
            webNames.add(config.name);
        }

        java.util.Set<String> allNames = new java.util.HashSet<>();
        for (java.util.UUID uuid : PlayerConfig.getTokens().getAllUuids()) {
            PlayerConfig.Player playerConfig = PlayerConfig.getConfig(uuid);
            if (playerConfig.name != null && !playerConfig.name.isEmpty()) {
                allNames.add(playerConfig.name);
            }
        }
        allNames.addAll(gameNames);
        allNames.addAll(webNames);

        java.util.List<String> sorted = new java.util.ArrayList<>(allNames);
        java.util.Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);

        if (viewerName != null && !viewerName.isEmpty()) {
            org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore store =
                new org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore(YinwuChat.getPlugin().getDataFolder());
            offlineCounts = store.getCounts(viewerName);
        }

        for (String name : sorted) {
            if (name == null || name.isEmpty()) continue;
            
            PlayerConfig.Player playerConfig = PlayerConfig.getPlayerConfigByName(name);
            boolean isVanished = playerConfig != null && playerConfig.vanish;

            // 状态逻辑：如果玩家隐身且查看者不是管理员，强制显示为 offline
            String status;
            if (isVanished && !isAdmin) {
                status = "offline";
            } else {
                status = gameNames.contains(name) ? "game" : (webNames.contains(name) ? "web" : "offline");
            }

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("name", name);
            jsonObject.addProperty("status", status);
            if ("game".equals(status)) {
                jsonObject.addProperty("server", gameServers.getOrDefault(name, ""));
            }
            Integer count = offlineCounts.get(name.toLowerCase());
            if (count != null && count > 0) {
                jsonObject.addProperty("offlineCount", count);
            }
            jsonArray.add(jsonObject);
        }
        JsonObject resultJsonObject = new JsonObject();
        resultJsonObject.addProperty("action", "player_status_list");
        resultJsonObject.add("players", jsonArray);
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

    public static void sendPlayerStatusList(Channel channel){
        NettyHttpServer server = YinwuChat.getWSServer();
        if (server!=null) {
            String viewerName = null;
            WsClientUtil util = WsClientHelper.get(channel);
            if (util != null && util.getUuid() != null) {
                PlayerConfig.Player config = PlayerConfig.getConfig(util.getUuid());
                viewerName = config.name;
            }
            NettyChannelMessageHelper.send(channel,getPlayerStatusList(viewerName));
        }
    }

    public static void sendPlayerStatusList(){
        NettyHttpServer server = YinwuChat.getWSServer();
        if (server!=null) {
            for (io.netty.channel.Channel channel : WsClientHelper.channels()) {
                sendPlayerStatusList(channel);
            }
        }
    }
}
