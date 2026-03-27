package org.lintx.plugins.yinwuchat.bukkit;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.Util.FoliaUtil;
import org.lintx.plugins.yinwuchat.bukkit.commands.PrivateMessage;
import org.lintx.plugins.yinwuchat.bukkit.commands.ViewBackpackCommand;
import org.lintx.plugins.yinwuchat.bukkit.commands.ViewItemCommand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class YinwuChat extends JavaPlugin {
    public List<String> bungeePlayerList = new ArrayList<>();
    private Listeners listeners;

    @Override
    public void onEnable() {
        Config.getInstance().load(this);
        MessageManage.getInstance().setPlugin(this);
        Listeners listeners = new Listeners(this);
        this.listeners = listeners;
        // 注册插件消息通道 - 同时尝试 Velocity 和 Bukkit 两种兼容通道
        boolean velocityChannelRegistered = false;
        boolean bukkitChannelRegistered = false;

        // 优先注册 Velocity 兼容通道
        try {
            getServer().getMessenger().registerOutgoingPluginChannel(this, Const.PLUGIN_CHANNEL_VELOCITY);
            getServer().getMessenger().registerIncomingPluginChannel(this, Const.PLUGIN_CHANNEL_VELOCITY, listeners);
            getLogger().info("Successfully registered velocity-compatible plugin channel: " + Const.PLUGIN_CHANNEL_VELOCITY);
            velocityChannelRegistered = true;
        } catch (Exception e) {
            getLogger().warning("Failed to register velocity channel: " + e.getMessage());
        }

        // 注册 Bukkit 兼容通道
        try {
            getServer().getMessenger().registerOutgoingPluginChannel(this, Const.PLUGIN_CHANNEL_BUKKIT);
            getServer().getMessenger().registerIncomingPluginChannel(this, Const.PLUGIN_CHANNEL_BUKKIT, listeners);
            getLogger().info("Successfully registered bukkit-compatible plugin channel: " + Const.PLUGIN_CHANNEL_BUKKIT);
            bukkitChannelRegistered = true;

            // 如果 Bukkit 通道注册成功但 Velocity 通道注册失败，切换监听器的响应通道
            if (!velocityChannelRegistered) {
                updateChannelForListeners(Const.PLUGIN_CHANNEL_BUKKIT);
            }
        } catch (Exception e) {
            getLogger().warning("Failed to register bukkit channel: " + e.getMessage());
        }

        if (!velocityChannelRegistered && !bukkitChannelRegistered) {
            getLogger().severe("Failed to register any plugin channel!");
        }
        getServer().getPluginManager().registerEvents(listeners,this);
        getCommand("msg").setExecutor(new PrivateMessage(this));
        getCommand("yinwuchat-bukkit").setExecutor(new org.lintx.plugins.yinwuchat.bukkit.commands.YinwuChat(this));
        getCommand("viewitem").setExecutor(new ViewItemCommand(this));
        getCommand("viewbackpack").setExecutor(new ViewBackpackCommand(this));
        
        // 初始化物品展示缓存
        ItemDisplayCache.getInstance().setPlugin(this);

        // 输出服务器类型信息
        String serverType = FoliaUtil.getServerType();
        getLogger().info("Detected server type: " + serverType);
        if (FoliaUtil.isFolia()) {
            getLogger().info("Folia support enabled - using region-aware scheduling");
        }

        requirePlayerList();
    }

    /**
     * 更新监听器用于发送响应的通道
     */
    private void updateChannelForListeners(String channel) {
        try {
            java.lang.reflect.Field channelField = Listeners.class.getDeclaredField("responseChannel");
            channelField.setAccessible(true);
            channelField.set(listeners, channel);
        } catch (Exception e) {
            getLogger().warning("Could not update listener channel: " + e.getMessage());
        }
    }

    /**
     * 通过反射绕过 Paper 的验证机制注册插件消息通道
     */
    private void registerChannelBypassingValidation(String channel, Listeners listeners) throws Exception {
        try {
            getLogger().info("Attempting to register channel '" + channel + "' using reflection...");

            Object messenger = getServer().getMessenger();

            // 尝试不同的字段名查找发出通道集合（Paper 可能会重映射字段名）
            java.lang.reflect.Field outgoingField = null;
            String[] possibleOutgoingNames = {"outgoing", "c", "field_1234"};

            for (String fieldName : possibleOutgoingNames) {
                try {
                    outgoingField = messenger.getClass().getDeclaredField(fieldName);
                    outgoingField.setAccessible(true);
                    if (java.util.Set.class.isAssignableFrom(outgoingField.getType())) {
                        break;
                    }
                } catch (NoSuchFieldException e) {
                    // 尝试下一个字段名
                }
            }

            if (outgoingField == null) {
                throw new NoSuchFieldException("Could not find outgoing channels field");
            }

            @SuppressWarnings("unchecked")
            java.util.Set<String> outgoing = (java.util.Set<String>) outgoingField.get(messenger);

            outgoing.add(channel);

            // 尝试正常方式注册传入通道
            try {
                getServer().getMessenger().registerIncomingPluginChannel(this, channel, listeners);
            } catch (Exception e) {
                getLogger().warning("Failed to register incoming channel normally, but outgoing channel was registered");
            }

            getLogger().info("Successfully registered plugin channel '" + channel + "' using reflection");
        } catch (Exception e) {
            getLogger().severe("Failed to register channel using reflection: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }

    /**
     * 获取当前实际注册成功的插件消息通道
     */
    public String getActualChannel() {
        return listeners != null ? listeners.responseChannel : Const.PLUGIN_CHANNEL_VELOCITY;
    }

    private void requirePlayerList(){
        Collection<? extends Player> players = getServer().getOnlinePlayers();
        if (players==null || players.isEmpty() || !players.iterator().hasNext()) return;
        Player player = players.iterator().next();
        if (player==null) return;
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(Const.PLUGIN_SUB_CHANNEL_PLAYER_LIST);
        player.sendPluginMessage(this, getActualChannel(), output.toByteArray());
    }
}
