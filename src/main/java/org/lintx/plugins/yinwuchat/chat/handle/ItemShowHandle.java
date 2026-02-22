package org.lintx.plugins.yinwuchat.chat.handle;

import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.chat.struct.Chat;
import org.lintx.plugins.yinwuchat.chat.struct.ChatSource;

public class ItemShowHandle extends ChatHandle {
    @Override
    public void handle(Chat chat) {
        if (chat.source != ChatSource.GAME) return;
        if (chat.items == null) return;

        handle(chat, Const.ITEM_PLACEHOLDER, (matcher) -> {
            if (chat.items.isEmpty()) return null;
            return chat.items.remove(0);
        });
    }
}
