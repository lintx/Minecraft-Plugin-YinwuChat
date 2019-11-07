package org.lintx.plugins.yinwuchat.bukkit;

import org.lintx.plugins.modules.configure.Configure;
import org.lintx.plugins.modules.configure.YamlConfig;
import org.lintx.plugins.yinwuchat.json.MessageFormat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@YamlConfig
public class Config {
    private static Config instance = new Config();

    public static Config getInstance(){
        return instance;
    }

    @YamlConfig
    public List<MessageFormat> format = null;

    @YamlConfig
    public List<MessageFormat> toFormat = null;

    @YamlConfig
    public List<MessageFormat> fromFormat = null;

    @YamlConfig
    public int eventDelayTime = 50;

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
        File file = new File(plugin.getDataFolder(),"config.yml");
        if (!file.exists()){
            Configure.bukkitSave(plugin,this);
        }
    }
}