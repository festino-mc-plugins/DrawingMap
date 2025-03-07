package com.festp.maps;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

import com.festp.Logger;

public class MapDataMonitor {
	
	private final JavaPlugin plugin;
	private final HashSet<String> loadedMaps = new HashSet<>();
	
	public MapDataMonitor(JavaPlugin plugin) {
		this.plugin = plugin;
	}
	
	public void update() {

    	try {
        	//UtilsReflection.printAllFields(Bukkit.getWorlds().get(0).getClass());
			//UtilsReflection.printAllFields(Class.forName("net.minecraft.server.level.WorldServer"));
			//UtilsReflection.printAllFields(NmsWorldMapHelper.getCraftbukkitClass("persistence.CraftPersistentDataContainer"));
			Field worldField = Bukkit.getWorlds().get(0).getClass().getDeclaredField("world");
			worldField.setAccessible(true);
			Object worldServer = worldField.get(Bukkit.getWorlds().get(0));
			//UtilsReflection.printAllMethods(worldServer.getClass());
			
			// net.minecraft.world.level.storage.WorldData
			Method getDataStorage7 = null;
			//System.out.println(worldServer.getClass().getCanonicalName() + " has " + worldServer.getClass().getDeclaredMethods().length + " methods:");
			for (Method m : worldServer.getClass().getDeclaredMethods()) {
				if (m.getParameterCount() > 0) continue;
				//System.out.println(m.getReturnType().getSimpleName() + " " + m.getName());
				if (!m.getReturnType().getSimpleName().equals("WorldPersistentData")) continue; // WorldData
				//System.out.println(m.getName());
				getDataStorage7 = m;
				break;
			}
			//Method getDataStorage7 = worldServer.getClass().getDeclaredMethod("z_");
			getDataStorage7.setAccessible(true);
			Object dataStorage7 = getDataStorage7.invoke(worldServer);
			//UtilsReflection.printAllFields(dataStorage7.getClass());
			//UtilsReflection.printAllMethods(dataStorage7.getClass());

			Field mapField = dataStorage7.getClass().getDeclaredField("b");
			mapField.setAccessible(true);
			Map<?, ?> map = (Map<?, ?>)mapField.get(dataStorage7);
			
			HashSet<String> unloadedMaps = new HashSet<>(loadedMaps);
			
			for (Entry<?, ?> entry : map.entrySet()) {
				Object key = entry.getKey();
				if (!(key instanceof String)) continue;
				String keyString = (String)key;
				if (!keyString.startsWith("map_")) continue;
				
				if (unloadedMaps.remove(keyString)) continue;
				
				loadedMaps.add(keyString);
				/*try {
					int mapId = Integer.parseInt(keyString.substring(4));
					onMapLoaded(mapId);
				} catch (Exception e) {
					e.printStackTrace();
				}*/

				Object worldMap = entry.getValue();
				
				// I don't know why id is null by default, so I set it according to
				// https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/src/main/java/org/bukkit/craftbukkit/map/CraftMapView.java?until=3ae90697f36e3553994cb0cd840e38e53d30904d&untilPath=src%2Fmain%2Fjava%2Forg%2Fbukkit%2Fcraftbukkit%2Fmap%2FCraftMapView.java&at=refs%2Fheads%2Fversion%2F1.20.6
				try {
					Field worldMapIdField = worldMap.getClass().getDeclaredField("id");
					worldMapIdField.setAccessible(true);
					worldMapIdField.set(worldMap, keyString);

					Field mapViewField = worldMap.getClass().getDeclaredField("mapView");
					mapViewField.setAccessible(true);
					MapView mapView = (MapView)mapViewField.get(worldMap);
					onMapLoaded(mapView);
				} catch (Exception e) {
					// id was removed somewhen in 1.20.3-1.21.4
					try {
						int mapId = Integer.parseInt(keyString.substring("map_".length()));
						onMapLoaded(mapId);
					} catch (Exception e2) {
						e.printStackTrace();
					}
				}
				
			}
			
			// can add unload event
			loadedMaps.removeAll(unloadedMaps);

			// (WorldMap) WorldServer#getServer().overworld().getDataStorage().get(WorldMap.factory(), mapid.key());
			/*Method getServer = worldServer.getClass().getDeclaredMethod("getServer");
			getServer.setAccessible(true);
			Object server = getServer.invoke(worldServer);
			UtilsReflection.printAllMethods(server.getClass());
			
			Method getOverworld = server.getClass().getDeclaredMethod("overworld");
			getOverworld.setAccessible(true);
			Object overworldObject = getOverworld.invoke(server);
			UtilsReflection.printAllMethods(overworldObject.getClass());
			
			Method getDataStorage = overworldObject.getClass().getDeclaredMethod("getDataStorage");
			getDataStorage.setAccessible(true);
			Object dataStorage = getDataStorage.invoke(overworldObject);
			
			UtilsReflection.printAllFields(dataStorage.getClass());
			UtilsReflection.printAllMethods(dataStorage.getClass());*/
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void onMapLoaded(int id) {
		MapView mapView = MapUtils.getView(id);
		Logger.info("Loaded " + mapView.getId());
		MapEventHandler.onMapLoad(mapView);
	}
	
	private void onMapLoaded(MapView mapView) {
		Logger.info("Loaded " + mapView.getId());
		MapEventHandler.onMapLoad(mapView);
	}
}
