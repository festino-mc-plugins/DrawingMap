package com.festp.utils;

import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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
		if (stack == null || stack.getType() != Material.FILLED_MAP)
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
		if (stack == null || !stack.hasItemMeta() || !(stack.getItemMeta() instanceof MapMeta))
			return -1;
		MapMeta meta = (MapMeta)stack.getItemMeta();
		if (!meta.hasMapId())
			return -1;
		return meta.getMapId();
	}
	
	public static ItemStack setDisplayName(ItemStack item, String nbt) {
		String NAME_PLACEHOLDER = "abcde12345";
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(NAME_PLACEHOLDER);
		item.setItemMeta(meta);
		String ymlStr = getYml(item);
		// '{"extra":[{"text":"abcde123450"}],"text":""}'
		ymlStr = ymlStr.replace("{\"text\":\"" + NAME_PLACEHOLDER + "\"}", nbt);
		return fromYml(ymlStr);
	}
	public static ItemStack setLore(ItemStack item, String[] nbt) {
		String LORE_PLACEHOLDER = "abcde12345";
		String[] loreTemp = new String[nbt.length];
		for (int i = 0; i < loreTemp.length; i++) {
			loreTemp[i] = LORE_PLACEHOLDER + i;
		}
		ItemMeta meta = item.getItemMeta();
		meta.setLore(Arrays.asList(loreTemp));
		item.setItemMeta(meta);
		String ymlStr = getYml(item);
		for (int i = 0; i < loreTemp.length; i++) {
			// '{"extra":[{"text":"abcde123450"}],"text":""}'
			if (nbt[i].length() == 0)
				ymlStr = ymlStr.replace(LORE_PLACEHOLDER + i, nbt[i]);
			else
				ymlStr = ymlStr.replace("{\"text\":\"" + LORE_PLACEHOLDER + i + "\"}", nbt[i]);
		}
		return fromYml(ymlStr);
	}
	private static String getYml(ItemStack item) {
		String KEY = "a";
		Reader reader = new StringReader("");
		FileConfiguration ymlFormat = YamlConfiguration.loadConfiguration(reader);
		ymlFormat.set(KEY, item);
		return ymlFormat.saveToString();
	}
	private static ItemStack fromYml(String ymlStr) {
		String KEY = "a";
		Reader reader = new StringReader("");
		FileConfiguration ymlFormat = YamlConfiguration.loadConfiguration(reader);
		try {
			ymlFormat.loadFromString(ymlStr);
			return ymlFormat.getItemStack(KEY);
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}
		return null;
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
