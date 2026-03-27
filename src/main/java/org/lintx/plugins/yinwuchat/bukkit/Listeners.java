package org.lintx.plugins.yinwuchat.bukkit;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.lintx.plugins.yinwuchat.bukkit.commands.ViewBackpackCommand;
import org.lintx.plugins.yinwuchat.bukkit.commands.ViewItemCommand;
import com.google.common.io.ByteArrayDataInput;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.Util.BackpackViewDebugLogUtil;
import org.lintx.plugins.yinwuchat.Util.Gson;
import org.lintx.plugins.yinwuchat.Util.ItemResponseJsonUtil;
import org.lintx.plugins.yinwuchat.Util.ItemUtil;
import org.lintx.plugins.yinwuchat.Util.ModernItemUtil;
import org.lintx.plugins.yinwuchat.bukkit.display.BackpackDisplayLayout;
import org.lintx.plugins.yinwuchat.bukkit.display.BackpackDisplayPayload;
import org.lintx.plugins.yinwuchat.velocity.json.ItemRequest;
import org.lintx.plugins.yinwuchat.velocity.json.ItemResponse;

import java.util.logging.Level;

import java.util.ArrayList;
import java.util.List;

public class Listeners implements Listener, PluginMessageListener {
    private final YinwuChat plugin;
    public String responseChannel = Const.PLUGIN_CHANNEL_VELOCITY; // Default to velocity channel

    Listeners(YinwuChat plugin){
        this.plugin = plugin;
    }

    /**
     * 拦截物品展示界面的点击事件，禁止取出物品
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith(ViewItemCommand.DISPLAY_TITLE_PREFIX)
                || event.getView().getTitle().startsWith(ViewBackpackCommand.DISPLAY_TITLE_PREFIX)) {
            event.setCancelled(true);
        }
    }
    
    /**
     * 拦截物品展示界面的拖拽事件，禁止拖拽物品
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().startsWith(ViewItemCommand.DISPLAY_TITLE_PREFIX)
                || event.getView().getTitle().startsWith(ViewBackpackCommand.DISPLAY_TITLE_PREFIX)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST,ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event){
        // 延迟处理 - 在异步事件中使用 Thread.sleep 是安全的
        // AsyncPlayerChatEvent 总是在异步线程上运行，不受 Folia 区域线程限制
        long delayTime = Config.getInstance().eventDelayTime;
        if (event.isAsynchronous() && delayTime > 0){
            try {
                Thread.sleep(delayTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        String chat = event.getMessage();

        // 所有消息都发送到Velocity处理，实现跨服聊天
        try {
            MessageManage.getInstance().onPublicMessage(player, chat);
            if (chat.contains("[i]")) {
                plugin.getLogger().info("Sent item message to Velocity for processing: " + chat);
            } else {
                plugin.getLogger().info("Sent regular message to Velocity for processing: " + chat);
            }

            // 取消事件，让Velocity处理所有消息显示
            event.setCancelled(true);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not send message to Velocity: " + e.getMessage());
            // 如果发送失败，回退到本地处理
            String formattedMessage = MessageManage.getInstance().getFormattedMessage(player, chat);
            String serverName = MessageManage.getInstance().getServerName();
            String customFormat = String.format("[%s]%%1$s >>> %%2$s", serverName);
            event.setFormat(customFormat);
            event.setMessage(chat);
            plugin.getLogger().info("Fallback to local processing: " + chat);
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] bytes) {
        plugin.getLogger().info("Received plugin message on channel: " + channel + " from player: " + player.getName());
        if (!Const.PLUGIN_CHANNEL_VELOCITY.equals(channel) && !Const.PLUGIN_CHANNEL_BUKKIT.equals(channel)){
            plugin.getLogger().info("Ignoring message on channel: " + channel);
            return;
        }
        ByteArrayDataInput input = ByteStreams.newDataInput(bytes);
        String subchannel = input.readUTF();
        plugin.getLogger().info("Subchannel: " + subchannel);
        if (Const.PLUGIN_SUB_CHANNEL_AT.equals(subchannel)){
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS,1.0f,1.0f);
        }
        else if (Const.PLUGIN_SUB_CHANNEL_PLAYER_LIST.equals(subchannel)){
            try {
                plugin.bungeePlayerList = Gson.gson().fromJson(input.readUTF(),new TypeToken<List<String>>(){}.getType());
            }catch (Exception ignored){

            }
        }
        else if (Const.PLUGIN_SUB_CHANNEL_ITEM_REQUEST.equals(subchannel)){
            handleItemRequest(player, input);
        }
        else if (Const.PLUGIN_SUB_CHANNEL_ITEM_DISPLAY_RESPONSE.equals(subchannel)){
            handleItemDisplayResponse(player, input);
        }
    }
    
    /**
     * 处理 Velocity 返回的物品展示数据
     */
    private void handleItemDisplayResponse(Player player, ByteArrayDataInput input) {
        try {
            String itemId = input.readUTF();
            boolean success = input.readBoolean();
            String itemJson = input.readUTF();
            String playerName = input.readUTF();
            String serverName = input.readUTF();
            
            plugin.getLogger().info("Received item display response: id=" + itemId + ", success=" + success);
            
            // 调用 ViewItemCommand 处理响应
            ViewItemCommand.handleItemDisplayResponse(plugin, player, itemId, success, itemJson, playerName, serverName);
            ViewBackpackCommand.handleItemDisplayResponse(plugin, player, itemId, success, itemJson, playerName, serverName);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to handle item display response: " + e.getMessage());
        }
    }

    /**
     * 处理物品请求
     */
    private void handleItemRequest(Player player, ByteArrayDataInput input) {
        try {
            String jsonRequest = input.readUTF();
            plugin.getLogger().fine("Received item request JSON: " + jsonRequest);
            ItemRequest request = parseItemRequest(jsonRequest);
            String requestType = request.requestType == null || request.requestType.isEmpty() ? "hand" : request.requestType;
            plugin.getLogger().fine("Extracted request type: '" + requestType + "'");
            if ("backpackview".equalsIgnoreCase(requestType)) {
                plugin.getLogger().log(Level.FINE, "[backpackview] bukkit parsed request: {0}",
                        BackpackViewDebugLogUtil.summarizeRequest(request));
            }

            List<String> items = new ArrayList<>();
            String viewerName = request.playerName == null || request.playerName.trim().isEmpty() ? player.getName() : request.playerName.trim();
            Player targetPlayer = resolveRequestedPlayer(player, request.targetPlayer);

            switch (requestType) {
                case "hand":
                case "chat_items":
                    // 获取手中物品（用于聊天 [i] 占位符）
                    String handItemJson = getPlayerHandItem(player);
                    plugin.getLogger().info("Player " + player.getName() + " requested hand item, got: " + (handItemJson != null ? "item data" : "null"));
                    // 即使没有物品也添加一个占位符，这样前端知道请求被处理了
                    items.add(handItemJson != null ? handItemJson : "");
                    break;
                case "inventory":
                    // 获取背包物品
                    items = getPlayerInventoryItems(player);
                    break;
                case "enderchest":
                    // 获取末影箱物品
                    items = getPlayerEnderChestItems(player);
                    break;
                case "backpackview":
                    if (targetPlayer == null || !targetPlayer.isOnline()) {
                        plugin.getLogger().log(Level.FINE, "[backpackview] bukkit target unavailable: viewer={0}, target={1}",
                                new Object[]{viewerName, request.targetPlayer});
                        sendItemError(player, viewerName, requestType, "目标玩家不在线");
                        return;
                    }
                    String payload = createBackpackDisplayData(targetPlayer);
                    plugin.getLogger().log(Level.FINE, "[backpackview] bukkit created payload: {0}",
                            BackpackViewDebugLogUtil.summarizePayload(payload));
                    items.add(payload);
                    sendItemResponse(targetPlayer, viewerName, targetPlayer.getName(), requestType, items);
                    return;
                default:
                    sendItemError(player, viewerName, requestType, "未知的请求类型: " + requestType);
                    return;
            }

            // 发送响应回 Velocity
            sendItemResponse(player, viewerName, player.getName(), requestType, items);

        } catch (Exception e) {
            sendItemError(player, player.getName(), "unknown", "处理请求时出错");
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to handle item request", e);
        }
    }

    private ItemRequest parseItemRequest(String json) {
        try {
            return Gson.gson().fromJson(json, ItemRequest.class);
        } catch (Exception ignored) {
            ItemRequest fallback = new ItemRequest();
            fallback.requestType = extractRequestType(json);
            return fallback;
        }
    }

    private Player resolveRequestedPlayer(Player fallback, String requestedName) {
        if (requestedName == null || requestedName.trim().isEmpty()) {
            return fallback;
        }
        Player exact = org.bukkit.Bukkit.getPlayerExact(requestedName.trim());
        if (exact != null) {
            return exact;
        }
        return org.bukkit.Bukkit.getPlayer(requestedName.trim());
    }

    /**
     * 从 JSON 中提取请求类型
     */
    private String extractRequestType(String json) {
        try {
            // 使用 Gson 进行更可靠的 JSON 解析
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            if (jsonObject.has("requestType")) {
                return jsonObject.get("requestType").getAsString();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse JSON with Gson, falling back to regex: " + json);
            // 回退到正则表达式方法作为备选方案
            try {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"requestType\"\\s*:\\s*\"([^\"]+)\"");
                java.util.regex.Matcher matcher = pattern.matcher(json);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (Exception e2) {
                plugin.getLogger().warning("Regex parsing also failed: " + e2.getMessage());
            }
        }
        return "hand";
    }

    /**
     * 获取玩家手中物品
     */
    private String getPlayerHandItem(Player player) {
        try {
            var item = player.getInventory().getItemInMainHand();
            if (item != null && !item.getType().isAir()) {
                return ModernItemUtil.getItemDataForTransfer(item);
            }
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to get hand item", e);
        }
        return null;
    }

    /**
     * 获取玩家背包物品
     */
    private List<String> getPlayerInventoryItems(Player player) {
        List<String> items = new ArrayList<>();
        try {
            var inventory = player.getInventory();
            for (var item : inventory.getContents()) {
                if (item != null && !item.getType().isAir()) {
                    String itemJson = ModernItemUtil.getItemDataForTransfer(item);
                    if (itemJson != null) {
                        items.add(itemJson);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to get inventory items", e);
        }
        return items;
    }

    /**
     * 获取玩家末影箱物品
     */
    private List<String> getPlayerEnderChestItems(Player player) {
        List<String> items = new ArrayList<>();
        try {
            var enderChest = player.getEnderChest();
            for (var item : enderChest.getContents()) {
                if (item != null && !item.getType().isAir()) {
                    String itemJson = ModernItemUtil.getItemDataForTransfer(item);
                    if (itemJson != null) {
                        items.add(itemJson);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to get ender chest items", e);
        }
        return items;
    }

    /**
     * 发送物品响应
     */
    private void sendItemResponse(Player player, String viewerName, String ownerName, String requestType, List<String> items) {
        try {
            String responseJson = createItemResponseJson(viewerName, ownerName, requestType, items);
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF(Const.PLUGIN_SUB_CHANNEL_ITEM_RESPONSE);
            output.writeUTF(responseJson);

            if ("backpackview".equalsIgnoreCase(requestType)) {
                ItemResponse debugResponse = new ItemResponse();
                debugResponse.playerName = viewerName;
                debugResponse.ownerName = ownerName;
                debugResponse.requestType = requestType;
                debugResponse.success = true;
                debugResponse.items = items;
                plugin.getLogger().log(Level.FINE, "[backpackview] bukkit sending response via {0}: {1}",
                        new Object[]{responseChannel, BackpackViewDebugLogUtil.summarizeResponse(debugResponse)});
            }
            plugin.getLogger().info("Sending item response to " + player.getName() + " via channel " + responseChannel);
            player.sendPluginMessage(plugin, responseChannel, output.toByteArray());
            plugin.getLogger().info("Item response sent successfully");
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to send item response", e);
        }
    }

    /**
     * 发送错误响应
     */
    private void sendItemError(Player player, String viewerName, String requestType, String errorMessage) {
        try {
            String errorJson = createItemErrorJson(viewerName, requestType, errorMessage);
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF(Const.PLUGIN_SUB_CHANNEL_ITEM_RESPONSE);
            output.writeUTF(errorJson);

            if ("backpackview".equalsIgnoreCase(requestType)) {
                plugin.getLogger().log(Level.FINE, "[backpackview] bukkit sending error via {0}: viewer={1}, error={2}",
                        new Object[]{responseChannel, viewerName, errorMessage});
            }
            player.sendPluginMessage(plugin, responseChannel, output.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to send item error", e);
        }
    }

    /**
     * 创建物品响应 JSON
     */
    private String createItemResponseJson(String playerName, String ownerName, String requestType, List<String> items) {
        return ItemResponseJsonUtil.success(playerName, ownerName, requestType, items);
    }

    /**
     * 创建错误响应 JSON
     */
    private String createItemErrorJson(String playerName, String requestType, String errorMessage) {
        return ItemResponseJsonUtil.error(playerName, requestType, errorMessage);
    }

    private String createBackpackDisplayData(Player player) {
        var inventory = player.getInventory();
        List<String> storage = new ArrayList<>();
        for (int slot = 9; slot <= 35; slot++) {
            storage.add(ModernItemUtil.getItemDataForTransfer(inventory.getItem(slot)));
        }
        List<String> hotbar = new ArrayList<>();
        for (int slot = 0; slot <= 8; slot++) {
            hotbar.add(ModernItemUtil.getItemDataForTransfer(inventory.getItem(slot)));
        }
        List<String> armor = new ArrayList<>();
        armor.add(ModernItemUtil.getItemDataForTransfer(inventory.getHelmet()));
        armor.add(ModernItemUtil.getItemDataForTransfer(inventory.getChestplate()));
        armor.add(ModernItemUtil.getItemDataForTransfer(inventory.getLeggings()));
        armor.add(ModernItemUtil.getItemDataForTransfer(inventory.getBoots()));
        String offhand = ModernItemUtil.getItemDataForTransfer(inventory.getItemInOffHand());
        String displayId = ItemDisplayCache.getInstance().generateDisplayId();
        List<String> chestSlots = BackpackDisplayLayout.buildChestSlots(storage, hotbar, armor, offhand);
        String payload = BackpackDisplayPayload.toJson(player.getName(), displayId, chestSlots);
        plugin.getLogger().log(Level.FINE, "[backpackview] bukkit built display payload for {0}: {1}",
                new Object[]{player.getName(), BackpackViewDebugLogUtil.summarizePayload(payload)});
        sendItemToVelocity(player, displayId, payload);
        return payload;
    }

    private void sendItemToVelocity(Player player, String itemId, String itemJson) {
        try {
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF(Const.PLUGIN_SUB_CHANNEL_ITEM_DISPLAY_STORE);
            output.writeUTF(itemId);
            output.writeUTF(itemJson);
            output.writeUTF(player.getName());
            plugin.getLogger().log(Level.FINE, "[backpackview] bukkit caching payload to proxy: {0}",
                    BackpackViewDebugLogUtil.summarizeDisplayRequest(itemId, player.getName()));
            player.sendPluginMessage(plugin, responseChannel, output.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to cache backpack display payload", e);
        }
    }
}
