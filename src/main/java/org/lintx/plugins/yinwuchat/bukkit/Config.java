package org.lintx.plugins.yinwuchat.bukkit;

import org.lintx.plugins.modules.configure.Configure;
import org.lintx.plugins.modules.configure.YamlConfig;
import org.lintx.plugins.yinwuchat.json.MessageFormat;

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

    private Config(){

    }

    public void load(YinwuChat plugin){
        Configure.bukkitLoad(plugin,this);
        if (format==null || format.isEmpty()){
            format = new ArrayList<>();
            format.add(new MessageFormat("&b[%player_server%]","所在服务器：%player_server%","/server %player_server%"));
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
            fromFormat.add(new MessageFormat("&b[%player_server%]","所在服务器：%player_server%","/server %player_server%"));
            fromFormat.add(new MessageFormat("&e{displayName}","点击私聊","/msg {displayName}"));
            fromFormat.add(new MessageFormat(" &6-> &7我"));
            fromFormat.add(new MessageFormat(" &6>>> "));
            fromFormat.add(new MessageFormat("&r{message}"));
        }
        Configure.bukkitSave(plugin,this);
    }
}