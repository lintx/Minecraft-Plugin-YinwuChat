package org.lintx.plugins.yinwuchat.velocity.json;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * 服务器消息输出
 */
public class OutputServerMessage {
    private String message;
    private String type;
    
    public OutputServerMessage(String message, String type) {
        this.message = message;
        this.type = type;
    }
    
    public String getJSON() {
        JsonObject json = new JsonObject();
        json.addProperty("action", "server_message");
        json.addProperty("type", type);
        json.addProperty("message", message);
        return new Gson().toJson(json);
    }
    
    public static OutputServerMessage errorJSON(String message) {
        return new OutputServerMessage(message, "error");
    }
    
    public static OutputServerMessage infoJSON(String message) {
        return new OutputServerMessage(message, "info");
    }
}
