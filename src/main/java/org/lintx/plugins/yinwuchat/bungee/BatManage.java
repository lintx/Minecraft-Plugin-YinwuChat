package org.lintx.plugins.yinwuchat.bungee;

import fr.Alphart.BAT.BAT;
import net.md_5.bungee.api.connection.ProxiedPlayer;

class BatManage {
    private final YinwuChat plugin;
    private boolean hasBAT;
    private BAT bat;

    BatManage(YinwuChat plugin){
        this.plugin = plugin;

    }

    private void checkBat(){
        hasBAT = plugin.getProxy().getPluginManager().getPlugin("BungeeAdminTools") != null;
        if (hasBAT){
            try {
                bat = BAT.getInstance();
                if (bat==null){
                    hasBAT = false;
                }
            }
            catch (Exception e){
                hasBAT = false;
            }
        }
    }

    boolean isBan(ProxiedPlayer player,String server){
        checkBat();
        if (!hasBAT) return false;
        try {
            return bat.getModules().getBanModule().isBan(player,server);
        }
        catch (Exception e){
            return false;
        }
    }

    boolean isBan(String player,String server){
        checkBat();
        if (!hasBAT) return false;
        try {
            return bat.getModules().getBanModule().isBan(player,server);
        }
        catch (Exception e){
            return false;
        }
    }

    boolean isMute(ProxiedPlayer player,String server){
        if (!hasBAT) return false;
        try {
            return bat.getModules().getMuteModule().isMute(player,server) > 0;
        }
        catch (Exception e){
            return false;
        }
    }

    boolean isMute(String player,String server){
        if (!hasBAT) return false;
        try {
            return bat.getModules().getMuteModule().isMute(player,server,true);
        }
        catch (Exception e){
            return false;
        }
    }
}
