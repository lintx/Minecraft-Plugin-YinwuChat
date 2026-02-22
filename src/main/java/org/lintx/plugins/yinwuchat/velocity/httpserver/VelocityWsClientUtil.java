package org.lintx.plugins.yinwuchat.velocity.httpserver;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WebSocket 客户端工具类
 */
public class VelocityWsClientUtil {
    private UUID uuid = null;
    private String token;
    private String account = "";
    private LocalDateTime lastDate;
    
    private VelocityWsClientUtil(String token, UUID uuid) {
        this.token = token;
        this.uuid = uuid;
        this.lastDate = LocalDateTime.MIN;
    }
    
    public VelocityWsClientUtil(String token) {
        this(token, null);
    }
    
    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }
    
    public UUID getUuid() {
        return uuid;
    }

    public void setAccount(String account) {
        this.account = account == null ? "" : account;
    }

    public String getAccount() {
        return account;
    }
    
    public String getToken() {
        return token;
    }
    
    public LocalDateTime getLastDate() {
        return lastDate;
    }
    
    public void updateLastDate() {
        lastDate = LocalDateTime.now();
    }
}
