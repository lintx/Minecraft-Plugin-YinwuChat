package org.lintx.plugins.yinwuchat.bukkit;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.json.Message;
import org.lintx.plugins.yinwuchat.json.MessageFormat;
import org.lintx.plugins.yinwuchat.json.PrivateMessage;
import org.lintx.plugins.yinwuchat.json.PublicMessage;
import org.lintx.plugins.yinwuchat.Util.ItemUtil;

import java.util.ArrayList;
import java.util.List;

public class MessageManage {
    private static MessageManage instance = new MessageManage();
    private YinwuChat plugin;
    private MessageManage(){}
    void setPlugin(YinwuChat plugin){
        this.plugin = plugin;
    }
    static MessageManage getInstance(){
        return instance;
    }

    void sendPrivateMessage(Player player,String toPlayer,String chat){
        PrivateMessage privateMessage = new PrivateMessage();
        privateMessage.toPlayer = toPlayer;
        privateMessage.player = player.getDisplayName();
        privateMessage.chat = chat;

        privateMessage.toFormat = format(player,Config.getInstance().toFormat);
        privateMessage.fromFormat = format(player,Config.getInstance().fromFormat);

        if (chat.contains(Const.ITEM_PLACEHOLDER)){
            privateMessage.item = ItemUtil.itemJsonWithPlayer(player);
        }

        sendPluginMessage(player,Const.PLUGIN_SUB_CHANNEL_MSG,privateMessage);
    }

    void sendPublicMessage(Player player, String chat){
        PublicMessage publicMessage = new PublicMessage();
        publicMessage.player = player.getDisplayName();
        publicMessage.chat = chat;

        publicMessage.format = format(player,Config.getInstance().format);

        if (chat.contains(Const.ITEM_PLACEHOLDER)){
            publicMessage.item = ItemUtil.itemJsonWithPlayer(player);
        }

        sendPluginMessage(player,Const.PLUGIN_SUB_CHANNEL_CHAT,publicMessage);
    }

    private void sendPluginMessage(Player player,String channel, Message message){
        String json = new Gson().toJson(message);
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(channel);
        output.writeUTF(json);
        player.sendPluginMessage(plugin,Const.PLUGIN_CHANNEL,output.toByteArray());
    }

    private List<MessageFormat> format(Player player, List<MessageFormat> formats){
        boolean hasPAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null &&
                Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        List<MessageFormat> list = new ArrayList<>();
        for (MessageFormat message:formats){
            if (message.message==null || message.message.equals("")) continue;

            String msg = message.message;
            msg = papi(hasPAPI,msg,player);

            MessageFormat format = new MessageFormat(msg);
            if (message.hover!=null){
                format.hover = papi(hasPAPI,message.hover,player);
            }
            if (message.click!=null){
                format.click = papi(hasPAPI,message.click,player);
            }
            list.add(format);
        }
        return list;
    }

    private String papi(boolean open,String string,Player player){
        if (!open){
            return string;
        }
        return PlaceholderAPI.setPlaceholders(player,string);
    }
}
