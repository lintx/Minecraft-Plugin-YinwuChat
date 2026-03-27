package org.lintx.plugins.yinwuchat.Util;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class BackpackViewCommandUtil {

    private BackpackViewCommandUtil() {
    }

    public static List<String> suggestTargets(String prefix, Collection<String> playerNames) {
        String normalizedPrefix = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return playerNames.stream()
                .filter(name -> name != null && !name.isEmpty())
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
