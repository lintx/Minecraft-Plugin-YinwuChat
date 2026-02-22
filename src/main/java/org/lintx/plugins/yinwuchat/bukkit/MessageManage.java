package org.lintx.plugins.yinwuchat.bukkit;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.Util.MessageUtil;
import org.lintx.plugins.yinwuchat.json.*;
import org.lintx.plugins.yinwuchat.Util.ModernItemUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManage {
    private static MessageManage instance = new MessageManage();
    private YinwuChat plugin;
    private MessageManage(){}
    void setPlugin(YinwuChat plugin){
        this.plugin = plugin;
    }
    public static MessageManage getInstance(){
        return instance;
    }

    private String filterStyle(Player player,String chat){
        String permissions = "0123456789abcdefklmnor";
        StringBuilder deny = new StringBuilder();
        for (int i=0;i<permissions.length();i++){
            String p = permissions.substring(i, i+1);
            String permission = "yinwuchat.style." + p;
            if (!player.hasPermission(permission)){
                deny.append(p);
            }
        }
        if (!"".equals(deny.toString())){
            chat = MessageUtil.filter(chat, deny.toString());
        }
        if (!player.hasPermission("yinwuchat.style.rgb")){
            chat = MessageUtil.filterRGB(chat);
        }
        return chat;
    }

    public void onPrivateMessage(Player player, String toPlayerName, String chat){
        chat = filterStyle(player,chat);
        PrivateMessage privateMessage = new PrivateMessage();
        privateMessage.toPlayer = toPlayerName;
        privateMessage.player = player.getName();
        privateMessage.chat = chat;

        privateMessage.toFormat = format(player,Config.getInstance().toFormat);
        privateMessage.fromFormat = format(player,Config.getInstance().fromFormat);

        for (HandleConfig config:Config.getInstance().messageHandles){
            HandleConfig handleConfig = new HandleConfig();
            handleConfig.placeholder = config.placeholder;
            handleConfig.format = format(player,config.format);
            privateMessage.handles.add(handleConfig);
        }

        privateMessage.items = getMessageItems(chat,player);
        sendPluginMessage(player,Const.PLUGIN_SUB_CHANNEL_PRIVATE_MESSAGE,privateMessage);
    }

    void onPublicMessage(Player player, String chat){
        chat = filterStyle(player,chat);
        PublicMessage publicMessage = new PublicMessage();
        publicMessage.player = player.getName();
        publicMessage.chat = chat;
        publicMessage.serverName = getServerName(); // 设置服务器名称

        publicMessage.format = format(player,Config.getInstance().format);

        for (HandleConfig config:Config.getInstance().messageHandles){
            HandleConfig handleConfig = new HandleConfig();
            handleConfig.placeholder = config.placeholder;
            handleConfig.format = format(player,config.format);
            publicMessage.handles.add(handleConfig);
        }

        publicMessage.items = getMessageItems(chat,player);

        plugin.getLogger().info("Sending public message to Velocity: player=" + player.getName() + ", chat=" + chat + ", items=" + (publicMessage.items != null ? publicMessage.items.size() : 0));
        sendPluginMessage(player,Const.PLUGIN_SUB_CHANNEL_PUBLIC_MESSAGE,publicMessage);
    }

    /**
     * 获取格式化的消息文本（用于直接设置事件消息）
     */
    String getFormattedMessage(Player player, String chat) {
        chat = filterStyle(player, chat);

        // 构建消息格式列表
        List<MessageFormat> formatList = format(player, Config.getInstance().format);

        // 处理物品占位符
        List<String> items = getMessageItems(chat, player);

        // 构建基础格式
        StringBuilder formatted = new StringBuilder();
        for (MessageFormat fmt : formatList) {
            String part = fmt.message;
            if (part != null) {
                part = part.replace("{displayName}", player.getName());

                // 替换服务器名称 - 在Bukkit端，我们使用配置文件中定义的服务器名称
                String serverName = getServerName();
                part = part.replace("[ServerName]", serverName);
                part = part.replace("{ServerName}", serverName);

                if (chat.contains("[i]") && items != null && !items.isEmpty()) {
                    part = part.replace("{message}", "[物品]");
                } else {
                    part = part.replace("{message}", chat);
                }
                formatted.append(part);
            }
        }

        // 转换颜色代码从 & 到 §
        return MessageUtil.replace(formatted.toString());
    }

    // 获取服务器名称
    public String getServerName() {
        // 尝试从各种来源获取服务器名称，优先级从高到低
        try {
            // 1. 首先尝试从配置文件获取
            String serverName = Config.getInstance().serverName;
            if (serverName != null && !serverName.trim().isEmpty()) {
                return serverName.trim();
            }

            // 2. 然后尝试从系统属性获取
            serverName = System.getProperty("yinwuchat.server.name");
            if (serverName != null && !serverName.trim().isEmpty()) {
                return serverName.trim();
            }

            // 3. 然后尝试从环境变量获取
            serverName = System.getenv("YINWUCHAT_SERVER_NAME");
            if (serverName != null && !serverName.trim().isEmpty()) {
                return serverName.trim();
            }

            // 4. 最后尝试从Bukkit服务器名称获取
            serverName = org.bukkit.Bukkit.getServer().getName();
            if (serverName != null && !serverName.trim().isEmpty() && !serverName.equals("Unknown Server")) {
                return serverName.trim();
            }

            // 默认值
            return "lobby";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private void sendPluginMessage(Player player,String channel, Message message){
        String json = new Gson().toJson(message);
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(channel);
        output.writeUTF(json);

        // Use the correct channel that was actually registered with Paper
        YinwuChat yinwuChat = (YinwuChat) plugin;
        String actualChannel = yinwuChat.getActualChannel();
        player.sendPluginMessage(plugin, actualChannel, output.toByteArray());
    }

    private List<MessageFormat> format(Player player, List<MessageFormat> formats){
        boolean hasPAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null &&
                Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        List<MessageFormat> list = new ArrayList<>();
        for (MessageFormat message:formats){
            if (message.message==null || message.message.equals("")) continue;

            String msg = message.message;
            msg = papi(hasPAPI,msg,player);

            MessageFormat format = new MessageFormat(msg);
            if (message.hover!=null){
                format.hover = papi(hasPAPI,message.hover,player);
            }
            if (message.click!=null){
                format.click = papi(hasPAPI,message.click,player);
            }
            list.add(format);
        }
        return list;
    }

    private String papi(boolean open,String string,Player player){
        // 首先处理内置的玩家位置占位符（不依赖 PlaceholderAPI）
        string = replaceBuiltinPlaceholders(string, player);
        
        if (!open){
            return string;
        }
        try {
            return PlaceholderAPI.setPlaceholders(player,string);
        }catch (Exception ignored){
            return string;
        }
    }
    
    /**
     * 替换内置的玩家信息占位符（不依赖 PlaceholderAPI）
     */
    private String replaceBuiltinPlaceholders(String string, Player player) {
        if (string == null || player == null) {
            return string;
        }
        
        try {
            org.bukkit.Location loc = player.getLocation();
            
            // 替换坐标占位符
            string = string.replace("%player_x%", String.valueOf(loc.getBlockX()));
            string = string.replace("%player_y%", String.valueOf(loc.getBlockY()));
            string = string.replace("%player_z%", String.valueOf(loc.getBlockZ()));
            
            // 替换世界名称
            String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";
            string = string.replace("%player_world%", worldName);
            
            // 替换玩家名称
            string = string.replace("%player_name%", player.getName());
            string = string.replace("%player_displayname%", player.getDisplayName());
            
            // 替换服务器名称
            String serverName = getServerName();
            string = string.replace("ServerName", serverName);
            string = string.replace("{ServerName}", serverName);
            string = string.replace("[ServerName]", serverName);
            
        } catch (Exception e) {
            // 忽略错误，返回原始字符串
        }
        
        return string;
    }

    private List<String> getMessageItems(String message,Player player){
        Pattern pattern = Pattern.compile(Const.ITEM_PLACEHOLDER);
        Matcher matcher = pattern.matcher(message);
        List<String> list = new ArrayList<>();
        PlayerInventory inventory = player.getInventory();
        while (matcher.find()){
            int index = -1;
            String s = matcher.group(2);
            try {
                index = Integer.parseInt(s);
                if (index>40 || index<0){
                    index = -1;
                }
            }catch (Exception ignored){ }
            ItemStack itemStack;
            if (index==-1){
                itemStack = inventory.getItemInMainHand()==null?player.getInventory().getItemInOffHand():player.getInventory().getItemInMainHand();
            }else {
                itemStack = inventory.getItem(index);
            }

            // 获取物品数据用于传输
            String itemData = ModernItemUtil.getItemDataForTransfer(itemStack);
            
            // 将物品存储到本地缓存和 Velocity 缓存，获取唯一ID用于点击查看
            if (itemStack != null && !itemStack.getType().isAir()) {
                String itemId = ItemDisplayCache.getInstance().storeItem(itemStack, player.getName());
                if (itemId != null && itemData != null) {
                    // 将物品ID添加到JSON数据中
                    try {
                        com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(itemData).getAsJsonObject();
                        json.addProperty("displayId", itemId);
                        itemData = json.toString();
                        
                        // 发送物品数据到 Velocity 缓存（用于跨服展示）
                        sendItemToVelocity(player, itemId, itemData);
                    } catch (Exception e) {
                        // 如果解析失败，保持原样
                    }
                }
            }
            
            list.add(itemData);
        }
        return list;
    }
    
    /**
     * 发送物品数据到 Velocity 缓存
     * 用于跨服务器物品展示
     */
    private void sendItemToVelocity(Player player, String itemId, String itemJson) {
        try {
            com.google.common.io.ByteArrayDataOutput output = com.google.common.io.ByteStreams.newDataOutput();
            output.writeUTF(Const.PLUGIN_SUB_CHANNEL_ITEM_DISPLAY_STORE);
            output.writeUTF(itemId);
            output.writeUTF(itemJson);
            output.writeUTF(player.getName());
            
            YinwuChat yinwuChat = (YinwuChat) plugin;
            player.sendPluginMessage(plugin, yinwuChat.getActualChannel(), output.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send item to Velocity: " + e.getMessage());
        }
    }
}
