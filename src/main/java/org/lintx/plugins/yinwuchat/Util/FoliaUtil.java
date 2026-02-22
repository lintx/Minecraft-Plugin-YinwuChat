package org.lintx.plugins.yinwuchat.Util;

/**
 * Folia 运行时检测工具类
 * 用于检测当前服务器是否运行在 Folia 环境中
 */
public class FoliaUtil {
    
    private static final boolean IS_FOLIA;
    private static final boolean HAS_REGION_SCHEDULER;
    
    static {
        boolean folia = false;
        boolean regionScheduler = false;
        
        // 检测 Folia 核心类
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
            // 不是 Folia
        }
        
        // 检测区域调度器
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            regionScheduler = true;
        } catch (ClassNotFoundException ignored) {
            // 没有区域调度器
        }
        
        IS_FOLIA = folia;
        HAS_REGION_SCHEDULER = regionScheduler;
    }
    
    /**
     * 检测当前服务器是否运行在 Folia 环境中
     * @return true 如果是 Folia 服务器
     */
    public static boolean isFolia() {
        return IS_FOLIA;
    }
    
    /**
     * 检测是否支持区域调度器
     * @return true 如果支持区域调度器
     */
    public static boolean hasRegionScheduler() {
        return HAS_REGION_SCHEDULER;
    }
    
    /**
     * 获取服务器类型描述
     * @return 服务器类型字符串
     */
    public static String getServerType() {
        if (IS_FOLIA) {
            return "Folia";
        }
        
        // 检测 Paper
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return "Paper";
        } catch (ClassNotFoundException ignored) {
        }
        
        try {
            Class.forName("io.papermc.paper.configuration.Configuration");
            return "Paper";
        } catch (ClassNotFoundException ignored) {
        }
        
        // 检测 Spigot
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return "Spigot";
        } catch (ClassNotFoundException ignored) {
        }
        
        return "Unknown";
    }
}
