package org.lintx.plugins.yinwuchat.velocity.chat;

import org.lintx.plugins.yinwuchat.chat.struct.ChatSource;
import org.lintx.plugins.yinwuchat.velocity.config.Config;

/**
 * Velocity 版本的权限样式处理器
 * 根据来源过滤不允许的样式代码
 */
public class VelocityStylePermissionHandle extends VelocityChatHandle {

    @Override
    public void handle(VelocityChat chat) {
        Config config = Config.getInstance();
        if (chat.source == ChatSource.QQ) {
            if (config.coolQConfig != null) {
                handle(chat, message -> filterStyles(message, config.coolQConfig.qqDenyStyle));
            }
        } else if (chat.source == ChatSource.WEB) {
            // Web 样式过滤，如果需要的话
            handle(chat, message -> message); // 暂时保持不变
        }
    }

    private String filterStyles(String message, String denyStyle) {
        if (denyStyle == null || denyStyle.isEmpty()) {
            return message;
        }
        message = message.replaceAll("&[" + denyStyle + "]", "");
        message = message.replaceAll("§[" + denyStyle + "]", "");
        return message;
    }
}
