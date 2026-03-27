package org.lintx.plugins.yinwuchat.bukkit.display;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BackpackDisplayLayoutTest {

    @Test
    public void mapsInventorySectionsIntoFixedChestSlots() {
        List<String> storage = Arrays.asList(
                "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8",
                "s9", "s10", "s11", "s12", "s13", "s14", "s15", "s16", "s17",
                "s18", "s19", "s20", "s21", "s22", "s23", "s24", "s25", "s26"
        );
        List<String> hotbar = Arrays.asList("h0", "h1", "h2", "h3", "h4", "h5", "h6", "h7", "h8");
        List<String> armor = Arrays.asList("helmet", "chest", "legs", "boots");

        List<String> slots = BackpackDisplayLayout.buildChestSlots(storage, hotbar, armor, "offhand");

        assertEquals(54, slots.size());
        assertEquals("s0", slots.get(9));
        assertEquals("s26", slots.get(35));
        assertEquals("h0", slots.get(36));
        assertEquals("h8", slots.get(44));
        assertEquals("helmet", slots.get(45));
        assertEquals("chest", slots.get(46));
        assertEquals("legs", slots.get(47));
        assertEquals("boots", slots.get(48));
        assertEquals("offhand", slots.get(49));
        assertNull(slots.get(0));
        assertNull(slots.get(53));
    }
}
