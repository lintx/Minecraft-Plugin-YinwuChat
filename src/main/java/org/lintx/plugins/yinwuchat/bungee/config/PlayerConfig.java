package org.lintx.plugins.yinwuchat.bungee.config;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.lintx.plugins.modules.configure.Configure;
import org.lintx.plugins.modules.configure.YamlConfig;
import org.lintx.plugins.yinwuchat.bungee.YinwuChat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class PlayerConfig {
    private static PlayerConfig instance = new PlayerConfig();
    private static Map<UUID,Player> configs = new HashMap<>();
    private static Tokens tokens = null;
    public static PlayerConfig getInstance(){
        return instance;
    }

    public static Player getConfig(ProxiedPlayer player){
        return getInstance().getPlayerConfig(player);
    }

    public static Player getConfig(UUID uuid){
        return getInstance().getPlayerConfig(uuid);
    }

    public static void unloadConfig(ProxiedPlayer player){
        UUID uuid = player.getUniqueId();
        if (configs.containsKey(uuid)){
            Player config = configs.get(uuid);
            config.save();
            configs.remove(uuid);
        }
    }

    private Player getPlayerConfig(UUID uuid){
        Player config = configs.get(uuid);
        if (config==null){
            String path = "player/" + uuid.toString() + ".yml";
            config = new Player(path);
            config.load();
            configs.put(uuid,config);
        }
        return config;
    }

    private Player getPlayerConfig(ProxiedPlayer player){
        UUID uuid = player.getUniqueId();
        Player config = configs.get(uuid);
        if (config==null){
            String path = "player/" + uuid.toString() + ".yml";
            config = new Player(path);
            config.load();
            configs.put(uuid,config);
        }
        if (!config.name.equals(player.getName())){
            config.name = player.getName();
            config.save();
        }
        return config;
    }

    public static Tokens getTokens(){
        return getInstance().getPlayerTokens();
    }

    private Tokens getPlayerTokens(){
        if (tokens==null){
            tokens = new Tokens();
            tokens.load();
            tokens.clearEmptyToken();
            tokens.save();
        }
        return tokens;
    }

    @YamlConfig(path = "player/nouser.yml")
    public static class Player{
        final String path;

        Player(String path){
            this.path = path;
        }

        void load(){
            if (this.path.equals("player/nouser.yml")){
                return;
            }
            Configure.bungeeLoad(YinwuChat.getPlugin(),this,path);
        }

        public void save(){
            Configure.bungeeSave(YinwuChat.getPlugin(),this,path);
        }

        public boolean isCooldown(){
            if (lastTime.isEqual(LocalDateTime.MIN)){
                return false;
            }
            Duration duration = Duration.between(lastTime,LocalDateTime.now());
            return duration.toMillis() < Config.getInstance().atcooldown * 1000;
        }

        public void updateCooldown(){
            lastTime = LocalDateTime.now();
        }

        private LocalDateTime lastTime = LocalDateTime.MIN;

        public boolean isIgnore(ProxiedPlayer player){
            return ignorePlayer.contains(player.getUniqueId());
        }

        public boolean isIgnore(UUID uuid){
            return ignorePlayer.contains(uuid);
        }

        public boolean ignore(UUID uuid){
            if (isIgnore(uuid)){
                ignorePlayer.remove(uuid);
                return false;
            }
            else {
                ignorePlayer.add(uuid);
                return true;
            }
        }

        @YamlConfig
        public boolean vanish = false;

        @YamlConfig
        public boolean monitor = true;

        @YamlConfig
        public boolean muteAt = false;

        @YamlConfig
        public boolean banAt = false;

        @YamlConfig
        List<UUID> ignorePlayer = new ArrayList<>();

        @YamlConfig
        public String name = "";

        @YamlConfig
        public String publicPrefix = "";

        @YamlConfig
        public String publicSuffix = "";

        @YamlConfig
        public String privatePrefix = "";

        @YamlConfig
        public String privateSuffix = "";
    }

    @YamlConfig(path = "tokens.yml")
    public static class Tokens{
        @YamlConfig
        Map<String,String> tokens = new HashMap<>();

        void clearEmptyToken(){
            tokens.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().equals(""));
            save();
        }

        public String newToken(){
            UUID uuid = UUID.randomUUID();
            String token = uuid.toString();
            if (tokens.containsKey(token)){
                return newToken();
            }
            tokens.put(token,"");
            return token;
        }

        public boolean bindToken(String token, ProxiedPlayer player){
            if (tokenNotVaild(token)){
                return false;
            }
            if (tokenIsBind(token)){
                return false;
            }
            tokens.put(token,player.getUniqueId().toString());
            save();
            return true;
        }

        public boolean tokenNotVaild(String token){
            return !tokens.containsKey(token);
        }

        public boolean tokenIsBind(String token){
            return tokens.get(token) != null && !tokens.get(token).equals("");
        }

        public UUID getUuid(String token){
            return UUID.fromString(tokens.get(token));
        }

        public void unbindToken(String token){
            tokens.remove(token);
            save();
        }

        public List<String> tokenWithPlayer(ProxiedPlayer player){
            List<String> list = new ArrayList<>();
            String pid = player.getUniqueId().toString();
            for (Map.Entry<String,String> entry:tokens.entrySet()){
                if (entry.getValue().equals(pid)){
                    list.add(entry.getKey());
                }
            }
            return list;
        }

        public void save(){
            Configure.bungeeSave(YinwuChat.getPlugin(),this);
        }

        void load(){
            Configure.bungeeLoad(YinwuChat.getPlugin(),this);
        }
    }
}
