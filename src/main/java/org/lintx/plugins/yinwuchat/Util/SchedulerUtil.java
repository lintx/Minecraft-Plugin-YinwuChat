package org.lintx.plugins.yinwuchat.Util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 任务调度工具类
 * 自动识别并兼容 Bukkit 和 Folia 的任务调度机制
 * 使用反射调用 Folia API，避免编译期依赖
 */
public class SchedulerUtil {

    private static Method getAsyncSchedulerMethod;
    private static Method runAtFixedRateMethod;
    private static Method runDelayedMethod;
    private static Method getEntitySchedulerMethod;
    private static Method entitySchedulerRunMethod;
    private static boolean reflectionInitialized = false;

    private static synchronized void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;

        if (!FoliaUtil.isFolia()) return;

        try {
            getAsyncSchedulerMethod = Bukkit.getServer().getClass().getMethod("getAsyncScheduler");
            Object asyncScheduler = getAsyncSchedulerMethod.invoke(Bukkit.getServer());
            Class<?> asyncSchedulerClass = asyncScheduler.getClass();

            for (Method m : asyncSchedulerClass.getMethods()) {
                if (m.getName().equals("runAtFixedRate") && m.getParameterCount() == 5) {
                    runAtFixedRateMethod = m;
                }
                if (m.getName().equals("runDelayed") && m.getParameterCount() == 4) {
                    runDelayedMethod = m;
                }
            }

            getEntitySchedulerMethod = Player.class.getMethod("getScheduler");
            Object tempScheduler = null;
            for (Player p : Bukkit.getOnlinePlayers()) {
                tempScheduler = getEntitySchedulerMethod.invoke(p);
                break;
            }
            if (tempScheduler != null) {
                for (Method m : tempScheduler.getClass().getMethods()) {
                    if (m.getName().equals("run") && m.getParameterCount() == 3) {
                        entitySchedulerRunMethod = m;
                        break;
                    }
                }
            } else {
                Class<?> entitySchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
                for (Method m : entitySchedulerClass.getMethods()) {
                    if (m.getName().equals("run") && m.getParameterCount() == 3) {
                        entitySchedulerRunMethod = m;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[SchedulerUtil] Failed to initialize Folia reflection: " + e.getMessage());
        }
    }

    /**
     * 异步延迟执行任务
     *
     * @param plugin 插件实例
     * @param task   要执行的任务
     * @param delay  延迟时间 (ticks, 1 tick = 50ms)
     */
    public static void runTaskLaterAsynchronously(Plugin plugin, Runnable task, long delay) {
        if (FoliaUtil.isFolia()) {
            initReflection();
            try {
                Object asyncScheduler = getAsyncSchedulerMethod.invoke(Bukkit.getServer());
                runDelayedMethod.invoke(asyncScheduler, plugin,
                        (java.util.function.Consumer<Object>) t -> task.run(),
                        delay * 50, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                plugin.getLogger().warning("[SchedulerUtil] Folia runDelayed failed, falling back: " + e.getMessage());
                new Thread(() -> {
                    try { Thread.sleep(delay * 50); } catch (InterruptedException ignored) {}
                    task.run();
                }).start();
            }
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
        }
    }

    /**
     * 异步定时重复执行任务
     *
     * @param plugin 插件实例
     * @param task   要执行的任务
     * @param delay  初次延迟时间 (ticks, 1 tick = 50ms)
     * @param period 执行间隔时间 (ticks, 1 tick = 50ms)
     */
    public static void runTaskTimerAsynchronously(Plugin plugin, Runnable task, long delay, long period) {
        if (FoliaUtil.isFolia()) {
            initReflection();
            try {
                Object asyncScheduler = getAsyncSchedulerMethod.invoke(Bukkit.getServer());
                runAtFixedRateMethod.invoke(asyncScheduler, plugin,
                        (java.util.function.Consumer<Object>) t -> task.run(),
                        delay * 50, period * 50, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                plugin.getLogger().warning("[SchedulerUtil] Folia runAtFixedRate failed, falling back: " + e.getMessage());
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                        task, delay * 50, period * 50, TimeUnit.MILLISECONDS);
            }
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
        }
    }

    /**
     * 在玩家所在的区域线程执行任务 (用于 Folia 中的实体/物品栏操作)
     * 在普通 Bukkit/Paper 中将回退到主线程同步执行
     *
     * @param plugin 插件实例
     * @param player 目标玩家
     * @param task   要执行的任务
     */
    public static void runTaskForPlayer(Plugin plugin, Player player, Runnable task) {
        if (!player.isOnline()) {
            return;
        }

        if (FoliaUtil.isFolia()) {
            initReflection();
            try {
                Object entityScheduler = getEntitySchedulerMethod.invoke(player);
                entitySchedulerRunMethod.invoke(entityScheduler, plugin,
                        (java.util.function.Consumer<Object>) t -> {
                            if (player.isOnline()) {
                                task.run();
                            }
                        }, null);
            } catch (Exception e) {
                plugin.getLogger().warning("[SchedulerUtil] Folia entity scheduler failed: " + e.getMessage());
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    task.run();
                }
            });
        }
    }
}
