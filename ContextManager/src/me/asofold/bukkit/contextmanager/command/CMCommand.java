package me.asofold.bukkit.contextmanager.command;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.asofold.bukkit.contextmanager.ContextManager;
import me.asofold.bukkit.contextmanager.chat.HistoryElement;
import me.asofold.bukkit.contextmanager.config.Channels;
import me.asofold.bukkit.contextmanager.core.CMCore;
import me.asofold.bukkit.contextmanager.core.ContextType;
import me.asofold.bukkit.contextmanager.core.PlayerData;
import me.asofold.bukkit.contextmanager.util.Utils;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CMCommand implements CommandExecutor {
	
	/**
	 * First is the label, rest aliases, for each array.	
	 */
	private static final String[][] presetCommandAliases = new String[][]{
		// main commands
		{"cmreload"},
		{"mute", "cmmute"},
		{"unmute", "demute", "cmunmute"},
		{"muted"},
		{"context", "cx"},
		// sub commands:
		{"channel", "chan", "ch", "c", "channels"},
		{"world", "wor", "w"},
		{"info", "?", "help", "hlp"},
		{"reset", "clear", "clr", "cl", "res"},
		{"ignore", "ign", "ig" , "i"},
		{"all", "al", "a"},
		{"global", "glob", "glo", "gl", "g"},
		{"recipients", "recipient", "recip", "rec", "re" , "r"},
		{"cxc", "cxch"},
		{"cxr", "cxrec"},
		{"history", "hist", "h"},
		{"default", "def"},
		{"greedy", "greed", "gre"},
	};
	
	private static final String[] allCommands = new String[]{
		"cmreload",	"cmmute", "cmunmute", "mute", "unmute", "demute", "muted",
		"context", "cxc", "cxch", "cxr", "cxrec", "cxign", "cxcl", "cxinf",
	};
	
	private CMCore core;
	
	Map<String, String> commandAliases = new HashMap<String, String>();
	
	public CMCommand(CMCore core){
		this.core = core;
		// map aliases to label.
		for ( String[] ref : presetCommandAliases){
			String label = ref[0];
			for ( String n : ref){
				commandAliases.put(n, label);
			}
		}
	}
	
	/**
	 * Get lower case version, possibly mapped from an abreviation.
	 * @param input
	 * @return
	 */
	public String getMappedCommandLabel(String input){
		input = input.trim().toLowerCase();
		String out = commandAliases.get(input);
		if (out == null) return input;
		else return out;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		core.lightChecks();
		label = getMappedCommandLabel(label);
		int len = args.length;
		
		if (label.equals("cxc")) return onCommand(sender, null, "context", inflate(args, "channel"));
		else if (label.equals("cxr")) return onCommand(sender, null, "context", inflate(args, "recipients"));
		else if (label.equals("cxign")) return onCommand(sender, null, "context", inflate(args, "ignore"));
		else if (label.equals("cxcl")) return onCommand(sender, null, "context", inflate(args, "reset"));
		else if (label.equals("cxinf")) return onCommand(sender, null, "context", inflate(args, "info"));
		else if ( label.equals("cmreload")){
			if( !Utils.checkPerm(sender, "contextmanager.admin.cmd.reload")) return true;
			core.loadSettings();
			sender.sendMessage(ContextManager.plgLabel+" Settings reloaded");
			return true;
			
		} 
		else if ((len==1 || len==2 ) && label.equals("mute")){
			if( !Utils.checkPerm(sender, "contextmanager.admin.cmd.mute")) return true;
			int minutes = 0;
			if ( len == 2){
				try{
					minutes = Integer.parseInt(args[1]);
					if ( minutes <=0 ) throw new NumberFormatException();
				} catch ( NumberFormatException e){
					Utils.send(sender, ChatColor.DARK_RED+"Bad number for minutes: "+args[1]);
				}
			}
			String name = args[0].trim().toLowerCase();
			boolean known = Utils.isPlayerKnown(name);
			
			String end = ".";
			Long ts = 0L;
			if (minutes>0){
				end = " for "+minutes+" minutes.";
				ts = System.currentTimeMillis() + 60000L* (long)minutes;
			}
			core.getMuted().put(name, ts);
			Utils.send(sender, ChatColor.YELLOW+ContextManager.plgLabel+" Muted "+(known?"":(ChatColor.RED+"unknown "+ChatColor.YELLOW))+"player '"+name+"'"+end);
			return true;
		} 
		else if ((len==1 ) && label.equals("unmute")){
			if( !Utils.checkPerm(sender, "contextmanager.admin.cmd.unmute")) return true;
			if ( args[0].equals("*")){
				core.getMuted().clear();
				Utils.send(sender,ChatColor.YELLOW+ContextManager.plgLabel+" Cleared muted players list.");
				return true;
			}
			String name = args[0].trim().toLowerCase();
			Long ts = core.getMuted().remove(name);
			if ( ts!=null ){
				Utils.send(sender, ChatColor.YELLOW+ContextManager.plgLabel+" Removed from muted: "+name);
			} else{
				Utils.send(sender, ChatColor.GRAY+ContextManager.plgLabel+" Not in muted: "+name);
			}
			return true;
		} 
		else if (label.equals("muted")){
			if( !Utils.checkPerm(sender, "contextmanager.admin.cmd.muted")) return true;
			Utils.send(sender, ChatColor.YELLOW+ContextManager.plgLabel+" Muted: "+ChatColor.GRAY+Utils.join(core.getMuted().keySet(), ChatColor.DARK_GRAY+" | "+ChatColor.GRAY));
			return true;
		}
		else if (label.equals("context")){
			if (!Utils.checkPlayer(sender)) return true;
			return contextCommand((Player) sender, args);
		}
		return false;
	}
	
	private String[] inflate(String[] args, String cmd) {
		String[] out = new String[args.length+1];
		out[0] = cmd;
		for ( int i = 0 ; i<args.length; i++){
			out[i+1] = args[i];
		}
		return out;
	}

	/**
	 * Handling a "context" command for a player.
	 * @param player
	 * @param args
	 * @return
	 */
	private boolean contextCommand(Player player, String[] args) {
		int len = args.length;
		if (len == 0) return false; // send usage info.
		String cmd = getMappedCommandLabel(args[0]);
		// TODO: permissions
		PlayerData data = core.getPlayerData(player.getName());
		if (cmd.equals("reset")){
			if (len == 1){
				data.resetContexts();
				Utils.send(player, ChatColor.YELLOW+ContextManager.plgLabel+" Contexts reset.");
			} else if (len==2){
				String target = getMappedCommandLabel(args[1]);
				if (target.equals("ignore")){
					data.resetIgnored();
					Utils.send(player, ChatColor.YELLOW+ContextManager.plgLabel+" Ignored players reset.");
				}
				else if (target.equals("all")){
					data.resetAll();
					Utils.send(player, ChatColor.YELLOW+ContextManager.plgLabel+" Everything reset.");
				}
				else if (target.equals("recipients")){
					data.resetRecipients();
					Utils.send(player, ChatColor.YELLOW+ContextManager.plgLabel+" Recipients reset.");
				}
				else if (target.equals("channel")){
					data.resetChannel();
					Utils.send(player, ChatColor.YELLOW+ContextManager.plgLabel+" Channel reset.");
				}
			}
			sendInfo(player, data);
			return true;
		}
		else if (cmd.equals("ignore")){
			// TODO: also use access methods.
			for (int i = 1; i< args.length; i++){
				String c = args[i].trim().toLowerCase();
				if (c.isEmpty()) continue;
				if (c.startsWith("-") && c.length()>1){
					data.ignored.remove(c.substring(1));
					continue;
				}
				else data.ignored.add(c);
			}
			sendInfo(player, data);
			return true;
		}
		else if (cmd.equals("recipients")){
			if (len>1) data.addRecipients(args, 1);
			sendInfo(player, data);
			return true;
		}
		else if (cmd.equals("channel")){
			if (len == 2){
				if (getMappedCommandLabel(args[1]).equals(Channels.defaultChannelName)) data.resetChannel();
				else{
					String channel = core.getAvailableChannel(args[1]);
					if (channel == null){
						player.sendMessage(ChatColor.DARK_RED+"[Context] Unavailable channel: "+args[1]);
					}
					else{
						data.setChannel(channel);
					}
				}
			}
			else if (len == 1){
				player.sendMessage(core.getChannesString());
			}
			sendInfo(player, data);
			return true;
		}
		else if(cmd.equals("info")){
			// send a bunch of info
			sendInfo(player, data);
			return true;
		}
		else if (cmd.equals("history")){
			// shows 50 at a time , max.
			if (!Utils.checkPerm(player,"contextmanager.admin.cmd.history")) return true;
			int startIndex = 0;
			if (len>=2){
				try{
					int i = Integer.parseInt(args[1].trim());
					if (i<0) return badNumber(player, args[1]);
					startIndex = i;
				} catch (NumberFormatException e){
					return badNumber(player, args[1]);
				}
			} 			
			int endIndex = Math.max(0, startIndex + 50);
			
			// collect permissions:
			Map<ContextType, Boolean> perms = new HashMap<ContextType, Boolean>();
			boolean hasSomePerm = false;
			for ( ContextType type : ContextType.values()){
				boolean has = Utils.hasPermission(player, "contextmanager.history.display."+type.toString().toLowerCase());
				hasSomePerm |= has;
				perms.put(type, has);
			}
			if (!hasSomePerm){
				Utils.send(player, ChatColor.DARK_RED+ContextManager.plgLabel+" You are lacking the permissions to view any entries.");
				return true;
			}
			// collect candidates: TODO: 
			List<HistoryElement> candidates = new LinkedList<HistoryElement>();
			List<HistoryElement> history = core.getHistory();
			int i = history.size()-1;
			for (HistoryElement element : history){
				if (i<startIndex) break;
				else if (i>endIndex);
				else if (perms.get(element.type)) candidates.add(element);
				i --;
			}
			String[] msgs = new String[2+candidates.size()]; 
			msgs[0] = ChatColor.YELLOW+"[Chat] History ("+endIndex+"..."+startIndex+"):";
			i = 1;
			for (HistoryElement element : candidates){
				msgs[i] = element.toString();
				i++;
			}
			msgs[1+candidates.size()] = ChatColor.YELLOW+"[Chat] History ("+startIndex+"..."+endIndex+" / "+history.size()+") - "+candidates.size() + " viewable, done.";
			player.sendMessage(msgs);
			return true;
		}
		else if (cmd.equals("greedy")){
			if (!Utils.checkPerm(player, "contextmanager.cmd.greedy")) return true;
			if (data.greedy == null || len>1){
				data.greedy = null;
				final ContextType[] availableGreedy = new ContextType[]{ContextType.CHANNEL, ContextType.PRIVATE};
				Set<ContextType> greedy = new HashSet<ContextType>();
				if (len>1){
					for (int i = 1; i< args.length; i++){
						String ref = args[i].trim().toUpperCase();
						if (ref.isEmpty()) continue;
						for (ContextType type : availableGreedy){
							// TODO: maybe something more efficient ?
							if (type.toString().startsWith(ref) && Utils.hasPermission(player, "contextmanager.greedy."+type.toString().toLowerCase())) greedy.add(type);
						}
						// TODO: maybe message if nothing found.
					}
				} else{
					for (ContextType type : availableGreedy){
						if (Utils.hasPermission(player, "contextmanager.greedy."+type.toString().toLowerCase())) greedy.add(type);
					}
				}
				if (greedy.isEmpty()){
					player.sendMessage(ChatColor.YELLOW + "[Context] No greedy entries available!");
				} else{
					player.sendMessage(ChatColor.YELLOW + "[Context] Greedy list set (see below).");
					data.greedy = greedy;
				}
			} 
			else{
				data.greedy = null;
				player.sendMessage(ChatColor.YELLOW + "[Context] Greedy list cleared.");
			}
			sendInfo(player, data);
			return true;
		}
		return false;
	}
	
	public boolean badNumber(CommandSender sender, String arg){
		Utils.send(sender, ChatColor.DARK_RED+ContextManager.plgLabel+" Bad number: "+arg);
		return true;
	}

	private void sendInfo(Player player, PlayerData data) {
		// TODO: refactor to work as complete info for another player.
		if (core.isMuted(player)) player.sendMessage(ChatColor.DARK_GRAY+"[Context] "+ChatColor.RED+"You are muted!");
		if (!data.ignored.isEmpty()) player.sendMessage(ChatColor.DARK_GRAY+"[Ignored] "+Utils.join(data.ignored, " | "));
		player.sendMessage(ChatColor.GRAY+"[Channel] "+(data.channel==null?(core.getDefaultChannelDisplayName()):data.channel));
		if (!data.recipients.isEmpty()) player.sendMessage(ChatColor.GRAY+"[Recipients] "+Utils.join(data.recipients, " | "));
		if (data.greedy != null) player.sendMessage(ChatColor.YELLOW+"[Greedy] "+Utils.joinObjects(data.greedy, " | "));
		if (core.isPartyChat(player)) player.sendMessage(ChatColor.YELLOW+"[Party] "+ChatColor.GREEN+"On");
	}

	public String[] getAllCommands() {
		return allCommands;
	}



}
