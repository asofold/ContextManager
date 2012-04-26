package me.asofold.bukkit.contextmanager.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.asofold.bukkit.contextmanager.ContextManager;
import me.asofold.bukkit.contextmanager.chat.HistoryElement;
import me.asofold.bukkit.contextmanager.config.Settings;
import me.asofold.bukkit.contextmanager.hooks.ServiceHook;
import me.asofold.bukkit.contextmanager.hooks.chestshop.ChestShopHook;
import me.asofold.bukkit.contextmanager.util.Utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import asofold.pluginlib.shared.Messaging;
import asofold.pluginlib.shared.permissions.pex.PexUtil;

public class CMCore  implements Listener{
	
	Settings settings = new Settings();
	
	 private Map<String, PlayerData> playerData = new HashMap<String, PlayerData>();
	 
	/**
	 * muted players
	 */
    private Map<String, Long> muted = new LinkedHashMap<String, Long>();
	
	private List<HistoryElement> history = new ArrayList<HistoryElement>();
	
	private Map<String, ServiceHook> serviceHookCommandMap = new HashMap<String, ServiceHook>();
	
	private Map<String, ServiceHook> registeredServiceHooks = new HashMap<String, ServiceHook>();
	
	
	public void loadSettings() {
		ContextManager cm = getPlugin();
		cm.reloadConfig();
		Configuration cfg = cm.getConfig();
		cfg.setDefaults(Settings.getDefaultSettings());
		cfg.options().copyDefaults(true);
		cm.saveConfig(); // if ( !new File(getDataFolder(), "plugin.yml").exists()) 
		settings.applySettings(cfg, this);
		checkPlayerChannels();
	}
	
	private void checkPlayerChannels() {
		for (PlayerData data: playerData.values()){
			if (data.channel != null) data.setChannel(settings.channels.getAvailableChannel(data.channel));
		}
	}

	public static ContextManager getPlugin() {
		// TODO: maybe store it.
		return (ContextManager) Bukkit.getServer().getPluginManager().getPlugin("ContextManager");
	}

	/**
	 * Obtain player data.
	 * @param playerName
	 * @param create
	 * @return
	 */
	public PlayerData getPlayerData(String playerName, boolean create){
		String lcName = playerName.toLowerCase(); 
		PlayerData data = playerData.get(lcName);
		if (data != null) return data;
		else if (!create) return null;
		else{
			data = new PlayerData(lcName);
			playerData.put(lcName, data);
			return data;
		}
	}
	
	/**
	 * Be sure to call only once per onEnable call if using a Listener (uses this plugin to register).
	 * @param hook
	 */
	public void addServiceHook(ServiceHook hook){
		String name = hook.getHookName();
		removeServiceHook(name);
		registeredServiceHooks.put(name, hook);
		for (String label : hook.getCommandLabels()){
			serviceHookCommandMap.put(label.toLowerCase(), hook);
		}
		Listener listener = hook.getListener();
		if (listener != null) Bukkit.getPluginManager().registerEvents(listener, getPlugin()); 
		hook.onAdd();
		System.out.println("[ContextManager] Added ServiceHook : "+hook.getHookName());
	}
	
	public boolean removeServiceHook(String name) {
		if (!registeredServiceHooks.containsKey(name)) return false;
		ServiceHook hook = registeredServiceHooks.remove(name);
		// also remove command mappings:
		List<String> rem = new LinkedList<String>();
		for (String cmd : hook.getCommandLabels()){
			String key = cmd.toLowerCase();
			ServiceHook ref = serviceHookCommandMap.get(key);
			if (hook == ref) rem.add(key);
		}
		for (String key : rem){
			serviceHookCommandMap.remove(key);
		}
		hook.onRemove();
		System.out.println("[ContextManager] Removed ServiceHook : "+hook.getHookName());
		return true;
	}
	
	public ServiceHook getServiceHook(String name){
		return registeredServiceHooks.get(name);
	}

	public void addStandardServiceHooks(){
		try{
			ServiceHook hook = new ChestShopHook();
			addServiceHook(hook);
		} catch (Throwable t){
			// TODO: log ?
		}
	}
	
	/**
	 * Obtain PlayerData, create if not existent.
	 * @param playerName
	 * @return
	 */
	public PlayerData getPlayerData(String playerName){
		return getPlayerData(playerName, true);
	}
	
	/**
	 * Light checks to be performed now and then.
	 * (such as cleanup muted).
	 */
	public void lightChecks(){
		checkMuted();
	}
	
	/**
	 * Remove expired from muted.
	 */
	void checkMuted(){
		List<String> rem = new LinkedList<String>();
		long ts = System.currentTimeMillis();
		for (String n : getMuted().keySet()){
			long nts = getMuted().get(n);
			if ( nts == 0 ) continue;
			else if (nts<ts) rem.add(n);
		}
		for ( String n : rem){
			getMuted().remove(n);
		}
	}


	
	/**
	 * Also messages the player or removes the timestamp from the map, if expired.
	 * @param player
	 * @return
	 */
	public boolean isMuted(Player player) {
		String lcn = player.getName().toLowerCase();
		Long tsMute = getMuted().get(lcn);
		if ( tsMute !=null){
			if ( ( tsMute!=0L) && (System.currentTimeMillis() > getMuted().get(lcn))){
				getMuted().remove(lcn);
			}
			else if ( Utils.hasPermission(player, "contextmanager.bypass.mute")){
				Utils.send(player, ChatColor.YELLOW+ContextManager.plgLabel+" Removed you from muted (permission present).");
				getMuted().remove(lcn);
			}
			else if ( isGlobalChat(player) ){
				// TODO: add time descr.
				Utils.send(player, ChatColor.RED+ContextManager.plgLabel+" You are currently muted.");
				return true;
			}
		}
		return false;
	}

	public boolean isPartyChat(Player player){
		try{
			if (com.gmail.nossr50.Users.getProfile(player).getPartyChatMode()) return true;
		} catch( Throwable t){
		}
		return false;
	}
	
	public boolean inSameParty( Player p1, Player p2){
		try{
			return com.gmail.nossr50.party.Party.getInstance().inSameParty(p1, p1);
		} catch( Throwable t){
			
		}
		return false;
	}
	
	public boolean isGlobalChat(Player player) {
		if (isPartyChat(player)) return false;
		return true;
	}

	public final String getFormat(final Player player, final String msgCol, boolean isAnnounce){
		final String playerName = player.getName();
		// TODO: context dependent ...
		if ( !isAnnounce && isPartyChat(player)) return getPartyFormat(playerName, msgCol);
		else return getNormalFormat(playerName, msgCol, isAnnounce);
	}
	
	public final String getNormalFormat(String playerName, String msgCol, boolean isAnnounce){
		if (msgCol == null) msgCol = settings.msgCol;
		String[] decorated = PexUtil.findDecoration(playerName);
		if (decorated[0] == null) decorated[0] = "";
		else decorated[0] = Messaging.withChatColors(decorated[0]);
		if (decorated[1] == null) decorated[1] = "";
		else decorated[1] = Messaging.withChatColors(decorated[1]);
		return msgCol+"<"+decorated[0]+"%1$s"+decorated[1]+msgCol+">"+(isAnnounce?"":getPlayerData(playerName).getExtraFormat())+msgCol+" %2$s";
	}
	
	public final String getPartyFormat(String playerName, String msgCol){
		if (msgCol == null) msgCol = settings.partyMsgCol;
		return settings.partyBracketCol+"("+settings.partyNameCol+"%1$s"+settings.partyBracketCol+")"+msgCol+" %2$s";
	}
	/**
	 * Adjust the list of recipients according to context.
	 * @param player
	 * @param recipients
	 */
	public final void adjustRecipients(Player player, Set<Player> recipients, boolean isAnnounce) {
		PlayerData data = getPlayerData(player.getName());
		if ( !isAnnounce && isPartyChat(player)){
			// TODO: these might be already set by mcmmo, on the other hand this allows for others.
			List<Player> add = new LinkedList<Player>();
			for ( Player rec : recipients){ // TODO: get directly from mcMMO
				if (inSameParty(player, rec)) add.add(rec);
			}
			recipients.clear(); 
			recipients.addAll(add);
		}
		else{
			List<Player> rem = new LinkedList<Player>();
			for (Player other : recipients){
				PlayerData otherData = getPlayerData(other.getName());
				if (!otherData.canHear(data, isAnnounce)) rem.add(other);
			}
			if (!rem.isEmpty()) recipients.removeAll(rem);
		}
	}
	
	public void addToHistory(HistoryElement element){
		history.add(element);
		while (history.size() > settings.histSize && !history.isEmpty()) history.remove(0);
	}
	
	public Map<String, Long> getMuted() {
		return muted;
	}

	public List<HistoryElement> getHistory() {
		return history;
	}
	
	public String getDefaultChannelDisplayName() {
		return settings.channels.getDefaultChannelDisplayName();
	}
	
	public String[] getChannesString() {
		return settings.channels.getChannesString(this);
	}
	
	public String getAvailableChannel(String name) {
		return settings.channels.getAvailableChannel(name);
	}
	
	// LISTENER STUFF -------------------------------------------------------------
	
	@EventHandler(priority=EventPriority.HIGHEST)
	void onPlayerCommand(PlayerCommandPreprocessEvent event){
		if (event.isCancelled()) return;
		final String msg = event.getMessage().trim();
		final String[] split = msg.split(" ");
		String cmd = split[0].toLowerCase();
		if (cmd.length()>1) cmd = cmd.substring(1);
		// TODO: check set of others ?
		final Player player = event.getPlayer();
		if (cmd.equals("tell")){
			// TODO: maybe log and maybe message player self.
			event.setCancelled(true);
			if (split.length > 1){
				final String playerName =  player.getName(); 
				String lcName = playerName.toLowerCase();
				String recipient = split[1].trim().toLowerCase();
				if (lcName.equals(recipient)) return;
				if (recipient.isEmpty()){
					// TODO: maybe better command parsing (stripping of space only parts).
					player.sendMessage(ChatColor.DARK_RED+ContextManager.plgLabel+" Bad tell message.");
					return;
				}
				PlayerData otherData = getPlayerData(recipient, false);
				if (otherData != null && otherData.ignored.contains(lcName) && !Utils.hasPermission(player, "contextmanager.bypass.tell")){
					player.sendMessage(ChatColor.DARK_RED+ContextManager.plgLabel+" You are ignored by this player.");
					return;
				}
				Player other = Bukkit.getServer().getPlayerExact(recipient);
				if (other != null && !player.canSee(other) && !Utils.hasPermission(player, "contextmanager.bypass.tell")){
					if (!otherData.recipients.contains(lcName)) other = null;
				}
				if (other != null){
					final String detail = "->"+other.getName();
					final String tellMsg = Utils.join(Utils.getCollection(split, 2), " ");
					final String sendMsg = "(" + playerName + detail + ") " + tellMsg;
					System.out.println("[Tell]" + sendMsg);
					player.sendMessage(ChatColor.DARK_GRAY + sendMsg);
					other.sendMessage(ChatColor.GRAY+ sendMsg);
					addToHistory(new HistoryElement(ContextType.PRIVATE, playerName, detail, tellMsg, false));
				}
				else{
					player.sendMessage(ChatColor.DARK_RED+"[Tell] "+recipient+" is not available.");
				}
				return; // TODO: consider cancelling tell always
			}
		}
		if (!cmd.equals("me") && !settings.mutePreventCommands.contains(cmd)) return;
		// TODO: maybe find the fastest way to do this !
		if (isMuted(player)) event.setCancelled(true);
	}
	
	@EventHandler(priority=EventPriority.LOW)
	void onPlayerChat(PlayerChatEvent event){
		if (event.isCancelled()) return;
		String message = event.getMessage();
		Player player = event.getPlayer();
		
		// Tell shortcut
		if (message.startsWith("@")){
			// TODO: allow for multicast ? // TODO: tell handling alike ?
			// tell shortcut
			event.setCancelled(true);
			final String cmd;
			if (message.startsWith("@ ")) cmd = "/tell "+message.substring(2);
			else cmd = "/tell "+message.substring(1);
			final PlayerCommandPreprocessEvent tellEvent = new PlayerCommandPreprocessEvent(player, cmd);
			Bukkit.getServer().getPluginManager().callEvent(tellEvent); // will be replaced by THIS plugin.
			return;
		}
		
		
		// Muted
		if (isMuted(player)){
			event.setCancelled(true);
			return;
		}
		
		// Announcements
		String msgCol = null; // this.msgCol;
		boolean forceBroadcast = false;
		boolean isAnnounce = false;
		if ( message.startsWith("!") && player.hasPermission("contextmanager.chat.announce")){
			isAnnounce = true;
			msgCol = settings.broadCastCol;
			int n = 1;
			if ( message.startsWith("!!")){
				forceBroadcast = true;
				n =2;
			}
			message = message.substring(n,message.length());
			if ( settings.useEvent) event.setMessage(message);
		}
		
		// History:
		final String playerName = player.getName();
		PlayerData data = getPlayerData(playerName);
		boolean isParty = isPartyChat(player);
		ContextType type;
		if (forceBroadcast) type = ContextType.BROADCAST;
		else if (isAnnounce){
			if (data.channel == null) type = ContextType.DEFAULT;
			else type = ContextType.CHANNEL;
		}
		else if (isParty) type = ContextType.PARTY;
		else if (!data.recipients.isEmpty()) type = ContextType.PRIVATE;
		else if (data.channel == null) type = ContextType.DEFAULT;
		else type = ContextType.CHANNEL;
		addToHistory(new HistoryElement(type, playerName, data.getExtraFormat(), message, isAnnounce));
		
		// recipients
		Set<Player> recipients;
		boolean useEvent = settings.useEvent;
		if (isAnnounce){
			// force recipients to prevent other plugins messing around.
			recipients = new HashSet<Player>();
			for (Player other :  Bukkit.getServer().getOnlinePlayers()){
				recipients.add(other);
			}
			useEvent = false;
		}
		else recipients = event.getRecipients();
		if ( !forceBroadcast){
			adjustRecipients(player, recipients, isAnnounce);
			if (recipients.size() == 0 || recipients.size()==1 && recipients.contains(player)) player.sendMessage(ChatColor.RED+"[Chat] There are no players that can hear your message!");
		}
		
		// TODO: filters for who wants to hear (or must) and who should hear / forces to hear
		
		// Assemble message
		String format;
		if (isAnnounce) format = getNormalFormat(playerName, msgCol, isAnnounce);
		else format = getFormat(player, msgCol, isAnnounce);
		if (useEvent) event.setFormat(format);
		else {
			event.setCancelled(true);
			String sendMsg = format.replace("%1$s", playerName).replace("%2$s", message);
			if (forceBroadcast) Bukkit.getServer().broadcastMessage(sendMsg);
			else{
				for ( Player other : recipients){
					other.sendMessage(sendMsg);
				}
				System.out.println(sendMsg);
			}
		}
	}

	/**
	 * Check the map for the labels
	 * @param sender
	 * @param args length >= 1
	 * @return If used by a hook.
	 */
	public boolean checkServiceHookCommand(CommandSender sender, String[] args){
		if (args.length == 0) return false;
		final String lcLabel = args[0].toLowerCase();
		ServiceHook hook = serviceHookCommandMap.get(lcLabel);
		if (hook == null) return false;
		String[] newArgs = new String[args.length -1];
		for (int i = 1; i<args.length; i++){
			newArgs[i-1] = args[i];
		}
		try{
			hook.onCommand(sender, lcLabel, newArgs);
		} catch (Throwable t){
			Bukkit.getLogger().warning("[ContextManager] Hook failed for command '"+lcLabel+"':");
			t.printStackTrace(); // TODO log on warning
		}
		return true;
	}

	public void onEnable(ContextManager plugin) {
		for (ServiceHook hook : registeredServiceHooks.values()){
			try{
				hook.onEnable(plugin);
			} 
			catch (Throwable t){
				asofold.pluginlib.shared.Logging.warn("[ContextManager] ServiceHook.onEnable ("+hook.getHookName()+"):",t);
			}
		}
	}

	public void onDisable() {
		for (ServiceHook hook : registeredServiceHooks.values()){
			try{
				hook.onDisable();
			} 
			catch (Throwable t){
				asofold.pluginlib.shared.Logging.warn("[ContextManager] ServiceHook.onDisable ("+hook.getHookName()+"):",t);
			}
		}
	}

	/**
	 * List of ServiceHooks.
	 * @return
	 */
	public String getServicesStr() {
		String out = ChatColor.GRAY+"[Context] Services:";
		for (ServiceHook hook : registeredServiceHooks.values()){
			out += " | " + hook.getHookName();
			String[] cmds = hook.getCommandLabels();
			if (cmds.length > 0) out += "("+ChatColor.YELLOW+cmds[0]+ChatColor.GRAY+")";
		}
		out += " |";
		return out;
	}

}
