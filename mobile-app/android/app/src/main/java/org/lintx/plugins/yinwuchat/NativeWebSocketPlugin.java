package org.lintx.plugins.yinwuchat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

@CapacitorPlugin(name = "NativeWebSocket")
public class NativeWebSocketPlugin extends Plugin {

    private WebSocket webSocket;
    private OkHttpClient client;

    private OkHttpClient getClient() {
        if (client == null) {
            try {
                X509TrustManager trustManager = new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                };
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                client = new OkHttpClient.Builder()
                        .sslSocketFactory(sslSocketFactory, trustManager)
                        .hostnameVerifier((hostname, session) -> true)
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(0, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .pingInterval(30, TimeUnit.SECONDS)
                        .build();
            } catch (Exception e) {
                client = new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(0, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .pingInterval(30, TimeUnit.SECONDS)
                        .build();
            }
        }
        return client;
    }

    @PluginMethod
    public void connect(PluginCall call) {
        String url = call.getString("url");
        if (url == null || url.isEmpty()) {
            call.reject("URL is required");
            return;
        }

        // 关闭已有连接
        if (webSocket != null) {
            try { webSocket.close(1000, "reconnect"); } catch (Exception ignored) {}
            webSocket = null;
        }

        Request request = new Request.Builder().url(url).build();

        webSocket = getClient().newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                JSObject data = new JSObject();
                data.put("url", url);
                notifyListeners("open", data);
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                JSObject data = new JSObject();
                data.put("data", text);
                notifyListeners("message", data);
            }

            @Override
            public void onClosing(WebSocket ws, int code, String reason) {
                // 自动确认关闭
                ws.close(code, reason);
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                JSObject data = new JSObject();
                data.put("code", code);
                data.put("reason", reason != null ? reason : "");
                notifyListeners("close", data);
                webSocket = null;
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                JSObject data = new JSObject();
                data.put("message", t.getMessage() != null ? t.getMessage() : "Unknown error");
                data.put("code", 1006);
                notifyListeners("error", data);
                // 也通知 close 事件
                JSObject closeData = new JSObject();
                closeData.put("code", 1006);
                closeData.put("reason", t.getMessage() != null ? t.getMessage() : "Connection failed");
                notifyListeners("close", closeData);
                webSocket = null;
            }
        });

        call.resolve();
    }

    @PluginMethod
    public void send(PluginCall call) {
        String data = call.getString("data");
        if (webSocket != null && data != null) {
            boolean ok = webSocket.send(data);
            if (ok) {
                call.resolve();
            } else {
                call.reject("Send failed");
            }
        } else {
            call.reject("WebSocket not connected or data is null");
        }
    }

    @PluginMethod
    public void close(PluginCall call) {
        int code = call.getInt("code", 1000);
        String reason = call.getString("reason", "Normal closure");
        if (webSocket != null) {
            try {
                webSocket.close(code, reason);
            } catch (Exception ignored) {}
            webSocket = null;
        }
        call.resolve();
    }
}
