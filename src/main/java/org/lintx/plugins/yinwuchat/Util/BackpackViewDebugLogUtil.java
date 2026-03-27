package org.lintx.plugins.yinwuchat.Util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.lintx.plugins.yinwuchat.velocity.json.ItemRequest;
import org.lintx.plugins.yinwuchat.velocity.json.ItemResponse;

import java.util.List;

public final class BackpackViewDebugLogUtil {

    private BackpackViewDebugLogUtil() {
    }

    public static String summarizeRequest(ItemRequest request) {
        if (request == null) {
            return "viewer=<null>, type=<null>, target=<null>";
        }
        return "viewer=" + safe(request.playerName)
                + ", type=" + safe(request.requestType)
                + ", target=" + safe(request.targetPlayer);
    }

    public static String summarizeResponse(ItemResponse response) {
        if (response == null) {
            return "viewer=<null>, owner=<null>, type=<null>, success=false, items=0";
        }
        List<String> items = response.items;
        StringBuilder summary = new StringBuilder()
                .append("viewer=").append(safe(response.playerName))
                .append(", owner=").append(safe(response.ownerName))
                .append(", type=").append(safe(response.requestType))
                .append(", success=").append(response.success)
                .append(", items=").append(items == null ? 0 : items.size());
        if (items != null && !items.isEmpty()) {
            summary.append(", firstItem=").append(summarizePayload(items.get(0)));
        }
        return summary.toString();
    }

    public static String summarizePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.trim().isEmpty()) {
            return "<empty>";
        }
        try {
            JsonObject json = JsonParser.parseString(payloadJson).getAsJsonObject();
            String displayType = stringValue(json, "displayType");
            if ("backpack".equalsIgnoreCase(displayType)) {
                JsonArray slots = json.has("slots") && json.get("slots").isJsonArray()
                        ? json.getAsJsonArray("slots")
                        : new JsonArray();
                return "backpack(displayId=" + safe(stringValue(json, "displayId"))
                        + ", owner=" + safe(stringValue(json, "ownerName"))
                        + ", slots=" + slots.size() + ")";
            }
            return "item(id=" + safe(stringValue(json, "id"))
                    + ", displayId=" + safe(stringValue(json, "displayId"))
                    + ", name=" + safe(firstNonEmpty(stringValue(json, "displayName"), stringValue(json, "name"))) + ")";
        } catch (Exception e) {
            return "<invalid-json>";
        }
    }

    public static String summarizeDisplayRequest(String displayId, String viewerName) {
        return "displayId=" + safe(displayId) + ", viewer=" + safe(viewerName);
    }

    public static String summarizeDisplayResponse(String displayId, boolean success, String itemJson, String playerName, String serverName) {
        return "displayId=" + safe(displayId)
                + ", success=" + success
                + ", owner=" + safe(playerName)
                + ", server=" + safe(serverName)
                + ", payload=" + summarizePayload(itemJson);
    }

    private static String stringValue(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static String safe(String value) {
        return value == null || value.isEmpty() ? "<empty>" : value;
    }
}
