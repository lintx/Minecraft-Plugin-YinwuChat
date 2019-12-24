package org.lintx.plugins.yinwuchat.bungee.config;

import org.lintx.plugins.modules.configure.YamlConfig;

public class TipsConfig {

    @YamlConfig
    public String shieldedKickTip = "你因为发送屏蔽词语，被踢出服务器";

    @YamlConfig
    public String shieldedReplace = "富强、民主、文明、和谐、自由、平等、公正、法治、爱国、敬业、诚信、友善";

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
    public String linkText = "&7[&f&l链接&r&7]&r";
}
