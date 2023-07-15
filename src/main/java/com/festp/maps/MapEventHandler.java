package com.festp.maps;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import com.festp.maps.drawing.DrawingInfo;
import com.festp.maps.drawing.DrawingMap;
import com.festp.maps.drawing.DrawingMapUtils;
import com.festp.maps.drawing.DrawingRenderer;
import com.festp.maps.nether.NetherCursorRenderer;
import com.festp.maps.small.SmallMap;
import com.festp.maps.small.SmallMapUtils;
import com.festp.maps.small.SmallRenderer;
import com.festp.utils.Utils;

public class MapEventHandler implements Listener {
	private static final int[] DRAWING_SCALES = { 8, 4, 2, 1 };

	/** Init new map or reinit drawing map */
	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event)
	{
		if (event.useInteractedBlock() == Result.DEFAULT)
			return;
		if (event.useItemInHand() == Result.DENY)
			return;
		
		if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;

		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType().isInteractable())
			return;

		if (!event.hasItem())
			return;
		
		ItemStack item = event.getItem();
		if (item.getType() == Material.MAP) {
			MapView newView = initMap(event);
			if (newView != null) {
				event.setCancelled(true);
				event.setUseInteractedBlock(Result.DENY);
				MapUtils.playInitSound(event.getPlayer());
				// onMapLoad was called before
				initRenderers(newView);
			}
		}
		if (item.getType() == Material.FILLED_MAP) {
			if (event.getHand() == EquipmentSlot.OFF_HAND)
				return;
			IMap newMap = reinitMap(item, event.getPlayer().getLocation());
			if (newMap != null) {
				event.setCancelled(true);
				event.setUseInteractedBlock(Result.DENY);
				MapUtils.playInitSound(event.getPlayer());
			}
		}
	}

	// TODO add /reload persistence
	/** Set up renderers and load last session map canvas */
	@EventHandler
	public void onMapLoad(MapInitializeEvent event)
	{
		onMapLoad(event.getMap());
	}
	
	public static void onMapLoad(MapView mapView) {
		MapRenderer mainRenderer = MapUtils.getMainRenderer(mapView);
		//System.out.println(mapView.getId() + " " + mapView.getRenderers().size() + " " + mainRenderer + (mainRenderer instanceof NetherCursorRenderer ? " " + ((NetherCursorRenderer)mainRenderer).getPrevRenderer() : ""));
		if (mainRenderer instanceof SmallRenderer)
			return;
		if (mainRenderer instanceof DrawingRenderer)
			return;
		if (MapFileManager.load(mapView.getId()) == null) { // it is vanilla
			// check if was initialized 
			if (mapView.getRenderers().size() != 1)
				return;
			if (mapView.getRenderers().get(0) instanceof NetherCursorRenderer)
				return;
		}
		// map was not initialized
		//  or was initialized as vanilla, but it is not vanilla
		initRenderers(mapView);
	}
	
	private static void initRenderers(MapView mapView) {
		IMap map = initMainRenderer(mapView);
		initNetherCursorRenderer(mapView, map);
	}
	
	private static IMap initMainRenderer(MapView mapView) {
		int id = mapView.getId();
		IMap map = MapFileManager.load(id);
		if (map == null) {
			return null;
		}

		MapRenderer vanillaRenderer = MapUtils.removeRenderers(mapView);
		MapRenderer renderer = null;
		if (map instanceof SmallMap) {
			renderer = new SmallRenderer((SmallMap) map, vanillaRenderer);
		} else if (map instanceof DrawingMap) {
			renderer = new DrawingRenderer((DrawingMap) map, vanillaRenderer);
		}
		if (renderer != null) {
			mapView.addRenderer(renderer);
		}
		return map;
	}
	
	private static void initNetherCursorRenderer(MapView mapView, IMap map) {
		// TODO use configuration to enable nether cursors
		if (mapView.getWorld().getEnvironment() != Environment.NORMAL)
			return;
		if (!mapView.isTrackingPosition())
			return;
		if (map != null && !(map instanceof SmallMap))
			return;
		NetherCursorRenderer.add(mapView);
	}
	
	private MapView initMap(PlayerInteractEvent event) {
		MapView view = null;
		ItemStack item = event.getItem();
		Player player = event.getPlayer();
		ItemStack mapItem;
		if (SmallMapUtils.isSmallMapByNbt(item)) {
			int scale = SmallMapUtils.getScale(item);
			view = SmallMapUtils.createSmallMap(player.getLocation(), scale);
		} else if (DrawingMapUtils.isDrawingMapByNbt(item)) {
			view = DrawingMapUtils.createDrawingMap(player.getLocation(), DRAWING_SCALES[0]);
		} else {
			return null;
		}
		mapItem = MapUtils.getMap(view.getId());
		
		if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
			item.setAmount(item.getAmount() - 1);
		else
			if (player.getInventory().firstEmpty() < 0)
				return view;
		
		Utils.giveOrDrop(player, mapItem);
		return view;
	}
	
	private IMap reinitMap(ItemStack item, Location newLoc) {
		if (!DrawingMapUtils.isDrawingMap(item))
			return null;
		
		DrawingMap map = (DrawingMap) MapFileManager.load(MapUtils.getMapId(item));
		if (map == null)
			return null;

		reinitDrawingMap(map, newLoc);
		return map;
	}

	private void reinitDrawingMap(DrawingMap map, Location newLoc) {
		World newWorld = newLoc.getWorld();
		int oldScale = map.getScale();
		int scaleIndex = getScaleIndex(oldScale);
		if (scaleIndex == -1) {
			// map can't change scale
			map.setInfo(DrawingInfo.buildFrom(newLoc, oldScale), newWorld);
			return;
		}

		DrawingInfo info = DrawingInfo.buildFrom(newLoc, DRAWING_SCALES[0]);
		if (!isEqualPlane(info, map)) {
			// full reinit
			map.setInfo(info, newWorld);
			return;
		}
		
		// shift scales
		int newScaleIndex = (scaleIndex + 1) % DRAWING_SCALES.length;
		int newScale = DRAWING_SCALES[newScaleIndex];
		info = DrawingInfo.buildFrom(newLoc, newScale);
		map.setInfo(info, newWorld);
		if (newScaleIndex == 0) {
			// TODO playsound? minecraft:block.grass.step
		}
		return;
	}
	
	private static int getScaleIndex(int scale) {
		int scaleIndex = -1;
		for (int i = 0; i < DRAWING_SCALES.length; i++) {
			if (DRAWING_SCALES[i] == scale) {
				scaleIndex = i;
				break;
			}
		}
		return scaleIndex;
	}
	private static boolean isEqualPlane(DrawingInfo info, DrawingMap map) {
		return info.xCenter == map.getX()
			&& info.yCenter == map.getY()
			&& info.zCenter == map.getZ()
			&& info.state == map.getDirection();
	}
}
