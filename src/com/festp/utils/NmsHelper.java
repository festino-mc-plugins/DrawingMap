package com.festp.utils;

import java.lang.reflect.Field;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import com.festp.Logger;
import com.festp.maps.IMap;
import com.festp.maps.MapUtils;

/**
 * Should be checked every spigot update
 */
public class NmsHelper {
	
	/** Use for paths like "org.bukkit.craftbukkit.v1_19_R1". */
	private static Class<?> getCraftbukkitClass(String relativePath)
	{
		String version = getVersionString();
		try {
			return Class.forName("org.bukkit.craftbukkit." + version + "." + relativePath);
		} catch (ClassNotFoundException e) { }
		return null;
	}
	
	private static Class<?> getNmsClass_WorldMap()
	{
		boolean isAbove1_17 = true;
		try {
			if (isAbove1_17)
				return Class.forName("net.minecraft.world.level.saveddata.maps.WorldMap");
			// net.minecraft.server.v1_16_R3.WorldMap
			String version = getVersionString();
			return Class.forName("net.minecraft.server." + version + ".WorldMap");
		} catch (ClassNotFoundException e) { }
		return null;
	}
	private static String getVersionString()
	{
		return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
	}
	

	@SuppressWarnings("unchecked")
	public static boolean copyPixels(IMap mapFrom, MapView viewTo) {
		try {
			MapView oldView = MapUtils.getView(mapFrom);
			Field fieldCanvases = oldView.getClass().getDeclaredField("canvases");
			fieldCanvases.setAccessible(true);
			Object preCanvases = fieldCanvases.get(oldView);
			if (!(preCanvases instanceof Map<?, ?>)) {
				Logger.severe("NmsHelper couldn't get canvases");
				return false;
			}
			
			Class<?> classCraftMapCanvas = getCraftbukkitClass("map.CraftMapCanvas");
			Field fieldBuffer = classCraftMapCanvas.getDeclaredField("buffer");
			fieldBuffer.setAccessible(true);
			byte[] pixels = null;
			//Map<MapRenderer, Map<CraftPlayer, CraftMapCanvas>> canvases = (Map<MapRenderer, Map<CraftPlayer, CraftMapCanvas>>) preCanvases;
			//for (Map<CraftPlayer, CraftMapCanvas> pair : canvases.values())
			Map<MapRenderer, Map<Object, Object>> canvases = (Map<MapRenderer, Map<Object, Object>>) preCanvases;
			for (Map<Object, Object> pair : canvases.values()) {
				//for (CraftMapCanvas canvas : pair.values())
				for (Object canvas : pair.values())
				{
					Object buffer = fieldBuffer.get(canvas);
					pixels = (byte[])buffer;
					break;
				}
			}
			byte[] colors = new byte[128 * 128];
			for (int i = 0; i < colors.length; i++) {
				colors[i] = pixels[i];
			}

			Field fieldImage = viewTo.getClass().getDeclaredField("worldMap");
			fieldImage.setAccessible(true);
			Object worldMap = fieldImage.get(viewTo);
			Class<?> classWorldMap = getNmsClass_WorldMap();
			Field fieldColors = classWorldMap.getDeclaredField("g"); // "g" = "colors"
			fieldColors.setAccessible(true);
			fieldColors.set(worldMap, colors);
		} catch (Exception e) {
			Logger.severe("Error while copy pixels from map #" + mapFrom.getId() + " to map view #" + viewTo.getId());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/* org.bukkit.craftbukkit.v1_18_R1.map.CraftMapCanvas
	 * public byte getPixel(int x, int y);
     if (x < 0 || y < 0 || x >= 128 || y >= 128)
         return 0;
     byte[] data = this.buffer;
     return data[y * 128 + x]; */
}
