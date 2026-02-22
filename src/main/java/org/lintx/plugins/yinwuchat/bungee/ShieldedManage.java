package org.lintx.plugins.yinwuchat.bungee;


import io.netty.channel.Channel;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.lintx.plugins.yinwuchat.Util.MessageUtil;
import org.lintx.plugins.yinwuchat.bungee.config.Config;
import org.lintx.plugins.yinwuchat.bungee.json.OutputServerMessage;
import org.lintx.plugins.yinwuchat.bungee.httpserver.NettyChannelMessageHelper;
import org.lintx.plugins.yinwuchat.bungee.manage.MuteManage;
import org.lintx.plugins.yinwuchat.common.auth.AuthService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ShieldedManage {
    private static final ShieldedManage instance = new ShieldedManage();
    public static ShieldedManage getInstance(){
        return instance;
    }

    private Map<String,Man> users = new HashMap<>();

    private static class Man {
        int count = 0;
        LocalDateTime first = null;
    }

    private String formatMessage(String string){
        string = string.replaceAll("&([0-9a-fA-FklmnorKLMNOR])","§$1");
        return string;
    }

    private String normalizeForShielded(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text;
        // Remove hex color sequences like §x§R§R§G§G§B§B or &x&R&R&G&G&B&B
        normalized = normalized.replaceAll("(?i)[§&]x(?:[§&][0-9a-f]){6}", "");
        // Remove shorthand hex like §#RRGGBB or &#RRGGBB
        normalized = normalized.replaceAll("(?i)[§&]#[0-9a-f]{6}", "");
        // Remove standard color/style codes
        normalized = normalized.replaceAll("(?i)[§&][0-9a-fk-or]", "");
        // Remove all whitespace
        normalized = normalized.replaceAll("\\s+", "");
        return normalized.toLowerCase(Locale.ROOT);
    }

    /**
     * 检查 Web 玩家消息是否包含屏蔽词（带账户名，用于封禁）
     * @param channel WebSocket 通道
     * @param uuid 玩家UUID
     * @param accountName Web 账户名
     * @param message 消息内容
     * @return 检查结果
     */
    Result checkShielded(Channel channel, String uuid, String accountName, String message){
        Result result = checkShielded(uuid,message);
        if (result.kick){
            // 封禁 Web 玩家 1 小时（而不是简单踢出）
            if (accountName != null && !accountName.isEmpty()) {
                long banDurationMillis = 60 * 60 * 1000L; // 1小时
                String banReason = "多次发送违禁词";
                AuthService authService = AuthService.getInstance(YinwuChat.getPlugin().getDataFolder());
                authService.banUser(accountName, banDurationMillis, banReason, "System");
                
                // 发送封禁通知并断开连接
                com.google.gson.JsonObject banKick = new com.google.gson.JsonObject();
                banKick.addProperty("action", "ban_kick");
                banKick.addProperty("player", "");
                banKick.addProperty("account", accountName);
                banKick.addProperty("durationText", "1小时");
                banKick.addProperty("reason", banReason);
                NettyChannelMessageHelper.send(channel, banKick.toString());
                channel.close();
                YinwuChat.getPlugin().getLogger().info("[屏蔽词] Web 玩家 " + accountName + " (" + uuid + ") 因多次发送违禁词被封禁1小时");
                
                // 同时禁言绑定的游戏玩家
                String playerName = authService.getBoundPlayerName(accountName);
                if (playerName != null && !playerName.isEmpty()) {
                    MuteManage.getInstance().mutePlayer(playerName, 3600L, "System", banReason);
                }
            } else {
                // 没有账户名时，仍然踢出
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON(formatMessage(Config.getInstance().tipsConfig.shieldedKickTip)).getJSON());
            channel.close();
            YinwuChat.getPlugin().getLogger().info("[屏蔽词] Web 玩家 " + uuid + " 因多次发送违禁词被断开连接");
            }
        }
        else if (result.shielded){
            NettyChannelMessageHelper.send(channel, OutputServerMessage.errorJSON(formatMessage(Config.getInstance().tipsConfig.shieldedTip)).getJSON());
        }
        return result;
    }

    /**
     * 检查 Web 玩家消息是否包含屏蔽词（旧接口，兼容性保留）
     * @param channel WebSocket 通道
     * @param uuid 玩家UUID
     * @param message 消息内容
     * @return 检查结果
     */
    Result checkShielded(Channel channel, String uuid, String message){
        return checkShielded(channel, uuid, null, message);
    }

    Result checkShielded(ProxiedPlayer player,String message){
        Result result = checkShielded(player.getUniqueId().toString(),message);
        if (result.kick){
            player.disconnect(MessageUtil.newTextComponent(formatMessage(Config.getInstance().tipsConfig.shieldedKickTip)));
            YinwuChat.getPlugin().getLogger().info("[屏蔽词] 玩家 " + player.getName() + " 因多次发送违禁词被踢出");
        }
        else if (result.shielded){
            player.sendMessage(MessageUtil.newTextComponent(formatMessage(Config.getInstance().tipsConfig.shieldedTip)));
        }
        return result;
    }

    private Result checkShielded(String uuid,String message){
        String cleanMessage = normalizeForShielded(message);
        
        Result result = new Result();
        Config config = Config.getInstance();
        
        // 检查是否包含屏蔽词
        if (config.shieldeds == null || config.shieldeds.isEmpty()) {
            return result;
        }
        
        // 检查消息是否包含任何屏蔽词（清理关键词以匹配 cleaned 消息）
        boolean containsShielded = config.shieldeds.parallelStream()
                .filter(Objects::nonNull)
                .map(this::normalizeForShielded)
                .anyMatch(cleanKeyword -> !cleanKeyword.isEmpty() && cleanMessage.contains(cleanKeyword));
        
        if (containsShielded){
            YinwuChat.getPlugin().getLogger().info(uuid + " send a shielded word: " + message);
            result.shielded = true;
            if (!users.containsKey(uuid)){
                Man man = new Man();
                man.first = LocalDateTime.now();
                users.put(uuid,man);
            }

            Man man = users.get(uuid);
            Duration duration = Duration.between(man.first,LocalDateTime.now());
            if (duration.toMillis()>config.shieldedKickTime*1000){
                man.first = LocalDateTime.now();
                man.count = 0;
            }
            man.count += 1;
            users.put(uuid,man);
            if (man.count>=config.shieldedKickCount){
                result.kick = true;
                users.remove(uuid);
                return result;
            }

            if (config.shieldedMode==1){
                result.msg = config.tipsConfig.shieldedReplace;
            }
            else {
                result.end = true;
            }
        }
        return result;
    }

    static class Result{
        boolean shielded = false;
        boolean end = false;
        boolean kick = false;
        String msg = "";
    }
}
