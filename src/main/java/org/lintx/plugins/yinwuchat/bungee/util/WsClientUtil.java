/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lintx.plugins.yinwuchat.bungee.util;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author jjcbw01
 */
public class WsClientUtil {
    private UUID uuid = null;
    private String token;
    private LocalDateTime lastDate;
    
    public WsClientUtil(String token,UUID uuid){
        this.token = token;
        this.uuid = uuid;
        this.lastDate = LocalDateTime.MIN;
    }
    
    public WsClientUtil(String token){
        this(token, null);
    }
    
    public void setUUID(UUID uuid){
        this.uuid = uuid;
    }
    
    public UUID getUuid(){
        return uuid;
    }
    
    public String getToken(){
        return token;
    }
    
    public LocalDateTime getLastDate(){
        return lastDate;
    }
    
    public void updateLastDate(){
        lastDate = LocalDateTime.now();
    }
}
