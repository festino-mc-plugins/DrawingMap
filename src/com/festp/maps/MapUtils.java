package com.festp.maps;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import com.festp.maps.drawing.DrawingMapUtils;
import com.festp.maps.small.SmallMap;
import com.festp.maps.small.SmallMapUtils;
import com.festp.utils.NBTUtils;
import com.festp.utils.UtilsVersion;
import com.google.common.collect.Lists;

public class MapUtils {
	
	public static int getEmptySlot(PlayerInventory inv)
	{
		ItemStack[] slots = inv.getStorageContents();
		if (slots[inv.getHeldItemSlot()] == null)
			return inv.getHeldItemSlot();
		
		for (int i = 8; i >= 0; i--)
			if (slots[i] == null)
				return i;
		for (int i = 35; i >= 9; i--)
			if (slots[i] == null)
				return i;
		return -1;
	}
	
	/** @return first removed renderer */
	public static MapRenderer removeRenderers(MapView view) {
		MapRenderer vanillaRenderer = null;
		for (MapRenderer m : view.getRenderers()) {
			view.removeRenderer(m);
			if (vanillaRenderer == null) {
				vanillaRenderer = m;
			}
		}
		return vanillaRenderer;
	}
	
	public static void addRenderer(MapView view, MapRenderer mapRenderer) {
		view.addRenderer(mapRenderer);
	}
	
	public static MapView genNewView(IMap map)
	{
		MapView oldView = Bukkit.getMap(map.getId());
		MapView view = Bukkit.createMap(oldView.getWorld());
		return view;
	}
	
	public static MapView getView(IMap map)
	{
		MapView view = Bukkit.getMap(map.getId());
		return view;
	}
	
	/** slow method */
	public static boolean isExists(int id)
	{
		return Bukkit.getMap(id) != null;
	}
	/** very slow method
	 * @return <b>-1</b> if no maps */
	public static int getMaxId()
	{
		if (UtilsVersion.SUPPORTS_INTEGER_MAP_ID) {
			// don't care about the implementation of negative ids
			if (isExists(-1) || isExists(Integer.MIN_VALUE))
				return getMaxId(Integer.MIN_VALUE, -1);
			else
				return getMaxId(-1, Integer.MAX_VALUE);
		} else {
			return getMaxId(-1, Short.MAX_VALUE);
		}
	}
	private static int getMaxId(int min, int max)
	{
		while (min < max)
		{
			int mid = (min + 1 + max) / 2;
			boolean isExists = isExists(mid);
			if (isExists)
				min = mid;
			else
				max = mid - 1;
		}
		return min;
	}
	
	/* uses USE_SCALE_NAMES as second argument */
	public static ItemStack getMap(int id) {
		return getMap(id, SmallMapUtils.USE_SCALE_NAMES);
	}
	
	public static ItemStack getMap(int id, boolean scaleName)
	{
		ItemStack item = new ItemStack(Material.FILLED_MAP, 1);
		item = NBTUtils.setMapId(item, id);
		if (SmallMapUtils.isSmallMap(id)) {
			ItemMeta meta = item.getItemMeta();
			
			SmallMap map = (SmallMap) MapFileManager.load(id);
			String[] lore = new String[] { "Scaling at " + map.getScale() + ":1" };
			meta.setLore(Lists.asList("", lore));
			if (scaleName)
				meta.setDisplayName("Map (" + map.getScale() + ":1)");
			
			item.setItemMeta(meta);
		} else if (DrawingMapUtils.isDrawingMap(id)) {
			ItemMeta meta = item.getItemMeta();
			meta.setLore(Arrays.asList(new String[] { "Drawing" }));
			item.setItemMeta(meta);
		}
		return item;
	}
	
	public static Integer getMapId(ItemStack item)
	{
		if (item == null || item.getType() != Material.FILLED_MAP)
			return null;
		return NBTUtils.getMapId(item);
	}

	public static boolean hasMap(Player p, int mapId) {
		for (ItemStack is : p.getInventory().getContents()) {
			Integer id = getMapId(is);
			if (id != null && mapId == id) {
				return true;
			}
		}
		return false;
	}
}
