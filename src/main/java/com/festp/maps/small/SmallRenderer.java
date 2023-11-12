package com.festp.maps.small;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursor.Type;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import com.festp.maps.DrawingMapCoordinator;
import com.festp.maps.MapUtils;
import com.festp.utils.NmsWorldMapHelper;

public class SmallRenderer extends MapRenderer {
	
	static final int RENDER_DISTANCE_SQUARED = 128 * 128;
	
	final SmallMap map;
	final MapRenderer vanillaRenderer;
	boolean initialized = false;
	
	public SmallRenderer(SmallMap map, MapRenderer vanillaRenderer) {
		this.map = map;
		this.vanillaRenderer = vanillaRenderer;
	}

	public void render(MapView view, MapCanvas canvas, Player player)
	{
		int expectedX = SmallMapUtils.getVanillaCenter(map.getX());
		int expectedZ = SmallMapUtils.getVanillaCenter(map.getZ());
		int viewX = view.getCenterX();
		int viewZ = view.getCenterZ();
		if (viewX != expectedX || viewZ != expectedZ) {
			view.setCenterX(expectedX);
			view.setCenterZ(expectedZ);
		}
		
		updatePixels(view, canvas, player);
		updateCursors(canvas);
		
		if (viewX != expectedX || viewZ != expectedZ) {
			view.setCenterX(viewX);
			view.setCenterZ(viewZ);
		}
	}
	
	private boolean canRender(MapView view, Player player) {
		if (player == null) {
			return true;
		}

		if (player.getWorld() != view.getWorld()) {
			return false;
		}
		
		int width = map.getWidth();
		int minX = map.getX();
		int minZ = map.getZ();
		int realX = minX + width / 2,
			realZ = minZ + width / 2;
		int playerX = player.getLocation().getBlockX();
		int playerZ = player.getLocation().getBlockZ();
		long distX = playerX - realX,
			distZ = playerZ - realZ;
		if (distX * distX + distZ * distZ > RENDER_DISTANCE_SQUARED) {
			return false;
		}
		return true;
	}
	private void updatePixels(MapView view, MapCanvas canvas, Player player) {
		if (initialized && !canRender(view, player))
			return;
		
		initialized = true;

		vanillaRenderer.render(view, canvas, player);
		byte[] colors = Arrays.copyOf(NmsWorldMapHelper.getColors(canvas), 0x3FFF);
		int minX = map.getX();
		int minZ = map.getZ();
		int minMapX = minX - view.getCenterX() + 64;
		int minMapZ = minZ - view.getCenterZ() + 64;
		int scale = map.getScale();
		int width = map.getWidth();
		for (int z = 0; z < width; z++)
		{
			for (int x = 0; x < width; x++)
			{
				int mapX = minMapX + x,
					mapZ = minMapZ + z;
				byte color = colors[mapZ * 128 + mapX];
				
				int pxX = x * scale;
				int pxZ = z * scale;
				for (int dx = 0; dx < scale; dx++)
					for (int dz = 0; dz < scale; dz++)
						canvas.setPixel(pxX + dx, pxZ + dz, color);
			}
		}
	}
	
	private void updateCursors(MapCanvas canvas)
	{
        MapCursorCollection vanillaCursors = canvas.getCursors();
        MapCursorCollection cursors = new MapCursorCollection();
        canvas.setCursors(cursors);

        int mapScale = map.getScale();
        int startX = worldToCursorX(canvas.getMapView().getCenterX());
        int startZ = worldToCursorZ(canvas.getMapView().getCenterZ());
        for (int i = 0; i < vanillaCursors.size(); i++) {
        	MapCursor cursor = vanillaCursors.getCursor(i);
        	Type cursorType = cursor.getType();
        	if (isPlayerCursor(cursorType)) {
        		continue;
        	}

        	int vanillaX = cursor.getX();
        	int vanillaY = cursor.getY();
        	//System.out.println(canvas.getMapView().getId() + " " + cursor.getType() + " " + cursor.getX() + " " + cursor.getY());
    		// move banners / green markers / etc to the center of a block
    		if (vanillaX % 2 != 0) {
    			vanillaX++;
    		}
    		if (vanillaY % 2 == 0) {
    			vanillaY++;
    		}
    		
        	int x = vanillaX * mapScale + startX - mapScale;
        	int z = vanillaY * mapScale + startZ;
        	if (x < -128 || 127 < x || z < -128 || 127 < z) {
        		continue;
        	}
        	
        	cursor.setX((byte)x);
        	cursor.setY((byte)z);
        	cursors.addCursor(cursor);
        }
        
        // precise player positions
        World mapWorld = canvas.getMapView().getWorld();
        for (Player player : Bukkit.getOnlinePlayers()) {
        	if (!mapWorld.equals(player.getWorld()))
        		continue;
        	if (!isRenderedPlayer(player))
        		continue;
        	
        	int x = worldToCursorX(player.getLocation().getX());
        	int z = worldToCursorZ(player.getLocation().getZ());
        	MapCursor cursor = new MapCursor((byte)0, (byte)0, getDirection(player), Type.WHITE_POINTER, true);
        	if (x < -128 || 127 < x || z < -128 || 127 < z) {
    			final int halfWidth = 128;
    			final int maxDistance = 5 * halfWidth;
    			if (x <= -maxDistance || maxDistance < x || z <= -maxDistance || maxDistance < z)
    				cursor.setType(Type.SMALL_WHITE_CIRCLE);
    			else
    				cursor.setType(Type.WHITE_CIRCLE);
    			cursor.setDirection((byte)0);
    			x = clamp(x, -128, 127);
    			z = clamp(z, -128, 127);
        	}
        	cursor.setX((byte)x);
        	cursor.setY((byte)z);
        	cursors.addCursor(cursor);
        }
	}
	
	private int worldToCursorX(double worldX) {
		return (int)Math.round(2 * map.getScale() * (worldX - map.getX())) - 128;
	}
	private int worldToCursorZ(double worldZ) {
		return (int)Math.round(2 * map.getScale() * (worldZ - map.getZ())) - 128;
	}
	
	private int clamp(int x, int a, int b) {
		return Math.max(a, Math.min(b, x));
	}
	
	private byte getDirection(Player player) {
		float yaw = player.getLocation().getYaw();
		return DrawingMapCoordinator.yawToDirection(yaw);
	}
	
	private static boolean isPlayerCursor(Type cursorType) {
		return cursorType == Type.WHITE_POINTER
				|| cursorType == Type.WHITE_CIRCLE
				|| cursorType == Type.SMALL_WHITE_CIRCLE;
	}
	
	private boolean isRenderedPlayer(Player p) {
		return MapUtils.hasMap(p, map.getId());
	}
}
