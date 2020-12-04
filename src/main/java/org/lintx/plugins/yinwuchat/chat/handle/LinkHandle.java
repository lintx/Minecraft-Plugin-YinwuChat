package org.lintx.plugins.yinwuchat.chat.handle;

import net.md_5.bungee.api.chat.TextComponent;
import org.lintx.plugins.yinwuchat.Util.MessageUtil;
import org.lintx.plugins.yinwuchat.bungee.config.Config;
import org.lintx.plugins.yinwuchat.chat.struct.Chat;

public class LinkHandle extends ChatHandle {
    @Override
    public void handle(Chat chat) {
        Config config = Config.getInstance();
        String regexp = config.linkRegex;
        handle(chat, regexp, (matcher) -> {
            String link = matcher.group(0);
            TextComponent component = MessageUtil.newTextComponent(MessageUtil.replace(config.tipsConfig.linkText));
            chat.setHover(component,link);
            chat.setClick(component,link);
            return component;
        });
    }
}
