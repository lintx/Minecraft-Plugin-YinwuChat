package org.lintx.plugins.yinwuchat.Util;

import org.lintx.plugins.yinwuchat.Const;

public final class PluginMessageChannelUtil {

    private PluginMessageChannelUtil() {
    }

    public static boolean sendWithFallback(ChannelSender sender) {
        if (sender == null) {
            return false;
        }
        if (sender.trySend(Const.PLUGIN_CHANNEL_BUKKIT)) {
            return true;
        }
        return sender.trySend(Const.PLUGIN_CHANNEL_VELOCITY);
    }

    @FunctionalInterface
    public interface ChannelSender {
        boolean trySend(String channel);
    }
}
