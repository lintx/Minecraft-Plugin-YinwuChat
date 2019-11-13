/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lintx.plugins.yinwuchat.bungee.httpserver;

import io.netty.channel.Channel;
import org.lintx.plugins.yinwuchat.bungee.json.OutputServerMessage;

import java.util.*;

/**
 *
 * @author LinTx
 */
public class WsClientHelper {
    private static final HashMap<Channel, WsClientUtil> clients = new HashMap<>();
    private static Channel coolQ = null;

    public static void updateCoolQ(Channel socket){
        if (coolQ!=null){
            try {
                coolQ.close();
            }catch (Exception ignored){

            }
        }
        coolQ = socket;
    }

    public static Channel getCoolQ(){
        return coolQ;
    }
    
    public static void add(Channel channel, WsClientUtil client){
        remove(channel);
        clients.put(channel, client);
    }

    public static Set<Channel> channels(){
        return clients.keySet();
    }

    public static Collection<WsClientUtil> utils(){
        return clients.values();
    }
    
    static void remove(Channel channel){
        clients.remove(channel);
    }
    
    public static WsClientUtil get(Channel channel){
        WsClientUtil client = null;
        if (clients.containsKey(channel)) {
            client = clients.get(channel);
        }
        return client;
    }
    
    public static void clear(){
        clients.clear();
    }
    
    public static Channel getWebSocket(String token){
        for (Map.Entry<Channel, WsClientUtil> entry : clients.entrySet()) {
            Channel key = entry.getKey();
            WsClientUtil value = entry.getValue();
            if (value.getToken().equalsIgnoreCase(token)) {
                return key;
            }
        }
        return null;
    }

    public static Channel getWebSocketAsUtil(WsClientUtil util){
        for (Map.Entry<Channel, WsClientUtil> entry : clients.entrySet()) {
            Channel key = entry.getKey();
            WsClientUtil value = entry.getValue();
            if (value.equals(util)) {
                return key;
            }
        }
        return null;
    }
    
    private static List<Channel> getChannels(UUID uuid){
        List<Channel> list = new ArrayList<>();
        for (Map.Entry<Channel, WsClientUtil> entry : clients.entrySet()) {
            Channel key = entry.getKey();
            WsClientUtil value = entry.getValue();
            if (value.getUuid()!=null && uuid.toString().equalsIgnoreCase(value.getUuid().toString())) {
                list.add(key);
            }
        }
        return list;
    }
    
    public static void kickOtherWS(Channel channel, UUID uuid){
        List<Channel> oldChannels = WsClientHelper.getChannels(uuid);
        if (!oldChannels.isEmpty()) {
            for (Channel oldChannel : oldChannels) {
                if (oldChannel != null && oldChannel != channel) {
                    NettyChannelMessageHelper.send(oldChannel, OutputServerMessage.errorJSON("你的帐号已经在其他地方上线，你已经被踢下线").getJSON());
                    oldChannel.close();
                    WsClientHelper.remove(oldChannel);
                }
            }
        }
    }
}
