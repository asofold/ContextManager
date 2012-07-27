package me.asofold.bpl.contextmanager.hooks;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public interface ServiceHook {
	
	/**
	 * Be sure to make this a unique name.
	 * @return
	 */
	public String getHookName();
	
	/**
	 * Sub-command labels.
	 * for "/cx <label> ..."
	 * @return
	 */
	public String[] getCommandLabels();
	
	/**
	 * Execute a command, delegated by ContextManager - args will not contain label.
	 * @param sender
	 * @param label
	 * @param args
	 */
	public void onCommand(CommandSender sender, String label, String[] args);
	
	/**
	 * Listener to be registered with ContextManager as plugin.
	 * @return May return null;
	 */
	public Listener getListener();
	
	/**
	 * Called in the last stage of ContextManager.onEnable (your hook might be added during runtime, so this might not get called ever for it).
	 * @param plugin
	 */
	public void onEnable(Plugin plugin);
	
	/**
	 * Called once the hook has been added
	 */
	public void onAdd();
	
	/**
	 * Called once the hook has been removed.
	 */
	public void onRemove();
	
	/**
	 * For data saving etc.
	 * @param plugin
	 */
	public void onDisable();
	
	/**
	 * 
	 * @param sender
	 * @param args args[0] will be "find" or arbitrary, should be ignored.
	 * @return If something was found (and messaged to the sender). The finding process might end with one true result.
	 */
	public boolean delegateFind(CommandSender sender, String[] args);
	
}
