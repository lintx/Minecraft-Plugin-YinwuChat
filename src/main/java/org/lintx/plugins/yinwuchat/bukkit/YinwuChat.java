package org.lintx.plugins.yinwuchat.bukkit;

import org.bukkit.plugin.java.JavaPlugin;
import org.lintx.plugins.yinwuchat.Const;

public class YinwuChat extends JavaPlugin {

    @Override
    public void onEnable() {
        Config.getInstance().load(this);
        MessageManage.getInstance().setPlugin(this);
        Listeners listeners = new Listeners();
        getServer().getMessenger().registerOutgoingPluginChannel(this, Const.PLUGIN_CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this,Const.PLUGIN_CHANNEL,listeners);
        getServer().getPluginManager().registerEvents(listeners,this);
        getCommand("msg").setExecutor(new Commands(this));
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }
}
