package com.festp.nms;

import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import com.festp.Logger;

/**
 * Should be checked every spigot update
 */
public class NBTUtils
{
	/** Use for paths like "org.bukkit.craftbukkit.v1_19_R1". */
	public static Class getCraftbukkitClass(String relativePath)
	{
		String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
		try {
			return Class.forName("org.bukkit.craftbukkit." + version + "." + relativePath);
		} catch (ClassNotFoundException e) { }
		return null;
	}
	private static Object getNMSCopy(ItemStack is)
	{
		//return CraftItemStack.asNMSCopy(is);
		Class bukkitClass = getCraftbukkitClass("inventory.CraftItemStack");
		Method asCopyMethod;
		try {
			asCopyMethod = bukkitClass.getMethod("asNMSCopy", ItemStack.class);
			return (net.minecraft.world.item.ItemStack)asCopyMethod.invoke(null, is);
		} catch (Exception e) {
			Logger.severe("Reflection error in NBTUtils.getNMSCopy(ItemStack)");
			return null;
		}
	}

	private static ItemStack getBukkitCopy(Object nmsStack)
	{
		//return CraftItemStack.asBukkitCopy(nmsStack);
		Class bukkitClass = getCraftbukkitClass("inventory.CraftItemStack");
		Method asCopyMethod;
		try {
			asCopyMethod = bukkitClass.getMethod("asBukkitCopy", nmsStack.getClass());
			return (ItemStack)asCopyMethod.invoke(null, nmsStack);
		} catch (Exception e) {
			Logger.severe("Reflection error in NBTUtils.getBukkitCopy(nms.ItemStack)");
			return null;
		}
	}
	
	private static INBTTagCompound getTag(Object nmsStack)
	{
        INBTTagCompound compound = null;
        if (true) // check version
        {
            compound = NBTTagCompound_1_19_R1.getTag(nmsStack);
        }
        return compound;
	}
	
	public static ItemStack setData(ItemStack i, String field, Object data)
	{
        if (data == null || field == null || i == null)
            return i;
        
		Object nmsStack = getNMSCopy(i);
        INBTTagCompound compound = getTag(nmsStack);
        if (compound == null)
        {
        	// TODO error
        }
        
        if (data instanceof String)
        	compound.setString(field, (String)data);
        else if (data instanceof Integer)
        	compound.setInt(field, (Integer)data);
        else if (data instanceof Boolean)
        	compound.setBoolean(field, (Boolean)data);
        else if (data instanceof byte[])
        	compound.setByteArray(field, (byte[])data);
        
        compound.setTag(nmsStack);
        i = getBukkitCopy(nmsStack);
        return i;
	}

	private static INBTTagCompound get(ItemStack i, String field) {
        if (field == null || i == null)
            return null;
        Object nmsStack = getNMSCopy(i);
        INBTTagCompound compound = getTag(nmsStack);
        return compound;
	}
	public static String getString(ItemStack i, String field) {
        INBTTagCompound compound = get(i, field);
        if (compound == null || !compound.hasKey(field))
            return null;
        return compound.getString(field);
	}
	public static Integer getInt(ItemStack i, String field) {
        INBTTagCompound compound = get(i, field);
        if (compound == null || !compound.hasKey(field))
            return null;
        return compound.getInt(field);
	}
	
	public static Boolean getBoolean(ItemStack i, String field) {
        INBTTagCompound compound = get(i, field);
        if (compound == null || !compound.hasKey(field))
            return null;
        return compound.getBoolean(field);
	}
	
	public static byte[] getByteArray(ItemStack i, String field) {
        INBTTagCompound compound = get(i, field);
        if (compound == null || !compound.hasKey(field))
            return null;
        return compound.getByteArray(field);
	}
	
	public static boolean hasDataField(ItemStack i, String field) {
        INBTTagCompound compound = get(i, field);
        if (compound == null || !compound.hasKey(field))
        	return false;
        return true;
	}
	
	public static boolean hasData(ItemStack i, String field, String data) {
        INBTTagCompound compound = get(i, field);
        if(data != null && compound != null && compound.hasKey(field) && data.equalsIgnoreCase(compound.getString(field)))
        	return true;
        return false;
	}
}
