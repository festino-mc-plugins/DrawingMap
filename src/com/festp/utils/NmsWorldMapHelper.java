package com.festp.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import com.festp.Logger;
import com.festp.maps.IMap;
import com.festp.maps.MapUtils;

/**
 * Should be checked every spigot update
 */
public class NmsWorldMapHelper
{
	private static final Class<?> MAP_ICON_CLASS = getNmsClass_MapIcon();
	
	/** Use for paths like "org.bukkit.craftbukkit.v1_19_R1". */
	private static Class<?> getCraftbukkitClass(String relativePath)
	{
		String version = getVersionString();
		try {
			return Class.forName("org.bukkit.craftbukkit." + version + "." + relativePath);
		} catch (ClassNotFoundException e) { }
		return null;
	}
	
	private static Class<?> getNmsClass_MapIcon()
	{
		try {
			if (UtilsVersion.USE_VERSION_INDEPENDENT_NMS)
				return Class.forName("net.minecraft.world.level.saveddata.maps.MapIcon");
			// net.minecraft.server.v1_16_R3.MapIcon
			String version = getVersionString();
			return Class.forName("net.minecraft.server." + version + ".MapIcon");
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
				Logger.severe("NmsWorldMapHelper couldn't get canvases");
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
			/* org.bukkit.craftbukkit.v1_18_R1.map.CraftMapCanvas
			 * public byte getPixel(int x, int y);
		     if (x < 0 || y < 0 || x >= 128 || y >= 128)
		         return 0;
		     byte[] data = this.buffer;
		     return data[y * 128 + x]; */
			byte[] colors = new byte[128 * 128];
			for (int i = 0; i < colors.length; i++) {
				colors[i] = pixels[i];
			}
			Field fieldWorldMap = viewTo.getClass().getDeclaredField("worldMap");
			fieldWorldMap.setAccessible(true);
			Object worldMap = fieldWorldMap.get(viewTo);
			Field fieldColors = worldMap.getClass().getDeclaredField("g"); // "g" = "colors"
			fieldColors.setAccessible(true);
			fieldColors.set(worldMap, colors);
		} catch (Exception e) {
			Logger.severe("Error while copy pixels from map #" + mapFrom.getId() + " to map view #" + viewTo.getId());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	public static MapCursorCollection getCursors(MapView view)
	{
		MapCursorCollection cursors = new MapCursorCollection();
		try {
			Field fieldWorldMap = view.getClass().getDeclaredField("worldMap");
			fieldWorldMap.setAccessible(true);
			Object worldMap = fieldWorldMap.get(view);
			Field fieldDecorations = worldMap.getClass().getDeclaredField("q");
			fieldDecorations.setAccessible(true);
			Object preDecorations = fieldDecorations.get(worldMap);
			Map<String, Object> decorations = (Map<String, Object>)preDecorations;

			Method methodGetX = MAP_ICON_CLASS.getDeclaredMethod("c");
			Method methodGetY = MAP_ICON_CLASS.getDeclaredMethod("d");
			Method methodGetRot = MAP_ICON_CLASS.getDeclaredMethod("e");
			Method methodGetType = MAP_ICON_CLASS.getDeclaredMethod("b");
			Method methodGetChatComponent = MAP_ICON_CLASS.getDeclaredMethod("g");
			
			//for (String key : worldMap.decorations.keySet()) {
			for (String key : decorations.keySet()) {
	            //MapIcon decoration = worldMap.decorations.get(key);
				//cursors.addCursor(decoration.getX(), decoration.getY(), (byte) (decoration.getRot() & 15), decoration.getType().getIcon(), true,
				//	CraftChatMessage.fromComponent(decoration.getName()));
				Object decoration = decorations.get(key);
				//cursors.addCursor(decoration.c(), decoration.d(), (byte) (decoration.e() & 15), decoration.b().a(), true, decoration.g().getString());
				byte x = (byte) methodGetX.invoke(decoration);
				byte y = (byte) methodGetY.invoke(decoration);
				byte rot = (byte) methodGetRot.invoke(decoration);
				Object type = methodGetType.invoke(decoration);
				Method methodGetIcon = type.getClass().getDeclaredMethod("a");
				byte icon = (byte) methodGetIcon.invoke(type);
				Object chatComponent = methodGetChatComponent.invoke(decoration);
				String name = null;
				if (chatComponent != null) {
					Method methodGetString = chatComponent.getClass().getMethod("getString");
					name = (String) methodGetString.invoke(chatComponent);
				}
	            cursors.addCursor(x, y, (byte) (rot & 15), icon, true, name);
			}
			/*Field fieldHumanTrackers = worldMap.getClass().getDeclaredField("n");
			fieldHumanTrackers.setAccessible(true);
			Object preHumanTrackers = fieldHumanTrackers.get(worldMap);
			List<Object> humanTrackers = (List<Object>) preHumanTrackers;
			for (Object humanTracker : humanTrackers) {
				System.out.println(humanTracker);
				WorldMapHumanTracker tracker = (WorldMapHumanTracker)humanTracker;
			}
			System.out.println(humanTrackers.size());*/
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cursors;
	}
}
