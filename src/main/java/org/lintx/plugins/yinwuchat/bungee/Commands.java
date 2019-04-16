package org.lintx.plugins.yinwuchat.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.java_websocket.WebSocket;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.bungee.json.*;
import org.lintx.plugins.yinwuchat.bungee.util.*;

import java.util.*;

public class Commands extends Command {
    private YinwuChat plugin;

    public Commands(YinwuChat plugin,String name) {
        super(name,null,"yw");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length>=1 && args[0].equalsIgnoreCase("reload")) {
            if (sender instanceof ProxiedPlayer) {
                if (sender.hasPermission(Const.PERMISSION_RELOAD)) {
                    reload(sender,args);
                }
                else{
                    sender.sendMessage(buildMessage(ChatColor.RED + "权限不足"));
                    return;
                }
            }
            else{
                reload(sender,args);
            }
            return;
        }
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(buildMessage("Must use command in-game"));
            return;
        }
        final ProxiedPlayer player = (ProxiedPlayer)sender;
        final UUID playerUUID = player.getUniqueId();
        if (playerUUID==null) {
            plugin.getLogger().info("Player " + sender.getName() + "has a null UUID");
            sender.sendMessage(buildMessage(ChatColor.RED + "You can't use that command right now. (No UUID)"));
            return;
        }
        if (args.length>=1) {
            String first = args[0];
            PlayerConfig.Player playerConfig = PlayerConfig.getConfig(player);
            if (first.equalsIgnoreCase("badword")){
                if (player.hasPermission(Const.PERMISSION_BADWORD)){
                    if (args.length>=3){
                        if (args[1].equalsIgnoreCase("add")){
                            String word = args[2].toLowerCase(Locale.ROOT);
                            Config.getInstance().shieldeds.add(word);
                            Config.getInstance().save(plugin);
                            sender.sendMessage(buildMessage(ChatColor.GREEN + "成功将关键词 " + word + " 添加到屏蔽库"));
                            return;
                        }
                        else if (args[1].equalsIgnoreCase("remove")){
                            String word = args[2].toLowerCase(Locale.ROOT);
                            if (Config.getInstance().shieldeds.contains(word)){
                                Config.getInstance().shieldeds.remove(word);
                                Config.getInstance().save(plugin);
                                sender.sendMessage(buildMessage(ChatColor.GREEN + "成功将关键词 " + word + " 从屏蔽库删除"));
                                return;
                            }
                        }
                    }
                    else if (args.length==2 && args[1].equalsIgnoreCase("list")){
                        sender.sendMessage(buildMessage(ChatColor.GREEN + "屏蔽关键词："));
                        for (String str:Config.getInstance().shieldeds){
                            sender.sendMessage(buildMessage(ChatColor.GREEN + str));
                        }
                        return;
                    }
                    sender.sendMessage(buildMessage(ChatColor.RED + "命令格式：/yinwuchat badword <add|remove|list> [word]"));
                    return;
                }
            }
            else if (first.equalsIgnoreCase("bind")) {
                if (args.length>=2) {
                    String token = args[1];
                    PlayerConfig.Tokens tokens = PlayerConfig.getTokens();
                    if (tokens.bindToken(token,player)){
                        sender.sendMessage(buildMessage(ChatColor.GREEN + "绑定成功"));
                        tokens.save();
                        WebSocket ws = WsClientHelper.getWebSocket(token);
                        if (ws != null) {
                            WsClientHelper.get(ws).setUUID(playerUUID);
                            ws.send((new InputCheckToken(token,false)).getJSON());
                            WsClientHelper.kickOtherWS(ws, playerUUID);
                        }
//                        YinwuChat.getWSServer().broadcast((new PlayerStatusJSON(player.getDisplayName(),PlayerStatusJSON.PlayerStatus.WEB_JOIN)).getWebStatusJSON());
//                        plugin.getProxy().broadcast(ChatUtil.formatJoinMessage(playerUUID));
                        PlayerListJSON.sendWebPlayerList();
                    }
                    else {
                        sender.sendMessage(buildMessage(ChatColor.RED + "绑定失败，你可以重试几次，如果持续失败，请联系OP，错误代码：002"));
                    }
                }
                else{
                    sender.sendMessage(buildMessage(ChatColor.RED + "命令格式：/yinwuchat token"));
                    sender.sendMessage(buildMessage(ChatColor.RED + "缺少token（token从网页客户端获取）"));
                }
                return;
            }
            else if (first.equalsIgnoreCase("list")) {
                PlayerConfig.Tokens tokens = PlayerConfig.getTokens();
                List<String> list = tokens.tokenWithPlayer(player);
                if (list.isEmpty()) {
                    sender.sendMessage(buildMessage(ChatColor.GREEN + "你没有绑定任何token"));
                    return;
                }
                sender.sendMessage(buildMessage(ChatColor.GREEN + "你一共绑定了"+list.size()+"个token，详情如下："));
                for (String token : list) {
                    sender.sendMessage(buildMessage(ChatColor.GREEN + token));
                }
                return;
            }
            else if (first.equalsIgnoreCase("unbind")) {
                if (args.length>=2) {
                    String token = args[1];

                    PlayerConfig.Tokens tokens = PlayerConfig.getTokens();
                    List<String> list = tokens.tokenWithPlayer(player);
                    if (list.isEmpty()) {
                        sender.sendMessage(buildMessage(ChatColor.GREEN + "你没有绑定任何token"));
                        return;
                    }
                    for (String t : list) {
                        if (t.startsWith(token)){
                            tokens.unbindToken(t);
                            sender.sendMessage(buildMessage(ChatColor.GREEN + "解绑成功"));
                            tokens.save();
                            return;
                        }
                    }
                    sender.sendMessage(buildMessage(ChatColor.RED + "在你已绑定的token中没有找到对应的数据，解绑失败"));
                }
                else{
                    sender.sendMessage(buildMessage(ChatColor.RED + "命令格式：/yinwuchat unbind token"));
                    sender.sendMessage(buildMessage(ChatColor.RED + "token可以只输入前面的部分"));
                }
                return;
            }
            else if (first.equalsIgnoreCase("vanish")){
                if (player.hasPermission(Const.PERMISSION_VANISH)){
                    playerConfig.vanish = !playerConfig.vanish;
                    playerConfig.save();
                    if (playerConfig.vanish){
                        sender.sendMessage(buildMessage(ChatColor.RED + "你现在在聊天系统中处于隐身状态"));
                    }
                    else {
                        sender.sendMessage(buildMessage(ChatColor.GREEN + "你现在在聊天系统中不再处于隐身状态"));
                    }
                    return;
                }
            }
            else if (first.equalsIgnoreCase("muteat")){
                playerConfig.muteAt = !playerConfig.muteAt;
                playerConfig.save();
                if (playerConfig.muteAt){
                    sender.sendMessage(buildMessage(ChatColor.RED + "你现在被@时不再会听到声音了"));
                }
                else {
                    sender.sendMessage(buildMessage(ChatColor.GREEN + "你现在被@时可以听到声音了"));
                }
                return;
            }
            else if (first.equalsIgnoreCase("noat")){
                playerConfig.banAt = !playerConfig.banAt;
                playerConfig.save();
                if (playerConfig.banAt){
                    sender.sendMessage(buildMessage(ChatColor.RED + "你现在不能被@了（管理员@全体除外）"));
                }
                else {
                    sender.sendMessage(buildMessage(ChatColor.GREEN + "你现在可以被@了"));
                }
                return;
            }
            else if (first.equalsIgnoreCase("ignore")){
                if (args.length>=2) {
                    String name = args[1].toLowerCase(Locale.ROOT);
                    for (ProxiedPlayer p:plugin.getProxy().getPlayers()){
                        if (p.getDisplayName().toLowerCase(Locale.ROOT).equals(name)){
                            if (playerConfig.ignore(p.getUniqueId())){
                                sender.sendMessage(buildMessage(ChatColor.RED + "你现在忽略了"+p.getDisplayName()+"的信息"));
                            }
                            else {
                                sender.sendMessage(buildMessage(ChatColor.GREEN + "你不再忽略"+p.getDisplayName()+"的信息"));
                            }
                            return;
                        }
                    }

                    for (WsClientUtil util:WsClientHelper.utils()){
                        PlayerConfig.Player p = PlayerConfig.getConfig(util.getUuid());
                        if (name.equals(p.name.toLowerCase(Locale.ROOT))){
                            if (playerConfig.ignore(util.getUuid())){
                                sender.sendMessage(buildMessage(ChatColor.RED + "你现在忽略了"+p.name+"的信息"));
                            }
                            else {
                                sender.sendMessage(buildMessage(ChatColor.GREEN + "你不再忽略"+p.name+"的信息"));
                            }
                            return;
                        }
                    }
                }
                else{
                    sender.sendMessage(buildMessage(ChatColor.GOLD + "忽略某人的消息：/yinwuchat ignore <player_name>（再输入一次不再忽略）"));
                }
                return;
            }
            /*
            else if (first.equalsIgnoreCase("msg")) {
                if (args.length>=3) {
                    //应该加入判断不能对自己发消息
                    String to_player_name = args[1];
                    List<String> tmpList = new ArrayList<>(Arrays.asList(args).subList(2, args.length));
                    String msg = String.join(" ", tmpList);
                    int message_id = -1;

                    if (to_player_name.equalsIgnoreCase(player.getDisplayName())) {
                        sender.sendMessage(buildMessage(ChatColor.RED + "你不能向自己发送私聊信息"));
                        return;
                    }

                    boolean issend = false;
                    ProxiedPlayer toPlayer = plugin.getProxy().getPlayer(to_player_name);
                    if (Permission.hidden(toPlayer)){
                        toPlayer = null;
                    }
                    String server_name = "";
                    try {
                        server_name = player.getServer().getInfo().getName();
                    } catch (Exception ignored) {
                    }
                    if (toPlayer != null) {
                        toPlayer.sendMessage(ChatUtil.formatPrivateMessage(playerUUID, msg));
                        issend = true;
                        message_id = Chat2SqlUtil.newMessage(playerUUID, toPlayer.getUniqueId(), server_name, msg);
                    }
                    WebSocket ws = WsClientHelper.getWebSocketAsPlayerName(to_player_name);
                    if (ws != null) {
                        PrivateMessageJSON msgJSON = new PrivateMessageJSON(player.getDisplayName(),to_player_name, msg, server_name);
                        if (!issend) {
                            message_id = Chat2SqlUtil.newMessage(playerUUID, WsClientHelper.get(ws).getUuid(), server_name, msg);
                        }
                        ws.send(msgJSON.getJSON(message_id));
                        issend = true;
                    }
                    if (!issend) {
                        sender.sendMessage(buildMessage(ChatColor.RED + "玩家" + to_player_name + "不在线"));
                    }
                    else{
                        sender.sendMessage(ChatUtil.formatMePrivateMessage(to_player_name, msg));
                    }
                }
                else{
                    sender.sendMessage(buildMessage(ChatColor.RED + "命令格式：/yinwuchat msg 玩家名 消息"));
                    sender.sendMessage(buildMessage(ChatColor.RED + "缺少玩家名或消息"));
                }
                return;
            }
             */
        }
        sender.sendMessage(buildMessage(ChatColor.GOLD + "YinwuChat Version "+ plugin.getDescription().getVersion() + ",Author:"+plugin.getDescription().getAuthor()));
        sender.sendMessage(buildMessage(ChatColor.GOLD + "插件帮助："));
        sender.sendMessage(buildMessage(ChatColor.GOLD + "插件基本命令为&b/yinwuchat&6或&b/yw&6"));
        sender.sendMessage(buildMessage(ChatColor.GOLD + "绑定：&b/yinwuchat bind <token>&6，token为web端获取"));
        sender.sendMessage(buildMessage(ChatColor.GOLD + "查询：&b/yinwuchat list&6，可以查询到你绑定的所有token"));
        sender.sendMessage(buildMessage(ChatColor.GOLD + "解绑：&b/yinwuchat unbind <token>"));
        sender.sendMessage(buildMessage(ChatColor.GOLD + "可以解绑对应的token，token为查询结果中的token,可以只输入前面的部分"));
        sender.sendMessage(buildMessage(ChatColor.GOLD + "发送私聊消息：&b/msg <player_name> <message>&6，例：&b/msg LinTx 一条私聊消息"));
        sender.sendMessage(buildMessage(ChatColor.GOLD + "被@时静音：&b/yinwuchat muteat"));
        sender.sendMessage(buildMessage(ChatColor.GOLD + "阻止自己被@：&b/yinwuchat noat&6（无法阻止被管理@全体）"));
        sender.sendMessage(buildMessage(ChatColor.GOLD + "忽略某人的消息：&b/yinwuchat ignore <player_name>&6（再输入一次不再忽略）"));
        if (sender.hasPermission(Const.PERMISSION_RELOAD)){
            sender.sendMessage(buildMessage("&c重新加载插件：&b/yinwuchat reload [config|ws]&c重新加载插件/插件配置/WebSocket"));
        }
        if (sender.hasPermission(Const.PERMISSION_BADWORD)){
            sender.sendMessage(buildMessage("&c聊天关键词管理：&b/yinwuchat badword <add|remove|list> [word]"));
        }
        if (sender.hasPermission(Const.PERMISSION_VANISH)){
            sender.sendMessage(buildMessage("&c聊天隐身：&b/yinwuchat vanish"));
        }
    }

    private TextComponent buildMessage(String message){
        return new TextComponent(MessageManage.getInstance().formatMessage(message));
    }

    private void reload(CommandSender sender, String[] args){
        sender.sendMessage(buildMessage(ChatColor.GREEN + "YinwuChat插件重载"));
        if (args.length==1){
            plugin.reload();
        }
        else if (args.length>1){
            String s = args[1];
            if (s.equalsIgnoreCase("config")){
                plugin.reloadConfig();
            }
            else if (s.equalsIgnoreCase("ws")){
                plugin.startWs();
            }
        }
    }

//    @Override
//    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
//        return null;
//    }
}
