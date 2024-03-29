package com.festp.maps;

import java.awt.image.BufferedImage;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import com.festp.DelayedTask;
import com.festp.TaskList;

public abstract class CustomRenderer extends MapRenderer {

	static final int SAVE_TICKS = 20 * 10;
	private int save_ticks = 0;
	
	boolean init = false;
	final IMap map;
	DelayedTask saveTask = null;
	
	public CustomRenderer(IMap map) {
		this.map = map;
	}
	
	public final void renderImage(MapCanvas canvas, BufferedImage image) {
		canvas.drawImage(0, 0, image);
		for (int x = 0; x < 128; x++)
			for (int z = 0; z < 128; z++)
				if (image.getRGB(x, z) == 0)
					canvas.setPixel(x, z, (byte) 0);
	}
	
	protected abstract void renderSpecific(MapView view, MapCanvas canvas, Player player);

	@Override
	public final void render(MapView view, MapCanvas canvas, Player player)
	{
		if (!init) {
			BufferedImage image = MapFileManager.loadImage(view.getId());
			if (image != null)
			{
				renderImage(canvas,image);
			}
			init = true;
			return;
		}
		
		renderSpecific(view, canvas, player);

		save_ticks++;
		if (save_ticks >= SAVE_TICKS)
		{
			save_ticks = 0;
			MapFileManager.saveMapCanvas(map, canvas);
			
			if (saveTask != null) {
				saveTask.terminate();
			}
			saveTask = new DelayedTask(SAVE_TICKS * 2, new Runnable() {
				@Override public void run() {
					MapFileManager.saveMapCanvas(map, canvas);
				}
			});
			TaskList.add(saveTask);
		}
	}
}
