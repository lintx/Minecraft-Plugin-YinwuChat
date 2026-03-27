package org.lintx.plugins.yinwuchat.Util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WebItemPayloadUtilTest {

    @Test
    public void preservesBackpackFieldsForWebRendering() {
        String payload = "{\"displayType\":\"backpack\",\"displayId\":\"abcd1234\",\"ownerName\":\"Steve\",\"displayName\":\"[Steve的背包]\",\"slots\":[\"\",\"\"]}";

        JsonObject webItem = WebItemPayloadUtil.toWebItem(payload);

        assertEquals("backpack", webItem.get("displayType").getAsString());
        assertEquals("abcd1234", webItem.get("displayId").getAsString());
        assertEquals("Steve", webItem.get("ownerName").getAsString());
        assertEquals("[Steve的背包]", webItem.get("name").getAsString());
        JsonArray slots = webItem.getAsJsonArray("slots");
        assertEquals(2, slots.size());
    }

    @Test
    public void keepsRegularItemFieldsAvailableForWebRendering() {
        String payload = "{\"id\":\"minecraft:diamond_sword\",\"displayName\":\"钻石剑\",\"count\":1,\"lore\":[\"锋利的武器\"],\"enchantments\":{\"sharpness\":5}}";

        JsonObject webItem = WebItemPayloadUtil.toWebItem(payload);

        assertEquals("钻石剑", webItem.get("name").getAsString());
        assertEquals(1, webItem.get("count").getAsInt());
        assertEquals("minecraft:diamond_sword", webItem.get("id").getAsString());
        assertTrue(webItem.getAsJsonArray("lore").size() > 0);
        assertTrue(webItem.getAsJsonArray("enchantments").size() > 0);
    }
}
