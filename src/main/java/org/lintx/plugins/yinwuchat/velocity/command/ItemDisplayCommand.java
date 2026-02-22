package org.lintx.plugins.yinwuchat.velocity.command;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.Util.Gson;
import org.lintx.plugins.yinwuchat.velocity.YinwuChat;
import org.lintx.plugins.yinwuchat.velocity.json.ItemRequest;

import java.util.List;
import java.util.Optional;

/**
 * 物品显示命令处理器
 * 处理玩家输入的物品显示占位符 [i]
 * 通过插件消息与后端服务器通信获取物品信息
 */
public class ItemDisplayCommand implements SimpleCommand {
    private final YinwuChat plugin;

    public ItemDisplayCommand(YinwuChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("只有玩家可以使用此命令！", NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;
        String[] args = invocation.arguments();

        if (args.length == 0) {
            // 显示手中物品信息
            requestHandItem(player);
        } else if (args.length == 1) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "hand":
                case "mainhand":
                    requestHandItem(player);
                    break;
                case "inventory":
                case "inv":
                    requestInventory(player);
                    break;
                case "enderchest":
                case "ec":
                    requestEnderChest(player);
                    break;
                default:
                    showUsage(player);
                    break;
            }
        } else {
            showUsage(player);
        }
    }

    /**
     * 请求玩家手中物品信息
     */
    private void requestHandItem(Player player) {
        requestItem(player, "hand");
    }

    /**
     * 请求玩家背包信息
     */
    private void requestInventory(Player player) {
        requestItem(player, "inventory");
    }

    /**
     * 请求玩家末影箱信息
     */
    private void requestEnderChest(Player player) {
        requestItem(player, "enderchest");
    }

    /**
     * 发送物品请求到后端服务器
     */
    private void requestItem(Player player, String requestType) {
        Optional<ServerConnection> serverConnection = player.getCurrentServer();
        if (serverConnection.isEmpty()) {
            player.sendMessage(Component.text("您当前未连接到任何服务器！", NamedTextColor.RED));
            return;
        }

        try {
            // 创建物品请求
            ItemRequest request = new ItemRequest(player.getUsername(), requestType);

            // 发送插件消息到后端服务器
            String jsonRequest = Gson.gson().toJson(request);
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF(Const.PLUGIN_SUB_CHANNEL_ITEM_REQUEST);
            output.writeUTF(jsonRequest);

            serverConnection.get().sendPluginMessage(
                com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier.from(Const.PLUGIN_CHANNEL),
                output.toByteArray()
            );

            player.sendMessage(Component.text("正在请求物品信息...", NamedTextColor.YELLOW));
        } catch (Exception e) {
            player.sendMessage(Component.text("发送请求失败，请稍后再试。", NamedTextColor.RED));
            plugin.getLogger().warn("Failed to send item request", e);
        }
    }

    /**
     * 显示使用说明
     */
    private void showUsage(Player player) {
        Component usage = Component.text()
            .append(Component.text("物品显示命令使用方法:\n", NamedTextColor.GOLD))
            .append(Component.text("/itemdisplay hand", NamedTextColor.GREEN))
            .append(Component.text(" - 显示手中物品\n", NamedTextColor.WHITE))
            .append(Component.text("/itemdisplay inventory", NamedTextColor.GREEN))
            .append(Component.text(" - 显示背包\n", NamedTextColor.WHITE))
            .append(Component.text("/itemdisplay enderchest", NamedTextColor.GREEN))
            .append(Component.text(" - 显示末影箱\n", NamedTextColor.WHITE))
            .append(Component.text("\n聊天中使用 ", NamedTextColor.YELLOW))
            .append(Component.text("[i]", NamedTextColor.GOLD))
            .append(Component.text(" 显示手中物品", NamedTextColor.YELLOW))
            .build();

        player.sendMessage(usage);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player)) {
            return true;
        }
        Player player = (Player) source;
        return player.hasPermission(Const.PERMISSION_ITEM_DISPLAY) || org.lintx.plugins.yinwuchat.velocity.config.Config.getInstance().isDefault(player);
    }
}
