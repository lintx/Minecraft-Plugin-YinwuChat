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
    private static int version = 6;
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
        if (configVersion<6){
            redisConfig.selfPrefixFormat = new ArrayList<>();
            redisConfig.selfPrefixFormat.add(new MessageFormat("&8[其他群组]&r","来自其他群组的消息",""));
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
    public String webDenyStyle = "klmnorxKLMNORX";

    @YamlConfig
    public List<String> admins = new ArrayList<>();

    @YamlConfig
    public String adminGroup = "admin";  // LuckPerms 管理员组名

    @YamlConfig
    public String defaultGroup = "default";  // LuckPerms 默认组名

    @YamlConfig
    public String permissionCommandPrefix = "lp";  // 权限插件命令前缀

    @YamlConfig
    public List<String> permissionPluginIds = new ArrayList<>(java.util.Arrays.asList("luckperms"));  // 权限插件 ID 列表

    public boolean isAdmin(String playerName) {
        if (playerName == null || admins == null) return false;
        for (String admin : admins) {
            if (admin != null && admin.equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查玩家是否拥有管理员权限（检查 LuckPerms 组或配置文件列表）
     * @param player BungeeCord 玩家对象
     * @return 是否拥有管理员权限
     */
    public boolean isAdmin(net.md_5.bungee.api.connection.ProxiedPlayer player) {
        if (player == null) return false;
        
        // 1. 优先检查 LuckPerms 组权限
        if (adminGroup != null && !adminGroup.isEmpty()) {
            if (player.hasPermission("group." + adminGroup)) {
                return true;
            }
        }
        
        // 2. 检查通用的管理员权限节点
        if (player.hasPermission(org.lintx.plugins.yinwuchat.Const.PERMISSION_ADMIN)) {
            return true;
        }
        
        // 3. 备用方案：检查配置文件中的 admins 列表
        return isAdmin(player.getName());
    }

    /**
     * 检查玩家是否拥有基础权限（检查 LuckPerms 组或通用权限节点）
     * @param player BungeeCord 玩家对象
     * @return 是否拥有基础权限
     */
    public boolean isDefault(net.md_5.bungee.api.connection.ProxiedPlayer player) {
        if (player == null) return false;
        
        // 如果是管理员，默认拥有所有基础权限
        if (isAdmin(player)) return true;
        
        // 1. 优先检查 LuckPerms 组权限
        if (defaultGroup != null && !defaultGroup.isEmpty()) {
            if (player.hasPermission("group." + defaultGroup)) {
                return true;
            }
        }
        
        // 2. 检查通用的基础权限节点
        if (player.hasPermission(org.lintx.plugins.yinwuchat.Const.PERMISSION_DEFAULT)) {
            return true;
        }
        
        return false;
    }

    @YamlConfig
    public boolean allowPlayerFormatPrefixSuffix = true;

    @YamlConfig
    public String playerFormatPrefixSuffixDenyStyle = "klmnorxKLMNORX";

    @YamlConfig
    public TipsConfig tipsConfig = new TipsConfig();

    @YamlConfig
    public FormatConfig formatConfig = new FormatConfig();

    @YamlConfig
    public CoolQConfig coolQConfig = new CoolQConfig();

    @YamlConfig
    public RedisConfig redisConfig = new RedisConfig();
}
