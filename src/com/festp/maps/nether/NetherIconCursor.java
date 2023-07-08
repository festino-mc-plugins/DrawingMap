package com.festp.maps.nether;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapCursor.Type;

public class NetherIconCursor implements NetherCursor {
	private final Player player;
	private final MapCursor cursor;
	
	public NetherIconCursor(Player player, MapCursor cursor) {
		this.player = player;
		this.cursor = cursor;
	}

	public Player getPlayer() {
		return player;
	}

	public void drawOn(MapCanvas canvas) {
		MapCursorCollection cursors = canvas.getCursors();
    	cursors.addCursor(cursor);
	}

	public void removeFrom(MapCanvas canvas) {
		MapCursorCollection cursors = canvas.getCursors();
		cursors.removeCursor(cursor);
	}

	public boolean isSmall() {
		return cursor.getType() == Type.SMALL_WHITE_CIRCLE;
	}

}
