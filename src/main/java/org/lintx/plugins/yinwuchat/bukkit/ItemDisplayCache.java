package org.lintx.plugins.yinwuchat.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 物品展示缓存系统
 * 用于存储玩家展示的物品，支持点击查看功能
 */
public class ItemDisplayCache {
    private static ItemDisplayCache instance;
    private final Map<String, CachedItem> displayItems = new ConcurrentHashMap<>();
    private Plugin plugin;
    
    // 缓存过期时间（毫秒）- 5分钟
    private static final long CACHE_EXPIRE_TIME = 5 * 60 * 1000;
    
    private ItemDisplayCache() {}
    
    public static ItemDisplayCache getInstance() {
        if (instance == null) {
            instance = new ItemDisplayCache();
        }
        return instance;
    }
    
    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
        // 启动定期清理任务
        startCleanupTask();
    }
    
    /**
     * 存储物品并返回唯一标识符
     * @param item 要存储的物品
     * @param playerName 发送者名称
     * @return 物品的唯一标识符（短格式）
     */
    public String storeItem(ItemStack item, String playerName) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        
        // 生成短格式的唯一标识符（使用 UUID 的前8位）
        String id = UUID.randomUUID().toString().substring(0, 8);
        
        CachedItem cachedItem = new CachedItem();
        cachedItem.item = item.clone();
        cachedItem.playerName = playerName;
        cachedItem.createTime = System.currentTimeMillis();
        
        displayItems.put(id, cachedItem);
        
        if (plugin != null) {
            plugin.getLogger().info("Stored item for display: " + id + " from player: " + playerName);
        }
        
        return id;
    }
    
    /**
     * 获取缓存的物品
     * @param id 物品标识符
     * @return 物品，如果不存在或已过期则返回 null
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
        if (plugin == null) return;
        
        // 每5分钟清理一次过期缓存
        org.lintx.plugins.yinwuchat.Util.SchedulerUtil.runTaskTimerAsynchronously(plugin, this::cleanupExpired, 
            6000L, 6000L); // 6000 ticks = 5分钟
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
        public ItemStack item;
        public String playerName;
        public long createTime;
    }
}
