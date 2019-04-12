package org.lintx.plugins.yinwuchat.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.bungee.util.WsClientHelper;

import java.io.IOException;
import java.net.ServerSocket;

public class YinwuChat extends Plugin {
    private static YinwuChat plugin;
    private static WSServer server;
    private static BatManage batManage;
    private Config config = Config.getInstance();
    public static WSServer getWSServer(){
        return server;
    }

    public static YinwuChat getPlugin(){
        return plugin;
    }

    public static BatManage getBatManage(){
        return batManage;
    }

    public void reload(){
        onDisable();
        onEnable();
    }

    public void reloadConfig(){
        boolean wsopen = config.openwsserver;
        int wsport = config.wsport;
        config.load(this);
        if (wsopen && !config.openwsserver){
            stopWsServer();
        }
        if (config.openwsserver && (!wsopen || wsport!=config.wsport)){
            startWs();
        }
    }

    public boolean wsIsOn(){
        return config.openwsserver && server!=null;
    }

    public WSServer getWsServer() {
        return server;
    }

    @Override
    public void onEnable() {
        plugin = this;
        batManage = new BatManage(this);
        config.load(this);
        if (config.openwsserver){
            startWs();
        }
        MessageManage.setPlugin(this);
        getProxy().registerChannel(Const.PLUGIN_CHANNEL);
        getProxy().getPluginManager().registerListener(this,new Listeners(this));
        getProxy().getPluginManager().registerCommand(this, new Commands(plugin,"yinwuchat"));
    }

    @Override
    public void onDisable() {
        stopWsServer();
        getProxy().unregisterChannel(Const.PLUGIN_CHANNEL);
        getProxy().getPluginManager().unregisterListeners(this);
        getProxy().getPluginManager().unregisterCommands(this);
    }

    public void startWs(){
        stopWsServer();

        int port = config.wsport;
        if (!isPortAvailable(port)) {
            getLogger().info(ChatColor.RED+"端口"+port+"被占用，无法开启WebSocket服务，请检查端口绑定情况，或稍后再试，或修改WebSocket端口！");
            return;
        }
        try {
            server = new WSServer(port,plugin);
            server.start();
            getLogger().info("WebSocket started on port:" + server.getPort());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopWsServer(){
        try {
            if (server != null) {
                getLogger().info("WebSocket stoping...");
                server.stop();
                server = null;
                WsClientHelper.clear();
                getLogger().info("WebSocket stoped");
            }
        } catch (Exception ignored) {
        }
    }

    private boolean isPortAvailable(int port) {
        try {
            ServerSocket server = new ServerSocket(port);
            server.close();
            return true;
        } catch (IOException ignored) {

        }
        return false;
    }
}
