package org.lintx.plugins.yinwuchat.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.bungee.announcement.Task;
import org.lintx.plugins.yinwuchat.bungee.util.WsClientHelper;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class YinwuChat extends Plugin {
    private static YinwuChat plugin;
    private static WSServer server = null;
    private static BatManage batManage;
    private ScheduledTask scheduledTask = null;
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
        //重载前是否开启了wsserver
        boolean wsopen = config.openwsserver;
        //重载前wsserver port
        int wsport = config.wsport;
        //重新加载配置
        config.load(this);
        //重载前已开启wsserver且重载后关闭wsserver时关闭wsserver
        if (wsopen && !config.openwsserver){
            stopWsServer();
        }
        //重载后开启wsserver且（重载前关闭wsserver或重载前port和重载后port不一致或wsserver未能成功开启）时启动wsserver
        if (config.openwsserver && (!wsopen || wsport!=config.wsport || server==null)){
            startWs();
        }
        //重新加载task
        org.lintx.plugins.yinwuchat.bungee.announcement.Config.getInstance().load(plugin);
    }

    public boolean wsIsOn(){
        return config.openwsserver && server!=null;
    }

    public WSServer getWsServer() {
        return server;
    }

    @Override
    public void onEnable() {
        if (getProxy().getPluginManager().getPlugin("ConfigureCore")==null){
            getLogger().info("§cDid not find ConfigureCore, YinwuChat has been deactivated!");
            this.onDisable();
            return;
        }
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

        org.lintx.plugins.yinwuchat.bungee.announcement.Config.getInstance().load(this);
        Task task = new Task();
        scheduledTask = getProxy().getScheduler().schedule(this,task, 0L,1L, TimeUnit.SECONDS);

        MessageManage.getInstance().sendPlayerList();
    }

    @Override
    public void onDisable() {
        if (scheduledTask!=null){
            scheduledTask.cancel();
        }
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
                WsClientHelper.updateCoolQ(null);
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
