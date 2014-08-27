package me.asofold.bpl.contextmanager;

import me.asofold.bpl.contextmanager.command.CMCommand;
import me.asofold.bpl.contextmanager.core.CMCore;
import me.asofold.bpl.contextmanager.plshared.Messaging;

import org.bukkit.command.PluginCommand;
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
			PluginCommand command = getCommand(cmd);
			if (command != null) command.setExecutor(cmdExe);
		}
		core.addStandardServiceHooks();
		core.addListeners(this);
		core.onEnable(this);
		Messaging.init();
		System.out.println(plgLabel+" "+getDescription().getFullName()+ " enabled.");
	}

	@Override
	public void onDisable() {
		core.onDisable();
		System.out.println(plgLabel+" "+getDescription().getFullName()+ " disabled.");
	}
	
	

}
