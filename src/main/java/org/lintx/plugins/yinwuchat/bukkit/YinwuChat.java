package org.lintx.plugins.yinwuchat.bukkit;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.bukkit.commands.PrivateMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class YinwuChat extends JavaPlugin {
    public List<String> bungeePlayerList = new ArrayList<>();

    @Override
    public void onEnable() {
        Config.getInstance().load(this);
        MessageManage.getInstance().setPlugin(this);
        Listeners listeners = new Listeners(this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, Const.PLUGIN_CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this,Const.PLUGIN_CHANNEL,listeners);
        getServer().getPluginManager().registerEvents(listeners,this);
        getCommand("msg").setExecutor(new PrivateMessage(this));
        getCommand("yinwuchat-bukkit").setExecutor(new org.lintx.plugins.yinwuchat.bukkit.commands.YinwuChat(this));

        requirePlayerList();
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }

    private void requirePlayerList(){
        Collection<? extends Player> players = getServer().getOnlinePlayers();
        if (players==null || players.isEmpty() || !players.iterator().hasNext()) return;
        Player player = players.iterator().next();
        if (player==null) return;
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(Const.PLUGIN_SUB_CHANNEL_PLAYER_LIST);
        player.sendPluginMessage(this,Const.PLUGIN_CHANNEL,output.toByteArray());
    }
}
