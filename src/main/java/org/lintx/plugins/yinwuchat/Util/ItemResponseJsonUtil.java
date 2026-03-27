package org.lintx.plugins.yinwuchat.Util;

import com.google.gson.Gson;
import org.lintx.plugins.yinwuchat.velocity.json.ItemResponse;

import java.util.List;

public final class ItemResponseJsonUtil {

    private static final Gson GSON = new Gson();

    private ItemResponseJsonUtil() {
    }

    public static String success(String playerName, String ownerName, String requestType, List<String> items) {
        ItemResponse response = new ItemResponse();
        response.playerName = playerName;
        response.ownerName = ownerName == null ? "" : ownerName;
        response.requestType = requestType;
        response.success = true;
        response.items = items;
        return GSON.toJson(response);
    }

    public static String error(String playerName, String requestType, String errorMessage) {
        ItemResponse response = new ItemResponse();
        response.playerName = playerName;
        response.ownerName = "";
        response.requestType = requestType == null ? "unknown" : requestType;
        response.success = false;
        response.errorMessage = errorMessage;
        return GSON.toJson(response);
    }
}
