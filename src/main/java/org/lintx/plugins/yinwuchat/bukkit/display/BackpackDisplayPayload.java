package org.lintx.plugins.yinwuchat.bukkit.display;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BackpackDisplayPayload {
    private static final Gson GSON = new Gson();

    public String displayType = "backpack";
    public String displayId = "";
    public String ownerName = "";
    public String displayName = "";
    public List<String> slots = new ArrayList<>();

    public static String toJson(String ownerName, String displayId, List<String> slots) {
        BackpackDisplayPayload payload = new BackpackDisplayPayload();
        payload.ownerName = ownerName == null ? "" : ownerName;
        payload.displayId = displayId == null ? "" : displayId;
        payload.displayName = "[" + payload.ownerName + "的背包]";
        payload.slots = slots == null ? new ArrayList<>() : new ArrayList<>(slots);
        return GSON.toJson(payload);
    }

    public static BackpackDisplayPayload fromJson(String json) {
        BackpackDisplayPayload payload = GSON.fromJson(json, BackpackDisplayPayload.class);
        if (payload == null) {
            return null;
        }
        if (payload.slots == null) {
            payload.slots = new ArrayList<>();
        }
        return payload;
    }

    public List<String> getSlots() {
        return Collections.unmodifiableList(slots);
    }
}
