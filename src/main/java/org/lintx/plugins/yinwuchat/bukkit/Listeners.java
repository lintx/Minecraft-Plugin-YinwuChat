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
import org.lintx.plugins.yinwuchat.bukkit.commands.ViewItemCommand;
import com.google.common.io.ByteArrayDataInput;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.Util.Gson;
import org.lintx.plugins.yinwuchat.Util.ItemUtil;
import org.lintx.plugins.yinwuchat.Util.ModernItemUtil;

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
        if (event.getView().getTitle().startsWith(ViewItemCommand.DISPLAY_TITLE_PREFIX)) {
            event.setCancelled(true);
        }
    }
    
    /**
     * 拦截物品展示界面的拖拽事件，禁止拖拽物品
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().startsWith(ViewItemCommand.DISPLAY_TITLE_PREFIX)) {
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
            plugin.getLogger().info("Received item request JSON: " + jsonRequest);

            // 简单解析 JSON，提取请求类型
            String requestType = extractRequestType(jsonRequest);
            plugin.getLogger().info("Extracted request type: '" + requestType + "'");

            List<String> items = new ArrayList<>();

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
                default:
                    sendItemError(player, "未知的请求类型: " + requestType);
                    return;
            }

            // 发送响应回 Velocity
            sendItemResponse(player, requestType, items);

        } catch (Exception e) {
            sendItemError(player, "处理请求时出错");
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to handle item request", e);
        }
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
    private void sendItemResponse(Player player, String requestType, List<String> items) {
        try {
            String responseJson = createItemResponseJson(player.getName(), requestType, items);
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF(Const.PLUGIN_SUB_CHANNEL_ITEM_RESPONSE);
            output.writeUTF(responseJson);

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
    private void sendItemError(Player player, String errorMessage) {
        try {
            String errorJson = createItemErrorJson(player.getName(), errorMessage);
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF(Const.PLUGIN_SUB_CHANNEL_ITEM_RESPONSE);
            output.writeUTF(errorJson);

            player.sendPluginMessage(plugin, responseChannel, output.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to send item error", e);
        }
    }

    /**
     * 创建物品响应 JSON
     */
    private String createItemResponseJson(String playerName, String requestType, List<String> items) {
        StringBuilder json = new StringBuilder();
        json.append("{\"playerName\":\"").append(playerName).append("\",")
            .append("\"requestType\":\"").append(requestType).append("\",")
            .append("\"success\":true,")
            .append("\"items\":[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(items.get(i).replace("\"", "\\\"")).append("\"");
        }
        json.append("]}");
        return json.toString();
    }

    /**
     * 创建错误响应 JSON
     */
    private String createItemErrorJson(String playerName, String errorMessage) {
        return "{\"playerName\":\"" + playerName + "\"," +
               "\"requestType\":\"unknown\"," +
               "\"success\":false," +
               "\"errorMessage\":\"" + errorMessage.replace("\"", "\\\"") + "\"}";
    }
}
