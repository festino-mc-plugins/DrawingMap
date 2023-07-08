package com.festp.maps.nether;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;

public interface NetherCursor {
	public Player getPlayer();
	public void drawOn(MapCanvas canvas);
	public void removeFrom(MapCanvas canvas);
}
