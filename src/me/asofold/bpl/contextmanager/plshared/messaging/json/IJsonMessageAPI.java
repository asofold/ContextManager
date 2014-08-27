package me.asofold.bpl.contextmanager.plshared.messaging.json;

import org.bukkit.entity.Player;

public interface IJsonMessageAPI {
	
	/**
	 * 
	 * @param player
	 * @param components Accept String and JMessage instances.
	 */
	public void sendMessage(Player player, Object... components);

}
