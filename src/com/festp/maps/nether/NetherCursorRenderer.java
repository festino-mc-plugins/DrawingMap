package com.festp.maps.nether;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MapView.Scale;
import org.bukkit.map.MapCursor.Type;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.util.Vector;

import com.festp.maps.DrawingMapCoordinator;
import com.festp.maps.MapFileManager;
import com.festp.maps.MapUtils;
import com.festp.maps.PlaneRotation3D;
import com.festp.maps.small.SmallMap;
import com.festp.maps.small.SmallMapUtils;
import com.festp.utils.Vector3i;

public class NetherCursorRenderer extends MapRenderer {
	
	private final Map<String, NetherCursor> netherCursors = new HashMap<>();
	private final MapInfo mapInfo;
	private final MapRenderer prevRenderer;
	public MapRenderer getPrevRenderer() {
		return prevRenderer;
	}
	
	private NetherCursorRenderer(MapInfo info, MapRenderer prevRenderer) {
		this.mapInfo = info;
		this.prevRenderer = prevRenderer;
	}

	public static void add(MapView view) {
		MapInfo info;
		if (SmallMapUtils.isSmallMap(view.getId())) {
			SmallMap map = (SmallMap)MapFileManager.load(view.getId());
			info = new MapInfo(map, view.getWorld());
		} else {
			info = new MapInfo(view);
		}
		MapRenderer oldRenderer = null;
		if (info.isVanilla) {
			oldRenderer = view.getRenderers().get(0);
			view.removeRenderer(oldRenderer);
		}
		view.addRenderer(new NetherCursorRenderer(info, oldRenderer));
	}

	@Override
	public void render(MapView view, MapCanvas canvas, Player player) {
        for (NetherCursor cursor : this.netherCursors.values()) {
        	cursor.removeFrom(canvas);
        }
        
		// TODO move the code to scheduler
		// and check all players (not only holding the map in hand)
		updateCursors(player);
		
		if (mapInfo.isVanilla) {
			renderVanilla(view, canvas, player);
		}

        for (NetherCursor cursor : this.netherCursors.values()) {
        	if (player != null && !player.canSee(cursor.getPlayer()))
				continue;
			if (!view.isUnlimitedTracking() && cursor.isSmall())
				continue;
        	cursor.drawOn(canvas);
        }
	}
	
	private void renderVanilla(MapView view, MapCanvas canvas, Player player) {
		prevRenderer.render(view, canvas, player);
		MapCursorCollection cursors = canvas.getCursors();
		for (int i = cursors.size() - 1; i >= 0; i--) {
			MapCursor cursor = cursors.getCursor(i);
			if (!isOverworldPlayerCursor(cursor.getType()))
				continue;
			
			cursors.removeCursor(cursor);
		}
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (player != null && !player.canSee(p))
				continue;
			MapCursor cursor = getOverworldCursor(mapInfo, p.getLocation());
			if (!view.isUnlimitedTracking() && cursor.getType() == Type.SMALL_WHITE_CIRCLE)
				continue;
			if (!MapUtils.hasMap(p, view.getId()))
				continue;
			cursors.addCursor(cursor);
		}
	}
	private static boolean isOverworldPlayerCursor(Type cursorType) {
		return cursorType == Type.WHITE_POINTER
				|| cursorType == Type.WHITE_CIRCLE
				|| cursorType == Type.SMALL_WHITE_CIRCLE;
	}

	private void updateCursors(Player player) {
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
		MapCursor cursor = getOverworldCursor(mapInfo, playerInfo.playerLoc.multiply(8));
		boolean useCursor = false;
		if (cursor.getType() == Type.WHITE_POINTER || useCursor) {
			cursor.setType(Type.RED_POINTER);
			netherCursors.put(playerInfo.playerName, new NetherIconCursor(playerInfo.player, cursor));
		} else {
			boolean isSmall = cursor.getType() == Type.SMALL_WHITE_CIRCLE;
			netherCursors.put(playerInfo.playerName, new NetherPixelCursor(playerInfo.player, cursor.getX(), cursor.getY(), isSmall));
		}
	}
	private static MapCursor getOverworldCursor(MapInfo mapInfo, Location loc) {
		final int halfWidth = mapInfo.width / 2;
		Vector cursorPlayer = mapInfo.coords.getMapCoord(
				new Vector(mapInfo.xCenter, mapInfo.yCenter, mapInfo.zCenter),
				loc.toVector());
		double x = cursorPlayer.getX();
		double y = cursorPlayer.getY();
		double mapX = Math.round(x * 2 * mapInfo.scale);
		double mapY = Math.round(y * 2 * mapInfo.scale);
		x = mapX * 0.5 / mapInfo.scale;
		y = mapY * 0.5 / mapInfo.scale;
		if (-halfWidth <= x && x < halfWidth && -halfWidth <= y && y < halfWidth) {
			MapCursor cursor = mapInfo.coords.getCursor3D((byte) mapX, (byte) mapY, loc, true);
			cursor.setType(Type.WHITE_POINTER);
			return cursor;
		}
		else {
			final int maxDistance = halfWidth + 2 * mapInfo.width;
			final boolean isNear = -maxDistance <= x && x < maxDistance && -maxDistance <= y && y < maxDistance;
			mapX = clamp(mapX, -128, 127);
			mapY = clamp(mapY, -128, 127);
			MapCursor cursor = new MapCursor((byte) mapX, (byte) mapY, (byte) 0, Type.WHITE_CIRCLE, true);
			if (!isNear)
				cursor.setType(Type.SMALL_WHITE_CIRCLE);
			return cursor;
		}
	}

	private static double clamp(double x, double a, double b) {
		return Math.max(a, Math.min(b, x));
	}

	// TODO united args boilerplate
	private static class MapInfo
	{
		public final boolean isVanilla;
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
			isVanilla = false;
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
			isVanilla = true;
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

	private static class PlayerInfo {
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
