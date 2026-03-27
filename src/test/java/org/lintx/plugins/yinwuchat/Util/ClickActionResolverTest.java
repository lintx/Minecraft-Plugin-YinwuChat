package org.lintx.plugins.yinwuchat.Util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClickActionResolverTest {
    private static final String LINK_REGEX = "((https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])";

    @Test
    public void resolveKeepsProtocolUrlAsOpenUrl() {
        ClickActionResolver.ResolvedClick resolved = ClickActionResolver.resolve("https://example.com/docs", LINK_REGEX);

        assertEquals(ClickActionResolver.ClickMode.OPEN_URL, resolved.getMode());
        assertEquals("https://example.com/docs", resolved.getValue());
    }

    @Test
    public void resolveAddsProtocolForWwwUrl() {
        ClickActionResolver.ResolvedClick resolved = ClickActionResolver.resolve("www.example.com/docs", LINK_REGEX);

        assertEquals(ClickActionResolver.ClickMode.OPEN_URL, resolved.getMode());
        assertEquals("https://www.example.com/docs", resolved.getValue());
    }

    @Test
    public void resolveTreatsSlashCommandAsSuggestCommand() {
        ClickActionResolver.ResolvedClick resolved = ClickActionResolver.resolve("/server lobby", LINK_REGEX);

        assertEquals(ClickActionResolver.ClickMode.SUGGEST_COMMAND, resolved.getMode());
        assertEquals("/server lobby", resolved.getValue());
    }

    @Test
    public void resolveTreatsItemPlaceholderAsSuggestCommand() {
        ClickActionResolver.ResolvedClick resolved = ClickActionResolver.resolve("[i:0]", LINK_REGEX);

        assertEquals(ClickActionResolver.ClickMode.SUGGEST_COMMAND, resolved.getMode());
        assertEquals("[i:0]", resolved.getValue());
    }

    @Test
    public void resolveTreatsPositionAndBackpackPlaceholdersAsSuggestCommand() {
        ClickActionResolver.ResolvedClick positionResolved = ClickActionResolver.resolve("[p]", LINK_REGEX);
        ClickActionResolver.ResolvedClick backpackResolved = ClickActionResolver.resolve("[b]", LINK_REGEX);

        assertEquals(ClickActionResolver.ClickMode.SUGGEST_COMMAND, positionResolved.getMode());
        assertEquals("[p]", positionResolved.getValue());
        assertEquals(ClickActionResolver.ClickMode.SUGGEST_COMMAND, backpackResolved.getMode());
        assertEquals("[b]", backpackResolved.getValue());
    }

    @Test
    public void resolveLeavesBareDomainAsSuggestCommand() {
        ClickActionResolver.ResolvedClick resolved = ClickActionResolver.resolve("example.com/docs", LINK_REGEX);

        assertEquals(ClickActionResolver.ClickMode.SUGGEST_COMMAND, resolved.getMode());
        assertEquals("example.com/docs", resolved.getValue());
    }
}
