package me.asofold.bpl.contextmanager.plshared;

import java.util.Collection;

import me.asofold.bpl.contextmanager.plshared.messaging.AddressingScheme;
import me.asofold.bpl.contextmanager.plshared.messaging.AddressingSchemeImpl;
import me.asofold.bpl.contextmanager.plshared.messaging.SendMessage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;


/**
 * TODO: REFACGTOR TO MORE GENERAL THING !
 * @author mc_dev
 *
 */
public class Messaging {
	
	public static  AddressingScheme getAddressingScheme(){
		// TODO: take from internals
		AddressingScheme scheme = new AddressingSchemeImpl();
		return scheme;
	}
	
	public static void sendErrorMessage(CommandSender sender, String msg){
		sendMessage(sender, msg, ChatColor.DARK_RED);
	}
	
	public static void sendSuccessMessage(CommandSender sender, String msg){
		sendMessage(sender, msg, ChatColor.DARK_GREEN);
	}
	
	public static void sendNeutralMessage(CommandSender sender, String msg){
		sendMessage(sender, msg, ChatColor.GRAY);
	}
	
	/**
	 * TODO: configurability: colors, prefix, addressing scheme !
	 * @param sender
	 * @param msg
	 * @param msgColor
	 */
	public static void sendMessage(CommandSender sender, String msg, ChatColor msgColor){
		String prefix = 
				"[pluginlib] ";
		if ( sender instanceof Player){
			sender.sendMessage(ChatColor.GRAY+prefix+msgColor+msg);
		} else{
			sender.sendMessage(Messaging.removeChatColors(prefix + msg));
		}
		
	}
	
	public static final void sendMessage(final CommandSender sender, final String[] messages){
		if ( sender instanceof Player){
			sender.sendMessage(messages);
		}
		else{
			final String[] stripped = new String[messages.length];
			for (int i = 0; i< messages.length; i++){
				stripped[i] = ChatColor.stripColor(messages[i]);
			}
			sender.sendMessage(messages);
		}
	}
	
	public static final void sendMessage(final CommandSender sender, final Collection<String> messages){
		String[] a = new String[messages.size()];
		if (sender instanceof Player){
			messages.toArray(a);
			sender.sendMessage(a);
		}
		else{
			int i = 0;
			for (String msg : messages){
				a[i] = ChatColor.stripColor(msg);
				i++;
			}
			sender.sendMessage(a);
		}
	}

	public static final String removeChatColors(String msg) {
		return ChatColor.stripColor(msg); // TODO: common color-aliases or just remove this?
	}
	
	public static void sendMessage(CommandSender sender, String message){
		if ( sender instanceof Player){
			sender.sendMessage(message);
		} else{
			sender.sendMessage(ChatColor.stripColor(message));
		}
	}
	
	/**
	 * Broadcast a message in a certain world.
	 * @param world
	 * @param message
	 */
	public static void broadcast(World world, String message){
		for ( Player player : Bukkit.getServer().getOnlinePlayers()){
			if ( world.equals(player.getWorld())) player.sendMessage(message);
		}
	}
	
	/**
	 * Broadcast the message in that world if the world exists.
	 * @param worldName
	 * @param message
	 */
	public static void broadcast( String worldName, String message){
		World world = Bukkit.getServer().getWorld(worldName);
		if (world == null) return;
		broadcast(world, message);
	}
	
	public static void scheduleMessage(Plugin plugin, Player player , String msg, long ticks,  boolean force){
		if (Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new SendMessage(player, msg), ticks) == -1){
			if ( force ){
				player.sendMessage(msg);
			}
		}
	}
	
	/**
	 * Compatibility method.
	 * @param input
	 * @return
	 */
	public static String withChatColors(String input, char colorChar) {
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if ((chars[i] == colorChar) && ("0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(chars[i+1]) >= 0)) {
                chars[i] = ChatColor.COLOR_CHAR;
                chars[i+1] = Character.toLowerCase(chars[i+1]);
            }
        }
        return new String(chars);
    }
	
	/**
	 * Convert "&9" and similar to ChatColor.getByCode(...)
	 * @param input
	 * @return
	 */
	public static String withChatColors(String input){
		return withChatColors(input, '&');
	}
	
	/**
	 * @deprecated unnecessary method
	 * @param input
	 * @param split
	 * @param useChatColor
	 * @return
	 */
	public static String withChatColors(String input, String split, boolean useChatColor){
		String[] s =(" "+input).split(split);
		if ( s.length == 1 ) return input;
		String out = s[0].substring(1);
		for ( int i = 1; i<s.length;i++){
			String c = s[i];
			if (c.length() >0 ) {
//				char x = c.charAt(0);
//				int code = Character.digit(x, 16);
				String x = null;
				ChatColor x2 = ChatColor.getByChar(c.charAt(0));
				if ( x2 != null){// code != -1){
					if ( useChatColor ) x = x2.toString();
					else x ="§"+ c.charAt(0);
					out += x+c.substring(1); // ChatColor.getByCode(code)+c.substring(1);
				} else{
					out += split+c;
				}
			} else out += split;
		}
		return out;
	}
	
	/**
	 * Try to find player (exact) and send the message if online.
	 * @param playerName
	 * @param message
	 * @return If message was sent.
	 */
	public static boolean tryMessage(String playerName, String message){
		Player player = Bukkit.getServer().getPlayerExact(playerName);
		if (player == null || !player.isOnline()) return false;
		player.sendMessage(message);
		return true;
	}
	
}
