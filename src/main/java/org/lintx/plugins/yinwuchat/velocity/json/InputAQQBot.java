package org.lintx.plugins.yinwuchat.velocity.json;

import com.google.gson.annotations.SerializedName;

/**
 * AQQBot (OneBot标准) 接收消息格式
 * 兼容 AQQBot、Lagrange、LLoneBot 等基于 OneBot 标准的 QQ 机器人框架
 */
public class InputAQQBot extends InputBase {
    @SerializedName("post_type")
    private String post_type = "";
    
    @SerializedName("message_type")
    private String message_type = "";
    
    @SerializedName("sub_type")
    private String sub_type = "";
    
    @SerializedName("group_id")
    private long group_id = 0;
    
    @SerializedName("message")
    private String message = "";
    
    @SerializedName("raw_message")
    private String raw_message = "";
    
    @SerializedName("sender")
    private Sender sender = new Sender();

    public String getPost_type() {
        return post_type;
    }

    public String getMessage_type() {
        return message_type;
    }

    public String getSub_type() {
        return sub_type;
    }

    public long getGroup_id() {
        return group_id;
    }

    public String getMessage() {
        return message;
    }

    public String getRaw_message() {
        return raw_message;
    }

    public Sender getSender() {
        return sender;
    }

    public static class Sender {
        @SerializedName("nickname")
        private String nickname = "";
        
        @SerializedName("card")
        private String card = "";

        public String getNickname() {
            return nickname;
        }

        public String getCard() {
            return card;
        }
    }
}








