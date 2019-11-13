package org.lintx.plugins.yinwuchat.Util;

public class MessageUtil {
    public static String filter(String message,String denyStyle){
        message = message.replaceAll("&[" + denyStyle + "]","");
        message = message.replaceAll("§[" + denyStyle + "]","");
        return message;
    }

    public static String replace(String str){
        return str.replaceAll("&([0-9a-fklmnor])","§$1");
    }

    public static String removeEmoji(String str){
        return str.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]","");
    }
}
