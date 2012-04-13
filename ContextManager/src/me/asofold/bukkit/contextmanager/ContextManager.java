package me.asofold.bukkit.contextmanager;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import asofold.pluginlib.shared.Messaging;
import asofold.pluginlib.shared.permissions.pex.PexUtil;

public class ContextManager extends JavaPlugin implements Listener{
	
	private final String plgLabel = "[ContextManager]";
	
	String msgCol = ChatColor.WHITE.toString();
	String partyNameCol = ChatColor.GRAY.toString();
	String partyBracketCol = ChatColor.GREEN.toString();
	String partyMsgCol = ChatColor.GRAY.toString();
	String broadCastCol = ChatColor.YELLOW.toString();
	
	/**
	 * muted players
	 */

    Map<String, Long> muted = new HashMap<String, Long>();
	
	boolean useEvent = true;
	
	@Override
	public void onEnable() {
		loadSettings();
		getServer().getPluginManager().registerEvents(this, this);
		for ( String cmd : new String[]{
			"cmreload",	"cmmute", "cmunmute", "mute", "unmute", "demute",
		}){
			getCommand(cmd).setExecutor(this);
		}
		System.out.println(plgLabel+getDescription().getFullName()+ "enabled.");
	}
	
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

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		lightChecks();
		label = label.trim().toLowerCase();
		int len = args.length;
		if ( label.equals("cmreload")){
			if( !checkPerm(sender, "contextmanager.admin.cmd.reload")) return true;
			loadSettings();
			sender.sendMessage(plgLabel+" Settings reloaded");
			return true;
			
		} 
		else if ( (len==1 || len==2 ) && (label.equals("mute") || label.equals("cmmute"))){
			if( !checkPerm(sender, "contextmanager.admin.cmd.mute")) return true;
			int minutes = 0;
			if ( len == 2){
				try{
					minutes = Integer.parseInt(args[1]);
					if ( minutes <=0 ) throw new NumberFormatException();
				} catch ( NumberFormatException e){
					send(sender, ChatColor.DARK_RED+"Bad number for minutes: "+args[1]);
				}
			}
			String name = args[0].trim().toLowerCase();
			boolean known = isPlayerKnown(name);
			
			String end = ".";
			Long ts = 0L;
			if (minutes>0){
				end = " for "+minutes+" minutes.";
				ts = System.currentTimeMillis() + 60000L* (long)minutes;
			}
			muted.put(name, ts);
			send(sender, ChatColor.YELLOW+plgLabel+" Muted "+(known?"":(ChatColor.RED+"unknown "+ChatColor.YELLOW))+"player '"+name+"'"+end);
			return true;
		} 
		else if ( (len==1 ) && (label.equals("unmute") || label.equals("demute") || label.equals("cmunmute"))){
			if( !checkPerm(sender, "contextmanager.admin.cmd.unmute")) return true;
			if ( args[0].equals("*")){
				muted.clear();
				send(sender,ChatColor.YELLOW+plgLabel+" Cleared muted players list.");
				return true;
			}
			String name = args[0].trim().toLowerCase();
			Long ts = muted.remove(name);
			if ( ts!=null ){
				send(sender, ChatColor.YELLOW+plgLabel+" Removed from muted: "+name);
			} else{
				send(sender, ChatColor.GRAY+plgLabel+" Not in muted: "+name);
			}
			return true;
		} 
		else if (label.equals("muted")){
			if( !checkPerm(sender, "contextmanager.admin.cmd.muted")) return true;
			send(sender, ChatColor.YELLOW+plgLabel+" Muted: "+ChatColor.GRAY+join(muted.keySet(), ChatColor.DARK_GRAY+" | "+ChatColor.GRAY));
			return true;
		}
		return false;
	}
	
	/**
	 * Light checks to be performed now and then.
	 * (such as cleanup muted).
	 */
	void lightChecks(){
		checkMuted();
	}
	
	/**
	 * Remove expired from muted.
	 */
	void checkMuted(){
		List<String> rem = new LinkedList<String>();
		long ts = System.currentTimeMillis();
		for (String n : muted.keySet()){
			long nts = muted.get(n);
			if ( nts == 0 ) continue;
			else if (nts<ts) rem.add(n);
		}
		for ( String n : rem){
			muted.remove(n);
		}
	}

	public void loadSettings() {
		reloadConfig();
		Configuration cfg = getConfig();
		cfg.setDefaults(getDefaultSettings());
		cfg.options().copyDefaults(true);
		saveConfig(); // if ( !new File(getDataFolder(), "plugin.yml").exists()) 
		useEvent = cfg.getBoolean("chat.use-event");
		broadCastCol = Messaging.withChatColors(cfg.getString("chat.color.announce"));
		msgCol = Messaging.withChatColors(cfg.getString("chat.color.normal"));
		partyBracketCol = Messaging.withChatColors(cfg.getString("chat.color.party.brackets"));
		partyNameCol = Messaging.withChatColors(cfg.getString("chat.color.party.name"));
		partyMsgCol = Messaging.withChatColors(cfg.getString("chat.color.party.message"));
	}
	
	public MemoryConfiguration getDefaultSettings(){
		MemoryConfiguration cfg = new MemoryConfiguration();
		cfg.set("chat.use-event", true);
		cfg.set("chat.color.normal", "&f");
		cfg.set("chat.color.announce", "&e");
		cfg.set("chat.color.party.brackets", "&a");
		cfg.set("chat.color.party.name", "&7");
		cfg.set("chat.color.party.message", "&7");
//		List<String> load = new LinkedList<String>();
//		for ( String plg : new String[]{
//				"PermissionsEx", "mcMMO"
//		}){
//			load.add(plg);
//		}
		return cfg;
	}

	@EventHandler(priority=EventPriority.LOW)
	void onPlayerChat(PlayerChatEvent event){
		if (event.isCancelled()) return;
		String message = event.getMessage();
		Player player = event.getPlayer();
		
		String lcn = player.getName().toLowerCase();
		Long tsMute = muted.get(lcn);
		if ( tsMute !=null){
			if ( ( tsMute!=0L) && (System.currentTimeMillis() > muted.get(lcn))){
				muted.remove(lcn);
			}
			else if ( hasPermission(player, "contextmanager.bypass.mute")){
				send(player, ChatColor.YELLOW+plgLabel+" Removed you from muted (permission present).");
				muted.remove(lcn);
			}
			else if ( isGlobalChat(player) ){
				event.setCancelled(true);
				// TODO: add time descr.
				send(player, ChatColor.RED+plgLabel+" You are currently muted.");
				event.setCancelled(true);
				return;
			}
		}
		
		String msgCol = null; // this.msgCol;
		boolean forceBroadcast = false;
		if ( message.startsWith("!") && player.hasPermission("contextmanager.chat.announce")){
			msgCol = broadCastCol;
			int n = 1;
			if ( message.startsWith("!!")){
				forceBroadcast = true;
				n =2;
			}
			message = message.substring(n,message.length());
			if ( useEvent) event.setMessage(message);
		}
		Set<Player> recipients = event.getRecipients();
		if ( !forceBroadcast) adjustRecipients(player, recipients);
		
		// TODO: filters for who wants to hear (or must) and who should hear / forces to hear
		
		// assemble message
		String format;
		if (forceBroadcast) format = getNormalFormat(player.getName(), msgCol);
		else format = getFormat(player, msgCol);
		if (useEvent) event.setFormat(format);
		else {
			event.setCancelled(true);
			String sendMsg = format.replace("%1$s", player.getName()).replace("%2$s", message);
			if (forceBroadcast) getServer().broadcastMessage(sendMsg);
			else{
				for ( Player other : recipients){
					other.sendMessage(sendMsg);
				}
				System.out.println(sendMsg);
			}
		}
	}
	
	private boolean isPartyChat(Player player){
		try{
			if (com.gmail.nossr50.Users.getProfile(player).getPartyChatMode()) return true;
		} catch( Throwable t){
		}
		return false;
	}
	
	private boolean inSameParty( Player p1, Player p2){
		try{
			return com.gmail.nossr50.party.Party.getInstance().inSameParty(p1, p1);
		} catch( Throwable t){
			
		}
		return false;
	}
	
	private boolean isGlobalChat(Player player) {
		if ( isPartyChat(player)) return false;
		return true;
	}

	public final String getFormat(final Player player, final String msgCol){
		final String playerName = player.getName();
		// TODO: context dependent ...
		if ( isPartyChat(player)) return getPartyFormat(playerName, msgCol);
		else return getNormalFormat(playerName, msgCol);
	}
	
	public final String getNormalFormat(String playerName, String msgCol){
		if (msgCol == null) msgCol = this.msgCol;
		String[] decorated = PexUtil.findDecoration(playerName);
		if (decorated[0] == null) decorated[0] = "";
		else decorated[0] = Messaging.withChatColors(decorated[0]);
		if (decorated[1] == null) decorated[1] = "";
		else decorated[1] = Messaging.withChatColors(decorated[1]);
		return msgCol+"<"+decorated[0]+"%1$s"+decorated[1]+msgCol+"> %2$s";
	}
	
	public final String getPartyFormat(String playerName, String msgCol){
		if (msgCol == null) msgCol = partyMsgCol;
		return partyBracketCol+"("+partyNameCol+"%1$s"+partyBracketCol+")"+msgCol+" %2$s";
	}
	/**
	 * Adjust the list of recipients according to context.
	 * @param player
	 * @param recipients
	 */
	public final void adjustRecipients(Player player, Set<Player> recipients) {
		if ( isPartyChat(player)){
			List<Player> add = new LinkedList<Player>();
			for ( Player rec : recipients){ // TODO: get directly from mcMMO
				if (inSameParty(player, rec)) add.add(rec);
			}
			recipients.clear(); 
			recipients.addAll(add);
		}
	}
}
