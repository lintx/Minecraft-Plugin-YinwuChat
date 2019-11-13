package org.lintx.plugins.yinwuchat.bukkit;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.lintx.plugins.yinwuchat.Const;

import java.util.List;

public class Listeners implements Listener, PluginMessageListener {
    private final YinwuChat plugin;
    Listeners(YinwuChat plugin){
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST,ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event){
        if (event.isAsynchronous() && Config.getInstance().eventDelayTime>0){
            try {
                Thread.sleep(Config.getInstance().eventDelayTime);
            } catch (InterruptedException ignored) {

            }
        }
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        String chat = event.getMessage();

        MessageManage.getInstance().onPublicMessage(player,chat);

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
        else if (Const.PLUGIN_SUB_CHANNEL_PLAYER_LIST.equals(subchannel)){
            try {
                Gson gson = new Gson();
                plugin.bungeePlayerList = gson.fromJson(input.readUTF(),new TypeToken<List<String>>(){}.getType());
            }catch (Exception ignored){

            }
        }
    }
}
