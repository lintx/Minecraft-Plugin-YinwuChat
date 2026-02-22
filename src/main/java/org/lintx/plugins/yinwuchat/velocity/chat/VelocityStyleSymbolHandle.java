package org.lintx.plugins.yinwuchat.velocity.chat;

import org.lintx.plugins.yinwuchat.chat.struct.ChatSource;

/**
 * Velocity 版本的样式符号处理器
 * 将 & 颜色代码转换为 § 颜色代码
 */
public class VelocityStyleSymbolHandle extends VelocityChatHandle {

    @Override
    public void handle(VelocityChat chat) {
        if (chat.source == ChatSource.QQ) return;

        handle(chat, (text) -> text.replaceAll("&([0-9a-fA-Fk-oK-OrRxX])", "§$1"));
    }
}
