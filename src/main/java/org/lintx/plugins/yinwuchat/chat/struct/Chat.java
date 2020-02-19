package org.lintx.plugins.yinwuchat.chat.struct;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.lintx.plugins.yinwuchat.Util.MessageUtil;
import org.lintx.plugins.yinwuchat.bungee.config.Config;
import org.lintx.plugins.yinwuchat.json.HandleConfig;
import org.lintx.plugins.yinwuchat.json.MessageFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Chat {
    public List<ChatStruct> chat;
    public ChatSource source;
    public ChatType type;
    public ChatPlayer fromPlayer;
    private ChatPlayer toPlayer;
    public List<HandleConfig> extraData = new ArrayList<>();
    public List<BaseComponent> items;
    private Config config = Config.getInstance();
    private TextComponent component;

    public Chat(){
        fromPlayer = new ChatPlayer();
    }

    public Chat(ChatPlayer fromPlayer, List<ChatStruct> chat, ChatSource source){
        this(fromPlayer,new ChatPlayer(),chat,source,ChatType.PUBLIC);
    }

    public Chat(ChatPlayer fromPlayer, ChatPlayer toPlayer, List<ChatStruct> chat, ChatSource source){
        this(fromPlayer,toPlayer,chat,source,ChatType.PRIVATE);
    }

    public Chat(ChatPlayer fromPlayer, ChatPlayer toPlayer, List<ChatStruct> chat, ChatSource source, ChatType type){
        this.fromPlayer = fromPlayer;
        this.toPlayer = toPlayer;
        this.chat = chat;
        this.source = source;
        this.type = type;
    }

    //build一个公屏消息
    public TextComponent buildPublicMessage(List<MessageFormat> formats){
        return buildMessage(formats,fromPlayer);
    }

    //build一个别人发给我的私聊消息
    public TextComponent buildPrivateFormMessage(List<MessageFormat> formats){
        return buildMessage(formats,fromPlayer);
    }

    //build一个我发给别人的私聊消息
    public TextComponent buildPrivateToMessage(List<MessageFormat> formats){
        return buildMessage(formats,toPlayer);
    }

    //build一个OP监听的私聊消息
    public TextComponent buildPrivateMonitorMessage(List<MessageFormat> formats){
        return buildMessage(formats);
    }

    //build一个只带一个玩家名的消息
    private TextComponent buildMessage(List<MessageFormat> formats, ChatPlayer player){
        TextComponent textComponent = new TextComponent();
        TextComponent message = build();
        for (MessageFormat format:formats){
            if (format.message==null || "".equals(format.message)) continue;
            TextComponent text = new TextComponent();
            MessageFormat newFormat = handlePlayerName(format, player.playerName);

            buildMessage(textComponent, message, newFormat, text);
        }
        return textComponent;
    }

    //build一个带2个玩家名的消息
    private TextComponent buildMessage(List<MessageFormat> formats){
        TextComponent textComponent = new TextComponent();
        TextComponent message = build();
        for (MessageFormat format:formats){
            if (format.message==null || "".equals(format.message)) continue;
            TextComponent text = new TextComponent();
            MessageFormat newFormat = handlePlayerName(format,fromPlayer.playerName,toPlayer.playerName);

            buildMessage(textComponent, message, newFormat, text);
        }
        return textComponent;
    }

    //build消息核心
    private void buildMessage(TextComponent textComponent, TextComponent message, MessageFormat format, TextComponent text) {
        setComponentEvent(format, text);

        if (format.message!=null && format.message.contains("{message}")){
            int index = format.message.indexOf("{message}");
            String msg1 = format.message.substring(0,index);
            String msg2 = format.message.substring(index+"{message}".length());
            text.setText(msg1);
            text.addExtra(message);
            text.addExtra(msg2);
        }
        else {
            text.setText(format.message);
        }

        textComponent.addExtra(text);
    }

    //build消息本体
    private TextComponent build(){
        if (this.component!=null) return this.component;
        TextComponent component = new TextComponent();
        for (ChatStruct chat:this.chat){
            if (null!=chat.chat && !"".equals(chat.chat)) component.addExtra(chat.chat);
            if (null!=chat.component) component.addExtra(chat.component);
        }
        this.component = component;
        return component;
    }

    //将format build成textcomponent
    public TextComponent buildFormat(MessageFormat format){
        if (format.message==null || "".equals(format.message)) return null;
        TextComponent component = new TextComponent();
        MessageFormat newFormat = handlePlayerName(format);

        setComponentEvent(newFormat, component);

        component.setText(newFormat.message);
        return component;
    }

    //使用发送玩家名来处理format
    private MessageFormat handlePlayerName(MessageFormat format){
        return handlePlayerName(format,fromPlayer.playerName);
    }

    //使用指定玩家名来处理format
    private MessageFormat handlePlayerName(MessageFormat format,String name){
        MessageFormat newFormat = new MessageFormat();
        newFormat.message = format.message.replaceAll("\\{displayName}",name);
        if (format.hover!=null && !"".equals(format.hover))
            newFormat.hover = format.hover.replaceAll("\\{displayName}",name);
        if (format.click!=null && !"".equals(format.click))
            newFormat.click = format.click.replaceAll("\\{displayName}",name);
        return newFormat;
    }

    //指定发送者和接收者来处理format
    private MessageFormat handlePlayerName(MessageFormat format,String fromPlayer,String toPlayer){
        MessageFormat newFormat = new MessageFormat();
        newFormat.message = format.message.replaceAll("\\{formPlayer}",fromPlayer);
        newFormat.message = newFormat.message.replaceAll("\\{toPlayer}",toPlayer);
        if (format.hover!=null && !"".equals(format.hover)){
            newFormat.hover = format.hover.replaceAll("\\{formPlayer}",fromPlayer);
            newFormat.hover = newFormat.hover.replaceAll("\\{toPlayer}",toPlayer);
        }
        if (format.click!=null && !"".equals(format.click)){
            newFormat.click = format.click.replaceAll("\\{formPlayer}",fromPlayer);
            newFormat.click = newFormat.click.replaceAll("\\{toPlayer}",toPlayer);
        }
        return newFormat;
    }

    //给component设置event（hover和click）
    private void setComponentEvent(MessageFormat format, TextComponent text) {
        format.message = MessageUtil.replace(format.message);
        if (null!=format.hover && !"".equals(format.hover)){
            format.hover = MessageUtil.replace(format.hover);
            setHover(text,format.hover);
        }
        if (null!=format.click && !"".equals(format.click)){
//            format.click = MessageUtil.replace(format.click);
            setClick(text,format.click);
        }
    }

    //个component设置hover
    public void setHover(TextComponent component,String hover){
        HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_TEXT,new BaseComponent[]{new TextComponent(hover)});
        component.setHoverEvent(event);
    }

    //给component设置click
    public void setClick(TextComponent component,String click){
        Pattern pattern = Pattern.compile(config.linkRegex);
        Matcher matcher = pattern.matcher(click);
        ClickEvent.Action action;
        if (matcher.find()){
            action = ClickEvent.Action.OPEN_URL;
        }else {
            if (click.startsWith("!")){
                click = click.replaceFirst("^!","");
                action = ClickEvent.Action.RUN_COMMAND;
            }else {
                action = ClickEvent.Action.SUGGEST_COMMAND;
            }
        }
        ClickEvent event = new ClickEvent(action,click);
        component.setClickEvent(event);
    }
}
