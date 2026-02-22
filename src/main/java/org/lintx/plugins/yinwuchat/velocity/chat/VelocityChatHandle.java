package org.lintx.plugins.yinwuchat.velocity.chat;

import net.kyori.adventure.text.Component;

import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Velocity 版本的聊天处理器抽象类
 * 使用 Adventure API 替代 BungeeCord API
 */
public abstract class VelocityChatHandle {
    
    /**
     * 处理带有组件替换的聊天消息
     */
    void handle(VelocityChat chat, String regexp, java.util.function.Function<Matcher, Component> callback) {
        ListIterator<VelocityChatStruct> iterator = chat.chat.listIterator();
        while (iterator.hasNext()) {
            VelocityChatStruct struct = iterator.next();
            Pattern pattern = Pattern.compile(regexp);
            Matcher matcher = pattern.matcher(struct.chat);
            while (matcher.find()) {
                String[] splits = struct.chat.split(regexp, 2);
                if (splits.length != 2) break;

                Component component = callback.apply(matcher);
                if (component == null) {
                    // 如果没有返回组件，使用原始文本
                    component = Component.text(matcher.group(0));
                }

                VelocityChatStruct child = new VelocityChatStruct();
                child.chat = splits[0];
                child.component = component;

                iterator.previous();
                iterator.add(child);
                iterator.next();

                struct.chat = splits[1];
                matcher = pattern.matcher(struct.chat);
            }
        }
    }

    /**
     * 处理纯文本替换的聊天消息
     */
    void handle(VelocityChat chat, java.util.function.UnaryOperator<String> callback) {
        for (VelocityChatStruct struct : chat.chat) {
            struct.chat = callback.apply(struct.chat);
        }
    }

    /**
     * 主要的处理方法，子类必须实现
     */
    public abstract void handle(VelocityChat chat);
}
