package org.lintx.plugins.yinwuchat.bungee.announcement;

import org.lintx.plugins.modules.configure.Configure;
import org.lintx.plugins.modules.configure.YamlConfig;
import org.lintx.plugins.yinwuchat.bungee.YinwuChat;
import org.lintx.plugins.yinwuchat.json.MessageFormat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@YamlConfig(path = "tasks.yml")
public class Config {
    private static Config instance = new Config();
    public static Config getInstance(){
        return instance;
    }

    @YamlConfig
    List<TaskConfig> tasks = new ArrayList<>();

    public void load(YinwuChat plugin){
        tasks = new ArrayList<>();
        Configure.bungeeLoad(plugin,this);
        File file = new File(plugin.getDataFolder(),"tasks.yml");
        if (!file.exists()){
            TaskConfig c = new TaskConfig();
            c.enable = true;
            c.interval = 300;
            c.list.add(new MessageFormat("&e[帮助]","服务器帮助文档",""));
            c.list.add(new MessageFormat("&r 在聊天中输入"));
            c.list.add(new MessageFormat("&b[i]","在聊天文本中包含这三个字符即可",""));
            c.list.add(new MessageFormat("&r可以展示你手中的物品，输入"));
            c.list.add(new MessageFormat("&b[i:x]","&b:&r冒号不区分中英文\n&bx&r为背包格子编号\n物品栏为0-8，然后从背包左上角\n从左至右从上至下为9-35\n装备栏为36-39，副手为40",""));
            c.list.add(new MessageFormat("&r可以展示背包中x位置对应的物品，一条消息中可以展示多个物品"));
            tasks.add(c);

            TaskConfig b = new TaskConfig();
            b.enable = true;
            b.interval = 300;
            b.list.add(new MessageFormat("&e[帮助]","服务器帮助文档",""));
            b.list.add(new MessageFormat("&r 在聊天中输入&b@&r然后后面跟上"));
            b.list.add(new MessageFormat("&b玩家名","不区分服务器\n不区分大小写\n可以只输入玩家名的前n个字符\n玩家名后需要跟中文或空格",""));
            b.list.add(new MessageFormat("&r即可@该玩家，如果不想被别人@可以输入&b/yinwuchat noat&r命令来切换自己是否允许被他人@"));
            tasks.add(b);
            Configure.bungeeSave(plugin,this);
        }
    }
}
