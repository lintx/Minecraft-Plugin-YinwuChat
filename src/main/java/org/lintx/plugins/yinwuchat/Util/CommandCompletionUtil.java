package org.lintx.plugins.yinwuchat.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CommandCompletionUtil {
    public static final List<String> DURATION_TEMPLATES = List.of("10m", "30m", "1h", "6h", "1d", "7d", "30d");
    public static final List<String> RELOAD_TARGETS = List.of("config", "ws");
    public static final List<String> WEBBIND_ACTIONS = List.of("query", "unbind");
    public static final List<String> BADWORD_ACTIONS = List.of("add", "remove", "list");
    public static final List<String> ATALLADMIN_ACTIONS = List.of("confirm");
    public static final List<String> FORMAT_ROOT_ACTIONS = List.of("edit", "show", "public", "private");
    public static final List<String> FORMAT_SCOPES = List.of("public", "private");
    public static final List<String> FORMAT_POSITIONS = List.of("prefix", "suffix");
    public static final List<String> FORMAT_ACTIONS_VELOCITY = List.of("set", "clear");
    public static final List<String> FORMAT_ACTIONS_BUNGEE = List.of("set", "clear");

    private CommandCompletionUtil() {
    }

    public static List<String> filterByPrefix(Collection<String> candidates, String prefix) {
        String normalizedPrefix = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> results = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (String candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            if (!seen.add(candidate)) {
                continue;
            }
            if (candidate.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                results.add(candidate);
            }
        }
        return results;
    }
}
