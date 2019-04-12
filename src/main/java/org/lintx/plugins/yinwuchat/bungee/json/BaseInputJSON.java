/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lintx.plugins.yinwuchat.bungee.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
                String action = object.get("action").getAsString();
                if (action.equalsIgnoreCase("check_token")) {
                    return new InputCheckToken(object.get("token").getAsString());
                }
                else if (action.equalsIgnoreCase("send_message")) {
                    return new InputMessage(object.get("message").getAsString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
