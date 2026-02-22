package org.lintx.plugins.yinwuchat.velocity.json;

/**
 * Web 已读游标上报
 */
public class InputReadCursor extends InputBase {
    private final String chat;
    private final long messageId;

    public InputReadCursor(String chat, long messageId) {
        this.chat = chat != null ? chat : "";
        this.messageId = messageId;
    }

    public String getChat() {
        return chat;
    }

    public long getMessageId() {
        return messageId;
    }
}
