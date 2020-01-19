package org.lintx.plugins.yinwuchat.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.*;
import io.netty.channel.Channel;
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

import java.util.*;

public class MessageManage {
    private static YinwuChat plugin;
    private static MessageManage instance = new MessageManage();
    private static final Config config = Config.getInstance();
    private static List<ChatHandle> handles = new ArrayList<>();
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

    private void monitorPrivateMessage(TextComponent textComponent,String fromPlayer,String toPlayer){
        for (ProxiedPlayer p:plugin.getProxy().getPlayers()){
            if (!p.hasPermission(Const.PERMISSION_MONITOR_PRIVATE_MESSAGE)) continue;
            if (p.getName().equalsIgnoreCase(fromPlayer) || p.getName().equalsIgnoreCase(toPlayer)) continue;
            PlayerConfig.Player playerConfig = PlayerConfig.getConfig(p);
            if (playerConfig.monitor){
                p.sendMessage(textComponent);
            }
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
                        list.add(ComponentSerializer.parse(s)[0]);
                    }
                }
            } catch (Exception ignored) {

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

                for (ChatHandle handle:handles){
                    handle.handle(chat);
                }
                TextComponent messageComponent = chat.buildPublicMessage(publicMessage.format);

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

                BungeeChatPlayer toPlayer = getPrivateMessageToPlayer(privateMessage.toPlayer);
                if (toPlayer.redisPlayerName==null){
                    if (toPlayer.playerName == null) {
                        player.sendMessage(new TextComponent(MessageUtil.replace(config.tipsConfig.toPlayerNoOnlineTip)));
                        return;
                    }
                    if (toPlayer.playerName.equalsIgnoreCase(privateMessage.player)) {
                        player.sendMessage(new TextComponent(MessageUtil.replace(config.tipsConfig.msgyouselfTip)));
                        return;
                    }
                    if (toPlayer.config.isIgnore(player)) {
                        player.sendMessage(new TextComponent(MessageUtil.replace(config.tipsConfig.ignoreTip)));
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
                if (toPlayer.player != null) {
                    toPlayer.player.sendMessage(fromComponent);
                }
                if (toPlayer.channel != null) {
                    sendWebMessage(toPlayer.channel, toWebMessage(fromComponent));
                }
                if (toPlayer.redisPlayerName != null){
                    RedisUtil.sendMessage(player.getUniqueId(),fromComponent,toPlayer.redisPlayerName);
                }
                monitorPrivateMessage(monitorComponent,privateMessage.player,toPlayer.playerName);
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
        try {
            if (YinwuChat.getBatManage().isMute(player,player.getServer().getInfo().getName())){
                player.sendMessage(new TextComponent(MessageUtil.replace(config.tipsConfig.youismuteTip)));
                return true;
            }
        }
        catch (Exception ignored){}
        try {
            if (YinwuChat.getBatManage().isBan(player,player.getServer().getInfo().getName())){
                player.sendMessage(new TextComponent(MessageUtil.replace(config.tipsConfig.youisbanTip)));
                return true;
            }
        }
        catch (Exception ignored){}
        return false;
    }

    //判断web端登录的玩家是否允许发送消息（判断是否被禁言）
    private boolean cantMessage(String player, Channel channel){
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
    private BungeeChatPlayer getPrivateMessageToPlayer(String name){
        BungeeChatPlayer bungeeChatPlayer = new BungeeChatPlayer();
        ProxiedPlayer toPlayer = null;
        ProxiedPlayer findPlayer = null;
        name = name.toLowerCase(Locale.ROOT);
        for (ProxiedPlayer p:plugin.getProxy().getPlayers()){
            PlayerConfig.Player playerConfig = PlayerConfig.getConfig(p);
            if (playerConfig.vanish){
                continue;
            }
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

        ShieldedManage.Result result = ShieldedManage.getInstance().checkShielded(channel,util.getUuid().toString(),message);
        if (result.kick){
            return;
        }
        if (result.shielded){
            if (result.end){
                return;
            }
            message = result.msg;
        }

        BungeeChatPlayer toPlayer = getPrivateMessageToPlayer(toName);
        if (toPlayer.redisPlayerName==null){
            if (toPlayer.playerName == null) {
                sendWebMessage(channel, OutputServerMessage.errorJSON(config.tipsConfig.toPlayerNoOnlineTip).getJSON());
                return;
            }
            if (toPlayer.playerName.equalsIgnoreCase(playerConfig.name)) {
                sendWebMessage(channel, OutputServerMessage.errorJSON(config.tipsConfig.msgyouselfTip).getJSON());
                return;
            }
            if (toPlayer.config.isIgnore(util.getUuid())) {
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

        sendWebMessage(channel, toWebMessage(toComponent));
        if (toPlayer.player!=null){
            toPlayer.player.sendMessage(fromComponent);
        }
        if (toPlayer.channel!=null){
            sendWebMessage(toPlayer.channel, toWebMessage(fromComponent));
        }
        if (toPlayer.redisPlayerName != null){
            RedisUtil.sendMessage(util.getUuid(),fromComponent,toPlayer.redisPlayerName);
        }

        //监听消息
        monitorPrivateMessage(monitorComponent,playerConfig.name,toPlayer.playerName);
        plugin.getLogger().info(monitorComponent.toPlainText());
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

        ShieldedManage.Result result = ShieldedManage.getInstance().checkShielded(channel,uuid.toString(),message);
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
                    RedisUtil.sendMessage(RedisMessageType.PUBLIC_MESSAGE,null,messageComponent,"",server);
                }
            }
        }
    }

    //发送广播消息
    private void broadcast(UUID playerUUID, TextComponent component, boolean noqq){
        for (ProxiedPlayer p: plugin.getProxy().getPlayers()){
            PlayerConfig.Player playerConfig = PlayerConfig.getConfig(p);
            if (playerUUID!=null && !p.getUniqueId().equals(playerUUID) && playerConfig.isIgnore(playerUUID)){
                continue;
            }
            sendBcMessage(p,component);
        }

        if (config.redisConfig.openRedis){
            RedisUtil.sendMessage(playerUUID,component);
        }

        String json = toWebMessage(component);
        if (plugin.wsIsOn()){
            for (Channel channel : WsClientHelper.channels()) {
                sendWebMessage(channel, json);
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
    private String toWebMessage(TextComponent component){
        String webmessage = component.toLegacyText();
        JsonObject webjson = new JsonObject();
        webjson.addProperty("action", "send_message");
        webjson.addProperty("message", webmessage);
        return new Gson().toJson(webjson);
    }

    //给一个bc端玩家发送消息
    private void sendBcMessage(ProxiedPlayer player, TextComponent component){
        player.sendMessage(component);
    }

    //给一个web端玩家发送消息
    private void sendWebMessage(Channel channel, String json){
        NettyChannelMessageHelper.send(channel,json);
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
