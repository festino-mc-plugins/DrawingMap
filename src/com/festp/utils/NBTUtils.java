package com.festp.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class NBTUtils
{
	private static JavaPlugin plugin;
	
	public static void setPlugin(JavaPlugin plugin)
	{
		NBTUtils.plugin = plugin;
	}
	
	@SuppressWarnings("deprecation")
	public static ItemStack setMapId(ItemStack stack, int val)
	{
		if (stack == null)
			return stack;
		MapMeta meta = (MapMeta)stack.getItemMeta();
		meta.setMapId(val);
		stack.setItemMeta(meta);
		return stack;
	}

	/** @return -1 if no id */
	@SuppressWarnings("deprecation")
	public static int getMapId(ItemStack stack)
	{
		if (stack == null || !stack.hasItemMeta())
			return -1;
		MapMeta meta = (MapMeta)stack.getItemMeta();
		if (!meta.hasMapId())
			return -1;
		return meta.getMapId();
	}
	
	public static ItemStack setInt(ItemStack stack, String key, int val)
	{
		if (stack == null)
			return stack;
		ItemMeta meta = stack.getItemMeta();
		NamespacedKey namedkey = new NamespacedKey(plugin, key);
		meta.getPersistentDataContainer().set(namedkey, PersistentDataType.INTEGER, val);
		stack.setItemMeta(meta);
		return stack;
	}

	/** @return -1 if no key */
	public static int getInt(ItemStack stack, String key)
	{
		if (stack == null || !stack.hasItemMeta())
			return -1;
		PersistentDataContainer container = stack.getItemMeta().getPersistentDataContainer();
		NamespacedKey namedkey = new NamespacedKey(plugin, key);
		if (!container.has(namedkey, PersistentDataType.INTEGER))
			return -1;
		return container.get(namedkey, PersistentDataType.INTEGER);
	}

	public static ItemStack setBoolean(ItemStack stack, String key, boolean val) {
		if (stack == null)
			return stack;
		ItemMeta meta = stack.getItemMeta();
		NamespacedKey namedkey = new NamespacedKey(plugin, key);
		meta.getPersistentDataContainer().set(namedkey, PersistentDataType.BYTE, val ? (byte)1 : (byte)0);
		stack.setItemMeta(meta);
		return stack;
	}
	/** @return false if no key */
	public static boolean getBoolean(ItemStack stack, String key) {
		if (stack == null || !stack.hasItemMeta())
			return false;
		PersistentDataContainer container = stack.getItemMeta().getPersistentDataContainer();
		NamespacedKey namedkey = new NamespacedKey(plugin, key);
		if (!container.has(namedkey, PersistentDataType.BYTE))
			return false;
		return container.get(namedkey, PersistentDataType.BYTE) != 0;
	}
}
