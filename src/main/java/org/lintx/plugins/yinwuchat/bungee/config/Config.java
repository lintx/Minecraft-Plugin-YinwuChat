package org.lintx.plugins.yinwuchat.bungee.config;

import org.lintx.plugins.modules.configure.Configure;
import org.lintx.plugins.modules.configure.YamlConfig;
import org.lintx.plugins.yinwuchat.bungee.YinwuChat;
import org.lintx.plugins.yinwuchat.json.MessageFormat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@YamlConfig
public class Config {
    private static int version = 5;
    private static Config instance = new Config();
    public static Config getInstance(){
        return instance;
    }

    public void load(YinwuChat plugin){
        Configure.bungeeLoad(plugin,this);
        if (formatConfig.format==null || formatConfig.format.isEmpty()){
            formatConfig.format = new ArrayList<>();
            formatConfig.format.add(new MessageFormat("&b[Web]","点击打开YinwuChat网页","https://chat.yinwurealm.org"));
            formatConfig.format.add(new MessageFormat("&e{displayName}","点击私聊","/msg {displayName}"));
            formatConfig.format.add(new MessageFormat(" &6>>> "));
            formatConfig.format.add(new MessageFormat("&r{message}"));
        }
        if (formatConfig.toFormat==null || formatConfig.toFormat.isEmpty()){
            formatConfig.toFormat = new ArrayList<>();
            formatConfig.toFormat.add(new MessageFormat("&7我 &6-> "));
            formatConfig.toFormat.add(new MessageFormat("&e{displayName}","点击私聊","/msg {displayName}"));
            formatConfig.toFormat.add(new MessageFormat(" &6>>> "));
            formatConfig.toFormat.add(new MessageFormat("&r{message}"));
        }
        if (formatConfig.monitorFormat==null || formatConfig.monitorFormat.isEmpty()){
            formatConfig.monitorFormat = new ArrayList<>();
            formatConfig.monitorFormat.add(new MessageFormat("&7{formPlayer} &6-> "));
            formatConfig.monitorFormat.add(new MessageFormat("&e{toPlayer}"));
            formatConfig.monitorFormat.add(new MessageFormat(" &6>>> "));
            formatConfig.monitorFormat.add(new MessageFormat("&r{message}"));
        }
        if (formatConfig.fromFormat==null || formatConfig.fromFormat.isEmpty()){
            formatConfig.fromFormat = new ArrayList<>();
            formatConfig.fromFormat.add(new MessageFormat("&b[Web]","点击打开YinwuChat网页","https://xxxxxx.xxxx.xxx"));
            formatConfig.fromFormat.add(new MessageFormat("&e{displayName}","点击私聊","/msg {displayName}"));
            formatConfig.fromFormat.add(new MessageFormat(" &6-> &7我"));
            formatConfig.fromFormat.add(new MessageFormat(" &6>>> "));
            formatConfig.fromFormat.add(new MessageFormat("&r{message}"));
        }
        if (formatConfig.qqFormat==null || formatConfig.qqFormat.isEmpty()){
            formatConfig.qqFormat = new ArrayList<>();
            formatConfig.qqFormat.add(new MessageFormat("&b[QQ群]","点击加入QQ群xxxxx","https://xxxxxx.xxxx.xxx"));
            formatConfig.qqFormat.add(new MessageFormat("&e{displayName}"));
            formatConfig.qqFormat.add(new MessageFormat(" &6>>> "));
            formatConfig.qqFormat.add(new MessageFormat("&r{message}"));
        }
        File file = new File(plugin.getDataFolder(),"config.yml");
        if (!file.exists() || version!=configVersion){
            if (file.exists()){
                File bakConfig = new File(plugin.getDataFolder(),"config_v" + configVersion + ".yml");
                try {
                    Files.copy(file.toPath(),bakConfig.toPath());
                } catch (IOException ignored) {

                }
            }
            configVersion = version;
            save(plugin);
        }
    }

    public void save(YinwuChat plugin){
        Configure.bungeeSave(plugin,this);
    }

    @YamlConfig
    public boolean openwsserver = false;

    @YamlConfig
    public int wsport = 8888;

    @YamlConfig
    public int wsCooldown = 1000;

    @YamlConfig
    public String webBATserver = "lobby";

    @YamlConfig
    public int atcooldown = 10;

    @YamlConfig
    public String atAllKey = "all";

    @YamlConfig
    public String linkRegex = "((https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])";

    @YamlConfig
    public List<String> shieldeds = new ArrayList<>();

    @YamlConfig
    public int shieldedMode = 1;

    @YamlConfig
    public int shieldedKickTime = 60;

    @YamlConfig
    public int shieldedKickCount = 3;

    @YamlConfig
    private int configVersion = 0;

    @YamlConfig
    public String webDenyStyle = "klmnor";

    @YamlConfig
    public boolean allowPlayerFormatPrefixSuffix = true;

    @YamlConfig
    public String playerFormatPrefixSuffixDenyStyle = "klmnor";

    @YamlConfig
    public TipsConfig tipsConfig = new TipsConfig();

    @YamlConfig
    public FormatConfig formatConfig = new FormatConfig();

    @YamlConfig
    public CoolQConfig coolQConfig = new CoolQConfig();

    @YamlConfig
    public RedisConfig redisConfig = new RedisConfig();
}
