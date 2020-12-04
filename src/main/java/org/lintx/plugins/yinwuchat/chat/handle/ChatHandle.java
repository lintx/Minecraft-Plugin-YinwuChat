package org.lintx.plugins.yinwuchat.chat.handle;

import net.md_5.bungee.api.chat.BaseComponent;
import org.lintx.plugins.yinwuchat.Util.MessageUtil;
import org.lintx.plugins.yinwuchat.chat.struct.Chat;
import org.lintx.plugins.yinwuchat.chat.struct.ChatStruct;

import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ChatHandle {
    void handle(Chat chat, String regexp, HandleComponentCallback callback) {
        ListIterator<ChatStruct> iterator = chat.chat.listIterator();
        while (iterator.hasNext()){
            ChatStruct struct = iterator.next();
            Pattern pattern = Pattern.compile(regexp);
            Matcher matcher = pattern.matcher(struct.chat);
            while (matcher.find()){
                String[] splits = struct.chat.split(regexp,2);
                if (splits.length!=2) break;

                BaseComponent component = callback.handle(matcher);
                if (component==null){
                    component = MessageUtil.newTextComponent(matcher.group(0));
                }

                ChatStruct child = new ChatStruct();
                child.chat = splits[0];
                child.component = component;

                iterator.previous();
                iterator.add(child);
                iterator.next();

                struct.chat = splits[1];

                matcher = pattern.matcher(struct.chat);
            }
        }
    }

    void handle(Chat chat, HandleTextCallback callback){
        for (ChatStruct struct : chat.chat) {
            struct.chat = callback.handle(struct.chat);
        }
    }

    public abstract void handle(Chat chat);
}
