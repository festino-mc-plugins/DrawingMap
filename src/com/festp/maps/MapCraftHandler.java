package com.festp.maps;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CartographyInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MapView.Scale;

import com.festp.CraftManager;
import com.festp.DelayedTask;
import com.festp.Logger;
import com.festp.Main;
import com.festp.TaskList;
import com.festp.maps.drawing.DrawingMap;
import com.festp.maps.drawing.DrawingMapUtils;
import com.festp.maps.drawing.DrawingRenderer;
import com.festp.maps.small.SmallMap;
import com.festp.maps.small.SmallMapUtils;
import com.festp.utils.NBTUtils;
import com.festp.utils.NmsWorldMapHelper;
import com.festp.utils.Utils;
import com.google.common.collect.Lists;

import net.md_5.bungee.api.ChatColor;

public class MapCraftHandler implements Listener {

	protected static class CraftMapInfo {
		int paperCount = 0;
		int smallCount = 0;
		int emptySmallCount = 0;
		int drawingCount = 0;
		int emptyDrawingCount = 0;
		ItemStack smallMap = null;
		ItemStack drawingMap = null;
		
		public static CraftMapInfo get(ItemStack[] matrix) {
			CraftMapInfo info = new CraftMapInfo();
			for (ItemStack item : matrix)
				if (item != null)
					if (item.getType() == Material.PAPER) {
						info.paperCount++;
					} else if (item.getType() == Material.MAP) {
						if (SmallMapUtils.isSmallMapByNbt(item)) {
							info.emptySmallCount++;
						} else if (DrawingMapUtils.isDrawingMapByNbt(item)) {
							info.emptyDrawingCount++;
						}
					} else if (SmallMapUtils.isSmallMap(item)) {
						info.smallCount++;
						info.smallMap = item;
					} else if (DrawingMapUtils.isDrawingMap(item)) {
						info.drawingCount++;
						info.drawingMap = item;
					}
			return info;
		}
	}
	
	@EventHandler
	public void onPrepareCraft(PrepareItemCraftEvent event)
	{
		ItemStack[] matrix = event.getInventory().getMatrix();
		CraftMapInfo info = CraftMapInfo.get(matrix);
		
		if (info.smallCount == 1 && info.paperCount == 8) {
			int id = MapUtils.getMapId(info.smallMap);
			ItemStack preMap = SmallMapUtils.getPreExtendedMap(id);
			event.getInventory().setResult(preMap);
		} else if (info.smallCount > 1 || info.emptySmallCount > 0) {
			event.getInventory().setResult(null);
		}
		if (info.emptyDrawingCount > 0 || info.drawingCount > 0) {
			event.getInventory().setResult(null);
		}
	}

	@EventHandler
	public void onCraft(CraftItemEvent event) {
		ItemStack[] matrix = event.getInventory().getMatrix();
		CraftMapInfo info = CraftMapInfo.get(matrix);

		if (info.smallCount == 1 && info.paperCount == 8) {
			int id = MapUtils.getMapId(info.smallMap);
			SmallMap map = (SmallMap) MapFileManager.load(id);
			ItemStack newMap = SmallMapUtils.extendMap(map);
			event.getInventory().setResult(newMap);
		} else if (info.smallCount > 1 || info.emptySmallCount > 0) {
			event.setCancelled(true);
			event.getInventory().setResult(null);
		}
		if (info.emptyDrawingCount > 0 || info.drawingCount > 0) {
			event.setCancelled(true);
			event.getInventory().setResult(null);
		}
	}

	@EventHandler
	public void onCartographyTable(InventoryClickEvent event)
	{
		if (event.isCancelled())
			return;
		if ( !(event.getView().getTopInventory() instanceof CartographyInventory) )
			return;
		
		CartographyInventory inv = (CartographyInventory)event.getView().getTopInventory();
		ItemStack item0 = inv.getItem(0);
		ItemStack item1 = inv.getItem(1);
		if (item0 == null && item1 == null)
			return;
		
		Integer id = MapUtils.getMapId(item0);
		if (id == null) {
			return;
		}
		IMap m = MapFileManager.load(id);

		int maxId = MapUtils.getMaxId();
		boolean wasCrafted = false;
		if (m instanceof SmallMap) {
			wasCrafted = onSmallCartography(event, (SmallMap) m);
		} else if (m instanceof DrawingMap) {
			wasCrafted = onDrawingCartography(event, (DrawingMap) m);
		} else if (m != null) {
			event.setCancelled(true);
			inv.setItem(2, null);
			((Player)event.getWhoClicked()).updateInventory();
			return;
		}
		if (wasCrafted) {
			MapUtils.playCraftSound(inv.getLocation().getBlock());
		}

		// init vanilla (nether cursors) and IMaps
		TaskList.add(new DelayedTask(1, new Runnable() {
			@Override
			public void run() {
				initRenderers(maxId);
			}
		}));
	}
	
	@SuppressWarnings("deprecation")
	private static void initRenderers(int lastMaxId) {
		int newMaxId = MapUtils.getMaxId();
		for (int i = lastMaxId + 1; i <= newMaxId; i++) {
			MapEventHandler.onMapLoad(Bukkit.getMap(i));
		}
	}
	
	public boolean onSmallCartography(InventoryClickEvent event, SmallMap map) {
		CartographyInventory inv = (CartographyInventory)event.getView().getTopInventory();
		ItemStack item0 = inv.getItem(0);
		ItemStack item1 = inv.getItem(1);
		
		int id = map.getId();

		// prepare craft: if 0 or 1 changed
		Runnable preAction = new Runnable() {
			@Override
			public void run() {
				ItemStack preMap = MapUtils.getMap(id, true);
				if (inv.contains(Material.PAPER) && map.getScale() / 2 > 1) {
					preMap = SmallMapUtils.getPreExtendedMap(id);
				}
				else if (inv.contains(Material.GLASS_PANE)) {
					ItemMeta preMapMeta = preMap.getItemMeta();
					preMapMeta.setDisplayName("Map (" + map.getScale() + ":1)");
					preMapMeta.setLore(Lists.asList("", new String[] {ChatColor.GRAY+"Locked"}));
					preMap.setItemMeta(preMapMeta);
				}
				if (inv.contains(Material.PAPER) || inv.contains(Material.GLASS_PANE)) { // pass copy
					inv.setItem(2, preMap);
				}
				Runnable updateAction = new Runnable() { @Override
					public void run() {
						for (HumanEntity human : inv.getViewers())
							((Player)human).updateInventory();
					} };
				DelayedTask updateTask = new DelayedTask(1, updateAction);
				TaskList.add(updateTask);
			} };
		DelayedTask preTask = new DelayedTask(1, preAction);
		TaskList.add(preTask);
		
		//craft: if 2 moved/dropped
		if (inv == event.getClickedInventory() && event.getSlot() == 2 &&
				(event.getAction() == InventoryAction.DROP_ALL_SLOT || event.getAction() == InventoryAction.DROP_ONE_SLOT
				|| event.getAction() == InventoryAction.PICKUP_ALL || event.getAction() == InventoryAction.PICKUP_HALF
				|| event.getAction() == InventoryAction.PICKUP_ONE || event.getAction() == InventoryAction.PICKUP_SOME
				|| (event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD || event.getAction() == InventoryAction.HOTBAR_SWAP)
					&& event.getView().getBottomInventory().getItem(event.getHotbarButton()) == null
				|| event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
					&& MapUtils.getEmptySlot(event.getWhoClicked().getInventory()) >= 0) )
		{
			ItemStack mapItem = inv.getItem(2);
			// extending
			if (inv.contains(Material.PAPER))
			{
				mapItem = SmallMapUtils.extendMap(map);
			}
			// new locked
			else if (inv.contains(Material.GLASS_PANE))
			{
				MapView view = MapUtils.genNewView(map);
				view.setCenterX(0);
				view.setCenterZ(0);
				view.setScale(Scale.CLOSEST);
				view.setLocked(true);
				NmsWorldMapHelper.copyPixels(map, view);
				mapItem = MapUtils.getMap(view.getId());
			} else {
				return false;
			}

			event.setCancelled(true);
			item0.setAmount(item0.getAmount() - 1);
			item1.setAmount(item1.getAmount() - 1);
			// TODO: try to stack
			if (event.getAction() == InventoryAction.DROP_ALL_SLOT || event.getAction() == InventoryAction.DROP_ONE_SLOT)
				Utils.drop(event.getWhoClicked().getEyeLocation(), mapItem, 1);
			else if (event.getAction() == InventoryAction.PICKUP_ALL || event.getAction() == InventoryAction.PICKUP_HALF
					|| event.getAction() == InventoryAction.PICKUP_ONE || event.getAction() == InventoryAction.PICKUP_SOME)
				event.setCursor(mapItem);
			else if (event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD || event.getAction() == InventoryAction.HOTBAR_SWAP)
				event.getWhoClicked().getInventory().setItem(event.getHotbarButton(), mapItem);
			else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY)
				event.getWhoClicked().getInventory().setItem(MapUtils.getEmptySlot(event.getWhoClicked().getInventory()), mapItem);
			return true;
		}
		return false;
	}
	
	// TODO refactor to avoid code repeating
	public boolean onDrawingCartography(InventoryClickEvent event, DrawingMap map) {
		CartographyInventory inv = (CartographyInventory)event.getView().getTopInventory();
		ItemStack item0 = inv.getItem(0);
		ItemStack item1 = inv.getItem(1);
		
		int id = map.getId();

		// prepare craft: if 0 or 1 changed
		Runnable preAction = new Runnable() {
			@Override
			public void run() {
				ItemStack preMap = MapUtils.getMap(id, false);
				if (inv.contains(Material.PAPER)) {
					preMap = null;
				} else if (inv.contains(Material.GLASS_PANE)) {
					ItemMeta preMapMeta = preMap.getItemMeta();
					preMapMeta.setLore(Arrays.asList(new String[] {"", ChatColor.GRAY+"Finished"}));
					preMap.setItemMeta(preMapMeta);
				} else {
					preMap = null;
				}
				inv.setItem(2, preMap);
				Runnable updateAction = new Runnable() { @Override
					public void run() {
						for (HumanEntity human : inv.getViewers())
							((Player)human).updateInventory();
					} };
				DelayedTask updateTask = new DelayedTask(1, updateAction);
				TaskList.add(updateTask);
			} };
		DelayedTask preTask = new DelayedTask(1, preAction);
		TaskList.add(preTask);
		
		//craft: if 2 moved/dropped
		if (inv == event.getClickedInventory() && event.getSlot() == 2 &&
				(event.getAction() == InventoryAction.DROP_ALL_SLOT || event.getAction() == InventoryAction.DROP_ONE_SLOT
				|| event.getAction() == InventoryAction.PICKUP_ALL || event.getAction() == InventoryAction.PICKUP_HALF
				|| event.getAction() == InventoryAction.PICKUP_ONE || event.getAction() == InventoryAction.PICKUP_SOME
				|| (event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD || event.getAction() == InventoryAction.HOTBAR_SWAP)
					&& event.getView().getBottomInventory().getItem(event.getHotbarButton()) == null
				|| event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
					&& MapUtils.getEmptySlot(event.getWhoClicked().getInventory()) >= 0) )
		{
			event.setCancelled(true);
			ItemStack mapItem = inv.getItem(2);
			if (inv.contains(Material.PAPER)) { // extending
				mapItem = null;
				Logger.warning("PlAyEr HaDnT rEciEvE An InVeNtOrY UpDaTe ?");
				return false;
			} else if (inv.contains(Material.GLASS_PANE)) { // locking
				MapView view = MapUtils.getView(map);
				NmsWorldMapHelper.copyPixels(map, view);
				view.setCenterX(0);
				view.setCenterZ(0);
				view.setScale(Scale.CLOSEST);
				view.setLocked(true);
				MapFileManager.delete(map);
				for (int i = view.getRenderers().size() - 1; i >= 0; i--) {
					MapRenderer renderer = view.getRenderers().get(i);
					if (renderer instanceof DrawingRenderer) {
						DrawingRenderer drawingRend = (DrawingRenderer) renderer;
						view.removeRenderer(drawingRend);
						view.addRenderer(drawingRend.vanillaRenderer);
					}
				}
				mapItem = MapUtils.getMap(view.getId());
			} else {
				return false;
			}

			item0.setAmount(item0.getAmount() - 1);
			item1.setAmount(item1.getAmount() - 1);
			// TODO: try to stack
			if (event.getAction() == InventoryAction.DROP_ALL_SLOT || event.getAction() == InventoryAction.DROP_ONE_SLOT)
				Utils.drop(event.getWhoClicked().getEyeLocation(), mapItem, 1);
			else if (event.getAction() == InventoryAction.PICKUP_ALL || event.getAction() == InventoryAction.PICKUP_HALF
					|| event.getAction() == InventoryAction.PICKUP_ONE || event.getAction() == InventoryAction.PICKUP_SOME)
				event.setCursor(mapItem);
			else if (event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD || event.getAction() == InventoryAction.HOTBAR_SWAP)
				event.getWhoClicked().getInventory().setItem(event.getHotbarButton(), mapItem);
			else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY)
				event.getWhoClicked().getInventory().setItem(MapUtils.getEmptySlot(event.getWhoClicked().getInventory()), mapItem);
			return true;
		}
		return false;
	}

	public static ItemStack getScaleMap(int scale)
	{
		ItemStack result = new ItemStack(Material.MAP, 1);
		ItemMeta meta = result.getItemMeta();
		meta.setDisplayName("Map (" + scale + ":1)");
		result.setItemMeta(meta);
		result = NBTUtils.setInt(result, SmallMapUtils.SCALE_FIELD, scale);
		return result;
	}

	public static ItemStack getDrawingMap()
	{
		ItemStack result = new ItemStack(Material.MAP, 1);
		ItemMeta meta = result.getItemMeta();
		meta.setLore(Arrays.asList(new String[] { "Drawing" }));
		result.setItemMeta(meta);
		result = NBTUtils.setBoolean(result, DrawingMapUtils.IS_DRAWING_FIELD, true);
		return result;
	}
	
	public static void addCrafts(Main plugin)
	{
    	CraftManager cm = plugin.getCraftManager();
    	Server server = plugin.getServer();
    	
		ItemStack result_8 = getScaleMap(8);
		NamespacedKey key_8 = new NamespacedKey(plugin, "map_8");
    	ShapedRecipe map_8 = new ShapedRecipe(key_8, result_8);
    	
    	map_8.shape(new String[] {" P ", "PCP", " P "});
    	map_8.setIngredient('P', Material.PAPER);
    	map_8.setIngredient('C', Material.COMPASS);
    	
    	cm.addCraftbookRecipe(key_8);
    	server.addRecipe(map_8);
    	

		ItemStack result_draw = getDrawingMap();
		NamespacedKey key_draw = new NamespacedKey(plugin, "map_draw");
    	ShapedRecipe map_draw = new ShapedRecipe(key_draw, result_draw);
    	
    	RecipeChoice dyeChoice = new RecipeChoice.MaterialChoice(Material.BLACK_DYE, Material.BLUE_DYE, Material.BROWN_DYE, Material.CYAN_DYE,
    			Material.GRAY_DYE, Material.GREEN_DYE, Material.LIGHT_BLUE_DYE, Material.LIGHT_GRAY_DYE, Material.LIME_DYE, Material.MAGENTA_DYE,
    			Material.ORANGE_DYE, Material.PINK_DYE, Material.PURPLE_DYE, Material.RED_DYE, Material.WHITE_DYE, Material.YELLOW_DYE);
    	map_draw.shape(new String[] {" P ", "DBD", "DDD"});
    	map_draw.setIngredient('P', Material.PAPER);
    	map_draw.setIngredient('B', Material.BOWL);
    	map_draw.setIngredient('D', dyeChoice);
    	
    	cm.addCraftbookRecipe(key_draw);
    	server.addRecipe(map_draw);
	}
}
