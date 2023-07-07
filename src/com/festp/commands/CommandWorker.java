package com.festp.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;

import com.festp.maps.IMap;
import com.festp.maps.MapCraftHandler;
import com.festp.maps.MapFileManager;
import com.festp.maps.MapUtils;
import com.festp.maps.drawing.DrawingMap;
import com.festp.maps.drawing.ScanManager;
import com.festp.maps.small.SmallMap;
import com.festp.utils.Utils;

public class CommandWorker implements CommandExecutor, TabCompleter
{
	public final static String MAIN_COMMAND = "drawingmap";
	private final static String CONFIG_SUBCOMMAND = "config";
	private final static String GET_SUBCOMMAND = "get";
	private final static String CREATE_SUBCOMMAND = "create";
	private final static String SCAN_SUBCOMMAND = "scan";
	private final static String GETINFO_SUBCOMMAND = "getinfo";
	
	public final static String CONFIG_USAGE_SHORT = "/" + MAIN_COMMAND + " " + CONFIG_SUBCOMMAND;
	public final static String GET_USAGE_SHORT = "/" + MAIN_COMMAND + " " + GET_SUBCOMMAND + " <id>";
	public final static String GETINFO_USAGE_SHORT = "/" + MAIN_COMMAND + " " + GETINFO_SUBCOMMAND + " <id>";
	public final static String CREATE_USAGE_SHORT = "/" + MAIN_COMMAND + " " + CREATE_SUBCOMMAND + " <scale/drawing> [count]";
	public final static String SCAN_USAGE_SHORT = "/" + MAIN_COMMAND + " " + SCAN_SUBCOMMAND + " <on/off>";
	public final static String GET_USAGE = "Usage:\n" + GET_USAGE_SHORT;
	public final static String GETINFO_USAGE = "Usage:\n" + GETINFO_USAGE_SHORT;
	public final static String CREATE_USAGE = "Usage:\n" + CREATE_USAGE_SHORT;
	public final static String SCAN_USAGE = "Usage:\n" + SCAN_USAGE_SHORT;
	public final static String MAIN_USAGE = "Usage:\n"
			+ CONFIG_USAGE_SHORT + "\n"
			+ GET_USAGE_SHORT + "\n"
			+ GETINFO_USAGE_SHORT + "\n"
			+ CREATE_USAGE_SHORT + "\n"
			+ SCAN_USAGE_SHORT + "\n";

	private final static ChatColor COLOR_USAGE = ChatColor.GRAY;
	private final static ChatColor COLOR_ERROR = ChatColor.RED;
	private final static ChatColor COLOR_OK = ChatColor.GREEN;

	private final ScanManager scanManager;
	public CommandWorker(ScanManager scanManager) {
		this.scanManager = scanManager;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args)
	{
		if (!sender.isOp())
		{
			sender.sendMessage(COLOR_ERROR + "The sender must be an op.");
			return false;
		}
		
		if (cmd.getName().equalsIgnoreCase(MAIN_COMMAND))
		{
			if (args.length == 0)
			{
				sender.sendMessage(COLOR_USAGE + MAIN_USAGE);
				return true;
			}
			if (args[0].equalsIgnoreCase(CONFIG_SUBCOMMAND))
			{
				sender.sendMessage(COLOR_OK + "Config was reloaded.");
				// TODO config: turn on/off craft recipes, turn on/off a global scan, save image to file/to map data
				return true;
			}
			else if (args[0].equalsIgnoreCase(GET_SUBCOMMAND))
			{
				if (!(sender instanceof Player))
				{
					sender.sendMessage(COLOR_ERROR + "The sender must be a player.");
					return false;
				}
				if (args.length == 1)
				{
					sender.sendMessage(COLOR_USAGE + GET_USAGE);
					return false;
				}

				int id = -1;
				try {
					id = Integer.parseInt(args[1]);
				} catch(Exception e) {
					sender.sendMessage(COLOR_ERROR + "\"" + args[1] + "\" is not valid id. Please use integer numbers.");
					return false;
				}
				if (!MapUtils.isExists(id))
				{
					sender.sendMessage(COLOR_ERROR + "Map #" + id + " doesn't exists.");
					return false;
				}
				
				ItemStack map = MapUtils.getMap(id);
				Player player = (Player)sender;
				Utils.giveOrDrop(player, map);
				
				return true;
			}
			else if (args[0].equalsIgnoreCase(GETINFO_SUBCOMMAND))
			{
				if (args.length == 1)
				{
					sender.sendMessage(COLOR_USAGE + GETINFO_USAGE);
					return false;
				}

				int id = -1;
				try {
					id = Integer.parseInt(args[1]);
				} catch(Exception e) {
					sender.sendMessage(COLOR_ERROR + "\"" + args[1] + "\" is not valid id. Please use integer numbers.");
					return false;
				}
				if (!MapUtils.isExists(id))
				{
					sender.sendMessage(COLOR_ERROR + "Map #" + id + " doesn't exists.");
					return false;
				}

				// TODO map info class
				String type = "error";
				String vanillaInfo = "";
				String info = "";
				MapView mp = Bukkit.getMap(id);
				vanillaInfo = "center: " + mp.getWorld().getName() + " "+ "(" + mp.getCenterX() + ", " + mp.getCenterZ() + ")"
						+ ", scale: "+ mp.getScale() + ", isLocked: " + mp.isLocked()
						+ ", tracking: { isEnabled: " + mp.isTrackingPosition() + ", isUnlimited: "+ mp.isUnlimitedTracking() + "}";
				IMap map = MapFileManager.load(id);
				if (map == null) {
					type = "vanilla";
				} else if (map instanceof SmallMap) {
					type = "small";
					SmallMap sm = (SmallMap) map;
					info = "(" + sm.getX() + ", " + sm.getZ() + "), scale: " + sm.getScale();
				} else if (map instanceof DrawingMap) {
					type = "drawing";
					DrawingMap dm = (DrawingMap) map;
					info = "(" + dm.getX() + ", " + dm.getY() + ", " + dm.getZ() + "), scale: " + dm.getScale()
							+ ", direction: " + dm.getDirection() + ", isFullDiscovered: " + dm.isFullDiscovered();
				}
				sender.sendMessage(COLOR_OK + "Map #" + id + " is " + type);
				sender.sendMessage(COLOR_OK + vanillaInfo);
				sender.sendMessage(COLOR_OK + info);
				
				return true;
			}
			else if (args[0].equalsIgnoreCase(CREATE_SUBCOMMAND))
			{
				if (!(sender instanceof Player))
				{
					sender.sendMessage(COLOR_ERROR + "The sender must be a player.");
					return false;
				}
				if (args.length == 1)
				{
					sender.sendMessage(COLOR_USAGE + CREATE_USAGE);
					return false;
				}
				ItemStack map = null;
				boolean isDrawing = args[1].equalsIgnoreCase("drawing");
				String name = "Drawing map";
				if (isDrawing)
				{
					map = MapCraftHandler.getDrawingMap();
				}
				else
				{
					try {
						int scale = Integer.parseUnsignedInt(args[1]);
						map = MapCraftHandler.getScaleMap(scale);
						name = "Map (" + scale + ":1)";
					} catch (Exception e) {
						sender.sendMessage(COLOR_ERROR + "\"" + args[1] + "\" is an invalid scale. Try follow the tab-completion.");
					}
				}
				
				if (args.length == 3)
				{
					try {
						int count = Integer.parseUnsignedInt(args[2]);
						map.setAmount(count);
					} catch (Exception e) {
						sender.sendMessage(COLOR_ERROR + "\"" + args[2] + "\" is an invalid count. Try number from the range [1, " + map.getMaxStackSize() + "].");
					}
				}
				Player player = (Player)sender;
				Utils.giveOrDrop(player, map);
				sender.sendMessage(COLOR_OK + "Gave " + map.getAmount() + " [" + name + "] to " + sender.getName());
				return true;
			}
			else if (args[0].equalsIgnoreCase(SCAN_SUBCOMMAND))
			{
				if (!(sender instanceof Player))
				{
					sender.sendMessage(COLOR_ERROR + "The sender must be a player.");
					return false;
				}
				if (args.length == 1)
				{
					sender.sendMessage(COLOR_USAGE + SCAN_USAGE);
					return false;
				}
				Boolean isScanning = tryParseToggler(args[1]);
				if (isScanning == null)
				{
					sender.sendMessage(COLOR_ERROR + "\"on\" or \"off\" were expected, but \"" + args[1] + "\" have gotten.");
					return false;
				}
				
				if (isScanning)
					scanManager.add((Player)sender);
				else
					scanManager.remove((Player)sender);
					
				
				sender.sendMessage(COLOR_OK + "Scan was turned " + (isScanning ? "on" : "off"));
				return true;
			}
			
			sender.sendMessage(COLOR_USAGE + MAIN_USAGE);
			return true;
		}
		sender.sendMessage(COLOR_ERROR + "Invalid command.");
		return false;
	}
	
	private static Boolean tryParseToggler(String s)
	{
		if (s.equalsIgnoreCase("on"))
			return true;
		if (s.equalsIgnoreCase("off"))
			return false;
		return null;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args)
	{
		List<String> options = new ArrayList<>();
		if (!sender.isOp())
			return options;
		
		if (args.length <= 1) {
			options.add(CONFIG_SUBCOMMAND);
			options.add(GETINFO_SUBCOMMAND);
			if (sender instanceof Player)
			{
				options.add(GET_SUBCOMMAND);
				options.add(CREATE_SUBCOMMAND);
				options.add(SCAN_SUBCOMMAND);
			}
		}
		
		if (!(sender instanceof Player))
			return options;
		
		if (args.length == 2) {
			if (args[0].equalsIgnoreCase(GET_SUBCOMMAND) || args[0].equalsIgnoreCase(GETINFO_SUBCOMMAND)) {
				int maxId = MapUtils.getMaxId();
				if (maxId >= 0)
					options.add("0");
				if (maxId > 0)
					options.add("" + maxId);
			}
			if (args[0].equalsIgnoreCase(CREATE_SUBCOMMAND)) {
				options.add("drawing");
				options.add("128");
				options.add("8");
				options.add("4");
				options.add("2");
			}
			if (args[0].equalsIgnoreCase(SCAN_SUBCOMMAND)) {
				options.add("on");
				options.add("off");
			}
		}
		return options;
	}
}
