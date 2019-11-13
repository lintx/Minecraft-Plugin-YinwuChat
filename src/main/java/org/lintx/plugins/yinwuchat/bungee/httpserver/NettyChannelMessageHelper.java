package org.lintx.plugins.yinwuchat.bungee.httpserver;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class NettyChannelMessageHelper {
    public static void send(Channel channel,String message){
        channel.writeAndFlush(new TextWebSocketFrame(message));
    }

    public static void broadcast(String message){
        for (Channel channel:WsClientHelper.channels()){
            send(channel,message);
        }
    }
}
