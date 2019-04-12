package org.lintx.plugins.yinwuchat.bukkit;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Commands implements CommandExecutor, TabExecutor {
    private final YinwuChat plugin;
    public Commands(YinwuChat plugin){
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
            //应该加入判断不能对自己发消息
            String to_player_name = args[0];
            List<String> tmpList = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
            String msg = String.join(" ", tmpList);

            String pn = player.getDisplayName().toLowerCase(Locale.ROOT);
            String tpn = to_player_name.toLowerCase(Locale.ROOT);
            if (tpn.equals(pn) || (tpn.length()<pn.length() && pn.startsWith(tpn))) {
                commandSender.sendMessage(ChatColor.RED + "你不能向自己发送私聊信息");
                return true;
            }

            MessageManage.getInstance().sendPrivateMessage(player,to_player_name,msg);
            return true;
        }
        else{
            commandSender.sendMessage(ChatColor.RED + "命令格式：/msg 玩家名 消息");
            commandSender.sendMessage(ChatColor.RED + "缺少玩家名或消息");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return null;
    }
}
