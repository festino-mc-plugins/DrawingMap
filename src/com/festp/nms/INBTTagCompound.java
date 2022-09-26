package com.festp.nms;

import net.minecraft.nbt.NBTTagCompound;

public interface INBTTagCompound
{
	//public static INBTTagCompound getTag(Object nmsStack);

	public void setTag(Object nmsStack);
	
	public boolean hasKey(String key);

	public void remove(String key);
	
	public int getInt(String key);

	public void setInt(String key, int value);
	
	public String getString(String key);

	public void setString(String key, String value);
	
	public boolean getBoolean(String key);

	public void setBoolean(String key, boolean value);
	
	public byte[] getByteArray(String key);

	public void setByteArray(String key, byte[] value);
}
