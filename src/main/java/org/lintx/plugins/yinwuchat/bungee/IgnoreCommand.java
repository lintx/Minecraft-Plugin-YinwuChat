package org.lintx.plugins.yinwuchat.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.lintx.plugins.yinwuchat.Util.MessageUtil;
import org.lintx.plugins.yinwuchat.bungee.config.PlayerConfig;
import org.lintx.plugins.yinwuchat.bungee.httpserver.WsClientHelper;
import org.lintx.plugins.yinwuchat.bungee.httpserver.WsClientUtil;

import java.util.Locale;

public class IgnoreCommand extends Command {
    private final YinwuChat plugin;
    public IgnoreCommand(YinwuChat plugin,String name) {
        super(name);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(buildMessage("Must use command in-game"));
            return;
        }
        final ProxiedPlayer player = (ProxiedPlayer)sender;
        if (args.length>=1) {
            PlayerConfig.Player playerConfig = PlayerConfig.getConfig(player);
            String name = args[0].toLowerCase(Locale.ROOT);
            for (ProxiedPlayer p:plugin.getProxy().getPlayers()){
                if (p.getName().toLowerCase(Locale.ROOT).equals(name)){
                    if (playerConfig.ignore(p.getUniqueId())){
                        sender.sendMessage(buildMessage(ChatColor.RED + "你现在忽略了"+p.getName()+"的信息"));
                    }
                    else {
                        sender.sendMessage(buildMessage(ChatColor.GREEN + "你不再忽略"+p.getName()+"的信息"));
                    }
                    return;
                }
            }

            for (WsClientUtil util: WsClientHelper.utils()){
                PlayerConfig.Player p = PlayerConfig.getConfig(util.getUuid());
                if (name.equals(p.name.toLowerCase(Locale.ROOT))){
                    if (playerConfig.ignore(util.getUuid())){
                        sender.sendMessage(buildMessage(ChatColor.RED + "你现在忽略了"+p.name+"的信息"));
                    }
                    else {
                        sender.sendMessage(buildMessage(ChatColor.GREEN + "你不再忽略"+p.name+"的信息"));
                    }
                    return;
                }
            }
        }
        sender.sendMessage(buildMessage(ChatColor.GOLD + "忽略某人的消息：/yinwuchat ignore <player_name>（再输入一次不再忽略）"));
    }

    private TextComponent buildMessage(String message){
        return new TextComponent(MessageUtil.replace(message));
    }
}
