package me.asofold.bukkit.contextmanager;

import me.asofold.bukkit.contextmanager.command.CMCommand;
import me.asofold.bukkit.contextmanager.core.CMCore;

import org.bukkit.plugin.java.JavaPlugin;

public class ContextManager extends JavaPlugin{
	
	public static final String plgLabel = "[ContextManager]";
	
	private final CMCore core;
	private final CMCommand cmdExe;
	
	public ContextManager(){
		core = new CMCore();
		cmdExe = new CMCommand(core);
	}
	
	@Override
	public void onEnable() {
		core.loadSettings();
		getServer().getPluginManager().registerEvents(core, this);
		for ( String cmd : cmdExe.getAllCommands()){
			// TODO: Most probably unnecessary !
			getCommand(cmd).setExecutor(cmdExe);
		}
		core.addStandardServiceHooks();
		core.onEnable(this);
		System.out.println(plgLabel+getDescription().getFullName()+ "enabled.");
	}

}
