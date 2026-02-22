package org.lintx.plugins.yinwuchat;

public class Const {
    public final static String ITEM_PLACEHOLDER = "\\[i([:：]?)(\\d+)?\\]";
    public final static String INVENTORY_PLACEHOLDER = "\\[inv([:：]?)([^\\]]*)\\]";
    public final static String ENDER_CHEST_PLACEHOLDER = "\\[ec([:：]?)([^\\]]*)\\]";
    public final static String PLUGIN_CHANNEL = "bungeecord";
    public final static String PLUGIN_CHANNEL_VELOCITY = "bungeecord";
    public final static String PLUGIN_CHANNEL_BUKKIT = "yinwuchat:main";
    public final static String PLUGIN_SUB_CHANNEL_AT = "org.lintx.plugins.yinwuchat:at";
    public final static String PLUGIN_SUB_CHANNEL_PLAYER_LIST = "org.lintx.plugins.yinwuchat:player_list";
    public final static String PLUGIN_SUB_CHANNEL_MESSAGE = "org.lintx.plugins.yinwuchat:chat";
    public final static String PLUGIN_SUB_CHANNEL_PUBLIC_MESSAGE = "org.lintx.plugins.yinwuchat:chat";
    public final static String PLUGIN_SUB_CHANNEL_PRIVATE_MESSAGE = "org.lintx.plugins.yinwuchat:msg";
    public final static String PLUGIN_SUB_CHANNEL_ITEM_REQUEST = "org.lintx.plugins.yinwuchat:item_request";
    public final static String PLUGIN_SUB_CHANNEL_ITEM_RESPONSE = "org.lintx.plugins.yinwuchat:item_response";
    
    // 物品展示相关的子通道
    public final static String PLUGIN_SUB_CHANNEL_ITEM_DISPLAY_STORE = "org.lintx.plugins.yinwuchat:item_display_store";
    public final static String PLUGIN_SUB_CHANNEL_ITEM_DISPLAY_REQUEST = "org.lintx.plugins.yinwuchat:item_display_request";
    public final static String PLUGIN_SUB_CHANNEL_ITEM_DISPLAY_RESPONSE = "org.lintx.plugins.yinwuchat:item_display_response";

    public final static String PERMISSION_ADMIN = "yinwuchat.admin";
    public final static String PERMISSION_RELOAD = "yinwuchat.admin.reload";
    public final static String PERMISSION_BAD_WORD = "yinwuchat.admin.badword";
    public final static String PERMISSION_AT_ALL = "yinwuchat.admin.atall";
    public final static String PERMISSION_VANISH = "yinwuchat.admin.vanish";
    public final static String PERMISSION_MUTE = "yinwuchat.admin.mute";
    public final static String PERMISSION_MONITOR_PRIVATE_MESSAGE = "yinwuchat.admin.monitor";
    public final static String PERMISSION_AT_ALL_ADMIN_RESET = "yinwuchat.admin.atalladmin.reset";
    public final static String PERMISSION_COOL_DOWN_BYPASS = "yinwuchat.admin.cooldown.bypass";
    
    public final static String PERMISSION_DEFAULT = "yinwuchat.default";
    public final static String PERMISSION_WS = "yinwuchat.default.ws";
    public final static String PERMISSION_BIND = "yinwuchat.default.bind";
    public final static String PERMISSION_LIST = "yinwuchat.default.list";
    public final static String PERMISSION_UNBIND = "yinwuchat.default.unbind";
    public final static String PERMISSION_IGNORE = "yinwuchat.default.ignore";
    public final static String PERMISSION_NOAT = "yinwuchat.default.noat";
    public final static String PERMISSION_MUTEAT = "yinwuchat.default.muteat";
    public final static String PERMISSION_FORMAT = "yinwuchat.default.format";
    public final static String PERMISSION_MSG = "yinwuchat.default.msg";
    public final static String PERMISSION_AT_ALL_ADMIN = "yinwuchat.default.atalladmin";
    public final static String PERMISSION_ITEM_DISPLAY = "yinwuchat.default.itemdisplay";
    public final static String PERMISSION_QQ = "yinwuchat.default.qq";
    public final static String PERMISSION_STYLE = "yinwuchat.style";

    public final static String[] ADMIN_PERMISSION_NODES = new String[] {
        PERMISSION_ADMIN,
        PERMISSION_RELOAD,
        PERMISSION_BAD_WORD,
        PERMISSION_AT_ALL,
        PERMISSION_VANISH,
        PERMISSION_MUTE,
        PERMISSION_MONITOR_PRIVATE_MESSAGE,
        PERMISSION_AT_ALL_ADMIN_RESET,
        PERMISSION_COOL_DOWN_BYPASS
    };

    public final static String[] DEFAULT_PERMISSION_NODES = new String[] {
        PERMISSION_DEFAULT,
        PERMISSION_WS,
        PERMISSION_BIND,
        PERMISSION_LIST,
        PERMISSION_UNBIND,
        PERMISSION_IGNORE,
        PERMISSION_NOAT,
        PERMISSION_MUTEAT,
        PERMISSION_FORMAT,
        PERMISSION_MSG,
        PERMISSION_AT_ALL_ADMIN,
        PERMISSION_ITEM_DISPLAY,
        PERMISSION_QQ,
        PERMISSION_STYLE,
        "yinwuchat.style.0",
        "yinwuchat.style.1",
        "yinwuchat.style.2",
        "yinwuchat.style.3",
        "yinwuchat.style.4",
        "yinwuchat.style.5",
        "yinwuchat.style.6",
        "yinwuchat.style.7",
        "yinwuchat.style.8",
        "yinwuchat.style.9",
        "yinwuchat.style.a",
        "yinwuchat.style.b",
        "yinwuchat.style.c",
        "yinwuchat.style.d",
        "yinwuchat.style.e",
        "yinwuchat.style.f",
        "yinwuchat.style.k",
        "yinwuchat.style.l",
        "yinwuchat.style.m",
        "yinwuchat.style.n",
        "yinwuchat.style.o",
        "yinwuchat.style.r",
        "yinwuchat.style.rgb"
    };
}
