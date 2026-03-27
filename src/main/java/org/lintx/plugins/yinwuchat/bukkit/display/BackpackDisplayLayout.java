package org.lintx.plugins.yinwuchat.bukkit.display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BackpackDisplayLayout {
    public static final int CHEST_SIZE = 54;
    public static final int STORAGE_START = 9;
    public static final int HOTBAR_START = 36;
    public static final int ARMOR_START = 45;
    public static final int OFFHAND_SLOT = 49;

    private BackpackDisplayLayout() {
    }

    public static List<String> buildChestSlots(List<String> storage, List<String> hotbar, List<String> armor, String offhand) {
        List<String> slots = new ArrayList<>(Collections.nCopies(CHEST_SIZE, null));
        writeSection(slots, STORAGE_START, storage, 27);
        writeSection(slots, HOTBAR_START, hotbar, 9);
        writeSection(slots, ARMOR_START, armor, 4);
        if (offhand != null && !offhand.isEmpty()) {
            slots.set(OFFHAND_SLOT, offhand);
        }
        return slots;
    }

    private static void writeSection(List<String> slots, int start, List<String> values, int maxSize) {
        if (values == null) {
            return;
        }
        for (int i = 0; i < values.size() && i < maxSize; i++) {
            slots.set(start + i, values.get(i));
        }
    }
}
