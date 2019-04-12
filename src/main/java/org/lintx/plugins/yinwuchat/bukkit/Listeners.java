package org.lintx.plugins.yinwuchat.bukkit;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.lintx.plugins.yinwuchat.Const;

public class Listeners implements Listener, PluginMessageListener {
    public Listeners(){
    }

    @EventHandler(priority = EventPriority.LOWEST,ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event){
        Player player = event.getPlayer();
        String chat = event.getMessage();

        MessageManage.getInstance().sendPublicMessage(player,chat);

        event.setCancelled(true);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] bytes) {
        if (!Const.PLUGIN_CHANNEL.equals(channel)){
            return;
        }
        ByteArrayDataInput input = ByteStreams.newDataInput(bytes);
        String subchannel = input.readUTF();
        if (Const.PLUGIN_SUB_CHANNEL_AT.equals(subchannel)){
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS,1.0f,1.0f);
        }
    }
}
