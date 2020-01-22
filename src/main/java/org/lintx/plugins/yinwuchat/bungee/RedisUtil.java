package org.lintx.plugins.yinwuchat.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.netty.channel.Channel;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.bungee.config.Config;
import org.lintx.plugins.yinwuchat.bungee.config.PlayerConfig;
import org.lintx.plugins.yinwuchat.bungee.config.RedisConfig;
import org.lintx.plugins.yinwuchat.bungee.httpserver.NettyChannelMessageHelper;
import org.lintx.plugins.yinwuchat.bungee.httpserver.WsClientHelper;
import org.lintx.plugins.yinwuchat.bungee.json.OutputCoolQ;
import org.lintx.plugins.yinwuchat.bungee.json.RedisMessage;
import org.lintx.plugins.yinwuchat.bungee.json.RedisMessageType;
import org.lintx.plugins.yinwuchat.chat.struct.Chat;
import org.lintx.plugins.yinwuchat.json.MessageFormat;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.*;

public class RedisUtil {
    private static final String REDIS_SUBSCRIBE_CHANNEL = "yinwuchat-redis-subscribe-channel";
    private static JedisPool jedisPool;
    private static Subscribe subscribe;
    private static YinwuChat plugin;
    public static Map<String,String> playerList = new HashMap<>();//player-server

    static void init(YinwuChat plugin){
        RedisUtil.plugin = plugin;
        RedisConfig config = Config.getInstance().redisConfig;
        if (!config.openRedis) return;
        if (jedisPool != null || subscribe != null){
            unload();
        }
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(Config.getInstance().redisConfig.maxConnection);
        String password = config.password;
        if (password!=null && password.isEmpty()) password = null;
        jedisPool = new JedisPool(poolConfig,config.ip,config.port,0,password);
        subscribe = new Subscribe();

        plugin.getProxy().getScheduler().runAsync(plugin,()->{
            try (Jedis jedis = jedisPool.getResource())  {
                jedis.subscribe(subscribe,REDIS_SUBSCRIBE_CHANNEL);
            }catch (Exception ignored){}
        });
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
            plugin.getProxy().getScheduler().runAsync(plugin,()->{
//                String messageId = UUID.randomUUID().toString();
//                long time1 = System.nanoTime();
//                plugin.getLogger().info("Start processing redis messages[" + messageId + "],nanoTime:" + time1 + ",message:" + message);
                if (channel.equals(REDIS_SUBSCRIBE_CHANNEL)){
                    try {
                        Gson gson = new Gson();
                        RedisMessage obj = gson.fromJson(message,RedisMessage.class);

                        RedisUtil.onMessage(obj);
                    }catch (Exception ignored){
                        ignored.printStackTrace();
                    }
                }
//                long time2 = System.nanoTime();
//                long usageTime = time2 - time1;
//                plugin.getLogger().info("redis message processed[" + messageId + "],nanoTime:" + time2 + ",usage time:" + usageTime);
            });
        }
    }

    private static void onMessage(RedisMessage message){
        RedisConfig config = Config.getInstance().redisConfig;
        //自己服务器发送的消息不执行后续动作
        if (message.fromServer.equals(config.selfName)){
            return;
        }

        //对单个服务器发送的消息，如果目标不是自己，则不执行后续动作
        if (!"".equals(message.toServer) && !message.toServer.equals(config.selfName)){
            return;
        }

        ProxiedPlayer player = null;
        PlayerConfig.Player pc = null;

        //对单个玩家发送的消息的前期处理
        if (!"".equals(message.toPlayerName)){
            player = YinwuChat.getPlugin().getProxy().getPlayer(message.toPlayerName);
            if (player==null) return;
            pc = PlayerConfig.getConfig(player);
            //如果该玩家忽略了发送消息的玩家的消息，则退出
            if (pc.isIgnore(message.fromPlayerUUID)) return;
        }

        if (!"".equals(message.message)) message.chat = ComponentSerializer.parse(message.message)[0];

        switch (message.type){
            case AT_PLAYER:
                //AT单个玩家
                if (!config.forwardBcAtOne) break;
                if (player==null) break;
                if (pc.banAt) break;
                player.sendMessage(message.chat);

                if (pc.muteAt) break;
                ByteArrayDataOutput output = ByteStreams.newDataOutput();
                output.writeUTF(Const.PLUGIN_SUB_CHANNEL_AT);
                player.getServer().sendData(Const.PLUGIN_CHANNEL,output.toByteArray());
                break;
            case AT_PLAYER_ALL:
                //AT 所有人
                if (!config.forwardBcAtAll) break;
                ByteArrayDataOutput output1 = ByteStreams.newDataOutput();
                output1.writeUTF(Const.PLUGIN_SUB_CHANNEL_AT);
                for (ProxiedPlayer p : YinwuChat.getPlugin().getProxy().getPlayers()){
                    p.sendMessage(message.chat);
                    p.getServer().sendData(Const.PLUGIN_CHANNEL,output1.toByteArray());
                }
                break;
            case PUBLIC_MESSAGE:
            case TASK:
                //公屏消息
                if (message.type==RedisMessageType.TASK && !config.forwardBcTask) break;
                for (ProxiedPlayer p : YinwuChat.getPlugin().getProxy().getPlayers()){
                    if (!"".equals(message.toMCServer)){
                        if (!p.getServer().getInfo().getName().equalsIgnoreCase(message.toMCServer)){
                            continue;
                        }
                    }
                    PlayerConfig.Player pc2 = PlayerConfig.getConfig(p);
                    if (pc2.isIgnore(message.fromPlayerUUID)) continue;
                    p.sendMessage(message.chat);

                    if (plugin.wsIsOn() && config.forwardBcMessageToWeb){
                        String webmessage = message.chat.toLegacyText();
                        JsonObject webjson = new JsonObject();
                        webjson.addProperty("action", "send_message");
                        webjson.addProperty("message", webmessage);
                        String json = new Gson().toJson(webjson);

                        for (Channel channel : WsClientHelper.channels()) {
                            NettyChannelMessageHelper.send(channel,json);
                        }
                    }
                    if (Config.getInstance().coolQConfig.coolQGameToQQ && config.forwardBcMessageToQQ){
                        Channel channel = WsClientHelper.getCoolQ();
                        if (channel!=null){
                            String qqmessage = message.chat.toPlainText();
                            qqmessage = qqmessage.replaceAll("§([0-9a-fklmnor])","");
                            try {
                                NettyChannelMessageHelper.send(channel,new OutputCoolQ(qqmessage).getJSON());
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }
                }
                break;
            case PRIVATE_MESSAGE:
                //私聊消息
                if (player==null) return;
                player.sendMessage(message.chat);
                break;
            case PLAYER_LIST:
                //玩家列表
                Collection<String> col = playerList.values();
                while (col.contains(message.fromServer)){
                    col.remove(message.fromServer);
                }
                for (String playerName:message.playerList){
                    playerList.put(playerName,message.fromServer);
                }
                MessageManage.getInstance().sendPlayerListToServer();
                break;
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
        Chat prefix = new Chat();
        TextComponent messageComponent = new TextComponent();
        List<MessageFormat> formats = Config.getInstance().redisConfig.selfPrefixFormat;
        if (formats!=null && !formats.isEmpty()){
            for (MessageFormat format:Config.getInstance().redisConfig.selfPrefixFormat){
                if (format.message==null || format.message.equals("")) continue;
                messageComponent.addExtra(prefix.buildFormat(format));
            }
        }
        messageComponent.addExtra(chat);

        RedisMessage message = new RedisMessage();
//        message.chat = chat;
        message.toMCServer = mcServer;
        message.message = ComponentSerializer.toString(messageComponent);
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
        if (jedisPool==null){
            return;
        }
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(REDIS_SUBSCRIBE_CHANNEL, new Gson().toJson(message));
            }
        });
    }
}
