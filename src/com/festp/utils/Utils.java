package com.festp.utils;

import java.text.DecimalFormat;
import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.util.Vector;

public class Utils {
	public static final double EPSILON = 0.0001;
	public static final double THROW_POWER_K = 0.2;

	/** Can give items only to players.
	 * @return <b>null</b> if the <b>stack</b> was only given<br>
	 * <b>Item</b> if at least one item was dropped*/
	public static void giveOrDrop(Player player, ItemStack stack)
	{
		HashMap<Integer, ItemStack> res = player.getInventory().addItem(stack);
		if (res.isEmpty())
			return;
		dropUngiven(player.getLocation(), res.get(0));
	}
	private static Item dropUngiven(Location l, ItemStack stack) {
		Item item = l.getWorld().dropItem(l, stack);
		item.setVelocity(new Vector());
		item.setPickupDelay(0);
		return item;
	}
	
	public static Item drop(Location loc, ItemStack stack, double throw_power) {
		if(stack != null && stack.getType() != Material.AIR) {
			Item it = loc.getWorld().dropItem(loc, stack);
			it.setVelocity(throwVector(loc, throw_power));
			it.setPickupDelay(30);
			return it;
		}
		return null;
	}
	/** @return Vector in the <i>location</i> direction(using its yaw and pitch)*/
	public static Vector throwVector(Location location, double throw_power) {
		throw_power = THROW_POWER_K * throw_power;
		double yaw = ( location.getYaw() + 90 ) /180*Math.PI,
		pitch = ( location.getPitch() ) /180*Math.PI;
		double vec_x = Math.cos(yaw)*Math.cos(pitch)*throw_power,
			vec_y = -Math.sin(pitch)*throw_power,
			vec_z = Math.sin(yaw)*Math.cos(pitch)*throw_power;
		return new Vector(vec_x,vec_y,vec_z);
	}
	
	public static String toString(Vector v) {
		if (v == null)
			return "(null)";
		DecimalFormat dec = new DecimalFormat("#0.00");
		return ("("+dec.format(v.getX())+"; "
				  +dec.format(v.getY())+"; "
				  +dec.format(v.getZ())+")")
				.replace(',', '.');
	}
	public static String toString(Location l) {
		if (l == null) return toString((Vector)null);
		return toString(new Vector(l.getX(), l.getY(), l.getZ()));
	}
	public static String toString(Block b) {
		if (b == null) return toString((Location)null);
		return toString(b.getLocation());
	}
	
	
	public static boolean contains(Object[] list, Object find) {
		for (Object m : list)
			if (m == find)
				return true;
		return false;
	}

	public static boolean equal_invs(Inventory inv1, Inventory inv2) {
		return inv1.toString().endsWith(inv2.toString().substring(inv2.toString().length()-8));
	}
	
	public static boolean isRenamed(ItemStack item) {
		return item.hasItemMeta() && item.getItemMeta().hasDisplayName()
				&& !item.getItemMeta().getDisplayName().equals((new ItemStack(item.getType())).getItemMeta().getDisplayName());
	}
	
	/**@return <b>true</b> if the <b>stack</b> was given<br>
	 * <b>false</b> if the <b>stack</b> can't be given without stacking*/
	public static boolean giveUnstackable(Inventory inv, ItemStack stack)
	{
		ItemStack[] stacks = inv.getStorageContents();
		for (int i = 0; i < stacks.length; i++)
		{
			if (stacks[i] == null)
			{
				stacks[i] = stack.clone();
				inv.setStorageContents(stacks);
				stack.setAmount(0);
				return true;
			}
		}
		return false;
	}
	
	public static <T> int indexOf(T needle, T[] haystack)
	{
	    for (int i=0; i<haystack.length; i++)
	    {
	        if (haystack[i] != null && haystack[i].equals(needle)
	            || needle == null && haystack[i] == null) return i;
	    }

	    return -1;
	}
	public static <T> T next(T needle, T[] haystack)
	{
		int index = Utils.indexOf(needle, haystack);
		if (index < 0)
			return null;
		return haystack[(index + 1) % haystack.length];
	}
	public static <T> T prev(T needle, T[] haystack)
	{
		int index = Utils.indexOf(needle, haystack);
		if (index < 0)
			return null;
		return haystack[((index - 1) % haystack.length + haystack.length) % haystack.length];
	}
	
	public static ItemStack setShulkerInventory(ItemStack shulker_box, Inventory inv)
	{
    	BlockStateMeta im = (BlockStateMeta)shulker_box.getItemMeta();
    	ShulkerBox shulker = (ShulkerBox) im.getBlockState();
    	shulker.getInventory().setContents(inv.getStorageContents());
    	im.setBlockState(shulker);
    	shulker_box.setItemMeta(im);
    	return shulker_box;
	}
	public static Inventory getShulkerInventory(ItemStack shulker_box)
	{
    	BlockStateMeta im = (BlockStateMeta)shulker_box.getItemMeta();
    	ShulkerBox shulker = (ShulkerBox) im.getBlockState();
    	return shulker.getInventory();
	}
}
