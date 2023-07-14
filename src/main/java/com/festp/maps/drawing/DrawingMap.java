package com.festp.maps.drawing;

import org.bukkit.World;
import org.bukkit.map.MapView;
import org.bukkit.map.MapView.Scale;

import com.festp.maps.IMap;
import com.festp.maps.MapFileManager;
import com.festp.maps.MapUtils;
import com.festp.maps.PlaneRotation3D;
import com.festp.utils.Vector3i;

public class DrawingMap implements IMap {
	
	private int id;
	private DrawingInfo info;
	private boolean needReset = false;
	
	public DrawingMap(int id, int scale, int xCenter, int yCenter, int zCenter, PlaneRotation3D pos)
	{
		this.id = id;
		this.info = new DrawingInfo(scale, new Vector3i(xCenter, yCenter, zCenter), pos);
		MapView mapview = MapUtils.getView(this);
		mapview.setScale(getDrawingMapScale());
	}
	
	public DrawingMap(int id, DrawingInfo info)
	{
		this.id = id;
		this.info = info;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " #" + getId() + " {x=" + getX() + ", y=" + getY() + ", z=" + getZ()
				+ ", scale=" + getScale() + ", rot=" + getDirection() + "}"; 
	}
	
	public void setInfo(DrawingInfo info, World world) {
		this.info = info;
		MapView mapview = MapUtils.getView(this);
		mapview.setWorld(world);
		mapview.setScale(getDrawingMapScale());
		needReset = true;
		MapFileManager.save(this);
	}
	
	public boolean tryReset() {
		boolean res = needReset; 
		needReset = false;
		return res;
	}
	
	public int getId() {
		return id;
	}
	
	public int getScale() {
		return info.scale;
	}
	
	public int getX() {
		return info.xCenter;
	}
	
	public int getY() {
		return info.yCenter;
	}
	
	public int getZ() {
		return info.zCenter;
	}
	
	public PlaneRotation3D getDirection() {
		return info.state;
	}
	
	public int getWidth() {
		return info.getWidth();
	}
	
	public boolean isFullDiscovered() {
		return info.isFullDiscovered;
	}
	
	public boolean[][] getDiscovered() {
		return info.discovered;
	}
	
	public void checkDiscovering() {
		boolean isFullDiscovered = true;
		int width = getWidth();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < width; y++) {
				if (!info.discovered[x][y]) {
					isFullDiscovered = false;
					break;
				}
			}
		}
		if (isFullDiscovered) {
			setDiscovered(true);
		}
	}

	public void setDiscovered(boolean isDiscovered) {
		if (isDiscovered)
		{
			info.discovered = null;
			info.isFullDiscovered = true;
		}
		else
		{
			info = new DrawingInfo(info.scale, info.xCenter, info.yCenter, info.zCenter, info.state);
		}
		MapFileManager.save(this);
	}
	
	private Scale getDrawingMapScale() {
		switch (getScale()) {
		case 8: return Scale.CLOSE;
		case 4: return Scale.NORMAL;
		case 2: return Scale.FAR;
		case 1: return Scale.FARTHEST;
		default: return Scale.CLOSEST;
		}
	}
}
