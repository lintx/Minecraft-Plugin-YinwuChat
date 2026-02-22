package org.lintx.plugins.yinwuchat.velocity.json;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.lintx.plugins.yinwuchat.velocity.config.PlayerConfig;

import java.util.UUID;

/**
 * Token 验证请求
 */
public class InputCheckToken extends InputBase {
    private Boolean isvaild = false;
    private Boolean isbind = false;
    private String message = "";
    private String token = "";
    private UUID uuid = null;

    public Boolean getIsvaild() {
        return isvaild;
    }
    
    public Boolean getIsbind() {
        return isbind;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getToken() {
        return token;
    }
    
    public UUID getUuid() {
        return uuid;
    }
    
    public String getJSON() {
        JsonObject json = new JsonObject();
        json.addProperty("action", "check_token");
        json.addProperty("isbind", isbind);
        json.addProperty("status", isvaild ? 0 : 1);
        json.addProperty("message", message);
        return new Gson().toJson(json);
    }
    
    public String getTokenJSON() {
        JsonObject json = new JsonObject();
        json.addProperty("action", "update_token");
        json.addProperty("token", token);
        return new Gson().toJson(json);
    }

    public InputCheckToken(String token) {
        this(token, true);
    }
    
    public InputCheckToken(String token, Boolean autoNewToken) {
        this.token = token;
        PlayerConfig.TokenManager tokens = PlayerConfig.getInstance().getTokenManager();
        
        if (token == null || token.isEmpty()) {
            if (autoNewToken) {
                message = "生成了新的token";
                this.token = tokens.newToken();
            }
        } else {
            if (tokens.tokenNotValid(token)) {
                message = "token无效";
                if (autoNewToken) {
                    message += "，生成了新的token";
                    this.token = tokens.newToken();
                }
            } else {
                isvaild = true;
                message = "success";
                isbind = tokens.tokenIsBind(token);
                if (isbind) {
                    uuid = tokens.getUuid(token);
                }
            }
        }
    }
}
