package com.festp.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Block;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.IBlockData;

public class PaletteUtils {
	
	public static final int SHADES_COUNT = 4;
	
	private static final Class<?> craftBlockStateClass = NmsWorldMapHelper.getCraftbukkitClass("block.CraftBlockState");
	private static final Method getHandle = getMethodOrNull(craftBlockStateClass, "getHandle");
	private static final Method getPosition = getMethodOrNull(craftBlockStateClass, "getPosition");
	private static final Method getWorldHandle = getMethodOrNull(craftBlockStateClass, "getWorldHandle");
	
	private static final Field colorIdField = getColorIdField();
	private static Field getColorIdField() {
		Class<?> materialMapColorClass = NmsWorldMapHelper.getNmsClass_MaterialMapColor();
		Field[] intFields = getFieldsByClass(materialMapColorClass, int.class);
		Field[] colorFields = getFieldsByClass(materialMapColorClass, materialMapColorClass);
		int[] timesMin = new int[intFields.length];
		try {
			for (Field colorField : colorFields) {
				if (!Modifier.isStatic(colorField.getModifiers()))
					continue;
				colorField.setAccessible(true);
				Object color = colorField.get(null);

				// id is < 64, color is > 256 (the only exception is color = 0 while id = 0)
				int minInt = Integer.MAX_VALUE;
				int minIndex = -1;
				for (int i = 0; i < intFields.length; i++) {
					Field field = intFields[i];
					int v = (Integer)field.get(color);
					if (v >= 0 && v < minInt) {
						minInt = v;
						minIndex = i;
					}
				}
				timesMin[minIndex]++;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		int maxTimes = 0;
		int maxTimesIndex = -1;
		for (int i = 0; i < timesMin.length; i++) {
			if (maxTimes < timesMin[i]) {
				maxTimes = timesMin[i];
				maxTimesIndex = i;
			}
		}
		return intFields[maxTimesIndex];
	}
	private static Field[] getFieldsByClass(Class<?> declaringClass, Class<?> clazz) {
		List<Field> res = new ArrayList<>();
		for (Field field : declaringClass.getDeclaredFields()) {
			//System.out.println(field.getName() + " class is " + field.getType());
			if (field.getType() == clazz) {
				res.add(field);
			}
		}
		return res.toArray(new Field[0]);
	}
	
	public static byte getColor(Block b)
	{
		if (b == null)
			return (byte) 0;
		
		
		Object craftBlockState = craftBlockStateClass.cast(b.getState());
		try {
			IBlockData nmsBlockData = (IBlockData)getHandle.invoke(craftBlockState);
			BlockPosition nmsPosition = (BlockPosition)getPosition.invoke(craftBlockState);
			IBlockAccess nmsBlockAccess = (IBlockAccess)getWorldHandle.invoke(craftBlockState);
			Object color = nmsBlockData.d(nmsBlockAccess, nmsPosition);

			int colorId = (Integer)colorIdField.get(color);;
			return (byte) (colorId * SHADES_COUNT + 1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	private static Method getMethodOrNull(Class<?> clazz, String methodName) {
		try {
			return clazz.getDeclaredMethod(methodName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
