package org.lintx.plugins.yinwuchat.chat.handle;

import org.lintx.plugins.yinwuchat.Util.MessageUtil;
import org.lintx.plugins.yinwuchat.bungee.config.Config;
import org.lintx.plugins.yinwuchat.chat.struct.Chat;
import org.lintx.plugins.yinwuchat.chat.struct.ChatSource;

public class StylePermissionHandle extends ChatHandle {
    @Override
    public void handle(Chat chat) {
        Config config = Config.getInstance();
        if (chat.source== ChatSource.QQ){
            handle(chat, message -> MessageUtil.filter(message,config.qqDenyStyle));
        }else if (chat.source==ChatSource.WEB){
            handle(chat, message -> MessageUtil.filter(message,config.webDenyStyle));
        }
    }
}
