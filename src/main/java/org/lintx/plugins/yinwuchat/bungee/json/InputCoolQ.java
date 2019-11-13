package org.lintx.plugins.yinwuchat.bungee.json;

import com.google.gson.annotations.SerializedName;

public class InputCoolQ extends InputBase {
    @SerializedName("post_type")
    private String post_type="";
    @SerializedName("message_type")
    private String message_type="";
    @SerializedName("sub_type")
    private String sub_type="";
    @SerializedName("group_id")
    private int group_id=0;
    @SerializedName("message")
    private String message="";
    @SerializedName("raw_message")
    private String raw_message="";
    @SerializedName("sender")
    private Sender sender=new Sender();

    public String getPost_type() {
        return post_type;
    }

    public String getMessage_type() {
        return message_type;
    }

    public String getSub_type() {
        return sub_type;
    }

    public int getGroup_id() {
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

    public static class Sender{
        @SerializedName("nickname")
        private String nickname="";
        @SerializedName("card")
        private String card="";

        public String getNickname() {
            return nickname;
        }

        public String getCard() {
            return card;
        }
    }
}
