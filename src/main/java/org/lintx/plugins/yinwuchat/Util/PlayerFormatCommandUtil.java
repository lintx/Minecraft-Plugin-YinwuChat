package org.lintx.plugins.yinwuchat.Util;

import java.util.Arrays;

public final class PlayerFormatCommandUtil {

    private PlayerFormatCommandUtil() {
    }

    public static String joinAndFilterContent(String[] args, int startIndex, String denyStyle) {
        if (args == null || startIndex < 0 || args.length <= startIndex) {
            return "";
        }

        String content = String.join(" ", Arrays.copyOfRange(args, startIndex, args.length));
        if (denyStyle == null || denyStyle.isEmpty()) {
            return content;
        }
        for (char styleCode : denyStyle.toCharArray()) {
            content = content.replace("&" + styleCode, "");
            content = content.replace("§" + styleCode, "");
        }
        return content;
    }
}
