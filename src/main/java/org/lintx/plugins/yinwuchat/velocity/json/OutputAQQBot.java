package org.lintx.plugins.yinwuchat.velocity.json;

import com.google.gson.annotations.SerializedName;
import org.lintx.plugins.yinwuchat.Util.Gson;
import org.lintx.plugins.yinwuchat.velocity.config.Config;

/**
 * 发送到 AQQBot (OneBot标准) 的消息格式（Velocity版本）
 * 兼容 AQQBot、Lagrange、LLoneBot 等基于 OneBot 标准的 QQ 机器人框架
 */
public class OutputAQQBot {
    public OutputAQQBot(String message) {
        this.params = new Params(message);
    }

    @SerializedName("action")
    private String action = "send_group_msg";
    
    @SerializedName("params")
    private Params params;

    public static class Params {
        Params(String message) {
            this.message = message;
            // 优先使用 AQQBot 配置，如果未配置则使用旧版 CoolQ 配置（向后兼容）
            Config config = Config.getInstance();
            if (config.aqqBotConfig != null && config.aqqBotConfig.qqGroup > 0) {
                this.group_id = config.aqqBotConfig.qqGroup;
            } else if (config.coolQConfig != null && config.coolQConfig.coolQGroup > 0) {
                this.group_id = config.coolQConfig.coolQGroup;
            } else {
                this.group_id = 0;
            }
        }
        
        @SerializedName("group_id")
        private long group_id;
        
        @SerializedName("message")
        private String message;
        
        @SerializedName("auto_escape")
        private boolean auto_escape = true;
    }

    /**
     * 获取JSON格式的消息（OneBot 标准格式）
     * @return JSON字符串
     */
    public String getJSON() {
        return Gson.gson().toJson(this);
    }
}








