package me.asofold.bpl.contextmanager.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import me.asofold.bpl.contextmanager.ContextManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Utils {

	public static final boolean hasPermission(CommandSender sender, String perm){
		return sender.isOp() || sender.hasPermission(perm);
	}

	public static final void send(CommandSender sender, String message){
		if ( sender instanceof Player) sender.sendMessage(message);
		else sender.sendMessage(ChatColor.stripColor(message));
	}

	/**
	 * With failure message.
	 * @param sender
	 * @param perm
	 * @return
	 */
	public static final boolean checkPerm(CommandSender sender, String perm){
		if (hasPermission(sender, perm)) return true;
		send(sender, ChatColor.DARK_RED+"Permission not present: "+perm);
		return false;
	}

	public static boolean checkPlayer(CommandSender sender){
		if (sender instanceof Player) return true;
		sender.sendMessage(ContextManager.plgLabel+" This is only available to players!");
		return false;
	}

	public static final boolean isPlayerKnown(String name){
		if ( Bukkit.getPlayerExact(name)!=null) return true;
		OfflinePlayer player = Bukkit.getOfflinePlayer(name);
		if ( player == null) return false;
		if ( player.hasPlayedBefore()) return true;
		return false;
	}

	/**
	 * 
	 * @param parts
	 * @param link can be null
	 * @return
	 */
	public static final String join(Collection<String> parts, String link){
		StringBuilder builder = new StringBuilder();
		int i = 0;
		int max = parts.size();
		for ( String part : parts){
			builder.append(part);
			i++;
			if ( i<max && link!=null ) builder.append(link);
		}
		return builder.toString();
	}

	public static final String joinObjects(Collection<?> parts, String link){
		LinkedList<String> ref = new LinkedList<String>();
		for (Object o : parts){
			ref.add(o.toString());
		}
		return join(ref, link);
	}

	public static Collection<String> getCollection(String[] args, int  startIndex){
		List<String> out = new LinkedList<String>();
		for ( int i = startIndex; i<args.length; i++){
			out.add(args[i]);
		}
		return out;
	}

	/**
	 * Send if online.
	 * @param playerName
	 * @param msg
	 */
	public static final void tryMessage(final String playerName, String msg) {
		Player player = Bukkit.getServer().getPlayerExact(playerName);
		if (player != null) player.sendMessage(msg);
	}

}
