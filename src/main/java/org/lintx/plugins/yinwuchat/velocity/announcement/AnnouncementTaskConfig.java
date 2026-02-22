package org.lintx.plugins.yinwuchat.velocity.announcement;

import org.lintx.plugins.yinwuchat.json.MessageFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 单条广播任务的配置模型
 */
public class AnnouncementTaskConfig {
    /** 是否启用该广播任务 */
    public boolean enable = false;

    /** 广播间隔（秒） */
    public int interval = 300;

    /** 是否发送到Web端（独立控制） */
    public boolean web = true;

    /** 白名单模式开关：开启后仅 includeServers 中的服务器接收 */
    public boolean includeMode = false;

    /** 白名单服务器列表 */
    public List<String> includeServers = new ArrayList<>();

    /** 黑名单模式开关：开启后 excludeServers 中的服务器不接收，其余都接收 */
    public boolean excludeMode = false;

    /** 黑名单服务器列表 */
    public List<String> excludeServers = new ArrayList<>();

    /** 广播内容列表，每个 MessageFormat 包含 message / hover / click */
    public List<MessageFormat> list = new ArrayList<>();

    /** 运行时记录上次广播时间（不序列化到YAML） */
    transient LocalDateTime lastTime = null;

    /**
     * 判断指定服务器是否应该接收该广播
     * includeMode 优先于 excludeMode；两者都关闭时全服接收
     */
    public boolean shouldSendToServer(String serverName) {
        if (includeMode) {
            return includeServers.stream().anyMatch(s -> s.equalsIgnoreCase(serverName));
        }
        if (excludeMode) {
            return excludeServers.stream().noneMatch(s -> s.equalsIgnoreCase(serverName));
        }
        return true;
    }
}
