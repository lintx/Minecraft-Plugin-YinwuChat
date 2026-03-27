package org.lintx.plugins.yinwuchat.Util;

import org.junit.Test;
import org.lintx.plugins.yinwuchat.velocity.json.ItemRequest;
import org.lintx.plugins.yinwuchat.velocity.json.ItemResponse;

import java.util.Collections;

import static org.junit.Assert.assertTrue;

public class BackpackViewDebugLogUtilTest {

    @Test
    public void summarizeRequestIncludesViewerTypeAndTarget() {
        ItemRequest request = new ItemRequest("Admin", "backpackview", "Steve");

        String summary = BackpackViewDebugLogUtil.summarizeRequest(request);

        assertTrue(summary.contains("viewer=Admin"));
        assertTrue(summary.contains("type=backpackview"));
        assertTrue(summary.contains("target=Steve"));
    }

    @Test
    public void summarizeResponseIncludesBackpackPayloadDetails() {
        ItemResponse response = new ItemResponse();
        response.playerName = "Admin";
        response.ownerName = "Steve";
        response.requestType = "backpackview";
        response.success = true;
        response.items = Collections.singletonList("{\"displayType\":\"backpack\",\"displayId\":\"abcd1234\",\"ownerName\":\"Steve\",\"slots\":[\"\",\"item\"]}");

        String summary = BackpackViewDebugLogUtil.summarizeResponse(response);

        assertTrue(summary.contains("viewer=Admin"));
        assertTrue(summary.contains("owner=Steve"));
        assertTrue(summary.contains("success=true"));
        assertTrue(summary.contains("items=1"));
        assertTrue(summary.contains("displayId=abcd1234"));
        assertTrue(summary.contains("slots=2"));
    }
}
