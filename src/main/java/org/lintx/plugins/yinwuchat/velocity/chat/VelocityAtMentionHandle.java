package org.lintx.plugins.yinwuchat.velocity.chat;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.lintx.plugins.yinwuchat.velocity.YinwuChat;
import org.lintx.plugins.yinwuchat.velocity.config.Config;
import org.lintx.plugins.yinwuchat.velocity.config.PlayerConfig;
import org.lintx.plugins.yinwuchat.velocity.util.RedisUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 处理聊天消息中的 @ 提及功能
 * 支持 @玩家名 的模糊匹配（不完整的玩家名也可以匹配）
 */
public class VelocityAtMentionHandle extends VelocityChatHandle {
    
    // @提及的正则表达式：@ 后跟 1-16 个有效的玩家名字符
    private static final Pattern AT_PATTERN = Pattern.compile("@([a-zA-Z0-9_]{1,16})");
    
    // 存储每条消息被@的玩家列表（用于后续发送声音提示）
    private static final Map<UUID, Set<Player>> mentionedPlayersMap = new ConcurrentHashMap<>();
    
    // AT 冷却时间记录（玩家UUID -> 上次AT时间戳）
    private static final Map<UUID, Long> atCooldowns = new ConcurrentHashMap<>();
    
    @Override
    public void handle(VelocityChat chat) {
        if (chat.chat == null || chat.chat.isEmpty()) {
            return;
        }
        
        // 获取发送者信息
        String senderName = chat.fromPlayer != null ? chat.fromPlayer.playerName : null;
        UUID senderUUID = getSenderUUID(senderName);
        
        // 存储本次消息被@的玩家名
        Set<String> mentionedPlayerNames = new HashSet<>();
        
        // 检查是否是 @all 提及
        Config config = Config.getInstance();
        String atAllKey = config.atAllKey;
        
        // 使用 handle 方法处理 @ 模式
        handle(chat, AT_PATTERN.pattern(), matcher -> {
            String mentionName = matcher.group(1);
            
            // 检查是否是 @all
            if (atAllKey != null && !atAllKey.isEmpty() && mentionName.equalsIgnoreCase(atAllKey)) {
                return handleAtAll(senderName, senderUUID, mentionedPlayerNames);
            }
            
            // 查找匹配的玩家名
            String matchedPlayerName = findPlayerNameByPartial(mentionName);
            
            if (matchedPlayerName != null) {
                // 检查目标玩家是否禁用了@提及、忽略了发送者或处于隐身状态
                PlayerConfig.PlayerSettings targetSettings = PlayerConfig.getInstance().getSettings(matchedPlayerName);
                if (targetSettings.vanished) {
                    // 如果目标玩家处于隐身状态，将其视为不在线
                    return Component.text("@" + mentionName)
                            .color(NamedTextColor.GRAY);
                }
                
                // 检查是否@自己
                if (matchedPlayerName.equalsIgnoreCase(senderName)) {
                    // @自己时，返回普通样式
                    return Component.text("@" + matchedPlayerName)
                            .color(NamedTextColor.GRAY);
                }
                
                // 检查目标玩家是否禁用了@提及或忽略了发送者
                if (targetSettings.isIgnored(senderName)) {
                    // 如果目标玩家忽略了发送者，不显示高亮@
                    return Component.text("@" + matchedPlayerName)
                            .color(NamedTextColor.GRAY);
                }
                
                if (targetSettings.disableAtMention) {
                    // 被@的玩家不想被打扰
                    String banatTip = Config.getInstance().tipsConfig.banatTip;
                    return Component.text("@" + matchedPlayerName)
                            .color(NamedTextColor.GRAY)
                            .hoverEvent(HoverEvent.showText(
                                    net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(banatTip)));
                }
                
                // 添加到被@玩家列表
                mentionedPlayerNames.add(matchedPlayerName);
                
                // 创建高亮的@组件
                return createMentionComponent(matchedPlayerName);
            } else {
                // 没有找到匹配的玩家，检查 Redis 中的玩家列表
                String redisPlayer = findPlayerInRedis(mentionName);
                if (redisPlayer != null) {
                    // 找到了跨集群的玩家
                    return Component.text("@" + redisPlayer)
                            .color(NamedTextColor.AQUA)
                            .decorate(TextDecoration.BOLD)
                            .hoverEvent(HoverEvent.showText(
                                    Component.text("点击私聊", NamedTextColor.YELLOW)))
                            .clickEvent(ClickEvent.suggestCommand("/msg " + redisPlayer + " "));
                }
                
                // 完全没有找到匹配的玩家，返回原始文本
                return Component.text("@" + mentionName)
                        .color(NamedTextColor.GRAY);
            }
        });
        
        // 存储被@的玩家列表，供后续发送声音提示使用
        if (senderUUID != null && !mentionedPlayerNames.isEmpty()) {
            Set<Player> players = new HashSet<>();
            for (String name : mentionedPlayerNames) {
                YinwuChat.getInstance().getProxy().getPlayer(name).ifPresent(players::add);
            }
            if (!players.isEmpty()) {
                mentionedPlayersMap.put(senderUUID, players);
            }
        }
    }
    
    /**
     * 处理 @all 提及
     */
    private Component handleAtAll(String senderName, UUID senderUUID, Set<String> mentionedPlayerNames) {
        YinwuChat plugin = YinwuChat.getInstance();
        if (plugin == null) {
            return Component.text("@all").color(NamedTextColor.GRAY);
        }
        
        // 检查权限
        Optional<Player> senderOpt = senderName != null ? 
                plugin.getProxy().getPlayer(senderName) : Optional.empty();
        
        if (senderOpt.isEmpty() || !senderOpt.get().hasPermission(org.lintx.plugins.yinwuchat.Const.PERMISSION_AT_ALL)) {
            return Component.text("@all")
                    .color(NamedTextColor.GRAY)
                    .hoverEvent(HoverEvent.showText(
                            Component.text("你没有@所有人的权限", NamedTextColor.RED)));
        }
        
        // 添加所有在线玩家（除了发送者）
        for (Player player : plugin.getProxy().getAllPlayers()) {
            if (!player.getUsername().equalsIgnoreCase(senderName)) {
                PlayerConfig.PlayerSettings settings = PlayerConfig.getInstance().getSettings(player);
                if (!settings.disableAtMention) {
                    mentionedPlayerNames.add(player.getUsername());
                }
            }
        }
        
        // 添加所有 Web 在线玩家
        for (io.netty.channel.Channel wsChannel : org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.getChannels()) {
            org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientUtil util = 
                org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.get(wsChannel);
            if (util != null && util.getUuid() != null) {
                String name = PlayerConfig.getInstance().getTokenManager().getName(util.getUuid());
                if (name != null && !name.equalsIgnoreCase(senderName)) {
                    PlayerConfig.PlayerSettings settings = PlayerConfig.getInstance().getSettings(name);
                    if (!settings.disableAtMention) {
                        mentionedPlayerNames.add(name);
                    }
                }
            }
        }
        
        return Component.text("@全体成员")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(
                        Component.text("@所有在线玩家", NamedTextColor.YELLOW)));
    }
    
    /**
     * 创建@提及的高亮组件
     */
    private Component createMentionComponent(String playerName) {
        return Component.text("@" + playerName)
                .color(NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(
                        Component.text("点击私聊 " + playerName, NamedTextColor.YELLOW)))
                .clickEvent(ClickEvent.suggestCommand("/msg " + playerName + " "));
    }
    
    /**
     * 根据部分名称查找玩家（模糊匹配），支持游戏内和 Web
     */
    private String findPlayerNameByPartial(String partialName) {
        YinwuChat plugin = YinwuChat.getInstance();
        if (plugin == null) {
            return null;
        }
        
        String lowerPartialName = partialName.toLowerCase();
        String exactMatch = null;
        String prefixMatch = null;
        
        // 检查游戏内玩家
        for (Player player : plugin.getProxy().getAllPlayers()) {
            String name = player.getUsername();
            if (name.equalsIgnoreCase(partialName)) return name;
            if (name.toLowerCase().startsWith(lowerPartialName) && prefixMatch == null) prefixMatch = name;
        }
        
        // 检查 Web 玩家
        for (io.netty.channel.Channel wsChannel : org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.getChannels()) {
            org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientUtil util = 
                org.lintx.plugins.yinwuchat.velocity.httpserver.VelocityWsClientHelper.get(wsChannel);
            if (util != null && util.getUuid() != null) {
                String name = PlayerConfig.getInstance().getTokenManager().getName(util.getUuid());
                if (name != null) {
                    if (name.equalsIgnoreCase(partialName)) return name;
                    if (name.toLowerCase().startsWith(lowerPartialName) && prefixMatch == null) prefixMatch = name;
                }
            }
        }
        
        return exactMatch != null ? exactMatch : prefixMatch;
    }
    
    /**
     * 在 Redis 玩家列表中查找玩家
     */
    private String findPlayerInRedis(String partialName) {
        if (!Config.getInstance().redisConfig.openRedis || !RedisUtil.isConnected()) {
            return null;
        }
        
        String lowerPartialName = partialName.toLowerCase();
        String exactMatch = null;
        String prefixMatch = null;
        
        for (String playerName : RedisUtil.playerList.keySet()) {
            String lowerPlayerName = playerName.toLowerCase();
            
            if (lowerPlayerName.equals(lowerPartialName)) {
                exactMatch = playerName;
                break;
            }
            
            if (lowerPlayerName.startsWith(lowerPartialName) && prefixMatch == null) {
                prefixMatch = playerName;
            }
        }
        
        return exactMatch != null ? exactMatch : prefixMatch;
    }
    
    /**
     * 获取发送者的 UUID
     */
    private UUID getSenderUUID(String senderName) {
        if (senderName == null) {
            return null;
        }
        
        YinwuChat plugin = YinwuChat.getInstance();
        if (plugin == null) {
            return null;
        }
        
        return plugin.getProxy().getPlayer(senderName)
                .map(Player::getUniqueId)
                .orElse(null);
    }
    
    /**
     * 检查玩家是否在 AT 冷却中
     * @param playerUUID 玩家 UUID
     * @return true 表示在冷却中，false 表示可以 AT
     */
    public static boolean isInCooldown(UUID playerUUID) {
        if (playerUUID == null) {
            return false;
        }
        
        Long lastAtTime = atCooldowns.get(playerUUID);
        if (lastAtTime == null) {
            return false;
        }
        
        long cooldownMs = Config.getInstance().atcooldown * 1000L;
        return System.currentTimeMillis() - lastAtTime < cooldownMs;
    }
    
    /**
     * 获取剩余冷却时间（秒）
     */
    public static long getRemainingCooldown(UUID playerUUID) {
        if (playerUUID == null) {
            return 0;
        }
        
        Long lastAtTime = atCooldowns.get(playerUUID);
        if (lastAtTime == null) {
            return 0;
        }
        
        long cooldownMs = Config.getInstance().atcooldown * 1000L;
        long elapsed = System.currentTimeMillis() - lastAtTime;
        long remaining = cooldownMs - elapsed;
        
        return remaining > 0 ? remaining / 1000 : 0;
    }
    
    /**
     * 更新玩家的 AT 冷却时间
     */
    public static void updateCooldown(UUID playerUUID) {
        if (playerUUID != null) {
            atCooldowns.put(playerUUID, System.currentTimeMillis());
        }
    }
    
    /**
     * 获取并清除指定发送者的被@玩家列表
     * @param senderUUID 发送者的 UUID
     * @return 被@的玩家集合
     */
    public static Set<Player> getMentionedPlayers(UUID senderUUID) {
        if (senderUUID == null) {
            return Collections.emptySet();
        }
        return mentionedPlayersMap.remove(senderUUID);
    }
    
    /**
     * 检查是否有被@的玩家
     */
    public static boolean hasMentionedPlayers(UUID senderUUID) {
        if (senderUUID == null) {
            return false;
        }
        Set<Player> players = mentionedPlayersMap.get(senderUUID);
        return players != null && !players.isEmpty();
    }
}
