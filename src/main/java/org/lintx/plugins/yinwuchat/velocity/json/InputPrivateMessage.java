package org.lintx.plugins.yinwuchat.velocity.json;

/**
 * 私聊消息输入
 */
public class InputPrivateMessage extends InputBase {
    private final String to;
    private final String message;
    
    public String getTo() {
        return to;
    }
    
    public String getMessage() {
        return message;
    }
    
    public InputPrivateMessage(String to, String message) {
        this.to = to != null ? to : "";
        this.message = message != null ? message : "";
    }
}
