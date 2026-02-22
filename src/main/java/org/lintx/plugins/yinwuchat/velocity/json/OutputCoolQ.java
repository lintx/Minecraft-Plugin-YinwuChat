package org.lintx.plugins.yinwuchat.velocity.json;

import com.google.gson.annotations.SerializedName;
import org.lintx.plugins.yinwuchat.Util.Gson;
import org.lintx.plugins.yinwuchat.velocity.config.Config;

/**
 * 发送到CoolQ的消息格式（Velocity版本）
 */
public class OutputCoolQ {
    public OutputCoolQ(String message) {
        this.params = new Params(message);
    }

    @SerializedName("action")
    private String action = "send_group_msg";
    
    @SerializedName("params")
    private Params params;

    public static class Params {
        Params(String message) {
            this.message = message;
            this.group_id = Config.getInstance().coolQConfig.coolQGroup;
        }
        
        @SerializedName("group_id")
        private long group_id;
        
        @SerializedName("message")
        private String message;
        
        @SerializedName("auto_escape")
        private boolean auto_escape = true;
    }

    /**
     * 获取JSON格式的消息
     * @return JSON字符串
     */
    public String getJSON() {
        return Gson.gson().toJson(this);
    }
}

