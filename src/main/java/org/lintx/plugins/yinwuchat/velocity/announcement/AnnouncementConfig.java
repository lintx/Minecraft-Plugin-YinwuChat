package org.lintx.plugins.yinwuchat.velocity.announcement;

import org.lintx.plugins.yinwuchat.json.MessageFormat;
import org.lintx.plugins.yinwuchat.velocity.YinwuChat;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 广播配置管理器，负责加载/保存 broadcast.yml
 */
public class AnnouncementConfig {
    private static final AnnouncementConfig instance = new AnnouncementConfig();

    public static AnnouncementConfig getInstance() {
        return instance;
    }

    /** 所有广播任务列表 */
    public List<AnnouncementTaskConfig> tasks = new ArrayList<>();

    /**
     * 从 broadcast.yml 加载配置，文件不存在时生成默认示例
     */
    @SuppressWarnings("unchecked")
    public void load(YinwuChat plugin) {
        tasks = new ArrayList<>();

        File configDir = plugin.getDataFolder().toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File file = new File(configDir, "broadcast.yml");

        if (file.exists()) {
            try (InputStream inputStream = new FileInputStream(file)) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(inputStream);
                if (data != null && data.containsKey("tasks")) {
                    Object tasksObj = data.get("tasks");
                    if (tasksObj instanceof List) {
                        for (Object item : (List<?>) tasksObj) {
                            if (item instanceof Map) {
                                tasks.add(loadTaskConfig((Map<String, Object>) item));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warn("Failed to load broadcast.yml: " + e.getMessage());
            }
        }

        // 文件不存在时生成带注释的默认配置
        if (!file.exists()) {
            createDefaults();
            saveWithComments(plugin);
        }
    }

    /**
     * 保存配置到 broadcast.yml（不含注释，用于 reload 后的正常保存）
     */
    public void save(YinwuChat plugin) {
        File configDir = plugin.getDataFolder().toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File file = new File(configDir, "broadcast.yml");

        try {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            options.setIndent(2);

            Yaml yaml = new Yaml(options);
            Map<String, Object> data = new LinkedHashMap<>();
            List<Map<String, Object>> tasksList = new ArrayList<>();
            for (AnnouncementTaskConfig task : tasks) {
                tasksList.add(taskConfigToMap(task));
            }
            data.put("tasks", tasksList);

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                yaml.dump(data, writer);
            }
        } catch (Exception e) {
            plugin.getLogger().warn("Failed to save broadcast.yml: " + e.getMessage());
        }
    }

    /**
     * 首次生成带中文注释的默认配置文件
     */
    private void saveWithComments(YinwuChat plugin) {
        File configDir = plugin.getDataFolder().toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File file = new File(configDir, "broadcast.yml");

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            sb.append("# ==========================================\n");
            sb.append("# YinwuChat 定期广播配置文件\n");
            sb.append("# ==========================================\n");
            sb.append("# 每条广播任务是 tasks 列表中的一个条目\n");
            sb.append("# 修改后执行 /yinwuchat reload 即可热重载\n");
            sb.append("#\n");
            sb.append("# 字段说明:\n");
            sb.append("#   enable         - 是否启用该广播 (true/false)\n");
            sb.append("#   interval       - 广播间隔，单位秒 (300 = 5分钟)\n");
            sb.append("#   web            - 是否发送到Web端 (true/false)\n");
            sb.append("#   includeMode    - 白名单模式 (true = 仅 includeServers 中的服务器接收)\n");
            sb.append("#   includeServers - 白名单服务器列表\n");
            sb.append("#   excludeMode    - 黑名单模式 (true = excludeServers 中的服务器不接收)\n");
            sb.append("#   excludeServers - 黑名单服务器列表\n");
            sb.append("#   list           - 广播内容列表\n");
            sb.append("#     message      - 显示文本 (支持&颜色码: &e黄 &b青 &a绿 &c红 &r重置 等)\n");
            sb.append("#     hover        - 鼠标悬停提示文本 (可选)\n");
            sb.append("#     click        - 点击后填入聊天栏的命令 (可选)\n");
            sb.append("#\n");
            sb.append("# 注意: includeMode 和 excludeMode 都关闭时，所有游戏服务器都接收\n");
            sb.append("#       两者同时开启时 includeMode 优先\n");
            sb.append("#       web 字段独立控制Web端，不受上述两种模式影响\n");
            sb.append("# ==========================================\n");
            sb.append("\n");
            sb.append("tasks:\n");

            for (int i = 0; i < tasks.size(); i++) {
                AnnouncementTaskConfig task = tasks.get(i);
                if (i > 0) sb.append("\n");
                sb.append("  # --- 广播任务 ").append(i + 1).append(" ---\n");
                sb.append("  - enable: ").append(task.enable).append("\n");
                sb.append("    interval: ").append(task.interval).append("\n");
                sb.append("    web: ").append(task.web).append("\n");
                sb.append("    # 白名单模式: 开启后仅 includeServers 中的服务器接收\n");
                sb.append("    includeMode: ").append(task.includeMode).append("\n");
                sb.append("    includeServers: ").append(formatStringList(task.includeServers)).append("\n");
                sb.append("    # 黑名单模式: 开启后 excludeServers 中的服务器不接收，其余都接收\n");
                sb.append("    excludeMode: ").append(task.excludeMode).append("\n");
                sb.append("    excludeServers: ").append(formatStringList(task.excludeServers)).append("\n");
                sb.append("    list:\n");
                for (MessageFormat format : task.list) {
                    sb.append("      - message: \"").append(escapeYaml(format.message)).append("\"\n");
                    if (format.hover != null && !format.hover.isEmpty()) {
                        sb.append("        hover: \"").append(escapeYaml(format.hover)).append("\"\n");
                    }
                    if (format.click != null && !format.click.isEmpty()) {
                        sb.append("        click: \"").append(escapeYaml(format.click)).append("\"\n");
                    }
                }
            }

            writer.write(sb.toString());
        } catch (Exception e) {
            plugin.getLogger().warn("Failed to save broadcast.yml: " + e.getMessage());
        }
    }

    /**
     * 格式化字符串列表为 YAML 行内格式，如 [sur, lobby] 或 []
     */
    private String formatStringList(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(list.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 转义 YAML 字符串中的特殊字符
     */
    private String escapeYaml(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 生成默认示例广播任务
     */
    private void createDefaults() {
        // 示例1：物品展示帮助
        AnnouncementTaskConfig task1 = new AnnouncementTaskConfig();
        task1.enable = true;
        task1.interval = 300;
        task1.web = true;
        task1.list.add(new MessageFormat("&e[帮助]", "服务器帮助文档", "/help"));
        task1.list.add(new MessageFormat("&r 在聊天中输入"));
        task1.list.add(new MessageFormat("&b[i]", "在聊天文本中包含这三个字符即可", "[i]"));
        task1.list.add(new MessageFormat("&r可以展示你手中的物品，输入"));
        task1.list.add(new MessageFormat("&b[i:x]", "&b:&r冒号不区分中英文\n&bx&r为背包格子编号\n物品栏为0-8，然后从背包左上角\n从左至右从上至下为9-35\n装备栏为36-39，副手为40", "[i:0]"));
        task1.list.add(new MessageFormat("&r可以展示背包中x位置对应的物品，一条消息中可以展示多个物品"));
        tasks.add(task1);

        // 示例2：@功能提示
        AnnouncementTaskConfig task2 = new AnnouncementTaskConfig();
        task2.enable = true;
        task2.interval = 300;
        task2.web = true;
        task2.list.add(new MessageFormat("&e[帮助]", "服务器帮助文档", "/help"));
        task2.list.add(new MessageFormat("&r 在聊天中输入&b@&r然后后面跟上"));
        task2.list.add(new MessageFormat("&b玩家名", "不区分服务器\n不区分大小写\n可以只输入玩家名的前n个字符\n玩家名后需要跟中文或空格", ""));
        task2.list.add(new MessageFormat("&r即可@该玩家，如果不想被别人@可以输入"));
        task2.list.add(new MessageFormat("&b/yinwuchat noat", "点击输入命令", "/yinwuchat noat"));
        task2.list.add(new MessageFormat("&r命令来切换自己是否允许被他人@"));
        tasks.add(task2);
    }

    /**
     * 从 YAML Map 加载单个任务配置，兼容新旧格式
     */
    @SuppressWarnings("unchecked")
    private AnnouncementTaskConfig loadTaskConfig(Map<String, Object> data) {
        AnnouncementTaskConfig config = new AnnouncementTaskConfig();
        if (data.containsKey("enable")) config.enable = (Boolean) data.get("enable");
        if (data.containsKey("interval")) config.interval = ((Number) data.get("interval")).intValue();

        // 新格式字段
        if (data.containsKey("web")) config.web = (Boolean) data.get("web");
        if (data.containsKey("includeMode")) config.includeMode = (Boolean) data.get("includeMode");
        if (data.containsKey("includeServers")) {
            Object obj = data.get("includeServers");
            if (obj instanceof List) {
                config.includeServers = new ArrayList<>();
                for (Object item : (List<?>) obj) {
                    if (item != null) config.includeServers.add(item.toString());
                }
            }
        }
        if (data.containsKey("excludeMode")) config.excludeMode = (Boolean) data.get("excludeMode");
        if (data.containsKey("excludeServers")) {
            Object obj = data.get("excludeServers");
            if (obj instanceof List) {
                config.excludeServers = new ArrayList<>();
                for (Object item : (List<?>) obj) {
                    if (item != null) config.excludeServers.add(item.toString());
                }
            }
        }

        // 向后兼容旧的 server 字段
        if (data.containsKey("server") && !data.containsKey("includeMode")) {
            String server = (String) data.get("server");
            if ("all".equalsIgnoreCase(server)) {
                config.web = true;
            } else if ("web".equalsIgnoreCase(server)) {
                config.web = true;
                // Web-only 模式：排除所有游戏服务器（无法精确列举，用 include 空列表实现）
                config.includeMode = true;
                config.includeServers = new ArrayList<>();
            } else {
                // 单个服务器名
                config.includeMode = true;
                config.includeServers = new ArrayList<>();
                config.includeServers.add(server);
            }
        }

        if (data.containsKey("list")) {
            Object listObj = data.get("list");
            if (listObj instanceof List) {
                config.list = loadMessageFormatList((List<Object>) listObj);
            }
        }
        return config;
    }

    /**
     * 从 YAML 列表加载 MessageFormat 列表
     */
    @SuppressWarnings("unchecked")
    private List<MessageFormat> loadMessageFormatList(List<Object> list) {
        List<MessageFormat> result = new ArrayList<>();
        if (list == null) return result;
        for (Object item : list) {
            if (item instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) item;
                String message = (String) map.getOrDefault("message", "");
                String hover = (String) map.getOrDefault("hover", "");
                String click = (String) map.getOrDefault("click", "");
                result.add(new MessageFormat(message, hover, click));
            } else if (item instanceof String) {
                result.add(new MessageFormat((String) item));
            }
        }
        return result;
    }

    /**
     * 将单个任务配置转为 YAML Map
     */
    private Map<String, Object> taskConfigToMap(AnnouncementTaskConfig task) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("enable", task.enable);
        map.put("interval", task.interval);
        map.put("web", task.web);
        map.put("includeMode", task.includeMode);
        map.put("includeServers", task.includeServers);
        map.put("excludeMode", task.excludeMode);
        map.put("excludeServers", task.excludeServers);

        List<Map<String, String>> listData = new ArrayList<>();
        for (MessageFormat format : task.list) {
            Map<String, String> fmap = new LinkedHashMap<>();
            fmap.put("message", format.message);
            if (format.hover != null && !format.hover.isEmpty()) fmap.put("hover", format.hover);
            if (format.click != null && !format.click.isEmpty()) fmap.put("click", format.click);
            listData.add(fmap);
        }
        map.put("list", listData);
        return map;
    }
}
