package org.lintx.plugins.yinwuchat.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.bstats.velocity.Metrics;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.velocity.command.IgnoreCommand;
import org.lintx.plugins.yinwuchat.velocity.command.ItemDisplayCommand;
import org.lintx.plugins.yinwuchat.velocity.command.MuteCommand;
import org.lintx.plugins.yinwuchat.velocity.command.NoAtCommand;
import org.lintx.plugins.yinwuchat.velocity.command.PrivateMessageCommand;
import org.lintx.plugins.yinwuchat.velocity.command.VanishCommand;
import org.lintx.plugins.yinwuchat.velocity.command.QQCommand;
import org.lintx.plugins.yinwuchat.velocity.command.YinwuChatCommand;
import org.lintx.plugins.yinwuchat.velocity.announcement.AnnouncementConfig;
import org.lintx.plugins.yinwuchat.velocity.announcement.AnnouncementTask;
import org.lintx.plugins.yinwuchat.velocity.config.Config;
import org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityHttpServer;
import org.lintx.plugins.yinwuchat.velocity.listeners.Listeners;
import org.lintx.plugins.yinwuchat.velocity.message.MessageManage;
import org.lintx.plugins.yinwuchat.velocity.util.RedisUtil;
import com.velocitypowered.api.scheduler.ScheduledTask;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * YinwuChat Velocity Proxy Plugin
 */
@Plugin(
    id = "yinwuchat-velocity",
    name = "YinwuChat",
    version = "2.12.76",
    description = "Cross-server chat synchronization for Velocity",
    authors = {"LinTx"}
)
public class YinwuChat {
    private static YinwuChat plugin;
    private static VelocityHttpServer server = null;
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private Config config;
    private Metrics.Factory metricsFactory;
    private ScheduledTask announcementScheduledTask;

    @Inject
    public YinwuChat(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
        plugin = this;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        logger.info("YinwuChat initializing...");
        
        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                logger.error("Failed to create data directory", e);
                return;
            }
        }

        config = Config.getInstance();
        config.load(this);
        
        MessageManage.getInstance().setPlugin(this);
        
        proxy.getEventManager().register(this, new Listeners(this));
        
        try {
            proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from(Const.PLUGIN_CHANNEL_VELOCITY));
        } catch (Exception e) {
            logger.warn("Failed to register velocity channel '{}': {}", Const.PLUGIN_CHANNEL_VELOCITY, e.getMessage());
        }
        try {
            proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from(Const.PLUGIN_CHANNEL_BUKKIT));
        } catch (Exception e) {
            logger.warn("Failed to register bukkit channel '{}': {}", Const.PLUGIN_CHANNEL_BUKKIT, e.getMessage());
        }
        
        CommandManager commandManager = proxy.getCommandManager();
        
        YinwuChatCommand yinwuChatCommand = new YinwuChatCommand(this);
        commandManager.register(
            commandManager.metaBuilder("yinwuchat")
                .plugin(this)
                .build(),
            yinwuChatCommand
        );
        commandManager.register(
            commandManager.metaBuilder("chatban")
                .plugin(this)
                .build(),
            yinwuChatCommand
        );
        commandManager.register(
            commandManager.metaBuilder("chatunban")
                .plugin(this)
                .build(),
            yinwuChatCommand
        );
        
        PrivateMessageCommand msgCommand = new PrivateMessageCommand(proxy);
        commandManager.register(
            commandManager.metaBuilder("msg")
                .plugin(this)
                .aliases("tell", "whisper", "w", "t")
                .build(),
            msgCommand
        );
        
        QQCommand qqCommand = new QQCommand(this);
        commandManager.register(
            commandManager.metaBuilder("qq")
                .plugin(this)
                .build(),
            qqCommand
        );

        ItemDisplayCommand itemDisplayCommand = new ItemDisplayCommand(this);
        commandManager.register(
            commandManager.metaBuilder("itemdisplay")
                .plugin(this)
                .aliases("showitem", "displayitem")
                .build(),
            itemDisplayCommand
        );
        
        MuteCommand muteCommand = new MuteCommand(this, "mute");
        commandManager.register(
            commandManager.metaBuilder("mute")
                .plugin(this)
                .build(),
            muteCommand
        );
        
        MuteCommand unmuteCommand = new MuteCommand(this, "unmute");
        commandManager.register(
            commandManager.metaBuilder("unmute")
                .plugin(this)
                .build(),
            unmuteCommand
        );
        
        MuteCommand muteInfoCommand = new MuteCommand(this, "muteinfo");
        commandManager.register(
            commandManager.metaBuilder("muteinfo")
                .plugin(this)
                .build(),
            muteInfoCommand
        );
        
        IgnoreCommand ignoreCommand = new IgnoreCommand(this);
        commandManager.register(
            commandManager.metaBuilder("ignore")
                .plugin(this)
                .build(),
            ignoreCommand
        );
        
        NoAtCommand noAtCommand = new NoAtCommand(this);
        commandManager.register(
            commandManager.metaBuilder("noat")
                .plugin(this)
                .build(),
            noAtCommand
        );
        
        VanishCommand vanishCommand = new VanishCommand(this);
        commandManager.register(
            commandManager.metaBuilder("vanish")
                .plugin(this)
                .build(),
            vanishCommand
        );
        
        if (config.openwsserver) {
            startWsServer();
        }
        
        if (config.redisConfig.openRedis) {
            RedisUtil.init(this);
        }
        
        try {
            if (metricsFactory != null) {
                metricsFactory.make(this, 10357);
            } else {
                logger.info("bStats metrics factory unavailable, skipping metrics initialization");
            }
        } catch (Exception e) {
            logger.info("bStats metrics unavailable: {}", e.getMessage());
        }
        
        // 加载广播配置并启动定时广播任务
        try {
            startAnnouncementTask();
        } catch (Exception e) {
            logger.warn("Failed to start broadcast task: " + e.getMessage());
        }
        
        extractWebFiles();
        
        logger.info("YinwuChat initialized successfully");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("YinwuChat shutting down...");
        stopAnnouncementTask();
        RedisUtil.unload();
        stopWsServer();
    }

    public static YinwuChat getPlugin() {
        return plugin;
    }
    
    public static YinwuChat getInstance() {
        return plugin;
    }

    public static VelocityHttpServer getWSServer() {
        return server;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataFolder() {
        return dataDirectory;
    }

    public Config getConfig() {
        return config;
    }

    public void reload() {
        stopWsServer();
        RedisUtil.unload();
        stopAnnouncementTask();
        config.load(this);
        if (config.openwsserver) {
            startWsServer();
        }
        if (config.redisConfig.openRedis) {
            RedisUtil.init(this);
        }
        // 重新加载广播配置并启动任务
        try {
            startAnnouncementTask();
        } catch (Exception e) {
            logger.warn("Failed to start broadcast task on reload: " + e.getMessage());
        }
    }

    private void startWsServer() {
        stopWsServer();
        int port = config.wsport;
        if (!isPortAvailable(port)) return;
        try {
            server = new VelocityHttpServer(port, this, dataDirectory.resolve("web").toFile());
            proxy.getScheduler().buildTask(this, server::start).schedule();
        } catch (Exception e) {}
    }

    private void stopWsServer() {
        try {
            if (server != null) {
                server.stop();
                server = null;
            }
        } catch (Exception e) {}
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new java.net.InetSocketAddress(port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void extractWebFiles() {
        Path webPath = dataDirectory.resolve("web");
        if (!Files.exists(webPath)) {
            try {
                Files.createDirectories(webPath);
            } catch (IOException e) {
                return;
            }
        }
        
        File indexFile = webPath.resolve("index.html").toFile();
        try (InputStream input = getClass().getResourceAsStream("/web/index.html")) {
            if (input != null) {
                Files.copy(input, indexFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {}

        File avatarFile = webPath.resolve("avater.png").toFile();
        try (InputStream input = getClass().getResourceAsStream("/web/avater.png")) {
            if (input != null) {
                Files.copy(input, avatarFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {}

        File forgeFile = webPath.resolve("forge.min.js").toFile();
        try (InputStream input = getClass().getResourceAsStream("/web/forge.min.js")) {
            if (input != null) {
                Files.copy(input, forgeFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {}

        File logoFile = webPath.resolve("logo.png").toFile();
        try (InputStream input = getClass().getResourceAsStream("/web/logo.png")) {
            if (input != null) {
                Files.copy(input, logoFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {}

        Path fontsPath = webPath.resolve("fonts");
        try {
            Files.createDirectories(fontsPath);
        } catch (IOException ignored) {}
        copyWebResource("/web/fonts/HarmonyOS_Sans_SC_Regular.ttf", fontsPath.resolve("HarmonyOS_Sans_SC_Regular.ttf"));
        copyWebResource("/web/fonts/HarmonyOS_Sans_SC_Medium.ttf", fontsPath.resolve("HarmonyOS_Sans_SC_Medium.ttf"));
        copyWebResource("/web/fonts/HarmonyOS_Sans_SC_Bold.ttf", fontsPath.resolve("HarmonyOS_Sans_SC_Bold.ttf"));
    }

    private void copyWebResource(String classpathResource, Path targetPath) {
        try (InputStream input = getClass().getResourceAsStream(classpathResource)) {
            if (input != null) {
                Files.copy(input, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {}
    }

    /**
     * 加载广播配置并启动定时广播任务（每秒检查一次）
     */
    private void startAnnouncementTask() {
        stopAnnouncementTask();
        AnnouncementConfig.getInstance().load(this);
        announcementScheduledTask = proxy.getScheduler()
                .buildTask(this, new AnnouncementTask())
                .repeat(1, TimeUnit.SECONDS)
                .schedule();
        logger.info("Broadcast task started");
    }

    /**
     * 停止广播定时任务
     */
    private void stopAnnouncementTask() {
        if (announcementScheduledTask != null) {
            announcementScheduledTask.cancel();
            announcementScheduledTask = null;
        }
    }
}

