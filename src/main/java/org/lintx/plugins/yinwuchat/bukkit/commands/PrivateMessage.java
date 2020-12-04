package org.lintx.plugins.yinwuchat.bukkit.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.lintx.plugins.yinwuchat.bukkit.MessageManage;
import org.lintx.plugins.yinwuchat.bukkit.YinwuChat;

import java.util.*;

public class PrivateMessage implements CommandExecutor, TabExecutor {
    private final YinwuChat plugin;
    public PrivateMessage(YinwuChat plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String labelName, String[] args) {
        if (!(commandSender instanceof Player)){
            commandSender.sendMessage(ChatColor.RED +"Must use command in-game");
            return true;
        }
        Player player = (Player)commandSender;
        if (args.length>=2) {
            String to_player_name = args[0];
            List<String> tmpList = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
            String msg = String.join(" ", tmpList);

            MessageManage.getInstance().onPrivateMessage(player,to_player_name,msg);
            return true;
        }
        else{
            commandSender.sendMessage(ChatColor.RED + "命令格式：/msg 玩家名 消息");
            commandSender.sendMessage(ChatColor.RED + "缺少玩家名或消息");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String alias, String[] args) {
        List<String> players = new ArrayList<>(plugin.bungeePlayerList);
        if (args.length>0){
            String name = args[0];
            String n = name.toLowerCase(Locale.ROOT);
            players.removeIf(s -> !s.toLowerCase(Locale.ROOT).contains(n)); //移除无相关性的玩家
        }
        return players;
    }
}
