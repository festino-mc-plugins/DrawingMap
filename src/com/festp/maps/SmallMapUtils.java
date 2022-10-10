package com.festp.maps;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapView;
import org.bukkit.map.MapView.Scale;

import com.festp.utils.NBTUtils;
import com.google.common.collect.Lists;

public class SmallMapUtils {
	
	public static final String SCALE_FIELD = "map_scale";
	public static final boolean USE_SCALE_NAMES = false; // bad for frames, good for understanding
	/** New scales (8:1, 4:1, 2:1)
	 *  	craft: item tag
	 *  MapInitializeEvent: cancel, create and store ID+data(scale, rot)
	 *  MapRenderer
	 *  Only cloning remains*/

	public static ItemStack extendMap(SmallMap map)
	{
		if (map.getScale() / 2 > 1)
		{
			MapView view = MapUtils.getView(map);
			Location loc = new Location(view.getWorld(), map.getX(), 0, map.getZ());
			SmallMap new_map = genSmallMap(loc, map.getScale() / 2);
			return MapUtils.getMap(new_map.getId(), false);
		}
		else
		{
			MapView view = MapUtils.genNewView(map);
			int x = (int) Math.floor(map.getX() / 128.0) * 128;
			int z = (int) Math.floor(map.getZ() / 128.0) * 128;
			view.setCenterX(x + 64);
			view.setCenterZ(z + 64);
			view.setScale(Scale.CLOSEST); // 1:1
			return MapUtils.getMap(view.getId());
		}
	}
	public static ItemStack getPreExtendedMap(int id)
	{
		SmallMap map = (SmallMap) MapFileManager.load(id);
		ItemStack pre_map = MapUtils.getMap(id, true);
		ItemMeta pre_map_meta = pre_map.getItemMeta();
		int scale = map.getScale() / 2;
		pre_map_meta.setDisplayName("Map (" + scale + ":1)");
		String[] lore = new String[] { "Scaling at " + scale + ":1" };
		pre_map_meta.setLore(Lists.asList("", lore));
		pre_map.setItemMeta(pre_map_meta);
		
		return pre_map;
	}
	
	/** create new map and attach renderer*/
	public static SmallMap genSmallMap(Location l, int scale)
	{
		MapView view = Bukkit.createMap(l.getWorld());
		view.setScale(Scale.CLOSEST);

		int ratio = 128 / scale;
		int start_x = (int)Math.floor(l.getBlockX() / (float)ratio) * ratio;
		int start_z = (int)Math.floor(l.getBlockZ() / (float)ratio) * ratio;
		SmallMap newMap = new SmallMap(view.getId(), scale, start_x, start_z);
		SmallRenderer renderer = new SmallRenderer(newMap);
		MapUtils.setRenderer(view, renderer);
		MapFileManager.addMap(newMap);
		
		return newMap;
	}
	
	public static boolean isSmallMap(int id)
	{
		IMap map = MapFileManager.load(id);
		return map != null && map instanceof SmallMap;
	}
	public static boolean isSmallMap(ItemStack stack)
	{
		Integer id = MapUtils.getMapId(stack);
		if (id == null)
			return false;
		return isSmallMap(id);
	}
	public static boolean isSmallMapByNbt(ItemStack stack)
	{
		return getScale(stack) >= 1;
	}
	/** @return -1 if no scale or scale is not valid */
	public static int getScale(ItemStack stack)
	{
		int scale = NBTUtils.getInt(stack, SCALE_FIELD);
		if (1 <= scale && scale <= 128)
			return scale;
		return -1;
	}
}
