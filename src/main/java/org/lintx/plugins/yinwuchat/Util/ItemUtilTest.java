package org.lintx.plugins.yinwuchat.Util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试类，用于验证 ModernItemUtil 的功能
 */
public class ItemUtilTest {
    
    public static void testItemSerialization() {
        Bukkit.getLogger().info("[YinwuChat] 开始测试 ModernItemUtil 物品序列化功能...");
        
        // 创建测试物品
        ItemStack testItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = testItem.getItemMeta();
        meta.setDisplayName("§e测试剑");
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        testItem.setItemMeta(meta);
        
        // 测试现代序列化
        String modernResult = ModernItemUtil.itemJsonWithPlayer(testItem);
        Bukkit.getLogger().info("[YinwuChat] 现代序列化结果: " + (modernResult != null ? "成功" : "失败"));
        
        // 测试传统序列化（如果现代方法失败）
        if (modernResult == null) {
            Bukkit.getLogger().info("[YinwuChat] 现代序列化失败，回退到传统方法");
        }
        
        // 测试不同类型的物品
        List<ItemStack> testItems = new ArrayList<>();
        testItems.add(new ItemStack(Material.STONE));
        testItems.add(new ItemStack(Material.APPLE, 5));
        testItems.add(new ItemStack(Material.ENCHANTED_BOOK));
        
        for (ItemStack item : testItems) {
            String result = ModernItemUtil.itemJsonWithPlayer(item);
            Bukkit.getLogger().info("[YinwuChat] 物品 " + item.getType() + " 序列化: " + (result != null ? "成功" : "失败"));
        }
        
        Bukkit.getLogger().info("[YinwuChat] ModernItemUtil 测试完成");
    }
}