package me.asofold.bpl.contextmanager.hooks;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Convenience: partial implementation, which does nothing. 
 * @author mc_dev
 *
 */
public abstract class AbstractServiceHook implements ServiceHook {

	// NO-OPs:
	
	@Override
	public String[] getCommandLabelAliases(String label){
		return null;
	}

	@Override
	public Listener getListener() {
		return null;
	}

	@Override
	public void onEnable(Plugin plugin) {
	}

	@Override
	public void onAdd() {
	}

	@Override
	public void onRemove() {
	}

	@Override
	public void onDisable() {
	}

	@Override
	public boolean delegateFind(CommandSender sender, String[] args) {
		return false;
	}
	
	

}
