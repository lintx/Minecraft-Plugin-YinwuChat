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
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.lintx.plugins.yinwuchat.common.auth.AuthService;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

public class NettyHttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private AsciiString htmlType = AsciiString.cached("text/html");
    private AsciiString jsType = AsciiString.cached("text/javascript");
    private AsciiString cssType = AsciiString.cached("text/css");
    private AsciiString jpegType = AsciiString.cached("image/jpeg");
    private final File rootFolder;
    private final AuthService authService;


    NettyHttpRequestHandler(File rootFolder){
        this.rootFolder = rootFolder;
        File dataFolder = rootFolder.getParentFile();
        this.authService = AuthService.getInstance(dataFolder);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            URI uri = new URI(request.uri());
            String path = normalizeHttpPath(uri.getPath());
            if (path.equals("/api/wsinfo")) {
                writeJson(ctx, request, buildWsInfoJson(request));
                return;
            }
            if (path.startsWith("/api/auth/")) {
                handleAuthApi(ctx, request, path);
                return;
            }
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

    private void handleAuthApi(ChannelHandlerContext ctx, FullHttpRequest request, String path) {
        if (request.method() == HttpMethod.OPTIONS) {
            writeCorsPreflight(ctx, request);
            return;
        }
        if (request.method() == HttpMethod.GET) {
            if (path.equals("/api/auth/handshake")) {
                writeJson(ctx, request, authService.toJson(authService.createHandshakeResponse()));
                return;
            }
            if (path.equals("/api/auth/captcha")) {
                writeJson(ctx, request, authService.toJson(authService.createCaptchaResponse()));
                return;
            }
            if (path.startsWith("/api/auth/reset/status/")) {
                String username = path.substring("/api/auth/reset/status/".length());
                writeJson(ctx, request, authService.toJson(authService.handleCheckResetStatus(username)));
                return;
            }
        }
        if (request.method() == HttpMethod.POST) {
            String body = request.content().toString(StandardCharsets.UTF_8);
            if (path.equals("/api/auth/register")) {
                writeJson(ctx, request, authService.toJson(authService.handleRegister(body)));
                return;
            }
            if (path.equals("/api/auth/login")) {
                com.google.gson.JsonObject result = authService.handleLogin(body);
                appendAccountsToLoginResult(result);
                writeJson(ctx, request, authService.toJson(result));
                return;
            }
            if (path.equals("/api/auth/quick-login")) {
                com.google.gson.JsonObject result = authService.handleQuickLogin(body);
                appendAccountsToLoginResult(result);
                writeJson(ctx, request, authService.toJson(result));
                return;
            }
            if (path.equals("/api/auth/accounts/list")) {
                com.google.gson.JsonObject result = authService.handleListAccounts(body, playerName -> {
                    java.util.UUID u = org.lintx.plugins.yinwuchat.bungee.config.PlayerConfig.getTokens().getUuidByName(playerName);
                    if (u == null) {
                        return "";
                    }
                    String t = org.lintx.plugins.yinwuchat.bungee.config.PlayerConfig.getTokens().getToken(u);
                    return t == null ? "" : t;
                });
                writeJson(ctx, request, authService.toJson(result));
                return;
            }
            if (path.equals("/api/auth/accounts/remove")) {
                writeJson(ctx, request, authService.toJson(authService.handleRemoveBoundPlayer(body)));
                return;
            }
            if (path.equals("/api/auth/accounts/bind-token")) {
                String newToken = org.lintx.plugins.yinwuchat.bungee.config.PlayerConfig.getTokens().newToken();
                writeJson(ctx, request, authService.toJson(authService.handleRegisterBindToken(body, newToken)));
                return;
            }
            if (path.equals("/api/auth/reset-token")) {
                writeJson(ctx, request, authService.toJson(authService.handleResetToken(body, playerName -> {
                    org.lintx.plugins.yinwuchat.bungee.config.PlayerConfig.getTokens().removeUuidByName(playerName);
                })));
                return;
            }
            if (path.equals("/api/auth/delete")) {
                writeJson(ctx, request, authService.toJson(authService.handleDeleteAccount(body, playerName -> {
                    org.lintx.plugins.yinwuchat.bungee.config.PlayerConfig.getTokens().removeUuidByName(playerName);
                })));
                return;
            }
            if (path.equals("/api/auth/reset/request")) {
                writeJson(ctx, request, authService.toJson(authService.handleRequestReset(body, (accountName, playerName) -> {
                    String mapped = authService.resolveAccountByPlayerName(playerName);
                    return mapped != null && !mapped.isEmpty() && mapped.equalsIgnoreCase(accountName);
                })));
                return;
            }
            if (path.equals("/api/auth/reset/submit")) {
                writeJson(ctx, request, authService.toJson(authService.handleResetPassword(body)));
                return;
            }
        }
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, METHOD_NOT_ALLOWED);
        addCorsHeaders(response.headers(), request);
        write(ctx, response, AsciiString.cached("application/json"));
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

    private void writeJson(ChannelHandlerContext ctx, FullHttpRequest request, String json) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(json, StandardCharsets.UTF_8)
        );
        addCorsHeaders(response.headers(), request);
        write(ctx, response, AsciiString.cached("application/json"));
    }

    private void appendAccountsToLoginResult(com.google.gson.JsonObject result) {
        authService.appendAccountsToLoginResult(result, playerName -> {
            java.util.UUID u = org.lintx.plugins.yinwuchat.bungee.config.PlayerConfig.getTokens().getUuidByName(playerName);
            if (u == null) {
                return "";
            }
            String t = org.lintx.plugins.yinwuchat.bungee.config.PlayerConfig.getTokens().getToken(u);
            return t == null ? "" : t;
        });
    }

    private static String normalizeHttpPath(String path) {
        if (path == null) {
            return "";
        }
        if (path.startsWith("/new-chat")) {
            path = path.substring("/new-chat".length());
            if (path.isEmpty()) {
                return "/";
            }
            if (!path.startsWith("/")) {
                return "/" + path;
            }
        }
        return path;
    }

    private void writeCorsPreflight(ChannelHandlerContext ctx, FullHttpRequest request) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
        addCorsHeaders(response.headers(), request);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        ctx.writeAndFlush(response);
    }

    private void addCorsHeaders(HttpHeaders headers, FullHttpRequest request) {
        String origin = request.headers().get(HttpHeaderNames.ORIGIN);
        headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin != null ? origin : "*");
        headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");
        headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization");
        headers.set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, "86400");
    }

    private String buildWsInfoJson(FullHttpRequest request) {
        String host = request.headers().get(HttpHeaderNames.HOST);
        if (host == null || host.isEmpty()) host = "localhost";
        if (host.contains(":")) host = host.split(":")[0];
        String proto = "ws";
        String forwardedProto = request.headers().get("X-Forwarded-Proto");
        String forwardedPort = request.headers().get("X-Forwarded-Port");
        boolean isBehindProxy = forwardedProto != null && !forwardedProto.isEmpty();
        if ("https".equalsIgnoreCase(forwardedProto)) proto = "wss";
        int port = org.lintx.plugins.yinwuchat.bungee.config.Config.getInstance().wsport;
        // 通过反向代理访问时使用代理端口和新路径
        String wsUrl;
        String wsPath;
        if (isBehindProxy) {
            wsPath = "/new-ws";
            String proxyPort = (forwardedPort != null && !forwardedPort.isEmpty()) ? forwardedPort : "31115";
            // 如果是标准端口（443/80）则不显示端口号
            if ("443".equals(proxyPort) || "80".equals(proxyPort)) {
                wsUrl = proto + "://" + host + wsPath;
            } else {
                wsUrl = proto + "://" + host + ":" + proxyPort + wsPath;
            }
        } else {
            wsPath = "/ws";
            wsUrl = proto + "://" + host + ":" + port + wsPath;
        }
        String json = "{\"ok\":true,\"wsPort\":" + port + ",\"wsPath\":\"" + wsPath + "\",\"wsUrl\":\"" + wsUrl + "\"}";
        return json;
    }
}
