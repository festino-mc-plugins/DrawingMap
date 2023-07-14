package com.festp.maps.drawing;

import java.util.Arrays;

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

public class DrawingMapUtils {
	
	public static final String IS_DRAWING_FIELD = "is_drawing";
	
	public static boolean isDrawingMap(int id)
	{
		IMap map = MapFileManager.load(id);
		return map != null && map instanceof DrawingMap;
	}
	public static boolean isDrawingMap(ItemStack item)
	{
		Integer id = MapUtils.getMapId(item);
		if (id == null)
			return false;
		return isDrawingMap(id);
	}
	public static boolean isDrawingMapByNbt(ItemStack stack)
	{
		return NBTUtils.getBoolean(stack, IS_DRAWING_FIELD);
	}
	
	public static ItemStack getMap(int id) {
		ItemStack item = MapUtils.getVanillaMap(id);
		ItemMeta meta = item.getItemMeta();
		meta.setLore(Arrays.asList(new String[] { "Drawing" }));
		item.setItemMeta(meta);
		return item;
	}
	
	public static MapView createDrawingMap(Location playerLoc, int scale) {
		MapView view = Bukkit.createMap(playerLoc.getWorld());
		view.setScale(Scale.FARTHEST);
		DrawingMap map = new DrawingMap(view.getId(), DrawingInfo.buildFrom(playerLoc, scale));
		MapFileManager.addMap(map);
		return view;
	}
}
