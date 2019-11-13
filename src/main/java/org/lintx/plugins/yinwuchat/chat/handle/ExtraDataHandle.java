package org.lintx.plugins.yinwuchat.chat.handle;

import net.md_5.bungee.api.chat.TextComponent;
import org.lintx.plugins.yinwuchat.chat.struct.Chat;
import org.lintx.plugins.yinwuchat.chat.struct.ChatSource;
import org.lintx.plugins.yinwuchat.json.HandleConfig;
import org.lintx.plugins.yinwuchat.json.MessageFormat;


public class ExtraDataHandle extends ChatHandle {

    @Override
    public void handle(Chat chat) {
        if (chat.source!= ChatSource.GAME) return;
        if (chat.extraData==null || chat.extraData.isEmpty()) return;
        for (HandleConfig config:chat.extraData){
            handle(chat, config.placeholder, (matcher) -> {
                TextComponent component = new TextComponent();
                for (MessageFormat format:config.format){
                    TextComponent c = chat.buildFormat(format);
                    if (c!=null) component.addExtra(c);
                }
                if (component.getExtra()==null || component.getExtra().isEmpty()) return null;
                return component;
            });
        }
    }
}
