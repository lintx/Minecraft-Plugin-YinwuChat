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
import org.lintx.plugins.yinwuchat.bungee.json.RedisMessage;
import org.lintx.plugins.yinwuchat.bungee.json.RedisMessageType;
import org.lintx.plugins.yinwuchat.chat.struct.Chat;
import org.lintx.plugins.yinwuchat.chat.struct.BungeeChatPlayer;
import org.lintx.plugins.yinwuchat.chat.struct.ChatSource;

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
        if (chat.source!= ChatSource.GAME) return;
        if (!(chat.fromPlayer instanceof BungeeChatPlayer)) return;
        player = (BungeeChatPlayer)chat.fromPlayer;
        if (player.player==null) return;

        atPlayers = new ArrayList<>();
        isSendPermissionTip = false;
        atAll(chat);
        atOne(chat);

        if (!player.player.hasPermission(Const.PERMISSION_COOL_DOWN_BYPASS)){
            if (atPlayers.size()>0) chat.fromPlayer.config.updateCooldown();
        }
    }

    private void atAll(Chat chat){
        if (!player.player.hasPermission(Const.PERMISSION_AT_ALL)) return;
        String regexp = "@(\\w*?)("+config.atAllKey+")(?=\\W|$)";

        handle(chat, regexp, (matcher) -> {
//            MessageFormat extra = new MessageFormat();
            TextComponent component = new TextComponent();
            String server = matcher.group(1).toLowerCase();
            if ("".equals(server)){
                for (ProxiedPlayer p: YinwuChat.getPlugin().getProxy().getPlayers()){
                    if (!p.equals(player.player)){
                        if (atPlayers.contains(p)) continue;
                        if (atPlayer(player,p,true)){
                            atPlayers.add(p);
                        }
                    }
                }
                if (config.redisConfig.openRedis){
                    RedisUtil.sendMessage(RedisMessageType.AT_PLAYER_ALL,player.player.getUniqueId(),new TextComponent(MessageUtil.replace(config.tipsConfig.atyouTip.replaceAll("\\{player}",player.playerName))),"");
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
                    if (!p.equals(player.player)){
                        if (atPlayers.contains(p)) continue;
                        if (atPlayer(player,p,true)){
                            atPlayers.add(p);
                        }
                    }
                }
                if (config.redisConfig.openRedis){
                    RedisUtil.sendMessage(RedisMessageType.AT_PLAYER_ALL,player.player.getUniqueId(),new TextComponent(MessageUtil.replace(config.tipsConfig.atyouTip.replaceAll("\\{player}",player.playerName))),"");
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
            System.out.println(matcher.group(0));
            ProxiedPlayer atPlayer = null;
            ProxiedPlayer findPlayer = null;
            String str = matcher.group(1).toLowerCase(Locale.ROOT);
            if (str.equals("")){
                return null;
            }
            for (ProxiedPlayer p: YinwuChat.getPlugin().getProxy().getPlayers()){
                PlayerConfig.Player pc = PlayerConfig.getConfig(p);
                if (!pc.vanish){
                    String player_name = p.getName().toLowerCase(Locale.ROOT);
                    if (player_name.equalsIgnoreCase(str)){
                        atPlayer = p;
                        break;
                    }
                    if (player_name.startsWith(str)){
                        findPlayer = p;
                    }
                }
            }
            if (atPlayer==null){
                atPlayer = findPlayer;
            }
            if (atPlayer!=null){
                if (atPlayer.equals(player.player)){
                    atPlayer.sendMessage(new TextComponent(MessageUtil.replace(config.tipsConfig.atyouselfTip)));
                    return null;
                }
                if (atPlayers.contains(atPlayer)) return null;
                if (atPlayer(player,atPlayer,false)){
                    atPlayers.add(atPlayer);
                    return new TextComponent("§b@" + atPlayer.getName() + "§r");
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
                    RedisUtil.sendMessage(RedisMessageType.AT_PLAYER,player.player.getUniqueId(),new TextComponent(MessageUtil.replace(config.tipsConfig.atyouTip.replaceAll("\\{player}",player.playerName))),toPlayerName);
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
            if (pc.isIgnore(player.player)){
                player.player.sendMessage(new TextComponent(MessageUtil.replace(config.tipsConfig.ignoreTip)));
                return false;
            }
            if (pc.banAt){
                player.player.sendMessage(new TextComponent(MessageUtil.replace(config.tipsConfig.banatTip)));
                return false;
            }
        }
        if (!player.player.hasPermission(Const.PERMISSION_COOL_DOWN_BYPASS)){
            if (player.config.isCooldown()){
                if (!isSendPermissionTip)
                    player.player.sendMessage(new TextComponent(MessageUtil.replace(config.tipsConfig.cooldownTip)));
                isSendPermissionTip = true;
                return false;
            }
        }
        atPlayer.sendMessage(new TextComponent(MessageUtil.replace(config.tipsConfig.atyouTip.replaceAll("\\{player}",player.playerName))));

        if (!atAll && pc.muteAt){
            return true;
        }
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(Const.PLUGIN_SUB_CHANNEL_AT);
        atPlayer.getServer().sendData(Const.PLUGIN_CHANNEL,output.toByteArray());
        return true;
    }
}
