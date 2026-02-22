package org.lintx.plugins.yinwuchat.velocity.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Velocity 端的物品展示缓存系统
 * 用于跨服务器物品展示功能
 */
public class VelocityItemDisplayCache {
    private static VelocityItemDisplayCache instance;
    private final Map<String, CachedItem> displayItems = new ConcurrentHashMap<>();
    private ScheduledExecutorService cleanupExecutor;
    
    // 缓存过期时间（毫秒）- 5分钟
    private static final long CACHE_EXPIRE_TIME = 5 * 60 * 1000;
    
    private VelocityItemDisplayCache() {
        startCleanupTask();
    }
    
    public static VelocityItemDisplayCache getInstance() {
        if (instance == null) {
            instance = new VelocityItemDisplayCache();
        }
        return instance;
    }
    
    /**
     * 存储物品数据
     * @param id 物品的唯一标识符（由 Bukkit 端生成）
     * @param itemJson 物品的 JSON 数据
     * @param playerName 发送者名称
     * @param serverName 发送者所在服务器
     */
    public void storeItem(String id, String itemJson, String playerName, String serverName) {
        if (id == null || id.isEmpty() || itemJson == null || itemJson.isEmpty()) {
            return;
        }
        
        CachedItem cachedItem = new CachedItem();
        cachedItem.itemJson = itemJson;
        cachedItem.playerName = playerName;
        cachedItem.serverName = serverName;
        cachedItem.createTime = System.currentTimeMillis();
        
        displayItems.put(id, cachedItem);
    }
    
    /**
     * 获取缓存的物品数据
     * @param id 物品标识符
     * @return 物品数据，如果不存在或已过期则返回 null
     */
    public CachedItem getItem(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        
        CachedItem cached = displayItems.get(id);
        if (cached == null) {
            return null;
        }
        
        // 检查是否过期
        if (System.currentTimeMillis() - cached.createTime > CACHE_EXPIRE_TIME) {
            displayItems.remove(id);
            return null;
        }
        
        return cached;
    }
    
    /**
     * 移除缓存的物品
     * @param id 物品标识符
     */
    public void removeItem(String id) {
        displayItems.remove(id);
    }
    
    /**
     * 清理过期的缓存
     */
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        displayItems.entrySet().removeIf(entry -> 
            now - entry.getValue().createTime > CACHE_EXPIRE_TIME
        );
    }
    
    /**
     * 启动定期清理任务
     */
    private void startCleanupTask() {
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpired, 5, 5, TimeUnit.MINUTES);
    }
    
    /**
     * 关闭清理任务
     */
    public void shutdown() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
        }
    }
    
    /**
     * 获取缓存数量（用于调试）
     */
    public int getCacheSize() {
        return displayItems.size();
    }
    
    /**
     * 缓存的物品信息
     */
    public static class CachedItem {
        public String itemJson;      // 物品的完整 JSON 数据（用于重建 ItemStack）
        public String playerName;    // 发送者名称
        public String serverName;    // 发送者所在服务器
        public long createTime;      // 创建时间
    }
}
