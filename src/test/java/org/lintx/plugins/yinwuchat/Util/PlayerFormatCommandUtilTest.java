package org.lintx.plugins.yinwuchat.Util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PlayerFormatCommandUtilTest {

    @Test
    public void joinAndFilterContentMergesRemainingArgs() {
        String[] args = {"format", "public", "prefix", "set", "&a勇者", "称号"};

        String content = PlayerFormatCommandUtil.joinAndFilterContent(args, 4, "");

        assertEquals("&a勇者 称号", content);
    }

    @Test
    public void joinAndFilterContentRemovesDeniedStyles() {
        String[] args = {"format", "private", "suffix", "set", "&a勇者", "&l称号"};

        String content = PlayerFormatCommandUtil.joinAndFilterContent(args, 4, "l");

        assertEquals("&a勇者 称号", content);
    }

    @Test
    public void joinAndFilterContentReturnsEmptyWhenNoContentProvided() {
        String[] args = {"format", "public", "prefix", "set"};

        String content = PlayerFormatCommandUtil.joinAndFilterContent(args, 4, "l");

        assertEquals("", content);
    }
}
