package org.lintx.plugins.yinwuchat.chat.handle;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.Util.MessageUtil;
import org.lintx.plugins.yinwuchat.bungee.RedisUtil;
import org.lintx.plugins.yinwuchat.bungee.config.Config;
import org.lintx.plugins.yinwuchat.bungee.config.PlayerConfig;
import org.lintx.plugins.yinwuchat.bungee.YinwuChat;
import org.lintx.plugins.yinwuchat.bungee.json.RedisMessageType;
import org.lintx.plugins.yinwuchat.chat.struct.Chat;
import org.lintx.plugins.yinwuchat.chat.struct.BungeeChatPlayer;
import org.lintx.plugins.yinwuchat.chat.struct.ChatSource;
import org.lintx.plugins.yinwuchat.chat.struct.ChatType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BungeeAtPlayerHandle extends ChatHandle {
    private List<ProxiedPlayer> atPlayers;
    private Config config = Config.getInstance();
    private boolean isSendPermissionTip;
    private BungeeChatPlayer player;

    @Override
    public void handle(Chat chat) {
        if (chat.source!= ChatSource.GAME && chat.source!= ChatSource.WEB) return;
        if (chat.type!= ChatType.PUBLIC) return;
        if (!(chat.fromPlayer instanceof BungeeChatPlayer)) return;
        player = (BungeeChatPlayer)chat.fromPlayer;
        // 注意：Web 端发送时 player.player 可能为 null，但 player.config 和 player.playerName 是有的
        
        atPlayers = new ArrayList<>();
        isSendPermissionTip = false;
        atAll(chat);
        atOne(chat);

        if (player.player != null && !player.player.hasPermission(Const.PERMISSION_COOL_DOWN_BYPASS)){
            if (atPlayers.size()>0) chat.fromPlayer.config.updateCooldown();
        }
    }

    private void atAll(Chat chat){
        if (player.player != null && !player.player.hasPermission(Const.PERMISSION_AT_ALL)) return;
        String regexp = "@(\\w*?)("+config.atAllKey+")(?=\\W|$)";

        handle(chat, regexp, (matcher) -> {
            TextComponent component = new TextComponent();
            String server = matcher.group(1).toLowerCase();
            if ("".equals(server)){
                for (ProxiedPlayer p: YinwuChat.getPlugin().getProxy().getPlayers()){
                    if (player.player != null && p.equals(player.player)) continue;
                    if (p.getName().equalsIgnoreCase(player.playerName)) continue;
                    
                    if (atPlayers.contains(p)) continue;
                    if (atPlayer(player,p,true)){
                        atPlayers.add(p);
                    }
                }
                
                // 同时通知 Web 在线用户
                for (org.lintx.plugins.yinwuchat.bungee.httpserver.WsClientUtil util : org.lintx.plugins.yinwuchat.bungee.httpserver.WsClientHelper.utils()) {
                    if (util.getUuid() == null) continue;
                    String name = PlayerConfig.getConfig(util.getUuid()).name;
                    if (name != null && !name.equalsIgnoreCase(player.playerName)) {
                        // Web 端通知由 broadcast 逻辑中的 mention: true 处理
                    }
                }

                if (config.redisConfig.openRedis){
                    RedisUtil.sendMessage(RedisMessageType.AT_PLAYER_ALL,player.player != null ? player.player.getUniqueId() : PlayerConfig.getTokens().getAllUuids().iterator().next(),new TextComponent(MessageUtil.replace(config.tipsConfig.atyouTip.replaceAll("\\{player}",player.playerName))),"");
                }
                component.setText("§b" + matcher.group(0) + "§r");
                return component;
            }

            ServerInfo atServer = null;
            ServerInfo findServer = null;
            for (ServerInfo serverInfo : YinwuChat.getPlugin().getProxy().getServers().values()){
                String serverName = serverInfo.getName().toLowerCase(Locale.ROOT);
                if (serverName.equalsIgnoreCase(server)){
                    atServer = serverInfo;
                    break;
                }
                if (serverName.startsWith(server)){
                    findServer = serverInfo;
                }
            }
            if (atServer==null){
                atServer = findServer;
            }
            if (atServer!=null){
                for (ProxiedPlayer p: YinwuChat.getPlugin().getProxy().getServerInfo(atServer.getName()).getPlayers()){
                    if (player.player != null && p.equals(player.player)) continue;
                    if (p.getName().equalsIgnoreCase(player.playerName)) continue;
                    
                    if (atPlayers.contains(p)) continue;
                    if (atPlayer(player,p,true)){
                        atPlayers.add(p);
                    }
                }
                if (config.redisConfig.openRedis){
                    RedisUtil.sendMessage(RedisMessageType.AT_PLAYER_ALL,player.player != null ? player.player.getUniqueId() : PlayerConfig.getTokens().getAllUuids().iterator().next(),new TextComponent(MessageUtil.replace(config.tipsConfig.atyouTip.replaceAll("\\{player}",player.playerName))),"");
                }

                component.setText("§b@" +atServer.getName() + matcher.group(2) + "§r");
                return component;
            }
            return null;
        });
    }

    private void atOne(Chat chat){
        String regexp = "@(\\w*?)(?=\\W|$)";
        handle(chat, regexp, (matcher) -> {
            ProxiedPlayer atProxiedPlayer = null;
            ProxiedPlayer findProxiedPlayer = null;
            String str = matcher.group(1).toLowerCase(Locale.ROOT);
            if (str.equals("")){
                return null;
            }
            
            // 查找游戏内玩家
            for (ProxiedPlayer p: YinwuChat.getPlugin().getProxy().getPlayers()){
                PlayerConfig.Player pc = PlayerConfig.getConfig(p);
                if (!pc.vanish){
                    String player_name = p.getName().toLowerCase(Locale.ROOT);
                    if (player_name.equalsIgnoreCase(str)){
                        atProxiedPlayer = p;
                        break;
                    }
                    if (player_name.startsWith(str)){
                        findProxiedPlayer = p;
                    }
                }
            }
            if (atProxiedPlayer==null){
                atProxiedPlayer = findProxiedPlayer;
            }
            
            if (atProxiedPlayer!=null){
                if (atProxiedPlayer.getName().equalsIgnoreCase(player.playerName)){
                    atProxiedPlayer.sendMessage(new TextComponent(MessageUtil.replace(config.tipsConfig.atyouselfTip)));
                    return null;
                }
                if (atPlayers.contains(atProxiedPlayer)) return null;
                if (atPlayer(player,atProxiedPlayer,false)){
                    atPlayers.add(atProxiedPlayer);
                    return new TextComponent("§b@" + atProxiedPlayer.getName() + "§r");
                }
            }
            
            // 查找 Web 玩家
            for (org.lintx.plugins.yinwuchat.bungee.httpserver.WsClientUtil util : org.lintx.plugins.yinwuchat.bungee.httpserver.WsClientHelper.utils()) {
                if (util.getUuid() == null) continue;
                PlayerConfig.Player pc = PlayerConfig.getConfig(util.getUuid());
                if (pc.name != null && !pc.name.isEmpty() && !pc.vanish) {
                    if (pc.name.equalsIgnoreCase(str) || pc.name.toLowerCase(Locale.ROOT).startsWith(str)) {
                        if (pc.name.equalsIgnoreCase(player.playerName)) return null;
                        
                        // 检查忽略列表
                        if (pc.isIgnore(player.playerName)) return null;
                        if (pc.muteAt) return null; // 被@者开启了免打扰
                        
                        return new TextComponent("§b@" + pc.name + "§r");
                    }
                }
            }

            if (config.redisConfig.openRedis){
                String findPlayerName = null;
                String toPlayerName = null;
                for (String rpn : RedisUtil.playerList.keySet()){
                    String pn = rpn.toLowerCase(Locale.ROOT);
                    if (pn.equals(str)) {
                        toPlayerName = rpn;
                        break;
                    }
                    if (pn.startsWith(str)) {
                        findPlayerName = rpn;
                    }
                }
                if (toPlayerName == null && findPlayerName!=null) {
                    toPlayerName = findPlayerName;
                }
                if (toPlayerName!=null){
                    RedisUtil.sendMessage(RedisMessageType.AT_PLAYER,player.player != null ? player.player.getUniqueId() : PlayerConfig.getTokens().getAllUuids().iterator().next(),new TextComponent(MessageUtil.replace(config.tipsConfig.atyouTip.replaceAll("\\{player}",player.playerName))),toPlayerName);
                    return new TextComponent("§b@" + toPlayerName + "§r");
                }
            }
            return null;
        });
    }

    private boolean atPlayer(BungeeChatPlayer player, ProxiedPlayer atPlayer, boolean atAll){
        PlayerConfig.Player pc = PlayerConfig.getConfig(atPlayer);
        Config config = Config.getInstance();
        if (!atAll){
            if (pc.isIgnore(player.playerName)){
                if (player.player != null) player.player.sendMessage(MessageUtil.newTextComponent(MessageUtil.replace(config.tipsConfig.ignoreTip)));
                return false;
            }
            if (pc.muteAt){ // 原 muteAt 指的是禁止@声音
                // 继续发送文字提示，但不发送声音
            }
            if (pc.banAt){ // 原 banAt 指的是完全禁止@
                if (player.player != null) player.player.sendMessage(MessageUtil.newTextComponent(MessageUtil.replace(config.tipsConfig.banatTip)));
                return false;
            }
        }
        if (player.player != null && !player.player.hasPermission(Const.PERMISSION_COOL_DOWN_BYPASS)){
            if (player.config.isCooldown()){
                if (!isSendPermissionTip)
                    player.player.sendMessage(MessageUtil.newTextComponent(MessageUtil.replace(config.tipsConfig.cooldownTip)));
                isSendPermissionTip = true;
                return false;
            }
        }
        atPlayer.sendMessage(MessageUtil.newTextComponent(MessageUtil.replace(config.tipsConfig.atyouTip.replaceAll("\\{player}",player.playerName))));

        if (!atAll && pc.muteAt){
            return true;
        }
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(Const.PLUGIN_SUB_CHANNEL_AT);
        if (atPlayer.getServer() != null) {
            atPlayer.getServer().sendData(Const.PLUGIN_CHANNEL,output.toByteArray());
        }
        return true;
    }
}
