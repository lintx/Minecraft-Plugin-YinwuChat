package org.lintx.plugins.yinwuchat.chat.handle;

import net.md_5.bungee.api.chat.BaseComponent;

import java.util.regex.Matcher;

public interface HandleComponentCallback {
    BaseComponent handle(Matcher matcher);
}
