package org.lintx.plugins.yinwuchat.bungee;

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

    public void load(YinwuChat plugin){
        Configure.bungeeLoad(plugin,this);
        if (format==null || format.isEmpty()){
            format = new ArrayList<>();
            format.add(new MessageFormat("&b[Web]","点击打开YinwuChat网页","https://chat.yinwurealm.org"));
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
            fromFormat.add(new MessageFormat("&b[Web]","点击打开YinwuChat网页","https://xxxxxx.xxxx.xxx"));
            fromFormat.add(new MessageFormat("&e{displayName}","点击私聊","/msg {displayName}"));
            fromFormat.add(new MessageFormat(" &6-> &7我"));
            fromFormat.add(new MessageFormat(" &6>>> "));
            fromFormat.add(new MessageFormat("&r{message}"));
        }
        if (qqFormat==null || qqFormat.isEmpty()){
            qqFormat = new ArrayList<>();
            qqFormat.add(new MessageFormat("&b[QQ群]","点击加入QQ群xxxxx","https://xxxxxx.xxxx.xxx"));
            qqFormat.add(new MessageFormat("&e{displayName}"));
            qqFormat.add(new MessageFormat(" &6>>> "));
            qqFormat.add(new MessageFormat("&r{message}"));
        }
        File file = new File(plugin.getDataFolder(),"config.yml");
        if (!file.exists()){
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
    public List<MessageFormat> format = null;

    @YamlConfig
    public List<MessageFormat> qqFormat = null;

    @YamlConfig
    public List<MessageFormat> toFormat = null;

    @YamlConfig
    public List<MessageFormat> fromFormat = null;

    @YamlConfig
    public int atcooldown = 10;

    @YamlConfig
    public String atAllKey = "all";

    @YamlConfig
    public String linkRegex = "((https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])";

    @YamlConfig
    public String linkText = "&7[&f&l链接&r&7]&r";

    @YamlConfig
    public String qqImageText = "&7[图片]&r";

    @YamlConfig
    public String qqRecordText = "&7[语音]&r";

    @YamlConfig
    public String qqAtText = "&7[@{qq}]&r";

    @YamlConfig
    public String atyouselfTip = "&c你不能@你自己";

    @YamlConfig
    public String atyouTip = "&e{player}&b@了你";

    @YamlConfig
    public String cooldownTip = "&c每次使用@功能之间需要等待10秒";

    @YamlConfig
    public String ignoreTip = "&c对方忽略了你，并向你仍了一个烤土豆";

    @YamlConfig
    public String banatTip = "&c对方不想被@，只想安安静静的做一个美男子";

    @YamlConfig
    public String toPlayerNoOnlineTip = "&c对方不在线，无法发送私聊";

    @YamlConfig
    public String msgyouselfTip = "&c你不能私聊你自己";

    @YamlConfig
    public String youismuteTip = "&c你正在禁言中，不能说话";

    @YamlConfig
    public String youisbanTip = "&c你被ban了，不能说话";

    @YamlConfig
    public String shieldedTip = "&c发送的信息中有被屏蔽的词语，无法发送，继续发送将被踢出服务器";

    @YamlConfig
    public List<String> shieldeds = new ArrayList<>();

    @YamlConfig
    public int shieldedMode = 1;

    @YamlConfig
    public String shieldedReplace = "富强、民主、文明、和谐、自由、平等、公正、法治、爱国、敬业、诚信、友善";

    @YamlConfig
    public int shieldedKickTime = 60;

    @YamlConfig
    public int shieldedKickCount = 3;

    @YamlConfig
    public String shieldedKickTip = "你因为发送屏蔽词语，被踢出服务器";

    @YamlConfig
    public int coolQGroup = 0;

    @YamlConfig
    public String coolQAccessToken = "";
}
