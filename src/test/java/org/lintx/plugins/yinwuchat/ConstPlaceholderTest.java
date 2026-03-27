package org.lintx.plugins.yinwuchat;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class ConstPlaceholderTest {

    @Test
    public void backpackPlaceholderMatchesUppercaseAndLowercase() {
        Pattern pattern = Pattern.compile(Const.BACKPACK_PLACEHOLDER);

        assertTrue(pattern.matcher("[B]").matches());
        assertTrue(pattern.matcher("[b]").matches());
    }
}
