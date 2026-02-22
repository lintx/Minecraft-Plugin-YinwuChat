package org.lintx.plugins.yinwuchat.bukkit;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.Util.FoliaUtil;
import org.lintx.plugins.yinwuchat.bukkit.commands.PrivateMessage;
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
        // Register plugin channels - try both velocity and bukkit compatible channels
        boolean velocityChannelRegistered = false;
        boolean bukkitChannelRegistered = false;

        // Try registering velocity-compatible channel first
        try {
            getServer().getMessenger().registerOutgoingPluginChannel(this, Const.PLUGIN_CHANNEL_VELOCITY);
            getServer().getMessenger().registerIncomingPluginChannel(this, Const.PLUGIN_CHANNEL_VELOCITY, listeners);
            getLogger().info("Successfully registered velocity-compatible plugin channel: " + Const.PLUGIN_CHANNEL_VELOCITY);
            velocityChannelRegistered = true;
        } catch (Exception e) {
            getLogger().warning("Failed to register velocity channel: " + e.getMessage());
        }

        // Try registering bukkit-compatible channel
        try {
            getServer().getMessenger().registerOutgoingPluginChannel(this, Const.PLUGIN_CHANNEL_BUKKIT);
            getServer().getMessenger().registerIncomingPluginChannel(this, Const.PLUGIN_CHANNEL_BUKKIT, listeners);
            getLogger().info("Successfully registered bukkit-compatible plugin channel: " + Const.PLUGIN_CHANNEL_BUKKIT);
            bukkitChannelRegistered = true;

            // If bukkit channel is registered but velocity channel is not, update listeners
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
     * Update the channel used by listeners for sending responses
     */
    private void updateChannelForListeners(String channel) {
        // This is a simple way to communicate the channel to listeners
        // In a more sophisticated implementation, you might use dependency injection
        try {
            java.lang.reflect.Field channelField = Listeners.class.getDeclaredField("responseChannel");
            channelField.setAccessible(true);
            channelField.set(listeners, channel);
        } catch (Exception e) {
            getLogger().warning("Could not update listener channel: " + e.getMessage());
        }
    }

    /**
     * Register plugin channel bypassing Paper's validation using reflection
     */
    private void registerChannelBypassingValidation(String channel, Listeners listeners) throws Exception {
        try {
            getLogger().info("Attempting to register channel '" + channel + "' using reflection...");

            // Get the StandardMessenger
            Object messenger = getServer().getMessenger();

            // Try different field names for outgoing channels (Paper may remap them)
            java.lang.reflect.Field outgoingField = null;
            String[] possibleOutgoingNames = {"outgoing", "c", "field_1234"}; // Try common remapped names

            for (String fieldName : possibleOutgoingNames) {
                try {
                    outgoingField = messenger.getClass().getDeclaredField(fieldName);
                    outgoingField.setAccessible(true);
                    if (java.util.Set.class.isAssignableFrom(outgoingField.getType())) {
                        break;
                    }
                } catch (NoSuchFieldException e) {
                    // Try next field name
                }
            }

            if (outgoingField == null) {
                throw new NoSuchFieldException("Could not find outgoing channels field");
            }

            @SuppressWarnings("unchecked")
            java.util.Set<String> outgoing = (java.util.Set<String>) outgoingField.get(messenger);

            // Add the channel to outgoing set
            outgoing.add(channel);

            // Try to register incoming channel normally
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
     * Get the actual channel that was registered with the server
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
