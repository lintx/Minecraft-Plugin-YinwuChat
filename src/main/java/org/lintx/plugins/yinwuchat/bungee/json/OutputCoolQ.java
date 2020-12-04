package org.lintx.plugins.yinwuchat.bungee.json;

import com.google.gson.annotations.SerializedName;
import org.lintx.plugins.yinwuchat.Util.Gson;
import org.lintx.plugins.yinwuchat.bungee.config.Config;

public class OutputCoolQ {
    public OutputCoolQ(String message){
        this.params = new Params(message);
    }

    @SerializedName("action")
    private String action="send_group_msg";
    @SerializedName("params")
    private Params params;

    public static class Params{
        Params(String message){
            this.message = message;
        }
        @SerializedName("group_id")
        private int group_id= Config.getInstance().coolQConfig.coolQGroup;
        @SerializedName("message")
        private String message="";
        @SerializedName("auto_escape")
        private boolean auto_escape=true;
    }

    public String getJSON(){
        return Gson.gson().toJson(this);
    }
}
