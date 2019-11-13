package org.lintx.plugins.yinwuchat.chat.handle;

import org.lintx.plugins.yinwuchat.Util.MessageUtil;
import org.lintx.plugins.yinwuchat.chat.struct.Chat;

public class EmojiHandle extends ChatHandle {
    @Override
    public void handle(Chat chat) {
        handle(chat, MessageUtil::removeEmoji);
    }
}
