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
    private static int version = 1;
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
    private int configVersion = 0;

    private Config(){

    }

    public void load(YinwuChat plugin){
        Configure.bukkitLoad(plugin,this);
        if (format==null || format.isEmpty()){
            format = new ArrayList<>();
            format.add(new MessageFormat("&b[ServerName]","所在服务器：ServerName","/server ServerName"));
            format.add(new MessageFormat("&e{displayName}","点击私聊","/msg {displayName}"));
            format.add(new MessageFormat(" &6>>> "));
            format.add(new MessageFormat("&r{message}"));
        }
        if (toFormat==null || toFormat.isEmpty()){
            toFormat = new ArrayList<>();
            toFormat.add(new MessageFormat("&7我 &6-> "));
            toFormat.add(new MessageFormat("&e{displayName}","点击私聊","/msg {displayName}"));
            toFormat.add(new MessageFormat(" &6>>> "));
            toFormat.add(new MessageFormat("&r{message}"));
        }
        if (fromFormat==null || fromFormat.isEmpty()){
            fromFormat = new ArrayList<>();
            fromFormat.add(new MessageFormat("&b[ServerName]","所在服务器：ServerName","/server ServerName"));
            fromFormat.add(new MessageFormat("&e{displayName}","点击私聊","/msg {displayName}"));
            fromFormat.add(new MessageFormat(" &6-> &7我"));
            fromFormat.add(new MessageFormat(" &6>>> "));
            fromFormat.add(new MessageFormat("&r{message}"));
        }
        if (messageHandles==null) messageHandles = new ArrayList<>();
        if (configVersion==0 && messageHandles.isEmpty()){
            HandleConfig position = new HandleConfig();
            position.placeholder = "\\[p\\]";
            position.format = new ArrayList<>();
            position.format.add(new MessageFormat("&7[位置]","所在服务器：ServerName\n所在世界：%player_world%\n坐标：X:%player_x% Y:%player_y% Z:%player_z%",""));
            messageHandles.add(position);
        }
        File file = new File(plugin.getDataFolder(),"config.yml");
        if (!file.exists() || version!=configVersion){
            configVersion = version;
            Configure.bukkitSave(plugin,this);
        }
    }
}