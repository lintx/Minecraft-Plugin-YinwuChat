package org.lintx.plugins.yinwuchat.bungee.httpserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import org.lintx.plugins.yinwuchat.bungee.YinwuChat;

import java.io.File;

public class NettyHttpServer {
    private final int port;
    private NioEventLoopGroup group;
    private final YinwuChat plugin;
    private final File rootFolder;

    public NettyHttpServer(int port, YinwuChat plugin, File rootFolder) {
        this.port = port;
        this.plugin = plugin;
        this.rootFolder = rootFolder;
    }

    public void start(){
        ServerBootstrap bootstrap = new ServerBootstrap();
        this.group = new NioEventLoopGroup();
        bootstrap.group(group)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        channel.pipeline()
                                .addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(65536))
                                .addLast(new WebSocketServerCompressionHandler())
                                .addLast(new WebSocketServerProtocolHandler("/ws",null,true))
                                .addLast(new NettyWebSocketFrameHandler(plugin))
                                .addLast(new NettyHttpRequestHandler(rootFolder));
                    }
                });
        try {
            Channel ch = bootstrap.bind(port).sync().channel();
            plugin.getLogger().info("Http Server listener on port:" + port);
            ch.closeFuture().sync();
        } catch (InterruptedException ignored) {
        }finally {
            group.shutdownGracefully();
        }
    }

    public void stop(){
        try {
            group.shutdownGracefully();
        }catch (Exception | Error ignored){}
    }
}
