package com.festp.maps;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.map.MapView;

import com.festp.Logger;

public class MapDataMonitor {
	
	private static boolean init = false;
	private static Field CraftWorld_WorldServerField;
	private static Method WorldServer_getDataStorage;
	private static Field WorldPersistentData_MapField;
	private static Field WorldMap_id;
	private static Field WorldMap_mapView;
	
	private final HashSet<String> loadedMaps = new HashSet<>();
	
	public void update() {
		if (!init) {
			init = true;
			initReflection();
		}
		// from Minectaft sources:
		// (WorldMap) WorldServer#getServer().overworld().getDataStorage().get(WorldMap.factory(), mapid.key());
		// skipped getServer().overworld() by using default overworld index (TODO better)
		World overworld = Bukkit.getWorlds().get(0);
    	try {
			Object worldServer = CraftWorld_WorldServerField.get(overworld);
			Object dataStorage = WorldServer_getDataStorage.invoke(worldServer);
			Map<?, ?> map = (Map<?, ?>)WorldPersistentData_MapField.get(dataStorage);
			
			HashSet<String> unloadedMaps = new HashSet<>(loadedMaps);
			
			for (Entry<?, ?> entry : map.entrySet()) {
				Object key = entry.getKey();
				if (!(key instanceof String)) continue;
				String keyString = (String)key;
				if (!keyString.startsWith("map_")) continue;
				
				if (unloadedMaps.remove(keyString)) continue;
				
				loadedMaps.add(keyString);
				onMapLoad(keyString, entry.getValue());
			}
			
			// can add unload event
			loadedMaps.removeAll(unloadedMaps);
		} catch (Exception e) {
			Logger.severe("Couldn't update loaded map data");
			e.printStackTrace();
		}
	}
	
	private static void onMapLoad(int id) {
		MapView mapView = MapUtils.getView(id);
		MapEventHandler.onMapLoad(mapView);
	}
	
	private static void onMapLoad(MapView mapView) {
		MapEventHandler.onMapLoad(mapView);
	}
	
	private static void onMapLoad(String id, Object worldMap) {
		if (WorldMap_id != null && WorldMap_mapView != null) {
			try {
				// I don't know why id is null by default, so I set it according to
				// https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/src/main/java/org/bukkit/craftbukkit/map/CraftMapView.java?until=3ae90697f36e3553994cb0cd840e38e53d30904d&untilPath=src%2Fmain%2Fjava%2Forg%2Fbukkit%2Fcraftbukkit%2Fmap%2FCraftMapView.java&at=refs%2Fheads%2Fversion%2F1.20.6
				WorldMap_id.set(worldMap, id);
				MapView mapView = (MapView)WorldMap_mapView.get(worldMap);
				onMapLoad(mapView);
			} catch (Exception e) {
				Logger.severe("Couldn't update loaded map data for \"" + id + "\"");
				e.printStackTrace();
			}
		}
		else {
			// id field was removed somewhen in 1.20.3-1.21.4
			try {
				int mapId = Integer.parseInt(id.substring("map_".length()));
				onMapLoad(mapId);
			} catch (Exception e) {
				Logger.severe("Couldn't parse map id from \"" + id + "\" and update entry");
				e.printStackTrace();
			}
		}
	}
	
	private static void initReflection() {
		try {
			World overworld = Bukkit.getWorlds().get(0);
			CraftWorld_WorldServerField = overworld.getClass().getDeclaredField("world");
			CraftWorld_WorldServerField.setAccessible(true);
			
			Object worldServer = CraftWorld_WorldServerField.get(overworld);
			for (Method m : worldServer.getClass().getDeclaredMethods()) {
				if (m.getParameterCount() != 0) continue;
				if (!m.getReturnType().getSimpleName().equals("WorldPersistentData")) continue;
				WorldServer_getDataStorage = m;
				break;
			}
			WorldServer_getDataStorage.setAccessible(true);

			Object dataStorage = WorldServer_getDataStorage.invoke(worldServer);
			// TODO search by type Map
			WorldPersistentData_MapField = dataStorage.getClass().getDeclaredField("b");
			WorldPersistentData_MapField.setAccessible(true);
			
			Class<?> WorldMapClass = Class.forName("net.minecraft.world.level.saveddata.maps.WorldMap");

			// may throw on new versions:
			// id field was removed somewhen in 1.20.3-1.21.4
			WorldMap_id = WorldMapClass.getDeclaredField("id");
			WorldMap_id.setAccessible(true);
			if (WorldMap_id.getType() != String.class) {
				WorldMap_id = null;
			}

			WorldMap_mapView = WorldMapClass.getDeclaredField("mapView");
			WorldMap_mapView.setAccessible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
