package org.lintx.plugins.yinwuchat.Util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AdminAlertCommandUtilTest {

    @Test
    public void buildAtAllAdminConfirmCommandIncludesPlayerName() {
        assertEquals(
                "/yinwuchat atalladmin confirm LinTx",
                AdminAlertCommandUtil.buildAtAllAdminConfirmCommand("LinTx")
        );
    }

    @Test
    public void buildAtAllAdminConfirmCommandTrimsWhitespace() {
        assertEquals(
                "/yinwuchat atalladmin confirm LinTx",
                AdminAlertCommandUtil.buildAtAllAdminConfirmCommand("  LinTx  ")
        );
    }
}
