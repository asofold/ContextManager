package me.asofold.bpl.contextmanager.plshared;

import java.util.Collection;

import me.asofold.bpl.contextmanager.plshared.messaging.AddressingScheme;
import me.asofold.bpl.contextmanager.plshared.messaging.AddressingSchemeImpl;
import me.asofold.bpl.contextmanager.plshared.messaging.json.IJsonMessageAPI;
import me.asofold.bpl.contextmanager.plshared.messaging.json.JMessage;
import me.asofold.bpl.contextmanager.plshared.messaging.json.JsonMessageFactory;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


/**
 * TODO: REFACGTOR TO MORE GENERAL THING !
 * @author mc_dev
 *
 */
public class Messaging {
	
	private static IJsonMessageAPI jImpl = null;
	
	/**
	 * Run at enabling some plugin.
	 */
	public static void init() {
		jImpl = new JsonMessageFactory().getNewAPI();
	}
	
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
		if (sender instanceof Player){
			sender.sendMessage(ChatColor.GRAY.toString()+msgColor+msg);
		} else{
			sender.sendMessage(Messaging.removeChatColors(msg));
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
	 * Json message (quick one).
	 * @param sender
	 * @param message
	 * @param command
	 */
	public static void sendMessage(CommandSender sender, String message, String command) {
		if (command == null) {
			sendMessage(sender, message);
		} else {
			sendComplexMessage(sender, new JMessage(message, command, command));
		}
	}
	
	/**
	 * Convenience method for json content (many parts).
	 * @param sender
	 * @param components String or Message.
	 */
	public static void sendComplexMessage(CommandSender sender, Object... components) {
		sendMessage(sender, components);
	}
	
	/**
	 * Json message with many parts.
	 * @param sender
	 * @param components String or Message.
	 */
	public static void sendComplexMessage(CommandSender sender, Collection<Object> components) {
		// TODO: Naming not optimal !?
		final Object[] arr = new Object[components.size()];
		components.toArray(arr);
		sendMessage(sender, arr);
	}
	
	/**
	 * Json message with many parts.
	 * @param sender
	 * @param objects String or Message.
	 */
	public static void sendMessage(CommandSender sender, Object[] components) {
		if (sender instanceof Player && jImpl != null) {
			jImpl.sendMessage((Player) sender, components);
		} else {
			StringBuilder b = new StringBuilder(256);
			for (Object obj : components) {
				if (obj instanceof String) {
					b.append((String) obj);
				} else if (obj instanceof JMessage) {
					b.append(((JMessage) obj).message);
				}
			}
			String message = b.toString();
			if (!message.isEmpty()) {
				if (!(sender instanceof Player)) {
					message = ChatColor.stripColor(message);
				}
				sender.sendMessage(message);
			}
		}
	}
	
}
