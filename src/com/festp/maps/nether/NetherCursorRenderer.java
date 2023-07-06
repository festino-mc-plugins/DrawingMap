package com.festp.maps.nether;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MapView.Scale;
import org.bukkit.map.MapCursor.Type;
import org.bukkit.util.Vector;

import com.festp.maps.DrawingMapCoordinator;
import com.festp.maps.MapFileManager;
import com.festp.maps.MapUtils;
import com.festp.maps.PlaneRotation3D;
import com.festp.maps.small.SmallMap;
import com.festp.maps.small.SmallMapUtils;
import com.festp.utils.Vector3i;

public class NetherCursorRenderer extends MapRenderer {
	
	final Map<String, MapCursor> netherCursors = new HashMap<>();
	final MapInfo mapInfo;
	
	List<PixelCursor> pixelCursors = new ArrayList<>();
	List<PixelCursor> nextPixelCursors = new ArrayList<>();
	
	public NetherCursorRenderer(MapView view) {
		MapInfo info;
		if (SmallMapUtils.isSmallMap(view.getId())) {
			SmallMap map = (SmallMap)MapFileManager.load(view.getId());
			info = new MapInfo(map, view.getWorld());
		} else {
			info = new MapInfo(view);
		}
		mapInfo = info;
	}

	@Override
	public void render(MapView view, MapCanvas canvas, Player player) {
		// TODO move code to scheduler
		for (String playerName : netherCursors.keySet()) {
			Player p = Bukkit.getPlayerExact(playerName);
			PlayerInfo playerInfo = new PlayerInfo(p, mapInfo);
			updateNetherCursor(mapInfo, playerInfo);
		}
		if (!netherCursors.containsKey(player.getName())) {
			// TODO remove overworld pointer
			PlayerInfo playerInfo = new PlayerInfo(player, mapInfo);
			updateNetherCursor(mapInfo, playerInfo);
		}
		
		// clear and draw
		MapCursorCollection cursors = canvas.getCursors();
		while (cursors.size() > 0) {
            cursors.removeCursor(cursors.getCursor(0));
        }
        for (MapCursor cursor : this.netherCursors.values()) {
        	cursors.addCursor(cursor);
        }

		for (PixelCursor pc : pixelCursors) {
			pc.removeFrom(canvas);
		}
		pixelCursors.clear();
		List<PixelCursor> temp = pixelCursors;
		pixelCursors = nextPixelCursors;
		nextPixelCursors = temp;
		for (PixelCursor pc : pixelCursors) {
			pc.drawOn(canvas);
		}
	}

	private void updateNetherCursor(MapInfo mapInfo, PlayerInfo playerInfo) {
		Player p = playerInfo.player;
		if (p == null || !p.isOnline() || p.getWorld().getEnvironment() != Environment.NETHER) {
			netherCursors.remove(playerInfo.playerName);
			return;
		}
		
		boolean hasMap = MapUtils.hasMap(p, mapInfo.id);
		if (!hasMap) {
			netherCursors.remove(playerInfo.playerName);
			return;
		}
		
		putNetherCursor(mapInfo, playerInfo);
	}
	private void putNetherCursor(MapInfo mapInfo, PlayerInfo playerInfo) {
		final int halfWidth = mapInfo.width / 2;
		Vector cursorPlayer = mapInfo.coords.getMapCoord(
				new Vector(mapInfo.xCenter, mapInfo.yCenter, mapInfo.zCenter),
				playerInfo.playerLoc.toVector().multiply(8));
		double x = cursorPlayer.getX();
		double y = cursorPlayer.getY();
		double mapX = Math.round(x * 2 * mapInfo.scale);
		double mapY = Math.round(y * 2 * mapInfo.scale);
		x = mapX * 0.5 / mapInfo.scale;
		y = mapY * 0.5 / mapInfo.scale;
		if (-halfWidth <= x && x < halfWidth && -halfWidth <= y && y < halfWidth) {
			MapCursor cursor = mapInfo.coords.getCursor3D((byte) mapX, (byte) mapY, playerInfo.playerLoc.multiply(8), true);
			cursor.setType(Type.RED_POINTER);
			//cursor.setCaption(player.getDisplayName());
			netherCursors.put(playerInfo.playerName, cursor);
		}
		else {
			final int maxDistance = halfWidth + 2 * mapInfo.width;
			final boolean isNear = -maxDistance <= x && x < maxDistance && -maxDistance <= y && y < maxDistance;
			mapX = clamp(mapX, -128, 127);
			mapY = clamp(mapY, -128, 127);
			drawCircleCursor(playerInfo.playerName, (byte) mapX, (byte) mapY, !isNear);
		}
	}
	private void drawCircleCursor(String playerName, byte mapX, byte mapY, boolean isSmall) {
		boolean useCursor = false;
		if (useCursor) {
			MapCursor cursor = new MapCursor(mapX, mapY, (byte)0, Type.WHITE_CIRCLE, true);
			//cursor.setCaption(player.getDisplayName());
			if (isSmall)
				cursor.setType(Type.SMALL_WHITE_CIRCLE);
			netherCursors.put(playerName, cursor);
		}
		else {
			netherCursors.remove(playerName);
			nextPixelCursors.add(new PixelCursor(mapX, mapY, isSmall));
		}
		
	}

	private double clamp(double x, double a, double b) {
		return Math.max(a, Math.min(b, x));
	}

	// TODO united args boilerplate
	private class MapInfo
	{
		public final int id;
		public final World world;
		public final int xCenter;
		public final int yCenter;
		public final int zCenter;
		public final Vector3i center;
		public final double scale;
		public final int width;
		public final DrawingMapCoordinator coords;

		
		public MapInfo(SmallMap map, World w)
		{
			id = map.getId();
			world = w;
			scale = map.getScale();
			width = map.getWidth();
			xCenter = map.getX() + width / 2;
			zCenter = map.getZ() + width / 2;
			
			yCenter = 64;
			center = new Vector3i(xCenter, yCenter, zCenter);
			coords = new DrawingMapCoordinator(PlaneRotation3D.DOWN_NORTH, width);
		}
		
		public MapInfo(MapView view)
		{
			id = view.getId();
			world = view.getWorld();
			int blocksPerPixel = getBlocksPerPixel(view.getScale());
			scale = 1.0 / blocksPerPixel;
			width = 128 * blocksPerPixel;
			xCenter = view.getCenterX();
			zCenter = view.getCenterZ();
			
			yCenter = 64;
			center = new Vector3i(xCenter, yCenter, zCenter);
			coords = new DrawingMapCoordinator(PlaneRotation3D.DOWN_NORTH, width);
		}

		private int getBlocksPerPixel(Scale scale) {
			switch (scale) {
			case CLOSEST:
				return 1;
			case CLOSE:
				return 2;
			case NORMAL:
				return 4;
			case FAR:
				return 8;
			case FARTHEST:
				return 16;
			default:
				return 0;
			}
		}
	}

	private class PlayerInfo {
		public final Player player;
		public final String playerName;
		public final Location playerLoc;
		public final int playerX;
		public final int playerY;
		public final int playerZ;
		public final Vector3i mapPlayer;
		
		public PlayerInfo(Player player, MapInfo mapInfo) {
			this.player = player;
			playerName = player.getName();
			playerLoc = player.getLocation();
			playerX = playerLoc.getBlockX();
			playerY = playerLoc.getBlockY();
			playerZ = playerLoc.getBlockZ();


			mapPlayer = mapInfo.coords.getMapCoord(mapInfo.center, new Vector3i(playerX, playerY, playerZ));
			final int halfWidth = mapInfo.width / 2;
			mapPlayer.add(new Vector3i(halfWidth, halfWidth, 0));
		}
	}
	
	private class PixelCursor {
		private final byte[] SMALL_COLORS = {
				0, 119, 119, 0,
				119, 18, 17, 119,
				119, 18, 18, 119,
				0, 119, 119, 0,
		};
		private final byte[] BIG_COLORS = {
				0, 119, 119, 119, 119, 0,
				119, 16, 17, 17, 16, 119,
				119, 17, 18, 18, 17, 119,
				119, 17, 18, 18, 17, 119,
				119, 16, 17, 17, 16, 119,
				0, 119, 119, 119, 119, 0,
		};
		final boolean isSmall;
		final int width;
		final int minX;
		final int minY;
		
		public PixelCursor(int mapX, int mapY, boolean isSmall) {
			this.isSmall = isSmall;
			int w = 6;
			if (isSmall)
				w = 4;
			width = w;
			minX = getCursorMinCorner(mapX);
			minY = getCursorMinCorner(mapY);
		}
		
		private int getCursorMinCorner(int c) {
			c = (128 + c) / 2;
			return Math.max(0, Math.min(128 - width, c - width / 2));
		}
		
		public void drawOn(MapCanvas canvas) {
			for (int dy = 0; dy < width; dy++) {
				for (int dx = 0; dx < width; dx++) {
					byte color;
					if (isSmall)
						color = SMALL_COLORS[width * dy + dx];
					else
						color = BIG_COLORS[width * dy + dx];
					if (color == 0)
						continue;
					
					int x = minX + dx;
					int y = minY + dy;
					canvas.setPixel(x, y, color);
				}
			}
		}
		
		public void removeFrom(MapCanvas canvas) {
			for (int y = minY; y < minY + width; y++) {
				for (int x = minX; x < minX + width; x++) {
					canvas.setPixel(x, y, canvas.getBasePixel(x, y));
				}
			}
		}
	}
}
