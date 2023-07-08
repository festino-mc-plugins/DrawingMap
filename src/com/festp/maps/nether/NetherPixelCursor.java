package com.festp.maps.nether;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;

class NetherPixelCursor implements NetherCursor
{
	private final static byte O = 0; // transparent
	private final static byte B = 119; // black darkest
	private final static byte R1 = 16; // red dark
	private final static byte R2 = 17; // red normal
	private final static byte R3 = 18; // red brightest
	private final static byte[] SMALL_COLORS = {
			O, B,  B,  O,
			B, R3, R2, B,
			B, R3, R3, B,
			O, B,  B,  O,
	};
	private final static byte[] BIG_COLORS = {
			O, B,  B,  B,  B,  O,
			B, R1, R2, R2, R1, B,
			B, R2, R3, R3, R2, B,
			B, R2, R3, R3, R2, B,
			B, R1, R2, R2, R1, B,
			O, B,  B,  B,  B,  O,
	};
	
	private final static int SMALL_WIDTH = 4;
	private final static int BIG_WIDTH = 6;
	private final static int SMALL_OFFSET = 1;
	private final static int BIG_OFFSET = 2;
	
	final Player player;
	final boolean isSmall;
	final int offset;
	final int width;
	final int minX;
	final int minY;
	
	public NetherPixelCursor(Player player, int mapX, int mapY, boolean isSmall) {
		this.player = player;
		this.isSmall = isSmall;
		int width, offset;
		if (isSmall) {
			width = SMALL_WIDTH;
			offset = SMALL_OFFSET;
		} else {
			width = BIG_WIDTH;
			offset = BIG_OFFSET;
		}
		this.width = width;
		this.offset = offset;
		minX = getCursorMinCorner(mapX);
		minY = getCursorMinCorner(mapY);
	}
	
	private int getCursorMinCorner(int c) {
		c = (128 + c) / 2;
		int min = 0 - offset;
		int max = 128 + offset - width;
		return Math.max(min, Math.min(max, c - width / 2));
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public void drawOn(MapCanvas canvas) {
		final byte[] colors;
		if (isSmall)
			colors = SMALL_COLORS;
		else
			colors = BIG_COLORS;
		
		for (int dy = 0; dy < width; dy++) {
			for (int dx = 0; dx < width; dx++) {
				byte color = colors[width * dy + dx];
				if (color == O)
					continue;
				
				int x = minX + dx;
				int y = minY + dy;
				if (!isInside(x, y))
					continue;
				canvas.setPixel(x, y, color);
			}
		}
	}
	
	public void removeFrom(MapCanvas canvas) {
		for (int y = minY; y < minY + width; y++) {
			for (int x = minX; x < minX + width; x++) {
				if (!isInside(x, y))
					continue;
				canvas.setPixel(x, y, canvas.getBasePixel(x, y));
			}
		}
	}
	
	private boolean isInside(int x, int y) {
		return 0 <= x && x < 128 && 0 <= y && y < 128;
	}
}
