package org.lintx.plugins.yinwuchat.velocity.json;

/**
 * 消息输入
 */
public class InputMessage extends InputBase {
    private final String message;
    
    public String getMessage() {
        return message;
    }
    
    public InputMessage(String message) {
        if (message == null) {
            message = "";
        }
        this.message = message;
    }
}
