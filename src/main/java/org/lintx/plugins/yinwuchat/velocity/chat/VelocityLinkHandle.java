package org.lintx.plugins.yinwuchat.velocity.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.lintx.plugins.yinwuchat.velocity.config.Config;

/**
 * Velocity 版本的链接处理器
 * 将链接转换为可点击的组件
 */
public class VelocityLinkHandle extends VelocityChatHandle {

    @Override
    public void handle(VelocityChat chat) {
        handle(chat, "(https?://[\\w\\d./?=#&%-]+)", (matcher) -> {
            String url = matcher.group(1);
            Component linkComponent = Component.text(url)
                .color(NamedTextColor.BLUE)
                .clickEvent(ClickEvent.openUrl(url));
            return linkComponent;
        });
    }
}
