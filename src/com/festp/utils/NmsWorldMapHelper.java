package com.festp.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
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
	
	/** Use for paths like "org.bukkit.craftbukkit.v1_19_R1".
	 * @param relativePath is something like "map.CraftMapCanvas" for "org.bukkit.craftbukkit.v1_19_R1.map.CraftMapCanvas" class */
	public static Class<?> getCraftbukkitClass(String relativePath)
	{
		String version = getVersionString();
		try {
			return Class.forName("org.bukkit.craftbukkit." + version + "." + relativePath);
		} catch (ClassNotFoundException e) { }
		return null;
	}
	private static String getVersionString()
	{
		return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
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

	public static byte[] getColors(MapCanvas canvas) {
		try {
			//canvas.buffer
			Class<?> classCraftMapCanvas = getCraftbukkitClass("map.CraftMapCanvas");
			Field fieldBuffer = classCraftMapCanvas.getDeclaredField("buffer");
			fieldBuffer.setAccessible(true);
			Object buffer = fieldBuffer.get(canvas);
			return (byte[])buffer;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/** use <b>data[y * 128 + x]</b> to get colors by x and y coordinates 
	 * @return <b>null</b> if couldn't get pixels */
	public static byte[] getColors(MapView mapView)
	{
		/* org.bukkit.craftbukkit.v1_18_R1.map.CraftMapCanvas
		 * public byte getPixel(int x, int y);
	     if (x < 0 || y < 0 || x >= 128 || y >= 128)
	         return 0;
	     byte[] data = this.buffer;
	     return data[y * 128 + x]; */
		Map<MapRenderer, Map<Player, MapCanvas>> canvases = getCanvases(mapView);
		if (canvases == null)
			return null;
		for (Map<Player, MapCanvas> pair : canvases.values()) {
			for (MapCanvas canvas : pair.values()) {
				return getColors(canvas);
			}
		}
		return null;
	}
	public static MapCanvas getCanvas(MapView view, MapRenderer renderer, Player player) {
		Map<MapRenderer, Map<Player, MapCanvas>> canvases = getCanvases(view);
		for (Entry<MapRenderer, Map<Player, MapCanvas>> rendererEntry : canvases.entrySet()) {
			if (renderer.equals(rendererEntry.getKey())) {
				for (Entry<Player, MapCanvas> playerEntry : rendererEntry.getValue().entrySet()) {
					if (player.equals(playerEntry.getKey()) || !renderer.isContextual()) {
						return (MapCanvas)playerEntry.getValue();
					}
				}
			}
		}
		return null;
	}
	/** Map<MapRenderer, Map<CraftPlayer, CraftMapCanvas>> */
	@SuppressWarnings("unchecked")
	private static Map<MapRenderer, Map<Player, MapCanvas>> getCanvases(MapView mapView) {
		try {
			Field fieldCanvases = mapView.getClass().getDeclaredField("canvases");
			fieldCanvases.setAccessible(true);
			Object preCanvases = fieldCanvases.get(mapView);
			if (!(preCanvases instanceof Map<?, ?>)) {
				Logger.severe(NmsWorldMapHelper.class.getSimpleName() + " couldn't get canvases");
				return null;
			}

			//Map<MapRenderer, Map<CraftPlayer, CraftMapCanvas>> canvases = (Map<MapRenderer, Map<CraftPlayer, CraftMapCanvas>>) preCanvases;
			return (Map<MapRenderer, Map<Player, MapCanvas>>) preCanvases;
		} catch (Exception e) {
			Logger.severe("Error while get pixels from map #" + mapView.getId());
			e.printStackTrace();
			return null;
		}
	}

	public static boolean copyPixels(IMap mapFrom, MapView viewTo)
	{
		try {
			MapView oldView = MapUtils.getView(mapFrom);
			byte[] pixels = getColors(oldView);
			if (pixels == null)
				return false;
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
	
	public static MapCanvas getCanvasBefore(MapView view, MapRenderer beforeRenderer, Player player) {
		MapRenderer prevRenderer = getRendererBefore(view, beforeRenderer);
		return getCanvas(view, prevRenderer, player);
	}
	private static MapRenderer getRendererBefore(MapView view, MapRenderer beforeRenderer) {
		MapRenderer prevRenderer = null;
		for (MapRenderer renderer : view.getRenderers()) {
			if (renderer.equals(beforeRenderer))
				return prevRenderer;
			prevRenderer = renderer;
		}
		return null;
	}
}
