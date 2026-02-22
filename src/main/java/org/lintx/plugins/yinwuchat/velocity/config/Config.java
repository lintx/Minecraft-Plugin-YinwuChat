package org.lintx.plugins.yinwuchat.velocity.config;

import org.lintx.plugins.modules.configure.YamlConfig;
import org.lintx.plugins.yinwuchat.json.MessageFormat;
import org.lintx.plugins.yinwuchat.velocity.YinwuChat;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Configuration management for Velocity plugin
 * Uses SnakeYAML directly to avoid BungeeCord class dependencies
 */
@YamlConfig
public class Config {
    private static final int VERSION = 7; // 更新版本号以强制重新生成私聊格式配置
    private static final Config instance = new Config();

    public static Config getInstance() {
        return instance;
    }

    @SuppressWarnings("unchecked")
    public void load(YinwuChat plugin) {
        File configDir = plugin.getDataFolder().toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        File configFile = new File(configDir, "config.yml");
        
        // 尝试从文件加载配置
        if (configFile.exists()) {
            try (InputStream inputStream = new FileInputStream(configFile)) {
                Yaml yaml = new Yaml();
                java.util.Map<String, Object> data = yaml.load(inputStream);
                if (data != null) {
                    loadFromMap(data);
                }
            } catch (Exception e) {
                plugin.getLogger().warn("Failed to load configuration: " + e.getMessage());
            }
        }
        
        // 设置默认格式
        initDefaultFormats();
        
        // 版本兼容性
        if (configVersion < 6) {
            redisConfig.selfPrefixFormat = new ArrayList<>();
            redisConfig.selfPrefixFormat.add(new MessageFormat("&8[Other Cluster]&r", "Message from other cluster", ""));
        }
        
        // 如果配置文件不存在或版本不匹配，保存默认配置
        if (!configFile.exists() || VERSION != configVersion) {
            if (configFile.exists()) {
                File bakConfig = new File(configDir, "config_v" + configVersion + ".yml");
                try {
                    if (!bakConfig.exists()) {
                        Files.copy(configFile.toPath(), bakConfig.toPath());
                    }
                } catch (IOException ignored) {}
            }
            configVersion = VERSION;
            save(plugin);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void loadFromMap(java.util.Map<String, Object> data) {
        if (data.containsKey("openwsserver")) openwsserver = (Boolean) data.get("openwsserver");
        if (data.containsKey("wsport")) wsport = ((Number) data.get("wsport")).intValue();
        if (data.containsKey("wsCooldown")) wsCooldown = ((Number) data.get("wsCooldown")).intValue();
        if (data.containsKey("atcooldown")) atcooldown = ((Number) data.get("atcooldown")).intValue();
        if (data.containsKey("atAllKey")) atAllKey = (String) data.get("atAllKey");
        if (data.containsKey("linkRegex")) linkRegex = (String) data.get("linkRegex");
        if (data.containsKey("shieldeds")) shieldeds = (List<String>) data.get("shieldeds");
        if (data.containsKey("shieldedMode")) shieldedMode = ((Number) data.get("shieldedMode")).intValue();
        if (data.containsKey("shieldedKickTime")) shieldedKickTime = ((Number) data.get("shieldedKickTime")).intValue();
        if (data.containsKey("shieldedKickCount")) shieldedKickCount = ((Number) data.get("shieldedKickCount")).intValue();
        if (data.containsKey("configVersion")) configVersion = ((Number) data.get("configVersion")).intValue();
        if (data.containsKey("webDenyStyle")) webDenyStyle = (String) data.get("webDenyStyle");
        
        // 加载玩家前后缀配置
        if (data.containsKey("allowPlayerFormatPrefixSuffix")) allowPlayerFormatPrefixSuffix = (Boolean) data.get("allowPlayerFormatPrefixSuffix");
        if (data.containsKey("maxPrefixLength")) maxPrefixLength = ((Number) data.get("maxPrefixLength")).intValue();
        if (data.containsKey("maxSuffixLength")) maxSuffixLength = ((Number) data.get("maxSuffixLength")).intValue();
        if (data.containsKey("playerFormatPrefixSuffixDenyStyle")) playerFormatPrefixSuffixDenyStyle = (String) data.get("playerFormatPrefixSuffixDenyStyle");
        
        // 加载管理员列表
        if (data.containsKey("admins")) {
            Object obj = data.get("admins");
            if (obj instanceof List) {
                admins = new ArrayList<>();
                for (Object item : (List<?>) obj) {
                    if (item != null) admins.add(item.toString());
                }
            }
        }
        
        if (data.containsKey("adminGroup")) adminGroup = (String) data.get("adminGroup");
        if (data.containsKey("defaultGroup")) defaultGroup = (String) data.get("defaultGroup");
        if (data.containsKey("permissionCommandPrefix")) permissionCommandPrefix = (String) data.get("permissionCommandPrefix");
        if (data.containsKey("permissionPluginIds")) {
            Object obj = data.get("permissionPluginIds");
            if (obj instanceof List) {
                permissionPluginIds = new ArrayList<>();
                for (Object item : (List<?>) obj) {
                    if (item != null) permissionPluginIds.add(item.toString());
                }
            }
        }
        
        // 加载子配置
        if (data.containsKey("tipsConfig") && data.get("tipsConfig") instanceof java.util.Map) {
            loadTipsConfig((java.util.Map<String, Object>) data.get("tipsConfig"));
        }
        if (data.containsKey("formatConfig") && data.get("formatConfig") instanceof java.util.Map) {
            loadFormatConfig((java.util.Map<String, Object>) data.get("formatConfig"));
        }
        if (data.containsKey("coolQConfig") && data.get("coolQConfig") instanceof java.util.Map) {
            loadCoolQConfig((java.util.Map<String, Object>) data.get("coolQConfig"));
        }
        if (data.containsKey("aqqBotConfig") && data.get("aqqBotConfig") instanceof java.util.Map) {
            loadAQQBotConfig((java.util.Map<String, Object>) data.get("aqqBotConfig"));
        }
        if (data.containsKey("redisConfig") && data.get("redisConfig") instanceof java.util.Map) {
            loadRedisConfig((java.util.Map<String, Object>) data.get("redisConfig"));
        }
    }
    
    private void loadTipsConfig(java.util.Map<String, Object> data) {
        if (data.containsKey("shieldedKickTip")) tipsConfig.shieldedKickTip = (String) data.get("shieldedKickTip");
        if (data.containsKey("shieldedReplace")) tipsConfig.shieldedReplace = (String) data.get("shieldedReplace");
        if (data.containsKey("atyouselfTip")) tipsConfig.atyouselfTip = (String) data.get("atyouselfTip");
        if (data.containsKey("atyouTip")) tipsConfig.atyouTip = (String) data.get("atyouTip");
        if (data.containsKey("cooldownTip")) tipsConfig.cooldownTip = (String) data.get("cooldownTip");
        if (data.containsKey("ignoreTip")) tipsConfig.ignoreTip = (String) data.get("ignoreTip");
        if (data.containsKey("banatTip")) tipsConfig.banatTip = (String) data.get("banatTip");
        if (data.containsKey("toPlayerNoOnlineTip")) tipsConfig.toPlayerNoOnlineTip = (String) data.get("toPlayerNoOnlineTip");
        if (data.containsKey("msgyouselfTip")) tipsConfig.msgyouselfTip = (String) data.get("msgyouselfTip");
        if (data.containsKey("youismuteTip")) tipsConfig.youismuteTip = (String) data.get("youismuteTip");
        if (data.containsKey("youisbanTip")) tipsConfig.youisbanTip = (String) data.get("youisbanTip");
        if (data.containsKey("shieldedTip")) tipsConfig.shieldedTip = (String) data.get("shieldedTip");
        if (data.containsKey("linkText")) tipsConfig.linkText = (String) data.get("linkText");
        if (data.containsKey("qqCooldownTip")) tipsConfig.qqCooldownTip = (String) data.get("qqCooldownTip");
        if (data.containsKey("qqSendSuccessTip")) tipsConfig.qqSendSuccessTip = (String) data.get("qqSendSuccessTip");
        if (data.containsKey("qqSendFailTip")) tipsConfig.qqSendFailTip = (String) data.get("qqSendFailTip");
        if (data.containsKey("qqNotEnabledTip")) tipsConfig.qqNotEnabledTip = (String) data.get("qqNotEnabledTip");
    }
    
    @SuppressWarnings("unchecked")
    private void loadFormatConfig(java.util.Map<String, Object> data) {
        if (data.containsKey("format")) formatConfig.format = loadMessageFormatList((List<Object>) data.get("format"));
        if (data.containsKey("qqFormat")) formatConfig.qqFormat = loadMessageFormatList((List<Object>) data.get("qqFormat"));
        if (data.containsKey("toFormat")) formatConfig.toFormat = loadMessageFormatList((List<Object>) data.get("toFormat"));
        if (data.containsKey("fromFormat")) formatConfig.fromFormat = loadMessageFormatList((List<Object>) data.get("fromFormat"));
        if (data.containsKey("monitorFormat")) formatConfig.monitorFormat = loadMessageFormatList((List<Object>) data.get("monitorFormat"));
    }
    
    @SuppressWarnings("unchecked")
    private List<MessageFormat> loadMessageFormatList(List<Object> list) {
        if (list == null) return null;
        List<MessageFormat> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof java.util.Map) {
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) item;
                String message = (String) map.getOrDefault("message", "");
                String hover = (String) map.getOrDefault("hover", "");
                String click = (String) map.getOrDefault("click", "");
                result.add(new MessageFormat(message, hover, click));
            } else if (item instanceof String) {
                result.add(new MessageFormat((String) item));
            }
        }
        return result;
    }
    
    private void loadCoolQConfig(java.util.Map<String, Object> data) {
        if (data.containsKey("coolQQQToGame")) coolQConfig.coolQQQToGame = (Boolean) data.get("coolQQQToGame");
        if (data.containsKey("coolqToGameStart")) coolQConfig.coolqToGameStart = (String) data.get("coolqToGameStart");
        if (data.containsKey("coolQGameToQQ")) coolQConfig.coolQGameToQQ = (Boolean) data.get("coolQGameToQQ");
        if (data.containsKey("gameToCoolqStart")) coolQConfig.gameToCoolqStart = (String) data.get("gameToCoolqStart");
        if (data.containsKey("qqDenyStyle")) coolQConfig.qqDenyStyle = (String) data.get("qqDenyStyle");
        if (data.containsKey("coolQGroup")) coolQConfig.coolQGroup = ((Number) data.get("coolQGroup")).longValue();
        if (data.containsKey("coolQAccessToken")) coolQConfig.coolQAccessToken = (String) data.get("coolQAccessToken");
        if (data.containsKey("qqAtText")) coolQConfig.qqAtText = (String) data.get("qqAtText");
        if (data.containsKey("qqImageText")) coolQConfig.qqImageText = (String) data.get("qqImageText");
        if (data.containsKey("qqRecordText")) coolQConfig.qqRecordText = (String) data.get("qqRecordText");
    }
    
    private void loadAQQBotConfig(java.util.Map<String, Object> data) {
        if (data.containsKey("qqToGame")) aqqBotConfig.qqToGame = (Boolean) data.get("qqToGame");
        if (data.containsKey("qqToGameStart")) aqqBotConfig.qqToGameStart = (String) data.get("qqToGameStart");
        if (data.containsKey("gameToQQ")) aqqBotConfig.gameToQQ = (Boolean) data.get("gameToQQ");
        if (data.containsKey("gameToQQStart")) aqqBotConfig.gameToQQStart = (String) data.get("gameToQQStart");
        if (data.containsKey("qqDenyStyle")) aqqBotConfig.qqDenyStyle = (String) data.get("qqDenyStyle");
        if (data.containsKey("qqGroup")) aqqBotConfig.qqGroup = ((Number) data.get("qqGroup")).longValue();
        if (data.containsKey("accessToken")) aqqBotConfig.accessToken = (String) data.get("accessToken");
        if (data.containsKey("qqAtText")) aqqBotConfig.qqAtText = (String) data.get("qqAtText");
        if (data.containsKey("qqImageText")) aqqBotConfig.qqImageText = (String) data.get("qqImageText");
        if (data.containsKey("qqRecordText")) aqqBotConfig.qqRecordText = (String) data.get("qqRecordText");
    }
    
    @SuppressWarnings("unchecked")
    private void loadRedisConfig(java.util.Map<String, Object> data) {
        if (data.containsKey("openRedis")) redisConfig.openRedis = (Boolean) data.get("openRedis");
        if (data.containsKey("ip")) redisConfig.ip = (String) data.get("ip");
        if (data.containsKey("port")) redisConfig.port = ((Number) data.get("port")).intValue();
        if (data.containsKey("maxConnection")) redisConfig.maxConnection = ((Number) data.get("maxConnection")).intValue();
        if (data.containsKey("password")) redisConfig.password = (String) data.get("password");
        if (data.containsKey("selfName")) redisConfig.selfName = (String) data.get("selfName");
        if (data.containsKey("forwardBcTask")) redisConfig.forwardBcTask = (Boolean) data.get("forwardBcTask");
        if (data.containsKey("forwardBcMessageToQQ")) redisConfig.forwardBcMessageToQQ = (Boolean) data.get("forwardBcMessageToQQ");
        if (data.containsKey("forwardBcMessageToWeb")) redisConfig.forwardBcMessageToWeb = (Boolean) data.get("forwardBcMessageToWeb");
        if (data.containsKey("forwardBcAtAll")) redisConfig.forwardBcAtAll = (Boolean) data.get("forwardBcAtAll");
        if (data.containsKey("forwardBcAtOne")) redisConfig.forwardBcAtOne = (Boolean) data.get("forwardBcAtOne");
        if (data.containsKey("selfPrefixFormat")) {
            redisConfig.selfPrefixFormat = loadMessageFormatList((List<Object>) data.get("selfPrefixFormat"));
        }
    }
    
    private void initDefaultFormats() {
        if (formatConfig.format == null || formatConfig.format.isEmpty()) {
            formatConfig.format = new ArrayList<>();
            formatConfig.format.add(new MessageFormat("&b[ServerName]", "所在服务器：ServerName", "/server ServerName"));
            formatConfig.format.add(new MessageFormat("&e{displayName}", "点击私聊", "/msg {displayName}"));
            formatConfig.format.add(new MessageFormat(" &6>>> "));
            formatConfig.format.add(new MessageFormat("&r{message}"));
        }
        
        if (formatConfig.toFormat == null || formatConfig.toFormat.isEmpty()) {
            formatConfig.toFormat = new ArrayList<>();
            formatConfig.toFormat.add(new MessageFormat("&7我 &6-> "));
            formatConfig.toFormat.add(new MessageFormat("&e{toPlayer}", "点击私聊", "/msg {toPlayer}"));
            formatConfig.toFormat.add(new MessageFormat(" &6>>> "));
            formatConfig.toFormat.add(new MessageFormat("&r{message}"));
        }
        
        if (formatConfig.fromFormat == null || formatConfig.fromFormat.isEmpty()) {
            formatConfig.fromFormat = new ArrayList<>();
            formatConfig.fromFormat.add(new MessageFormat("&e{formPlayer}", "点击私聊", "/msg {formPlayer}"));
            formatConfig.fromFormat.add(new MessageFormat(" &6-> &7我 "));
            formatConfig.fromFormat.add(new MessageFormat(" &6>>> "));
            formatConfig.fromFormat.add(new MessageFormat("&r{message}"));
        }
    }

    public void save(YinwuChat plugin) {
        File configDir = plugin.getDataFolder().toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        File configFile = new File(configDir, "config.yml");
        
        try {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            options.setIndent(2);
            
            Yaml yaml = new Yaml(options);
            java.util.Map<String, Object> data = toMap();
            
            try (Writer writer = new FileWriter(configFile)) {
                yaml.dump(data, writer);
            }
        } catch (Exception e) {
            plugin.getLogger().warn("Failed to save configuration: " + e.getMessage());
        }
    }
    
    private java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        
        map.put("configVersion", configVersion);
        map.put("openwsserver", openwsserver);
        map.put("wsport", wsport);
        map.put("wsCooldown", wsCooldown);
        map.put("atcooldown", atcooldown);
        map.put("atAllKey", atAllKey);
        map.put("linkRegex", linkRegex);
        map.put("shieldeds", shieldeds);
        map.put("shieldedMode", shieldedMode);
        map.put("shieldedKickTime", shieldedKickTime);
        map.put("shieldedKickCount", shieldedKickCount);
        map.put("webDenyStyle", webDenyStyle);
        
        // 玩家前后缀配置
        map.put("allowPlayerFormatPrefixSuffix", allowPlayerFormatPrefixSuffix);
        map.put("maxPrefixLength", maxPrefixLength);
        map.put("maxSuffixLength", maxSuffixLength);
        map.put("playerFormatPrefixSuffixDenyStyle", playerFormatPrefixSuffixDenyStyle);
        
        // 管理员列表（绕过权限检查）
        map.put("admins", admins);
        map.put("adminGroup", adminGroup);
        map.put("defaultGroup", defaultGroup);
        map.put("permissionCommandPrefix", permissionCommandPrefix);
        map.put("permissionPluginIds", permissionPluginIds);
        
        // 子配置
        map.put("tipsConfig", tipsConfigToMap());
        map.put("formatConfig", formatConfigToMap());
        map.put("coolQConfig", coolQConfigToMap());
        map.put("aqqBotConfig", aqqBotConfigToMap());
        map.put("redisConfig", redisConfigToMap());
        
        return map;
    }
    
    private java.util.Map<String, Object> tipsConfigToMap() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("shieldedKickTip", tipsConfig.shieldedKickTip);
        map.put("shieldedReplace", tipsConfig.shieldedReplace);
        map.put("atyouselfTip", tipsConfig.atyouselfTip);
        map.put("atyouTip", tipsConfig.atyouTip);
        map.put("cooldownTip", tipsConfig.cooldownTip);
        map.put("ignoreTip", tipsConfig.ignoreTip);
        map.put("banatTip", tipsConfig.banatTip);
        map.put("toPlayerNoOnlineTip", tipsConfig.toPlayerNoOnlineTip);
        map.put("msgyouselfTip", tipsConfig.msgyouselfTip);
        map.put("youismuteTip", tipsConfig.youismuteTip);
        map.put("youisbanTip", tipsConfig.youisbanTip);
        map.put("shieldedTip", tipsConfig.shieldedTip);
        map.put("linkText", tipsConfig.linkText);
        map.put("qqCooldownTip", tipsConfig.qqCooldownTip);
        map.put("qqSendSuccessTip", tipsConfig.qqSendSuccessTip);
        map.put("qqSendFailTip", tipsConfig.qqSendFailTip);
        map.put("qqNotEnabledTip", tipsConfig.qqNotEnabledTip);
        return map;
    }
    
    private java.util.Map<String, Object> formatConfigToMap() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("format", messageFormatListToList(formatConfig.format));
        map.put("qqFormat", messageFormatListToList(formatConfig.qqFormat));
        map.put("toFormat", messageFormatListToList(formatConfig.toFormat));
        map.put("fromFormat", messageFormatListToList(formatConfig.fromFormat));
        map.put("monitorFormat", messageFormatListToList(formatConfig.monitorFormat));
        return map;
    }
    
    private List<java.util.Map<String, String>> messageFormatListToList(List<MessageFormat> formats) {
        if (formats == null) return null;
        List<java.util.Map<String, String>> list = new ArrayList<>();
        for (MessageFormat format : formats) {
            java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
            map.put("message", format.message);
            if (format.hover != null && !format.hover.isEmpty()) map.put("hover", format.hover);
            if (format.click != null && !format.click.isEmpty()) map.put("click", format.click);
            list.add(map);
        }
        return list;
    }
    
    private java.util.Map<String, Object> coolQConfigToMap() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("coolQQQToGame", coolQConfig.coolQQQToGame);
        map.put("coolqToGameStart", coolQConfig.coolqToGameStart);
        map.put("coolQGameToQQ", coolQConfig.coolQGameToQQ);
        map.put("gameToCoolqStart", coolQConfig.gameToCoolqStart);
        map.put("qqDenyStyle", coolQConfig.qqDenyStyle);
        map.put("coolQGroup", coolQConfig.coolQGroup);
        map.put("coolQAccessToken", coolQConfig.coolQAccessToken);
        map.put("qqAtText", coolQConfig.qqAtText);
        map.put("qqImageText", coolQConfig.qqImageText);
        map.put("qqRecordText", coolQConfig.qqRecordText);
        return map;
    }
    
    private java.util.Map<String, Object> aqqBotConfigToMap() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("qqToGame", aqqBotConfig.qqToGame);
        map.put("qqToGameStart", aqqBotConfig.qqToGameStart);
        map.put("gameToQQ", aqqBotConfig.gameToQQ);
        map.put("gameToQQStart", aqqBotConfig.gameToQQStart);
        map.put("qqDenyStyle", aqqBotConfig.qqDenyStyle);
        map.put("qqGroup", aqqBotConfig.qqGroup);
        map.put("accessToken", aqqBotConfig.accessToken);
        map.put("qqAtText", aqqBotConfig.qqAtText);
        map.put("qqImageText", aqqBotConfig.qqImageText);
        map.put("qqRecordText", aqqBotConfig.qqRecordText);
        return map;
    }
    
    private java.util.Map<String, Object> redisConfigToMap() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("openRedis", redisConfig.openRedis);
        map.put("ip", redisConfig.ip);
        map.put("port", redisConfig.port);
        map.put("maxConnection", redisConfig.maxConnection);
        map.put("password", redisConfig.password);
        map.put("selfName", redisConfig.selfName);
        map.put("forwardBcTask", redisConfig.forwardBcTask);
        map.put("forwardBcMessageToQQ", redisConfig.forwardBcMessageToQQ);
        map.put("forwardBcMessageToWeb", redisConfig.forwardBcMessageToWeb);
        map.put("forwardBcAtAll", redisConfig.forwardBcAtAll);
        map.put("forwardBcAtOne", redisConfig.forwardBcAtOne);
        map.put("selfPrefixFormat", messageFormatListToList(redisConfig.selfPrefixFormat));
        return map;
    }

    @YamlConfig
    public boolean openwsserver = false;

    @YamlConfig
    public int wsport = 8888;

    @YamlConfig
    public int wsCooldown = 1000;

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
    
    // 玩家前后缀配置
    @YamlConfig
    public boolean allowPlayerFormatPrefixSuffix = true;  // 是否允许玩家自定义前后缀
    
    @YamlConfig
    public int maxPrefixLength = 16;  // 前缀最大长度
    
    @YamlConfig
    public int maxSuffixLength = 16;  // 后缀最大长度
    
    @YamlConfig
    public String playerFormatPrefixSuffixDenyStyle = "klmnorxKLMNORX";  // 禁止使用的样式代码
    
    @YamlConfig
    public List<String> admins = new ArrayList<>();  // 管理员列表（绕过权限检查，用于 LuckPerms 权限桥接失效的情况）
    
    @YamlConfig
    public String adminGroup = "admin";  // LuckPerms 管理员组名

    @YamlConfig
    public String defaultGroup = "default";  // LuckPerms 默认组名

    @YamlConfig
    public String permissionCommandPrefix = "lp";  // 权限插件命令前缀

    @YamlConfig
    public List<String> permissionPluginIds = new ArrayList<>(java.util.Arrays.asList("luckperms"));  // 权限插件 ID 列表
    
    /**
     * 检查玩家是否是管理员（在配置的 admins 列表中）
     * @param playerName 玩家名称
     * @return 是否是管理员
     */
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
     * @param player Velocity 玩家对象
     * @return 是否拥有管理员权限
     */
    public boolean isAdmin(com.velocitypowered.api.proxy.Player player) {
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
        return isAdmin(player.getUsername());
    }

    /**
     * 检查玩家是否拥有基础权限（检查 LuckPerms 组或通用权限节点）
     * @param player Velocity 玩家对象
     * @return 是否拥有基础权限
     */
    public boolean isDefault(com.velocitypowered.api.proxy.Player player) {
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
    public TipsConfig tipsConfig = new TipsConfig();

    @YamlConfig
    public FormatConfig formatConfig = new FormatConfig();

    @YamlConfig
    public CoolQConfig coolQConfig = new CoolQConfig(); // 保留以兼容旧配置
    
    @YamlConfig
    public AQQBotConfig aqqBotConfig = new AQQBotConfig(); // AQQBot 配置（推荐使用）

    @YamlConfig
    public RedisConfig redisConfig = new RedisConfig();
}
