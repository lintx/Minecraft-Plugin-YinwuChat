/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lintx.plugins.yinwuchat.bungee.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 *
 * @author jjcbw01
 */
public class BaseInputJSON {
    
    public static BaseInputJSON getObject(String json){
        try {
            JsonParser parser = new JsonParser();
            JsonElement jsonTree = parser.parse(json);
            if (jsonTree.isJsonObject()) {
                JsonObject object = jsonTree.getAsJsonObject();
                JsonElement actionElement = object.get("action");
                if (actionElement!=null){
                    String action = actionElement.getAsString();
                    if (action.equalsIgnoreCase("check_token")) {
                        return new InputCheckToken(object.get("token").getAsString());
                    }
                    else if (action.equalsIgnoreCase("send_message")) {
                        return new InputMessage(object.get("message").getAsString());
                    }
                }else {
                    JsonElement postTypeElement = object.get("post_type");
                    if (postTypeElement!=null){
                        CoolQInputJSON inputModel = null;
                        try {
                            Gson gson = new Gson();
                            inputModel = gson.fromJson(json,new TypeToken<CoolQInputJSON>(){}.getType());
                            return inputModel;
                        }catch (Exception ignored){
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
