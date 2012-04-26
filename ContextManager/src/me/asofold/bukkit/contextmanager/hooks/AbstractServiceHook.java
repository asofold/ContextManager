package me.asofold.bukkit.contextmanager.hooks;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Convenience: partial implementation, which does nothing. 
 * @author mc_dev
 *
 */
public abstract class AbstractServiceHook implements ServiceHook {
	
	// Override:

	@Override
	public abstract String getHookName();

	@Override
	public abstract String[] getCommandLabels();
	
	@Override
	public abstract void onCommand(CommandSender sender, String label, String[] args);

	// NO-OPs:

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

}
