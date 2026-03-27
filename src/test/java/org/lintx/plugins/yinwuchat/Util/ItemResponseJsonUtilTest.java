package org.lintx.plugins.yinwuchat.Util;

import com.google.gson.Gson;
import org.junit.Test;
import org.lintx.plugins.yinwuchat.velocity.json.ItemResponse;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ItemResponseJsonUtilTest {

    @Test
    public void serializesBackpackPayloadIntoValidJson() {
        String backpackPayload = "{\"displayType\":\"backpack\",\"displayId\":\"e2d1e2ad\",\"ownerName\":\"Xx_Kirov_xX\",\"slots\":[\"{\\\"id\\\":\\\"minecraft:stone\\\",\\\"count\\\":1}\",\"\"]}";

        String json = ItemResponseJsonUtil.success("viewer", "owner", "backpackview", Collections.singletonList(backpackPayload));
        ItemResponse parsed = new Gson().fromJson(json, ItemResponse.class);

        assertTrue(parsed.success);
        assertEquals("viewer", parsed.playerName);
        assertEquals("owner", parsed.ownerName);
        assertEquals("backpackview", parsed.requestType);
        assertEquals(1, parsed.items.size());
        assertEquals(backpackPayload, parsed.items.get(0));
    }

    @Test
    public void serializesErrorIntoValidJson() {
        String json = ItemResponseJsonUtil.error("viewer", "backpackview", "目标玩家不在线");
        ItemResponse parsed = new Gson().fromJson(json, ItemResponse.class);

        assertTrue(!parsed.success);
        assertEquals("viewer", parsed.playerName);
        assertEquals("backpackview", parsed.requestType);
        assertEquals("目标玩家不在线", parsed.errorMessage);
    }
}
