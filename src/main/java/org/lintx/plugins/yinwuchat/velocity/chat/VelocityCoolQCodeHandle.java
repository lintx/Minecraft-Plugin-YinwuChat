package org.lintx.plugins.yinwuchat.velocity.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.lintx.plugins.yinwuchat.chat.struct.ChatSource;
import org.lintx.plugins.yinwuchat.velocity.config.Config;

/**
 * Velocity 版本的 CoolQ 代码处理器
 * 处理 QQ 消息中的 CQ 代码（如图片、语音、@等）
 */
public class VelocityCoolQCodeHandle extends VelocityChatHandle {

    @Override
    public void handle(VelocityChat chat) {
        if (chat.source != ChatSource.QQ) return;

        Config config = Config.getInstance();
        String regexp = "\\[CQ:(.*?),(.*?)]";

        handle(chat, regexp, (matcher) -> {
            String func = matcher.group(1);
            String ext = matcher.group(2);

            if (func.equalsIgnoreCase("image")) {
                if (config.coolQConfig != null) {
                    return Component.text(config.coolQConfig.qqImageText);
                }
            } else if (func.equalsIgnoreCase("record")) {
                if (config.coolQConfig != null) {
                    return Component.text(config.coolQConfig.qqRecordText);
                }
            } else if (func.equalsIgnoreCase("at")) {
                if (config.coolQConfig != null) {
                    String qq = ext.replaceAll("qq=", "");
                    String atText = config.coolQConfig.qqAtText.replaceAll("\\{qq}", qq);
                    return Component.text(atText);
                }
            } else if (func.equalsIgnoreCase("share")) {
                String url = "";
                String[] a = ext.split(",");
                for (String kv : a) {
                    String[] b = kv.split("=", 2);
                    if (b.length == 2 && b[0].equalsIgnoreCase("url")) {
                        url = b[1];
                        break;
                    }
                }

                Component component;
                if (config.tipsConfig != null) {
                    component = Component.text(config.tipsConfig.linkText);
                } else {
                    component = Component.text("[链接]");
                }

                if (!url.isEmpty()) {
                    component = component
                        .hoverEvent(HoverEvent.showText(Component.text(url)))
                        .clickEvent(ClickEvent.openUrl(url));
                }

                return component;
            }

            return null;
        });
    }
}
