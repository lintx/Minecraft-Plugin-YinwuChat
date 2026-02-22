package org.lintx.plugins.yinwuchat.velocity.chat;

/**
 * Velocity 版本的 CoolQ 转义处理器
 * 解码 CoolQ 消息中的 HTML 实体编码
 */
public class VelocityCoolQEscapeHandle extends VelocityChatHandle {

    @Override
    public void handle(VelocityChat chat) {
        handle(chat, message -> message
            .replaceAll("&amp;", "& ")
            .replaceAll("&#91;", "[")
            .replaceAll("&#93;", "]"));
    }
}
