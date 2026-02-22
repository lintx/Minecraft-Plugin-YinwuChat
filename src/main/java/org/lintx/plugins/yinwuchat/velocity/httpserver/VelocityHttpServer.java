package org.lintx.plugins.yinwuchat.velocity.httpserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.lintx.plugins.yinwuchat.velocity.YinwuChat;

import java.io.File;

/**
 * WebSocket HTTP Server for Velocity proxy
 * Handles WebSocket connections from web clients
 */
public class VelocityHttpServer {
    private final int port;
    private NioEventLoopGroup group;
    private final YinwuChat plugin;
    private final File rootFolder;

    public VelocityHttpServer(int port, YinwuChat plugin, File rootFolder) {
        this.port = port;
        this.plugin = plugin;
        this.rootFolder = rootFolder;
    }

    public void start() {
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
                                .addLast(new ChunkedWriteHandler())
                                .addLast("ws-path-rewrite", new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        if (msg instanceof FullHttpRequest) {
                                            FullHttpRequest req = (FullHttpRequest) msg;
                                            String uri = req.uri();
                                            if (uri.startsWith("/new-ws") || uri.startsWith("/new-chat/ws")) {
                                                String newUri = "/ws";
                                                int qIdx = uri.indexOf('?');
                                                if (qIdx >= 0) newUri += uri.substring(qIdx);
                                                req.setUri(newUri);
                                            }
                                        }
                                        super.channelRead(ctx, msg);
                                    }
                                })
                                .addLast(new WebSocketServerCompressionHandler())
                                .addLast(new WebSocketServerProtocolHandler("/ws", null, true))
                                .addLast(new VelocityWebSocketFrameHandler(plugin))
                                .addLast(new VelocityHttpRequestHandler(rootFolder));
                    }
                });
        // 异步绑定，不阻塞线程
        bootstrap.bind(port).addListener(future -> {
            if (future.isSuccess()) {
                // 使用 future.channel() 获取绑定的通道，不要使用 getNow()
                Channel ch = ((io.netty.channel.ChannelFuture) future).channel();
                if (ch != null) {
                    plugin.getLogger().info("WebSocket server listening on port: " + port);
                    // 异步等待关闭，不阻塞线程
                    ch.closeFuture().addListener(closeFuture -> {
                        if (closeFuture.isSuccess()) {
                            plugin.getLogger().info("WebSocket server channel closed");
                        } else {
                            plugin.getLogger().warn("WebSocket server channel closed with error", closeFuture.cause());
                        }
                    });
                }
            } else {
                plugin.getLogger().error("Failed to bind WebSocket server to port " + port, future.cause());
                group.shutdownGracefully();
            }
        });
    }

    public void stop() {
        try {
            if (group != null) {
                // 同步等待关闭完成，确保端口完全释放后再返回
                group.shutdownGracefully().sync();
            }
        } catch (Exception e) {
            plugin.getLogger().warn("Error stopping WebSocket server", e);
        }
    }
}
