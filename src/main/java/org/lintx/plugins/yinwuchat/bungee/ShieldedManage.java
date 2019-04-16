package org.lintx.plugins.yinwuchat.bungee;


import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.java_websocket.WebSocket;
import org.lintx.plugins.yinwuchat.bungee.json.ServerMessageJSON;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ShieldedManage {
    private static final ShieldedManage instance = new ShieldedManage();
    public static ShieldedManage getInstance(){
        return instance;
    }

    Map<String,Man> users = new HashMap<>();

    private class Man {
        int count = 0;
        LocalDateTime first = null;
    }

    String formatMessage(String string){
        string = string.replaceAll("&([0-9a-fklmnor])","§$1");
        return string;
    }

    Result checkShielded(WebSocket webSocket,String uuid,String message){
        Result result = checkShielded(uuid,message);
        if (result.kick){
            webSocket.send(ServerMessageJSON.errorJSON(formatMessage(Config.getInstance().shieldedKickTip)).getJSON());
            webSocket.close();
        }
        else if (result.shielded){
            webSocket.send(ServerMessageJSON.errorJSON(formatMessage(Config.getInstance().shieldedTip)).getJSON());
        }
        return result;
    }

    Result checkShielded(ProxiedPlayer player,String message){
        Result result = checkShielded(player.getUniqueId().toString(),message);
        if (result.kick){
            player.disconnect(new TextComponent(formatMessage(Config.getInstance().shieldedKickTip)));
        }
        else if (result.shielded){
            player.sendMessage(new TextComponent(formatMessage(Config.getInstance().shieldedTip)));
        }
        return result;
    }

    private Result checkShielded(String uuid,String message){
        String string = message.replaceAll("&([0-9a-fklmnor])","").replaceAll(" ","").toLowerCase(Locale.ROOT);
        Result result = new Result();
        Config config = Config.getInstance();
        if (config.shieldeds.parallelStream().anyMatch(string::contains)){
            YinwuChat.getPlugin().getLogger().info(uuid + " send a shielded word: " + message);
            result.shielded = true;
            if (!users.containsKey(uuid)){
                Man man = new Man();
                man.first = LocalDateTime.now();
                users.put(uuid,man);
            }

            Man man = users.get(uuid);
            Duration duration = Duration.between(man.first,LocalDateTime.now());
            if (duration.toMillis()>config.shieldedKickTime*1000){
                man.first = LocalDateTime.now();
                man.count = 0;
            }
            man.count += 1;
            users.put(uuid,man);
            if (man.count>=config.shieldedKickCount){
                result.kick = true;
                return result;
            }

            if (config.shieldedMode==1){
                result.msg = config.shieldedReplace;
            }
            else {
                result.end = true;
            }
        }
        return result;
    }

    class Result{
        boolean shielded = false;
        boolean end = false;
        boolean kick = false;
        String msg = "";
    }
}
