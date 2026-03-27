package org.lintx.plugins.yinwuchat.velocity.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;

import java.util.List;
import java.util.Optional;
import org.lintx.plugins.yinwuchat.velocity.json.ItemRequest;
import org.lintx.plugins.yinwuchat.json.MessageFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.json.PrivateMessage;
import org.lintx.plugins.yinwuchat.json.PublicMessage;
import org.lintx.plugins.yinwuchat.chat.struct.ChatSource;
import org.lintx.plugins.yinwuchat.chat.struct.ChatType;
import org.lintx.plugins.yinwuchat.velocity.YinwuChat;
import org.lintx.plugins.yinwuchat.velocity.chat.VelocityChat;
import org.lintx.plugins.yinwuchat.velocity.chat.VelocityChatHandle;
import org.lintx.plugins.yinwuchat.chat.struct.ChatPlayer;
import org.lintx.plugins.yinwuchat.velocity.chat.VelocityChatStruct;
import org.lintx.plugins.yinwuchat.velocity.json.ItemResponse;
import org.lintx.plugins.yinwuchat.velocity.message.MessageManage;
import org.lintx.plugins.yinwuchat.velocity.config.PlayerConfig;
import org.lintx.plugins.yinwuchat.velocity.util.VelocityItemUtil;
import org.lintx.plugins.yinwuchat.velocity.cache.VelocityItemDisplayCache;
import org.lintx.plugins.yinwuchat.Util.BackpackViewDebugLogUtil;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

/**
 * Event listeners for Velocity proxy server
 * Handles player chat messages, plugin messages and other proxy events
 */
public class Listeners {
    private final YinwuChat plugin;
    private final Gson gson = new Gson();

    public Listeners(YinwuChat plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        // 对于签名消息，Velocity不处理聊天事件，让后端服务器完全处理
        // 这避免了Velocity消息发送API在签名消息环境下的问题
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        // Handle player disconnect if needed
        plugin.getLogger().debug("Player disconnected: " + player.getUsername());
        org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWebSocketFrameHandler.broadcastPlayerList(plugin);
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        PlayerConfig.TokenManager tokens = PlayerConfig.getInstance().getTokenManager();
        String token = tokens.getToken(player.getUniqueId());
        if (token != null && !token.isEmpty()) {
            tokens.bindToken(token, player.getUniqueId(), player.getUsername());
        }
        org.lintx.plugins.yinwuchat.velocity.message.MessageManage.getInstance()
            .deliverOfflineMessagesToPlayer(player);
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        // 在玩家连接到服务器后（包括跨服）触发刷新，此时 getCurrentServer() 已更新
        org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWebSocketFrameHandler.broadcastPlayerList(plugin);
    }

    // 注意：所有插件消息的处理都在 onPluginMessage 方法中统一处理
    // 避免多个处理器导致重复处理的问题
    
    /**
     * 监听来自后端服务器的插件消息
     * 处理 Bukkit 端发送的聊天消息和物品数据
     */
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        // 只处理来自服务器的消息
        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }
        
        // 检查频道
        String channel = event.getIdentifier().getId();
        if (!Const.PLUGIN_CHANNEL_VELOCITY.equalsIgnoreCase(channel) &&
            !channel.equalsIgnoreCase("yinwuchat:main") &&
            !channel.equalsIgnoreCase("bungeecord/main") &&
            !channel.equalsIgnoreCase("bungeecord:main") &&
            !channel.equalsIgnoreCase("BungeeCord")) {
            return;
        }
        
        ServerConnection serverConnection = (ServerConnection) event.getSource();
        Player player = serverConnection.getPlayer();
        
        try {
            ByteArrayDataInput input = ByteStreams.newDataInput(event.getData());
            String subChannel = input.readUTF();
            
            switch (subChannel) {
                case Const.PLUGIN_SUB_CHANNEL_PUBLIC_MESSAGE:
                    handlePublicMessage(player, input);
                    break;
                case Const.PLUGIN_SUB_CHANNEL_PRIVATE_MESSAGE:
                    handlePrivateMessage(player, input);
                    break;
                case Const.PLUGIN_SUB_CHANNEL_PLAYER_LIST:
                    handlePlayerListRequest(player);
                    break;
                case Const.PLUGIN_SUB_CHANNEL_ITEM_RESPONSE:
                    handleItemResponse(player, input);
                    break;
                case Const.PLUGIN_SUB_CHANNEL_ITEM_DISPLAY_STORE:
                    handleItemDisplayStore(player, serverConnection, input);
                    break;
                case Const.PLUGIN_SUB_CHANNEL_ITEM_DISPLAY_REQUEST:
                    handleItemDisplayRequest(player, serverConnection, input);
                    break;
                default:
                    plugin.getLogger().debug("Unknown sub-channel: " + subChannel);
            }
            
            // 标记消息已处理，不再转发
            event.setResult(PluginMessageEvent.ForwardResult.handled());
            
        } catch (Exception e) {
            plugin.getLogger().warn("Failed to handle plugin message", e);
        }
    }
    
    /**
     * 处理公屏消息
     */
    private void handlePublicMessage(Player player, ByteArrayDataInput input) {
        try {
            String json = input.readUTF();
            PublicMessage publicMessage = gson.fromJson(json, PublicMessage.class);

            if (publicMessage.chat == null || publicMessage.chat.isEmpty()) {
                plugin.getLogger().debug("Public message chat is empty, ignoring");
                return;
            }
            int itemCount = publicMessage.items != null ? publicMessage.items.size() : 0;
            if (publicMessage.chat.contains("[i]")) {
                plugin.getLogger().info("[i] 物品展示已生成: player=" + publicMessage.player + ", items=" + itemCount);
            } else {
                plugin.getLogger().debug("Processing public message: player=" + publicMessage.player + ", chat=" + publicMessage.chat + ", items=" + itemCount);
            }

            // 调用 MessageManage 处理消息（包含物品展示）
            MessageManage.getInstance().handleBukkitPublicMessage(player, publicMessage);

        } catch (Exception e) {
            plugin.getLogger().warn("Failed to handle public message", e);
        }
    }
    
    /**
     * 处理私聊消息
     */
    private void handlePrivateMessage(Player player, ByteArrayDataInput input) {
        try {
            String json = input.readUTF();
            PrivateMessage privateMessage = gson.fromJson(json, PrivateMessage.class);
            
            if (privateMessage.chat == null || privateMessage.chat.isEmpty()) {
                return;
            }
            
            // 调用 MessageManage 处理私聊消息
            MessageManage.getInstance().handleBukkitPrivateMessage(player, privateMessage);
            
        } catch (Exception e) {
            plugin.getLogger().warn("Failed to handle private message", e);
        }
    }
    
    /**
     * 处理玩家列表请求
     */
    private void handlePlayerListRequest(Player player) {
        // 向请求的服务器发送玩家列表
        MessageManage.getInstance().sendPlayerListToServer(player);
    }

    /**
     * 处理包含物品占位符的聊天消息
     */
    private void handleChatWithItems(Player player, String originalMessage) {
        // Check if message contains [i] placeholders
        if (!originalMessage.contains("[i")) {
            // No item placeholders, handle normally
            MessageManage.getInstance().handlePlayerChat(player, originalMessage);
            return;
        }

        // Send item request to backend server
        requestPlayerItemsForChat(player, originalMessage);
    }

    /**
     * 为聊天消息请求玩家物品信息
     */
    private void requestPlayerItemsForChat(Player player, String message) {
        Optional<ServerConnection> serverConnection = player.getCurrentServer();
        if (serverConnection.isEmpty()) {
            player.sendMessage(Component.text("您当前未连接到任何服务器！", NamedTextColor.RED));
            return;
        }

        try {
            // Create item request for chat
            ItemRequest request = new ItemRequest(player.getUsername(), "chat_items");
            request.targetPlayer = player.getUsername(); // For chat context

            // Send plugin message to backend server
            String jsonRequest = gson.toJson(request);
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF(Const.PLUGIN_SUB_CHANNEL_ITEM_REQUEST);
            output.writeUTF(jsonRequest);

            serverConnection.get().sendPluginMessage(
                com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier.from(Const.PLUGIN_CHANNEL),
                output.toByteArray()
            );

            // Message will be stored in MessageManage for later processing

        } catch (Exception e) {
            player.sendMessage(Component.text("处理聊天消息时出错", NamedTextColor.RED));
            plugin.getLogger().warn("Failed to request items for chat", e);
        }
    }

    /**
     * 处理物品展示存储请求
     * Bukkit 端发送物品数据到 Velocity 缓存
     */
    private void handleItemDisplayStore(Player player, ServerConnection serverConnection, ByteArrayDataInput input) {
        try {
            String itemId = input.readUTF();
            String itemJson = input.readUTF();
            String playerName = input.readUTF();
            String serverName = serverConnection.getServerInfo().getName();
            plugin.getLogger().debug("[backpackview] velocity store payload: "
                    + BackpackViewDebugLogUtil.summarizeDisplayResponse(itemId, true, itemJson, playerName, serverName));
            
            // 存储到 Velocity 缓存
            VelocityItemDisplayCache.getInstance().storeItem(itemId, itemJson, playerName, serverName);
            plugin.getLogger().debug("Stored item display: id=" + itemId + ", player=" + playerName + ", server=" + serverName);
            
        } catch (Exception e) {
            plugin.getLogger().warn("Failed to store item display", e);
        }
    }
    
    /**
     * 处理物品展示请求
     * Bukkit 端请求从 Velocity 缓存获取物品数据
     */
    private void handleItemDisplayRequest(Player player, ServerConnection serverConnection, ByteArrayDataInput input) {
        try {
            String itemId = input.readUTF();
            plugin.getLogger().debug("[backpackview] velocity fetch cached payload request: "
                    + BackpackViewDebugLogUtil.summarizeDisplayRequest(itemId, player.getUsername()));
            
            // 从 Velocity 缓存获取物品
            VelocityItemDisplayCache.CachedItem cached = VelocityItemDisplayCache.getInstance().getItem(itemId);
            
            // 发送响应回 Bukkit
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF(Const.PLUGIN_SUB_CHANNEL_ITEM_DISPLAY_RESPONSE);
            output.writeUTF(itemId);
            
            if (cached != null) {
                output.writeBoolean(true);  // success
                output.writeUTF(cached.itemJson);
                output.writeUTF(cached.playerName);
                output.writeUTF(cached.serverName);
                plugin.getLogger().debug("[backpackview] velocity fetch cached payload hit: "
                        + BackpackViewDebugLogUtil.summarizeDisplayResponse(itemId, true, cached.itemJson, cached.playerName, cached.serverName));
                plugin.getLogger().debug("Sending item display response: id=" + itemId + ", success=true");
            } else {
                output.writeBoolean(false); // not found
                output.writeUTF("");
                output.writeUTF("");
                output.writeUTF("");
                plugin.getLogger().debug("[backpackview] velocity fetch cached payload miss: "
                        + BackpackViewDebugLogUtil.summarizeDisplayResponse(itemId, false, "", "", ""));
                plugin.getLogger().debug("Sending item display response: id=" + itemId + ", success=false (not found)");
            }
            
            serverConnection.sendPluginMessage(
                MinecraftChannelIdentifier.from(Const.PLUGIN_CHANNEL_BUKKIT),
                output.toByteArray()
            );
            
        } catch (Exception e) {
            plugin.getLogger().warn("Failed to handle item display request", e);
        }
    }

    /**
     * 处理物品响应
     */
    private void handleItemResponse(Player player, ByteArrayDataInput input) {
        try {
            String jsonResponse = input.readUTF();

            ItemResponse response = gson.fromJson(jsonResponse, ItemResponse.class);
            plugin.getLogger().debug("Parsed item response: player=" + player.getUsername() + ", success=" + response.success + ", items=" + (response.items != null ? response.items.size() : 0));

            // 委托给 MessageManage 处理
            MessageManage.getInstance().handleItemResponse(player, response);

        } catch (Exception e) {
            player.sendMessage(Component.text("处理物品响应时出错", NamedTextColor.RED));
            plugin.getLogger().warn("Failed to handle item response", e);
        }
    }

    /**
     * 显示手中物品
     */
    private void displayHandItem(Player player, ItemResponse response) {
        if (response.items == null || response.items.isEmpty()) {
            player.sendMessage(Component.text("您手中没有物品", NamedTextColor.YELLOW));
            return;
        }

        // 显示手中物品
        Component itemDisplay = VelocityItemUtil.createItemComponent(
            response.items.get(0),
            "手中物品",
            1
        );

        player.sendMessage(Component.text("您的手中物品:", NamedTextColor.GREEN));
        player.sendMessage(itemDisplay);
    }

    /**
     * 显示背包
     */
    private void displayInventory(Player player, ItemResponse response) {
        if (response.items == null || response.items.isEmpty()) {
            player.sendMessage(Component.text("您的背包是空的", NamedTextColor.YELLOW));
            return;
        }

        Component invDisplay = VelocityItemUtil.createInventoryComponent(
            String.join(",", response.items),
            player.getUsername()
        );

        player.sendMessage(Component.text("您的背包:", NamedTextColor.GREEN));
        player.sendMessage(invDisplay);
    }

    /**
     * 显示末影箱
     */
    private void displayEnderChest(Player player, ItemResponse response) {
        if (response.items == null || response.items.isEmpty()) {
            player.sendMessage(Component.text("您的末影箱是空的", NamedTextColor.YELLOW));
            return;
        }

        Component ecDisplay = VelocityItemUtil.createEnderChestComponent(
            String.join(",", response.items),
            player.getUsername()
        );

        player.sendMessage(Component.text("您的末影箱:", NamedTextColor.GREEN));
        player.sendMessage(ecDisplay);
    }

    /**
     * 处理包含物品占位符的聊天消息
     */
    private void handleChatMessageWithItems(Player player, String originalMessage, ItemResponse response) {
        try {
            // 创建 ChatPlayer 对象
            ChatPlayer fromPlayer = new ChatPlayer();
            fromPlayer.playerName = player.getUsername();

            // 构建消息结构
            VelocityChatStruct struct = new VelocityChatStruct();
            struct.chat = originalMessage;
            java.util.List<VelocityChatStruct> chatStructList = new java.util.ArrayList<>();
            chatStructList.add(struct);

            VelocityChat chat = new VelocityChat(fromPlayer, chatStructList, ChatSource.VELOCITY_PROXY);
            chat.type = ChatType.PUBLIC;

            // 物品数据的设置在 MessageManage.handleChatMessageWithItems 中处理

            // 应用物品显示处理器
            for (VelocityChatHandle handle : MessageManage.getInstance().getHandles()) {
                if (handle instanceof org.lintx.plugins.yinwuchat.velocity.chat.VelocityItemShowHandle) {
                    handle.handle(chat);
                    break; // 只应用物品显示处理器
                }
            }

            // 构建最终消息 - 使用默认格式
            List<MessageFormat> defaultFormats = MessageManage.getInstance().createDefaultFormats();
            Component finalMessage = chat.buildPublicMessage(defaultFormats);

            // 发送处理后的消息给发送者（物品显示是个人化的，不需要广播）
            // 广播给所有玩家而不是只给发送者，因为物品信息应该对所有人可见
            for (Player onlinePlayer : plugin.getProxy().getAllPlayers()) {
                onlinePlayer.sendMessage(finalMessage);
            }
            plugin.getLogger().debug("Broadcast processed message from player " + player.getUsername());

        } catch (Exception e) {
            player.sendMessage(Component.text("处理聊天消息失败，请稍后再试", NamedTextColor.RED));
            plugin.getLogger().warn("Failed to handle chat message with items", e);
        }
    }
}
