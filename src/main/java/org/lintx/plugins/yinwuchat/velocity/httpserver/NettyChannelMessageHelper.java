package org.lintx.plugins.yinwuchat.velocity.httpserver;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper for managing WebSocket channel messaging
 */
public class NettyChannelMessageHelper {
    private static final Set<Channel> channels = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void send(Channel channel, String message) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new TextWebSocketFrame(message));
        }
    }

    public static void broadcast(String message) {
        for (Channel channel : channels) {
            send(channel, message);
        }
    }

    public static void register(Channel channel) {
        channels.add(channel);
    }

    public static void unregister(Channel channel) {
        channels.remove(channel);
    }

    public static Set<Channel> getChannels() {
        return Collections.unmodifiableSet(channels);
    }
}
