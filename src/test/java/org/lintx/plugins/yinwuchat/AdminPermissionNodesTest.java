package org.lintx.plugins.yinwuchat;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class AdminPermissionNodesTest {

    @Test
    public void adminPermissionNodesIncludeBackpackViewPermission() {
        assertTrue(Arrays.asList(Const.ADMIN_PERMISSION_NODES).contains(Const.PERMISSION_BACKPACK_VIEW));
    }
}
