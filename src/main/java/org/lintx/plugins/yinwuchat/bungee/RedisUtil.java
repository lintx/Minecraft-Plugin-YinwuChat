package org.lintx.plugins.yinwuchat.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.bungee.config.Config;
import org.lintx.plugins.yinwuchat.bungee.config.PlayerConfig;
import org.lintx.plugins.yinwuchat.bungee.config.RedisConfig;
import org.lintx.plugins.yinwuchat.bungee.json.RedisMessage;
import org.lintx.plugins.yinwuchat.bungee.json.RedisMessageType;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RedisUtil {
    private static final String REDIS_SUBSCRIBE_CHANNEL = "yinwuchat-redis-subscribe-channel";
    private static JedisPool jedisPool;
    private static Subscribe subscribe;
    public static Map<String,String> playerList = new HashMap<>();//player-server

    static void init(){
        RedisConfig config = Config.getInstance().redisConfig;
        if (!config.openRedis) return;
        if (jedisPool != null || subscribe != null){
            unload();
        }
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(Config.getInstance().redisConfig.maxConnection);
        jedisPool = new JedisPool(poolConfig,config.ip,config.port,0,config.password);
        subscribe = new Subscribe();

        final Jedis jedis = jedisPool.getResource();
        new Thread(() -> {
            try {
                jedis.subscribe(subscribe,REDIS_SUBSCRIBE_CHANNEL);
            }catch (Exception ignored){}
        }).start();
    }

    static void unload(){
        try {
            subscribe.unsubscribe();
        }catch (Exception ignored){}
        subscribe = null;
        jedisPool = null;
    }

    private static class Subscribe extends JedisPubSub {
        @Override
        public void onMessage(String channel, String message) {
            super.onMessage(channel, message);
            if (channel.equals(REDIS_SUBSCRIBE_CHANNEL)){
                try {
                    RedisConfig config = Config.getInstance().redisConfig;
                    Gson gson = new Gson();
                    RedisMessage obj = gson.fromJson(message,RedisMessage.class);

                    //自己服务器发送的消息不执行后续动作
                    if (obj.fromServer.equals(config.selfName)){
                        return;
                    }

                    //对单个服务器发送的消息，如果目标不是自己，则不执行后续动作
                    if (!"".equals(obj.toServer) && !obj.toServer.equals(config.selfName)){
                        return;
                    }

                    ProxiedPlayer player = null;
                    PlayerConfig.Player pc = null;

                    //对单个玩家发送的消息的前期处理
                    if (!"".equals(obj.toPlayerName)){
                        player = YinwuChat.getPlugin().getProxy().getPlayer(obj.toPlayerName);
                        if (player==null) return;
                        pc = PlayerConfig.getConfig(player);
                        //如果该玩家忽略了发送消息的玩家的消息，则退出
                        if (pc.isIgnore(obj.fromPlayerUUID)) return;
                    }

                    if (!"".equals(obj.message)) obj.chat = ComponentSerializer.parse(obj.message)[0];

                    switch (obj.type){
                        case AT_PLAYER:
                            //AT单个玩家
                            if (player==null) return;
                            if (pc.banAt) return;
                            player.sendMessage(obj.chat);

                            if (pc.muteAt) return;
                            ByteArrayDataOutput output = ByteStreams.newDataOutput();
                            output.writeUTF(Const.PLUGIN_SUB_CHANNEL_AT);
                            player.getServer().sendData(Const.PLUGIN_CHANNEL,output.toByteArray());
                            break;
                        case AT_PLAYER_ALL:
                            //AT 所有人
                            ByteArrayDataOutput output1 = ByteStreams.newDataOutput();
                            output1.writeUTF(Const.PLUGIN_SUB_CHANNEL_AT);
                            for (ProxiedPlayer p : YinwuChat.getPlugin().getProxy().getPlayers()){
                                p.sendMessage(obj.chat);
                                p.getServer().sendData(Const.PLUGIN_CHANNEL,output1.toByteArray());
                            }
                            break;
                        case PUBLIC_MESSAGE:
                            //公屏消息
                            for (ProxiedPlayer p : YinwuChat.getPlugin().getProxy().getPlayers()){
                                if (!"".equals(obj.toMCServer)){
                                    if (!p.getServer().getInfo().getName().equalsIgnoreCase(obj.toMCServer)){
                                        continue;
                                    }
                                }
                                PlayerConfig.Player pc2 = PlayerConfig.getConfig(p);
                                if (pc2.isIgnore(obj.fromPlayerUUID)) continue;
                                p.sendMessage(obj.chat);
                            }
                            break;
                        case PRIVATE_MESSAGE:
                            //私聊消息
                            if (player==null) return;
                            player.sendMessage(obj.chat);
                            break;
                        case PLAYER_LIST:
                            //玩家列表
                            Collection<String> col = playerList.values();
                            while (col.contains(obj.fromServer)){
                                col.remove(obj.fromServer);
                            }
                            for (String playerName:obj.playerList){
                                playerList.put(playerName,obj.fromServer);
                            }
                            MessageManage.getInstance().sendPlayerListToServer();
                            break;
                    }
                }catch (Exception ignored){
                    ignored.printStackTrace();
                }
            }
        }
    }

    public static void sendMessage( UUID uuid,BaseComponent chat){
        sendMessage(RedisMessageType.PUBLIC_MESSAGE,uuid,chat,"");
    }

    public static void sendMessage( UUID uuid,BaseComponent chat,String toPlayer){
        sendMessage(RedisMessageType.PRIVATE_MESSAGE,uuid,chat,toPlayer);
    }

    public static void sendMessage(RedisMessageType type, UUID uuid, BaseComponent chat, String toPlayer){
        sendMessage(type,uuid,chat,toPlayer,"");
    }

    public static void sendMessage(RedisMessageType type, UUID uuid, BaseComponent chat, String toPlayer, String mcServer){
        RedisMessage message = new RedisMessage();
//        message.chat = chat;
        message.toMCServer = mcServer;
        message.message = ComponentSerializer.toString(chat);
        message.toPlayerName = toPlayer;
        message.fromServer = Config.getInstance().redisConfig.selfName;
        message.type = type;
        message.fromPlayerUUID = uuid;
        if (!"".equals(toPlayer)){
            String toServer = playerList.get(toPlayer);
            if (toServer!=null) message.toServer = toServer;
        }
        sendMessage(message);
    }

    public static void sendPlayerList(){
        RedisMessage message = new RedisMessage();
        message.type = RedisMessageType.PLAYER_LIST;
        message.fromServer = Config.getInstance().redisConfig.selfName;
        for (ProxiedPlayer p : YinwuChat.getPlugin().getProxy().getPlayers()){
            message.playerList.add(p.getName());
        }
        sendMessage(message);
    }

    private static void sendMessage(RedisMessage message){
        if (jedisPool==null) return;
        final Jedis jedis = jedisPool.getResource();
        jedis.publish(REDIS_SUBSCRIBE_CHANNEL,new Gson().toJson(message));
    }
}
