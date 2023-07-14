package com.festp.maps;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;

import com.festp.Logger;
import com.festp.Main;
import com.festp.maps.drawing.DrawingInfo;
import com.festp.maps.drawing.DrawingMap;
import com.festp.maps.small.SmallMap;
import com.festp.utils.PaletteUtils;

public class MapFileManager {
	private static final String DIR = Main.getPath() + Main.mapsdir + System.getProperty("file.separator");
	private static final String IMG_OLD_FORMAT = "bmp";
	private static final String IMG_FORMAT = "png";
	private static final String BOOLEAN_ARRAY_FORMAT = "bitset";
	private static final Color DEFAULT_SAVE_COLOR = new Color(255, 255, 127, 0); //Color.MAGENTA;
	
	private static List<IMap> maps = new ArrayList<>(); 
	
	/** on new map craft */
	public static void addMap(IMap map)
	{
		save(map);
		maps.add(map);
	}
	
	/** on map loading */
	public static IMap load(int id)
	{
		for (IMap map : maps)
			if (map.getId() == id)
				return map;
		
		IMap map = null;
		File file = new File(DIR + id + ".dat");
		if (file.exists())
		{
			try {
				FileConfiguration ymlFormat = YamlConfiguration.loadConfiguration(file);
				Object objSmall = ymlFormat.get("start_x");
				if (objSmall != null) {
					Integer start_x = (Integer) objSmall;
					Integer start_z = (Integer) ymlFormat.get("start_z");
					Integer scale = (Integer) ymlFormat.get("scale");
					map = new SmallMap(id, scale, start_x, start_z);
				}
				Object objDrawing = ymlFormat.get("position");
				if (objDrawing != null) {
					Integer scale = (Integer) ymlFormat.get("scale");
					Integer xCenter = (Integer) ymlFormat.get("x_center");
					Integer yCenter = (Integer) ymlFormat.get("y_center");
					Integer zCenter = (Integer) ymlFormat.get("z_center");
					PlaneRotation3D state = PlaneRotation3D.valueOf((String) objDrawing);
					DrawingInfo info = new DrawingInfo(scale, xCenter, yCenter, zCenter, state);
					info.isFullDiscovered = ymlFormat.getBoolean("is_discovered");
					if (!info.isFullDiscovered) {
						File discoveredFile = getDiscoveredFile(id);
						if (discoveredFile.exists()) {
							int width = info.getWidth();
							byte[] discoveredBits = new byte[width * width / 8];
							FileInputStream reader = new FileInputStream(discoveredFile);
							reader.read(discoveredBits);
							reader.close();
							boolean[][] discovered = info.discovered;
							for (int x = 0; x < width; x++) {
								for (int y = 0; y < width; y += 8) {
									byte info8 = discoveredBits[(x * width + y) / 8];
									for (int b = 0; b < 8; b++) {
										boolean data = (info8 & 0x1 << b) > 0;
										discovered[x][y + b] = data;
									}
								}
							}
						}
						// else info already created empty array
					}
					map = new DrawingMap(id, info);
				}
			} catch (Exception e) {
				Logger.severe("[WARN] Couldn't load map #" + id + "! (" + e.getClass().getSimpleName() + " : " + e.getMessage() + ")");
				Logger.printStackTracePeak(e, 3);
			}
		}
		
		if (map != null) {
			maps.add(map);
		}
		return map;
	}

	public static boolean isLoaded(int id) {
		for (IMap map : maps)
			if (map.getId() == id)
				return true;
		return false;
	}
	
	public static void save(IMap map)
	{
		File file = new File(DIR, map.getId() + ".dat");
		try {
			if (map instanceof SmallMap) {
				SmallMap smallMap = (SmallMap) map;
				FileConfiguration ymlFormat = YamlConfiguration.loadConfiguration(file);
				ymlFormat.set("scale", smallMap.getScale());
				ymlFormat.set("start_x", smallMap.getX());
				ymlFormat.set("start_z", smallMap.getZ());
				ymlFormat.save(file);
			}
			if (map instanceof DrawingMap) {
				DrawingMap drawingMap = (DrawingMap) map;
				FileConfiguration ymlFormat = YamlConfiguration.loadConfiguration(file);
				ymlFormat.set("scale", drawingMap.getScale());
				ymlFormat.set("x_center", drawingMap.getX());
				ymlFormat.set("y_center", drawingMap.getY());
				ymlFormat.set("z_center", drawingMap.getZ());
				ymlFormat.set("position", drawingMap.getDirection().name());
				ymlFormat.set("is_discovered", drawingMap.isFullDiscovered());
				ymlFormat.save(file);

				saveDiscovered(drawingMap);
			}
		} catch (IOException e) {
			Logger.severe("Error while creating map file '" + map.getId() + ".dat'.");
			e.printStackTrace();
		}
	}
	
	/** @return <b>false</b> if there was error */
	public static boolean saveDiscovered(DrawingMap map) {
		if (map.isFullDiscovered()) {
			File discoveredFile = getDiscoveredFile(map.getId());
			if (discoveredFile.exists()) {
				discoveredFile.delete();
			}
			return true;
		} else {
			return saveBitset(getDiscoveredFile(map.getId()), map.getDiscovered());
		}
	}

	public static void delete(IMap map) {
		File file = new File(DIR, map.getId() + ".dat");
		try {
			file.delete();
			if (map instanceof DrawingMap) {
				File discoveredFile = getDiscoveredFile(map.getId());
				if (discoveredFile.exists()) {
					discoveredFile.delete();
				}
				File imageFile = getImageFile(map.getId());
				if (imageFile.exists()) {
					imageFile.delete();
				}
			}
			maps.remove(map);
		} catch (Exception e) {
			Logger.severe("Error while deleting map file '" + map.getId() + ".dat'.");
			e.printStackTrace();
		}
	}
	
	private static File getDiscoveredFile(int id) {
		return new File(DIR + id + "." + BOOLEAN_ARRAY_FORMAT);
	}
	
	private static boolean saveBitset(File file, boolean[][] bitMap) {
		try {
			int width = bitMap.length;
			int height = bitMap[0].length;
			byte discoveredBits[] = new byte[width / 8 * height];
			int i = 0;
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y += 8) {
					byte info8 = 0;
					for (int b = 0; b < 8; b++) {
						int data = bitMap[x][y + b] ? 1 : 0;
						info8 |= data << b;
					}
					discoveredBits[i] = info8;
					i++;
				}
			}
			FileOutputStream writer = new FileOutputStream(file);
			writer.write(discoveredBits);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private static File getImageFile(int id) {
		File imageFile = new File(DIR + id + "." + IMG_OLD_FORMAT);
		if (!imageFile.exists())
			imageFile = new File(DIR + id + "." + IMG_FORMAT);
		return imageFile;
	}
	
	public static BufferedImage loadImage(int id)
	{
		File imageFile = getImageFile(id);
		
		if (!imageFile.exists())
			return null;
		
		try {
			BufferedImage image = ImageIO.read(imageFile);
			if (imageFile.getName().endsWith(IMG_OLD_FORMAT)) {
				imageFile.delete();
				saveImage(id, image);
			}
			return image;
		} catch (IOException e) {
			Logger.severe("Error while loading image '" + imageFile.getName() +"'.");
			return null;
		}
	}
	
	public static void saveImage(int id, BufferedImage image)
	{
		File imageFile = new File(DIR + id + "." + IMG_FORMAT);
		
		try {
			ImageIO.write(image, IMG_FORMAT, imageFile);
		} catch (IOException e) {
			Logger.severe("Error while saving image '" + id + "." + IMG_FORMAT +"'.");
		}
	}
	
	@SuppressWarnings("deprecation")
	public static void saveMapCanvas(IMap map, MapCanvas canvas) {
		if (load(map.getId()) == null)
			return;
		
		BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB_PRE);
		
		int errorCount = 0;
		int lastError = 0;
		for (int x = 0; x < 128; x++) {
			for (int z = 0; z < 128; z++) {
				byte mapColor = canvas.getPixel(x, z);
				Color color = DEFAULT_SAVE_COLOR;
				try {
					color = MapPalette.getColor(mapColor);
				} catch (Exception e) {
					errorCount++;
					lastError = mapColor;
				}
				if (0 <= mapColor && mapColor < PaletteUtils.SHADES_COUNT) {
					// FFFF FFFF FF00 0000 - transparent magenta
					color = new Color(0, 0, 0, 0);
				}
				image.setRGB(x, z, color.getRGB());
			}
		}
		if (errorCount > 0)
		{
			Logger.severe(errorCount + " errors while saving map #" + map.getId() + ": unknown color codes, last color id " + lastError);
		}
		
		MapFileManager.saveImage(map.getId(), image);
	}
}
