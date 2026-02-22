package org.lintx.plugins.yinwuchat.velocity.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Velocity 物品工具类
 * 用于处理物品信息的显示和悬停效果
 * 参考 InteractiveChat 的实现方式
 */
public class VelocityItemUtil {
    private static final Map<String, String> LANG_ZH_CN = loadZhLangMap();
    private static final Map<String, String> ITEM_NAME_ZH_FALLBACK = createItemNameZhFallbackMap();

    /**
     * 创建物品显示组件
     * @param itemJson 物品的 JSON 字符串
     * @param itemName 物品名称
     * @param amount 物品数量
     * @return 带有悬停效果和点击事件的 Component
     */
    public static Component createItemComponent(String itemJson, String itemName, int amount) {
        // 创建基础的物品文本
        Component itemText = Component.text("[")
            .color(NamedTextColor.GRAY)
            .append(Component.text(itemName)
                .color(NamedTextColor.WHITE))
            .append(Component.text("]"))
            .color(NamedTextColor.GRAY);

        // 如果数量大于1，添加数量显示
        if (amount > 1) {
            itemText = itemText.append(Component.text(" x" + amount)
                .color(NamedTextColor.YELLOW));
        }

        // 始终使用服务端文本悬停，确保中英文显示统一且可控（不依赖客户端语言包）
        Component hoverText = createItemHoverText(itemJson, itemName, amount);
        itemText = itemText.hoverEvent(HoverEvent.showText(hoverText));
        
        // 尝试提取物品展示ID并添加点击事件
        String displayId = extractDisplayId(itemJson);
        if (displayId != null && !displayId.isEmpty()) {
            // 添加点击事件 - 执行 /viewitem 命令来查看物品
            itemText = itemText.clickEvent(ClickEvent.runCommand("/viewitem " + displayId));
        }
        
        return itemText;
    }
    
    /**
     * 从物品 JSON 中提取展示ID
     * @param itemJson 物品的 JSON 字符串
     * @return 展示ID，如果不存在则返回 null
     */
    private static String extractDisplayId(String itemJson) {
        if (itemJson == null || itemJson.isEmpty()) {
            return null;
        }
        
        try {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(itemJson).getAsJsonObject();
            if (json.has("displayId")) {
                return json.get("displayId").getAsString();
            }
        } catch (Exception e) {
            // 解析失败，返回 null
        }
        
        return null;
    }

    /**
     * 创建物品悬停文本
     * 参考 InteractiveChat 的悬停信息显示方式
     * 支持解析 ModernItemUtil.getItemDataForTransfer 返回的 JSON 格式
     */
    private static Component createItemHoverText(String itemJson, String itemName, int amount) {
        // 先尝试从 JSON 中获取自定义名称
        String customDisplayName = null;
        java.util.List<String> loreLines = null;
        String enchantmentsText = null;
        java.util.List<String> potionEffectLabels = java.util.Collections.emptyList();
        
        if (itemJson != null && !itemJson.isEmpty()) {
            try {
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(itemJson).getAsJsonObject();
                
                // 提取自定义显示名称
                if (json.has("displayName")) {
                    customDisplayName = json.get("displayName").getAsString();
                }
                
                // 提取 Lore
                if (json.has("lore") && json.get("lore").isJsonArray()) {
                    loreLines = new java.util.ArrayList<>();
                    com.google.gson.JsonArray loreArray = json.getAsJsonArray("lore");
                    for (int i = 0; i < loreArray.size(); i++) {
                        loreLines.add(loreArray.get(i).getAsString());
                    }
                }
                
                // 提取附魔
                enchantmentsText = parseEnchantments(itemJson);
                potionEffectLabels = parsePotionEffectLabels(itemJson);
            } catch (Exception ignored) {}
        }
        
        // 使用自定义名称或传入的物品名称
        String displayName = customDisplayName != null ? customDisplayName : itemName;
        
        // 构建悬停组件 - 模拟原版 Minecraft 物品提示样式
        Component hoverComponent = Component.empty();
        
        // 物品名称 - 使用斜体白色（自定义名称）或普通白色
        if (customDisplayName != null) {
            // 自定义名称 - 解析颜色代码并显示
            hoverComponent = hoverComponent.append(parseColoredText(customDisplayName));
        } else {
            hoverComponent = hoverComponent.append(Component.text(displayName, NamedTextColor.WHITE));
        }
        
        // 附魔信息 - 蓝色显示
        if (enchantmentsText != null && !enchantmentsText.isEmpty()) {
            String[] enchants = enchantmentsText.split(", ");
            for (String ench : enchants) {
                hoverComponent = hoverComponent
                    .append(Component.text("\n"))
                    .append(Component.text(ench, NamedTextColor.GRAY));
            }
        }

        // 药水效果（支持多效果）
        if (potionEffectLabels != null && !potionEffectLabels.isEmpty()) {
            for (String effectLabel : potionEffectLabels) {
                hoverComponent = hoverComponent
                        .append(Component.text("\n"))
                        .append(Component.text(effectLabel, NamedTextColor.AQUA));
            }
        }
        
        // Lore - 紫色斜体显示
        if (loreLines != null && !loreLines.isEmpty()) {
            for (String loreLine : loreLines) {
            hoverComponent = hoverComponent
                .append(Component.text("\n"))
                    .append(parseColoredText(loreLine).color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.ITALIC));
            }
        }

        // 尝试解析更多物品信息（旧格式兼容）
        if (itemJson != null && !itemJson.isEmpty()) {
            try {
                // 解析耐久度
                String damage = extractJsonValue(itemJson, "Damage");
                if (damage != null && !damage.equals("0")) {
                    hoverComponent = hoverComponent
                        .append(Component.text("\n"))
                        .append(Component.text("耐久度: ", NamedTextColor.GRAY))
                        .append(Component.text(damage, NamedTextColor.RED));
                }

                // 解析药水效果
                if (itemJson.contains("\"Potion\"") || itemJson.contains("\"CustomPotionEffects\"")) {
                    hoverComponent = hoverComponent
                        .append(Component.text("\n"))
                        .append(Component.text("药水效果", NamedTextColor.LIGHT_PURPLE));
                }

            } catch (Exception ignored) {}
                }

        // 如果有展示ID，添加点击提示
        String displayId = extractDisplayId(itemJson);
        if (displayId != null && !displayId.isEmpty()) {
                    hoverComponent = hoverComponent
                .append(Component.text("\n"))
                .append(Component.text("点击查看物品详情", NamedTextColor.YELLOW, TextDecoration.ITALIC));
        }

        return hoverComponent;
    }
    
    /**
     * 解析带有 Minecraft 颜色代码的文本
     * 支持 § 和 & 颜色代码
     */
    private static Component parseColoredText(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        // 替换 & 为 §
        text = text.replace('&', '§');
        
        Component result = Component.empty();
        StringBuilder currentText = new StringBuilder();
        NamedTextColor currentColor = NamedTextColor.WHITE;
        boolean bold = false;
        boolean italic = false;
        boolean underlined = false;
        boolean strikethrough = false;
        boolean obfuscated = false;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '§' && i + 1 < text.length()) {
                // 先添加当前累积的文本
                if (currentText.length() > 0) {
                    Component part = Component.text(currentText.toString()).color(currentColor);
                    if (bold) part = part.decorate(TextDecoration.BOLD);
                    if (italic) part = part.decorate(TextDecoration.ITALIC);
                    if (underlined) part = part.decorate(TextDecoration.UNDERLINED);
                    if (strikethrough) part = part.decorate(TextDecoration.STRIKETHROUGH);
                    if (obfuscated) part = part.decorate(TextDecoration.OBFUSCATED);
                    result = result.append(part);
                    currentText = new StringBuilder();
                }
                
                char code = text.charAt(i + 1);
                i++; // 跳过颜色代码字符
                
                switch (Character.toLowerCase(code)) {
                    case '0': currentColor = NamedTextColor.BLACK; break;
                    case '1': currentColor = NamedTextColor.DARK_BLUE; break;
                    case '2': currentColor = NamedTextColor.DARK_GREEN; break;
                    case '3': currentColor = NamedTextColor.DARK_AQUA; break;
                    case '4': currentColor = NamedTextColor.DARK_RED; break;
                    case '5': currentColor = NamedTextColor.DARK_PURPLE; break;
                    case '6': currentColor = NamedTextColor.GOLD; break;
                    case '7': currentColor = NamedTextColor.GRAY; break;
                    case '8': currentColor = NamedTextColor.DARK_GRAY; break;
                    case '9': currentColor = NamedTextColor.BLUE; break;
                    case 'a': currentColor = NamedTextColor.GREEN; break;
                    case 'b': currentColor = NamedTextColor.AQUA; break;
                    case 'c': currentColor = NamedTextColor.RED; break;
                    case 'd': currentColor = NamedTextColor.LIGHT_PURPLE; break;
                    case 'e': currentColor = NamedTextColor.YELLOW; break;
                    case 'f': currentColor = NamedTextColor.WHITE; break;
                    case 'k': obfuscated = true; break;
                    case 'l': bold = true; break;
                    case 'm': strikethrough = true; break;
                    case 'n': underlined = true; break;
                    case 'o': italic = true; break;
                    case 'r':
                        currentColor = NamedTextColor.WHITE;
                        bold = italic = underlined = strikethrough = obfuscated = false;
                        break;
                }
            } else {
                currentText.append(c);
            }
        }
        
        // 添加剩余的文本
        if (currentText.length() > 0) {
            Component part = Component.text(currentText.toString()).color(currentColor);
            if (bold) part = part.decorate(TextDecoration.BOLD);
            if (italic) part = part.decorate(TextDecoration.ITALIC);
            if (underlined) part = part.decorate(TextDecoration.UNDERLINED);
            if (strikethrough) part = part.decorate(TextDecoration.STRIKETHROUGH);
            if (obfuscated) part = part.decorate(TextDecoration.OBFUSCATED);
            result = result.append(part);
        }
        
        return result;
    }

    /**
     * 提取物品的显示名称
     */
    private static String extractDisplayName(String itemJson) {
        try {
            // 查找 display.Name 字段
            String displayPattern = "\"display\"\\s*:\\s*\\{[^}]*\"Name\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(displayPattern, java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher matcher = pattern.matcher(itemJson);

            if (matcher.find()) {
                return matcher.group(1);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从物品 JSON 解析物品名称
     * 支持 ModernItemUtil.getItemDataForTransfer 生成的 JSON 格式
     */
    public static String parseItemName(String itemJson) {
        if (itemJson == null || itemJson.isEmpty()) {
            return "未知物品";
        }

        try {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(itemJson).getAsJsonObject();

            // 优先使用 displayName（自定义显示名称）
            if (json.has("displayName")) {
                return json.get("displayName").getAsString();
            }

            String id = null;
            if (json.has("id")) {
                id = json.get("id").getAsString();
            } else {
                if (json.has("hoverEvent")) {
                    com.google.gson.JsonObject hoverEvent = json.getAsJsonObject("hoverEvent");
                    if (hoverEvent.has("contents") && hoverEvent.get("contents").isJsonArray()) {
                        com.google.gson.JsonArray contents = hoverEvent.getAsJsonArray("contents");
                        if (contents.size() > 0 && contents.get(0).isJsonObject()) {
                            com.google.gson.JsonObject content = contents.get(0).getAsJsonObject();
                            if (content.has("text")) {
                                return content.get("text").getAsString();
                            }
                        }
                    }
                }

                if (json.has("text")) {
                    String text = json.get("text").getAsString();
                    if (text.contains("物品")) {
                        if (json.has("extra") && json.get("extra").isJsonArray()) {
                            com.google.gson.JsonArray extra = json.getAsJsonArray("extra");
                            for (int i = 0; i < extra.size(); i++) {
                                if (extra.get(i).isJsonObject()) {
                                    com.google.gson.JsonObject extraObj = extra.get(i).getAsJsonObject();
                                    if (extraObj.has("extra") && extraObj.get("extra").isJsonArray()) {
                                        com.google.gson.JsonArray innerExtra = extraObj.getAsJsonArray("extra");
                                        for (int j = 0; j < innerExtra.size(); j++) {
                                            if (innerExtra.get(j).isJsonObject()) {
                                                com.google.gson.JsonObject innerObj = innerExtra.get(j).getAsJsonObject();
                                                if (innerObj.has("translate")) {
                                                    String translate = innerObj.get("translate").getAsString();
                                                    if (translate.startsWith("block.") || translate.startsWith("item.")) {
                                                        String itemName = translate.substring(translate.lastIndexOf('.') + 1);
                                                        return formatItemName(itemName);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        return "物品";
                    }
                    return text;
                }
            }

            if (id != null && !id.isEmpty()) {
                String detailedName = tryBuildSpecialItemName(json, id, itemJson);
                if (detailedName != null && !detailedName.isEmpty()) {
                    return detailedName;
                }
                return formatItemName(id);
            }

            return "物品";
        } catch (Exception e) {
            return "未知物品";
        }
    }

    /**
     * 解析物品的附魔信息
     */
    private static String parseEnchantments(String itemJson) {
        try {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(itemJson).getAsJsonObject();

            if (json.has("enchantments")) {
                com.google.gson.JsonObject enchantments = json.getAsJsonObject("enchantments");
                java.util.Set<java.util.Map.Entry<String, com.google.gson.JsonElement>> entries = enchantments.entrySet();

                if (!entries.isEmpty()) {
                    StringBuilder result = new StringBuilder();
                    for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : entries) {
                        int level = entry.getValue().getAsInt();
                        if (result.length() > 0) {
                            result.append(", ");
                        }
                        result.append(formatEnchantmentLabel(entry.getKey(), level));
                    }
                    return result.toString();
                }
            }

            // 检查旧格式的附魔字段
            if (json.has("Enchantments") && json.get("Enchantments").isJsonArray()) {
                com.google.gson.JsonArray enchArray = json.getAsJsonArray("Enchantments");
                if (!enchArray.isEmpty()) {
                    StringBuilder result = new StringBuilder();
                    for (int i = 0; i < enchArray.size(); i++) {
                        com.google.gson.JsonObject ench = enchArray.get(i).getAsJsonObject();
                        String enchNameId = "未知附魔";
                        int level = 1;

                        if (ench.has("id")) {
                            String id = ench.get("id").getAsString();
                            enchNameId = id;
                        }

                        if (ench.has("lvl")) {
                            level = ench.get("lvl").getAsInt();
                        }

                        if (result.length() > 0) {
                            result.append(", ");
                        }
                        result.append(formatEnchantmentLabel(enchNameId, level));
                    }
                    return result.toString();
                }
            }

            String stored = parseStoredEnchantmentsFromNbt(json, itemJson);
            if (stored != null && !stored.isEmpty()) {
                return stored;
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String parseStoredEnchantmentsFromNbt(com.google.gson.JsonObject json, String itemJson) {
        String nbt = null;
        if (json != null && json.has("nbt")) {
            try {
                nbt = json.get("nbt").getAsString();
            } catch (Exception ignored) {
            }
        }

        String source = (nbt == null ? "" : nbt + " ") + (itemJson == null ? "" : itemJson);
        if (!source.contains("StoredEnchantments") && !source.contains("stored_enchantments")) {
            return null;
        }

        java.util.List<String> labels = new java.util.ArrayList<>();
        java.util.regex.Pattern p1 = java.util.regex.Pattern.compile(
                "id\\s*[:=]\\s*\"?([a-z0-9_:.]+)\"?\\s*,\\s*(?:lvl|level)\\s*[:=]\\s*([0-9]+)s?",
                java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher m1 = p1.matcher(source);
        while (m1.find()) {
            labels.add(formatEnchantmentLabel(m1.group(1), parseSafeInt(m1.group(2), 1)));
        }

        if (labels.isEmpty()) {
            java.util.regex.Pattern p2 = java.util.regex.Pattern.compile(
                    "(?:lvl|level)\\s*[:=]\\s*([0-9]+)s?\\s*,\\s*id\\s*[:=]\\s*\"?([a-z0-9_:.]+)\"?",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher m2 = p2.matcher(source);
            while (m2.find()) {
                labels.add(formatEnchantmentLabel(m2.group(2), parseSafeInt(m2.group(1), 1)));
            }
        }

        if (labels.isEmpty()) {
            return null;
        }
        return String.join(", ", labels);
    }

    /**
     * 格式化附魔名称，使其更易读
     */
    private static String formatEnchantmentName(String enchId) {
        if (enchId == null || enchId.isEmpty()) {
            return "未知附魔";
        }
        String normalized = enchId.toLowerCase();
        if (normalized.startsWith("minecraft:")) {
            normalized = normalized.substring("minecraft:".length());
        } else if (normalized.contains(":")) {
            normalized = normalized.substring(normalized.indexOf(':') + 1);
        }

        // 优先使用最新语言表词条，确保新附魔（如 lunge -> 突刺）可及时生效
        String localized = LANG_ZH_CN.get("enchantment.minecraft." + normalized);
        if (localized != null && !localized.isEmpty()) {
            return localized;
        }

        // 常见的附魔名称映射
        switch (normalized) {
            case "protection": return "保护";
            case "fire_protection": return "火焰保护";
            case "feather_falling": return "摔落保护";
            case "blast_protection": return "爆炸保护";
            case "projectile_protection": return "弹射物保护";
            case "respiration": return "水下呼吸";
            case "aqua_affinity": return "水下速掘";
            case "thorns": return "荆棘";
            case "depth_strider": return "深海探索者";
            case "frost_walker": return "冰霜行者";
            case "binding_curse": return "绑定诅咒";
            case "soul_speed": return "灵魂疾行";
            case "swift_sneak": return "迅捷潜行";
            case "sharpness": return "锋利";
            case "smite": return "亡灵杀手";
            case "bane_of_arthropods": return "节肢杀手";
            case "knockback": return "击退";
            case "fire_aspect": return "火焰附加";
            case "looting": return "抢夺";
            case "sweeping": return "横扫之刃";
            case "efficiency": return "效率";
            case "silk_touch": return "精准采集";
            case "unbreaking": return "耐久";
            case "fortune": return "时运";
            case "power": return "力量";
            case "punch": return "冲击";
            case "flame": return "火矢";
            case "infinity": return "无限";
            case "luck_of_the_sea": return "海之眷顾";
            case "lure": return "饵钓";
            case "loyalty": return "忠诚";
            case "impaling": return "穿刺";
            case "riptide": return "激流";
            case "channeling": return "引雷";
            case "multishot": return "多重射击";
            case "quick_charge": return "快速装填";
            case "piercing": return "穿透";
            case "density": return "致密";
            case "breach": return "破裂";
            case "wind_burst": return "风爆";
            case "mending": return "经验修补";
            default:
                // 对于未知附魔，将下划线替换为空格并首字母大写
                String[] parts = normalized.split("_");
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) result.append(" ");
                    String part = parts[i];
                    if (!part.isEmpty()) {
                        result.append(Character.toUpperCase(part.charAt(0)))
                              .append(part.substring(1).toLowerCase());
                    }
                }
                return result.toString();
        }
    }

    public static String formatEnchantmentLabel(String enchId, int level) {
        return formatEnchantmentName(enchId) + " " + toRoman(level);
    }

    private static String tryBuildSpecialItemName(com.google.gson.JsonObject json, String itemId, String itemJson) {
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }
        String normalizedId = itemId.toLowerCase();
        if (normalizedId.startsWith("minecraft:")) {
            normalizedId = normalizedId.substring("minecraft:".length());
        }

        if ("potion".equals(normalizedId) || "splash_potion".equals(normalizedId)
                || "lingering_potion".equals(normalizedId) || "tipped_arrow".equals(normalizedId)) {
            String potionKey = extractPotionKey(json, itemJson);
            if (potionKey != null && !potionKey.isEmpty()) {
                // 先尝试完整 key（包含 strong_/long_ 前缀）
                String exactKey = "item.minecraft." + normalizedId + ".effect." + potionKey;
                String localized = LANG_ZH_CN.get(exactKey);
                if (localized != null && !localized.isEmpty()) {
                    return localized;
                }
                // Minecraft 语言文件中 strong_/long_ 变体没有独立词条，去掉前缀重试
                String baseKey = stripPotionVariantPrefix(potionKey);
                if (!baseKey.equals(potionKey)) {
                    String fallbackKey = "item.minecraft." + normalizedId + ".effect." + baseKey;
                    localized = LANG_ZH_CN.get(fallbackKey);
                    if (localized != null && !localized.isEmpty()) {
                        return localized;
                    }
                }
            }
        }

        if ("enchanted_book".equals(normalizedId)) {
            // 按需求：附魔信息只在详情/hover 展示，名称保持“附魔书”
            return formatItemName(normalizedId);
        }
        return null;
    }

    private static String stripPotionVariantPrefix(String potionId) {
        if (potionId == null) return potionId;
        if (potionId.startsWith("strong_")) return potionId.substring("strong_".length());
        if (potionId.startsWith("long_")) return potionId.substring("long_".length());
        return potionId;
    }

    private static String extractPotionKey(com.google.gson.JsonObject json, String itemJson) {
        if (json != null && json.has("potionType")) {
            try {
                String raw = json.get("potionType").getAsString();
                if (raw != null && !raw.isEmpty()) {
                    return raw;
                }
            } catch (Exception ignored) {
            }
        }

        if (json != null && json.has("Potion")) {
            try {
                String raw = json.get("Potion").getAsString();
                if (raw.startsWith("minecraft:")) {
                    return raw.substring("minecraft:".length());
                }
                return raw;
            } catch (Exception ignored) {
            }
        }

        String nbt = null;
        if (json != null && json.has("nbt")) {
            try {
                nbt = json.get("nbt").getAsString();
            } catch (Exception ignored) {
            }
        }

        String source = (nbt == null ? "" : nbt + " ") + (itemJson == null ? "" : itemJson);
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "Potion\\s*[:=]\\s*\"minecraft:([a-z0-9_]+)\"",
                java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher m = p.matcher(source);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public static java.util.List<String> parsePotionEffectLabels(String itemJson) {
        java.util.List<String> labels = new java.util.ArrayList<>();
        if (itemJson == null || itemJson.isEmpty()) {
            return labels;
        }

        try {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(itemJson).getAsJsonObject();
            java.util.List<PotionEffectData> effectDataList = new java.util.ArrayList<>();

            String potionType = extractPotionKey(json, itemJson);
            if (potionType != null && !potionType.isEmpty()) {
                effectDataList.addAll(buildBasePotionEffects(potionType));
            }

            if (json.has("potionEffects") && json.get("potionEffects").isJsonArray()) {
                com.google.gson.JsonArray effects = json.getAsJsonArray("potionEffects");
                for (int i = 0; i < effects.size(); i++) {
                    if (!effects.get(i).isJsonObject()) continue;
                    com.google.gson.JsonObject effect = effects.get(i).getAsJsonObject();
                    String type = effect.has("type") ? effect.get("type").getAsString() : "";
                    int amplifier = effect.has("amplifier") ? effect.get("amplifier").getAsInt() : 0;
                    int duration = effect.has("duration") ? effect.get("duration").getAsInt() : 0;
                    effectDataList.add(new PotionEffectData(type, amplifier, duration));
                }
            }

            for (PotionEffectData data : effectDataList) {
                labels.add(formatPotionEffectLabel(data.type, data.amplifier, data.durationTicks));
            }
            labels.addAll(buildPotionModifierLines(effectDataList));
        } catch (Exception ignored) {
        }

        // 去重，保持顺序
        java.util.LinkedHashSet<String> uniq = new java.util.LinkedHashSet<>(labels);
        return new java.util.ArrayList<>(uniq);
    }

    private static java.util.List<PotionEffectData> buildBasePotionEffects(String potionType) {
        java.util.List<PotionEffectData> list = new java.util.ArrayList<>();
        String key = potionType == null ? "" : potionType.toLowerCase();
        if (key.startsWith("minecraft:")) {
            key = key.substring("minecraft:".length());
        }

        switch (key) {
            case "swiftness":
                list.add(new PotionEffectData("speed", 0, 180 * 20));
                break;
            case "long_swiftness":
                list.add(new PotionEffectData("speed", 0, 480 * 20));
                break;
            case "strong_swiftness":
                list.add(new PotionEffectData("speed", 1, 90 * 20));
                break;
            case "slowness":
                list.add(new PotionEffectData("slowness", 0, 90 * 20));
                break;
            case "long_slowness":
                list.add(new PotionEffectData("slowness", 0, 240 * 20));
                break;
            case "strong_slowness":
                list.add(new PotionEffectData("slowness", 3, 20 * 20));
                break;
            case "strength":
                list.add(new PotionEffectData("strength", 0, 180 * 20));
                break;
            case "long_strength":
                list.add(new PotionEffectData("strength", 0, 480 * 20));
                break;
            case "strong_strength":
                list.add(new PotionEffectData("strength", 1, 90 * 20));
                break;
            case "healing":
                list.add(new PotionEffectData("instant_health", 0, 0));
                break;
            case "strong_healing":
                list.add(new PotionEffectData("instant_health", 1, 0));
                break;
            case "harming":
                list.add(new PotionEffectData("instant_damage", 0, 0));
                break;
            case "strong_harming":
                list.add(new PotionEffectData("instant_damage", 1, 0));
                break;
            case "poison":
                list.add(new PotionEffectData("poison", 0, 45 * 20));
                break;
            case "long_poison":
                list.add(new PotionEffectData("poison", 0, 120 * 20));
                break;
            case "strong_poison":
                list.add(new PotionEffectData("poison", 1, 22 * 20));
                break;
            case "regeneration":
                list.add(new PotionEffectData("regeneration", 0, 45 * 20));
                break;
            case "long_regeneration":
                list.add(new PotionEffectData("regeneration", 0, 120 * 20));
                break;
            case "strong_regeneration":
                list.add(new PotionEffectData("regeneration", 1, 22 * 20));
                break;
            case "leaping":
                list.add(new PotionEffectData("jump_boost", 0, 180 * 20));
                break;
            case "long_leaping":
                list.add(new PotionEffectData("jump_boost", 0, 480 * 20));
                break;
            case "strong_leaping":
                list.add(new PotionEffectData("jump_boost", 1, 90 * 20));
                break;
            case "night_vision":
                list.add(new PotionEffectData("night_vision", 0, 180 * 20));
                break;
            case "long_night_vision":
                list.add(new PotionEffectData("night_vision", 0, 480 * 20));
                break;
            case "invisibility":
                list.add(new PotionEffectData("invisibility", 0, 180 * 20));
                break;
            case "long_invisibility":
                list.add(new PotionEffectData("invisibility", 0, 480 * 20));
                break;
            case "water_breathing":
                list.add(new PotionEffectData("water_breathing", 0, 180 * 20));
                break;
            case "long_water_breathing":
                list.add(new PotionEffectData("water_breathing", 0, 480 * 20));
                break;
            case "fire_resistance":
                list.add(new PotionEffectData("fire_resistance", 0, 180 * 20));
                break;
            case "long_fire_resistance":
                list.add(new PotionEffectData("fire_resistance", 0, 480 * 20));
                break;
            case "slow_falling":
                list.add(new PotionEffectData("slow_falling", 0, 90 * 20));
                break;
            case "long_slow_falling":
                list.add(new PotionEffectData("slow_falling", 0, 240 * 20));
                break;
            case "weakness":
                list.add(new PotionEffectData("weakness", 0, 90 * 20));
                break;
            case "long_weakness":
                list.add(new PotionEffectData("weakness", 0, 240 * 20));
                break;
            case "luck":
                list.add(new PotionEffectData("luck", 0, 300 * 20));
                break;
            case "turtle_master":
                list.add(new PotionEffectData("slowness", 3, 20 * 20));
                list.add(new PotionEffectData("resistance", 2, 20 * 20));
                break;
            case "long_turtle_master":
                list.add(new PotionEffectData("slowness", 3, 40 * 20));
                list.add(new PotionEffectData("resistance", 2, 40 * 20));
                break;
            case "strong_turtle_master":
                list.add(new PotionEffectData("slowness", 5, 20 * 20));
                list.add(new PotionEffectData("resistance", 3, 20 * 20));
                break;
            default:
                break;
        }
        return list;
    }

    private static String formatPotionEffectLabel(String type, int amplifier, int durationTicks) {
        String normalized = type == null ? "" : type.toLowerCase();
        if (normalized.startsWith("minecraft:")) {
            normalized = normalized.substring("minecraft:".length());
        }
        String zh = LANG_ZH_CN.get("effect.minecraft." + normalized);
        if (zh == null || zh.isEmpty()) {
            zh = normalized.isEmpty() ? "未知效果" : normalized.replace('_', ' ');
        }

        StringBuilder sb = new StringBuilder(zh);
        if (amplifier >= 0) {
            sb.append(" ").append(toRoman(amplifier + 1));
        }
        if (durationTicks > 0) {
            int total = durationTicks / 20;
            int min = total / 60;
            int sec = total % 60;
            sb.append(" (").append(min).append(":").append(String.format("%02d", sec)).append(")");
        }
        return sb.toString();
    }

    private static java.util.List<String> buildPotionModifierLines(java.util.List<PotionEffectData> effects) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        java.util.List<String> modifiers = new java.util.ArrayList<>();
        for (PotionEffectData data : effects) {
            String effectKey = data.type == null ? "" : data.type.toLowerCase();
            if (effectKey.startsWith("minecraft:")) {
                effectKey = effectKey.substring("minecraft:".length());
            }
            if ("slowness".equals(effectKey)) {
                int percent = -15 * (data.amplifier + 1);
                modifiers.add(percent + "% 速度");
            } else if ("speed".equals(effectKey)) {
                int percent = 20 * (data.amplifier + 1);
                modifiers.add("+" + percent + "% 速度");
            }
        }
        if (!modifiers.isEmpty()) {
            lines.add("当生效后：");
            lines.addAll(modifiers);
        }
        return lines;
    }

    private static final class PotionEffectData {
        private final String type;
        private final int amplifier;
        private final int durationTicks;

        private PotionEffectData(String type, int amplifier, int durationTicks) {
            this.type = type;
            this.amplifier = amplifier;
            this.durationTicks = durationTicks;
        }
    }

    private static int parseSafeInt(String text, int fallback) {
        try {
            return Integer.parseInt(text);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /**
     * 从物品 JSON 解析物品数量
     */
    public static int parseItemAmount(String itemJson) {
        if (itemJson == null || itemJson.isEmpty()) {
            return 1;
        }

        try {
            // 使用 Gson 进行更可靠的 JSON 解析
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(itemJson).getAsJsonObject();

            // 尝试不同的字段名（count 或 Count）
            if (json.has("count")) {
                return json.get("count").getAsInt();
            } else if (json.has("Count")) {
                return json.get("Count").getAsInt();
            }

            return 1;
        } catch (Exception e) {
            // 如果 JSON 解析失败，使用正则表达式作为备选方案
            try {
                String countStr = extractJsonValue(itemJson, "count");
                if (countStr == null || countStr.isEmpty()) {
                    countStr = extractJsonValue(itemJson, "Count");
                }
                if (countStr != null && !countStr.isEmpty()) {
                    return Integer.parseInt(countStr);
                }
            } catch (Exception e2) {
                // 完全解析失败
            }
            return 1;
        }
    }

    /**
     * 从 JSON 字符串中提取指定键的值
     */
    private static String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }

            // 尝试匹配数字值
            pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
            p = java.util.regex.Pattern.compile(pattern);
            m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 格式化物品名称，使其更易读
     */
    private static String formatItemName(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return "未知物品";
        }

        // 移除 minecraft: 前缀
        if (itemId.startsWith("minecraft:")) {
            itemId = itemId.substring(10);
        }

        String normalizedId = itemId.toLowerCase();
        String mapped = LANG_ZH_CN.get("item.minecraft." + normalizedId);
        if (mapped == null) {
            mapped = LANG_ZH_CN.get("block.minecraft." + normalizedId);
        }
        if (mapped == null) {
            mapped = ITEM_NAME_ZH_FALLBACK.get(normalizedId);
        }
        if (mapped != null) {
            return mapped;
        }

        // 将下划线替换为空格，并首字母大写
        String[] parts = itemId.split("_");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) result.append(" ");
            String part = parts[i];
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)))
                      .append(part.substring(1).toLowerCase());
            }
        }

        return result.toString();
    }

    private static String toRoman(int value) {
        if (value <= 0) {
            return String.valueOf(value);
        }
        if (value > 20) {
            return String.valueOf(value);
        }
        String[] romans = {
            "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
            "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"
        };
        return romans[value];
    }

    private static Map<String, String> loadZhLangMap() {
        Map<String, String> map = new HashMap<>();
        try (InputStream in = VelocityItemUtil.class.getClassLoader().getResourceAsStream("lang/zh_cn.json")) {
            if (in != null) {
                mergeLangJson(map, new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
        }

        // 尝试拉取最新语言资源覆盖本地词条（网络异常时自动回退本地资源）
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(4))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://assets.mcasset.cloud/latest/assets/minecraft/lang/zh_cn.json"))
                    .timeout(java.time.Duration.ofSeconds(6))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 200 && resp.statusCode() < 300 && resp.body() != null && !resp.body().isEmpty()) {
                mergeLangJson(map, new java.io.StringReader(resp.body()));
            }
        } catch (Exception ignored) {
        }

        return Collections.unmodifiableMap(map);
    }

    private static void mergeLangJson(Map<String, String> target, java.io.Reader reader) {
        com.google.gson.JsonObject root = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : root.entrySet()) {
            if (entry.getValue() != null && entry.getValue().isJsonPrimitive()
                    && entry.getValue().getAsJsonPrimitive().isString()) {
                target.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
    }

    private static Map<String, String> createItemNameZhFallbackMap() {
        Map<String, String> map = new HashMap<>();
        map.put("stone", "石头");
        map.put("cobblestone", "圆石");
        map.put("dirt", "泥土");
        map.put("grass_block", "草方块");
        map.put("sand", "沙子");
        map.put("gravel", "砂砾");
        map.put("oak_log", "橡木原木");
        map.put("birch_log", "白桦原木");
        map.put("spruce_log", "云杉原木");
        map.put("jungle_log", "丛林原木");
        map.put("acacia_log", "金合欢原木");
        map.put("dark_oak_log", "深色橡木原木");
        map.put("mangrove_log", "红树原木");
        map.put("cherry_log", "樱花木原木");
        map.put("diamond", "钻石");
        map.put("emerald", "绿宝石");
        map.put("gold_ingot", "金锭");
        map.put("iron_ingot", "铁锭");
        map.put("netherite_ingot", "下界合金锭");
        map.put("coal", "煤炭");
        map.put("redstone", "红石");
        map.put("lapis_lazuli", "青金石");
        map.put("diamond_sword", "钻石剑");
        map.put("diamond_pickaxe", "钻石镐");
        map.put("diamond_axe", "钻石斧");
        map.put("diamond_shovel", "钻石锹");
        map.put("diamond_hoe", "钻石锄");
        map.put("netherite_sword", "下界合金剑");
        map.put("netherite_pickaxe", "下界合金镐");
        map.put("netherite_axe", "下界合金斧");
        map.put("netherite_shovel", "下界合金锹");
        map.put("netherite_hoe", "下界合金锄");
        map.put("bow", "弓");
        map.put("crossbow", "弩");
        map.put("trident", "三叉戟");
        map.put("shield", "盾牌");
        map.put("elytra", "鞘翅");
        map.put("totem_of_undying", "不死图腾");
        map.put("enchanted_golden_apple", "附魔金苹果");
        map.put("golden_apple", "金苹果");
        map.put("apple", "苹果");
        map.put("bread", "面包");
        map.put("carrot", "胡萝卜");
        map.put("potato", "马铃薯");
        map.put("cooked_beef", "熟牛肉");
        map.put("beef", "生牛肉");
        map.put("golden_carrot", "金胡萝卜");
        map.put("cake", "蛋糕");
        map.put("carrot_on_a_stick", "胡萝卜钓竿");
        map.put("book", "书");
        map.put("enchanted_book", "附魔书");
        map.put("anvil", "铁砧");
        map.put("experience_bottle", "附魔之瓶");
        map.put("ender_pearl", "末影珍珠");
        map.put("ender_eye", "末影之眼");
        map.put("obsidian", "黑曜石");
        map.put("water_bucket", "水桶");
        map.put("lava_bucket", "熔岩桶");
        map.put("bucket", "桶");
        map.put("torch", "火把");
        map.put("lantern", "灯笼");
        map.put("crafting_table", "工作台");
        map.put("furnace", "熔炉");
        map.put("chest", "箱子");
        map.put("ender_chest", "末影箱");
        map.put("shulker_box", "潜影盒");
        return Collections.unmodifiableMap(map);
    }

    /**
     * 创建背包显示组件
     */
    public static Component createInventoryComponent(String inventoryJson, String playerName) {
        Component invText = Component.text("[" + playerName + "的背包]")
            .color(NamedTextColor.BLUE)
            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/invsee " + playerName));

        Component hoverText = Component.text("点击查看 " + playerName + " 的完整背包")
            .color(NamedTextColor.GREEN);

        return invText.hoverEvent(HoverEvent.showText(hoverText));
    }

    /**
     * 创建末影箱显示组件
     */
    public static Component createEnderChestComponent(String enderChestJson, String playerName) {
        Component ecText = Component.text("[" + playerName + "的末影箱]")
            .color(NamedTextColor.DARK_PURPLE)
            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/endersee " + playerName));

        Component hoverText = Component.text("点击查看 " + playerName + " 的末影箱")
            .color(NamedTextColor.LIGHT_PURPLE);

        return ecText.hoverEvent(HoverEvent.showText(hoverText));
    }
}
