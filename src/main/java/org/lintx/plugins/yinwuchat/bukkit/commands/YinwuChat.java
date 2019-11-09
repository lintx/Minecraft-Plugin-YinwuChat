package org.lintx.plugins.yinwuchat.bukkit.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.lintx.plugins.yinwuchat.bukkit.Config;

import java.util.ArrayList;
import java.util.List;

public class YinwuChat implements CommandExecutor, TabExecutor {
    private final org.lintx.plugins.yinwuchat.bukkit.YinwuChat plugin;
    public YinwuChat(org.lintx.plugins.yinwuchat.bukkit.YinwuChat plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String labelName, String[] args) {
        if (args.length>=1){
            String arg1 = args[0];
            if (arg1.equalsIgnoreCase("reload")){
                if (commandSender instanceof Player){
                    if (!commandSender.hasPermission("yinwuchat.reload")){
                        commandSender.sendMessage(ChatColor.RED + "权限不足");
                        return true;
                    }
                }
                Config.getInstance().load(plugin);
                commandSender.sendMessage(ChatColor.GREEN + "插件配置重新加载成功");
                return true;
            }
        }
        commandSender.sendMessage(ChatColor.RED + "命令格式：/yinwuchat-bukkit reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        list.add("reload");
        return list;
    }
}
