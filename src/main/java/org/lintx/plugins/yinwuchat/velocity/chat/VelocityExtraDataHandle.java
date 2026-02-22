package org.lintx.plugins.yinwuchat.velocity.chat;

import net.kyori.adventure.text.Component;
import org.lintx.plugins.yinwuchat.chat.struct.ChatSource;
import org.lintx.plugins.yinwuchat.json.HandleConfig;

/**
 * Velocity 版本的额外数据处理器
 * 处理额外的消息格式和占位符替换
 */
public class VelocityExtraDataHandle extends VelocityChatHandle {

    @Override
    public void handle(VelocityChat chat) {
        if (chat.source != ChatSource.GAME) return;
        if (chat.extraData == null || chat.extraData.isEmpty()) return;

        for (HandleConfig config : chat.extraData) {
            handle(chat, config.placeholder, (matcher) -> {
                Component result = Component.empty();
                for (org.lintx.plugins.yinwuchat.json.MessageFormat format : config.format) {
                    Component c = chat.buildFormat(format);
                    if (c != null) {
                        result = result.append(c);
                    }
                }
                return result.children().isEmpty() ? null : result;
            });
        }
    }
}
