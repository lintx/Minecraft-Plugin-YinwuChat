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
import net.md_5.bungee.chat.ComponentSerializer;
import org.java_websocket.WebSocket;
import org.lintx.plugins.yinwuchat.Const;
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

    public void bukkitMessage(ProxiedPlayer player, ByteArrayDataInput input){
        String subchannel = input.readUTF();
        if (Const.PLUGIN_SUB_CHANNEL_CHAT.equals(subchannel)){
            if (!canMessage(player)){
                return;
            }
            String json = input.readUTF();
            PublicMessage publicMessage = new Gson().fromJson(json,PublicMessage.class);

            if (config.shieldeds.parallelStream().anyMatch(publicMessage.chat::contains)){
                player.sendMessage(new TextComponent(formatMessage(config.shieldedTip)));
                return;
            }

            BaseComponent item = null;
            if (publicMessage.item!=null){
                try {
                    item = ComponentSerializer.parse(publicMessage.item)[0];
                }
                catch (Exception ignored){

                }
            }
            BaseComponent messageComponent = formatMessage(publicMessage.chat,item,player);
            TextComponent textComponent = new TextComponent();
            for (MessageFormat format:publicMessage.format){
                textComponent.addExtra(parseJson(publicMessage.player,messageComponent,format.message,format.hover,format.click));
            }
            broadcast(player,textComponent);
        }
        else if (Const.PLUGIN_SUB_CHANNEL_MSG.equals(subchannel)){
            if (!canMessage(player)){
                return;
            }
            String json = input.readUTF();
            PrivateMessage privateMessage = new Gson().fromJson(json,PrivateMessage.class);

            if (config.shieldeds.parallelStream().anyMatch(privateMessage.chat::contains)){
                player.sendMessage(new TextComponent(formatMessage(config.shieldedTip)));
                return;
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

            BaseComponent item = null;
            if (privateMessage.item!=null){
                try {
                    item = ComponentSerializer.parse(privateMessage.item)[0];
                }
                catch (Exception ignored){

                }
            }
            BaseComponent messageComponent = formatMessage(privateMessage.chat,item,player);

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
    }

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

    public void webSendPrivateMessage(WebSocket webSocket,WsClientUtil util,String toName,String message){
        PlayerConfig.Player playerConfig = PlayerConfig.getConfig(util.getUuid());
        if (config.shieldeds.parallelStream().anyMatch(message::contains)){
            sendWebMessage(webSocket,ServerMessageJSON.errorJSON(formatMessage(config.shieldedTip)).getJSON());
            return;
        }
        if (playerConfig.name==null || playerConfig.name.equals("")){
            sendWebMessage(webSocket,ServerMessageJSON.errorJSON("你可能没有绑定token").getJSON());
            return;
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

    public void webBroadcastMessage(UUID uuid, String message,WebSocket webSocket){
        if (config.shieldeds.parallelStream().anyMatch(message::contains)){
            sendWebMessage(webSocket,ServerMessageJSON.errorJSON(formatMessage(config.shieldedTip)).getJSON());
            return;
        }
        PlayerConfig.Player playerConfig = PlayerConfig.getConfig(uuid);
        if (!canMessage(playerConfig.name,webSocket)){
            return;
        }

        BaseComponent messageComponent = formatLink(message);
        TextComponent textComponent = new TextComponent();
        for (MessageFormat format:Config.getInstance().format){
            if (format.message==null || format.message.equals("")) continue;
            textComponent.addExtra(parseJson(playerConfig.name,messageComponent,format.message,format.hover,format.click));
        }
        broadcast(null,textComponent);
    }

    public void broadcast(ProxiedPlayer player,TextComponent component){
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
    }

    private String getWebMessage(BaseComponent component){
        String webmessage = component.toLegacyText();
        JsonObject webjson = new JsonObject();
        webjson.addProperty("action", "send_message");
        webjson.addProperty("message", webmessage);
        String json = new Gson().toJson(webjson);
        return json;
    }

    public void sendBcMessage(ProxiedPlayer player,TextComponent component){
        player.sendMessage(component);
    }

    public void sendWebMessage(WebSocket webSocket,String json){
        webSocket.send(json);
    }

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

    private BaseComponent formatMessage(String message,BaseComponent item,ProxiedPlayer player){
        TextComponent textComponent = new TextComponent();
        message = formatAt(message,player);
        if (item!=null && message.contains(Const.ITEM_PLACEHOLDER)){
            int index = message.indexOf(Const.ITEM_PLACEHOLDER);
            String msg1 = message.substring(0,index);
            String msg2 = message.substring(index+Const.ITEM_PLACEHOLDER.length());
            textComponent = formatLink(msg1);
            textComponent.addExtra(item);
            textComponent.addExtra(formatLink(msg2));
        }
        else {
            textComponent = formatLink(message);
        }
        return textComponent;
    }

    private TextComponent formatLink(String message){
        TextComponent textComponent = new TextComponent();
        Pattern pattern = Pattern.compile(config.linkRegex);
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()){
            String link = matcher.group(0);
            if (link==null){
                break;
            }
            String[] arr = message.split(link);
            if (arr.length==0){
                break;
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

    String formatMessage(String string){
        string = string.replaceAll("&([0-9a-fklmnor])","§$1");
        return string;
    }

    private String formatAt(String message,ProxiedPlayer player){
        if (player.hasPermission(Const.PERMISSION_ATALL)){
            String atall = formatAtAll(message,player);
            if (!atall.equals(message)){
                return atall;
            }
        }
        PlayerConfig.Player playerConfig = PlayerConfig.getConfig(player);
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
                String player_name = p.getName().toLowerCase(Locale.ROOT);
                if (!playerConfig.vanish){
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
}
