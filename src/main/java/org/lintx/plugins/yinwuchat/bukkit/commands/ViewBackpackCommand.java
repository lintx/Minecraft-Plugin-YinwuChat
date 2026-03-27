package org.lintx.plugins.yinwuchat.bukkit.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lintx.plugins.yinwuchat.Const;
import org.lintx.plugins.yinwuchat.Util.BackpackViewDebugLogUtil;
import org.lintx.plugins.yinwuchat.bukkit.YinwuChat;
import org.lintx.plugins.yinwuchat.bukkit.display.BackpackDisplayPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ViewBackpackCommand implements CommandExecutor {
    public static final String DISPLAY_TITLE_PREFIX = "背包展示";

    private final YinwuChat plugin;
    private static final Map<String, PendingRequest> pendingRequests = new ConcurrentHashMap<>();

    public ViewBackpackCommand(YinwuChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令只能由玩家执行");
            return true;
        }
        Player player = (Player) sender;
        if (args.length < 1) {
            player.sendMessage("§c用法: /viewbackpack <展示ID>");
            return true;
        }
        requestBackpackFromVelocity(player, args[0]);
        return true;
    }

    private void requestBackpackFromVelocity(Player player, String displayId) {
        PendingRequest request = new PendingRequest();
        request.playerUuid = player.getUniqueId();
        request.displayId = displayId;
        pendingRequests.put(displayId + ":" + player.getUniqueId(), request);
        try {
            plugin.getLogger().fine("[backpackview] bukkit requesting cached payload: "
                    + BackpackViewDebugLogUtil.summarizeDisplayRequest(displayId, player.getName()));
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF(Const.PLUGIN_SUB_CHANNEL_ITEM_DISPLAY_REQUEST);
            output.writeUTF(displayId);
            player.sendPluginMessage(plugin, plugin.getActualChannel(), output.toByteArray());
            org.lintx.plugins.yinwuchat.Util.SchedulerUtil.runTaskLaterAsynchronously(plugin, () -> {
                PendingRequest pending = pendingRequests.remove(displayId + ":" + player.getUniqueId());
                if (pending != null) {
                    Player viewer = Bukkit.getPlayer(pending.playerUuid);
                    if (viewer != null && viewer.isOnline()) {
                        viewer.sendMessage("§c背包展示已过期或不存在");
                    }
                }
            }, 100L);
        } catch (Exception e) {
            player.sendMessage("§c请求背包数据失败");
            plugin.getLogger().warning("Failed to request backpack from Velocity: " + e.getMessage());
        }
    }

    public static void handleItemDisplayResponse(YinwuChat plugin, Player player, String displayId,
                                                 boolean success, String itemJson, String playerName, String serverName) {
        plugin.getLogger().fine("[backpackview] bukkit received cached payload response: "
                + BackpackViewDebugLogUtil.summarizeDisplayResponse(displayId, success, itemJson, playerName, serverName));
        String key = displayId + ":" + player.getUniqueId();
        PendingRequest pending = pendingRequests.remove(key);
        if (pending == null) {
            plugin.getLogger().fine("[backpackview] bukkit ignored response because pending request was missing: "
                    + BackpackViewDebugLogUtil.summarizeDisplayRequest(displayId, player.getName()));
            return;
        }
        if (!success || itemJson == null || itemJson.isEmpty()) {
            player.sendMessage("§c背包展示已过期或不存在");
            return;
        }
        openBackpackInventoryStatic(plugin, player, playerName, itemJson);
    }

    private static void openBackpackInventoryStatic(YinwuChat plugin, Player viewer, String ownerName, String payloadJson) {
        org.lintx.plugins.yinwuchat.Util.SchedulerUtil.runTaskForPlayer(plugin, viewer, () -> {
            BackpackDisplayPayload payload = BackpackDisplayPayload.fromJson(payloadJson);
            if (payload == null) {
                viewer.sendMessage("§c背包展示数据损坏");
                return;
            }
            plugin.getLogger().fine("[backpackview] bukkit opening inventory for " + viewer.getName() + ": "
                    + BackpackViewDebugLogUtil.summarizePayload(payloadJson));
            String title = DISPLAY_TITLE_PREFIX + " - " + (ownerName == null || ownerName.isEmpty() ? payload.ownerName : ownerName);
            Inventory display = Bukkit.createInventory(null, 54, title);
            decorateUnusedSlots(display);
            for (int slot = 0; slot < payload.slots.size() && slot < 54; slot++) {
                String itemJson = payload.slots.get(slot);
                if (itemJson == null || itemJson.trim().isEmpty()) {
                    continue;
                }
                ItemStack item = ViewItemCommand.deserializeItem(itemJson);
                if (item != null && !item.getType().isAir()) {
                    display.setItem(slot, item);
                }
            }
            viewer.openInventory(display);
        });
    }

    private static void decorateUnusedSlots(Inventory display) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        for (int slot = 0; slot <= 8; slot++) {
            display.setItem(slot, filler);
        }
        for (int slot = 50; slot <= 53; slot++) {
            display.setItem(slot, filler);
        }
    }

    private static class PendingRequest {
        UUID playerUuid;
        String displayId;
    }
}
