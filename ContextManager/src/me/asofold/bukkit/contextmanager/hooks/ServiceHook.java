package me.asofold.bukkit.contextmanager.hooks;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;

public interface ServiceHook {
	/**
	 * for "/cx <label> ..."
	 * @return
	 */
	public String[] getCommandLabels();
	
	/**
	 * 
	 * @return May return null;
	 */
	public Listener getListener();
	
	/**
	 * Execute a command, delegated by ContextManager - args will not contain label.
	 * @param sender
	 * @param label
	 * @param args
	 */
	public void onCommand(CommandSender sender, String label, String[] args);
	
	
}
