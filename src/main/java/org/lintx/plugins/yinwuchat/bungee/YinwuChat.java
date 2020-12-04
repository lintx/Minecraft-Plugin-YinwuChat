package org.lintx.plugins.yinwuchat.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.bstats.bungeecord.Metrics;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.bungee.announcement.Task;
import org.lintx.plugins.yinwuchat.bungee.config.Config;
import org.lintx.plugins.yinwuchat.bungee.httpserver.WsClientHelper;
import org.lintx.plugins.yinwuchat.bungee.httpserver.NettyHttpServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class YinwuChat extends Plugin {
    private static YinwuChat plugin;
    private static NettyHttpServer server = null;
    private static BatManage batManage;
    private ScheduledTask scheduledTask = null;
    private Config config = Config.getInstance();
    public static NettyHttpServer getWSServer(){
        return server;
    }

    public static YinwuChat getPlugin(){
        return plugin;
    }

    static BatManage getBatManage(){
        return batManage;
    }

    void reload(){
        onDisable();
        onEnable();
    }

    void reloadConfig(){
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
        redisBungee();
    }

    boolean wsIsOn(){
        return config.openwsserver && server!=null;
    }

    @Override
    public void onEnable() {
//        System.out.println("test show item start");
//        try {
//            String json = "{\"extra\":[{\"text\":\"§r§7[§r\"},{\"extra\":[{\"translate\":\"block.minecraft.dirt\"}],\"text\":\"\"},{\"text\":\"§r§7]§r\"}],\"hoverEvent\":{\"action\":\"show_item\",\"value\":[{\"text\":\"{id:\\\"minecraft:dirt\\\",Count:1b}\"}]},\"text\":\"\"}";
//            System.out.println(json);
//            BaseComponent[] component = ComponentSerializer.parse(json);
//            if (component.length>0){
//                System.out.println(component[0].toLegacyText());
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        System.out.println("test show item end");
        autoFreedWeb();
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
        getProxy().getPluginManager().registerCommand(this,new IgnoreCommand(this,"ignore"));

        org.lintx.plugins.yinwuchat.bungee.announcement.Config.getInstance().load(this);
        Task task = new Task();
        scheduledTask = getProxy().getScheduler().schedule(this,task, 0L,1L, TimeUnit.SECONDS);

        MessageManage.getInstance().sendPlayerListToServer();

        redisBungee();

        Metrics metrics = new Metrics(this);
    }

    private void redisBungee(){
        if (config.redisConfig.openRedis){
            RedisUtil.init(this);
        }
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
        RedisUtil.unload();
    }

    void startWs(){
        stopWsServer();

        int port = config.wsport;
        if (!isPortAvailable(port)) {
            getLogger().info(ChatColor.RED+"端口"+port+"被占用，无法开启WebSocket服务，请检查端口绑定情况，或稍后再试，或修改WebSocket端口！");
            return;
        }
        try {
            server = new NettyHttpServer(config.wsport,this,new File(getDataFolder(),"web"));
            getProxy().getScheduler().runAsync(this, () -> server.start());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopWsServer(){
        try {
            if (server != null) {
                getLogger().info("Http Server stoping...");
                server.stop();
                server = null;
                WsClientHelper.clear();
                WsClientHelper.updateCoolQ(null);
                getLogger().info("Http Server stoped");
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

    private void autoFreedWeb(){
        String rootFolder = "web/";
        File folder = this.getDataFolder();
        folder.mkdir();
        new File(folder,rootFolder).mkdir();

        try (ZipFile zipFile = new ZipFile(getFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                copyFile(zipFile, entry, rootFolder);
            }
        } catch (IOException ignored) {

        }
    }

    private void copyFile(ZipFile zipFile, ZipEntry entry, String rootFolder){
        String name = entry.getName();
        if(!name.startsWith(rootFolder)){
            return;
        }

        File file = new File(getDataFolder(), name);
        if(entry.isDirectory()) {
            file.mkdirs();
        }else {
            if (file.exists()){
                return;
            }
            if (file.getParentFile().isDirectory()){
                file.getParentFile().mkdir();
            }
            FileOutputStream outputStream = null;
            InputStream inputStream = null;
            try {
                byte[] buffer = new byte[1024];
                outputStream = new FileOutputStream(file);
                inputStream = zipFile.getInputStream(entry);
                int len;
                while ((len = inputStream.read(buffer)) >= 0) {
                    outputStream.write(buffer,  0,  len);
                }
            } catch (IOException ignored) {

            } finally {
                try {
                    outputStream.close();
                } catch (IOException ignored) {

                }
                try {
                    inputStream.close();
                } catch (IOException ignored) {

                }
            }
        }
    }
}
