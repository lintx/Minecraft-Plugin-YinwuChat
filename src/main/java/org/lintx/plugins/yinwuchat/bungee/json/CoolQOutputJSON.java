package org.lintx.plugins.yinwuchat.bungee.json;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.lintx.plugins.yinwuchat.bungee.Config;

public class CoolQOutputJSON {
    public CoolQOutputJSON(String message){
        this.params = new Params(message);
    }
    @SerializedName("action")
    private String action="send_group_msg";
    @SerializedName("params")
    private Params params=null;

    public static class Params{
        Params(String message){
            this.message = message;
        }
        @SerializedName("group_id")
        private int group_id= Config.getInstance().coolQGroup;
        @SerializedName("message")
        private String message="";
        @SerializedName("auto_escape")
        private boolean auto_escape=true;
    }

    public String getJSON(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
