package com.festp.maps.drawing;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.util.Vector;

import com.festp.maps.AbstractRenderer;
import com.festp.maps.DrawingMapCoordinator;
import com.festp.maps.MapFileManager;
import com.festp.maps.MapUtils;
import com.festp.maps.PaletteUtils;
import com.festp.maps.PlaneRotation3D;
import com.festp.utils.Vector3i;

public class DrawingRenderer extends AbstractRenderer {

	static final int RENDER_DISTANCE = 6;
	static final int RENDER_DISTANCE_SQUARED = RENDER_DISTANCE * RENDER_DISTANCE;
	static final int RAYS_DISTANCE_SQUARED = 100 * 100;
	private static final int GRID_ROWS = 4;
	private static final int GRID_SIZE = GRID_ROWS * GRID_ROWS;
	private static final int MAX_ROW_PIXELS = 128;
	private static final int MAX_PIXELS = MAX_ROW_PIXELS * MAX_ROW_PIXELS;
	private static final int RENDER_QUOTA = (MAX_PIXELS / 20) * 6 / 10; // 60% in 1s

	public final DrawingMap map;
	public final MapRenderer vanillaRenderer;
	private DrawingMapGrid grids = null;
	
	public DrawingRenderer(DrawingMap map, MapRenderer vanillaRenderer) {
		super(map);
		this.map = map;
		this.vanillaRenderer = vanillaRenderer;
	}

	@Override
	public void renderSpecific(MapView view, MapCanvas canvas, Player player) {
		if (map.needReset) {
			grids = null;
			byte initColor = PaletteUtils.getColor(null);
			for (int x = 0; x < 128; x++)
				for (int y = 0; y < 128; y++)
					canvas.setPixel(x, y, initColor);
			map.needReset = false;
		}
		if (grids == null) {
			grids = new DrawingMapGrid(GRID_SIZE, map.getWidth() / GRID_ROWS, System.currentTimeMillis());
		}
		
		// test if the player holds the map in hands
		Integer main_id = MapUtils.getMapId(player.getInventory().getItemInMainHand());
		if (main_id == null || main_id != view.getId())
		{
			Integer off_id = MapUtils.getMapId(player.getInventory().getItemInOffHand());
			if (off_id == null || off_id != view.getId())
				return;
		}

		DrawingMapRenderArgs args = new DrawingMapRenderArgs(map, player, view.getWorld());

		updateCursor(args, canvas, player.getDisplayName(), player.isSneaking());

		boolean canRender = canRenderByZDistance(args);
		if (canRender && !map.isFullDicovered()) {
			updateDiscovered(args);
		}

		DrawingMapPixelRenderer pixeler = new DrawingMapPixelRenderer(canvas, map, args.coords);
		int totalRendered = renderPlayerSurroundings(pixeler, args);
		grids.sort();
		renderMainSurroundings(pixeler, args, totalRendered);
	}

	private class DrawingMapRenderArgs
	{
		public final Location playerLoc;
		public final int playerX;
		public final int playerY;
		public final int playerZ;

		public final World world;
		public final int xCenter;
		public final int yCenter;
		public final int zCenter;
		public final Vector3i center;
		public final int scale;
		public final int width;
		public final PlaneRotation3D rot;
		public final DrawingMapCoordinator coords;
		
		public final Vector3i mapPlayer;
		public final int mapPlayerX;
		public final int mapPlayerY;
		
		public DrawingMapRenderArgs(DrawingMap map, Player player, World world)
		{
			playerLoc = player.getLocation();
			playerX = playerLoc.getBlockX();
			playerY = playerLoc.getBlockY();
			playerZ = playerLoc.getBlockZ();
			
			xCenter = map.getX();
			yCenter = map.getY();
			zCenter = map.getZ();
			center = new Vector3i(xCenter, yCenter, zCenter);
			scale = map.getScale();
			width = map.getWidth();
			rot = map.getDirection();
			this.world = world;
			coords = new DrawingMapCoordinator(rot, width);

			mapPlayer = coords.getMapCoord(center, new Vector3i(playerX, playerY, playerZ));
			final int halfWidth = width / 2;
			mapPlayer.add(new Vector3i(halfWidth, halfWidth, 0));
			mapPlayerX = mapPlayer.getX();
			mapPlayerY = mapPlayer.getY();
		}
	}

	private void renderMainSurroundings(DrawingMapPixelRenderer pixeler, DrawingMapRenderArgs args, int totalRendered)
	{
		final long time = System.currentTimeMillis();
		final DrawingMapCoordinator coords = args.coords;
		final int playerX = args.playerX;
		final int playerY = args.playerY;
		final int playerZ = args.playerZ;
		final int xCenter = args.xCenter;
		final int yCenter = args.yCenter;
		final int zCenter = args.zCenter;
		
		int minX = args.mapPlayerX - RAYS_DISTANCE_SQUARED;
		int maxX = args.mapPlayerX + RAYS_DISTANCE_SQUARED;
		int minY = args.mapPlayerY - RAYS_DISTANCE_SQUARED;
		int maxY = args.mapPlayerY + RAYS_DISTANCE_SQUARED;
		minX = Math.max(minX, 0) / GRID_ROWS;
		minY = Math.max(minY, 0) / GRID_ROWS;
		maxX = Math.min(maxX + 1, args.width) / GRID_ROWS;
		maxY = Math.min(maxY + 1, args.width) / GRID_ROWS;
		for (int i = 0; i < GRID_SIZE; i++) {
			int n = grids.get(i);
			int gridRendered = 0;
			for (int gridX = minX; gridX < maxX; gridX++) {
				for (int gridY = minY; gridY < maxY; gridY++) {
					int seed = n + gridX * gridX + 3 * gridY * gridY + 3 * gridX + gridY;
					int randomedX = GRID_ROWS * gridX + seed % GRID_ROWS;
					int randomedY = GRID_ROWS * gridY + seed / GRID_ROWS % GRID_ROWS;
					Vector3i offsets0 = coords.getWorldCoord(randomedX, randomedY, 0);
					int d1 = offsets0.getX(),
						d2 = offsets0.getY(),
						d3 = offsets0.getZ();
					int dist1 = playerX - (xCenter + d1),
						dist2 = playerY - (yCenter + d2),
						dist3 = playerZ - (zCenter + d3);
					int dist = dist1 * dist1 + dist2 * dist2 + dist3 * dist3;
					if (dist > RAYS_DISTANCE_SQUARED)
						continue;
					
					if (pixeler.tryRender(randomedX, randomedY)) {
						gridRendered++;
					}
				}
			}
			grids.updateTime(i, time, gridRendered);
			totalRendered += gridRendered;
			if (totalRendered >= RENDER_QUOTA) {
				break;
			}
		}
	}
	
	private int renderPlayerSurroundings(DrawingMapPixelRenderer pixeler, DrawingMapRenderArgs args)
	{
		int totalRendered = 0;
		int minX = args.mapPlayerX - RENDER_DISTANCE;
		int maxX = args.mapPlayerX + RENDER_DISTANCE;
		int minY = args.mapPlayerY - RENDER_DISTANCE;
		int maxY = args.mapPlayerY + RENDER_DISTANCE;
		minX = Math.max(minX, 0);
		minY = Math.max(minY, 0);
		maxX = Math.min(maxX, args.width - 1);
		maxY = Math.min(maxY, args.width - 1);
		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y < maxY; y++) {
				if (pixeler.tryRender(x, y)) {
					totalRendered++;
				}
			}
		}
		return totalRendered;
	}
	
	private boolean canRenderByZDistance(DrawingMapRenderArgs args)
	{
		boolean canRender = true;
		Vector3i renderDir = args.coords.getWorldCoord(0, 0, 1).subtract(args.coords.getWorldCoord(0, 0, 0));
		Vector3i block = new Vector3i(args.playerX, args.playerY, args.playerZ);
		Vector3i renderCoord = renderDir.getCoordwiseMult(renderDir); // (1, 0, 0), (0, 1, 0) or (0, 0, 1)
		Vector3i distProj = renderCoord.getCoordwiseMult(args.center.clone().subtract(block)); // 
		if (distProj.lengthSquared() != 0) {
			int dist = (int) distProj.length();
			distProj.normalize();
			Block b;
			for (int i = 0; i <= dist; i++) {
				block.add(distProj);
				b = args.world.getBlockAt(block.getX(), block.getY(), block.getZ());
				if (!canLookThrough(b)) {
					canRender = false;
					break;
				}
			}
		}
		return canRender;
	}
	
	private void updateCursor(DrawingMapRenderArgs args, MapCanvas canvas, String name, boolean renderCursor)
	{
		for (int i = 0; i < canvas.getCursors().size(); i++) {
			MapCursor cursor = canvas.getCursors().getCursor(i);
			if (cursor.getCaption() == name) {
				canvas.getCursors().removeCursor(cursor);
				break;
			}
		}
		if (renderCursor) {
			final int halfWidth = args.width / 2;
			Vector cursorPlayer = args.coords.getMapCoord(
					new Vector(args.xCenter, args.yCenter, args.zCenter),
					args.playerLoc.toVector());
			double x = cursorPlayer.getX();
			double y = cursorPlayer.getY();
			if (-halfWidth <= x && x < halfWidth && -halfWidth <= y && y < halfWidth) {
				x = Math.round(x * 2 * args.scale);
				y = Math.round(y * 2 * args.scale);
				MapCursor cursor = args.coords.getCursor3D((byte) x, (byte) y, args.playerLoc, false);
				cursor.setCaption(name);
				canvas.getCursors().addCursor(cursor);
			}
		}
	}
	
	private void updateDiscovered(DrawingMapRenderArgs args)
	{
		final DrawingMapCoordinator coords = args.coords;
		final int xCenter = args.xCenter;
		final int yCenter = args.yCenter;
		final int zCenter = args.zCenter;
		final int mapPlayerX = args.mapPlayerX;
		final int mapPlayerY = args.mapPlayerY;
		final World world = args.world;
		final boolean[][] discovered = map.getDicovered();
		final int width = args.coords.width;

		boolean updated = false;
		for (int index = 0; index < 4; index++) {
			for (int c = 0; c < width - 1; c++) {
				// map edge point
				int xOffset, yOffset;
				if (index == 0) {
					xOffset = c;
					yOffset = 0;
				} else if (index == 1) {
					xOffset = 0;
					yOffset = 1 + c;
				} else if (index == 2) {
					xOffset = c;
					yOffset = width - 1;
				} else {
					xOffset = width - 1;
					yOffset = 1 + c;
				}

				Vector p = new Vector(mapPlayerX, mapPlayerY, 0);
				Vector dir = new Vector(xOffset, yOffset, 0).subtract(p);
				p.add(new Vector(0.5, 0.5, 0));
				if (dir.length() == 0) {
					discovered[xOffset][yOffset] = true;
					continue;
				}
				dir.normalize();
				double dirX = dir.getX() == 0 ? 0 : dir.getX() > 0 ? 1 : -1;
				double dirY = dir.getY() == 0 ? 0 : dir.getY() > 0 ? 1 : -1;
				//System.out.print("FROM " + mapPlayer.getX() + "; " + mapPlayer.getY() + " TO " + xOffset + "; " + yOffset);
				while (true) {
					double dx = Math.floor(p.getX()) - p.getX();
					if (dirX >= 0) {
						dx = 1 + dx;
					}
					double dy = Math.floor(p.getY()) - p.getY();
					if (dirY >= 0) {
						dy = 1 + dy;
					}
					double tx = dx / dir.getX();
					double ty = dy / dir.getY();
					// fix vertexes
					if (tx == 0)
						tx = 1;
					if (ty == 0)
						ty = 1;
					
					double t = Math.min(tx, ty);
					p = p.add(dir.clone().multiply(t));
					
					int x = (int) p.getX();
					int y = (int) p.getY();
					if (x < 0 || width <= x || y < 0 || width <= y)
						break;
					if (discovered[x][y])
						continue;
					int mapDx = mapPlayerX - x;
					int mapDy = mapPlayerY - y;
					if (mapDx * mapDx + mapDy * mapDy > RAYS_DISTANCE_SQUARED)
						break;
					
					Vector3i offsets = coords.getWorldCoord(x, y, 0);
					int realX = xCenter + offsets.getX();
					int realY = yCenter + offsets.getY();
					int realZ = zCenter + offsets.getZ();
					Block b = world.getBlockAt(realX, realY, realZ);
					if (canLookThrough(b)) {
						discovered[x][y] = true;
						if (!updated) {
							updated = true;
						}
					} else {
						break;
					}
				}
			}
		}
		if (updated) {
			map.checkDiscovering();
			MapFileManager.saveDiscovered(map);
		}
	}
	
	@SuppressWarnings("deprecation")
	private static boolean canLookThrough(Block b) {
		return b.isPassable() || !b.getType().isOccluding() || b.getType().isTransparent();
	}
}
