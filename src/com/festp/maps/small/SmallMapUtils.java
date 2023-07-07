package com.festp.maps.small;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapView;
import org.bukkit.map.MapView.Scale;

import com.festp.maps.IMap;
import com.festp.maps.MapFileManager;
import com.festp.maps.MapUtils;
import com.festp.utils.NBTUtils;

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
			MapView newView = genSmallMap(loc, map.getScale() / 2);
			return MapUtils.getMap(newView.getId(), false);
		}
		else
		{
			MapView view = MapUtils.genNewView(map);
			initVanillaMap(view, map.getX() + map.getWidth() / 2, map.getZ() + map.getWidth() / 2);
			return MapUtils.getMap(view.getId());
		}
	}
	public static ItemStack getMap(int id, boolean scaleName) {
		ItemStack item = MapUtils.getVanillaMap(id);
		SmallMap map = (SmallMap) MapFileManager.load(id);
		
		if (scaleName) {
			String name = "{\"translate\":\"item.minecraft.filled_map\"},{\"text\":\" (" + map.getScale() + ":1)\"}";
			item = NBTUtils.setDisplayName(item, name);
		}
		String[] lore = new String[] { "", "{\"translate\":\"filled_map.scale\", \"with\":[\"" + 1.0 / map.getScale() + "\"]}"}; // Scaling at 1:%s
		item = NBTUtils.setLore(item, lore);
		
		return item;
	}
	public static ItemStack getPreExtendedMap(int id)
	{
		SmallMap map = (SmallMap) MapFileManager.load(id);
		ItemStack preMap = MapUtils.getMap(id, true);
		int scale = map.getScale() / 2;
		String name = "{\"translate\":\"item.minecraft.filled_map\"},{\"text\":\" (" + scale + ":1)\"}";
		String[] lore = new String[] { "", "{\"translate\":\"filled_map.scale\", \"with\":[\"" + 1.0 / scale + "\"]}"}; // Scaling at 1:%s
		if (scale == 1) 
			lore = new String[] { "", "{\"text\":\"Vanilla\"}"};
		preMap = NBTUtils.setDisplayName(preMap, name);
		preMap = NBTUtils.setLore(preMap, lore);
		
		return preMap;
	}
	
	/** create new map */
	public static MapView genSmallMap(Location l, int scale)
	{
		MapView view = Bukkit.createMap(l.getWorld());
		initVanillaMap(view, l.getBlockX(), l.getBlockZ());
		view.setTrackingPosition(true);
		view.setUnlimitedTracking(true);

		int ratio = 128 / scale;
		int startX = floorCoord(l.getBlockX(), ratio);
		int startZ = floorCoord(l.getBlockZ(), ratio);
		SmallMap newMap = new SmallMap(view.getId(), scale, startX, startZ);
		
		MapFileManager.addMap(newMap);
		
		return view;
	}
	
	private static void initVanillaMap(MapView view, int x, int z) {
		view.setCenterX(floorCoord(x, 128) + 64);
		view.setCenterZ(floorCoord(z, 128) + 64);
		view.setScale(Scale.CLOSEST); // 1:1
		view.setTrackingPosition(true);
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
