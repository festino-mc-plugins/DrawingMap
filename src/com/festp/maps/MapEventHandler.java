package com.festp.maps;

import java.awt.image.BufferedImage;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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
import org.bukkit.map.MapView.Scale;

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

	/** Init new map */
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
			}
		}
	}

	// TODO add /reload persistence
	/** Set up renderers and load last session map canvas */
	@EventHandler
	public void onMapLoad(MapInitializeEvent event)
	{
		MapView mapView = event.getMap();
		int id = mapView.getId();
		if (MapFileManager.isLoaded(id))
			return;
		
		initRenderers(mapView);
	}
	
	private void initRenderers(MapView mapView) {
		IMap map = initMainRenderer(mapView);
		initNetherCursorRenderer(mapView, map);
	}
	
	private IMap initMainRenderer(MapView mapView) {
		int id = mapView.getId();
		IMap map = MapFileManager.load(id);
		if (map == null) {
			return null;
		}

		MapRenderer vanillaRenderer = MapUtils.removeRenderers(mapView);
		AbstractRenderer renderer = null;
		if (map instanceof SmallMap) {
			renderer = new SmallRenderer((SmallMap) map, vanillaRenderer);
		} else if (map instanceof DrawingMap) {
			renderer = new DrawingRenderer((DrawingMap) map, vanillaRenderer);
		}
		if (renderer != null) {
			MapUtils.addRenderer(mapView, renderer);
			BufferedImage image = MapFileManager.loadImage(id);
			if (image != null)
			{
				renderer.renderImage(image);
			}
		}
		return map;
	}
	
	private void initNetherCursorRenderer(MapView mapView, IMap map) {
		// TODO use configuration to enable nether cursors
		if (mapView.getWorld().getEnvironment() != Environment.NORMAL)
			return;
		if (!mapView.isTrackingPosition())
			return;
		if (map != null && !(map instanceof SmallMap))
			return;
		MapUtils.addRenderer(mapView, new NetherCursorRenderer(mapView));
	}
	
	private MapView initMap(PlayerInteractEvent event) {
		MapView res = null;
		ItemStack item = event.getItem();
		Player player = event.getPlayer();
		ItemStack mapItem;
		if (SmallMapUtils.isSmallMapByNbt(item)) {
			int scale = SmallMapUtils.getScale(item);
			MapView view = SmallMapUtils.genSmallMap(player.getLocation(), scale);
			mapItem = MapUtils.getMap(view.getId());
			res = view;
		} else if (DrawingMapUtils.isDrawingMapByNbt(item)) {
			MapView view = Bukkit.createMap(player.getWorld());
			view.setScale(Scale.FARTHEST);
			DrawingMap map = new DrawingMap(view.getId(), DrawingInfo.buildFrom(player.getLocation()));
			MapFileManager.addMap(map);
			mapItem = MapUtils.getMap(view.getId());
			res = view;
		} else {
			return null;
		}

		player.getWorld().playSound(player, Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundCategory.PLAYERS, 1, 1);
		
		if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
			item.setAmount(item.getAmount() - 1);
		else
			if (player.getInventory().firstEmpty() < 0)
				return res;
		
		Utils.giveOrDrop(player, mapItem);
		return res;
	}
	
	private IMap reinitMap(ItemStack item, Location newLoc) {
		if (!DrawingMapUtils.isDrawingMap(item))
			return null;
		
		DrawingMap map = (DrawingMap) MapFileManager.load(MapUtils.getMapId(item));
		if (map == null)
			return null;

		map.setInfo(DrawingInfo.buildFrom(newLoc));
		MapUtils.getView(map).setWorld(newLoc.getWorld());;
		map.needReset = true;
		MapFileManager.save(map);
		return map;
	}
}
