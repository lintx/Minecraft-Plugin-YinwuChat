/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lintx.plugins.yinwuchat.bungee.json;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.lintx.plugins.yinwuchat.bungee.PlayerConfig;
import java.util.UUID;

/**
 *
 * @author jjcbw01
 */
public class InputCheckToken extends BaseInputJSON{
    private Boolean isvaild = false;
    private Boolean isbind = false;
    private String message = "";
    private String token = "";
    private UUID uuid = null;
    private PlayerConfig.Tokens tokens = PlayerConfig.getTokens();
    
    public Boolean getIsvaild(){
        return isvaild;
    }
    
    public Boolean getIsbind(){
        return isbind;
    }
    
    public String getMessage(){
        return message;
    }
    
    public String getToken(){
        return token;
    }
    
    public UUID getUuid(){
        return uuid;
    }
    
    public String getJSON(){
        JsonObject json = new JsonObject();
        json.addProperty("action", "check_token");
        json.addProperty("isbind", isbind);
        json.addProperty("status", isvaild);
        json.addProperty("message", message);
        return new Gson().toJson(json);
    }
    
    public String getTokenJSON(){
        JsonObject json = new JsonObject();
        json.addProperty("action", "update_token");
        json.addProperty("token", token);
        return new Gson().toJson(json);
    }

    public InputCheckToken(String token){
        this(token,true);
    }
    
    public InputCheckToken(String token,Boolean autoNewToken){
        this.token = token;
        if (token==null || token.equalsIgnoreCase("")) {
            if (autoNewToken) {
                message = "生成了新的token";
                this.token = tokens.newToken();
            }
        }
        else{
            if (!tokens.tokenIsVaild(token)) {
                message = "token无效";
                if (autoNewToken) {
                    message += "，生成了新的token";
                    this.token = tokens.newToken();
                }
            }
            else{
                isvaild = true;
                message = "success";
                isbind = tokens.tokenIsBind(token);
                if (isbind){
                    uuid = tokens.getUuid(token);
                }
            }
        }
    }

}
