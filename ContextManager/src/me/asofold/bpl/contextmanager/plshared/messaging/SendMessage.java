package me.asofold.bpl.contextmanager.plshared.messaging;

import me.asofold.bpl.contextmanager.plshared.Messaging;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SendMessage implements Runnable{
	String playerName;
	String message;
	String command;
	
	public SendMessage(Player player, String message){
		this(player, message, null);
	}
	
	public SendMessage(Player player, String message, String command){
		this(player.getName(), message, command);
	}

	public SendMessage(String exactName, String message, String command) {
		playerName = exactName;
		this.message = message;
		this.command = command;
	}

	@Override
	public void run() {
		// TODO: players !?
		Player player = Bukkit.getServer().getPlayerExact(playerName);
		if (player != null) {
			if (command == null) {
				player.sendMessage(message);
			} else {
				Messaging.sendMessage(player, message, command);
			}
		}
	}
	
	public boolean schedule(Plugin plugin, long ticksDelay){
		return Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin	, this, ticksDelay) != -1;
	}
}
