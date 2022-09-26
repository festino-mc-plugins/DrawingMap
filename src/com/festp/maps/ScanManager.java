package com.festp.maps;

import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ScanManager
{
	boolean isGlobalScanning = false;
	HashSet<Player> scanningPlayers = new HashSet<>();
	
	public void setGlobalScanning(boolean isEnabled)
	{
		if (isEnabled == isGlobalScanning)
			return;
		isGlobalScanning = isEnabled;
		// may be null the set
	}
	
	public void tick()
	{
		if (isGlobalScanning)
		{
			for (Player player : Bukkit.getOnlinePlayers())
			{
				applyScan(player);
			}
		}
		else
		{
			for (Player player : scanningPlayers)
			{
				applyScan(player);
			}
		}
	}

	private void applyScan(Player player)
	{
		applyScan(player.getInventory().getItemInMainHand());
		applyScan(player.getInventory().getItemInOffHand());
	}
	private void applyScan(ItemStack stack)
	{
		if (stack == null || stack.getType() != Material.FILLED_MAP)
			return;
		
		if (DrawingMapUtils.isDrawingMap(stack))
		{
			IMap map = MapFileManager.load(MapUtils.getMapId(stack));
			((DrawingMap)map).setDiscovered(true);
		}
	}

	public void add(Player player) {
		scanningPlayers.add(player);
	}

	public void remove(Player player) {
		scanningPlayers.remove(player);
		
	}
}
