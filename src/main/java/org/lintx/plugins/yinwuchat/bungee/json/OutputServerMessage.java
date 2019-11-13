/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lintx.plugins.yinwuchat.bungee.json;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Date;

/**
 *
 * @author LinTx
 */
public class OutputServerMessage {
    private final String message;
    private final String action = "server_message";
    private final int status;
    private final static String ERROR_COLOR = "&c";
    private final static String SUCCESS_COLOR = "&a";
    private final static String INFO_COLOR = "&6";
    
    private OutputServerMessage(String message, int status){
        this.message = message;
        this.status = status;
    }
    
    public String getJSON(){
        JsonObject json = new JsonObject();
        json.addProperty("action", action);
        json.addProperty("message", message);
        json.addProperty("time", new Date().getTime());
        json.addProperty("status", status);
        return new Gson().toJson(json);
    }
    
    public static OutputServerMessage errorJSON(String message){
        return new OutputServerMessage(OutputServerMessage.ERROR_COLOR + message,1);
    }
    
    public static OutputServerMessage errorJSON(String message, int status){
        return new OutputServerMessage(OutputServerMessage.ERROR_COLOR + message,status);
    }
    
    public static OutputServerMessage successJSON(String message){
        return new OutputServerMessage(OutputServerMessage.SUCCESS_COLOR + message,0);
    }
    
    public static OutputServerMessage infoJSON(String message){
        return new OutputServerMessage(OutputServerMessage.INFO_COLOR + message,0);
    }
}
