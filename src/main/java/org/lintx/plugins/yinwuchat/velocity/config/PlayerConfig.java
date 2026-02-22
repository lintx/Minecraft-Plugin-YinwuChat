package org.lintx.plugins.yinwuchat.velocity.config;

import com.velocitypowered.api.proxy.Player;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Velocity 玩家配置管理系统
 * 管理玩家的个人设置（忽略列表、绑定的 Web token 等）
 */
public class PlayerConfig {
    private static PlayerConfig instance;
    private static final Path CONFIG_DIR = Paths.get("plugins/YinwuChat/players");
    private static final Path TOKENS_FILE = Paths.get("plugins/YinwuChat/tokens.yml");
    
    // 玩家配置缓存
    private final Map<String, PlayerSettings> playerCache = new ConcurrentHashMap<>();
    // Token 管理器
    private final TokenManager tokenManager = new TokenManager();
    
    private PlayerConfig() {
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public TokenManager getTokenManager() {
        return tokenManager;
    }
    
    public static PlayerConfig getInstance() {
        if (instance == null) {
            synchronized (PlayerConfig.class) {
                if (instance == null) {
                    instance = new PlayerConfig();
                }
            }
        }
        return instance;
    }
    
    /**
     * 获取玩家配置
     */
    public PlayerSettings getSettings(String playerName) {
        if (playerName == null) return null;
        String lowerName = playerName.toLowerCase(Locale.ROOT);
        return playerCache.computeIfAbsent(lowerName, name -> loadOrCreateSettings(playerName));
    }
    
    /**
     * 获取玩家配置（从 Player 对象）
     */
    public PlayerSettings getSettings(Player player) {
        return getSettings(player.getUsername());
    }
    
    /**
     * 加载或创建玩家配置
     */
    private PlayerSettings loadOrCreateSettings(String playerName) {
        // 尝试加载现有文件（可能是不同的大小写，但在 Windows 上文件名不敏感，Linux 上敏感）
        // 最好统一使用小写文件名
        String lowerName = playerName.toLowerCase(Locale.ROOT);
        Path playerConfigPath = CONFIG_DIR.resolve(lowerName + ".yml");
        
        if (Files.exists(playerConfigPath)) {
            try (Reader reader = new FileReader(playerConfigPath.toFile())) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(reader);
                if (data != null) {
                    return PlayerSettings.fromMap(playerName, data);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // 创建新配置
        return new PlayerSettings(playerName);
    }
    
    /**
     * 保存玩家配置
     */
    public void saveSettings(PlayerSettings settings) {
        String lowerName = settings.playerName.toLowerCase(Locale.ROOT);
        Path playerConfigPath = CONFIG_DIR.resolve(lowerName + ".yml");
        
        try (Writer writer = new FileWriter(playerConfigPath.toFile())) {
            Yaml yaml = new Yaml();
            yaml.dump(settings.toMap(), writer);
            playerCache.put(lowerName, settings);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 玩家配置数据类
     */
    public static class PlayerSettings {
        public String playerName;
        public Set<String> ignoredPlayers = ConcurrentHashMap.newKeySet();
        public boolean muteAtMention = false;
        public boolean disableAtMention = false;  // 禁止被@
        public boolean vanished = false;  // 隐身模式
        public String bindToken = "";
        public long lastSaveTime = 0;
        
        // 玩家聊天前后缀
        public String publicPrefix = "";   // 公聊前缀
        public String publicSuffix = "";   // 公聊后缀
        public String privatePrefix = "";  // 私聊前缀
        public String privateSuffix = "";  // 私聊后缀
        
        // 突发事件报警
        public long lastAtAllAdmin = 0;     // 上次使用 @所有管理员 的时间戳
        
        // 禁言相关
        public boolean muted = false;       // 是否被禁言
        public long mutedUntil = 0;         // 禁言结束时间（时间戳，0表示永久）
        public String mutedBy = "";         // 禁言操作者
        public String muteReason = "";      // 禁言原因
        
        public PlayerSettings(String playerName) {
            this.playerName = playerName;
        }
        
        /**
         * 检查玩家是否正在被禁言
         */
        public boolean isMuted() {
            if (!muted) {
                return false;
            }
            // 检查是否过期（0表示永久禁言）
            if (mutedUntil > 0 && System.currentTimeMillis() > mutedUntil) {
                // 禁言已过期，自动解除
                muted = false;
                mutedUntil = 0;
                mutedBy = "";
                muteReason = "";
                return false;
            }
            return true;
        }
        
        /**
         * 获取剩余禁言时间（秒）
         * @return 剩余秒数，-1表示永久，0表示未禁言
         */
        public long getRemainingMuteTime() {
            if (!isMuted()) {
                return 0;
            }
            if (mutedUntil == 0) {
                return -1;  // 永久
            }
            return Math.max(0, (mutedUntil - System.currentTimeMillis()) / 1000);
        }
        
        /**
         * 忽略玩家
         */
        public void ignorePlayer(String targetName) {
            if (targetName != null) {
                ignoredPlayers.add(targetName.toLowerCase(Locale.ROOT));
            }
        }
        
        /**
         * 取消忽略玩家
         */
        public void unignorePlayer(String targetName) {
            if (targetName != null) {
                ignoredPlayers.remove(targetName.toLowerCase(Locale.ROOT));
            }
        }
        
        /**
         * 检查是否忽略了某个玩家
         */
        public boolean isIgnored(String playerName) {
            if (playerName == null) return false;
            return ignoredPlayers.contains(playerName.toLowerCase(Locale.ROOT));
        }
        
        /**
         * 转换为 Map（用于 YAML 序列化）
         */
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("player_name", playerName);
            map.put("ignored_players", new ArrayList<>(ignoredPlayers));
            map.put("mute_at_mention", muteAtMention);
            map.put("disable_at_mention", disableAtMention);
            map.put("vanished", vanished);
            map.put("bind_token", bindToken);
            map.put("public_prefix", publicPrefix);
            map.put("public_suffix", publicSuffix);
            map.put("private_prefix", privatePrefix);
            map.put("private_suffix", privateSuffix);
            map.put("last_at_all_admin", lastAtAllAdmin);
            // 禁言相关
            map.put("muted", muted);
            map.put("muted_until", mutedUntil);
            map.put("muted_by", mutedBy);
            map.put("mute_reason", muteReason);
            return map;
        }
        
        /**
         * 从 Map 恢复（用于 YAML 反序列化）
         */
        public static PlayerSettings fromMap(String playerName, Map<String, Object> map) {
            PlayerSettings settings = new PlayerSettings(playerName);
            
            if (map.containsKey("ignored_players")) {
                @SuppressWarnings("unchecked")
                List<String> ignored = (List<String>) map.get("ignored_players");
                if (ignored != null) {
                    settings.ignoredPlayers.addAll(ignored);
                }
            }
            
            if (map.containsKey("mute_at_mention")) {
                Object obj = map.get("mute_at_mention");
                settings.muteAtMention = obj instanceof Boolean ? (Boolean) obj : false;
            }
            
            if (map.containsKey("disable_at_mention")) {
                Object obj = map.get("disable_at_mention");
                settings.disableAtMention = obj instanceof Boolean ? (Boolean) obj : false;
            }
            
            if (map.containsKey("vanished")) {
                Object obj = map.get("vanished");
                settings.vanished = obj instanceof Boolean ? (Boolean) obj : false;
            }
            
            if (map.containsKey("bind_token")) {
                Object obj = map.get("bind_token");
                settings.bindToken = obj != null ? obj.toString() : "";
            }
            
            if (map.containsKey("public_prefix")) {
                Object obj = map.get("public_prefix");
                settings.publicPrefix = obj != null ? obj.toString() : "";
            }
            
            if (map.containsKey("public_suffix")) {
                Object obj = map.get("public_suffix");
                settings.publicSuffix = obj != null ? obj.toString() : "";
            }
            
            if (map.containsKey("private_prefix")) {
                Object obj = map.get("private_prefix");
                settings.privatePrefix = obj != null ? obj.toString() : "";
            }
            
            if (map.containsKey("private_suffix")) {
                Object obj = map.get("private_suffix");
                settings.privateSuffix = obj != null ? obj.toString() : "";
            }
            
            if (map.containsKey("last_at_all_admin")) {
                Object obj = map.get("last_at_all_admin");
                if (obj instanceof Number) {
                    settings.lastAtAllAdmin = ((Number) obj).longValue();
                }
            }
            
            // 禁言相关
            if (map.containsKey("muted")) {
                Object obj = map.get("muted");
                settings.muted = obj instanceof Boolean ? (Boolean) obj : false;
            }
            
            if (map.containsKey("muted_until")) {
                Object obj = map.get("muted_until");
                if (obj instanceof Number) {
                    settings.mutedUntil = ((Number) obj).longValue();
                }
            }
            
            if (map.containsKey("muted_by")) {
                Object obj = map.get("muted_by");
                settings.mutedBy = obj != null ? obj.toString() : "";
            }
            
            if (map.containsKey("mute_reason")) {
                Object obj = map.get("mute_reason");
                settings.muteReason = obj != null ? obj.toString() : "";
            }
            
            return settings;
        }
    }
    
    /**
     * Token 管理器 - 管理 Web 客户端的 Token
     */
    public static class TokenManager {
        private final Map<String, UUID> tokens = Collections.synchronizedMap(new HashMap<>());
        private final Map<UUID, String> uuidToToken = Collections.synchronizedMap(new HashMap<>());
        private final Map<UUID, String> uuidToName = Collections.synchronizedMap(new HashMap<>());
        
        public TokenManager() {
            load();
        }
        
        @SuppressWarnings("unchecked")
        private void load() {
            if (Files.exists(TOKENS_FILE)) {
                try (Reader reader = new FileReader(TOKENS_FILE.toFile())) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> data = yaml.load(reader);
                    if (data != null && data.containsKey("tokens")) {
                        Map<String, String> tokenData = (Map<String, String>) data.get("tokens");
                        if (tokenData != null) {
                            for (Map.Entry<String, String> entry : tokenData.entrySet()) {
                                try {
                                    UUID uuid = UUID.fromString(entry.getValue());
                                    tokens.put(entry.getKey(), uuid);
                                    uuidToToken.put(uuid, entry.getKey());
                                } catch (Exception ignored) {
                                    tokens.put(entry.getKey(), null);
                                }
                            }
                        }
                    }
                    if (data != null && data.containsKey("names")) {
                        Map<String, String> nameData = (Map<String, String>) data.get("names");
                        if (nameData != null) {
                            for (Map.Entry<String, String> entry : nameData.entrySet()) {
                                try {
                                    UUID uuid = UUID.fromString(entry.getKey());
                                    uuidToName.put(uuid, entry.getValue());
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        private void save() {
            try {
                Files.createDirectories(TOKENS_FILE.getParent());
                try (Writer writer = new FileWriter(TOKENS_FILE.toFile())) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> data = new LinkedHashMap<>();
                    Map<String, String> tokenData = new LinkedHashMap<>();
                    synchronized (tokens) {
                        for (Map.Entry<String, UUID> entry : tokens.entrySet()) {
                            tokenData.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : "");
                        }
                    }
                    data.put("tokens", tokenData);
                    Map<String, String> nameData = new LinkedHashMap<>();
                    synchronized (uuidToName) {
                        for (Map.Entry<UUID, String> entry : uuidToName.entrySet()) {
                            nameData.put(entry.getKey().toString(), entry.getValue());
                        }
                    }
                    data.put("names", nameData);
                    yaml.dump(data, writer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        public String newToken() {
            String token = UUID.randomUUID().toString();
            tokens.put(token, null);
            save();
            return token;
        }
        
        public boolean tokenNotValid(String token) {
            return !tokens.containsKey(token);
        }
        
        public boolean tokenIsBind(String token) {
            return tokens.containsKey(token) && tokens.get(token) != null;
        }
        
        public UUID getUuid(String token) {
            return tokens.get(token);
        }
        
        public void bindToken(String token, UUID uuid) {
            bindToken(token, uuid, null);
        }

        public void bindToken(String token, UUID uuid, String name) {
            String oldToken = uuidToToken.get(uuid);
            if (oldToken != null && !oldToken.equals(token)) {
                tokens.remove(oldToken);
            }
            tokens.put(token, uuid);
            uuidToToken.put(uuid, token);
            if (name != null && !name.isEmpty()) {
                uuidToName.put(uuid, name);
            }
            save();
        }
        
        public void unbindToken(String token) {
            UUID uuid = tokens.get(token);
            if (uuid != null) {
                uuidToToken.remove(uuid);
                uuidToName.remove(uuid);
            }
            tokens.put(token, null);
            save();
        }
        
        public String getToken(UUID uuid) {
            return uuidToToken.get(uuid);
        }

        public String getName(UUID uuid) {
            return uuidToName.get(uuid);
        }

        public List<String> getAllNames() {
            return new ArrayList<>(uuidToName.values());
        }


        public UUID getUuidByName(String name) {
            if (name == null) return null;
            for (Map.Entry<UUID, String> entry : uuidToName.entrySet()) {
                if (name.equalsIgnoreCase(entry.getValue())) {
                    return entry.getKey();
                }
            }
            return null;
        }

        public void removeUuid(UUID uuid) {
            if (uuid == null) return;
            uuidToToken.remove(uuid);
            int removed = 0;
            synchronized (tokens) {
                tokens.entrySet().removeIf(entry -> {
                    return uuid.equals(entry.getValue());
                });
            }
            uuidToName.remove(uuid);
            save();
        }
    }
}
