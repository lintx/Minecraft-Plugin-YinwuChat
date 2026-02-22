package org.lintx.plugins.yinwuchat.Util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ReflectionUtil {
    /*
     * The server version string to location NMS & OBC classes
     */
    private static String versionString;

    /*
     * Cache of NMS classes that we've searched for
     */
    private static Map<String, Class<?>> loadedNMSClasses = new HashMap<String, Class<?>>();

    /*
     * Cache of OBS classes that we've searched for
     */
    private static Map<String, Class<?>> loadedOBCClasses = new HashMap<String, Class<?>>();

    /*
     * Cache of methods that we've found in particular classes
     */
    private static Map<Class<?>, Map<String, Method>> loadedMethods = new HashMap<Class<?>, Map<String, Method>>();

    /*
     * Cache of fields that we've found in particular classes
     */
    private static Map<Class<?>, Map<String, Field>> loadedFields = new HashMap<Class<?>, Map<String, Field>>();

    /**
     * Gets the version string for NMS & OBC class paths
     *
     * @return The version string of OBC and NMS packages
     */
    public static String getVersion() {
        if (versionString == null) {
            String name = Bukkit.getServer().getClass().getPackage().getName();
            versionString = name.substring(name.lastIndexOf('.') + 1) + ".";
        }

        return versionString;
    }

    /**
     * Get an NMS Class
     *
     * @param nmsClassName The name of the class
     * @return The class
     */
    public static Class<?> getNMSClass(String nmsClassName) {
        if (loadedNMSClasses.containsKey(nmsClassName)) {
            return loadedNMSClasses.get(nmsClassName);
        }

        // 尝试新版本的包路径 (1.20.5+)
        // possiblePaths reserved for future version support

        // 对于 ItemStack 类，我们单独处理
        if ("ItemStack".equals(nmsClassName)) {
            String newClazzName = "net.minecraft.world.item.ItemStack";
            Class<?> clazz = tryLoadClass(newClazzName);
            if (clazz != null) {
                loadedNMSClasses.put(nmsClassName, clazz);
                return clazz;
            }
        } else if ("NBTTagCompound".equals(nmsClassName)) {
            String newClazzName = "net.minecraft.nbt.NBTTagCompound";
            Class<?> clazz = tryLoadClass(newClazzName);
            if (clazz != null) {
                loadedNMSClasses.put(nmsClassName, clazz);
                return clazz;
            }
        } else if ("Item".equals(nmsClassName)) {
            String newClazzName = "net.minecraft.world.item.Item";
            Class<?> clazz = tryLoadClass(newClazzName);
            if (clazz != null) {
                loadedNMSClasses.put(nmsClassName, clazz);
                return clazz;
            }
        }

        // 尝试旧版本的包路径
        String oldClazzName = "net.minecraft.server." + getVersion() + nmsClassName;
        Class<?> clazz = tryLoadClass(oldClazzName);

        if (clazz == null) {
            // 如果旧路径失败，尝试新路径模式
            String newClazzName = getNewNMSPath(nmsClassName);
            clazz = tryLoadClass(newClazzName);
        }

        loadedNMSClasses.put(nmsClassName, clazz);
        return clazz;
    }

    /**
     * 尝试加载类并处理异常
     */
    private static Class<?> tryLoadClass(String className) {
        try {
            return Class.forName(className);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 根据类名获取新版本的 NMS 路径
     */
    private static String getNewNMSPath(String nmsClassName) {
        // 根据常见的类名映射到新路径
        switch (nmsClassName) {
            case "ItemStack":
                return "net.minecraft.world.item.ItemStack";
            case "NBTTagCompound":
                return "net.minecraft.nbt.NBTTagCompound";
            case "NBTTagList":
                return "net.minecraft.nbt.NBTTagList";
            case "MinecraftServer":
                return "net.minecraft.server.MinecraftServer";
            case "WorldServer":
                return "net.minecraft.server.level.WorldServer";
            case "EntityPlayer":
                return "net.minecraft.server.level.EntityPlayer";
            case "Item":
                return "net.minecraft.world.item.Item";
            case "Blocks":
                return "net.minecraft.world.level.block.Blocks";
            default:
                // 对于未知类，按优先级尝试几种可能的路径
                // 为了避免频繁的 ClassNotFoundException，我们预先定义常见类的映射
                // 如果都不匹配，返回默认路径
                String[] prefixes = {
                    "net.minecraft.world.item.",
                    "net.minecraft.nbt.",
                    "net.minecraft.server.level.",
                    "net.minecraft.world.level.",
                    "net.minecraft.core."
                };

                // 为了性能考虑，我们只在第一次查找时尝试多种路径，并缓存结果
                for (String prefix : prefixes) {
                    try {
                        Class.forName(prefix + nmsClassName);
                        return prefix + nmsClassName;
                    } catch (ClassNotFoundException e) {
                        // 继续尝试下一个前缀
                    }
                }

                // 如果都找不到，返回一个可能的路径
                return "net.minecraft.world.item." + nmsClassName; // 默认假设在 item 包下
        }
    }

    /**
     * Get a class from the org.bukkit.craftbukkit package
     *
     * @param obcClassName the path to the class
     * @return the found class at the specified path
     */
    public synchronized static Class<?> getOBCClass(String obcClassName) {
        if (loadedOBCClasses.containsKey(obcClassName)) {
            return loadedOBCClasses.get(obcClassName);
        }

        String clazzName = "org.bukkit.craftbukkit." + getVersion() + obcClassName;
        Class<?> clazz;

        try {
            clazz = Class.forName(clazzName);
        } catch (Throwable t) {
            t.printStackTrace();
            loadedOBCClasses.put(obcClassName, null);
            return null;
        }

        loadedOBCClasses.put(obcClassName, clazz);
        return clazz;
    }

    /**
     * Get a Bukkit {@link Player} players NMS playerConnection object
     *
     * @param player The player
     * @return The players connection
     */
    public static Object getConnection(Player player) {
        Method getHandleMethod = getMethod(player.getClass(), "getHandle");

        if (getHandleMethod != null) {
            try {
                Object nmsPlayer = getHandleMethod.invoke(player);
                Field playerConField = getField(nmsPlayer.getClass(), "playerConnection");
                return playerConField.get(nmsPlayer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Get a classes constructor
     *
     * @param clazz  The constructor class
     * @param params The parameters in the constructor
     * @return The constructor object
     */
    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... params) {
        try {
            return clazz.getConstructor(params);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Get a method from a class that has the specific paramaters
     *
     * @param clazz      The class we are searching
     * @param methodName The name of the method
     * @param params     Any parameters that the method has
     * @return The method with appropriate paramaters
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... params) {
        if (!loadedMethods.containsKey(clazz)) {
            loadedMethods.put(clazz, new HashMap<String, Method>());
        }

        Map<String, Method> methods = loadedMethods.get(clazz);

        if (methods.containsKey(methodName)) {
            return methods.get(methodName);
        }

        try {
            Method method = clazz.getMethod(methodName, params);
            methods.put(methodName, method);
            loadedMethods.put(clazz, methods);
            return method;
        } catch (Exception e) {
            e.printStackTrace();
            methods.put(methodName, null);
            loadedMethods.put(clazz, methods);
            return null;
        }
    }

    /**
     * Get a field with a particular name from a class
     *
     * @param clazz     The class
     * @param fieldName The name of the field
     * @return The field object
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        if (!loadedFields.containsKey(clazz)) {
            loadedFields.put(clazz, new HashMap<String, Field>());
        }

        Map<String, Field> fields = loadedFields.get(clazz);

        if (fields.containsKey(fieldName)) {
            return fields.get(fieldName);
        }

        try {
            Field field = clazz.getField(fieldName);
            fields.put(fieldName, field);
            loadedFields.put(clazz, fields);
            return field;
        } catch (Exception e) {
            e.printStackTrace();
            fields.put(fieldName, null);
            loadedFields.put(clazz, fields);
            return null;
        }
    }
}
