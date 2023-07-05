package com.festp.maps;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapRenderer;
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
			SmallMap newMap = genSmallMap(loc, map.getScale() / 2);
			return MapUtils.getMap(newMap.getId(), false);
		}
		else
		{
			MapView view = MapUtils.genNewView(map);
			initVanillaMap(view, map.getX() + map.getWidth() / 2, map.getZ() + map.getWidth() / 2);
			return MapUtils.getMap(view.getId());
		}
	}
	public static ItemStack getPreExtendedMap(int id)
	{
		SmallMap map = (SmallMap) MapFileManager.load(id);
		ItemStack preMap = MapUtils.getMap(id, true);
		ItemMeta preMapMeta = preMap.getItemMeta();
		int scale = map.getScale() / 2;
		preMapMeta.setDisplayName("Map (" + scale + ":1)");
		String[] lore = new String[] { "Scaling at " + scale + ":1" };
		preMapMeta.setLore(Lists.asList("", lore));
		preMap.setItemMeta(preMapMeta);
		
		return preMap;
	}
	
	/** create new map and attach renderer*/
	public static SmallMap genSmallMap(Location l, int scale)
	{
		MapView view = Bukkit.createMap(l.getWorld());
		initVanillaMap(view, l.getBlockX(), l.getBlockZ());
		view.setTrackingPosition(true);
		view.setUnlimitedTracking(true);
		MapRenderer vanillaRenderer = MapUtils.removeRenderers(view);

		int ratio = 128 / scale;
		int startX = floorCoord(l.getBlockX(), ratio);
		int startZ = floorCoord(l.getBlockZ(), ratio);
		SmallMap newMap = new SmallMap(view.getId(), scale, startX, startZ);
		SmallRenderer renderer = new SmallRenderer(newMap, vanillaRenderer);
		MapUtils.addRenderer(view, renderer);
		
		MapFileManager.addMap(newMap);
		
		return newMap;
	}
	
	private static void initVanillaMap(MapView view, int x, int z) {
		view.setCenterX(floorCoord(x, 128) + 64);
		view.setCenterZ(floorCoord(z, 128) + 64);
		view.setScale(Scale.CLOSEST); // 1:1
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
	
	private static int floorCoord(int x, int step) {
		// vanilla maps grid aligned on (-64, -64), center is (0, 0) for 1:1
		return (int)Math.floor((x + 64) / (float)step) * step - 64;
		//return (int)Math.floor(x / (float)step) * step;
	}
}
