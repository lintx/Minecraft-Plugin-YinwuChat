package org.lintx.plugins.yinwuchat.Util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class BackpackViewCommandUtilTest {

    @Test
    public void filtersAndSortsPlayerNameSuggestions() {
        List<String> suggestions = BackpackViewCommandUtil.suggestTargets("ki", List.of(
                "Steve",
                "Kirov",
                "kiro_2",
                "Alex"
        ));

        assertEquals(List.of("kiro_2", "Kirov"), suggestions);
    }
}
