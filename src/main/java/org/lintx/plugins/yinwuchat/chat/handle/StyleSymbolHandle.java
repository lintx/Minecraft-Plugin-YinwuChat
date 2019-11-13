package org.lintx.plugins.yinwuchat.chat.handle;

import org.lintx.plugins.yinwuchat.Util.MessageUtil;
import org.lintx.plugins.yinwuchat.chat.struct.Chat;
import org.lintx.plugins.yinwuchat.chat.struct.ChatSource;

public class StyleSymbolHandle extends ChatHandle {
    @Override
    public void handle(Chat chat) {
        if (chat.source!= ChatSource.QQ) handle(chat, MessageUtil::replace);
    }
}
