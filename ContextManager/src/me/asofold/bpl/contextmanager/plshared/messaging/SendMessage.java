package me.asofold.bpl.contextmanager.plshared.messaging;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SendMessage implements Runnable{
	String playerName;
	String message;
	public SendMessage(Player player, String message){
		playerName = player.getName();
		this.message = message;
	}
	
	public SendMessage(String exactName, String message){
		playerName = exactName;
		this.message = message;
	}

	@Override
	public void run() {
		Player player = Bukkit.getServer().getPlayerExact(playerName);
		if (player != null) player.sendMessage(message);
	}
	
	public boolean schedule(Plugin plugin, long ticksDelay){
		return Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin	, this, ticksDelay) != -1;
	}
}
