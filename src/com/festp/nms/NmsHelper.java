package com.festp.nms;

import java.lang.reflect.Field;
import java.util.Map;

import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.map.CraftMapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import com.festp.Logger;
import com.festp.maps.IMap;
import com.festp.maps.MapUtils;

import net.minecraft.world.level.saveddata.maps.WorldMap;

public class NmsHelper {
	
	public static boolean copyPixels(IMap mapFrom, MapView viewTo) {
		try {
			Field fieldImage = viewTo.getClass().getDeclaredField("worldMap");
			fieldImage.setAccessible(true);
			WorldMap mapImage = (WorldMap) fieldImage.get(viewTo);

			MapView oldView = MapUtils.getView(mapFrom);
			Field fieldCanvases = oldView.getClass().getDeclaredField("canvases");
			fieldCanvases.setAccessible(true);
			Object preCanvases = fieldCanvases.get(oldView);
			if (!(preCanvases instanceof Map<?, ?>)) {
				Logger.severe("NmsHelper couldn't get canvases");
				return false;
			}
			@SuppressWarnings("unchecked")
			Map<MapRenderer, Map<CraftPlayer, CraftMapCanvas>> canvases = (Map<MapRenderer, Map<CraftPlayer, CraftMapCanvas>>) preCanvases;
			byte[] colors = new byte[128*128];
			for (Map<CraftPlayer, CraftMapCanvas> pair : canvases.values())
				for (CraftMapCanvas canvas : pair.values())
				{
					for (int x = 0; x < 128; x++)
						for (int z = 0; z < 128; z++)
							colors[x + 128*z] = canvas.getPixel(x, z);
					break;
				}
			mapImage.g = colors; // "g" = "colors"
		} catch (Exception e) {
			Logger.severe("Error while copy pixels from map #" + mapFrom.getId() + " to map view #" + viewTo.getId());
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
