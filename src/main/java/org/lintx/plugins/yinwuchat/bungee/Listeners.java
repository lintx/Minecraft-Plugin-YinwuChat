package org.lintx.plugins.yinwuchat.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.bungee.config.Config;
import org.lintx.plugins.yinwuchat.bungee.config.PlayerConfig;
import org.lintx.plugins.yinwuchat.bungee.json.OutputPlayerList;

public class Listeners implements Listener {
    private final YinwuChat plugin;
    private Config config = Config.getInstance();

    Listeners(YinwuChat plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event){
        plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
            @Override
            public void run() {
                if (event.getTag().equals(Const.PLUGIN_CHANNEL)){
                    if (event.getReceiver() instanceof ProxiedPlayer && event.getSender() instanceof Server){
                        ProxiedPlayer player = (ProxiedPlayer)event.getReceiver();
                        ByteArrayDataInput input = ByteStreams.newDataInput(event.getData());
                        MessageManage.getInstance().handleBukkitMessage(player,input);
                    }
                }
            }
        });
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event){
        if (event.getPlayer() != null) {
            PlayerConfig.getConfig(event.getPlayer());
        }
        OutputPlayerList.sendGamePlayerList();
        MessageManage.getInstance().sendPlayerListToServer();
        if (config.redisConfig.openRedis){
            RedisUtil.sendPlayerList();
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event){
        if (event.getPlayer() != null) {
            PlayerConfig.unloadConfig(event.getPlayer());
        }
        OutputPlayerList.sendGamePlayerList();
        MessageManage.getInstance().sendPlayerListToServer();
        if (config.redisConfig.openRedis){
            RedisUtil.sendPlayerList();
        }
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent event){
        MessageManage.getInstance().sendPlayerListToServer(event.getServer());
    }

    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event){
        MessageManage.getInstance().sendPlayerListToServer(event.getPlayer().getServer());
    }
}
