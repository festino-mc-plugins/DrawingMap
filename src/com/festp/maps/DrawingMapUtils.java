package com.festp.maps;

import org.bukkit.inventory.ItemStack;

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
}
