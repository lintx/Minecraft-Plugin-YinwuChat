package org.lintx.plugins.yinwuchat.Util;

import org.lintx.plugins.yinwuchat.Const;

import java.util.regex.Pattern;

public final class ClickActionResolver {
    private static final Pattern WWW_LINK_PATTERN = Pattern.compile("^www\\.[^\\s/$.?#].[^\\s]*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern POSITION_PLACEHOLDER_PATTERN = Pattern.compile("^\\[p\\]$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ITEM_PLACEHOLDER_PATTERN = Pattern.compile("^" + Const.ITEM_PLACEHOLDER + "$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BACKPACK_PLACEHOLDER_PATTERN = Pattern.compile("^" + Const.BACKPACK_PLACEHOLDER + "$", Pattern.CASE_INSENSITIVE);

    private ClickActionResolver() {
    }

    public static ResolvedClick resolve(String click, String linkRegex) {
        String normalized = click == null ? "" : click.trim();
        if (normalized.isEmpty()) {
            return new ResolvedClick(ClickMode.SUGGEST_COMMAND, "");
        }

        if (matchesConfiguredUrl(normalized, linkRegex)) {
            return new ResolvedClick(ClickMode.OPEN_URL, normalized);
        }

        if (WWW_LINK_PATTERN.matcher(normalized).matches()) {
            return new ResolvedClick(ClickMode.OPEN_URL, "https://" + normalized);
        }

        return new ResolvedClick(ClickMode.SUGGEST_COMMAND, normalized);
    }

    private static boolean matchesConfiguredUrl(String click, String linkRegex) {
        if (linkRegex != null && !linkRegex.isEmpty() && Pattern.compile(linkRegex).matcher(click).find()) {
            return true;
        }
        return false;
    }

    public static boolean isPlaceholderCommand(String click) {
        if (click == null || click.isEmpty()) {
            return false;
        }
        return ITEM_PLACEHOLDER_PATTERN.matcher(click).matches()
                || BACKPACK_PLACEHOLDER_PATTERN.matcher(click).matches()
                || POSITION_PLACEHOLDER_PATTERN.matcher(click).matches();
    }

    public enum ClickMode {
        OPEN_URL,
        SUGGEST_COMMAND
    }

    public static final class ResolvedClick {
        private final ClickMode mode;
        private final String value;

        public ResolvedClick(ClickMode mode, String value) {
            this.mode = mode;
            this.value = value;
        }

        public ClickMode getMode() {
            return mode;
        }

        public String getValue() {
            return value;
        }
    }
}
