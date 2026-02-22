package org.lintx.plugins.yinwuchat.bukkit.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.lintx.plugins.yinwuchat.bukkit.Config;

import java.util.ArrayList;
import java.util.List;

public class YinwuChat implements TabExecutor {
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
                    Player player = (Player) commandSender;
                    if (!player.hasPermission(org.lintx.plugins.yinwuchat.Const.PERMISSION_RELOAD) && !Config.getInstance().isAdmin(player)){
                        commandSender.sendMessage(ChatColor.RED + "权限不足");
                        return true;
                    }
                }
                Config.getInstance().load(plugin);
                commandSender.sendMessage(ChatColor.GREEN + "插件配置重新加载成功");
                return true;
            }
            if (arg1.equalsIgnoreCase("atalladmin")){
                if (commandSender instanceof Player){
                    Player player = (Player) commandSender;
                    if (!player.hasPermission(org.lintx.plugins.yinwuchat.Const.PERMISSION_AT_ALL_ADMIN) && !Config.getInstance().isAdmin(player)){
                        commandSender.sendMessage(ChatColor.RED + "权限不足");
                        return true;
                    }
                    // 在 Bukkit 端执行时，通常是为了触发 Proxy 端的逻辑
                    // 由于 Bukkit 端不处理具体的冷却和提醒逻辑（因为提醒是跨服的），
                    // 我们只需将指令转发给 Proxy 即可。
                    // 但是通常玩家直接输入 /yinwuchat 会被 Proxy 拦截。
                    // 如果由于某些原因（如命令冲突）被转发到了 Bukkit，
                    // 我们在这里提示玩家使用 Proxy 端的指令。
                    commandSender.sendMessage(ChatColor.YELLOW + "请确保您是在代理端（Velocity/BungeeCord）执行此指令。");
                }
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
