/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lintx.plugins.yinwuchat.bungee.httpserver;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 *
 * @author LinTx
 */
public class WsClientUtil {
    private UUID uuid = null;
    private String token;
    private LocalDateTime lastDate;
    
    private WsClientUtil(String token, UUID uuid){
        this.token = token;
        this.uuid = uuid;
        this.lastDate = LocalDateTime.MIN;
    }
    
    WsClientUtil(String token){
        this(token, null);
    }
    
    public void setUUID(UUID uuid){
        this.uuid = uuid;
    }
    
    public UUID getUuid(){
        return uuid;
    }
    
    String getToken(){
        return token;
    }
    
    LocalDateTime getLastDate(){
        return lastDate;
    }
    
    void updateLastDate(){
        lastDate = LocalDateTime.now();
    }
}
