package org.lintx.plugins.yinwuchat.Util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Map;

public final class WebItemPayloadUtil {

    private WebItemPayloadUtil() {
    }

    public static JsonObject toWebItem(String rawItemJson) {
        JsonObject raw = JsonParser.parseString(rawItemJson).getAsJsonObject();
        JsonObject webItem = new JsonObject();

        if (isBackpackPayload(raw)) {
            webItem.addProperty("displayType", "backpack");
            webItem.addProperty("displayId", getString(raw, "displayId"));
            webItem.addProperty("ownerName", getString(raw, "ownerName"));
            webItem.addProperty("name", firstNonEmpty(
                    getString(raw, "displayName"),
                    "[" + firstNonEmpty(getString(raw, "ownerName"), "玩家") + "的背包]"
            ));
            webItem.add("slots", copyStringArray(raw, "slots"));
            webItem.add("lore", new JsonArray());
            webItem.add("enchantments", new JsonArray());
            webItem.addProperty("count", 1);
            return webItem;
        }

        webItem.addProperty("name", firstNonEmpty(
                getString(raw, "displayName"),
                getString(raw, "name"),
                firstNonEmpty(getString(raw, "id"), "物品")
        ));
        webItem.addProperty("count", getInt(raw, "count", getInt(raw, "amount", 1)));
        if (raw.has("id")) {
            webItem.addProperty("id", getString(raw, "id"));
        }
        if (raw.has("displayId")) {
            webItem.addProperty("displayId", getString(raw, "displayId"));
        }
        webItem.add("lore", copyStringArray(raw, "lore"));

        JsonArray enchants = new JsonArray();
        if (raw.has("enchantments") && raw.get("enchantments").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : raw.getAsJsonObject("enchantments").entrySet()) {
                enchants.add(entry.getKey() + " " + entry.getValue().getAsInt());
            }
        }
        webItem.add("enchantments", enchants);
        return webItem;
    }

    private static boolean isBackpackPayload(JsonObject raw) {
        return raw.has("displayType") && "backpack".equalsIgnoreCase(getString(raw, "displayType"));
    }

    private static JsonArray copyStringArray(JsonObject raw, String key) {
        JsonArray arr = new JsonArray();
        if (!raw.has(key) || !raw.get(key).isJsonArray()) {
            return arr;
        }
        for (JsonElement element : raw.getAsJsonArray(key)) {
            arr.add(element.isJsonNull() ? "" : element.getAsString());
        }
        return arr;
    }

    private static String getString(JsonObject raw, String key) {
        return raw.has(key) && !raw.get(key).isJsonNull() ? raw.get(key).getAsString() : "";
    }

    private static int getInt(JsonObject raw, String key, int defaultValue) {
        try {
            return raw.has(key) && !raw.get(key).isJsonNull() ? raw.get(key).getAsInt() : defaultValue;
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return "";
    }
}
