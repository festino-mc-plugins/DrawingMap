package com.festp.maps.drawing;

import org.bukkit.Location;

import com.festp.maps.PlaneRotation3D;
import com.festp.utils.Vector3i;

public class DrawingInfo {
	public static final int MAX_WIDTH = 128;
	
	public final int scale;
	public final int xCenter, yCenter, zCenter;
	public final PlaneRotation3D state;
	public boolean isFullDiscovered;
	public boolean[][] discovered;

	public DrawingInfo(int scale, Vector3i blockCenter, PlaneRotation3D state) {
		this(scale, blockCenter.getX(), blockCenter.getY(), blockCenter.getZ(), state);
	}
	
	public DrawingInfo(Integer scale, Integer xCenter, Integer yCenter, Integer zCenter, PlaneRotation3D state) {
		this.scale = scale;
		this.xCenter = xCenter;
		this.yCenter = yCenter;
		this.zCenter = zCenter;
		this.state = state;

		this.isFullDiscovered = false;
		int width = getWidth();
		this.discovered = new boolean[width][width];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < width; y++) {
				this.discovered[x][y] = false;
			}
		}
	}
	
	public int getWidth() {
		return MAX_WIDTH / scale;
	}
	
	/**@param loc is point closest to the map center 
	 * @param scale is the number of pixels per block */
	public static DrawingInfo buildFrom(Location loc, int scale) {
		int xCenter = (int) Math.round(loc.getX()),
			yCenter = (int) Math.round(loc.getY()),
			zCenter = (int) Math.round(loc.getZ());
		PlaneRotation3D state = PlaneRotation3D.get(loc);
		if (state.isUp() || state.isVertical()) {
			yCenter += 1;
		}
		return new DrawingInfo(scale, xCenter, yCenter, zCenter, state);
	}
}
