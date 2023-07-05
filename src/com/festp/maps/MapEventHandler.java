package com.festp.maps;

import java.awt.image.BufferedImage;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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

import com.festp.utils.Utils;

public class MapEventHandler implements Listener {

	// TODO add reload persistence
	/** load last session map canvas */
	@EventHandler
	public void onMapLoad(MapInitializeEvent event)
	{
		MapView mapView = event.getMap();
		int id = mapView.getId();
		if (MapFileManager.isLoaded(id))
			return;
		
		IMap map = MapFileManager.load(id);
		if (map == null)
			return;

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
	}

	/** init new map */
	@EventHandler
	public void onPlayerInitMap(PlayerInteractEvent event)
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
		Player player = event.getPlayer();
		if (item.getType() == Material.MAP) {
			ItemStack mapItem;
			if (SmallMapUtils.isSmallMapByNbt(item)) {
				int scale = SmallMapUtils.getScale(item);
				SmallMap map = SmallMapUtils.genSmallMap(player.getLocation(), scale);
				mapItem = MapUtils.getMap(map.getId());
			} else if (DrawingMapUtils.isDrawingMapByNbt(item)) {
				MapView view = Bukkit.createMap(event.getPlayer().getWorld());
				view.setScale(Scale.FARTHEST);
				DrawingMap newMap = new DrawingMap(view.getId(), DrawingInfo.buildFrom(player.getLocation()));
				
				MapRenderer vanillaRenderer = MapUtils.removeRenderers(view);
				DrawingRenderer renderer = new DrawingRenderer(newMap, vanillaRenderer);
				MapUtils.addRenderer(view, renderer);
				MapFileManager.addMap(newMap);
				
				mapItem = MapUtils.getMap(view.getId());
			} else {
				return;
			}

			player.getWorld().playSound(player, Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundCategory.PLAYERS, 1, 1);
			event.setCancelled(true);
			event.setUseInteractedBlock(Result.DENY);
			
			if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
				item.setAmount(item.getAmount() - 1);
			else
				if (player.getInventory().firstEmpty() < 0)
					return;
			
			Utils.giveOrDrop(player, mapItem);
		}
		if (item.getType() == Material.FILLED_MAP) {
			if (!DrawingMapUtils.isDrawingMap(item))
				return;
			if (event.getHand() == EquipmentSlot.OFF_HAND)
				return;
			
			DrawingMap map = (DrawingMap) MapFileManager.load(MapUtils.getMapId(item));
			if (map == null)
				return;

			event.setCancelled(true);
			map.setInfo(DrawingInfo.buildFrom(player.getLocation()));
			MapUtils.getView(map).setWorld(player.getWorld());;
			map.needReset = true;
			MapFileManager.save(map);
		}
	}
}
