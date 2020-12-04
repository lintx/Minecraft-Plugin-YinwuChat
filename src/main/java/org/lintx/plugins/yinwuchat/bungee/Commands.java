package org.lintx.plugins.yinwuchat.bungee;

import io.netty.channel.Channel;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.Util.MessageUtil;
import org.lintx.plugins.yinwuchat.bungee.config.Config;
import org.lintx.plugins.yinwuchat.bungee.config.PlayerConfig;
import org.lintx.plugins.yinwuchat.bungee.json.*;
import org.lintx.plugins.yinwuchat.bungee.httpserver.NettyChannelMessageHelper;
import org.lintx.plugins.yinwuchat.bungee.httpserver.WsClientHelper;
import org.lintx.plugins.yinwuchat.bungee.httpserver.WsClientUtil;

import java.util.*;

public class Commands extends Command {
    private YinwuChat plugin;

    Commands(YinwuChat plugin, String name) {
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
                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "权限不足"));
                    return;
                }
            }
            else{
                reload(sender,args);
            }
            return;
        }
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(MessageUtil.newTextComponent("Must use command in-game"));
            return;
        }
        final ProxiedPlayer player = (ProxiedPlayer)sender;
        final UUID playerUUID = player.getUniqueId();
        if (playerUUID==null) {
            plugin.getLogger().info("Player " + sender.getName() + "has a null UUID");
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "You can't use that command right now. (No UUID)"));
            return;
        }
        if (args.length>=1) {
            String first = args[0];
            PlayerConfig.Player playerConfig = PlayerConfig.getConfig(player);
            if (first.equalsIgnoreCase("badword")){
                if (player.hasPermission(Const.PERMISSION_BAD_WORD)){
                    if (args.length>=3){
                        if (args[1].equalsIgnoreCase("add")){
                            String word = args[2].toLowerCase(Locale.ROOT);
                            Config.getInstance().shieldeds.add(word);
                            Config.getInstance().save(plugin);
                            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "成功将关键词 " + word + " 添加到屏蔽库"));
                            return;
                        }
                        else if (args[1].equalsIgnoreCase("remove")){
                            String word = args[2].toLowerCase(Locale.ROOT);
                            if (Config.getInstance().shieldeds.contains(word)){
                                Config.getInstance().shieldeds.remove(word);
                                Config.getInstance().save(plugin);
                                sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "成功将关键词 " + word + " 从屏蔽库删除"));
                                return;
                            }
                        }
                    }
                    else if (args.length==2 && args[1].equalsIgnoreCase("list")){
                        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "屏蔽关键词："));
                        for (String str:Config.getInstance().shieldeds){
                            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + str));
                        }
                        return;
                    }
                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "命令格式：/yinwuchat badword <add|remove|list> [word]"));
                    return;
                }
            }
            else if (first.equalsIgnoreCase("bind")) {
                if (args.length>=2) {
                    String token = args[1];
                    PlayerConfig.Tokens tokens = PlayerConfig.getTokens();
                    if (tokens.bindToken(token,player)){
                        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "绑定成功"));
                        tokens.save();
                        Channel channel = WsClientHelper.getWebSocket(token);
                        if (channel != null) {
                            WsClientHelper.get(channel).setUUID(playerUUID);
                            NettyChannelMessageHelper.send(channel,(new InputCheckToken(token,false)).getJSON());
                            WsClientHelper.kickOtherWS(channel, playerUUID);
                        }
//                        YinwuChat.getWSServer().broadcast((new PlayerStatusJSON(player.getDisplayName(),PlayerStatusJSON.PlayerStatus.WEB_JOIN)).getWebStatusJSON());
//                        plugin.getProxy().broadcast(ChatUtil.formatJoinMessage(playerUUID));
                        OutputPlayerList.sendWebPlayerList();
                    }
                    else {
                        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "绑定失败，你可以重试几次，如果持续失败，请联系OP，错误代码：002"));
                    }
                }
                else{
                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "命令格式：/yinwuchat token"));
                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "缺少token（token从网页客户端获取）"));
                }
                return;
            }
            else if (first.equalsIgnoreCase("list")) {
                PlayerConfig.Tokens tokens = PlayerConfig.getTokens();
                List<String> list = tokens.tokenWithPlayer(player);
                if (list.isEmpty()) {
                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你没有绑定任何token"));
                    return;
                }
                sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你一共绑定了"+list.size()+"个token，详情如下："));
                for (String token : list) {
                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + token));
                }
                return;
            }
            else if (first.equalsIgnoreCase("unbind")) {
                if (args.length>=2) {
                    String token = args[1];

                    PlayerConfig.Tokens tokens = PlayerConfig.getTokens();
                    List<String> list = tokens.tokenWithPlayer(player);
                    if (list.isEmpty()) {
                        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你没有绑定任何token"));
                        return;
                    }
                    for (String t : list) {
                        if (t.startsWith(token)){
                            tokens.unbindToken(t);
                            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "解绑成功"));
                            tokens.save();
                            return;
                        }
                    }
                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "在你已绑定的token中没有找到对应的数据，解绑失败"));
                }
                else{
                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "命令格式：/yinwuchat unbind token"));
                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "token可以只输入前面的部分"));
                }
                return;
            }
            else if (first.equalsIgnoreCase("format") && Config.getInstance().allowPlayerFormatPrefixSuffix){
                if (args.length>=4){
                    String namespace = args[1].toLowerCase(Locale.ROOT);
                    String position = args[2].toLowerCase(Locale.ROOT);
                    String action = args[3].toLowerCase(Locale.ROOT);
                    String str = "";
                    if (args.length>=5){
                        str = MessageUtil.filter(args[4],Config.getInstance().playerFormatPrefixSuffixDenyStyle);
                    }
                    if (namespace.equals("public")){
                        if (position.equals("prefix")){
                            if ("view".equals(action)){
                                if ("".equals(playerConfig.publicPrefix)){
                                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你没有设置公共消息前缀"));
                                }else {
                                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你的公共消息前缀是:"+playerConfig.publicPrefix.replaceAll("([&§])([0-9a-fklmnor])","$1&a$2")));
                                }
                                return;
                            }
                            else if ("set".equals(action)) {
                                playerConfig.publicPrefix = str;
                                playerConfig.save();
                                sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你的公共消息前缀已设置为:"+playerConfig.publicPrefix.replaceAll("([&§])([0-9a-fklmnor])","$1&a$2")));
                                return;
                            }
                        }
                        else if (position.equals("suffix")){
                            if ("view".equals(action)){
                                if ("".equals(playerConfig.publicSuffix)){
                                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你没有设置公共消息后缀"));
                                }else {
                                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你的公共消息后缀是:"+playerConfig.publicSuffix.replaceAll("([&§])([0-9a-fklmnor])","$1&a$2")));
                                }
                                return;
                            }
                            else if ("set".equals(action))  {
                                playerConfig.publicSuffix = str;
                                playerConfig.save();
                                sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你的公共消息后缀已设置为:"+playerConfig.publicSuffix.replaceAll("([&§])([0-9a-fklmnor])","$1&a$2")));
                                return;
                            }
                        }
                    }
                    else if (namespace.equals("private")){
                        if (position.equals("prefix")){
                            if ("view".equals(action)){
                                if ("".equals(playerConfig.privatePrefix)){
                                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你没有设置私聊消息前缀"));
                                }else {
                                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你的私聊消息前缀是:"+playerConfig.privatePrefix.replaceAll("([&§])([0-9a-fklmnor])","$1&a$2")));
                                }
                                return;
                            }
                            else if ("set".equals(action))  {
                                playerConfig.privatePrefix = str;
                                playerConfig.save();
                                sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你的私聊消息前缀已设置为:"+playerConfig.privatePrefix.replaceAll("([&§])([0-9a-fklmnor])","$1&a$2")));
                                return;
                            }
                        }
                        else if (position.equals("suffix")){
                            if ("view".equals(action)){
                                if ("".equals(playerConfig.privateSuffix)){
                                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你没有设置私聊消息后缀"));
                                }else {
                                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你的私聊消息后缀是:"+playerConfig.privateSuffix.replaceAll("([&§])([0-9a-fklmnor])","$1&a$2")));
                                }
                                return;
                            }
                            else if ("set".equals(action))  {
                                playerConfig.privateSuffix = str;
                                playerConfig.save();
                                sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你的私聊消息后缀已设置为:"+playerConfig.privateSuffix.replaceAll("([&§])([0-9a-fklmnor])","$1&a$2")));
                                return;
                            }
                        }
                    }
                }
            }
            else if (first.equalsIgnoreCase("vanish")){
                if (player.hasPermission(Const.PERMISSION_VANISH)){
                    playerConfig.vanish = !playerConfig.vanish;
                    playerConfig.save();
                    if (playerConfig.vanish){
                        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "你现在在聊天系统中处于隐身状态"));
                    }
                    else {
                        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你现在在聊天系统中不再处于隐身状态"));
                    }
                    return;
                }
            }
            else if (first.equalsIgnoreCase("muteat")){
                playerConfig.muteAt = !playerConfig.muteAt;
                playerConfig.save();
                if (playerConfig.muteAt){
                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "你现在被@时不再会听到声音了"));
                }
                else {
                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你现在被@时可以听到声音了"));
                }
                return;
            }
            else if (first.equalsIgnoreCase("noat")){
                playerConfig.banAt = !playerConfig.banAt;
                playerConfig.save();
                if (playerConfig.banAt){
                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "你现在不能被@了（管理员@全体除外）"));
                }
                else {
                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你现在可以被@了"));
                }
                return;
            }
            else if (first.equalsIgnoreCase("ignore")){
                if (args.length>=2) {
                    String name = args[1].toLowerCase(Locale.ROOT);
                    for (ProxiedPlayer p:plugin.getProxy().getPlayers()){
                        if (p.getName().toLowerCase(Locale.ROOT).equals(name)){
                            if (playerConfig.ignore(p.getUniqueId())){
                                sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "你现在忽略了"+p.getName()+"的信息"));
                            }
                            else {
                                sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你不再忽略"+p.getName()+"的信息"));
                            }
                            return;
                        }
                    }

                    for (WsClientUtil util:WsClientHelper.utils()){
                        PlayerConfig.Player p = PlayerConfig.getConfig(util.getUuid());
                        if (name.equals(p.name.toLowerCase(Locale.ROOT))){
                            if (playerConfig.ignore(util.getUuid())){
                                sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "你现在忽略了"+p.name+"的信息"));
                            }
                            else {
                                sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你不再忽略"+p.name+"的信息"));
                            }
                            return;
                        }
                    }
                }
                else{
                    sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GOLD + "忽略某人的消息：/yinwuchat ignore <player_name>（再输入一次不再忽略）"));
                }
                return;
            }
            else if (first.equalsIgnoreCase("monitor")){
                if (player.hasPermission(Const.PERMISSION_MONITOR_PRIVATE_MESSAGE)){
                    playerConfig.monitor = !playerConfig.monitor;
                    playerConfig.save();
                    if (playerConfig.monitor){
                        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "你现在会监听其他玩家的私聊信息"));
                    }
                    else {
                        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.RED + "你现在不会监听其他玩家的私聊信息"));
                    }
                    return;
                }
            }
        }
        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GOLD + "YinwuChat Version "+ plugin.getDescription().getVersion() + ",Author:"+plugin.getDescription().getAuthor()));
        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GOLD + "插件帮助："));
        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GOLD + "插件基本命令为&b/yinwuchat&6或&b/yw&6"));
        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GOLD + "绑定：&b/yinwuchat bind <token>&6，token为web端获取"));
        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GOLD + "查询：&b/yinwuchat list&6，可以查询到你绑定的所有token"));
        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GOLD + "解绑：&b/yinwuchat unbind <token>"));
        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GOLD + "可以解绑对应的token，token为查询结果中的token,可以只输入前面的部分"));
        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GOLD + "发送私聊消息：&b/msg <player_name> <message>&6，例：&b/msg LinTx 一条私聊消息"));
        if (Config.getInstance().allowPlayerFormatPrefixSuffix){
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GOLD + "查看/设置聊天前后缀：&b/yinwuchat format public/private prefix/suffix view/set [prefix/suffix]"));
        }
        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GOLD + "被@时静音：&b/yinwuchat muteat"));
        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GOLD + "阻止自己被@：&b/yinwuchat noat&6（无法阻止被管理@全体）"));
        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GOLD + "忽略某人的消息：&b/yinwuchat ignore <player_name>&6（再输入一次不再忽略）"));
        if (sender.hasPermission(Const.PERMISSION_RELOAD)){
            sender.sendMessage(MessageUtil.newTextComponent("&c重新加载插件：&b/yinwuchat reload [config|ws]&c重新加载插件/插件配置/WebSocket"));
        }
        if (sender.hasPermission(Const.PERMISSION_BAD_WORD)){
            sender.sendMessage(MessageUtil.newTextComponent("&c聊天关键词管理：&b/yinwuchat badword <add|remove|list> [word]"));
        }
        if (sender.hasPermission(Const.PERMISSION_VANISH)){
            sender.sendMessage(MessageUtil.newTextComponent("&c聊天隐身：&b/yinwuchat vanish"));
        }
        if (sender.hasPermission(Const.PERMISSION_MONITOR_PRIVATE_MESSAGE)){
            sender.sendMessage(MessageUtil.newTextComponent("&c切换是否监听其他玩家私聊信息：&b/yinwuchat monitor"));
        }
    }

    private void reload(CommandSender sender, String[] args){
        sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + "YinwuChat插件重载"));
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
}
