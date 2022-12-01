package com.festp.maps;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapView;
import org.bukkit.util.Vector;

import com.festp.utils.Vector3i;

public class SmallRenderer extends AbstractRenderer {
	
	static final int RENDER_DISTANCE_SQUARED = 128 * 128;

	private BlockContainer lastColorBlock = new BlockContainer();
	
	final SmallMap map;
	
	public SmallRenderer(SmallMap map) {
		super(map);
		this.map = map;
	}

	@Override
	protected void renderSpecific(MapView view, MapCanvas canvas, Player player) {
		Integer mainId = MapUtils.getMapId(player.getInventory().getItemInMainHand());
		if (mainId == null || mainId != view.getId())
		{
			Integer offId = MapUtils.getMapId(player.getInventory().getItemInOffHand());
			if (offId == null || offId != view.getId())
				return;
		}
		//canvas.setCursors(null);new MapCursorCollection()
		int playerX = player.getLocation().getBlockX();
		int playerZ = player.getLocation().getBlockZ();
		
		int scale = map.getScale();
		int width = 128 / scale;
		int minX = map.getX();
		int minZ = map.getZ();
		for (int x = 0; x < width; x++)
		{
			// top block pseudorender
			PaletteUtils.getColor(view.getWorld(), minX + x, minZ - 1, lastColorBlock);
			for (int z = 0; z < width; z++)
			{
				int realX = minX + x,
					realZ = minZ + z;
				int distX = playerX - realX,
					distZ = playerZ - realZ;
				if (distX * distX + distZ * distZ > RENDER_DISTANCE_SQUARED + 1)
					continue;
				// brighter block, darker under block
				int lastY = lastColorBlock.getY();
				byte color = PaletteUtils.getColor(view.getWorld(), realX, realZ, lastColorBlock);
				
				if (distX * distX + distZ * distZ > RENDER_DISTANCE_SQUARED)
					continue;
				
				if (color == PaletteUtils.getColor(PaletteUtils.WATER))
				{
					// count water blocks
					Block b = lastColorBlock.get();
					int depth = 0;
					while (b.getType() == Material.WATER && b.getY() > 0) {
						depth++;
						b = b.getRelative(BlockFace.DOWN);
					}
					if (depth < 3)
						color += 1; // brighter
					else if (depth > 6)
						color -= 1; // darker
					//some ununderstandable specific
				}
				else
				{
					int y = lastColorBlock.getY();
					if (lastY < y)
						color += 1; // brighter
					else if (lastY > y)
						color -= 1; // darker
				}
				
				int pxX = x * scale;
				int pxZ = z * scale;
				for (int dx = 0; dx < scale; dx++)
					for (int dz = 0; dz < scale; dz++)
						canvas.setPixel(pxX + dx, pxZ + dz, color);
			}
		}

		SmallMapRenderArgs args = new SmallMapRenderArgs(map, player, view.getWorld());

		updateCursor(args, canvas, player.getDisplayName(), true);
	}

	// TODO united args boilerplate
	private class SmallMapRenderArgs
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
		public final DrawingMapCoordinator coords;
		
		public final Vector3i mapPlayer;
		public final int mapPlayerX;
		public final int mapPlayerY;
		
		public SmallMapRenderArgs(SmallMap map, Player player, World world)
		{
			playerLoc = player.getLocation();
			playerX = playerLoc.getBlockX();
			playerY = playerLoc.getBlockY();
			playerZ = playerLoc.getBlockZ();
			
			xCenter = map.getX();
			yCenter = playerY;
			zCenter = map.getZ();
			center = new Vector3i(xCenter, yCenter, zCenter);
			scale = map.getScale();
			width = map.getWidth();
			this.world = world;
			coords = new DrawingMapCoordinator(PlaneRotation3D.DOWN_NORTH, width);

			mapPlayer = coords.getMapCoord(center, new Vector3i(playerX, playerY, playerZ));
			final int halfWidth = width / 2;
			mapPlayer.add(new Vector3i(halfWidth, halfWidth, 0));
			mapPlayerX = mapPlayer.getX();
			mapPlayerY = mapPlayer.getY();
		}
	}


	private void updateCursor(SmallMapRenderArgs args, MapCanvas canvas, String name, boolean renderCursor)
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
				MapCursor cursor = args.coords.getCursor3D((byte) x, (byte) y, args.playerLoc);
				cursor.setCaption(name);
				canvas.getCursors().addCursor(cursor);
			}
		}
	}
}
