package org.lintx.plugins.yinwuchat.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.*;
import io.netty.channel.Channel;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.chat.ComponentSerializer;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.Util.MessageUtil;
import org.lintx.plugins.yinwuchat.bungee.config.Config;
import org.lintx.plugins.yinwuchat.bungee.config.PlayerConfig;
import org.lintx.plugins.yinwuchat.bungee.json.InputCoolQ;
import org.lintx.plugins.yinwuchat.bungee.json.OutputCoolQ;
import org.lintx.plugins.yinwuchat.bungee.json.OutputServerMessage;
import org.lintx.plugins.yinwuchat.bungee.httpserver.NettyChannelMessageHelper;
import org.lintx.plugins.yinwuchat.bungee.httpserver.WsClientUtil;
import org.lintx.plugins.yinwuchat.bungee.json.RedisMessageType;
import org.lintx.plugins.yinwuchat.chat.handle.*;
import org.lintx.plugins.yinwuchat.chat.struct.Chat;
import org.lintx.plugins.yinwuchat.chat.struct.BungeeChatPlayer;
import org.lintx.plugins.yinwuchat.chat.struct.ChatSource;
import org.lintx.plugins.yinwuchat.chat.struct.ChatStruct;
import org.lintx.plugins.yinwuchat.json.MessageFormat;
import org.lintx.plugins.yinwuchat.json.PrivateMessage;
import org.lintx.plugins.yinwuchat.json.PublicMessage;
import org.lintx.plugins.yinwuchat.bungee.httpserver.WsClientHelper;
import org.lintx.plugins.yinwuchat.bungee.manage.MuteManage;

import java.util.*;

public class MessageManage {
    private static YinwuChat plugin;
    private static MessageManage instance = new MessageManage();
    private static final Config config = Config.getInstance();
    private static List<ChatHandle> handles = new ArrayList<>();
    private org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore offlineStore;
    static {
//        handles.add(new EmojiHandle());
        handles.add(new CoolQCodeHandle());
        handles.add(new CoolQEscapeHandle());
        handles.add(new ItemShowHandle());
        handles.add(new LinkHandle());
        handles.add(new BungeeAtPlayerHandle());
        handles.add(new StyleSymbolHandle());
        handles.add(new StylePermissionHandle());
        handles.add(new ExtraDataHandle());
    }

    public static void setPlugin(YinwuChat plugin){
        MessageManage.plugin = plugin;
    }

    public static MessageManage getInstance() {
        return instance;
    }

    private org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore getOfflineStore() {
        if (offlineStore == null && plugin != null) {
            offlineStore = new org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore(plugin.getDataFolder());
        }
        return offlineStore;
    }

    private void monitorPrivateMessage(TextComponent textComponent,String fromPlayer,String toPlayer){
        for (ProxiedPlayer p:plugin.getProxy().getPlayers()){
            if (!p.hasPermission(Const.PERMISSION_MONITOR_PRIVATE_MESSAGE)) continue;
            if (p.getName().equalsIgnoreCase(fromPlayer) || p.getName().equalsIgnoreCase(toPlayer)) continue;
            p.sendMessage(textComponent);
        }
    }

    private String handleShielded(ProxiedPlayer player,String message){
        ShieldedManage.Result result = ShieldedManage.getInstance().checkShielded(player, message);
        if (result.kick) {
            return "";
        }
        if (result.shielded) {
            if (result.end) {
                return "";
            }
            return result.msg;
        }
        return message;
    }

    private List<BaseComponent> getItems(List<String> items){
        List<BaseComponent> list = new ArrayList<>();
        if (items != null) {
            try {
                for (String s : items) {
                    if (s == null) {
                        list.add(null);
                    } else {
                        // 直接解析 JSON 组件数据，ModernItemUtil 确保返回兼容格式
                        list.add(ComponentSerializer.parse(s)[0]);
                    }
                }
            } catch (Exception ignored) {
                // 如果解析失败，尝试其他方法
                for (String s : items) {
                    if (s == null) {
                        list.add(null);
                    } else {
                        // 尝试作为普通文本处理
                        list.add(new TextComponent("[物品]"));
                    }
                }
            }
        }
        return list;
    }


    //处理bukkit发送的插件消息（包括公屏消息、私聊消息、请求玩家列表等）
    void handleBukkitMessage(ProxiedPlayer player, ByteArrayDataInput input){
        String subChannel = input.readUTF();
        switch (subChannel) {
            case Const.PLUGIN_SUB_CHANNEL_PUBLIC_MESSAGE: {
                if (cantMessage(player)) {
                    return;
                }
                String json = input.readUTF();
//                System.out.println(json);
                PublicMessage publicMessage = new Gson().fromJson(json, PublicMessage.class);
                if ("".equals(publicMessage.chat)) return;

                BungeeChatPlayer fromPlayer = new BungeeChatPlayer();
                fromPlayer.playerName = player.getName();
                fromPlayer.player = player;
                fromPlayer.config = PlayerConfig.getConfig(player);

                boolean notQQ = false;
                if (!"".equals(Config.getInstance().coolQConfig.gameToCoolqStart)){
                    notQQ = !publicMessage.chat.startsWith(Config.getInstance().coolQConfig.gameToCoolqStart);
                }

                if (config.allowPlayerFormatPrefixSuffix && null!=fromPlayer.config.publicPrefix && !"".equals(fromPlayer.config.publicPrefix)) publicMessage.chat = fromPlayer.config.publicPrefix + publicMessage.chat;
                if (config.allowPlayerFormatPrefixSuffix && null!=fromPlayer.config.publicSuffix && !"".equals(fromPlayer.config.publicSuffix)) publicMessage.chat = publicMessage.chat + fromPlayer.config.publicSuffix;

                String message = handleShielded(player,publicMessage.chat);
                if ("".equals(message)) return;


                ChatStruct struct = new ChatStruct();
                struct.chat = message;
                List<ChatStruct> list = new ArrayList<>();
                list.add(struct);

                Chat chat = new Chat(fromPlayer,list, ChatSource.GAME);
                chat.extraData = publicMessage.handles;
                chat.items = getItems(publicMessage.items);
//                System.out.println(publicMessage.items.get(0));
//                System.out.println(chat.items.get(0).toLegacyText());

                for (ChatHandle handle:handles){
                    handle.handle(chat);
                }
                TextComponent messageComponent = chat.buildPublicMessage(publicMessage.format);
//                messageComponent.setColor(ChatColor.of("#123456"));

                broadcast(player.getUniqueId(), messageComponent, notQQ);
                plugin.getLogger().info(messageComponent.toPlainText());
                break;
            }
            case Const.PLUGIN_SUB_CHANNEL_PRIVATE_MESSAGE: {
                if (cantMessage(player)) {
                    return;
                }
                String json = input.readUTF();
                PrivateMessage privateMessage = new Gson().fromJson(json, PrivateMessage.class);
                if ("".equals(privateMessage.chat)) return;

                BungeeChatPlayer fromPlayer = new BungeeChatPlayer();
                fromPlayer.playerName = player.getName();
                fromPlayer.player = player;
                fromPlayer.config = PlayerConfig.getConfig(player);

                if (config.allowPlayerFormatPrefixSuffix && null!=fromPlayer.config.privatePrefix && !"".equals(fromPlayer.config.privatePrefix)) privateMessage.chat = fromPlayer.config.privatePrefix + privateMessage.chat;
                if (config.allowPlayerFormatPrefixSuffix && null!=fromPlayer.config.privateSuffix && !"".equals(fromPlayer.config.privateSuffix)) privateMessage.chat = privateMessage.chat + fromPlayer.config.privateSuffix;

                BungeeChatPlayer toPlayer = getPrivateMessageToPlayer(privateMessage.toPlayer, player.getName());
                UUID targetUuid = toPlayer.uuid;

                if (toPlayer.redisPlayerName==null){
                    if (toPlayer.playerName == null && targetUuid == null) {
                        // 存入离线消息
                        org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore store = getOfflineStore();
                        if (store != null) {
                            org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore.OfflineMessage offline =
                                new org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore.OfflineMessage();
                            offline.from = player.getName();
                            offline.to = privateMessage.toPlayer;
                            offline.message = privateMessage.chat;
                            offline.time = System.currentTimeMillis();
                            store.addMessage(privateMessage.toPlayer, offline);
                        }
                        player.sendMessage(MessageUtil.newTextComponent(ChatColor.YELLOW + "玩家不在线，消息已存为离线留言: " + privateMessage.toPlayer));
                        return;
                    }
                    if (toPlayer.playerName != null && toPlayer.playerName.equalsIgnoreCase(privateMessage.player)) {
                        player.sendMessage(MessageUtil.newTextComponent(MessageUtil.replace(config.tipsConfig.msgyouselfTip)));
                        return;
                    }
                    if (toPlayer.config != null && toPlayer.config.isIgnore(player)) {
                        player.sendMessage(MessageUtil.newTextComponent(MessageUtil.replace(config.tipsConfig.ignoreTip)));
                        return;
                    }
                }
                else {
                    toPlayer.playerName = toPlayer.redisPlayerName;
                }

                String message = handleShielded(player,privateMessage.chat);
                if ("".equals(message)) return;


                ChatStruct struct = new ChatStruct();
                struct.chat = message;
                List<ChatStruct> list = new ArrayList<>();
                list.add(struct);

                Chat chat = new Chat(fromPlayer,toPlayer,list,ChatSource.GAME);
                chat.extraData = privateMessage.handles;
                chat.items = getItems(privateMessage.items);

                for (ChatHandle handle:handles){
                    handle.handle(chat);
                }

                TextComponent toComponent = chat.buildPrivateToMessage(privateMessage.toFormat);
                TextComponent fromComponent = chat.buildPrivateFormMessage(privateMessage.fromFormat);
                TextComponent monitorComponent = chat.buildPrivateMonitorMessage(config.formatConfig.monitorFormat);

                player.sendMessage(toComponent);
                
                // 同步到发送者的 Web 端
                syncPrivateMessageToWeb(player.getUniqueId(), player.getName(), toPlayer.playerName != null ? toPlayer.playerName : privateMessage.toPlayer, message, true);

                // 发送给接收者 (游戏内)
                if (toPlayer.player != null) {
                    toPlayer.player.sendMessage(fromComponent);
                }
                
                // 同步到接收者的所有 Web 端
                if (targetUuid != null) {
                    syncPrivateMessageToWeb(targetUuid, player.getName(), toPlayer.playerName != null ? toPlayer.playerName : privateMessage.toPlayer, message, false);
                } else if (toPlayer.channel != null) {
                    // 后备方案：如果找不到 UUID 但有 channel
                    sendWebMessage(toPlayer.channel, toWebMessage(fromComponent, toPlayer.channel, player.getUniqueId(), player.getName(), null));
                }
                
                if (toPlayer.redisPlayerName != null){
                    RedisUtil.sendMessage(player.getUniqueId(),fromComponent,toPlayer.redisPlayerName);
                }
                monitorPrivateMessage(monitorComponent,privateMessage.player,toPlayer.playerName != null ? toPlayer.playerName : privateMessage.toPlayer);
                plugin.getLogger().info(monitorComponent.toPlainText());
                break;
            }
            case Const.PLUGIN_SUB_CHANNEL_PLAYER_LIST:
                sendPlayerListToServer(player.getServer());
                break;
        }
    }

    //判断bc端登录的玩家是否允许发送消息（判断是否被禁言）
    private boolean cantMessage(ProxiedPlayer player){
        if (MuteManage.getInstance().checkMutedAndNotify(player)) {
            return true;
        }
        try {
            if (YinwuChat.getBatManage().isMute(player,player.getServer().getInfo().getName())){
                player.sendMessage(MessageUtil.newTextComponent(MessageUtil.replace(config.tipsConfig.youismuteTip)));
                return true;
            }
        }
        catch (Exception ignored){}
        try {
            if (YinwuChat.getBatManage().isBan(player,player.getServer().getInfo().getName())){
                player.sendMessage(MessageUtil.newTextComponent(MessageUtil.replace(config.tipsConfig.youisbanTip)));
                return true;
            }
        }
        catch (Exception ignored){}
        return false;
    }

    //判断web端登录的玩家是否允许发送消息（判断是否被禁言）
    private boolean cantMessage(String player, Channel channel){
        ProxiedPlayer proxiedPlayer = plugin.getProxy().getPlayer(player);
        if (proxiedPlayer != null) {
            if (MuteManage.getInstance().isMuted(proxiedPlayer)) {
                MuteManage.getInstance().checkMutedAndNotify(proxiedPlayer);
                // 同时通知 Web 端
                PlayerConfig.Player settings = PlayerConfig.getConfig(proxiedPlayer);
                String tip = config.tipsConfig.youismuteTip;
                long rem = settings.getRemainingMuteTime();
                if (rem == -1) tip += " (永久禁言)";
                else if (rem > 0) tip += " (剩余: " + MuteManage.formatTime(rem) + ")";
                sendWebMessage(channel, OutputServerMessage.errorJSON(tip).getJSON());
                return true;
            }
        }
        try {
            if (YinwuChat.getBatManage().isMute(player,config.webBATserver)){
                sendWebMessage(channel, OutputServerMessage.errorJSON(MessageUtil.replace(config.tipsConfig.youismuteTip)).getJSON());
                return true;
            }
        }
        catch (Exception ignored){}
        try {
            if (YinwuChat.getBatManage().isBan(player,config.webBATserver)){
                sendWebMessage(channel, OutputServerMessage.errorJSON(MessageUtil.replace(config.tipsConfig.youisbanTip)).getJSON());
                return true;
            }
        }
        catch (Exception ignored){}
        return false;
    }

    //根据一个名字查找对应的玩家并返回一个私聊消息配置（忽略大小写、前缀匹配）
    private BungeeChatPlayer getPrivateMessageToPlayer(String name, String viewerName){
        BungeeChatPlayer bungeeChatPlayer = new BungeeChatPlayer();
        ProxiedPlayer toPlayer = null;
        ProxiedPlayer findPlayer = null;
        name = name.toLowerCase(Locale.ROOT);
        // 注意：现在允许非管理玩家向隐身玩家发送消息，所以移除 vanish 状态的过滤

        for (ProxiedPlayer p:plugin.getProxy().getPlayers()){
            String pn = p.getName().toLowerCase(Locale.ROOT);
            if (pn.equals(name)){
                toPlayer = p;
                break;
            }
            if (pn.startsWith(name)){
                findPlayer = p;
            }
        }
        if (toPlayer==null){
            toPlayer = findPlayer;
        }
        String toPlayerName = null;
        WsClientUtil toUtil = null;
        if (plugin.wsIsOn()) {
            String findPlayerName = null;
            WsClientUtil findUtil = null;
            for (WsClientUtil util : WsClientHelper.utils()) {
                if (util.getUuid()==null){
                    continue;
                }
                PlayerConfig.Player playerConfig = PlayerConfig.getConfig(util.getUuid());
                if (playerConfig.name == null || playerConfig.name.equals("")) {
                    continue;
                }
                
                String pn = playerConfig.name.toLowerCase(Locale.ROOT);
                if (pn.equals(name)) {
                    toPlayerName = playerConfig.name;
                    toUtil = util;
                    break;
                }
                if (pn.startsWith(name)) {
                    findUtil = util;
                    findPlayerName = playerConfig.name;
                }
            }
            if (toUtil == null && findUtil!=null) {
                toPlayerName = findPlayerName;
                toUtil = findUtil;
            }
            if (toUtil!=null && toUtil.getUuid()!=null){
                bungeeChatPlayer.config = PlayerConfig.getConfig(toUtil.getUuid());
            }
        }

        if (toPlayerName==null && toPlayer!=null) {
            toPlayerName = toPlayer.getName();
            bungeeChatPlayer.config = PlayerConfig.getConfig(toPlayer);
        }
        if ("".equals(toPlayerName)){
            toPlayerName = null;
        }

        bungeeChatPlayer.player = toPlayer;
        bungeeChatPlayer.playerName = toPlayerName;
        if (toPlayer != null) {
            bungeeChatPlayer.uuid = toPlayer.getUniqueId();
        } else if (toUtil != null) {
            bungeeChatPlayer.uuid = toUtil.getUuid();
        }
        
        if (toUtil!=null){
            Channel channel = WsClientHelper.getWebSocketAsUtil(toUtil);
            if (channel!=null){
                bungeeChatPlayer.channel = channel;
            }
        }

        if (bungeeChatPlayer.playerName==null){
            if (config.redisConfig.openRedis) {
                String findPlayerName = null;
                for (String rpn : RedisUtil.playerList.keySet()){
                    String pn = rpn.toLowerCase(Locale.ROOT);
                    if (pn.equals(name)) {
                        toPlayerName = rpn;
                        break;
                    }
                    if (pn.startsWith(name)) {
                        findPlayerName = rpn;
                    }
                }
                if (toPlayerName == null && findPlayerName!=null) {
                    toPlayerName = findPlayerName;
                }
                if (toPlayerName!=null){
                    bungeeChatPlayer.redisPlayerName = toPlayerName;
                }
            }
        }

        return bungeeChatPlayer;
    }

    //web端发送私聊消息的处理
    public void handleWebPrivateMessage(Channel channel, WsClientUtil util, String toName, String message){
        PlayerConfig.Player playerConfig = PlayerConfig.getConfig(util.getUuid());
        if (playerConfig.name==null || playerConfig.name.equals("")){
            sendWebMessage(channel, OutputServerMessage.errorJSON("你可能没有绑定token").getJSON());
            return;
        }

        if (cantMessage(playerConfig.name, channel)){
            return;
        }

        if (config.allowPlayerFormatPrefixSuffix && null!=playerConfig.privatePrefix && !"".equals(playerConfig.privatePrefix)) message = playerConfig.privatePrefix + message;
        if (config.allowPlayerFormatPrefixSuffix && null!=playerConfig.privateSuffix && !"".equals(playerConfig.privateSuffix)) message = message + playerConfig.privateSuffix;

        // 屏蔽词检查（带账户名，用于 Web 端封禁）
        String senderAccount = util.getAccount();
        ShieldedManage.Result result = ShieldedManage.getInstance().checkShielded(channel,util.getUuid().toString(),senderAccount,message);
        if (result.kick){
            return;
        }
        if (result.shielded){
            if (result.end){
                return;
            }
            message = result.msg;
        }

        BungeeChatPlayer toPlayer = getPrivateMessageToPlayer(toName, playerConfig.name);
        UUID targetUuid = toPlayer.uuid;

        if (toPlayer.redisPlayerName==null){
            if (toPlayer.playerName == null && targetUuid == null) {
                org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore store = getOfflineStore();
                if (store != null) {
                    org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore.OfflineMessage offline =
                        new org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore.OfflineMessage();
                    offline.from = playerConfig.name;
                    offline.to = toName;
                    offline.message = message;
                    offline.time = System.currentTimeMillis();
                    store.addMessage(toName, offline);
                }
                sendWebMessage(channel, OutputServerMessage.infoJSON("玩家不在线，消息已留存").getJSON());
                
                // 同步到发送者的所有 Web 端
                syncPrivateMessageToWeb(util.getUuid(), playerConfig.name, toName, message, true);
                return;
            }
            if (toPlayer.playerName != null && toPlayer.playerName.equalsIgnoreCase(playerConfig.name)) {
                sendWebMessage(channel, OutputServerMessage.errorJSON(config.tipsConfig.msgyouselfTip).getJSON());
                return;
            }
            if (toPlayer.config != null && toPlayer.config.isIgnore(util.getUuid())) {
                sendWebMessage(channel, OutputServerMessage.errorJSON(config.tipsConfig.ignoreTip).getJSON());
                return;
            }
        }
        else {
            toPlayer.playerName = toPlayer.redisPlayerName;
        }

        if ("".equals(message)) return;

        BungeeChatPlayer fromPlayer = new BungeeChatPlayer();
        fromPlayer.playerName = playerConfig.name;
        fromPlayer.config = playerConfig;


        ChatStruct struct = new ChatStruct();
        struct.chat = message;
        List<ChatStruct> list = new ArrayList<>();
        list.add(struct);

        Chat chat = new Chat(fromPlayer,toPlayer,list,ChatSource.WEB);

        for (ChatHandle handle:handles){
            handle.handle(chat);
        }

        TextComponent toComponent = chat.buildPrivateToMessage(config.formatConfig.toFormat);
        TextComponent fromComponent = chat.buildPrivateFormMessage(config.formatConfig.fromFormat);
        TextComponent monitorComponent = chat.buildPrivateMonitorMessage(config.formatConfig.monitorFormat);

        // 同步到发送者的所有 Web 端
        syncPrivateMessageToWeb(util.getUuid(), playerConfig.name, toPlayer.playerName != null ? toPlayer.playerName : toName, message, true);
        
        // 发送者如果在线（游戏内），同步到游戏端
        ProxiedPlayer fromPlayerOnline = plugin.getProxy().getPlayer(playerConfig.name);
        if (fromPlayerOnline != null) {
            fromPlayerOnline.sendMessage(toComponent);
        }
        
        // 发送给接收者 (游戏内)
        if (toPlayer.player!=null){
            toPlayer.player.sendMessage(fromComponent);
        }
        
        // 同步到接收者的所有 Web 端
        if (targetUuid != null) {
            syncPrivateMessageToWeb(targetUuid, playerConfig.name, toPlayer.playerName != null ? toPlayer.playerName : toName, message, false);
        } else if (toPlayer.channel != null) {
            // 后备方案：如果找不到 UUID 但有 channel
            sendWebMessage(toPlayer.channel, toWebMessage(fromComponent, toPlayer.channel, util.getUuid(), playerConfig.name, null));
        }
        
        if (toPlayer.redisPlayerName != null){
            RedisUtil.sendMessage(util.getUuid(),fromComponent,toPlayer.redisPlayerName);
        }

        //监听消息
        monitorPrivateMessage(monitorComponent,playerConfig.name,toPlayer.playerName);
        plugin.getLogger().info(monitorComponent.toPlainText());
    }

    public void deliverOfflineMessagesToPlayer(ProxiedPlayer player) {
        if (player == null) return;
        org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore store = getOfflineStore();
        if (store == null) return;
        List<org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore.OfflineMessage> list =
            store.consumeMessages(player.getName());
        if (list.isEmpty()) return;
        for (org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore.OfflineMessage msg : list) {
            TextComponent text = new TextComponent("[离线私聊] " + msg.from + " -> " + msg.to + ": " + msg.message);
            player.sendMessage(text);
            notifyOfflineRead(msg.from, msg.to);
        }
    }

    private void notifyOfflineRead(String fromPlayer, String toPlayer) {
        if (fromPlayer == null || fromPlayer.isEmpty()) return;
        String tip = "你发给 " + toPlayer + " 的离线留言已读";
        ProxiedPlayer sender = plugin.getProxy().getPlayer(fromPlayer);
        if (sender != null) {
            sender.sendMessage(MessageUtil.newTextComponent(ChatColor.GREEN + tip));
        }
        for (WsClientUtil util : WsClientHelper.utils()) {
            if (util.getUuid() == null) continue;
            PlayerConfig.Player config = PlayerConfig.getConfig(util.getUuid());
            if (config.name != null && config.name.equalsIgnoreCase(fromPlayer)) {
                Channel channel = WsClientHelper.getWebSocketAsUtil(util);
                if (channel != null) {
                    sendWebMessage(channel, OutputServerMessage.infoJSON(tip).getJSON());
                }
            }
        }
    }

    //web端发送广播消息的处理
    public void handleWebPublicMessage(UUID uuid, String message, Channel channel){
        PlayerConfig.Player playerConfig = PlayerConfig.getConfig(uuid);
        if (playerConfig.name==null || playerConfig.name.equals("")){
            sendWebMessage(channel, OutputServerMessage.errorJSON("你可能没有绑定token").getJSON());
            return;
        }

        if (cantMessage(playerConfig.name, channel)){
            return;
        }

        boolean notQQ = false;
        if (!"".equals(Config.getInstance().coolQConfig.gameToCoolqStart)){
            notQQ = !message.startsWith(Config.getInstance().coolQConfig.gameToCoolqStart);
        }

        if (config.allowPlayerFormatPrefixSuffix && null!=playerConfig.publicPrefix && !"".equals(playerConfig.publicPrefix)) message = playerConfig.publicPrefix + message;
        if (config.allowPlayerFormatPrefixSuffix && null!=playerConfig.publicSuffix && !"".equals(playerConfig.publicSuffix)) message = message + playerConfig.publicSuffix;

        // 屏蔽词检查（带账户名，用于 Web 端封禁）
        String accountName = null;
        for (WsClientUtil wsUtil : WsClientHelper.utils()) {
            if (wsUtil != null && uuid.equals(wsUtil.getUuid())) {
                accountName = wsUtil.getAccount();
                break;
            }
        }
        ShieldedManage.Result result = ShieldedManage.getInstance().checkShielded(channel,uuid.toString(),accountName,message);
        if (result.kick){
            return;
        }
        if (result.shielded){
            if (result.end){
                return;
            }
            message = result.msg;
        }

        if ("".equals(message)) return;

        BungeeChatPlayer fromPlayer = new BungeeChatPlayer();
        fromPlayer.playerName = playerConfig.name;
        fromPlayer.config = playerConfig;

        ChatStruct struct = new ChatStruct();
        struct.chat = message;
        List<ChatStruct> list = new ArrayList<>();
        list.add(struct);

        Chat chat = new Chat(fromPlayer,list, ChatSource.WEB);

        for (ChatHandle handle:handles){
            handle.handle(chat);
        }
        TextComponent messageComponent = chat.buildPublicMessage(config.formatConfig.format);
        broadcast(uuid,messageComponent,notQQ);
        plugin.getLogger().info(messageComponent.toPlainText());
    }


    //qq端发送的消息的处理
    public void handleQQMessage(InputCoolQ json){
        if (!config.coolQConfig.coolQQQToGame) return;

        String name = json.getSender().getCard();
        if (name.equals("")){
            name = json.getSender().getNickname();
        }
        name = MessageUtil.removeEmoji(name);

        BungeeChatPlayer fromPlayer = new BungeeChatPlayer();
        fromPlayer.playerName = name;

        ChatStruct struct = new ChatStruct();
        struct.chat = json.getRaw_message().replaceAll("\n"," ").replaceAll("\r"," ");
        List<ChatStruct> list = new ArrayList<>();
        list.add(struct);

        Chat chat = new Chat(fromPlayer,list, ChatSource.QQ);

        for (ChatHandle handle:handles){
            handle.handle(chat);
        }
        TextComponent messageComponent = chat.buildPublicMessage(config.formatConfig.qqFormat);

        if (messageComponent.getExtra()==null || messageComponent.getExtra().size()==0){
            return;
        }

        broadcast(null,messageComponent,true);
        plugin.getLogger().info(messageComponent.toPlainText());
    }

    //定时任务发送广播消息
    public void broadcast(List<MessageFormat> formats,String server){
        Chat chat = new Chat();
        TextComponent messageComponent = new TextComponent();
        for (MessageFormat format:formats){
            if (format.message==null || format.message.equals("")) continue;
            messageComponent.addExtra(chat.buildFormat(format));
        }


        if (server.equalsIgnoreCase("all")){
            broadcast(null,messageComponent,true);
        }else {
            if (server.equalsIgnoreCase("web")){
                if (plugin.wsIsOn()){
                    String json = toWebMessage(messageComponent);
                    for (Channel channel : WsClientHelper.channels()) {
                        sendWebMessage(channel, json);
                    }
                }
            }else {
                for (ProxiedPlayer p: plugin.getProxy().getPlayers()){
                    if (p.getServer().getInfo().getName().equalsIgnoreCase(server)){
                        sendBcMessage(p,messageComponent);
                    }
                }
                if (config.redisConfig.openRedis){
                    RedisUtil.sendMessage(RedisMessageType.TASK,null,messageComponent,"",server);
                }
            }
        }
    }

    //发送广播消息
    private void broadcast(UUID playerUUID, TextComponent component, boolean noqq){
        String senderName = "";
        String serverName = "";
        if (playerUUID != null) {
            ProxiedPlayer p = plugin.getProxy().getPlayer(playerUUID);
            if (p != null) {
                senderName = p.getName();
                if (p.getServer() != null) {
                    serverName = p.getServer().getInfo().getName();
                }
            }
        }
        broadcast(playerUUID, senderName, serverName, component, noqq);
    }

    private void broadcast(UUID playerUUID, String senderName, String serverName, TextComponent component, boolean noqq){
        for (ProxiedPlayer p: plugin.getProxy().getPlayers()){
            PlayerConfig.Player playerConfig = PlayerConfig.getConfig(p);
            if (senderName != null && !senderName.isEmpty() && playerConfig.isIgnore(senderName)){
                continue;
            }
            sendBcMessage(p,component);
        }

        if (config.redisConfig.openRedis){
            RedisUtil.sendMessage(playerUUID,component);
        }

        if (plugin.wsIsOn()){
            for (Channel channel : WsClientHelper.channels()) {
                WsClientUtil util = WsClientHelper.get(channel);
                if (util != null && util.getUuid() != null) {
                    PlayerConfig.Player pc = PlayerConfig.getConfig(util.getUuid());
                    if (senderName != null && !senderName.isEmpty() && pc.isIgnore(senderName)) {
                        continue;
                    }
                }
                sendWebMessage(channel, toWebMessage(component, channel, playerUUID, senderName, serverName));
            }
        }
        if (!noqq && config.coolQConfig.coolQGameToQQ){
            Channel channel = WsClientHelper.getCoolQ();
            if (channel!=null){
                String message = component.toPlainText();
                message = message.replaceAll("§([0-9a-fklmnor])","");
                try {
                    NettyChannelMessageHelper.send(channel,new OutputCoolQ(message).getJSON());
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    //将mc消息转换为web端的消息格式
    private String toWebMessage(TextComponent component, Channel targetChannel, UUID senderUUID, String senderName, String serverName){
        String webmessage = component.toLegacyText();
        JsonObject webjson = new JsonObject();
        webjson.addProperty("action", "send_message");
        webjson.addProperty("message", webmessage);

        if (senderName != null && !senderName.isEmpty()) {
            webjson.addProperty("player", senderName);
        }
        if (serverName != null && !serverName.isEmpty()) {
            webjson.addProperty("server", serverName);
        }

        WsClientUtil util = WsClientHelper.get(targetChannel);
        if (util != null && util.getUuid() != null) {
            if (senderUUID != null && senderUUID.equals(util.getUuid())) {
                webjson.addProperty("is_self", true);
            }
            
            // 检查 Web 用户是否被 @ 提及
            PlayerConfig.Player pc = PlayerConfig.getConfig(util.getUuid());
            if (pc.name != null && !pc.name.isEmpty() && webmessage.contains("@" + pc.name)) {
                webjson.addProperty("mention", true);
            }
        }

        return new Gson().toJson(webjson);
    }

    private String toWebMessage(TextComponent component){
        return toWebMessage(component, null, null, null, null);
    }

    //给一个bc端玩家发送消息
    private void sendBcMessage(ProxiedPlayer player, TextComponent component){
        player.sendMessage(component);
    }

    //给一个web端玩家发送消息
    private void sendWebMessage(Channel channel, String json){
        NettyChannelMessageHelper.send(channel,json);
    }

    private void syncPrivateMessageToWeb(UUID playerUuid, String fromName, String toName, String message, boolean isSelf) {
        if (playerUuid == null) return;
        for (WsClientUtil util : WsClientHelper.utils()) {
            if (util != null && playerUuid.equals(util.getUuid())) {
                Channel channel = WsClientHelper.getWebSocketAsUtil(util);
                if (channel != null && channel.isActive()) {
                    JsonObject json = new JsonObject();
                    json.addProperty("action", "private_message");
                    json.addProperty("player", fromName);
                    json.addProperty("to", toName);
                    json.addProperty("message", message);
                    json.addProperty("is_self", isSelf);
                    sendWebMessage(channel, new Gson().toJson(json));
                }
            }
        }
    }

    //给所有服务器发送玩家列表信息
    void sendPlayerListToServer(){
        byte[] data = getPlayerListByteData();
        for (ServerInfo serverInfo : plugin.getProxy().getServers().values()){
            sendPlayerListToServer(serverInfo,data);
        }
    }

    //获取bc插件消息所用的字节数组格式的玩家列表
    private byte[] getPlayerListByteData(){
        List<String> list = new  ArrayList<>();
        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            list.add(player.getName());
        }
        if (config.redisConfig.openRedis){
            list.addAll(RedisUtil.playerList.keySet());
        }
        String json = new Gson().toJson(list);
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(Const.PLUGIN_SUB_CHANNEL_PLAYER_LIST);
        output.writeUTF(json);
        return output.toByteArray();
    }

    //根据服务器信息发送玩家列表信息
    private void sendPlayerListToServer(ServerInfo server, byte[] data){
        if (server==null) return;
        Collection<ProxiedPlayer> players = server.getPlayers();
        if (players==null || players.isEmpty() || !players.iterator().hasNext()) return;
        ProxiedPlayer player = players.iterator().next();
        sendPlayerListToServer(player.getServer(),data);
    }

    //根据一个和服务器的connect发送玩家列表信息
    void sendPlayerListToServer(Server server){
        sendPlayerListToServer(server, getPlayerListByteData());
    }

    //根据一个和服务器的connect发送玩家列表信息
    private void sendPlayerListToServer(Server server, byte[] data){
        server.sendData(Const.PLUGIN_CHANNEL,data);
    }
}
