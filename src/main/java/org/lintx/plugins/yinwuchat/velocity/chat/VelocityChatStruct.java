package org.lintx.plugins.yinwuchat.velocity.chat;

import net.kyori.adventure.text.Component;

/**
 * Velocity 版本的聊天结构
 * 使用 Adventure Component 替代 BungeeCord 的 BaseComponent
 */
public class VelocityChatStruct {
    public String chat;
    public Component component;
    
    public VelocityChatStruct() {
    }
    
    public VelocityChatStruct(String chat) {
        this.chat = chat;
    }
    
    public VelocityChatStruct(String chat, Component component) {
        this.chat = chat;
        this.component = component;
    }
}
