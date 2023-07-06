package com.festp.maps;

import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R1.block.CraftBlockState;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.MaterialMapColor;

public class PaletteUtils {
	
	public static final int SHADES_COUNT = 4;
	
	public static byte getColor(Block b)
	{
		if (b == null)
			return (byte) 0;
		
		CraftBlockState state = (CraftBlockState)b.getState();
		IBlockData nmsBlockData = state.getHandle();
		BlockPosition nmsPosition = state.getPosition();
		IBlockAccess nmsBlockAccess = state.getWorldHandle();
		MaterialMapColor color = nmsBlockData.d(nmsBlockAccess, nmsPosition);
		// find int fields and loop
		int minInt = Integer.MAX_VALUE;
		int v = color.ak;
		if (v < minInt)
			minInt = v;
		v = color.al;
		if (v < minInt)
			minInt = v;
		// id is < 64, color is > 256 (the only exception is color = 0 while id = 0)
		int colorId = minInt;
		return (byte) (colorId * SHADES_COUNT + 1);
	}
}
