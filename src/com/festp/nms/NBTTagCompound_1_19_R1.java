package com.festp.nms;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.item.ItemStack;

// TODO use reflection to support old versions NMS (set/get, etc)
public class NBTTagCompound_1_19_R1 implements INBTTagCompound
{
	NBTTagCompound compound;
	
	private NBTTagCompound_1_19_R1(Object nmsStack)
	{
		compound = getOrCreateTag(nmsStack);
	}
	private NBTTagCompound getOrCreateTag(Object nmsStack)
	{
		ItemStack nmsStackCast = (ItemStack)nmsStack;
		NBTTagCompound compound = nmsStackCast.v(); // u() = getTag(), v() = getOrCreateTag()
        
        /*if (compound == null) {
        	compound = new NBTTagCompound();
        	setTag(nmsStackCast, compound);
        	compound = getTag(nmsStack);
        }*/
		return compound;
	}
	
	public static INBTTagCompound getTag(Object nmsStack)
	{
		return new NBTTagCompound_1_19_R1(nmsStack);
	}

	public void setTag(Object nmsStack)
	{
		ItemStack nmsStackCast = (ItemStack)nmsStack;
		nmsStackCast.c(compound);
	}

	// invokeinterface java.util.Map.containsKey
	public boolean hasKey(String key) // = contains
	{
		return compound.e(key);
	}

	// invokeinterface java.util.Map.remove
	public void remove(String key)
	{
        compound.r(key);
	}

	// invokeinterface java.util.Map.get
	public int getInt(String key)
	{
    	return compound.h(key);
	}

	// invokeinterface java.util.Map.put
	public void setInt(String key, int value)
	{
        compound.a(key, value);
	}

	public String getString(String key)
	{
    	return compound.l(key);
	}

	public void setString(String key, String value)
	{
        compound.a(key, value);
	}

	// boolean is NBTNumber ~= byte, getBoolean gets byte and compares with 0, should check using:
	// (java.lang.String) : byte (with ifeq)
	public boolean getBoolean(String key)
	{
    	return compound.q(key);
	}

	public void setBoolean(String key, boolean value)
	{
        compound.a(key, value);
	}

	public byte[] getByteArray(String key)
	{
    	return compound.m(key);
	}

	public void setByteArray(String key, byte[] value)
	{
        compound.a(key, value);
	}
}
