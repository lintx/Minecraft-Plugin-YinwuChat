package org.lintx.plugins.yinwuchat.bungee.httpserver;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AsciiString;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.util.Locale;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

public class NettyHttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private AsciiString htmlType = AsciiString.cached("text/html");
    private AsciiString jsType = AsciiString.cached("text/javascript");
    private AsciiString cssType = AsciiString.cached("text/css");
    private AsciiString jpegType = AsciiString.cached("image/jpeg");
    private final File rootFolder;


    NettyHttpRequestHandler(File rootFolder){
        this.rootFolder = rootFolder;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            URI uri = new URI(request.uri());
            String path = uri.getPath();
            if (path.equals("/")){
                writeIndex(ctx);
                return;
            }
            if (request.method()!=HttpMethod.GET){
                write404(ctx);
                return;
            }
            writeFile(ctx,path);
        }
        catch (Exception e) {
            e.printStackTrace();
            write404(ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private void writeIndex(ChannelHandlerContext ctx){
        writeFile(ctx,"/index.html");
    }

    private void writeFile(ChannelHandlerContext ctx,String path){
        try {
            File file = new File(rootFolder,path);
            String canonical = file.getCanonicalPath();
            if (canonical.startsWith(rootFolder.getCanonicalPath()) && file.exists() && file.isFile()){
                AsciiString mime;
                String cs = canonical.toLowerCase(Locale.ROOT);
                if (cs.endsWith(".jpg") || cs.endsWith(".jpeg")){
                    mime = jpegType;
                }else if (cs.endsWith(".html")){
                    mime = htmlType;
                }else if (cs.endsWith(".js")){
                    mime = jsType;
                }else if (cs.endsWith(".css")){
                    mime = cssType;
                }else {
                    write404(ctx);
                    return;
                }

                final RandomAccessFile raf = new RandomAccessFile(file,"r");
                long fileLength = raf.length();
                HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK);
                HttpHeaders heads = response.headers();
                heads.add(HttpHeaderNames.CONTENT_TYPE, mime + "; charset=UTF-8");
                heads.add(HttpHeaderNames.CONTENT_LENGTH, fileLength);
                heads.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                ctx.write(response);

                ChannelFuture sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(),0,fileLength),ctx.newProgressivePromise());

                ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
                    @Override
                    public void operationProgressed(ChannelProgressiveFuture channelProgressiveFuture, long l, long l1) {

                    }

                    @Override
                    public void operationComplete(ChannelProgressiveFuture channelProgressiveFuture) throws Exception {
                        raf.close();
                    }
                });
            }else {
                write404(ctx);
            }
        } catch (IOException ignored) {
            write404(ctx);
        }
    }

    private void write404(ChannelHandlerContext ctx){
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, NOT_FOUND);
        String string = "<h1>File Not Found</h1>";
        response.content().writeBytes(Unpooled.wrappedBuffer(string.getBytes()));
        write(ctx,response,htmlType);
    }

    private void write(ChannelHandlerContext ctx,DefaultFullHttpResponse response,AsciiString type){
        HttpHeaders heads = response.headers();
        heads.add(HttpHeaderNames.CONTENT_TYPE, type + "; charset=UTF-8");
        heads.add(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        heads.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        ctx.writeAndFlush(response);
    }
}
