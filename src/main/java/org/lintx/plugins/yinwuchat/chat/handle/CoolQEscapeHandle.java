package org.lintx.plugins.yinwuchat.chat.handle;

import org.lintx.plugins.yinwuchat.chat.struct.Chat;

public class CoolQEscapeHandle extends ChatHandle {
    @Override
    public void handle(Chat chat) {
        handle(chat, message -> message.replaceAll("&amp;","& ").replaceAll("&#91;","[").replaceAll("&#93;","]"));
    }
}
