package org.lintx.plugins.yinwuchat.bukkit;

import org.lintx.plugins.modules.configure.Configure;
import org.lintx.plugins.modules.configure.YamlConfig;
import org.lintx.plugins.yinwuchat.json.HandleConfig;
import org.lintx.plugins.yinwuchat.json.MessageFormat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@YamlConfig
public class Config {
    private static int version = 2; // 更新版本号以强制重新生成配置
    private static Config instance = new Config();

    public static Config getInstance(){
        return instance;
    }

    @YamlConfig
    List<MessageFormat> format = null;

    @YamlConfig
    List<MessageFormat> toFormat = null;

    @YamlConfig
    List<MessageFormat> fromFormat = null;

    @YamlConfig
    int eventDelayTime = 0;

    @YamlConfig
    List<HandleConfig> messageHandles = null;

    @YamlConfig
    String serverName = null; // 服务器名称配置

    @YamlConfig
    public String adminGroup = "admin";  // LuckPerms 管理员组名

    @YamlConfig
    public List<String> admins = new ArrayList<>(); // 备用管理员列表

    /**
     * 检查玩家是否拥有管理员权限（检查 LuckPerms 组或配置文件列表）
     * @param player Bukkit 玩家对象
     * @return 是否拥有管理员权限
     */
    public boolean isAdmin(org.bukkit.entity.Player player) {
        if (player == null) return false;
        
        // 1. 优先检查 LuckPerms 组权限
        if (adminGroup != null && !adminGroup.isEmpty()) {
            if (player.hasPermission("group." + adminGroup)) {
                return true;
            }
        }
        
        // 2. 检查是否有通用的管理员权限
        if (player.hasPermission(org.lintx.plugins.yinwuchat.Const.PERMISSION_ADMIN)) {
            return true;
        }
        
        // 3. 备用方案：检查配置文件中的 admins 列表
        if (admins != null) {
            for (String admin : admins) {
                if (admin != null && admin.equalsIgnoreCase(player.getName())) {
                    return true;
                }
            }
        }
        
        return player.isOp();
    }

    @YamlConfig
    private int configVersion = 0;

    private Config(){

    }

    public void load(YinwuChat plugin){
        Configure.bukkitLoad(plugin,this);

        // 设置默认服务器名称（自动检测）
        if (serverName == null || serverName.trim().isEmpty()) {
            // 尝试自动检测服务器名称
            try {
                // 从系统属性获取
                String detectedName = System.getProperty("yinwuchat.server.name");
                if (detectedName == null || detectedName.trim().isEmpty()) {
                    // 从环境变量获取
                    detectedName = System.getenv("YINWUCHAT_SERVER_NAME");
                }
                if (detectedName == null || detectedName.trim().isEmpty()) {
                    // 使用默认名称
                    detectedName = "lobby";
                }
                serverName = detectedName.trim();
            } catch (Exception e) {
                serverName = "lobby";
            }
        }

        if (format==null || format.isEmpty()){
            format = new ArrayList<>();
            format.add(new MessageFormat("&b[ServerName]","&e所在服务器：&aServerName","/server ServerName"));
            format.add(new MessageFormat("&e{displayName}","&b点击私聊","/msg {displayName}"));
            format.add(new MessageFormat(" &6>>> "));
            format.add(new MessageFormat("&r{message}"));
        }
        if (toFormat==null || toFormat.isEmpty()){
            toFormat = new ArrayList<>();
            toFormat.add(new MessageFormat("&7我 &6-> "));
            toFormat.add(new MessageFormat("&e{toPlayer}","点击私聊","/msg {toPlayer}")); // 发送者看到的：显示接收者名称
            toFormat.add(new MessageFormat(" &6>>> "));
            toFormat.add(new MessageFormat("&r{message}"));
        }
        if (fromFormat==null || fromFormat.isEmpty()){
            fromFormat = new ArrayList<>();
            fromFormat.add(new MessageFormat("&e{formPlayer}","点击私聊","/msg {formPlayer}")); // 接收者看到的：显示发送者名称
            fromFormat.add(new MessageFormat(" &6-> &7我"));
            fromFormat.add(new MessageFormat(" &6>>> "));
            fromFormat.add(new MessageFormat("&r{message}"));
        }
        if (messageHandles==null) messageHandles = new ArrayList<>();
        if (configVersion==0 && messageHandles.isEmpty()){
            HandleConfig position = new HandleConfig();
            position.placeholder = "\\[p\\]";
            position.format = new ArrayList<>();
            position.format.add(new MessageFormat("&7[&cX:%player_x% &9Z:%player_z% &aY:%player_y%&7]","所在服务器：ServerName\n所在世界：%player_world%\n坐标：&cX:%player_x% &9Z:%player_z% &aY:%player_y%",""));
            messageHandles.add(position);
        }
        File file = new File(plugin.getDataFolder(),"config.yml");
        if (!file.exists() || version!=configVersion){
            configVersion = version;
            Configure.bukkitSave(plugin,this);
        }
    }
}