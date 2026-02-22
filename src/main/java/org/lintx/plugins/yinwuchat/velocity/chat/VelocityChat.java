package org.lintx.plugins.yinwuchat.velocity.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.lintx.plugins.yinwuchat.chat.struct.ChatPlayer;
import org.lintx.plugins.yinwuchat.chat.struct.ChatSource;
import org.lintx.plugins.yinwuchat.chat.struct.ChatType;
import org.lintx.plugins.yinwuchat.json.HandleConfig;
import org.lintx.plugins.yinwuchat.json.MessageFormat;
import org.lintx.plugins.yinwuchat.velocity.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Velocity 版本的聊天消息类
 * 使用 Adventure API 替代 BungeeCord API
 */
public class VelocityChat {
    public List<VelocityChatStruct> chat;
    public ChatSource source;
    public ChatType type;
    public ChatPlayer fromPlayer;
    private ChatPlayer toPlayer;
    public List<HandleConfig> extraData = new ArrayList<>();
    public List<Component> items;
    private Config config = Config.getInstance();
    private Component component;
    
    // 玩家自定义前后缀（在消息处理完成后添加，避免被占位符处理影响）
    private String playerPrefix = "";
    private String playerSuffix = "";

    public VelocityChat() {
        fromPlayer = new ChatPlayer();
    }

    public VelocityChat(ChatPlayer fromPlayer, List<VelocityChatStruct> chat, ChatSource source) {
        this(fromPlayer, new ChatPlayer(), chat, source, ChatType.PUBLIC);
    }

    public VelocityChat(ChatPlayer fromPlayer, ChatPlayer toPlayer, List<VelocityChatStruct> chat, ChatSource source) {
        this(fromPlayer, toPlayer, chat, source, ChatType.PRIVATE);
    }

    public VelocityChat(ChatPlayer fromPlayer, ChatPlayer toPlayer, List<VelocityChatStruct> chat, ChatSource source, ChatType type) {
        this.fromPlayer = fromPlayer;
        this.toPlayer = toPlayer;
        this.chat = chat;
        this.source = source;
        this.type = type;
    }
    
    /**
     * 设置玩家自定义前后缀
     * 这些前后缀会在消息内容构建完成后添加，不会被占位符处理影响
     */
    public void setPlayerPrefixSuffix(String prefix, String suffix) {
        this.playerPrefix = prefix != null ? prefix : "";
        this.playerSuffix = suffix != null ? suffix : "";
    }

    // 构建公屏消息
    public Component buildPublicMessage(List<MessageFormat> formats) {
        // 检查是否有组件（比如物品占位符被替换为组件，或@提及）
        boolean hasComponents = this.chat.stream().anyMatch(struct -> struct.component != null);

        // 直接构建消息，不使用buildMessage来避免重复
        Component result = Component.empty();

        for (MessageFormat format : formats) {
            if (format.message == null || format.message.isEmpty()) continue;

            // 如果有组件，不在格式中替换 {message}，后面会单独添加
            // 如果没有组件，则在格式中替换 {message}
            String messageContent = hasComponents ? null : extractOriginalMessage();
            
            MessageFormat newFormat = handlePlayerName(format, fromPlayer.playerName, null, messageContent);
            
            // 如果有组件且格式包含 {message}，需要分割处理
            if (hasComponents && format.message.contains("{message}")) {
                // 分割格式，在 {message} 位置插入组件化的消息
                String[] parts = newFormat.message.split("\\{message\\}", 2);
                if (parts.length == 2) {
                    // 添加 {message} 之前的部分
                    if (!parts[0].isEmpty()) {
                        Component beforeComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(parts[0]);
                        result = result.append(beforeComponent);
                    }
                    // 添加组件化的消息内容
                    Component messageComponent = build();
                    result = result.append(messageComponent);
                    // 添加 {message} 之后的部分
                    if (!parts[1].isEmpty()) {
                        Component afterComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(parts[1]);
                        result = result.append(afterComponent);
                    }
                } else {
                    // 没有 {message} 或只有一个部分，正常处理
                    Component formatComponent = buildFormat(newFormat);
                    if (formatComponent != null) {
                        result = result.append(formatComponent);
                    }
                }
            } else {
                Component formatComponent = buildFormat(newFormat);
                if (formatComponent != null) {
                    result = result.append(formatComponent);
                }
            }
        }

        return result;
    }

    /**
     * 提取原始消息内容，排除已经被处理的占位符
     * 包含玩家自定义前后缀
     */
    private String extractOriginalMessage() {
        StringBuilder result = new StringBuilder();
        
        // 添加玩家自定义前缀
        if (playerPrefix != null && !playerPrefix.isEmpty()) {
            result.append(playerPrefix);
        }
        
        boolean hasComponents = false;
        for (VelocityChatStruct struct : this.chat) {
            if (struct.chat != null && !struct.chat.trim().isEmpty()) {
                // 只包含文本内容，跳过组件
                result.append(struct.chat);
            }
            if (struct.component != null) {
                hasComponents = true;
            }
        }
        
        // 添加玩家自定义后缀
        if (playerSuffix != null && !playerSuffix.isEmpty()) {
            result.append(playerSuffix);
        }

        String message = result.toString().trim();

        // 如果消息为空但有组件，添加一个占位符来确保消息可见
        if (message.isEmpty() && hasComponents) {
            message = " "; // 使用空格作为占位符
        }

        // 对于普通消息（没有组件），直接返回完整的消息内容
        // 对于包含组件的消息，返回提取的内容（可能包含空格占位符）
        return message;
    }

    // 构建别人发给我的私聊消息
    public Component buildPrivateFormMessage(List<MessageFormat> formats) {
        return buildMessage(formats, fromPlayer);
    }

    // 构建我发给别人的私聊消息
    public Component buildPrivateToMessage(List<MessageFormat> formats) {
        // 提取消息内容用于替换 {message} 占位符（包含前后缀）
        StringBuilder messageContent = new StringBuilder();
        
        // 添加玩家自定义前缀
        if (playerPrefix != null && !playerPrefix.isEmpty()) {
            messageContent.append(playerPrefix);
        }
        
        for (VelocityChatStruct struct : this.chat) {
            if (struct.chat != null) {
                messageContent.append(struct.chat);
            }
        }
        
        // 添加玩家自定义后缀
        if (playerSuffix != null && !playerSuffix.isEmpty()) {
            messageContent.append(playerSuffix);
        }
        
        return buildMessage(formats, toPlayer, messageContent.toString().trim());
    }

    // 构建OP监听的私聊消息
    public Component buildPrivateMonitorMessage(List<MessageFormat> formats) {
        return buildMessage(formats);
    }

    // 构建只带一个玩家名的消息
    private Component buildMessage(List<MessageFormat> formats, ChatPlayer player) {
        return buildMessage(formats, player, null);
    }

    // 构建带玩家名和消息内容的消息
    private Component buildMessage(List<MessageFormat> formats, ChatPlayer player, String messageContent) {
        Component result = Component.empty();

        for (MessageFormat format : formats) {
            if (format.message == null || format.message.isEmpty()) continue;

            MessageFormat newFormat = handlePlayerName(format, player.playerName, null, messageContent);
            Component formatComponent = buildFormat(newFormat);
            if (formatComponent != null) {
                result = result.append(formatComponent);
            }
        }

        // 使用build()构建的消息内容，或者使用提供的messageContent
        Component message = build();
        return result.append(message);
    }

    // 构建带2个玩家名的消息
    private Component buildMessage(List<MessageFormat> formats) {
        Component message = build();
        Component result = Component.empty();
        
        for (MessageFormat format : formats) {
            if (format.message == null || format.message.isEmpty()) continue;
            
            MessageFormat newFormat = handlePlayerName(format, fromPlayer.playerName, toPlayer.playerName, null);
            Component formatComponent = buildFormat(newFormat);
            if (formatComponent != null) {
                result = result.append(formatComponent);
            }
        }
        
        return result.append(message);
    }

    // 构建消息本体
    private Component build() {
        if (this.component != null) return this.component;
        
        Component result = Component.empty();
        
        // 添加玩家自定义前缀
        if (playerPrefix != null && !playerPrefix.isEmpty()) {
            result = result.append(LegacyComponentSerializer.legacyAmpersand().deserialize(playerPrefix));
        }
        
        for (VelocityChatStruct chatStruct : this.chat) {
            if (chatStruct.chat != null && !chatStruct.chat.isEmpty()) {
                result = result.append(LegacyComponentSerializer.legacySection().deserialize(chatStruct.chat));
            }
            if (chatStruct.component != null) {
                result = result.append(chatStruct.component);
            }
        }
        
        // 添加玩家自定义后缀
        if (playerSuffix != null && !playerSuffix.isEmpty()) {
            result = result.append(LegacyComponentSerializer.legacyAmpersand().deserialize(playerSuffix));
        }
        
        this.component = result;
        return result;
    }

    // 将 format 构建成 Component（format 应该已经经过占位符替换）
    public Component buildFormat(MessageFormat format) {
        if (format.message == null || format.message.isEmpty()) return null;
        
        // 直接使用传入的 format，不再重复调用 handlePlayerName
        // 因为调用者（如 buildPublicMessage）已经处理过占位符替换
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(format.message);
        
        return setComponentEvent(format, component);
    }
    
    // 将原始 format 构建成 Component（会自动进行占位符替换）
    public Component buildFormatWithReplace(MessageFormat format) {
        if (format.message == null || format.message.isEmpty()) return null;
        
        MessageFormat newFormat = handlePlayerName(format);
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(newFormat.message);
        
        return setComponentEvent(newFormat, component);
    }

    // 使用发送玩家名来处理 format
    private MessageFormat handlePlayerName(MessageFormat format) {
        return handlePlayerName(format, fromPlayer.playerName, null);
    }

    // 使用指定玩家名来处理 format
    private MessageFormat handlePlayerName(MessageFormat format, String name) {
        return handlePlayerName(format, name, null);
    }

    // 使用指定玩家名和消息内容来处理 format
    private MessageFormat handlePlayerName(MessageFormat format, String name, String messageContent) {
        return handlePlayerName(format, name, null, messageContent);
    }

    // 重命名三参数版本为四参数版本
    private MessageFormat handlePlayerNameThreeParams(MessageFormat format, String fromPlayer, String toPlayer) {
        return handlePlayerName(format, fromPlayer, toPlayer, null);
    }

    // 指定发送者、接收者和消息内容来处理 format
    private MessageFormat handlePlayerName(MessageFormat format, String fromPlayer, String toPlayer, String messageContent) {
        MessageFormat newFormat = new MessageFormat();
        
        // 获取服务器名称
        String serverName = getServerName();
        
        // 替换消息文本中的占位符
        newFormat.message = format.message;
        if (newFormat.message != null) {
            newFormat.message = newFormat.message.replace("{formPlayer}", fromPlayer != null ? fromPlayer : "");
            newFormat.message = newFormat.message.replace("{toPlayer}", toPlayer != null ? toPlayer : "");
            newFormat.message = newFormat.message.replace("{displayName}", fromPlayer != null ? fromPlayer : "");
            newFormat.message = newFormat.message.replace("ServerName", serverName != null ? serverName : "Unknown");
            if (messageContent != null) {
                newFormat.message = newFormat.message.replace("{message}", messageContent);
            }
        }
        
        // 替换悬浮文本中的占位符 - 即使原始值为null也要初始化
        newFormat.hover = format.hover;
        if (newFormat.hover != null && !newFormat.hover.isEmpty()) {
            newFormat.hover = newFormat.hover.replace("{displayName}", fromPlayer != null ? fromPlayer : "");
            newFormat.hover = newFormat.hover.replace("{formPlayer}", fromPlayer != null ? fromPlayer : "");
            newFormat.hover = newFormat.hover.replace("{toPlayer}", toPlayer != null ? toPlayer : "");
            newFormat.hover = newFormat.hover.replace("ServerName", serverName != null ? serverName : "Unknown");
            if (messageContent != null) {
                newFormat.hover = newFormat.hover.replace("{message}", messageContent);
            }
        }
        
        // 替换点击命令中的占位符 - 即使原始值为null也要初始化
        newFormat.click = format.click;
        if (newFormat.click != null && !newFormat.click.isEmpty()) {
            newFormat.click = newFormat.click.replace("{displayName}", fromPlayer != null ? fromPlayer : "");
            newFormat.click = newFormat.click.replace("{formPlayer}", fromPlayer != null ? fromPlayer : "");
            newFormat.click = newFormat.click.replace("{toPlayer}", toPlayer != null ? toPlayer : "");
            newFormat.click = newFormat.click.replace("ServerName", serverName != null ? serverName : "Unknown");
            if (messageContent != null) {
                newFormat.click = newFormat.click.replace("{message}", messageContent);
            }
        }
        
        return newFormat;
    }

    // 获取服务器名称
    private String getServerName() {
        if (fromPlayer instanceof org.lintx.plugins.yinwuchat.chat.struct.VelocityChatPlayer) {
            return ((org.lintx.plugins.yinwuchat.chat.struct.VelocityChatPlayer) fromPlayer).getServerName();
        }
        return "Unknown";
    }

    // 给 component 设置 event（hover 和 click）
    private Component setComponentEvent(MessageFormat format, Component component) {
        Component result = component;
        
        if (format.hover != null && !format.hover.isEmpty()) {
            Component hoverComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(format.hover);
            result = result.hoverEvent(HoverEvent.showText(hoverComponent));
        }
        
        if (format.click != null && !format.click.isEmpty()) {
            ClickEvent.Action action = determineClickAction(format.click);
            result = result.clickEvent(ClickEvent.clickEvent(action, format.click));
        }
        
        return result;
    }

    // 确定点击动作类型
    private ClickEvent.Action determineClickAction(String click) {
        Pattern pattern = Pattern.compile(config.linkRegex);
        Matcher matcher = pattern.matcher(click);

        if (matcher.find()) {
            return ClickEvent.Action.OPEN_URL;
        } else if (click.startsWith("/msg")) {
            return ClickEvent.Action.SUGGEST_COMMAND;  // 私聊命令建议输入而不是直接运行
        } else if (click.startsWith("/")) {
            return ClickEvent.Action.RUN_COMMAND;  // 其他命令直接运行
        } else if (click.startsWith("!")) {
            return ClickEvent.Action.RUN_COMMAND;
        } else {
            return ClickEvent.Action.SUGGEST_COMMAND;
        }
    }
}
