package com.festp;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.festp.commands.CommandWorker;
import com.festp.maps.MapCraftHandler;
import com.festp.maps.MapEventHandler;
import com.festp.maps.drawing.ScanManager;
import com.festp.utils.NBTUtils;

public class Main extends JavaPlugin implements Listener
{
	public static final String mapsdir = "Maps";
	private static String PATH = "plugins" + System.getProperty("file.separator") + "ERROR_DRAWING_MAP_PLUGIN_NAME" + System.getProperty("file.separator");
	private static String pluginname;
	
	private CraftManager craft_manager;
	
	public static String getPath() {
		return PATH;
	}
	
	long t1;
	public void onEnable() {
		Logger.setLogger(getLogger());
		NBTUtils.setPlugin(this);
		pluginname = getName();
		PATH = "plugins" + System.getProperty("file.separator") + pluginname + System.getProperty("file.separator");
    	PluginManager pm = getServer().getPluginManager();
    	
    	MapCraftHandler mapCrafts = new MapCraftHandler();
    	pm.registerEvents(mapCrafts, this);
    	MapEventHandler mapHandler = new MapEventHandler();
    	pm.registerEvents(mapHandler, this);

    	craft_manager = new CraftManager(this, getServer());
    	MapCraftHandler.addCrafts(this);
    	pm.registerEvents(craft_manager, this);

    	ScanManager scanManager = new ScanManager();
    	CommandWorker cw = new CommandWorker(scanManager);
    	getCommand(CommandWorker.MAIN_COMMAND).setExecutor(cw);
    	getCommand(CommandWorker.MAIN_COMMAND).setTabCompleter(cw);
    	
    	Bukkit.getScheduler().scheduleSyncRepeatingTask(this,
    			new Runnable() {
    				public void run() {
    					TaskList.tick();
    					scanManager.tick();
    				}
    			}, 0L, 1L);
	}
	
	public CraftManager getCraftManager()
	{
		return craft_manager;
	}
}
