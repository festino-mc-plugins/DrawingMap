package com.festp.maps;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MapView.Scale;
import org.bukkit.map.MapCursor.Type;
import org.bukkit.util.Vector;

import com.festp.utils.NBTUtils;
import com.festp.utils.Vector3i;

public class NetherCursorRenderer extends MapRenderer {
	
	final Map<String, MapCursor> netherCursors = new HashMap<>();
	final MapInfo mapInfo;
	
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
		
		MapCursorCollection cursors = canvas.getCursors();
		while (cursors.size() > 0) {
            cursors.removeCursor(cursors.getCursor(0));
        }
		
        for (MapCursor cursor : this.netherCursors.values()) {
        	cursors.addCursor(cursor);
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
		if (-halfWidth <= x && x < halfWidth && -halfWidth <= y && y < halfWidth) {
			x = Math.round(x * 2 * mapInfo.scale);
			y = Math.round(y * 2 * mapInfo.scale);
			MapCursor cursor = mapInfo.coords.getCursor3D((byte) x, (byte) y, playerInfo.playerLoc.multiply(8), true);
			cursor.setType(Type.RED_POINTER);
			//cursor.setCaption(player.getDisplayName());
			netherCursors.put(playerInfo.playerName, cursor);
		}
		else {
			final int maxDistance = halfWidth + 2 * mapInfo.width;
			double mapX = Math.round(x * 2 * mapInfo.scale);
			double mapY = Math.round(y * 2 * mapInfo.scale);
			mapX = clamp(mapX, -128, 127);
			mapY = clamp(mapY, -128, 127);
			MapCursor cursor = mapInfo.coords.getCursor3D((byte) mapX, (byte) mapY, playerInfo.playerLoc.multiply(8), true);
			cursor.setDirection((byte)0);
			//cursor.setCaption(player.getDisplayName());
			if (-maxDistance <= x && x < maxDistance && -maxDistance <= y && y < maxDistance)
				cursor.setType(Type.WHITE_CIRCLE);
			else
				cursor.setType(Type.SMALL_WHITE_CIRCLE);
			netherCursors.put(playerInfo.playerName, cursor);
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
			scale = 1 / blocksPerPixel;
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
}
