package org.lintx.plugins.yinwuchat.velocity.message;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.chat.struct.ChatPlayer;
import org.lintx.plugins.yinwuchat.chat.struct.ChatSource;
import org.lintx.plugins.yinwuchat.chat.struct.ChatType;
import org.lintx.plugins.yinwuchat.json.HandleConfig;
import org.lintx.plugins.yinwuchat.json.MessageFormat;
import org.lintx.plugins.yinwuchat.json.PrivateMessage;
import org.lintx.plugins.yinwuchat.velocity.announcement.AnnouncementTaskConfig;
import org.lintx.plugins.yinwuchat.json.PublicMessage;
import org.lintx.plugins.yinwuchat.velocity.YinwuChat;
import org.lintx.plugins.yinwuchat.velocity.chat.*;
import org.lintx.plugins.yinwuchat.velocity.config.Config;
import org.lintx.plugins.yinwuchat.velocity.config.PlayerConfig;
import org.lintx.plugins.yinwuchat.velocity.manage.MuteManage;
import org.lintx.plugins.yinwuchat.velocity.httpserver.NettyChannelMessageHelper;
import org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper;
import org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientUtil;
import org.lintx.plugins.yinwuchat.velocity.json.ItemRequest;
import org.lintx.plugins.yinwuchat.velocity.json.ItemResponse;
import org.lintx.plugins.yinwuchat.velocity.json.OutputAQQBot;
import org.lintx.plugins.yinwuchat.velocity.util.VelocityItemUtil;
import org.lintx.plugins.yinwuchat.velocity.util.RedisUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.netty.channel.Channel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Message management for Velocity proxy
 * Handles chat routing, filtering, and message transformation
 */
public class MessageManage {
    private static final MessageManage instance = new MessageManage();
    private YinwuChat plugin;
    private final List<VelocityChatHandle> handles = new ArrayList<>();
    private final Map<UUID, String> pendingChatMessages = new ConcurrentHashMap<>();
    private org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore offlineStore;
    private WebChatReplayStore replayStore;
    private static final int WEB_REPLAY_PUBLIC_LIMIT = 300;
    private static final int WEB_REPLAY_PRIVATE_LIMIT = 300;
    
    public MessageManage() {
        // Initialize message handlers in order
        handles.add(new VelocityCoolQCodeHandle());
        handles.add(new VelocityCoolQEscapeHandle());
        handles.add(new VelocityItemShowHandle());
        handles.add(new VelocityAtMentionHandle()); // @ 提及处理器
        handles.add(new VelocityLinkHandle());
        handles.add(new VelocityStyleSymbolHandle());
        handles.add(new VelocityStylePermissionHandle());
        handles.add(new VelocityExtraDataHandle());
    }

    public static MessageManage getInstance() {
        return instance;
    }

    private YinwuChat getPluginSafe() {
        return plugin != null ? plugin : YinwuChat.getInstance();
    }

    private org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore getOfflineStore() {
        if (offlineStore == null) {
            YinwuChat current = getPluginSafe();
            if (current != null) {
                offlineStore = new org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore(current.getDataFolder().toFile());
            }
        }
        return offlineStore;
    }

    private WebChatReplayStore getReplayStore() {
        if (replayStore == null) {
            YinwuChat current = getPluginSafe();
            if (current != null) {
                replayStore = new WebChatReplayStore(current.getDataFolder().toFile());
            }
        }
        return replayStore;
    }

    /**
     * 获取处理器列表（用于外部访问）
     */
    public List<VelocityChatHandle> getHandles() {
        return handles;
    }

    /**
     * 创建默认的消息格式
     */
    public List<MessageFormat> createDefaultFormats() {
        List<MessageFormat> formats = new ArrayList<>();

        // 服务器名前缀
        MessageFormat serverFormat = new MessageFormat();
        serverFormat.message = "&b[ServerName]";
        formats.add(serverFormat);

        // 玩家名
        MessageFormat playerFormat = new MessageFormat();
        playerFormat.message = "&e{displayName}";
        formats.add(playerFormat);

        // 消息分隔符
        MessageFormat separatorFormat = new MessageFormat();
        separatorFormat.message = " &6>>> ";
        formats.add(separatorFormat);

        // 消息内容
        MessageFormat messageFormat = new MessageFormat();
        messageFormat.message = "&r{message}";
        formats.add(messageFormat);

        return formats;
    }

    /**
     * 处理物品响应
     * 当收到后端服务器的物品数据时调用
     */
    public void handleItemResponse(Player player, ItemResponse response) {
        // 检查是否有待处理的聊天消息
        String pendingMessage = pendingChatMessages.remove(player.getUniqueId());
        if (pendingMessage != null) {
            // 处理包含物品占位符的聊天消息
            handleChatMessageWithItems(player, pendingMessage, response);
            return;
        }

        // 处理普通物品显示命令的响应
        handleNormalItemResponse(player, response);
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
            List<VelocityChatStruct> chatStructList = new ArrayList<>();
            chatStructList.add(struct);

            VelocityChat chat = new VelocityChat(fromPlayer, chatStructList, ChatSource.VELOCITY_PROXY);
            chat.type = ChatType.PUBLIC;

            // 设置物品数据用于 [i] 占位符替换
            if (response.items != null && !response.items.isEmpty()) {
                // 过滤掉空的物品字符串
                List<String> validItems = response.items.stream()
                    .filter(item -> item != null && !item.trim().isEmpty())
                    .collect(java.util.stream.Collectors.toList());

                if (!validItems.isEmpty()) {
                    chat.items = parseItemComponents(validItems);
                } else {
                    // 如果没有有效物品，创建一个空的物品列表，[i] 会被替换为"手中无物品"
                    chat.items = new ArrayList<>();
                }
            } else {
                chat.items = new ArrayList<>();
            }

            // 应用物品显示处理器
            for (VelocityChatHandle handle : handles) {
                if (handle instanceof VelocityItemShowHandle) {
                    handle.handle(chat);
                    break; // 只应用物品显示处理器
                }
            }

            // 构建最终消息 - 使用默认格式
            List<MessageFormat> defaultFormats = createDefaultFormats();
            Component finalMessage = chat.buildPublicMessage(defaultFormats);

            // 发送处理后的消息给特定玩家
            sendMessage(player, finalMessage);

        } catch (Exception e) {
            player.sendMessage(Component.text("处理聊天消息失败，请稍后再试", NamedTextColor.RED));
            plugin.getLogger().warn("Failed to handle chat message with items", e);
        }
    }

    /**
     * 处理普通物品显示命令的响应
     */
    private void handleNormalItemResponse(Player player, ItemResponse response) {
        if (!response.success) {
            // 请求失败，显示错误信息
            player.sendMessage(Component.text("获取物品信息失败: " + response.errorMessage, NamedTextColor.RED));
            return;
        }

        // 根据请求类型处理响应
        switch (response.requestType) {
            case "hand":
            case "chat_items":
                displayHandItem(player, response);
                break;
            case "inventory":
                displayInventory(player, response);
                break;
            case "enderchest":
                displayEnderChest(player, response);
                break;
            default:
                player.sendMessage(Component.text("未知的物品请求类型: " + response.requestType, NamedTextColor.RED));
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
     * 处理来自 Web 的禁言指令
     */
    public void handleWebPlayerMuteCommand(Channel channel, String playerName, String command) {
        String[] args = command.split("\\s+");
        if (args.length == 0) return;
        
        String label = args[0].toLowerCase();
        YinwuChat plugin = getPluginSafe();
        if (plugin == null) return;

        org.lintx.plugins.yinwuchat.velocity.manage.MuteManage manager = org.lintx.plugins.yinwuchat.velocity.manage.MuteManage.getInstance();
        
        if (label.equals("mute")) {
            if (args.length < 2) {
                NettyChannelMessageHelper.send(channel, org.lintx.plugins.yinwuchat.velocity.json.OutputServerMessage.errorJSON("用法: /mute <玩家名> [时间(分钟)] [原因]").getJSON());
                return;
            }
            String target = args[1];
            long duration = 0;
            if (args.length >= 3) {
                try {
                    duration = Long.parseLong(args[2]) * 60; // 转为秒
                } catch (NumberFormatException ignored) {}
            }
            String reason = args.length >= 4 ? String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length)) : "";
            
            manager.mutePlayer(target, duration, playerName, reason);
            NettyChannelMessageHelper.send(channel, org.lintx.plugins.yinwuchat.velocity.json.OutputServerMessage.infoJSON("✓ 已禁言玩家 " + target + (duration > 0 ? " " + (duration/60) + " 分钟" : " (永久)")).getJSON());
        } else if (label.equals("unmute")) {
            if (args.length < 2) {
                NettyChannelMessageHelper.send(channel, org.lintx.plugins.yinwuchat.velocity.json.OutputServerMessage.errorJSON("用法: /unmute <玩家名>").getJSON());
                return;
            }
            String target = args[1];
            manager.unmutePlayer(target, playerName);
            NettyChannelMessageHelper.send(channel, org.lintx.plugins.yinwuchat.velocity.json.OutputServerMessage.infoJSON("✓ 已解除玩家 " + target + " 的禁言").getJSON());
        } else if (label.equals("muteinfo")) {
            if (args.length < 2) {
                NettyChannelMessageHelper.send(channel, org.lintx.plugins.yinwuchat.velocity.json.OutputServerMessage.errorJSON("用法: /muteinfo <玩家名>").getJSON());
                return;
            }
            String target = args[1];
            PlayerConfig.PlayerSettings settings = PlayerConfig.getInstance().getSettings(target);
            if (!settings.muted) {
                NettyChannelMessageHelper.send(channel, org.lintx.plugins.yinwuchat.velocity.json.OutputServerMessage.infoJSON("玩家 " + target + " 当前未被禁言").getJSON());
            } else {
                long remaining = settings.getRemainingMuteTime();
                String timeStr = remaining == -1 ? "永久" : (remaining / 60) + " 分钟";
                NettyChannelMessageHelper.send(channel, org.lintx.plugins.yinwuchat.velocity.json.OutputServerMessage.infoJSON("玩家 " + target + " 处于禁言状态\n剩余时间: " + timeStr + "\n原因: " + settings.muteReason).getJSON());
            }
        }
    }

    public void setPlugin(YinwuChat plugin) {
        this.plugin = plugin;
    }

    public void handlePlayerChat(Player player, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        // Check if message contains [i] placeholder
        if (message.contains("[i")) {
            handleChatWithItemPlaceholder(player, message);
            return;
        }

        // Normal message handling
        broadcastMessage(player, message);
    }

    /**
     * 处理包含物品占位符的聊天消息
     */
    private void handleChatWithItemPlaceholder(Player player, String message) {
        Optional<ServerConnection> serverConnection = player.getCurrentServer();
        if (serverConnection.isEmpty()) {
            player.sendMessage(Component.text("您当前未连接到任何服务器！", NamedTextColor.RED));
            return;
        }

        try {
            // Send item request to backend server
            ItemRequest request = new ItemRequest(player.getUsername(), "chat_items");
            request.targetPlayer = player.getUsername();

            String jsonRequest = new Gson().toJson(request);

            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF(Const.PLUGIN_SUB_CHANNEL_ITEM_REQUEST);
            output.writeUTF(jsonRequest);

            // Try sending to bukkit-compatible channel first
            boolean sent = false;
            try {
                serverConnection.get().sendPluginMessage(
                    com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier.from(Const.PLUGIN_CHANNEL_BUKKIT),
                    output.toByteArray()
                );
                sent = true;
            } catch (Exception e) {
                // Bukkit channel not available, try velocity channel
            }

            // Also try sending to velocity channel
            if (!sent) {
                try {
                    serverConnection.get().sendPluginMessage(
                        com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier.from(Const.PLUGIN_CHANNEL_VELOCITY),
                        output.toByteArray()
                    );
                    sent = true;
                } catch (Exception e) {
                    plugin.getLogger().warn("[物品请求] 发送失败: " + e.getMessage());
                }
            }

            if (!sent) {
                plugin.getLogger().error("[物品请求] 无可用通道连接后端服务器");
                player.sendMessage(Component.text("无法连接到后端服务器获取物品信息", NamedTextColor.RED));
                return;
            }

            // Store pending chat message
            pendingChatMessages.put(player.getUniqueId(), message);

        } catch (Exception e) {
            plugin.getLogger().warn("[物品请求] 处理失败", e);
        }
    }

    /**
     * 广播普通聊天消息
     */
    private void broadcastMessage(Player player, String message) {
        // Forward to WebSocket clients
        if (YinwuChat.getWSServer() != null && plugin.getConfig().openwsserver) {
            try {
                JsonObject jsonMsg = new JsonObject();
                jsonMsg.addProperty("action", "send_message");
                jsonMsg.addProperty("message", message);
                jsonMsg.addProperty("player", player.getUsername());
                jsonMsg.addProperty("time", System.currentTimeMillis());
                
                String jsonStr = new Gson().toJson(jsonMsg);
                NettyChannelMessageHelper.broadcast(jsonStr);
            } catch (Exception e) {
                plugin.getLogger().warn("Failed to forward message to WebSocket", e);
            }
        }

        // Forward to other servers in cluster via Redis (if enabled)
        if (plugin.getConfig().redisConfig.openRedis && RedisUtil.isConnected()) {
            try {
                Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
                RedisUtil.sendMessage(player.getUniqueId(), component);
            } catch (Exception e) {
                plugin.getLogger().warn("Failed to forward message to Redis", e);
            }
        }
    }

    /**
     * 处理 @ 玩家
     */
    public void handleAtPlayer(Player fromPlayer, String playerName, String message) {
        // 查找目标玩家
        Optional<Player> targetOpt = plugin.getProxy().getPlayer(playerName);
        if (targetOpt.isEmpty()) {
            // 如果本地没有，尝试通过 Redis 发送
            if (plugin.getConfig().redisConfig.openRedis && RedisUtil.isConnected()) {
                if (RedisUtil.playerList.containsKey(playerName)) {
                    Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
                    RedisUtil.sendMessage(
                        org.lintx.plugins.yinwuchat.velocity.json.RedisMessageType.AT_PLAYER,
                        fromPlayer.getUniqueId(),
                        component,
                        playerName
                    );
                }
            }
            return;
        }
        
        Player target = targetOpt.get();
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        target.sendMessage(component);
    }

    /**
     * 清理可能包含签名信息的消息内容
     * Minecraft 1.19.1+ 签名消息可能包含内部格式，需要提取实际内容
     */
    private String cleanSignedMessage(String message) {
        if (message == null) return null;

        // 检查是否包含签名消息的格式 <chat=uuid:[content]:>
        if (message.startsWith("<chat=") && message.contains(":") && message.endsWith(">")) {
            try {
                // 格式：<chat=uuid:[content]:>
                // 我们需要提取[content]部分
                int uuidEnd = message.indexOf(":", 6); // 跳过<chat=
                if (uuidEnd > 6) {
                    int contentStart = message.indexOf("[", uuidEnd);
                    int contentEnd = message.lastIndexOf("]:");
                    if (contentStart >= 0 && contentEnd > contentStart) {
                        String content = message.substring(contentStart, contentEnd + 1);
                        return content;
                    }
                }
            } catch (Exception e) {
                // 如果解析失败，返回原消息
                plugin.getLogger().debug("Failed to parse signed message: " + message, e);
            }
        }

        return message;
    }

    /**
     * 处理来自 Bukkit 的公屏消息（包含物品数据）
     * 注意：此方法只处理包含[i]的消息，普通消息由Bukkit直接处理
     * @param player 发送消息的玩家
     * @param publicMessage 公屏消息数据
     */
    public void handleBukkitPublicMessage(Player player, PublicMessage publicMessage) {
        if (publicMessage.chat == null || publicMessage.chat.isEmpty()) {
            return;
        }

        // 检查禁言状态
        if (MuteManage.getInstance().checkMutedAndNotify(player)) {
            return;
        }

        // 清理可能包含签名信息的消息内容
        publicMessage.chat = cleanSignedMessage(publicMessage.chat);

        // 检查屏蔽词
        org.lintx.plugins.yinwuchat.velocity.manage.ShieldedManage.Result shieldedResult = 
                org.lintx.plugins.yinwuchat.velocity.manage.ShieldedManage.getInstance()
                        .checkShielded(player, publicMessage.chat);
        
        if (shieldedResult.kick) {
            // 玩家已被踢出，不继续处理
            return;
        }
        
        if (shieldedResult.shielded) {
            if (shieldedResult.end) {
                // 阻止模式：不发送消息
                return;
            }
            if (shieldedResult.msg != null && !shieldedResult.msg.isEmpty()) {
                // 替换模式：用配置的文本替换消息
                publicMessage.chat = shieldedResult.msg;
            }
        }

        // 创建 VelocityChatPlayer 对象（支持服务器名称获取）
        // 优先使用传递的服务器名称，否则自动检测
        org.lintx.plugins.yinwuchat.chat.struct.VelocityChatPlayer fromPlayer;
        if (publicMessage.serverName != null && !publicMessage.serverName.trim().isEmpty()) {
            fromPlayer = new org.lintx.plugins.yinwuchat.chat.struct.VelocityChatPlayer(player, publicMessage.serverName.trim());
        } else {
            fromPlayer = new org.lintx.plugins.yinwuchat.chat.struct.VelocityChatPlayer(player);
        }

        // 检查 QQ 消息前缀
        Config config = plugin.getConfig();
        boolean notQQ = true;
        String gameToCoolqStart = config.coolQConfig != null ? config.coolQConfig.gameToCoolqStart : "";
        if (gameToCoolqStart != null && !gameToCoolqStart.isEmpty()) {
            notQQ = !publicMessage.chat.startsWith(gameToCoolqStart);
        }

        // 获取玩家自定义前后缀（稍后在消息构建后添加，避免被占位符处理影响）
        String playerPrefix = "";
        String playerSuffix = "";
        if (config.allowPlayerFormatPrefixSuffix) {
            PlayerConfig.PlayerSettings playerSettings = PlayerConfig.getInstance().getSettings(player);
            if (playerSettings.publicPrefix != null && !playerSettings.publicPrefix.isEmpty()) {
                playerPrefix = playerSettings.publicPrefix;
            }
            if (playerSettings.publicSuffix != null && !playerSettings.publicSuffix.isEmpty()) {
                playerSuffix = playerSettings.publicSuffix;
            }
        }

        // 构建消息结构（不包含自定义前后缀）
        VelocityChatStruct struct = new VelocityChatStruct();
        struct.chat = publicMessage.chat;
        List<VelocityChatStruct> chatStructList = new ArrayList<>();
        chatStructList.add(struct);

        VelocityChat chat = new VelocityChat(fromPlayer, chatStructList, ChatSource.GAME);
        chat.extraData = publicMessage.handles;

        // 解析物品数据 - 这里需要从 Bukkit 服务器接收的物品 JSON 转换为 Adventure Component
        if (publicMessage.items != null && !publicMessage.items.isEmpty()) {
            chat.items = parseItemComponents(publicMessage.items);
        }

        // 应用消息处理器（链接、物品显示、样式等）
        for (VelocityChatHandle handle : handles) {
            handle.handle(chat);
        }
        
        // 在消息处理完成后，将自定义前后缀设置到 VelocityChat 中
        chat.setPlayerPrefixSuffix(playerPrefix, playerSuffix);

        // 构建最终消息
        Component messageComponent = chat.buildPublicMessage(publicMessage.format);
        
        // 广播消息到所有玩家，并向 Web 端附带结构化物品数据
        // 对 rawChat 应用 handle 替换（如 [p] → 彩色坐标），确保 Web 端与游戏内显示一致
        String webRawChat = applyHandlesToRawChat(publicMessage.chat, publicMessage.handles);
        broadcast(player.getUniqueId(), messageComponent, notQQ, publicMessage.items, webRawChat);
        
        // 处理 @ 提及：发送声音提示和消息给被@的玩家
        handleAtMentionNotifications(player, publicMessage.chat);
    }
    
    /**
     * 处理 @ 提及的通知
     * 向被@的玩家发送声音提示和文字提示
     */
    private void handleAtMentionNotifications(Player sender, String originalMessage) {
        handleAtMentionNotifications(sender.getUniqueId(), sender.getUsername(), originalMessage);
    }

    private void handleAtMentionNotifications(UUID senderUUID, String senderName, String originalMessage) {
        if (senderUUID == null) return;
        
        // 检查是否有被@的玩家
        if (!VelocityAtMentionHandle.hasMentionedPlayers(senderUUID)) {
            return;
        }
        
        // 检查冷却
        if (VelocityAtMentionHandle.isInCooldown(senderUUID)) {
            long remaining = VelocityAtMentionHandle.getRemainingCooldown(senderUUID);
            String tip = plugin.getConfig().tipsConfig.cooldownTip
                    .replace("{time}", String.valueOf(remaining));
            
            // 如果是游戏内玩家，发送提示
            plugin.getProxy().getPlayer(senderUUID).ifPresent(p -> p.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(tip)));
            
            // 清除被@的玩家列表
            VelocityAtMentionHandle.getMentionedPlayers(senderUUID);
            return;
        }
        
        // 获取被@的玩家列表
        Set<Player> mentionedPlayers = VelocityAtMentionHandle.getMentionedPlayers(senderUUID);
        if (mentionedPlayers == null || mentionedPlayers.isEmpty()) {
            return;
        }
        
        // 更新冷却
        VelocityAtMentionHandle.updateCooldown(senderUUID);
        
        // 获取提示消息
        String atYouTip = plugin.getConfig().tipsConfig.atyouTip
                .replace("{player}", senderName);
        Component tipComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(atYouTip);
        
        // 向每个被@的玩家发送声音提示
        for (Player mentionedPlayer : mentionedPlayers) {
            // 发送文字提示
            mentionedPlayer.sendMessage(tipComponent);
            
            // 发送声音提示到 Bukkit 服务器
            sendAtSoundToPlayer(mentionedPlayer);
        }
        
        // 如果启用了 Redis，向其他集群发送 AT 通知
        if (plugin.getConfig().redisConfig.openRedis && RedisUtil.isConnected()) {
            // 查找在其他集群的被@玩家
            sendAtNotificationsToRedis(senderUUID, senderName, originalMessage);
        }
    }
    
    /**
     * 发送 AT 声音提示到玩家所在的 Bukkit 服务器
     */
    private void sendAtSoundToPlayer(Player player) {
        Optional<ServerConnection> serverConnection = player.getCurrentServer();
        if (serverConnection.isEmpty()) {
            return;
        }
        
        try {
            // 检查玩家是否禁用了@提醒声音
            PlayerConfig.PlayerSettings settings = PlayerConfig.getInstance().getSettings(player);
            if (settings.muteAtMention) {
                return;
            }
            
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF(Const.PLUGIN_SUB_CHANNEL_AT);
            
            // 发送到 Bukkit 服务器
            serverConnection.get().sendPluginMessage(
                    MinecraftChannelIdentifier.from(Const.PLUGIN_CHANNEL_BUKKIT),
                    output.toByteArray()
            );
            
            plugin.getLogger().debug("Sent AT sound notification to " + player.getUsername());
        } catch (Exception e) {
            plugin.getLogger().warn("Failed to send AT sound to " + player.getUsername(), e);
        }
    }
    
    /**
     * 向其他 Redis 集群发送 AT 通知
     */
    private void sendAtNotificationsToRedis(UUID senderUUID, String senderName, String originalMessage) {
        // 解析消息中的@玩家名
        java.util.regex.Pattern atPattern = java.util.regex.Pattern.compile("@([a-zA-Z0-9_]{1,16})");
        java.util.regex.Matcher matcher = atPattern.matcher(originalMessage);
        
        while (matcher.find()) {
            String mentionName = matcher.group(1);
            String lowerMentionName = mentionName.toLowerCase();
            
            // 检查是否在 Redis 玩家列表中
            for (Map.Entry<String, String> entry : RedisUtil.playerList.entrySet()) {
                String playerName = entry.getKey();
                if (playerName.toLowerCase().startsWith(lowerMentionName)) {
                    // 找到匹配的跨集群玩家，发送 AT 通知
                    String atYouTip = plugin.getConfig().tipsConfig.atyouTip
                            .replace("{player}", senderName);
                    Component tipComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(atYouTip);
                    
                    RedisUtil.sendMessage(
                            org.lintx.plugins.yinwuchat.velocity.json.RedisMessageType.AT_PLAYER,
                            senderUUID,
                            tipComponent,
                            playerName
                    );
                    
                    break; // 只匹配一个玩家
                }
            }
        }
    }
    
    /**
     * 处理来自 Bukkit 的私聊消息
     */
    public void handleBukkitPrivateMessage(Player player, PrivateMessage privateMessage) {
        if (privateMessage.chat == null || privateMessage.chat.isEmpty()) {
            return;
        }

        // 查找目标玩家
        Player toPlayer = findPlayer(privateMessage.toPlayer);
        if (toPlayer == null || PlayerConfig.getInstance().getSettings(toPlayer).vanished) {
            // 目标玩家不在线或已隐身
            Component tipComponent = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(plugin.getConfig().tipsConfig.toPlayerNoOnlineTip);
            player.sendMessage(tipComponent);
            return;
        }

        // 检查忽略列表
        if (PlayerConfig.getInstance().getSettings(toPlayer).isIgnored(player.getUsername())) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(plugin.getConfig().tipsConfig.ignoreTip));
            return;
        }

        if (toPlayer.getUniqueId().equals(player.getUniqueId())) {
            // 不能给自己发消息
            Component tipComponent = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(plugin.getConfig().tipsConfig.msgyouselfTip);
            player.sendMessage(tipComponent);
            return;
        }

        // 获取玩家自定义前后缀（稍后在消息构建后添加，避免被占位符处理影响）
        Config config = plugin.getConfig();
        String playerPrefix = "";
        String playerSuffix = "";
        if (config.allowPlayerFormatPrefixSuffix) {
            PlayerConfig.PlayerSettings playerSettings = PlayerConfig.getInstance().getSettings(player);
            if (playerSettings.privatePrefix != null && !playerSettings.privatePrefix.isEmpty()) {
                playerPrefix = playerSettings.privatePrefix;
            }
            if (playerSettings.privateSuffix != null && !playerSettings.privateSuffix.isEmpty()) {
                playerSuffix = playerSettings.privateSuffix;
            }
        }

        ChatPlayer fromChatPlayer = new ChatPlayer();
        fromChatPlayer.playerName = player.getUsername();
        ChatPlayer toChatPlayer = new ChatPlayer();
        toChatPlayer.playerName = toPlayer.getUsername();

        VelocityChatStruct struct = new VelocityChatStruct();
        struct.chat = privateMessage.chat;
        List<VelocityChatStruct> chatStructList = new ArrayList<>();
        chatStructList.add(struct);

        VelocityChat chat = new VelocityChat(fromChatPlayer, toChatPlayer, chatStructList, ChatSource.GAME);
        chat.extraData = privateMessage.handles;

        // 解析物品数据
        if (privateMessage.items != null && !privateMessage.items.isEmpty()) {
            chat.items = parseItemComponents(privateMessage.items);
        }

        // 应用消息处理器
        for (VelocityChatHandle handle : handles) {
            handle.handle(chat);
        }
        
        // 在消息处理完成后，将自定义前后缀设置到 VelocityChat 中
        chat.setPlayerPrefixSuffix(playerPrefix, playerSuffix);

        // 构建消息
        Component toComponent = chat.buildPrivateToMessage(privateMessage.toFormat);
        Component fromComponent = chat.buildPrivateFormMessage(privateMessage.fromFormat);

        // 发送给发送者
        sendMessage(player, toComponent);
        // 发送给接收者
        sendMessage(toPlayer, fromComponent);

        long privateMessageId = 0L;
        WebChatReplayStore store = getReplayStore();
        if (store != null) {
            privateMessageId = store.appendPrivateMessage(player.getUsername(), toPlayer.getUsername(), privateMessage.chat);
        }
        
        // 同步到发送者的 Web 端
        syncPrivateMessageToWeb(player.getUniqueId(), player.getUsername(), toPlayer.getUsername(), privateMessage.chat, true, privateMessageId, false);
        
        // 同步到接收者的 Web 端
        syncPrivateMessageToWeb(toPlayer.getUniqueId(), player.getUsername(), toPlayer.getUsername(), privateMessage.chat, false, privateMessageId, false);

        // 监听私聊（管理员可见）
        sendPrivateMonitor(player.getUsername(), toPlayer.getUsername(), privateMessage.chat);

    }

    /**
     * 解析物品组件数据
     * 将来自 Bukkit 的物品 JSON 转换为 Adventure Component
     * 参考 InteractiveChat 的实现方式
     */
    public List<Component> parseItemComponents(List<String> items) {
        List<Component> components = new ArrayList<>();
        for (String itemJson : items) {
            if (itemJson == null || itemJson.isEmpty()) {
                components.add(Component.text("[物品]").color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
                continue;
            }
            try {
                // 解析物品信息
                String itemName = VelocityItemUtil.parseItemName(itemJson);
                int amount = VelocityItemUtil.parseItemAmount(itemJson);

                // 创建物品显示组件（参考 InteractiveChat 的样式）
                Component itemComponent = VelocityItemUtil.createItemComponent(itemJson, itemName, amount);

                components.add(itemComponent);
            } catch (Exception e) {
                // 解析失败，添加占位符
                plugin.getLogger().warn("[物品解析] 失败: " + e.getMessage());
                components.add(Component.text("[物品]").color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
            }
        }
        return components;
    }


    /**
     * 广播消息到所有在线玩家
     */
    public void broadcast(java.util.UUID senderUUID, Component component, boolean notQQ) {
        // 获取发送者名称（用于检查忽略列表）
        String senderName = "";
        String serverName = "";
        if (senderUUID != null) {
            Optional<Player> playerOpt = plugin.getProxy().getPlayer(senderUUID);
            if (playerOpt.isPresent()) {
                Player p = playerOpt.get();
                senderName = p.getUsername();
                serverName = p.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("");
            }
        }
        
        broadcast(senderUUID, senderName, serverName, component, notQQ, null, null);
    }

    public void broadcast(java.util.UUID senderUUID, Component component, boolean notQQ,
                          List<String> rawItems, String rawChat) {
        String senderName = "";
        String serverName = "";
        if (senderUUID != null) {
            Optional<Player> playerOpt = plugin.getProxy().getPlayer(senderUUID);
            if (playerOpt.isPresent()) {
                Player p = playerOpt.get();
                senderName = p.getUsername();
                serverName = p.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("");
            }
        }
        broadcast(senderUUID, senderName, serverName, component, notQQ, rawItems, rawChat);
    }

    public void broadcast(java.util.UUID senderUUID, String senderName, String serverName, Component component, boolean notQQ) {
        broadcast(senderUUID, senderName, serverName, component, notQQ, null, null);
    }

    /**
     * 广播消息到所有在线玩家，并可选附带 Web 端结构化物品数据
     */
    public void broadcast(java.util.UUID senderUUID, String senderName, String serverName, Component component, boolean notQQ,
                          List<String> rawItems, String rawChat) {
        // 发送给所有在线玩家，包括发送者
        // Bukkit 端已经取消了原始聊天事件，所以不会有重复显示
        for (Player p : plugin.getProxy().getAllPlayers()) {
            if (senderName != null && !senderName.isEmpty()) {
                PlayerConfig.PlayerSettings settings = PlayerConfig.getInstance().getSettings(p);
                if (settings.isIgnored(senderName)) {
                    continue;
                }
            }
            sendMessage(p, component);
        }

        // 转发到 WebSocket
        if (plugin.getConfig().openwsserver && YinwuChat.getWSServer() != null) {
            try {
                String webMessage = LegacyComponentSerializer.legacySection().serialize(component);
                WebChatReplayStore store = getReplayStore();
                long messageId = store != null
                        ? store.appendPublicMessage(senderName, serverName, webMessage, rawChat, rawItems)
                        : 0L;
                long messageTime = System.currentTimeMillis();
                for (io.netty.channel.Channel wsChannel : org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.getChannels()) {
                    org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientUtil util = 
                        org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.get(wsChannel);
                    
                    JsonObject json = new JsonObject();
                    json.addProperty("action", "send_message");
                    json.addProperty("message", webMessage);
                    json.addProperty("player", senderName != null ? senderName : "");
                    json.addProperty("time", messageTime);
                    if (messageId > 0) {
                        json.addProperty("message_id", messageId);
                    }
                    if (serverName != null && !serverName.isEmpty()) {
                        json.addProperty("server", serverName);
                    }

                    // 向新版本 Web 客户端附带结构化物品信息；旧端仅使用 message 字段
                    if (rawChat != null && !rawChat.isEmpty()) {
                        json.addProperty("raw_chat", buildWebChatTemplate(rawChat, rawItems == null ? 0 : rawItems.size()));
                    }
                    if (rawItems != null && !rawItems.isEmpty()) {
                        JsonArray webItems = buildWebItems(rawItems);
                        if (!webItems.isEmpty()) {
                            json.add("items", webItems);
                        }
                    }
                    
                    if (util != null && util.getUuid() != null) {
                        String currentName = PlayerConfig.getInstance().getTokenManager().getName(util.getUuid());
                        
                        // 检查忽略列表
                        if (senderName != null && !senderName.isEmpty() && currentName != null) {
                            PlayerConfig.PlayerSettings settings = PlayerConfig.getInstance().getSettings(currentName);
                            if (settings.isIgnored(senderName)) {
                                continue;
                            }
                        }

                        if (util.getUuid().equals(senderUUID)) {
                            json.addProperty("is_self", true);
                        }
                        
                        // 检查 Web 用户是否被 @ 提及
                        if (currentName != null && !currentName.isEmpty() && webMessage.contains("@" + currentName)) {
                            json.addProperty("mention", true);
                        }
                    }
                    
                    org.lintx.plugins.yinwuchat.velocity.httpserver.NettyChannelMessageHelper.send(wsChannel, new Gson().toJson(json));
                }
            } catch (Exception e) {
                plugin.getLogger().warn("[WebSocket] 消息转发失败: " + e.getMessage());
            }
        }

        // 发送到 QQ 群
        if (!notQQ && plugin.getConfig().coolQConfig != null && plugin.getConfig().coolQConfig.coolQGameToQQ) {
            Channel aqqBotChannel = VelocityWsClientHelper.getAQQBot();
            if (aqqBotChannel != null && aqqBotChannel.isActive()) {
                try {
                    String message = LegacyComponentSerializer.legacySection().serialize(component);
                    message = removeColorCodes(message);
                    OutputAQQBot outputAQQBot = new OutputAQQBot(message);
                    NettyChannelMessageHelper.send(aqqBotChannel, outputAQQBot.getJSON());
                } catch (Exception e) {
                    plugin.getLogger().warn("[QQ] 消息发送失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 将 HandleConfig（如 [p] 位置展示）中的占位符替换为已解析的格式文本，
     * 使 Web 端 raw_chat 与游戏内显示保持一致。
     */
    private String applyHandlesToRawChat(String rawChat, List<HandleConfig> handles) {
        if (rawChat == null || handles == null || handles.isEmpty()) return rawChat;
        String result = rawChat;
        for (HandleConfig handle : handles) {
            if (handle.placeholder == null || handle.format == null) continue;
            StringBuilder replacement = new StringBuilder();
            for (MessageFormat fmt : handle.format) {
                if (fmt.message != null) {
                    replacement.append(fmt.message);
                }
            }
            try {
                result = result.replaceAll(handle.placeholder, Matcher.quoteReplacement(replacement.toString()));
            } catch (Exception ignored) {}
        }
        return result;
    }

    /**
     * 将原始聊天内容中的 [i] 占位符替换为 Web 端可识别占位符。
     */
    private String buildWebChatTemplate(String rawChat, int itemCount) {
        if (rawChat == null || rawChat.isEmpty()) {
            return "";
        }
        Pattern itemPattern = Pattern.compile(Const.ITEM_PLACEHOLDER);
        Matcher matcher = itemPattern.matcher(rawChat);
        StringBuffer sb = new StringBuffer();
        int itemIndex = 0;
        while (matcher.find()) {
            if (itemIndex < itemCount) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement("[[ITEM:" + itemIndex + "]]"));
                itemIndex++;
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 将 Bukkit 传来的物品 JSON 列表转换为 Web 端可直接渲染的结构化数据。
     */
    private JsonArray buildWebItems(List<String> rawItems) {
        JsonArray arr = new JsonArray();
        for (String itemJson : rawItems) {
            if (itemJson == null || itemJson.trim().isEmpty()) {
                continue;
            }
            try {
                JsonObject raw = com.google.gson.JsonParser.parseString(itemJson).getAsJsonObject();
                JsonObject webItem = new JsonObject();
                webItem.addProperty("name", VelocityItemUtil.parseItemName(itemJson));
                webItem.addProperty("count", VelocityItemUtil.parseItemAmount(itemJson));
                if (raw.has("id")) {
                    webItem.addProperty("id", raw.get("id").getAsString());
                }
                if (raw.has("displayId")) {
                    webItem.addProperty("displayId", raw.get("displayId").getAsString());
                }

                JsonArray lore = new JsonArray();
                if (raw.has("lore") && raw.get("lore").isJsonArray()) {
                    for (JsonElement elem : raw.getAsJsonArray("lore")) {
                        lore.add(elem.getAsString());
                    }
                }
                // 将药水效果也追加到展示细节中（支持多效果）
                List<String> potionEffectLabels = VelocityItemUtil.parsePotionEffectLabels(itemJson);
                for (String label : potionEffectLabels) {
                    lore.add(label);
                }
                webItem.add("lore", lore);

                JsonArray enchants = new JsonArray();
                if (raw.has("enchantments") && raw.get("enchantments").isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : raw.getAsJsonObject("enchantments").entrySet()) {
                        String label = VelocityItemUtil.formatEnchantmentLabel(
                                entry.getKey(),
                                entry.getValue().getAsInt()
                        );
                        enchants.add(label);
                    }
                }
                webItem.add("enchantments", enchants);

                arr.add(webItem);
            } catch (Exception ignored) {
                // 单个物品解析失败不影响整条消息
            }
        }
        return arr;
    }

    /**
     * 发送消息给玩家
     */
    private void sendMessage(Player player, Component component) {
        try {
            // 直接使用 player.sendMessage 发送消息给特定玩家
            player.sendMessage(component);
        } catch (Exception e) {
            // 发送失败，忽略
        }
    }

    /**
     * 根据名称查找玩家
     */
    private Player findPlayer(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        
        String lowerName = name.toLowerCase();
        Player exactMatch = null;
        Player prefixMatch = null;
        
        for (Player p : plugin.getProxy().getAllPlayers()) {
            String playerName = p.getUsername().toLowerCase();
            if (playerName.equals(lowerName)) {
                exactMatch = p;
                break;
            }
            if (playerName.startsWith(lowerName) && prefixMatch == null) {
                prefixMatch = p;
            }
        }
        
        return exactMatch != null ? exactMatch : prefixMatch;
    }
    
    /**
     * 发送玩家列表到请求的服务器
     */
    public void sendPlayerListToServer(Player player) {
        if (player.getCurrentServer().isEmpty()) {
            return;
        }
        
        try {
            List<String> playerNames = new ArrayList<>();
            for (Player p : plugin.getProxy().getAllPlayers()) {
                playerNames.add(p.getUsername());
            }
            
            String json = new Gson().toJson(playerNames);
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF(Const.PLUGIN_SUB_CHANNEL_PLAYER_LIST);
            output.writeUTF(json);
            
            player.getCurrentServer().get().sendPluginMessage(
                MinecraftChannelIdentifier.from(Const.PLUGIN_CHANNEL),
                output.toByteArray()
            );
        } catch (Exception e) {
            plugin.getLogger().warn("[玩家列表] 发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送消息到QQ群（通过 AQQBot / OneBot 标准）
     * @param player 发送消息的玩家
     * @param message 消息内容
     * @return true表示发送成功，false表示发送失败
     */
    public boolean sendMessageToQQ(Player player, String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        Config config = plugin.getConfig();
        
        // 优先检查 AQQBot 配置，如果未启用则检查旧版 CoolQ 配置（向后兼容）
        boolean enabled;
        String denyStyle;
        if (config.aqqBotConfig != null) {
            enabled = config.aqqBotConfig.gameToQQ;
            denyStyle = config.aqqBotConfig.qqDenyStyle;
        } else {
            enabled = config.coolQConfig != null && config.coolQConfig.coolQGameToQQ;
            denyStyle = config.coolQConfig != null ? config.coolQConfig.qqDenyStyle : "";
        }
        
        // 检查是否启用了QQ群同步
        if (!enabled) {
            return false;
        }

        // 获取 AQQBot WebSocket 连接
        Channel aqqBotChannel = VelocityWsClientHelper.getAQQBot();
        if (aqqBotChannel == null || !aqqBotChannel.isActive()) {
            return false;
        }

        try {
            // 格式化消息：玩家名 + 消息内容
            String formattedMessage = String.format("[%s] %s", player.getUsername(), message);
            
            // 移除Minecraft颜色代码（§和&）
            formattedMessage = removeColorCodes(formattedMessage);
            
            // 根据配置过滤不允许的样式代码
            if (denyStyle != null && !denyStyle.isEmpty()) {
                formattedMessage = removeDeniedStyles(formattedMessage, denyStyle);
            }
            
            // 创建 OutputAQQBot 消息对象（OneBot 标准格式）
            OutputAQQBot outputAQQBot = new OutputAQQBot(formattedMessage);
            
            // 发送到 AQQBot WebSocket 连接
            NettyChannelMessageHelper.send(aqqBotChannel, outputAQQBot.getJSON());
            return true;
        } catch (Exception e) {
            plugin.getLogger().error("[QQ] 发送消息到QQ群失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 移除Minecraft颜色代码
     * @param message 原始消息
     * @return 移除颜色代码后的消息
     */
    private String removeColorCodes(String message) {
        // 移除 § 颜色代码
        message = message.replaceAll("§[0-9a-fA-Fk-oK-OrRxX]", "");
        // 移除 & 颜色代码
        message = message.replaceAll("&[0-9a-fA-Fk-oK-OrRxX]", "");
        // 移除 RGB 颜色代码
        message = message.replaceAll("§#[0-9a-fA-F]{6}", "");
        message = message.replaceAll("&#[0-9a-fA-F]{6}", "");
        return message;
    }

    /**
     * 移除配置中不允许的样式代码
     * @param message 原始消息
     * @param denyStyle 不允许的样式代码（如 "0-9a-fA-Fk-oK-OrRxX"）
     * @return 移除不允许样式后的消息
     */
    private String removeDeniedStyles(String message, String denyStyle) {
        // 构建正则表达式：匹配 § 或 & 后跟 denyStyle 中的字符
        String regex = "[§&][" + denyStyle.replace("-", "\\-") + "]";
        message = message.replaceAll(regex, "");
        return message;
    }

    /**
     * 处理私聊消息
     * @param sender 发送者
     * @param targetName 目标玩家名
     * @param message 消息内容
     */
    public void handlePrivateMessage(Player sender, String targetName, String message) {
        // 查找目标玩家
        Optional<Player> targetOptional = plugin.getProxy().getPlayer(targetName);
        Player target = targetOptional.orElse(null);
        
        // 注意：现在允许非管理玩家向隐身玩家发送消息，所以不再根据 vanish 状态屏蔽 target

        // 如果游戏内不在线，尝试查找 Web 端是否在线
        UUID targetUuid = null;
        String finalTargetName = targetName;
        if (target == null) {
            for (io.netty.channel.Channel wsChannel : VelocityWsClientHelper.getChannels()) {
                VelocityWsClientUtil util = VelocityWsClientHelper.get(wsChannel);
                if (util == null || util.getUuid() == null) continue;
                String name = PlayerConfig.getInstance().getTokenManager().getName(util.getUuid());
                if (targetName.equalsIgnoreCase(name)) {
                    targetUuid = util.getUuid();
                    finalTargetName = name;
                    break;
                }
            }
        } else {
            targetUuid = target.getUniqueId();
            finalTargetName = target.getUsername();
        }

        if (targetUuid == null) {
            // 存入离线消息
            org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore store = getOfflineStore();
            if (store != null) {
                org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore.OfflineMessage offline =
                    new org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore.OfflineMessage();
                offline.from = sender.getUsername();
                offline.to = targetName;
                offline.message = message;
                offline.time = System.currentTimeMillis();
                store.addMessage(targetName, offline);
            }
            sender.sendMessage(Component.text("玩家不在线，消息已存为离线留言: " + targetName).color(NamedTextColor.YELLOW));
            return;
        }

        // 检查忽略列表
        PlayerConfig.PlayerSettings targetSettings = PlayerConfig.getInstance().getSettings(finalTargetName);
        if (targetSettings.isIgnored(sender.getUsername())) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(plugin.getConfig().tipsConfig.ignoreTip));
            return;
        }

        // 检查是否是给自己发消息
        if (target != null && sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(Component.text("不能给自己发私聊消息").color(NamedTextColor.RED));
            return;
        }

        // 应用玩家自定义前后缀
        Config config = Config.getInstance();
        if (config.allowPlayerFormatPrefixSuffix) {
            PlayerConfig.PlayerSettings playerSettings = PlayerConfig.getInstance().getSettings(sender);
            if (playerSettings.privatePrefix != null && !playerSettings.privatePrefix.isEmpty()) {
                message = playerSettings.privatePrefix + message;
            }
            if (playerSettings.privateSuffix != null && !playerSettings.privateSuffix.isEmpty()) {
                message = message + playerSettings.privateSuffix;
            }
        }

        // 使用配置中的私聊格式
        List<MessageFormat> senderFormats = Config.getInstance().formatConfig.toFormat;
        List<MessageFormat> targetFormats = Config.getInstance().formatConfig.fromFormat;

        // 创建私聊消息组件
        Component senderMessage = buildPrivateMessage(senderFormats, sender.getUsername(), finalTargetName, message);
        Component targetMessage = buildPrivateMessage(targetFormats, sender.getUsername(), finalTargetName, message);

        // 发送消息
        sender.sendMessage(senderMessage);
        if (target != null) {
            target.sendMessage(targetMessage);
        }
        
        long privateMessageId = 0L;
        WebChatReplayStore store = getReplayStore();
        if (store != null) {
            privateMessageId = store.appendPrivateMessage(sender.getUsername(), finalTargetName, message);
        }

        // 同步到发送者的 Web 端
        syncPrivateMessageToWeb(sender.getUniqueId(), sender.getUsername(), finalTargetName, message, true, privateMessageId, false);
        
        // 同步到接收者的 Web 端
        syncPrivateMessageToWeb(targetUuid, sender.getUsername(), finalTargetName, message, false, privateMessageId, false);

        // 监听私聊（管理员可见）
        sendPrivateMonitor(sender.getUsername(), finalTargetName, message);
    }

    /**
     * 同步私聊消息到 Web 端
     */
    private void syncPrivateMessageToWeb(UUID playerUuid, String fromName, String toName, String message, boolean isSelf, long messageId, boolean replay) {
        if (playerUuid == null) return;
        for (io.netty.channel.Channel wsChannel : org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.getChannels()) {
            org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientUtil util = 
                org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.get(wsChannel);
            if (util != null && playerUuid.equals(util.getUuid())) {
                com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                json.addProperty("action", "private_message");
                json.addProperty("player", fromName);
                json.addProperty("to", toName);
                json.addProperty("message", message);
                json.addProperty("is_self", isSelf);
                json.addProperty("time", System.currentTimeMillis());
                if (messageId > 0) {
                    json.addProperty("message_id", messageId);
                }
                if (replay) {
                    json.addProperty("replay", true);
                }
                org.lintx.plugins.yinwuchat.velocity.httpserver.NettyChannelMessageHelper.send(wsChannel, json.toString());
            }
        }
    }

    /**
     * 构建私聊消息组件
     */
    private Component buildPrivateMessage(List<MessageFormat> formats, String fromPlayer, String toPlayer, String message) {
        Component result = Component.empty();

        for (MessageFormat format : formats) {
            if (format.message == null || format.message.isEmpty()) continue;

            // 替换占位符 - 支持多种占位符格式以确保兼容性
            String messageText = format.message
                .replace("{formPlayer}", fromPlayer)
                .replace("{toPlayer}", toPlayer)
                .replace("{displayName}", fromPlayer)  // 兼容旧格式
                .replace("{message}", message);

            Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(messageText);

            // 添加hover和click事件
            if (format.hover != null && !format.hover.isEmpty()) {
                String hoverText = format.hover
                    .replace("{formPlayer}", fromPlayer)
                    .replace("{toPlayer}", toPlayer)
                    .replace("{displayName}", fromPlayer)  // 兼容旧格式
                    .replace("{message}", message);
                component = component.hoverEvent(HoverEvent.showText(
                    LegacyComponentSerializer.legacyAmpersand().deserialize(hoverText)));
            }

            if (format.click != null && !format.click.isEmpty()) {
                String clickCommand = format.click
                    .replace("{formPlayer}", fromPlayer)
                    .replace("{toPlayer}", toPlayer)
                    .replace("{displayName}", fromPlayer)  // 兼容旧格式
                    .replace("{message}", message);

                ClickEvent.Action action = determineClickAction(clickCommand);
                component = component.clickEvent(ClickEvent.clickEvent(action, clickCommand));
            }

            result = result.append(component);
        }

        return result;
    }

    // 确定点击动作类型
    private ClickEvent.Action determineClickAction(String click) {
        Pattern pattern = Pattern.compile("((https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])");
        Matcher matcher = pattern.matcher(click);

        if (matcher.find()) {
            return ClickEvent.Action.OPEN_URL;
        } else if (click.startsWith("/msg")) {
            return ClickEvent.Action.SUGGEST_COMMAND;  // 私聊命令建议输入而不是直接运行
        } else if (click.startsWith("/")) {
            return ClickEvent.Action.RUN_COMMAND;  // 其他命令直接运行
        } else if (click.startsWith("!")) {
            return ClickEvent.Action.RUN_COMMAND;
        } else {
            return ClickEvent.Action.SUGGEST_COMMAND;
        }
    }
    
    /**
     * 处理来自 Web 的公屏消息
     */
    public void handleWebPublicMessage(UUID playerUuid, String message, io.netty.channel.Channel channel) {
        if (message == null || message.isEmpty()) return;
        
        // 获取玩家名称
        String playerName = plugin.getProxy().getPlayer(playerUuid)
            .map(Player::getUsername)
            .orElse("");
        if (playerName.isEmpty()) {
            playerName = org.lintx.plugins.yinwuchat.velocity.config.PlayerConfig
                .getInstance()
                .getTokenManager()
                .getName(playerUuid);
            if (playerName == null || playerName.isEmpty()) {
                playerName = "Web用户";
            }
        }

        // 检查禁言状态（Web 端）
        if (isWebMuted(playerName, channel)) {
            return;
        }
        
        // 检查屏蔽词（带账户名，用于 Web 端封禁）
        String accountName = null;
        for (io.netty.channel.Channel wsChannel : VelocityWsClientHelper.getChannels()) {
            VelocityWsClientUtil wsUtil = VelocityWsClientHelper.get(wsChannel);
            if (wsUtil != null && playerUuid.equals(wsUtil.getUuid())) {
                accountName = wsUtil.getAccount();
                break;
            }
        }
        org.lintx.plugins.yinwuchat.velocity.manage.ShieldedManage.Result shieldedResult = 
                org.lintx.plugins.yinwuchat.velocity.manage.ShieldedManage.getInstance()
                        .checkShielded(channel, playerUuid.toString(), accountName, message);
        
        if (shieldedResult.kick) return;
        if (shieldedResult.shielded) {
            if (shieldedResult.end) return;
            if (shieldedResult.msg != null && !shieldedResult.msg.isEmpty()) message = shieldedResult.msg;
        }

        // 构建消息
        Config config = plugin.getConfig();
        List<MessageFormat> formats = config.formatConfig.format;
        
        // 创建 VelocityChatPlayer，来源为 Web 时强制服务器名为 "Web"
        org.lintx.plugins.yinwuchat.chat.struct.VelocityChatPlayer fromPlayer;
        Optional<Player> onlinePlayer = plugin.getProxy().getPlayer(playerUuid);
        if (onlinePlayer.isPresent()) {
            fromPlayer = new org.lintx.plugins.yinwuchat.chat.struct.VelocityChatPlayer(onlinePlayer.get(), "Web");
        } else {
            fromPlayer = new org.lintx.plugins.yinwuchat.chat.struct.VelocityChatPlayer(playerUuid, playerName, "Web");
        }

        org.lintx.plugins.yinwuchat.velocity.chat.VelocityChatStruct struct = new org.lintx.plugins.yinwuchat.velocity.chat.VelocityChatStruct();
        struct.chat = message;
        List<org.lintx.plugins.yinwuchat.velocity.chat.VelocityChatStruct> chatStructList = new ArrayList<>();
        chatStructList.add(struct);

        org.lintx.plugins.yinwuchat.velocity.chat.VelocityChat chat = new org.lintx.plugins.yinwuchat.velocity.chat.VelocityChat(fromPlayer, chatStructList, org.lintx.plugins.yinwuchat.chat.struct.ChatSource.WEB);
        
        // 应用处理器（包括 AT 处理）
        for (org.lintx.plugins.yinwuchat.velocity.chat.VelocityChatHandle handle : handles) {
            handle.handle(chat);
        }

        Component chatComponent = chat.buildPublicMessage(formats);
        
        // 广播消息
        broadcast(playerUuid, playerName, "Web", chatComponent, true);
        
        // 处理 @ 提及通知
        handleAtMentionNotifications(playerUuid, playerName, message);
    }
    
    /**
     * 处理来自 Web 的私聊消息
     */
    public void handleWebPrivateMessage(io.netty.channel.Channel channel, 
            org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientUtil util, 
            String toPlayerName, String message) {
        if (message == null || message.isEmpty()) return;
        if (util.getUuid() == null) return;
        
        // 获取发送者名称
        String fromPlayerName = plugin.getProxy().getPlayer(util.getUuid())
            .map(Player::getUsername)
            .orElse("");
        if (fromPlayerName.isEmpty()) {
            fromPlayerName = org.lintx.plugins.yinwuchat.velocity.config.PlayerConfig
                .getInstance()
                .getTokenManager()
                .getName(util.getUuid());
            if (fromPlayerName == null || fromPlayerName.isEmpty()) {
                fromPlayerName = "Web用户";
            }
        }

        // 检查禁言状态（Web 端）
        if (isWebMuted(fromPlayerName, channel)) {
            return;
        }
        
        // 查找目标玩家
        Optional<Player> targetOpt = plugin.getProxy().getPlayer(toPlayerName);
        Player target = targetOpt.orElse(null);
        UUID targetUuid = null;
        String finalTargetName = toPlayerName;

        if (target != null) {
            targetUuid = target.getUniqueId();
            finalTargetName = target.getUsername();
        } else {
            // 查找 Web 端在线的玩家 UUID
            for (io.netty.channel.Channel wsChannel : org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.getChannels()) {
                org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientUtil wsUtil = 
                    org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.get(wsChannel);
                if (wsUtil == null || wsUtil.getUuid() == null) continue;
                String name = PlayerConfig.getInstance().getTokenManager().getName(wsUtil.getUuid());
                if (toPlayerName.equalsIgnoreCase(name)) {
                    targetUuid = wsUtil.getUuid();
                    finalTargetName = name;
                    break;
                }
            }
        }

        long privateMessageId = 0L;
        WebChatReplayStore replayStore = getReplayStore();
        if (replayStore != null) {
            privateMessageId = replayStore.appendPrivateMessage(fromPlayerName, finalTargetName, message);
        }

        if (targetUuid == null) {
            // 存入离线消息
            org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore store = getOfflineStore();
            if (store != null) {
                org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore.OfflineMessage offline =
                    new org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore.OfflineMessage();
                offline.from = fromPlayerName;
                offline.to = toPlayerName;
                offline.message = message;
                offline.time = System.currentTimeMillis();
                store.addMessage(toPlayerName, offline);
            }
            org.lintx.plugins.yinwuchat.velocity.httpserver.NettyChannelMessageHelper.send(channel,
                org.lintx.plugins.yinwuchat.velocity.json.OutputServerMessage.infoJSON("玩家不在线，消息已留存").getJSON());
            
            // 发送给发送者 (WebSocket) - 同步到发送者的所有 Web 端
            syncPrivateMessageToWeb(util.getUuid(), fromPlayerName, toPlayerName, message, true, privateMessageId, false);
            return;
        }
        
        // 检查忽略列表
        PlayerConfig.PlayerSettings targetSettings = PlayerConfig.getInstance().getSettings(finalTargetName);
        if (targetSettings.isIgnored(fromPlayerName)) {
            org.lintx.plugins.yinwuchat.velocity.httpserver.NettyChannelMessageHelper.send(channel,
                org.lintx.plugins.yinwuchat.velocity.json.OutputServerMessage.errorJSON(plugin.getConfig().tipsConfig.ignoreTip).getJSON());
            return;
        }
        
        // 屏蔽词检查（带账户名，用于 Web 端封禁）
        String senderAccount = util.getAccount();
        org.lintx.plugins.yinwuchat.velocity.manage.ShieldedManage.Result shieldedResult = 
            org.lintx.plugins.yinwuchat.velocity.manage.ShieldedManage.getInstance().checkShielded(channel, util.getUuid().toString(), senderAccount, message);
        if (shieldedResult.kick) {
            return;
        }
        if (shieldedResult.shielded) {
            if (shieldedResult.end) return;
            message = shieldedResult.msg;
        }

        // 构建私聊消息
        Config config = plugin.getConfig();
        Component toMessage = buildPrivateMessage(config.formatConfig.toFormat, fromPlayerName, finalTargetName, message);
        Component fromMessage = buildPrivateMessage(config.formatConfig.fromFormat, fromPlayerName, finalTargetName, message);
        
        // 发送给接收者 (游戏内)
        if (target != null) {
            target.sendMessage(fromMessage);
        }
        
        // 如果发送者在线（游戏内），同步到游戏端
        plugin.getProxy().getPlayer(util.getUuid()).ifPresent(sender -> sender.sendMessage(toMessage));
        
        // 发送给发送者 (WebSocket) - 同步到发送者的所有 Web 端
        syncPrivateMessageToWeb(util.getUuid(), fromPlayerName, finalTargetName, message, true, privateMessageId, false);
        
        // 发送给接收者 (WebSocket) - 同步到接收者的所有 Web 端
        syncPrivateMessageToWeb(targetUuid, fromPlayerName, finalTargetName, message, false, privateMessageId, false);

        // 监听私聊（管理员可见）
        sendPrivateMonitor(fromPlayerName, finalTargetName, message);
    }

    public void deliverOfflineMessagesToPlayer(Player player) {
        if (player == null) return;
        org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore store = getOfflineStore();
        if (store == null) return;
        List<org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore.OfflineMessage> list =
            store.consumeMessages(player.getUsername());
        if (list.isEmpty()) return;
        Config config = Config.getInstance();
        for (org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore.OfflineMessage msg : list) {
            Component targetMessage = buildPrivateMessage(config.formatConfig.fromFormat, msg.from, msg.to, msg.message);
            player.sendMessage(targetMessage);
            sendPrivateMonitor(msg.from, msg.to, msg.message);
            notifyOfflineRead(msg.from, msg.to);
        }
    }

    public void deliverOfflineMessagesToWeb(io.netty.channel.Channel channel, String playerName) {
        if (channel == null || playerName == null || playerName.isEmpty()) return;
        org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore store = getOfflineStore();
        if (store == null) return;
        List<org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore.OfflineMessage> list =
            store.consumeMessages(playerName);
        if (list.isEmpty()) return;
        for (org.lintx.plugins.yinwuchat.common.message.OfflineMessageStore.OfflineMessage msg : list) {
            com.google.gson.JsonObject receiverJson = new com.google.gson.JsonObject();
            receiverJson.addProperty("action", "private_message");
            receiverJson.addProperty("player", msg.from);
            receiverJson.addProperty("to", msg.to);
            receiverJson.addProperty("message", msg.message);
            receiverJson.addProperty("is_self", false);
            org.lintx.plugins.yinwuchat.velocity.httpserver.NettyChannelMessageHelper.send(channel, receiverJson.toString());
            notifyOfflineRead(msg.from, msg.to);
        }
    }

    public void replayMessagesToWeb(io.netty.channel.Channel channel, String playerName) {
        if (channel == null || playerName == null || playerName.isEmpty()) return;
        WebChatReplayStore store = getReplayStore();
        if (store == null) return;

        WebChatReplayStore.ReplaySnapshot snapshot =
                store.createSnapshot(playerName, WEB_REPLAY_PUBLIC_LIMIT, WEB_REPLAY_PRIVATE_LIMIT);

        JsonObject cursorSnapshot = new JsonObject();
        cursorSnapshot.addProperty("action", "read_cursor_snapshot");
        cursorSnapshot.addProperty("public_last_read_id", snapshot.publicLastReadId);
        JsonObject privateCursor = new JsonObject();
        for (Map.Entry<String, Long> entry : snapshot.privateLastReadByPeer.entrySet()) {
            privateCursor.addProperty(entry.getKey(), entry.getValue());
        }
        cursorSnapshot.add("private_last_read", privateCursor);
        NettyChannelMessageHelper.send(channel, cursorSnapshot.toString());

        for (WebChatReplayStore.PublicMessageRecord record : snapshot.publicMessages) {
            // 跳过历史遗留的“系统广播”，防止它们出现在公屏聊天界面中
            if (record.player != null && record.player.equals("系统广播")) {
                continue;
            }
            
            JsonObject json = new JsonObject();
            json.addProperty("action", "send_message");
            json.addProperty("message", record.message);
            json.addProperty("player", record.player);
            json.addProperty("time", record.time);
            json.addProperty("message_id", record.id);
            json.addProperty("replay", true);
            if (record.server != null && !record.server.isEmpty()) {
                json.addProperty("server", record.server);
            }
            if (record.rawChat != null && !record.rawChat.isEmpty()) {
                json.addProperty("raw_chat", buildWebChatTemplate(record.rawChat, record.rawItems == null ? 0 : record.rawItems.size()));
            }
            if (record.rawItems != null && !record.rawItems.isEmpty()) {
                JsonArray webItems = buildWebItems(record.rawItems);
                if (!webItems.isEmpty()) {
                    json.add("items", webItems);
                }
            }
            if (record.player != null && record.player.equalsIgnoreCase(playerName)) {
                json.addProperty("is_self", true);
            }
            NettyChannelMessageHelper.send(channel, json.toString());
        }

        for (WebChatReplayStore.PrivateMessageRecord record : snapshot.privateMessages) {
            JsonObject json = new JsonObject();
            json.addProperty("action", "private_message");
            json.addProperty("player", record.from);
            json.addProperty("to", record.to);
            json.addProperty("message", record.message);
            json.addProperty("time", record.time);
            json.addProperty("message_id", record.id);
            json.addProperty("is_self", false);
            json.addProperty("replay", true);
            NettyChannelMessageHelper.send(channel, json.toString());
        }
    }

    public void updateWebReadCursor(String playerName, String chat, long messageId) {
        if (playerName == null || playerName.isEmpty()) return;
        if (chat == null || chat.isEmpty()) return;
        if (messageId <= 0) return;
        WebChatReplayStore store = getReplayStore();
        if (store == null) return;
        store.updateReadCursor(playerName, chat, messageId);
    }

    private void notifyOfflineRead(String fromPlayer, String toPlayer) {
        if (fromPlayer == null || fromPlayer.isEmpty()) return;
        String tip = "你发给 " + toPlayer + " 的离线留言已读";
        // 游戏内通知
        plugin.getProxy().getPlayer(fromPlayer).ifPresent(p -> p.sendMessage(
            LegacyComponentSerializer.legacyAmpersand().deserialize("&a" + tip)
        ));
        // Web 端通知
        for (io.netty.channel.Channel wsChannel : VelocityWsClientHelper.getChannels()) {
            org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientUtil util =
                VelocityWsClientHelper.get(wsChannel);
            if (util == null || util.getUuid() == null) continue;
            String name = PlayerConfig.getInstance().getTokenManager().getName(util.getUuid());
            if (fromPlayer.equalsIgnoreCase(name)) {
                NettyChannelMessageHelper.send(wsChannel,
                    org.lintx.plugins.yinwuchat.velocity.json.OutputServerMessage.infoJSON(tip).getJSON());
            }
        }
    }

    private boolean isWebMuted(String playerName, io.netty.channel.Channel channel) {
        if (playerName == null || playerName.isEmpty()) return false;
        PlayerConfig.PlayerSettings settings = PlayerConfig.getInstance().getSettings(playerName);
        if (!settings.isMuted()) return false;
        Config config = plugin.getConfig();
        String tipMessage = config.tipsConfig.youismuteTip;
        long remaining = settings.getRemainingMuteTime();
        if (remaining == -1) {
            tipMessage += " (永久禁言)";
        } else if (remaining > 0) {
            tipMessage += " (剩余: " + MuteManage.formatTime(remaining) + ")";
        }
        if (settings.muteReason != null && !settings.muteReason.isEmpty()) {
            tipMessage += "\n&7原因: &f" + settings.muteReason;
        }
        org.lintx.plugins.yinwuchat.velocity.httpserver.NettyChannelMessageHelper.send(
            channel,
            org.lintx.plugins.yinwuchat.velocity.json.OutputServerMessage.errorJSON(tipMessage).getJSON()
        );
        return true;
    }

    private void sendPrivateMonitor(String fromPlayer, String toPlayer, String message) {
        Config config = plugin.getConfig();
        Component monitor = Component.text("[监听] ")
            .append(Component.text(fromPlayer))
            .append(Component.text(" -> "))
            .append(Component.text(toPlayer))
            .append(Component.text(": "))
            .append(Component.text(message));
        for (Player p : plugin.getProxy().getAllPlayers()) {
            boolean canMonitor = p.hasPermission(Const.PERMISSION_MONITOR_PRIVATE_MESSAGE)
                    || config.isAdmin(p);
            if (!canMonitor) continue;
            if (p.getUsername().equalsIgnoreCase(fromPlayer) || p.getUsername().equalsIgnoreCase(toPlayer)) {
                continue;
            }
            p.sendMessage(monitor);
        }
    }

    /**
     * 广播公告消息到游戏端和Web端
     * 根据 AnnouncementTaskConfig 中的白名单/黑名单/Web 配置过滤发送目标
     * @param task 广播任务配置
     * @param taskIndex 该任务在配置列表中的索引，用于Web端按任务去重
     */
    public void broadcastAnnouncement(AnnouncementTaskConfig task, int taskIndex) {
        if (task.list == null || task.list.isEmpty()) return;

        Component component = buildAnnouncementComponent(task.list);

        for (Player p : plugin.getProxy().getAllPlayers()) {
            p.getCurrentServer().ifPresent(conn -> {
                if (task.shouldSendToServer(conn.getServerInfo().getName())) {
                    sendMessage(p, component);
                }
            });
        }

        if (task.web) {
            sendAnnouncementToWeb(task.list, taskIndex);
        }
    }

    /**
     * 从 MessageFormat 列表构建广播用的 Adventure Component
     * 广播中的点击事件统一使用 SUGGEST_COMMAND（填入聊天栏）
     */
    private Component buildAnnouncementComponent(List<MessageFormat> formats) {
        Component result = Component.empty();
        for (MessageFormat format : formats) {
            if (format.message == null || format.message.isEmpty()) continue;

            Component part = LegacyComponentSerializer.legacyAmpersand().deserialize(format.message);

            // 添加悬停提示
            if (format.hover != null && !format.hover.isEmpty()) {
                Component hoverComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(format.hover);
                part = part.hoverEvent(HoverEvent.showText(hoverComponent));
            }

            // 添加点击事件（统一使用 SUGGEST_COMMAND，将内容填入聊天栏）
            if (format.click != null && !format.click.isEmpty()) {
                part = part.clickEvent(ClickEvent.suggestCommand(format.click));
            }

            result = result.append(part);
        }
        return result;
    }

    /**
     * 将广播消息发送到所有 WebSocket 客户端
     * 使用 "broadcast" action，附带结构化的 segments 数据和 task_index
     */
    private void sendAnnouncementToWeb(List<MessageFormat> formats, int taskIndex) {
        if (!plugin.getConfig().openwsserver || YinwuChat.getWSServer() == null) return;

        try {
            // 构建纯文本版本（用于兼容）
            StringBuilder plainText = new StringBuilder();
            for (MessageFormat format : formats) {
                if (format.message != null) {
                    plainText.append(format.message);
                }
            }

            // 构建结构化 segments
            JsonArray segments = new JsonArray();
            for (MessageFormat format : formats) {
                if (format.message == null || format.message.isEmpty()) continue;
                JsonObject seg = new JsonObject();
                seg.addProperty("text", format.message);
                if (format.hover != null && !format.hover.isEmpty()) {
                    seg.addProperty("hover", format.hover);
                }
                if (format.click != null && !format.click.isEmpty()) {
                    seg.addProperty("click", format.click);
                }
                segments.add(seg);
            }

            // 取消存入 ReplayStore，广播不再进入聊天历史流
            long messageTime = System.currentTimeMillis();

            JsonObject json = new JsonObject();
            json.addProperty("action", "broadcast");
            json.addProperty("task_index", taskIndex);
            json.addProperty("message", plainText.toString());
            json.add("segments", segments);
            json.addProperty("time", messageTime);

            String jsonStr = new Gson().toJson(json);

            for (io.netty.channel.Channel wsChannel : org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.getChannels()) {
                org.lintx.plugins.yinwuchat.velocity.httpserver.NettyChannelMessageHelper.send(wsChannel, jsonStr);
            }
        } catch (Exception e) {
            plugin.getLogger().warn("[WebSocket] 广播消息发送失败: " + e.getMessage());
        }
    }
}
