package org.lintx.plugins.yinwuchat.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.*;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.chat.ComponentSerializer;
import org.java_websocket.WebSocket;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.bungee.json.CoolQInputJSON;
import org.lintx.plugins.yinwuchat.bungee.json.CoolQOutputJSON;
import org.lintx.plugins.yinwuchat.bungee.json.ServerMessageJSON;
import org.lintx.plugins.yinwuchat.bungee.util.WsClientUtil;
import org.lintx.plugins.yinwuchat.json.MessageFormat;
import org.lintx.plugins.yinwuchat.json.PrivateMessage;
import org.lintx.plugins.yinwuchat.json.PublicMessage;
import org.lintx.plugins.yinwuchat.bungee.util.WsClientHelper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManage {
    private static YinwuChat plugin;
    private static MessageManage instance = new MessageManage();
    private static final Config config = Config.getInstance();

    public static void setPlugin(YinwuChat plugin){
        MessageManage.plugin = plugin;
    }

    public static MessageManage getInstance() {
        return instance;
    }

    //处理bukkit发送的插件消息（包括公屏消息、私聊消息、请求玩家列表等）
    public void bukkitMessage(ProxiedPlayer player, ByteArrayDataInput input){
        String subchannel = input.readUTF();
        if (Const.PLUGIN_SUB_CHANNEL_CHAT.equals(subchannel)){
            if (!canMessage(player)){
                return;
            }
            String json = input.readUTF();
            PublicMessage publicMessage = new Gson().fromJson(json,PublicMessage.class);

            ShieldedManage.Result result = ShieldedManage.getInstance().checkShielded(player,publicMessage.chat);
            if (result.kick){
                return;
            }
            if (result.shielded){
                if (result.end){
                    return;
                }
                publicMessage.chat = result.msg;
            }

            List<BaseComponent> items = new ArrayList<>();
            if (publicMessage.items!=null){
                try {
                    for (String s : publicMessage.items){
                        if (s==null){
                            items.add(null);
                        }else {
                            items.add(ComponentSerializer.parse(s)[0]);
                        }
                    }
                }
                catch (Exception ignored){

                }
            }
            BaseComponent messageComponent = formatMessage(publicMessage.chat,items,player);
            TextComponent textComponent = new TextComponent();
            for (MessageFormat format:publicMessage.format){
                textComponent.addExtra(parseJson(publicMessage.player,messageComponent,format.message,format.hover,format.click));
            }
            broadcast(player,textComponent,false);
            plugin.getLogger().info(textComponent.toPlainText());
        }
        else if (Const.PLUGIN_SUB_CHANNEL_MSG.equals(subchannel)){
            if (!canMessage(player)){
                return;
            }
            String json = input.readUTF();
            PrivateMessage privateMessage = new Gson().fromJson(json,PrivateMessage.class);

            ShieldedManage.Result result = ShieldedManage.getInstance().checkShielded(player,privateMessage.chat);
            if (result.kick){
                return;
            }
            if (result.shielded){
                if (result.end){
                    return;
                }
                privateMessage.chat = result.msg;
            }

            PrivateMessageConf conf = privateMessageConf(privateMessage.toPlayer);
            if (conf.name==null){
                player.sendMessage(new TextComponent(formatMessage(config.toPlayerNoOnlineTip)));
                return;
            }
            if (conf.name.equalsIgnoreCase(privateMessage.player)){
                player.sendMessage(new TextComponent(formatMessage(config.msgyouselfTip)));
                return;
            }
            if (conf.config.isIgnore(player)){
                player.sendMessage(new TextComponent(formatMessage(config.ignoreTip)));
                return;
            }

            List<BaseComponent> items = new ArrayList<>();
            if (privateMessage.items!=null){
                try {
                    for (String s : privateMessage.items){
                        if (s==null){
                            items.add(null);
                        }else {
                            items.add(ComponentSerializer.parse(s)[0]);
                        }
                    }
                }
                catch (Exception ignored){

                }
            }
            BaseComponent messageComponent = formatMessage(privateMessage.chat,items,player);

            TextComponent toComponent = new TextComponent();
            for (MessageFormat format:privateMessage.toFormat){
                toComponent.addExtra(parseJson(conf.name,messageComponent,format.message,format.hover,format.click));
            }
            player.sendMessage(toComponent);

            TextComponent fromComponent = new TextComponent();
            for (MessageFormat format:privateMessage.fromFormat){
                fromComponent.addExtra(parseJson(privateMessage.player,messageComponent,format.message,format.hover,format.click));
            }
            if (conf.player!=null){
                conf.player.sendMessage(fromComponent);
            }
            if (conf.webSocket!=null){
                sendWebMessage(conf.webSocket, getWebMessage(fromComponent));
            }
        }
        else if (Const.PLUGIN_SUB_CHANNEL_PLAYER_LIST.equals(subchannel)){
            sendPlayerList(player.getServer());
        }
    }

    //判断bc端登录的玩家是否允许发送消息（判断是否被禁言）
    private boolean canMessage(ProxiedPlayer player){
        try {
            if (YinwuChat.getBatManage().isMute(player,player.getServer().getInfo().getName())){
                player.sendMessage(new TextComponent(formatMessage(config.youismuteTip)));
                return false;
            }
        }
        catch (Exception ignored){}
        try {
            if (YinwuChat.getBatManage().isBan(player,player.getServer().getInfo().getName())){
                player.sendMessage(new TextComponent(formatMessage(config.youisbanTip)));
                return false;
            }
        }
        catch (Exception ignored){}
        return true;
    }

    //判断web端登录的玩家是否允许发送消息（判断是否被禁言）
    private boolean canMessage(String player,WebSocket webSocket){
        try {
            if (YinwuChat.getBatManage().isMute(player,config.webBATserver)){
                sendWebMessage(webSocket,ServerMessageJSON.errorJSON(formatMessage(config.youismuteTip)).getJSON());
                return false;
            }
        }
        catch (Exception ignored){}
        try {
            if (YinwuChat.getBatManage().isBan(player,config.webBATserver)){
                sendWebMessage(webSocket,ServerMessageJSON.errorJSON(formatMessage(config.youisbanTip)).getJSON());
                return false;
            }
        }
        catch (Exception ignored){}
        return true;
    }

    //根据一个名字查找对应的玩家并返回一个私聊消息配置（忽略大小写、前缀匹配）
    private PrivateMessageConf privateMessageConf(String name){
        PrivateMessageConf conf = new PrivateMessageConf();
        ProxiedPlayer toPlayer = null;
        ProxiedPlayer findPlayer = null;
        name = name.toLowerCase(Locale.ROOT);
        for (ProxiedPlayer p:plugin.getProxy().getPlayers()){
            PlayerConfig.Player playerConfig = PlayerConfig.getConfig(p);
            if (playerConfig.vanish){
                continue;
            }
            String pn = p.getDisplayName().toLowerCase(Locale.ROOT);
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
                conf.config = PlayerConfig.getConfig(toUtil.getUuid());
            }
        }

        if (toPlayerName==null && toPlayer!=null) {
            toPlayerName = toPlayer.getDisplayName();
            conf.config = PlayerConfig.getConfig(toPlayer);
        }
        if ("".equals(toPlayerName)){
            toPlayerName = null;
        }

        conf.player = toPlayer;
        conf.name = toPlayerName;
        if (toUtil!=null){
            WebSocket webSocket = WsClientHelper.getWebSocketAsUtil(toUtil);
            if (webSocket!=null){
                conf.webSocket = webSocket;
            }
        }
        return conf;
    }

    //web端发送私聊消息的处理
    public void webSendPrivateMessage(WebSocket webSocket,WsClientUtil util,String toName,String message){
        PlayerConfig.Player playerConfig = PlayerConfig.getConfig(util.getUuid());
        if (playerConfig.name==null || playerConfig.name.equals("")){
            sendWebMessage(webSocket,ServerMessageJSON.errorJSON("你可能没有绑定token").getJSON());
            return;
        }

        ShieldedManage.Result result = ShieldedManage.getInstance().checkShielded(webSocket,util.getUuid().toString(),message);
        if (result.kick){
            return;
        }
        if (result.shielded){
            if (result.end){
                return;
            }
            message = result.msg;
        }

        if (!canMessage(playerConfig.name,webSocket)){
            return;
        }
        PrivateMessageConf conf = privateMessageConf(toName);

        if (conf.name==null){
            sendWebMessage(webSocket,ServerMessageJSON.errorJSON(config.toPlayerNoOnlineTip).getJSON());
            return;
        }
        if (conf.name.equals(playerConfig.name)){
            sendWebMessage(webSocket,ServerMessageJSON.errorJSON(config.msgyouselfTip).getJSON());
            return;
        }
        if (conf.config.isIgnore(util.getUuid())){
            sendWebMessage(webSocket,ServerMessageJSON.errorJSON(config.ignoreTip).getJSON());
            return;
        }

        BaseComponent messageComponent = formatLink(message);
        TextComponent toComponent = new TextComponent();
        for (MessageFormat format:Config.getInstance().toFormat){
            if (format.message==null || format.message.equals("")) continue;
            toComponent.addExtra(parseJson(conf.name,messageComponent,format.message,format.hover,format.click));
        }
        sendWebMessage(webSocket,getWebMessage(toComponent));

        TextComponent fromComponent = new TextComponent();
        for (MessageFormat format:Config.getInstance().fromFormat){
            fromComponent.addExtra(parseJson(playerConfig.name,messageComponent,format.message,format.hover,format.click));
        }
        if (conf.player!=null){
            conf.player.sendMessage(fromComponent);
        }
        if (conf.webSocket!=null){
            sendWebMessage(conf.webSocket, getWebMessage(fromComponent));
        }
    }

    //web端发送广播消息的处理
    public void webBroadcastMessage(UUID uuid, String message,WebSocket webSocket){
        PlayerConfig.Player playerConfig = PlayerConfig.getConfig(uuid);
        if (playerConfig.name==null || playerConfig.name.equals("")){
            sendWebMessage(webSocket,ServerMessageJSON.errorJSON("你可能没有绑定token").getJSON());
            return;
        }

        ShieldedManage.Result result = ShieldedManage.getInstance().checkShielded(webSocket,uuid.toString(),message);
        if (result.kick){
            return;
        }
        if (result.shielded){
            if (result.end){
                return;
            }
            message = result.msg;
        }

        if (!canMessage(playerConfig.name,webSocket)){
            return;
        }

        BaseComponent messageComponent = formatLink(message);
        TextComponent textComponent = new TextComponent();
        for (MessageFormat format:Config.getInstance().format){
            if (format.message==null || format.message.equals("")) continue;
            textComponent.addExtra(parseJson(playerConfig.name,messageComponent,format.message,format.hover,format.click));
        }
        broadcast(null,textComponent,false);
        plugin.getLogger().info(textComponent.toPlainText());
    }

    //移除字符串中的emoji表情
    private String removeEmoji(String str){
        return str.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]","");
    }

    //qq端发送的消息的处理
    public void qqMessage(CoolQInputJSON json){
        if (!config.coolQQQToGame) return;
        TextComponent component = formatCQCode(json);
        if (component.getExtra()==null || component.getExtra().size()==0){
            return;
        }
        TextComponent textComponent = new TextComponent();
        String name = json.getSender().getCard();
        if (name.equals("")){
            name = json.getSender().getNickname();
        }
        name = removeEmoji(name);

        for (MessageFormat format:config.qqFormat){
            if (format.message==null || format.message.equals("")) continue;
            textComponent.addExtra(parseJson(name,component,format.message,format.hover,format.click));
        }
        broadcast(null,textComponent,true);
        plugin.getLogger().info(textComponent.toPlainText());
    }

    //定时任务发送广播消息
    public void broadcast(List<MessageFormat> formats,String server){
        TextComponent textComponent = new TextComponent();
        for (MessageFormat format:formats){
            if (format.message==null || format.message.equals("")) continue;
            textComponent.addExtra(parseJson("",null,format.message,format.hover,format.click));
        }
        if (server.equalsIgnoreCase("all")){
            broadcast(null,textComponent,true);
        }else {
            if (server.equalsIgnoreCase("web")){
                if (plugin.wsIsOn()){
                    String json = getWebMessage(textComponent);
                    for (WebSocket webSocket : WsClientHelper.clients()) {
                        sendWebMessage(webSocket, json);
                    }
                }
            }else {
                for (ProxiedPlayer p: plugin.getProxy().getPlayers()){
                    if (p.getServer().getInfo().getName().equalsIgnoreCase(server)){
                        sendBcMessage(p,textComponent);
                    }
                }
            }
        }
    }

    //发送广播消息
    public void broadcast(ProxiedPlayer player,TextComponent component,boolean noqq){
        for (ProxiedPlayer p: plugin.getProxy().getPlayers()){
            PlayerConfig.Player playerConfig = PlayerConfig.getConfig(p);
            if (player!=null && !p.equals(player) && playerConfig.isIgnore(player)){
                continue;
            }
            sendBcMessage(p,component);
        }
        String json = getWebMessage(component);
        if (plugin.wsIsOn()){
            for (WebSocket webSocket : WsClientHelper.clients()) {
                sendWebMessage(webSocket, json);
            }
        }
        if (!noqq && config.coolQGameToQQ){
            WebSocket socket = WsClientHelper.getCoolQ();
            if (socket!=null){
                String message = component.toPlainText();
                message = message.replaceAll("§([0-9a-fklmnor])","");
                try {
                    socket.send(new CoolQOutputJSON(message).getJSON());
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    //将mc消息转换为web端的消息格式
    private String getWebMessage(BaseComponent component){
        String webmessage = component.toLegacyText();
        JsonObject webjson = new JsonObject();
        webjson.addProperty("action", "send_message");
        webjson.addProperty("message", webmessage);
        String json = new Gson().toJson(webjson);
        return json;
    }

    //给一个bc端玩家发送消息
    public void sendBcMessage(ProxiedPlayer player,TextComponent component){
        player.sendMessage(component);
    }

    //个一个web端玩家发送消息
    public void sendWebMessage(WebSocket webSocket,String json){
        webSocket.send(json);
    }

    //将消息按照config中的消息块转换为bc端消息
    private BaseComponent parseJson(String playerName, BaseComponent messageComponent,String message,String hover,String click){
        TextComponent textComponent = new TextComponent();
        if (message!=null && !message.equals("")){
            message = formatMessage(message);
            message = message.replaceAll("\\{displayName}",playerName);

            if (messageComponent!=null && message.contains("{message}")){
                int index = message.indexOf("{message}");
                String msg1 = message.substring(0,index);
                String msg2 = message.substring(index+"{message}".length());
                textComponent.setText(msg1);
                textComponent.addExtra(messageComponent);
                textComponent.addExtra(msg2);
            }
            else {
                textComponent.setText(message);
            }
        }
        if (hover!=null && !hover.equals("")){
            hover = formatMessage(hover);
            hover = hover.replaceAll("\\{displayName}",playerName);
            HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_TEXT,new BaseComponent[]{new TextComponent(hover)});
            textComponent.setHoverEvent(event);
        }
        if (click!=null && !click.equals("")){
            click = click.replaceAll("\\{displayName}",playerName);
            Pattern pattern = Pattern.compile(config.linkRegex);
            Matcher matcher = pattern.matcher(click);
            ClickEvent.Action action = matcher.find() ? ClickEvent.Action.OPEN_URL : ClickEvent.Action.SUGGEST_COMMAND;
            ClickEvent event = new ClickEvent(action,click);
            textComponent.setClickEvent(event);
        }
        return textComponent;
    }

    //将消息bukkit端发送的文本消息和物品转换为bc端消息
    private BaseComponent formatMessage(String message,List<BaseComponent> items,ProxiedPlayer player){
        TextComponent textComponent = new TextComponent();
        message = formatAt(message,player);
        if(items!=null && items.size()>0){
            textComponent = formatItem(message,items);
        }
        else {
            textComponent = formatLink(message);
        }
        return textComponent;
    }

    //将qq端发送的文本消息处理（反转义&[]）
    private String formatQQMessage(String message){
        return message.replaceAll("&amp;","& ").replaceAll("&#91;","[").replaceAll("&#93;","]");
    }

    //处理qq端发送的文本消息（主要处理cq码）
    private TextComponent formatCQCode(CoolQInputJSON json){
        String regex = "\\[CQ:(.*?),(.*?)\\]";
        String message = json.getRaw_message().replaceAll("\n"," ").replaceAll("\r"," ");
        message = removeEmoji(message);

        TextComponent textComponent = new TextComponent();
//        Pattern pattern = Pattern.compile("\\[i([:：](\\d+))?\\]");
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()){
            String str = matcher.group(0);
            if (str==null){
                break;
            }
            String[] arr = message.split(regex,2);
            if (arr.length==0){
                message = "";
            }
            String a1 = formatQQMessage(arr[0]);
            if (arr.length==1){
                if (!message.startsWith(str)){
                    textComponent.addExtra(formatLink(a1));
                    message = "";
                }
            }
            else if (arr.length==2){
                message = arr[1];
                textComponent.addExtra(formatLink(a1));
            }
            else {
                message = String.join(str,new ArrayList<>(Arrays.asList(arr).subList(1, arr.length)));
                textComponent.addExtra(formatLink(a1));
            }
            String func = matcher.group(1);
            String ext = matcher.group(2);

            if (func.equalsIgnoreCase("image")){
                textComponent.addExtra(formatMessage(config.qqImageText));
            }else if (func.equalsIgnoreCase("record")){
                textComponent.addExtra(formatMessage(config.qqRecordText));
            }else if (func.equalsIgnoreCase("at")){
                textComponent.addExtra(formatMessage(config.qqAtText.replaceAll("\\{qq\\}",ext.replaceAll("qq=",""))));
            }else if (func.equalsIgnoreCase("share")){
                String url = "";
                String[] a = ext.split(",");
                for (String kv : a) {
                    String[] b = kv.split("=",2);
                    if (b.length==2 && b[0].equalsIgnoreCase("url")){
                        url = b[1];
                        break;
                    }
                }
                TextComponent linkComponent = new TextComponent(formatMessage(config.linkText));
                if (!"".equals(url)){
                    HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT,new BaseComponent[]{new TextComponent(url)});
                    linkComponent.setHoverEvent(hoverEvent);
                    ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL,url);
                    linkComponent.setClickEvent(clickEvent);
                }
                textComponent.addExtra(linkComponent);
            }

            if (arr.length==1 && message.startsWith(str)){
                textComponent.addExtra(formatLink(a1));
                message = "";
            }
            if (message.equals("")){
                break;
            }
            matcher = pattern.matcher(message);
        }
        if (!message.equals("")){
            textComponent.addExtra(formatLink(formatQQMessage(message)));
        }
        return textComponent;
    }

    //处理bukkit端发送的消息中的物品展示部分
    private TextComponent formatItem(String message,List<BaseComponent> items){
        TextComponent textComponent = new TextComponent();
        Pattern pattern = Pattern.compile(Const.ITEM_PLACEHOLDER);
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()){
            String str = matcher.group(0);
            if (str==null){
                break;
            }
            String[] arr = message.split(Const.ITEM_PLACEHOLDER,2);
            if (arr.length==0){
                message = "";
            }
            else if (arr.length==1){
                if (!message.startsWith(str)){
                    textComponent.addExtra(formatLink(arr[0]));
                    message = "";
                }
            }
            else if (arr.length==2){
                message = arr[1];
                textComponent.addExtra(formatLink(arr[0]));
            }
            else {
                message = String.join(str,new ArrayList<>(Arrays.asList(arr).subList(1, arr.length)));
                textComponent.addExtra(formatLink(arr[0]));
            }
            if (items.size()>0){
                BaseComponent item = items.remove(0);
                if (item==null){
                    textComponent.addExtra(str);
                }else {
                    textComponent.addExtra(item);
                }
            }

            if (arr.length==1 && message.startsWith(str)){
                textComponent.addExtra(formatLink(arr[0]));
                message = "";
            }
            if (message.equals("")){
                break;
            }
            matcher = pattern.matcher(message);
        }
        if (!message.equals("")){
            textComponent.addExtra(formatLink(message));
        }
        return textComponent;
    }

    //处理消息中的链接部分
    private TextComponent formatLink(String message){
        TextComponent textComponent = new TextComponent();
        Pattern pattern = Pattern.compile(config.linkRegex);
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()){
            String link = matcher.group(0);
            if (link==null){
                break;
            }
            String[] arr = message.split(config.linkRegex,2);
            if (arr.length==0){
                message = "";
            }
            else if (arr.length==1){
                if (!message.startsWith(link)){
                    textComponent.addExtra(formatMessage(arr[0]));
                    message = "";
                }
            }
            else if (arr.length==2){
                message = arr[1];
                textComponent.addExtra(formatMessage(arr[0]));
            }
            else {
                message = String.join(link,new ArrayList<>(Arrays.asList(arr).subList(1, arr.length)));
                textComponent.addExtra(formatMessage(arr[0]));
            }
            TextComponent linkComponent = new TextComponent(formatMessage(config.linkText));
            HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT,new BaseComponent[]{new TextComponent(link)});
            linkComponent.setHoverEvent(hoverEvent);
            ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL,link);
            linkComponent.setClickEvent(clickEvent);
            textComponent.addExtra(linkComponent);

            if (arr.length==1 && message.startsWith(link)){
                textComponent.addExtra(formatMessage(arr[0]));
                message = "";
            }
            if (message.equals("")){
                break;
            }
            matcher = pattern.matcher(message);
        }
        if (!message.equals("")){
            textComponent.addExtra(formatMessage(message));
        }
        return textComponent;
    }

    //将消息中的样式代码所用的&替换为mc锁用的分节符
    String formatMessage(String string){
        string = string.replaceAll("&([0-9a-fklmnor])","§$1");
        return string;
    }

    //处理at消息
    private String formatAt(String message,ProxiedPlayer player){
        if (player.hasPermission(Const.PERMISSION_ATALL)){
            String atall = formatAtAll(message,player);
            if (!atall.equals(message)){
                return atall;
            }
        }
        Pattern pattern = Pattern.compile("@(\\w*?)(?=\\W|$)");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()){
            ProxiedPlayer atPlayer = null;
            ProxiedPlayer findPlayer = null;
            String str = matcher.group(1).toLowerCase(Locale.ROOT);
            if (str.equals("")){
                continue;
            }
            for (ProxiedPlayer p: plugin.getProxy().getPlayers()){
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
                if (atPlayer.equals(player)){
                    atPlayer.sendMessage(new TextComponent(formatMessage(config.atyouselfTip)));
                    continue;
                }

                if (atPlayer(player,atPlayer,false)){
                    message = message.replaceAll(matcher.group(0),"&b@"+atPlayer.getName()+"&r");
                }
            }
        }
        return message;
    }

    //处理at所有人消息
    private String formatAtAll(String message,ProxiedPlayer player){
        Pattern pattern = Pattern.compile("@(\\w*?)("+config.atAllKey+")(?=\\W|$)");
        Matcher matcher = pattern.matcher(message);

        List<String> servers = new ArrayList<>();
        while (matcher.find()){
            String str = matcher.group(1).toLowerCase();
            if (str.equals("")){
                for (ProxiedPlayer p: plugin.getProxy().getPlayers()){
                    if (!p.equals(player)){
                        atPlayer(player,p,true);
                    }
                }
                message = message.replaceAll(matcher.group(0),"&b"+matcher.group(0)+"&r");
                break;
            }
            ServerInfo atServer = null;
            ServerInfo findServer = null;
            for (ServerInfo serverInfo : plugin.getProxy().getServers().values()){
                String sn = serverInfo.getName().toLowerCase(Locale.ROOT);
                if (sn.equalsIgnoreCase(str)){
                    atServer = serverInfo;
                    break;
                }
                if (sn.startsWith(str)){
                    findServer = serverInfo;
                }
            }
            if (atServer==null){
                atServer = findServer;
            }
            if (atServer!=null){
                if (servers.contains(atServer.getName())){
                    continue;
                }
                servers.add(atServer.getName());
                message = message.replaceAll(matcher.group(0),"&b@"+atServer.getName()+matcher.group(2)+"&r");

                for (ProxiedPlayer p: plugin.getProxy().getServerInfo(atServer.getName()).getPlayers()){
                    if (!p.equals(player)){
                        atPlayer(player,p,true);
                    }
                }
            }
        }
        return message;
    }

    //at单个玩家
    private boolean atPlayer(ProxiedPlayer player,ProxiedPlayer atplayer,boolean atall){
        PlayerConfig.Player config = PlayerConfig.getConfig(atplayer);
        if (!atall){
            if (config.isIgnore(player)){
                player.sendMessage(new TextComponent(formatMessage(this.config.ignoreTip)));
                return false;
            }
            if (config.banAt){
                player.sendMessage(new TextComponent(formatMessage(this.config.banatTip)));
                return false;
            }
        }
        if (!player.hasPermission(Const.PERMISSION_COOLDOWN_BYPASS)){
            if (config.isCooldown()){
                player.sendMessage(new TextComponent(formatMessage(this.config.cooldownTip)));
                return false;
            }
            config.updateCooldown();
        }
        atplayer.sendMessage(new TextComponent(formatMessage(this.config.atyouTip.replaceAll("\\{player}",player.getName()))));

        if (config.muteAt){
            return true;
        }
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(Const.PLUGIN_SUB_CHANNEL_AT);
        atplayer.getServer().sendData(Const.PLUGIN_CHANNEL,output.toByteArray());
        return true;
    }

    //给所有服务器发送玩家列表信息
    public void sendPlayerList(){
        byte[] data = getPlayerListData();
        for (ServerInfo serverInfo : plugin.getProxy().getServers().values()){
            sendPlayerList(serverInfo,data);
        }
    }

    //获取bc插件消息所用的字节数组格式的玩家列表
    private byte[] getPlayerListData(){
        List<String> list = new  ArrayList<>();
        Iterator<ProxiedPlayer> iterator = plugin.getProxy().getPlayers().iterator();
        while (iterator.hasNext()){
            ProxiedPlayer player = iterator.next();
            list.add(player.getName());
        }
        String json = new Gson().toJson(list);
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(Const.PLUGIN_SUB_CHANNEL_PLAYER_LIST);
        output.writeUTF(json);
        return output.toByteArray();
    }

    //根据服务器信息发送玩家列表信息
    private void sendPlayerList(ServerInfo server,byte[] data){
        if (server==null) return;
        Collection<ProxiedPlayer> players = server.getPlayers();
        if (players==null || players.isEmpty() || !players.iterator().hasNext()) return;
        ProxiedPlayer player = players.iterator().next();
        sendPlayerList(player.getServer(),data);
    }

    //根据一个和服务器的connect发送玩家列表信息
    public void sendPlayerList(Server server){
        sendPlayerList(server,getPlayerListData());
    }

    //根据一个和服务器的connect发送玩家列表嘻嘻
    public void sendPlayerList(Server server,byte[] data){
        server.sendData(Const.PLUGIN_CHANNEL,data);
    }
}
