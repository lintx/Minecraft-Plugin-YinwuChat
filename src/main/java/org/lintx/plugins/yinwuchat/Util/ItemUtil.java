package org.lintx.plugins.yinwuchat.Util;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class ItemUtil {
    private static String convertItemToJson(ItemStack itemStack){
        // ItemStack methods to get a net.minecraft.server.ItemStack object for serialization
        Class<?> craftItemStackClazz = ReflectionUtil.getOBCClass("inventory.CraftItemStack");
        Method asNMSCopyMethod = ReflectionUtil.getMethod(craftItemStackClazz, "asNMSCopy", ItemStack.class);

        // NMS Method to serialize a net.minecraft.server.ItemStack to a valid Json string
        Class<?> nmsItemStackClazz = ReflectionUtil.getNMSClass("ItemStack");
        Class<?> nbtTagCompoundClazz = ReflectionUtil.getNMSClass("NBTTagCompound");
        Method saveNmsItemStackMethod = ReflectionUtil.getMethod(nmsItemStackClazz, "save", nbtTagCompoundClazz);

        Object nmsNbtTagCompoundObj; // This will just be an empty NBTTagCompound instance to invoke the saveNms method
        Object nmsItemStackObj; // This is the net.minecraft.server.ItemStack object received from the asNMSCopy method
        Object itemAsJsonObject; // This is the net.minecraft.server.ItemStack after being put through saveNmsItem method
        String result;

        try {
            nmsNbtTagCompoundObj = nbtTagCompoundClazz.newInstance();
            nmsItemStackObj = asNMSCopyMethod.invoke(null, itemStack);
            itemAsJsonObject = saveNmsItemStackMethod.invoke(nmsItemStackObj, nmsNbtTagCompoundObj);
            result = itemAsJsonObject.toString();
        } catch (Throwable t) {
            Bukkit.getLogger().log(Level.SEVERE, "failed to serialize itemstack to nms item", t);
            return null;
        }

        // Return a string representation of the serialized object
        return result;
    }

    private static String getItemKey(ItemStack itemStack){
        // ItemStack methods to get a net.minecraft.server.ItemStack object for serialization
        Class<?> craftItemStackClazz = ReflectionUtil.getOBCClass("inventory.CraftItemStack");
        Method asNMSCopyMethod = ReflectionUtil.getMethod(craftItemStackClazz, "asNMSCopy", ItemStack.class);

        // NMS Method to serialize a net.minecraft.server.ItemStack to a valid Json string
        Class<?> nmsItemStackClazz = ReflectionUtil.getNMSClass("ItemStack");

        Method getItemMethod = ReflectionUtil.getMethod(nmsItemStackClazz,"getItem");
        Class<?> nmsItemClazz = ReflectionUtil.getNMSClass("Item");
        Method getNameMethod = ReflectionUtil.getMethod(nmsItemClazz,"getName");

        Object nmsItemStackObj; // This is the net.minecraft.server.ItemStack object received from the asNMSCopy method
        Object nmsItemObj;
        Object nmsItemName;
        String result;

        try {
            nmsItemStackObj = asNMSCopyMethod.invoke(null, itemStack);

            nmsItemObj = getItemMethod.invoke(nmsItemStackObj);
            nmsItemName = getNameMethod.invoke(nmsItemObj);
            result = nmsItemName.toString();
        } catch (Throwable t) {
            Bukkit.getLogger().log(Level.SEVERE, "failed to serialize itemstack to nms item", t);
            return null;
        }

        return result;
    }

    //color:ItemStack.u().e.name();
    private static BaseComponent getItemComponent(ItemStack itemStack){

        TextComponent component = new TextComponent();
        if (itemStack.hasItemMeta()){
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta.hasDisplayName()){
                TextComponent textComponent = new TextComponent(itemMeta.getDisplayName());
                component.addExtra(textComponent);
                return component;
            }
        }
        String nmsName = getItemKey(itemStack);
        if (nmsName!=null && !nmsName.equals("")){
            TranslatableComponent itemComponent = new TranslatableComponent(nmsName);
            component.addExtra(itemComponent);
            return component;
        }
        TextComponent textComponent = new TextComponent(itemStack.getType().name());
        component.addExtra(textComponent);
        return component;
    }

    public static BaseComponent componentWithPlayer(ItemStack itemStack){
        if (itemStack==null || itemStack.getType().equals(Material.AIR)){
            return null;
        }
        ItemStack item = itemStack.clone();
        try {
            if (item.getType().equals(Material.WRITABLE_BOOK) || item.getType().equals(Material.WRITTEN_BOOK)){
                BookMeta bm = (BookMeta)item.getItemMeta();
                bm.setPages(Collections.emptyList());
                item.setItemMeta(bm);
            }
        }
        catch (Exception | Error ignored){

        }
        try {
            List<Material> shulker_boxes = new ArrayList<>(Arrays.asList(Material.BLACK_SHULKER_BOX, Material.BLUE_SHULKER_BOX,
                    Material.BROWN_SHULKER_BOX, Material.CYAN_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.GREEN_SHULKER_BOX,
                    Material.LIGHT_BLUE_SHULKER_BOX, Material.LIME_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX, Material.ORANGE_SHULKER_BOX,
                    Material.PINK_SHULKER_BOX, Material.PURPLE_SHULKER_BOX, Material.RED_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX,
                    Material.WHITE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX));
            if (shulker_boxes.contains(item.getType())){
                if (item.hasItemMeta()){
                    BlockStateMeta bsm = (BlockStateMeta)item.getItemMeta();
                    if (bsm.hasBlockState()){
                        ShulkerBox sb = (ShulkerBox)bsm.getBlockState();
                        for (ItemStack i:sb.getInventory()){
                            if (i==null){
                                continue;
                            }
                            if (i.getType().equals(Material.AIR)){
                                continue;
                            }
                            if (!i.hasItemMeta()){
                                continue;
                            }
                            ItemMeta im = Bukkit.getItemFactory().getItemMeta(i.getType());
                            ItemMeta original = i.getItemMeta();
                            if (original.hasDisplayName()){
                                im.setDisplayName(original.getDisplayName());
                            }
                            i.setItemMeta(im);
                        }
                        bsm.setBlockState(sb);
                    }
                    item.setItemMeta(bsm);
                }
            }
        }
        catch (Exception | Error ignored){

        }

        TextComponent component = new TextComponent("");

        component.addExtra("§r§7[§r");
        try {
            component.addExtra(getItemComponent(item));
        }
        catch (Exception | Error e){
            component.addExtra(item.getType().name());
        }
        if (item.getAmount()>1){
            component.addExtra(" x" + item.getAmount());
        }
        component.addExtra("§r§7]§r");


        String itemJson = convertItemToJson(itemStack);
        BaseComponent[] hoverEventComponents = new BaseComponent[]{
                new TextComponent(itemJson)
        };

        HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_ITEM,hoverEventComponents);
        component.setHoverEvent(event);

        return component;
    }

    public static String itemJsonWithPlayer(ItemStack itemStack){
        BaseComponent component = componentWithPlayer(itemStack);
        if (component==null) return null;
        return ComponentSerializer.toString(component);
    }
}
