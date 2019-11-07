/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lintx.plugins.yinwuchat.bungee.util;

import org.java_websocket.WebSocket;
import org.lintx.plugins.yinwuchat.bungee.json.ServerMessageJSON;

import java.util.*;

/**
 *
 * @author jjcbw01
 */
public class WsClientHelper {
    private static final HashMap<WebSocket,WsClientUtil> clients = new HashMap<WebSocket,WsClientUtil>();
    private static WebSocket coolQ = null;

    public static void updateCoolQ(WebSocket socket){
        if (coolQ!=null){
            try {
                coolQ.close();
            }catch (Exception ignored){

            }
        }
        coolQ = socket;
    }

    public static WebSocket getCoolQ(){
        return coolQ;
    }
    
    public static void add(WebSocket socket, WsClientUtil client){
        remove(socket);
        clients.put(socket, client);
    }

    public static Set<WebSocket> clients(){
        return clients.keySet();
    }

    public static Collection<WsClientUtil> utils(){
        return clients.values();
    }
    
    public static void remove(WebSocket socket){
        clients.remove(socket);
    }
    
    public static WsClientUtil get(WebSocket socket){
        WsClientUtil client = null;
        if (clients.containsKey(socket)) {
            client = clients.get(socket);
        }
        return client;
    }
    
    public static void clear(){
        clients.clear();
    }
    
    public static WebSocket getWebSocket(String token){
        for (Map.Entry<WebSocket, WsClientUtil> entry : clients.entrySet()) {
            WebSocket key = entry.getKey();
            WsClientUtil value = entry.getValue();
            if (value.getToken().equalsIgnoreCase(token)) {
                return key;
            }
        }
        return null;
    }

    public static WebSocket getWebSocketAsUtil(WsClientUtil util){
        for (Map.Entry<WebSocket, WsClientUtil> entry : clients.entrySet()) {
            WebSocket key = entry.getKey();
            WsClientUtil value = entry.getValue();
            if (value.equals(util)) {
                return key;
            }
        }
        return null;
    }
    
    public static List<WebSocket> getWebSocket(UUID uuid){
        List<WebSocket> list = new ArrayList<>();
        for (Map.Entry<WebSocket, WsClientUtil> entry : clients.entrySet()) {
            WebSocket key = entry.getKey();
            WsClientUtil value = entry.getValue();
            if (value.getUuid()!=null && uuid.toString().equalsIgnoreCase(value.getUuid().toString())) {
                list.add(key);
            }
        }
        return list;
    }
    
    public static void kickOtherWS(WebSocket ws, UUID uuid){
        List<WebSocket> oldSockets = WsClientHelper.getWebSocket(uuid);
        if (!oldSockets.isEmpty()) {
            for (WebSocket oldSocket : oldSockets) {
                if (oldSocket != null && oldSocket != ws) {
                    oldSocket.send(ServerMessageJSON.errorJSON("你的帐号已经在其他地方上线，你已经被踢下线").getJSON());
                    oldSocket.close(3000, "");
                    WsClientHelper.remove(oldSocket);
                }
            }
        }
    }
}
