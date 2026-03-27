package org.lintx.plugins.yinwuchat.Util;

import org.junit.Test;
import org.lintx.plugins.yinwuchat.Const;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PluginMessageChannelUtilTest {

    @Test
    public void triesBukkitChannelBeforeVelocityFallback() {
        List<String> attempts = new ArrayList<>();

        boolean sent = PluginMessageChannelUtil.sendWithFallback(channel -> {
            attempts.add(channel);
            return Const.PLUGIN_CHANNEL_BUKKIT.equals(channel);
        });

        assertTrue(sent);
        assertEquals(1, attempts.size());
        assertEquals(Const.PLUGIN_CHANNEL_BUKKIT, attempts.get(0));
    }

    @Test
    public void fallsBackToVelocityChannelWhenBukkitChannelFails() {
        List<String> attempts = new ArrayList<>();

        boolean sent = PluginMessageChannelUtil.sendWithFallback(channel -> {
            attempts.add(channel);
            return Const.PLUGIN_CHANNEL_VELOCITY.equals(channel);
        });

        assertTrue(sent);
        assertEquals(2, attempts.size());
        assertEquals(Const.PLUGIN_CHANNEL_BUKKIT, attempts.get(0));
        assertEquals(Const.PLUGIN_CHANNEL_VELOCITY, attempts.get(1));
    }
}
