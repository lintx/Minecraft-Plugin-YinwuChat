package org.lintx.plugins.yinwuchat.Util;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * 现代化物品工具类
 * 支持 Minecraft 1.13 - 1.21.1+ 的物品序列化和展示
 * 
 * 版本兼容性:
 * - 1.20.5+: 使用新的 Data Components 系统和 HoverEvent.showItem(Item) API
 * - 1.13-1.20.4: 使用传统的 NBT 序列化方法
 */
public class ModernItemUtil {
    
    // 版本检测缓存
    private static Boolean supportsDataComponents = null;
    private static Boolean supportsNewHoverEvent = null;
    private static int serverVersion = -1;
    
    // Paper UnsafeValues 缓存
    private static Object cachedUnsafeValues = null;
    private static Method cachedSerializeItemMethod = null;
    
    // 1.21+ 使用的新方法缓存 (预留扩展)
    @SuppressWarnings("unused")
    private static Method cachedGetItemDataMethod = null;
    @SuppressWarnings("unused") 
    private static Method cachedAsHoverEventMethod = null;
    
    /**
     * 获取服务器主版本号 (例如: 1.20 返回 20, 1.21 返回 21)
     */
    private static int getServerVersion() {
        if (serverVersion == -1) {
            try {
                String version = Bukkit.getBukkitVersion();
                // 格式如: 1.21.1-R0.1-SNAPSHOT
                String[] parts = version.split("-")[0].split("\\.");
                if (parts.length >= 2) {
                    serverVersion = Integer.parseInt(parts[1]);
                } else {
                    serverVersion = 0;
                }
            } catch (Exception e) {
                serverVersion = 0;
            }
        }
        return serverVersion;
    }
    
    /**
     * 检查是否支持 Data Components (1.20.5+)
     */
    private static boolean supportsDataComponents() {
        if (supportsDataComponents != null) {
            return supportsDataComponents;
        }
        
        int version = getServerVersion();
        if (version < 20) {
            supportsDataComponents = false;
            return false;
        }
        
        // 1.20.5+ 才支持 Data Components
        if (version == 20) {
            // 检查是否是 1.20.5+
            try {
                String fullVersion = Bukkit.getBukkitVersion();
                String[] parts = fullVersion.split("-")[0].split("\\.");
                if (parts.length >= 3) {
                    int patch = Integer.parseInt(parts[2]);
                    supportsDataComponents = patch >= 5;
                } else {
                    supportsDataComponents = false;
                }
            } catch (Exception e) {
                supportsDataComponents = false;
            }
        } else {
            // 1.21+ 肯定支持
            supportsDataComponents = true;
        }
        
        return supportsDataComponents;
    }
    
    /**
     * 检查是否支持新的 HoverEvent API (BungeeCord-Chat 1.20+)
     */
    private static boolean supportsNewHoverEvent() {
        if (supportsNewHoverEvent != null) {
            return supportsNewHoverEvent;
        }
        
        try {
            // 检查是否存在 Item 类和新的 HoverEvent.showItem 方法
            Class.forName("net.md_5.bungee.api.chat.hover.content.Item");
            supportsNewHoverEvent = true;
        } catch (ClassNotFoundException e) {
            supportsNewHoverEvent = false;
        }
        
        return supportsNewHoverEvent;
    }
    
    /**
     * 检查是否支持 Paper 的现代序列化方法
     */
    private static boolean supportsModernSerialization() {
        if (!supportsDataComponents()) {
            return false;
        }
        
        if (cachedUnsafeValues != null && cachedSerializeItemMethod != null) {
            return true;
        }

        try {
            Object server = Bukkit.getServer();
            if (server != null) {
                Method getUnsafeValuesMethod = server.getClass().getMethod("getUnsafe");
                Object unsafeValues = getUnsafeValuesMethod.invoke(server);

                // 尝试获取 serializeItem 或 serializeItemAsBytes 方法
                Method serializeItemMethod = null;
                try {
                    serializeItemMethod = unsafeValues.getClass().getMethod("serializeItem", ItemStack.class);
                } catch (NoSuchMethodException e) {
                    // 1.21+ 可能使用不同的方法名
                    try {
                        serializeItemMethod = unsafeValues.getClass().getMethod("serializeItemAsBytes", ItemStack.class);
                    } catch (NoSuchMethodException ignored) {}
                }

                if (serializeItemMethod != null) {
                    cachedUnsafeValues = unsafeValues;
                    cachedSerializeItemMethod = serializeItemMethod;
                    return true;
                }
            }
        } catch (Exception ignored) {}

        return false;
    }
    
    /**
     * 将 ItemStack 转换为 JSON 字符串（兼容新旧版本）
     */
    private static String convertItemToJson(ItemStack itemStack) {
        // 优先使用现代方法
        if (supportsModernSerialization()) {
            String result = convertItemToJsonModern(itemStack);
            if (result != null) {
                return result;
            }
        }
        
        // 回退到传统方法
            return convertItemToJsonLegacy(itemStack);
    }
    
    /**
     * 现代版本的物品序列化方法 (1.20.5+)
     */
    private static String convertItemToJsonModern(ItemStack itemStack) {
        try {
            if (cachedSerializeItemMethod != null && cachedUnsafeValues != null) {
                Object serializedItem = cachedSerializeItemMethod.invoke(cachedUnsafeValues, itemStack);
                if (serializedItem instanceof byte[]) {
                    // 如果返回的是字节数组，需要转换
                    return new String((byte[]) serializedItem, java.nio.charset.StandardCharsets.UTF_8);
                }
                return serializedItem.toString();
            }
            return null;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to serialize itemstack using modern method", e);
            return null;
        }
    }
    
    /**
     * 传统版本的物品序列化方法 (1.13-1.20.4)
     */
    private static String convertItemToJsonLegacy(ItemStack itemStack) {
        Class<?> craftItemStackClazz = ReflectionUtil.getOBCClass("inventory.CraftItemStack");
        if (craftItemStackClazz == null) {
            return createFallbackItemJson(itemStack);
        }
        
        Method asNMSCopyMethod = ReflectionUtil.getMethod(craftItemStackClazz, "asNMSCopy", ItemStack.class);
        if (asNMSCopyMethod == null) {
            return createFallbackItemJson(itemStack);
        }

        Class<?> nmsItemStackClazz = ReflectionUtil.getNMSClass("ItemStack");
        if (nmsItemStackClazz == null) {
            return createFallbackItemJson(itemStack);
        }
        
        Class<?> nbtTagCompoundClazz = ReflectionUtil.getNMSClass("NBTTagCompound");
        if (nbtTagCompoundClazz == null) {
            return createFallbackItemJson(itemStack);
        }
        
        // 尝试多种方法名
        Method saveNmsItemStackMethod = ReflectionUtil.getMethod(nmsItemStackClazz, "save", nbtTagCompoundClazz);
        if (saveNmsItemStackMethod == null) {
            saveNmsItemStackMethod = ReflectionUtil.getMethod(nmsItemStackClazz, "b", nbtTagCompoundClazz);
        }
        if (saveNmsItemStackMethod == null) {
            return createFallbackItemJson(itemStack);
        }

        try {
            Object nmsNbtTagCompoundObj = nbtTagCompoundClazz.getDeclaredConstructor().newInstance();
            Object nmsItemStackObj = asNMSCopyMethod.invoke(null, itemStack);
            Object itemAsJsonObject = saveNmsItemStackMethod.invoke(nmsItemStackObj, nmsNbtTagCompoundObj);
            return itemAsJsonObject.toString();
        } catch (Throwable t) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to serialize itemstack using legacy method", t);
            return createFallbackItemJson(itemStack);
        }
    }
    
    /**
     * 创建备用的物品 JSON（当其他方法都失败时）
     */
    private static String createFallbackItemJson(ItemStack itemStack) {
        // 返回最基本的物品信息
        String itemId = getItemId(itemStack);
        int count = itemStack.getAmount();
        return String.format("{\"id\":\"%s\",\"count\":%d}", itemId, count);
    }

    /**
     * 获取物品的显示名称
     */
    private static BaseComponent getItemComponent(ItemStack itemStack) {
        TextComponent component = new TextComponent();
        if (itemStack.hasItemMeta()) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta.hasDisplayName()) {
                TextComponent textComponent = new TextComponent(itemMeta.getDisplayName());
                component.addExtra(textComponent);
                return component;
            }
        }
        
        // 使用翻译键作为显示名称（更好的本地化支持）
        String translationKey = getTranslationKey(itemStack);
        if (translationKey != null) {
            net.md_5.bungee.api.chat.TranslatableComponent transComponent = 
                new net.md_5.bungee.api.chat.TranslatableComponent(translationKey);
            component.addExtra(transComponent);
            return component;
        }
        
        // 使用 Material 名称作为最终备选
        String materialName = formatMaterialName(itemStack.getType().name());
        TextComponent textComponent = new TextComponent(materialName);
        component.addExtra(textComponent);
        return component;
    }

    /**
     * 获取物品的翻译键
     */
    private static String getTranslationKey(ItemStack itemStack) {
        // 药水类物品需要特殊处理：翻译键由容器类型+药水效果组合而成
        if (isPotionItem(itemStack.getType()) && itemStack.hasItemMeta() && itemStack.getItemMeta() instanceof PotionMeta) {
            String potionKey = getPotionTranslationKey(itemStack);
            if (potionKey != null) {
                return potionKey;
            }
        }

        try {
            Method getTranslationKeyMethod = itemStack.getClass().getMethod("translationKey");
            Object key = getTranslationKeyMethod.invoke(itemStack);
            return key.toString();
        } catch (Exception e1) {
            try {
                if (itemStack.hasItemMeta()) {
                    ItemMeta meta = itemStack.getItemMeta();
                    Method getTransKeyMethod = meta.getClass().getMethod("getLocalizedName");
                    Object localized = getTransKeyMethod.invoke(meta);
                    if (localized != null && !localized.toString().isEmpty()) {
                        return localized.toString();
                    }
                }
            } catch (Exception ignored) {}
            
            String key = getItemKeyName(itemStack.getType());
            if (itemStack.getType().isBlock()) {
                return "block.minecraft." + key;
            } else {
                return "item.minecraft." + key;
            }
        }
    }

    private static boolean isPotionItem(Material material) {
        String name = material.name();
        return name.equals("POTION") || name.equals("SPLASH_POTION")
                || name.equals("LINGERING_POTION") || name.equals("TIPPED_ARROW");
    }

    /**
     * 构造药水的完整翻译键，格式: item.minecraft.<容器类型>.effect.<药水基础ID>
     * Minecraft 语言文件中 strong_/long_ 变体没有独立词条，翻译键统一使用基础 ID。
     * 例: 滞留型剧毒药水 II 的翻译键为 item.minecraft.lingering_potion.effect.poison
     */
    private static String getPotionTranslationKey(ItemStack itemStack) {
        PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();
        String materialKey = getItemKeyName(itemStack.getType());

        // 1.20.5+ API: PotionMeta.getBasePotionType() → PotionType.getKey()
        try {
            Method getBasePotionType = potionMeta.getClass().getMethod("getBasePotionType");
            Object potionType = getBasePotionType.invoke(potionMeta);
            if (potionType != null) {
                Method getKey = potionType.getClass().getMethod("getKey");
                Object nsKey = getKey.invoke(potionType);
                Method getKeyName = nsKey.getClass().getMethod("getKey");
                String potionId = getKeyName.invoke(nsKey).toString();
                if (potionId != null && !potionId.isEmpty()) {
                    return "item.minecraft." + materialKey + ".effect." + stripPotionVariantPrefix(potionId);
                }
            }
        } catch (Exception ignored) {}

        // Legacy API: PotionMeta.getBasePotionData()
        try {
            PotionData base = potionMeta.getBasePotionData();
            if (base != null && base.getType() != null) {
                String potionId = mapLegacyPotionType(base);
                if (potionId != null) {
                    return "item.minecraft." + materialKey + ".effect." + stripPotionVariantPrefix(potionId);
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * 去掉 strong_ / long_ 前缀，返回基础药水 ID。
     * Minecraft 语言文件中 strong_poison / long_poison 等变体没有独立翻译键。
     */
    private static String stripPotionVariantPrefix(String potionId) {
        if (potionId == null) return null;
        if (potionId.startsWith("strong_")) return potionId.substring("strong_".length());
        if (potionId.startsWith("long_")) return potionId.substring("long_".length());
        return potionId;
    }

    /**
     * 将旧版 PotionData 映射为 Minecraft 药水注册 ID
     */
    @SuppressWarnings("deprecation")
    private static String mapLegacyPotionType(PotionData data) {
        String base;
        switch (data.getType().name()) {
            case "NIGHT_VISION":   base = "night_vision"; break;
            case "INVISIBILITY":   base = "invisibility"; break;
            case "JUMP":           base = "leaping"; break;
            case "FIRE_RESISTANCE":base = "fire_resistance"; break;
            case "SPEED":          base = "swiftness"; break;
            case "SLOWNESS":       base = "slowness"; break;
            case "WATER_BREATHING":base = "water_breathing"; break;
            case "INSTANT_HEAL":   base = "healing"; break;
            case "INSTANT_DAMAGE": base = "harming"; break;
            case "POISON":         base = "poison"; break;
            case "REGEN":          base = "regeneration"; break;
            case "STRENGTH":       base = "strength"; break;
            case "WEAKNESS":       base = "weakness"; break;
            case "LUCK":           base = "luck"; break;
            case "TURTLE_MASTER":  base = "turtle_master"; break;
            case "SLOW_FALLING":   base = "slow_falling"; break;
            case "MUNDANE":        base = "mundane"; break;
            case "THICK":          base = "thick"; break;
            case "AWKWARD":        base = "awkward"; break;
            case "WATER":          base = "water"; break;
            default:               base = data.getType().name().toLowerCase(); break;
        }
        if (data.isUpgraded()) {
            return "strong_" + base;
        } else if (data.isExtended()) {
            return "long_" + base;
        }
        return base;
    }
    
    /**
     * 获取 Material 的 key 名称（兼容新旧API）
     */
    private static String getItemKeyName(Material material) {
        try {
            Method getKeyMethod = material.getClass().getMethod("getKey");
            Object key = getKeyMethod.invoke(material);
            Method getKeyNameMethod = key.getClass().getMethod("getKey");
            return getKeyNameMethod.invoke(key).toString();
        } catch (Exception e) {
            return material.name().toLowerCase();
        }
    }
    
    /**
     * 格式化 Material 名称（将下划线转换为空格，首字母大写）
     */
    private static String formatMaterialName(String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (result.length() > 0) result.append(" ");
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
            }
        }
        return result.toString();
    }

    /**
     * 创建带有悬停效果的物品组件（支持 1.13-1.21.1+）
     */
    public static BaseComponent componentWithPlayer(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().equals(Material.AIR)) {
            return null;
        }
        
        ItemStack item = itemStack.clone();
        
        // 处理书本 - 清空页面内容以减少数据量
        try {
            if (item.getType().equals(Material.WRITABLE_BOOK) || item.getType().equals(Material.WRITTEN_BOOK)) {
                BookMeta bm = (BookMeta) item.getItemMeta();
                bm.setPages(Collections.emptyList());
                item.setItemMeta(bm);
            }
        } catch (Exception | Error ignored) {}
        
        // 处理潜影盒 - 简化内容
        try {
            if (isShulkerBox(item.getType())) {
                if (item.hasItemMeta()) {
                    BlockStateMeta bsm = (BlockStateMeta) item.getItemMeta();
                    if (bsm.hasBlockState()) {
                        ShulkerBox sb = (ShulkerBox) bsm.getBlockState();
                        for (ItemStack i : sb.getInventory()) {
                            if (i == null || i.getType().equals(Material.AIR) || !i.hasItemMeta()) {
                                continue;
                            }
                            ItemMeta im = Bukkit.getItemFactory().getItemMeta(i.getType());
                            ItemMeta original = i.getItemMeta();
                            if (original != null && original.hasDisplayName()) {
                                im.setDisplayName(original.getDisplayName());
                            }
                            i.setItemMeta(im);
                        }
                        bsm.setBlockState(sb);
                    }
                    item.setItemMeta(bsm);
                }
            }
        } catch (Exception | Error ignored) {}

        // 构建显示文本
        TextComponent component = new TextComponent("");
        component.addExtra("§r§7[§r");
        
        try {
            component.addExtra(getItemComponent(item));
        } catch (Exception | Error e) {
            component.addExtra(formatMaterialName(item.getType().name()));
        }
        
        if (item.getAmount() > 1) {
            component.addExtra(" x" + item.getAmount());
        }
        component.addExtra("§r§7]§r");

        // 设置悬停事件
        setItemHoverEvent(component, itemStack);

        return component;
    }
    
    /**
     * 检查是否是潜影盒
     */
    private static boolean isShulkerBox(Material material) {
        String name = material.name();
        return name.endsWith("SHULKER_BOX");
    }
    
    /**
     * 获取物品ID（兼容新旧API）
     */
    private static String getItemId(ItemStack itemStack) {
        try {
            // 尝试使用新API
            Material type = itemStack.getType();
            Method getKeyMethod = type.getClass().getMethod("getKey");
            Object key = getKeyMethod.invoke(type);
            return key.toString();
        } catch (Exception e) {
            // 回退到Material名称
            return "minecraft:" + itemStack.getType().name().toLowerCase();
        }
    }
    
    /**
     * 设置物品悬停事件（兼容多个版本）
     */
    private static void setItemHoverEvent(TextComponent component, ItemStack itemStack) {
        // 方法1: 尝试使用新的 Content-based HoverEvent API (BungeeCord-Chat 1.16+)
        if (supportsNewHoverEvent()) {
            try {
                String itemId = getItemId(itemStack);
                int count = itemStack.getAmount();
                
                // 获取物品的 NBT/组件数据
                String itemTag = null;
                String itemJson = convertItemToJson(itemStack);
                if (itemJson != null && !itemJson.isEmpty()) {
                    // 提取 tag 或 components 部分
                    itemTag = extractItemTag(itemJson);
                }
                
                // 使用反射创建 Item 和 ItemTag，因为这些类可能不存在于旧版本
                Class<?> itemClass = Class.forName("net.md_5.bungee.api.chat.hover.content.Item");
                Object itemContent;
                
                if (itemTag != null && !itemTag.isEmpty()) {
                    Class<?> itemTagClass = Class.forName("net.md_5.bungee.api.chat.hover.content.ItemTag");
                    Method ofNbtMethod = itemTagClass.getMethod("ofNbt", String.class);
                    Object itemTagObj = ofNbtMethod.invoke(null, itemTag);
                    
                    java.lang.reflect.Constructor<?> itemConstructor = itemClass.getConstructor(String.class, int.class, itemTagClass);
                    itemContent = itemConstructor.newInstance(itemId, count, itemTagObj);
                } else {
                    java.lang.reflect.Constructor<?> itemConstructor = itemClass.getConstructor(String.class, int.class);
                    itemContent = itemConstructor.newInstance(itemId, count);
                }
                
                // 创建 HoverEvent
                Class<?> contentClass = Class.forName("net.md_5.bungee.api.chat.hover.content.Content");
                java.lang.reflect.Constructor<HoverEvent> hoverConstructor = 
                    HoverEvent.class.getConstructor(HoverEvent.Action.class, contentClass.arrayType());
                Object contentArray = java.lang.reflect.Array.newInstance(contentClass, 1);
                java.lang.reflect.Array.set(contentArray, 0, itemContent);
                HoverEvent hoverEvent = hoverConstructor.newInstance(HoverEvent.Action.SHOW_ITEM, contentArray);
                
                component.setHoverEvent(hoverEvent);
                return;
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.FINE, "Failed to use new HoverEvent API, falling back", e);
            }
        }
        
        // 方法2: 使用传统的 JsonArray-based HoverEvent
        try {
            String itemJson = convertItemToJson(itemStack);
            if (itemJson == null || itemJson.isEmpty()) {
                return;
            }
            
            // 使用反射创建旧版 HoverEvent
            com.google.gson.JsonArray jsonArray = new com.google.gson.JsonArray();
            jsonArray.add(itemJson);
            
            java.lang.reflect.Constructor<HoverEvent> constructor = 
                HoverEvent.class.getDeclaredConstructor(HoverEvent.Action.class, com.google.gson.JsonArray.class);
            HoverEvent event = constructor.newInstance(HoverEvent.Action.SHOW_ITEM, jsonArray);
            component.setHoverEvent(event);
        } catch (Exception e) {
            // 最终回退: 使用文本悬停显示物品信息
            try {
                String displayText = getItemDisplayText(itemStack);
                TextComponent hoverText = new TextComponent(displayText);
                // 使用反射创建 Text Content（如果可用）
                try {
                    Class<?> textClass = Class.forName("net.md_5.bungee.api.chat.hover.content.Text");
                    java.lang.reflect.Constructor<?> textConstructor = textClass.getConstructor(BaseComponent[].class);
                    Object textContent = textConstructor.newInstance((Object) new BaseComponent[]{hoverText});
                    
                    Class<?> contentClass = Class.forName("net.md_5.bungee.api.chat.hover.content.Content");
                    java.lang.reflect.Constructor<HoverEvent> hoverConstructor = 
                        HoverEvent.class.getConstructor(HoverEvent.Action.class, contentClass.arrayType());
                    Object contentArray = java.lang.reflect.Array.newInstance(contentClass, 1);
                    java.lang.reflect.Array.set(contentArray, 0, textContent);
                    HoverEvent hoverEvent = hoverConstructor.newInstance(HoverEvent.Action.SHOW_TEXT, contentArray);
                    component.setHoverEvent(hoverEvent);
                } catch (Exception e2) {
                    // 使用旧版 API
                    BaseComponent[] hoverComponents = new BaseComponent[]{hoverText};
                    @SuppressWarnings("deprecation")
                    HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponents);
                    component.setHoverEvent(event);
                }
            } catch (Exception ignored) {}
        }
    }
    
    /**
     * 从物品 JSON 中提取 tag/components 数据
     */
    private static String extractItemTag(String itemJson) {
        if (itemJson == null || itemJson.isEmpty()) {
            return null;
        }
        try {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(itemJson).getAsJsonObject();
            
            // 1.20.5+ 使用 "components"
            if (json.has("components")) {
                return json.get("components").toString();
            }
            
            // 1.20.4 及更早版本使用 "tag"
            if (json.has("tag")) {
                return json.get("tag").toString();
            }
            
            // 如果没有额外数据，返回整个 JSON（除了基本字段）
            json.remove("id");
            json.remove("Count");
            json.remove("count");
            if (json.size() > 0) {
                return json.toString();
            }
        } catch (Exception ignored) {
            // JSON 解析失败时，尝试从 SNBT 中提取 tag/components
            return extractItemTagFromSnbt(itemJson);
        }
        
        return null;
    }

    /**
     * 从 SNBT 字符串中提取 tag/components 数据
     */
    private static String extractItemTagFromSnbt(String snbt) {
        String key = null;
        int componentsIndex = snbt.indexOf("components:");
        int tagIndex = snbt.indexOf("tag:");
        if (componentsIndex >= 0 && (tagIndex < 0 || componentsIndex < tagIndex)) {
            key = "components:";
        } else if (tagIndex >= 0) {
            key = "tag:";
        }
        if (key == null) {
            return null;
        }
        int start = snbt.indexOf('{', snbt.indexOf(key));
        if (start < 0) {
            return null;
        }
        int depth = 0;
        for (int i = start; i < snbt.length(); i++) {
            char c = snbt.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return snbt.substring(start, i + 1);
                }
            }
        }
        return null;
    }
    
    /**
     * 获取物品的显示文本（用于文本悬停备选方案）
     */
    private static String getItemDisplayText(ItemStack itemStack) {
        StringBuilder sb = new StringBuilder();
        
        // 物品名称
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
            sb.append(itemStack.getItemMeta().getDisplayName());
        } else {
            sb.append(formatMaterialName(itemStack.getType().name()));
        }
        
        // 数量
        if (itemStack.getAmount() > 1) {
            sb.append(" x").append(itemStack.getAmount());
        }
        
        // Lore
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasLore()) {
            List<String> lore = itemStack.getItemMeta().getLore();
            if (lore != null) {
                for (String line : lore) {
                    sb.append("\n").append(line);
                }
            }
        }
        
        return sb.toString();
    }

    /**
     * 获取物品的 JSON 表示（用于序列化传输）
     */
    public static String itemJsonWithPlayer(ItemStack itemStack) {
        BaseComponent component = componentWithPlayer(itemStack);
        if (component == null) return null;
        return ComponentSerializer.toString(component);
    }
    
    /**
     * 获取物品的简化数据（用于跨服传输）
     * 包含: id, count, displayName, lore, enchantments, nbt, fullItemData
     */
    public static String getItemDataForTransfer(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().equals(Material.AIR)) {
            return null;
        }
        
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        
        // 基本信息
        json.addProperty("id", getItemId(itemStack));
        json.addProperty("count", itemStack.getAmount());
        
        // 显示名称
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
            json.addProperty("displayName", itemStack.getItemMeta().getDisplayName());
        }
        
        // Lore
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasLore()) {
            com.google.gson.JsonArray loreArray = new com.google.gson.JsonArray();
            List<String> lore = itemStack.getItemMeta().getLore();
            if (lore != null) {
                for (String line : lore) {
                    loreArray.add(line);
                }
            }
            json.add("lore", loreArray);
        }
        
        // 附魔（包含普通附魔 + 附魔书存储附魔）
        com.google.gson.JsonObject enchants = new com.google.gson.JsonObject();
        boolean hasEnchantments = false;

        if (!itemStack.getEnchantments().isEmpty()) {
            itemStack.getEnchantments().forEach((ench, level) -> {
                String enchKey = normalizeEnchantmentKey(ench);
                enchants.addProperty(enchKey, level);
            });
            hasEnchantments = true;
        }

        if (itemStack.hasItemMeta() && itemStack.getItemMeta() instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta storageMeta = (EnchantmentStorageMeta) itemStack.getItemMeta();
            if (!storageMeta.getStoredEnchants().isEmpty()) {
                storageMeta.getStoredEnchants().forEach((ench, level) -> {
                    String enchKey = normalizeEnchantmentKey(ench);
                    enchants.addProperty(enchKey, level);
                });
                hasEnchantments = true;
            }
        }

        if (hasEnchantments) {
            json.add("enchantments", enchants);
        }

        // 药水信息（包含基础药水类型与自定义效果，支持多效果）
        if (itemStack.hasItemMeta() && itemStack.getItemMeta() instanceof PotionMeta) {
            PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();

            boolean potionTypeResolved = false;

            // 1.20.5+ API: PotionMeta.getBasePotionType()
            try {
                Method getBasePotionType = potionMeta.getClass().getMethod("getBasePotionType");
                Object ptObj = getBasePotionType.invoke(potionMeta);
                if (ptObj != null) {
                    Method getKey = ptObj.getClass().getMethod("getKey");
                    Object nsKey = getKey.invoke(ptObj);
                    Method getKeyName = nsKey.getClass().getMethod("getKey");
                    String potionId = getKeyName.invoke(nsKey).toString();
                    if (potionId != null && !potionId.isEmpty()) {
                        json.addProperty("potionType", potionId);
                        potionTypeResolved = true;
                    }
                }
            } catch (Exception ignored) {}

            // Legacy API 兜底
            if (!potionTypeResolved) {
                try {
                    PotionData base = potionMeta.getBasePotionData();
                    if (base != null && base.getType() != null) {
                        String potionType = mapLegacyPotionType(base);
                        if (potionType != null) {
                            json.addProperty("potionType", potionType);
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (potionMeta.hasCustomEffects()) {
                com.google.gson.JsonArray effects = new com.google.gson.JsonArray();
                for (PotionEffect effect : potionMeta.getCustomEffects()) {
                    com.google.gson.JsonObject effectObj = new com.google.gson.JsonObject();
                    String effectKey;
                    try {
                        Method getKeyMethod = effect.getType().getClass().getMethod("getKey");
                        effectKey = String.valueOf(getKeyMethod.invoke(effect.getType()));
                        if (effectKey.startsWith("minecraft:")) {
                            effectKey = effectKey.substring("minecraft:".length());
                        }
                    } catch (Exception e) {
                        effectKey = effect.getType().getName().toLowerCase();
                    }
                    effectObj.addProperty("type", effectKey);
                    effectObj.addProperty("amplifier", effect.getAmplifier());
                    effectObj.addProperty("duration", effect.getDuration());
                    effects.add(effectObj);
                }
                if (effects.size() > 0) {
                    json.add("potionEffects", effects);
                }
            }
        }
        
        // 尝试添加 NBT/组件数据（用于原版 Hover）
        String fullJson = convertItemToJson(itemStack);
        String itemTag = extractItemTag(fullJson);
        if (itemTag != null && !itemTag.isEmpty()) {
            json.addProperty("nbt", itemTag);
        }
        
        // 添加完整的序列化数据（用于跨服完整恢复物品，支持插件自定义物品）
        String fullItemData = serializeItemFully(itemStack);
        if (fullItemData != null && !fullItemData.isEmpty()) {
            json.addProperty("fullItemData", fullItemData);
        }
        
        return json.toString();
    }
    
    /**
     * 完整序列化物品（包含所有 NBT 数据）
     * 使用 Bukkit 的序列化 API，确保插件自定义物品的完整性
     */
    private static String serializeItemFully(ItemStack itemStack) {
        try {
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            org.bukkit.util.io.BukkitObjectOutputStream dataOutput = new org.bukkit.util.io.BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(itemStack);
            dataOutput.close();
            return java.util.Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.FINE, "Failed to fully serialize item", e);
            return null;
        }
    }

    private static String normalizeEnchantmentKey(org.bukkit.enchantments.Enchantment ench) {
        String enchKey;
        try {
            Method getKeyMethod = ench.getClass().getMethod("getKey");
            enchKey = getKeyMethod.invoke(ench).toString();
            if (enchKey.startsWith("minecraft:")) {
                enchKey = enchKey.substring(10);
            }
        } catch (Exception e) {
            enchKey = ench.toString().toLowerCase().replace("enchantment{", "").replace("}", "");
            if (enchKey.contains("minecraft:")) {
                enchKey = enchKey.substring(enchKey.lastIndexOf(":") + 1);
            }
        }
        return enchKey;
    }
}