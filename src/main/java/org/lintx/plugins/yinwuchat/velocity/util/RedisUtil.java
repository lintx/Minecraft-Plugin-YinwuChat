package org.lintx.plugins.yinwuchat.velocity.util;

import com.google.gson.JsonObject;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.lintx.plugins.yinwuchat.Util.Gson;
import org.lintx.plugins.yinwuchat.velocity.YinwuChat;
import org.lintx.plugins.yinwuchat.velocity.config.Config;
import org.lintx.plugins.yinwuchat.velocity.config.PlayerConfig;
import org.lintx.plugins.yinwuchat.velocity.config.RedisConfig;
import org.lintx.plugins.yinwuchat.velocity.httpserver.NettyChannelMessageHelper;
import org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper;
import org.lintx.plugins.yinwuchat.velocity.json.RedisMessage;
import org.lintx.plugins.yinwuchat.velocity.json.RedisMessageType;
import org.lintx.plugins.yinwuchat.json.MessageFormat;
import org.lintx.plugins.yinwuchat.velocity.chat.VelocityChat;
import io.netty.channel.Channel;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.lintx.plugins.yinwuchat.Const;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Velocity Redis 工具类
 * 用于跨集群聊天消息同步
 */
public class RedisUtil {
    private static final String REDIS_SUBSCRIBE_CHANNEL = "yinwuchat-redis-subscribe-channel";
    private static JedisPool jedisPool;
    private static Subscribe subscribe;
    private static YinwuChat plugin;
    public static Map<String, String> playerList = new ConcurrentHashMap<>(); // player-server

    public static void init(YinwuChat plugin) {
        RedisUtil.plugin = plugin;
        RedisConfig config = Config.getInstance().redisConfig;
        if (!config.openRedis) return;
        
        if (jedisPool != null || subscribe != null) {
            unload();
        }
        
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(config.maxConnection);
        String password = config.password;
        if (password != null && password.isEmpty()) password = null;
        jedisPool = new JedisPool(poolConfig, config.ip, config.port, 0, password);
        subscribe = new Subscribe();

        // 异步启动订阅
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(subscribe, REDIS_SUBSCRIBE_CHANNEL);
            } catch (Exception e) {
                plugin.getLogger().warn("[Redis] 订阅错误: " + e.getMessage());
            }
        }).schedule();
        
        plugin.getLogger().info("[Redis] 连接已初始化");
    }

    public static void unload() {
        try {
            if (subscribe != null) {
                subscribe.unsubscribe();
            }
        } catch (Exception ignored) {}
        subscribe = null;
        
        try {
            if (jedisPool != null) {
                jedisPool.close();
            }
        } catch (Exception ignored) {}
        jedisPool = null;
    }

    private static class Subscribe extends JedisPubSub {
        @Override
        public void onMessage(String channel, String message) {
            plugin.getProxy().getScheduler().buildTask(plugin, () -> {
                if (channel.equals(REDIS_SUBSCRIBE_CHANNEL)) {
                    try {
                        RedisMessage obj = Gson.gson().fromJson(message, RedisMessage.class);
                        RedisUtil.onMessage(obj);
                    } catch (Exception e) {
                        plugin.getLogger().warn("[Redis] 消息处理失败: " + e.getMessage());
                    }
                }
            }).schedule();
        }
    }

    private static void onMessage(RedisMessage message) {
        RedisConfig config = Config.getInstance().redisConfig;
        
        // 自己服务器发送的消息不执行后续动作
        if (message.fromServer.equals(config.selfName)) {
            return;
        }

        // 对单个服务器发送的消息，如果目标不是自己，则不执行后续动作
        if (!"".equals(message.toServer) && !message.toServer.equals(config.selfName)) {
            return;
        }

        Player player = null;
        PlayerConfig.PlayerSettings settings = null;

        // 对单个玩家发送的消息的前期处理
        if (!"".equals(message.toPlayerName)) {
            Optional<Player> optPlayer = plugin.getProxy().getPlayer(message.toPlayerName);
            if (optPlayer.isEmpty()) return;
            player = optPlayer.get();
            settings = PlayerConfig.getInstance().getSettings(player);
            // 如果该玩家忽略了发送消息的玩家的消息，则退出
            if (message.fromPlayerUUID != null && settings.isIgnored(message.fromPlayerUUID.toString())) return;
        }

        Component chat = null;
        if (!"".equals(message.message)) {
            try {
                chat = GsonComponentSerializer.gson().deserialize(message.message);
            } catch (Exception e) {
                chat = LegacyComponentSerializer.legacyAmpersand().deserialize(message.message);
            }
        }

        switch (message.type) {
            case AT_PLAYER:
                // AT单个玩家
                if (!config.forwardBcAtOne) break;
                if (player == null || chat == null) break;
                if (settings != null && settings.disableAtMention) break;
                player.sendMessage(chat);
                // 发送声音提示到 Bukkit 服务器
                sendAtSoundToPlayer(player, settings);
                break;
                
            case AT_PLAYER_ALL:
                // AT 所有人
                if (!config.forwardBcAtAll) break;
                if (chat == null) break;
                for (Player p : plugin.getProxy().getAllPlayers()) {
                    p.sendMessage(chat);
                }
                break;
                
            case PUBLIC_MESSAGE:
            case TASK:
                // 公屏消息
                if (message.type == RedisMessageType.TASK && !config.forwardBcTask) break;
                if (chat == null) break;
                
                for (Player p : plugin.getProxy().getAllPlayers()) {
                    if (!"".equals(message.toMCServer)) {
                        Optional<String> serverName = p.getCurrentServer().map(s -> s.getServerInfo().getName());
                        if (serverName.isEmpty() || !serverName.get().equalsIgnoreCase(message.toMCServer)) {
                            continue;
                        }
                    }
                    PlayerConfig.PlayerSettings ps = PlayerConfig.getInstance().getSettings(p);
                    if (message.fromPlayerUUID != null && ps.isIgnored(message.fromPlayerUUID.toString())) continue;
                    p.sendMessage(chat);
                }
                
                // 转发到 WebSocket
                if (config.forwardBcMessageToWeb) {
                    String webMessage = LegacyComponentSerializer.legacyAmpersand().serialize(chat);
                    JsonObject webJson = new JsonObject();
                    webJson.addProperty("action", "send_message");
                    webJson.addProperty("message", webMessage);
                    String json = Gson.gson().toJson(webJson);
                    
                    for (Channel channel : VelocityWsClientHelper.getChannels()) {
                        NettyChannelMessageHelper.send(channel, json);
                    }
                }
                break;
                
            case PRIVATE_MESSAGE:
                // 私聊消息
                if (player == null || chat == null) return;
                player.sendMessage(chat);
                break;
                
            case PLAYER_LIST:
                // 玩家列表
                playerList.values().removeIf(v -> v.equals(message.fromServer));
                for (String playerName : message.playerList) {
                    playerList.put(playerName, message.fromServer);
                }
                sendPlayerList();
                break;
                
            case UNKNOWN:
            default:
                break;
        }
    }

    /**
     * 发送公屏消息到其他集群
     */
    public static void sendMessage(UUID uuid, Component chat) {
        sendMessage(RedisMessageType.PUBLIC_MESSAGE, uuid, chat, "");
    }

    /**
     * 发送私聊消息到其他集群
     */
    public static void sendMessage(UUID uuid, Component chat, String toPlayer) {
        sendMessage(RedisMessageType.PRIVATE_MESSAGE, uuid, chat, toPlayer);
    }

    public static void sendMessage(RedisMessageType type, UUID uuid, Component chat, String toPlayer) {
        sendMessage(type, uuid, chat, toPlayer, "");
    }

    public static void sendMessage(RedisMessageType type, UUID uuid, Component chat, String toPlayer, String mcServer) {
        Config config = Config.getInstance();
        
        // 添加集群前缀
        Component finalComponent = chat;
        List<MessageFormat> prefixFormats = config.redisConfig.selfPrefixFormat;
        if (prefixFormats != null && !prefixFormats.isEmpty()) {
            Component prefix = Component.empty();
            VelocityChat velocityChat = new VelocityChat();
            for (MessageFormat format : prefixFormats) {
                if (format.message == null || format.message.isEmpty()) continue;
                prefix = prefix.append(velocityChat.buildFormat(format));
            }
            finalComponent = prefix.append(chat);
        }

        RedisMessage message = new RedisMessage();
        message.toMCServer = mcServer;
        message.message = GsonComponentSerializer.gson().serialize(finalComponent);
        message.toPlayerName = toPlayer;
        message.fromServer = config.redisConfig.selfName;
        message.type = type;
        message.fromPlayerUUID = uuid;
        
        if (!"".equals(toPlayer)) {
            String toServer = playerList.get(toPlayer);
            if (toServer != null) message.toServer = toServer;
        }
        
        sendMessage(message);
    }

    /**
     * 发送玩家列表到其他集群
     */
    public static void sendPlayerList() {
        RedisMessage message = new RedisMessage();
        message.type = RedisMessageType.PLAYER_LIST;
        message.fromServer = Config.getInstance().redisConfig.selfName;
        
        for (Player p : plugin.getProxy().getAllPlayers()) {
            message.playerList.add(p.getUsername());
        }
        
        sendMessage(message);
    }

    private static void sendMessage(RedisMessage message) {
        if (jedisPool == null) {
            return;
        }
        
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(REDIS_SUBSCRIBE_CHANNEL, Gson.gson().toJson(message));
            } catch (Exception e) {
                plugin.getLogger().warn("[Redis] 发布消息失败: " + e.getMessage());
            }
        }).schedule();
    }
    
    /**
     * 发送 AT 声音提示到玩家所在的 Bukkit 服务器
     */
    private static void sendAtSoundToPlayer(Player player, PlayerConfig.PlayerSettings settings) {
        Optional<ServerConnection> serverConnection = player.getCurrentServer();
        if (serverConnection.isEmpty()) {
            return;
        }
        
        try {
            // 检查玩家是否禁用了@提醒声音
            if (settings != null && settings.muteAtMention) {
                return;
            }
            
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF(Const.PLUGIN_SUB_CHANNEL_AT);
            
            // 发送到 Bukkit 服务器
            serverConnection.get().sendPluginMessage(
                    MinecraftChannelIdentifier.from(Const.PLUGIN_CHANNEL_BUKKIT),
                    output.toByteArray()
            );
            
        } catch (Exception e) {
            plugin.getLogger().warn("[Redis] 发送@提醒失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查 Redis 是否已连接
     */
    public static boolean isConnected() {
        return jedisPool != null;
    }
}
