package me.asofold.bpl.contextmanager.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.asofold.bpl.contextmanager.ContextManager;
import me.asofold.bpl.contextmanager.chat.HistoryElement;
import me.asofold.bpl.contextmanager.command.CMCommand;
import me.asofold.bpl.contextmanager.config.Settings;
import me.asofold.bpl.contextmanager.hooks.ServiceHook;
import me.asofold.bpl.contextmanager.hooks.chestshop.ChestShopHook;
import me.asofold.bpl.contextmanager.hooks.regions.RegionsHook;
import me.asofold.bpl.contextmanager.listeners.mcMMOChatListener;
import me.asofold.bpl.contextmanager.plshared.Logging;
import me.asofold.bpl.contextmanager.plshared.Messaging;
import me.asofold.bpl.contextmanager.plshared.permissions.pex.PexUtil;
import me.asofold.bpl.contextmanager.util.Utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;


public class CMCore  implements Listener{
	
	private final Settings settings = new Settings();
	
	/** Player data (synchronized). */
	private final Map<String, PlayerData> playerData = Collections.synchronizedMap(new HashMap<String, PlayerData>());
	 
	/** Muted players (synchronized). */
    private final Map<String, Long> muted = Collections.synchronizedMap(new LinkedHashMap<String, Long>());
	
    /** History (synchronized). */
	private final List<HistoryElement> history = Collections.synchronizedList(new ArrayList<HistoryElement>());
	
	/** Map commands to service hooks. */
	private final Map<String, ServiceHook> serviceHookCommandMap = new HashMap<String, ServiceHook>();
	
	/** All registered service hooks. */
	private final Map<String, ServiceHook> registeredServiceHooks = new HashMap<String, ServiceHook>();
	
	public void addListeners(Plugin plugin){
		try{
			if (settings.mcMMOChat) Bukkit.getPluginManager().registerEvents(new mcMMOChatListener(this), plugin);
		}
		catch (Throwable t){}
	}
	
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
		synchronized(playerData){
			for (PlayerData data: playerData.values()){
				if (data.channel != null) data.setChannel(settings.channels.getAvailableChannel(data.channel));
			}
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
			String lcLabel = label.toLowerCase();
			serviceHookCommandMap.put(lcLabel, hook);
			String[] aliases = hook.getCommandLabelAliases(label);
			if (aliases != null){
				for (String alias : aliases){
					String lcAlias = alias.toLowerCase();
					if (!serviceHookCommandMap.containsKey(lcAlias)) serviceHookCommandMap.put(lcAlias, hook);
				}
			}
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
		for (String label : hook.getCommandLabels()){
			String lcLabel = label.toLowerCase();
			ServiceHook ref = serviceHookCommandMap.get(lcLabel);
			if (hook == ref) rem.add(lcLabel);
			for (String alias : hook.getCommandLabelAliases(label)){
				String lcAlias = alias.toLowerCase();
				ref = serviceHookCommandMap.get(lcAlias);
				if (hook == ref) rem.add(lcLabel);
			}
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
			addServiceHook(new ChestShopHook());
		} catch (Throwable t){}
		try{
			addServiceHook(new RegionsHook());
		}
		catch (Throwable t){}
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
		synchronized(muted){
			for (String n : getMuted().keySet()){
				long nts = getMuted().get(n);
				if ( nts == 0 ) continue;
				else if (nts<ts) rem.add(n);
			}
			for ( String n : rem){
				getMuted().remove(n);
			}
		}
	}


	
	/**
	 * Also messages the player or removes the timestamp from the map, if expired.
	 * @param player
	 * @return
	 */
	public boolean isMuted(Player player, boolean message) {
		String lcn = player.getName().toLowerCase();
		if (!muted.containsKey(lcn)) return false;
		boolean isMuted = false;
		synchronized (muted) {
			Long tsMute = muted.get(lcn);
			if ( tsMute !=null){
				if (( tsMute!=0L) && (System.currentTimeMillis() > tsMute)){
					muted.remove(lcn);
				}
				else if (getPlayerData(lcn).permMuted){
					// The message locks longer than wanted, but it is seldom.
					if (message) player.sendMessage(ChatColor.YELLOW+ContextManager.plgLabel+" Removed you from muted (permission present).");
					muted.remove(lcn);
				}
				else if (isGlobalChat(player) ){
					isMuted = true;
				}
			}
		}
		if (isMuted){
			// TODO: add time descr.
			if (message) player.sendMessage(ChatColor.RED+ContextManager.plgLabel+" You are currently muted.");
			return true;
		}
		else 
			return false;
	}

	public boolean isPartyChat(Player player){
		try{
			// TODO:
			com.gmail.nossr50.datatypes.PlayerProfile pp = com.gmail.nossr50.util.Users.getProfile(player);
			return pp.getPartyChatMode() && pp.getParty() != null;
//			return com.gmail.nossr50.mcMMO.p.getPlayerProfile(player).getPartyChatMode();
		}
		catch (Throwable t){
		}
		return false;
	}
	
//	public final boolean inSameParty(Player playera, Player playerb) {
//		final String name1 = playera.getName();
//		try{
//			final com.gmail.nossr50.datatypes.PlayerProfile pp1 =  com.gmail.nossr50.mcMMO.p.getPlayerProfile(name1);
//			if (pp1 == null) return false;
//			else if (!pp1.inParty()) return false;
//			final com.gmail.nossr50.party.Party party = pp1.getParty();
//			if (party == null) return false; // overly...
//			final String name2 = playerb.getName();
//			for (Player member : party.getOnlineMembers()){
//					if (member.getName().equals(name2)) return true;
//			}
//			return false;
//		} catch (Throwable t){
//			return false;
//		}
//	}
	
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
	 * @return Number of recipients (including seeing state), might be lower than recipients.size(), might or might not include player.
	 */
	public final int adjustRecipients(final Player player, final Set<Player> recipients, final boolean isAnnounce) {
		int n = 0;
		PlayerData data = getPlayerData(player.getName());
//		if ( !isAnnounce && isPartyChat(player)){
//			// TODO: these might be already set by mcmmo, on the other hand this allows for others.
//			List<Player> add = new LinkedList<Player>();
//			for ( Player rec : recipients){ // TODO: get directly from mcMMO
//				if (inSameParty(player, rec)){
//					add.add(rec);
//					n ++;
//				}
//			}
//			recipients.clear(); 
//			recipients.addAll(add);
//		}
//		else{
		List<Player> rem = new LinkedList<Player>();
		for (Player other : recipients){
			PlayerData otherData = getPlayerData(other.getName());
			if (!otherData.canHear(data, isAnnounce)) rem.add(other);
			else if (!player.canSee(other)){
				// depends now on the settings:
				if (otherData.recipients.contains(data.lcName)) n++;
				else if (settings.ignoreCanSee) n ++;
				// else keep recipient but don't count.
			}
			else{
				n ++;
			}
		}
		if (!rem.isEmpty()) recipients.removeAll(rem);
//		}
		return n;
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
	
	public String[] getChannelNames(){
		return settings.channels.getChannelNames();
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
		
		if (cmd.equals("me") || settings.mutePreventCommands.contains(cmd)){
			if (isMuted(player, true)){
				event.setCancelled(true);
				return;
			}
		}
	}
	
	/**
	 * Schedule the command with PlayerCommandPreProcess event.
	 * @param player
	 * @param cmd
	 */
	private void scheduleCommand(final Player player, final String cmd) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
			@Override
			public void run() {
				if (!player.isOnline()) return;
				final PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/" + cmd);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled()) return;
				Bukkit.getServer().dispatchCommand(player, cmd);
			}
		});
	}
	
	@EventHandler(priority=EventPriority.LOW)
	final void onPlayerChat(final AsyncPlayerChatEvent event){
		if (event.isCancelled()) return;
		final String message = event.getMessage();
		final Player player = event.getPlayer();
		
		// Tell shortcut
		if (settings.shortcutTell && message.startsWith("@")){
			// TODO: allow for multicast ? // TODO: tell handling alike ?
			// tell shortcut
			event.setCancelled(true);
			final String cmd;
			if (message.startsWith("@ ")) cmd = "tellplayer "+message.substring(2);
			else cmd = "tellplayer "+message.substring(1);
			scheduleCommand(player, cmd);
			return;
		}
		
		final String playerName = player.getName();
		final PlayerData data = getPlayerData(playerName);
		
		// Announcements
		if (settings.shortcutAnnounce && data.permAnnounce && checkAnnounce(playerName, message)){
			event.setCancelled(true);
			return;
		}
		
		// Ignore party chat for the moment: 
		if (isPartyChat(player)) return;
		
		
		// Ignore muted players:
		if (isMuted(player, true)){
			event.setCancelled(true);
			return;
		}
		
		// (Only normal chat should arrive here.)
		
		// recipients
		final Set<Player> recipients = event.getRecipients();;
		int n = adjustRecipients(player, recipients, false);
		if (n == 0 || n==1 && recipients.contains(player)){
			// TODO: might have to schedule the message !
			player.sendMessage(settings.msgAlone);
		}
		
		// Assemble message
		event.setFormat(data.normalFormat);
		
		// History:
		final ContextType type;
		if (!data.recipients.isEmpty()) type = ContextType.PRIVATE;
		else if (data.channel == null) type = ContextType.DEFAULT;
		else type = ContextType.CHANNEL;
		addToHistory(new HistoryElement(type, playerName, data.getExtraFormat(), message, false));
		// TODO: History probably should be done on monitor, somehow.
	}
	
	/**
	 * Check if a message is an announcement, schedule the command for it.<br>
	 * No pre permission or config flag check (must be done elsewhere!).  
	 * @param player
	 * @param message
	 * @return
	 */
	public final boolean checkAnnounce(final String playerName, final String message) {
		if (!message.startsWith("!")) return false;
		final int n;
		final boolean global;
		if ( message.startsWith("!!")){
			global = true;
			n = 2;
		}
		else{
			global = false;
			n = 1;
		}
		scheduleAnnounce(playerName, message.substring(n,message.length()), global);
		return true;
	}
	
	/**
	 * Check if a message is an announcement
	 * @param playerName
	 * @return true if it is an annoucnement (cancel event then)
	 */
	public boolean checkPartyAnnounce(final String playerName, final String message) {
		final PlayerData data = getPlayerData(playerName, false); 
		if (data == null) return false;
		return settings.shortcutAnnounce && data.permAnnounce && checkAnnounce(playerName, message);
	}

	private final void scheduleAnnounce(final String playerName, final String message, final boolean global){
		// TODO: issue command.
		Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
			@Override
			public void run() {
				final Player player = Bukkit.getPlayerExact(playerName);
				if (player == null) return;
				if (!player.isOnline()) return;
				final String cmd = (global?"tellall ":"tellchannel ") + message;
				final PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/" + cmd);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled()) return;
				Bukkit.getServer().dispatchCommand(player, (global?"tellall ":"tellchannel ") + message);
			}
		});
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
		Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			@Override
			public void run() {
				updatePlayerInfos();
			}
		}, 117, 117);
		for (ServiceHook hook : registeredServiceHooks.values()){
			try{
				hook.onEnable(plugin);
			} 
			catch (Throwable t){
				Logging.warn("[ContextManager] ServiceHook.onEnable ("+hook.getHookName()+"):",t);
			}
		}
	}

	public void onDisable() {
		for (ServiceHook hook : registeredServiceHooks.values()){
			try{
				hook.onDisable();
			} 
			catch (Throwable t){
				Logging.warn("[ContextManager] ServiceHook.onDisable ("+hook.getHookName()+"):",t);
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

	public void onContextFind(CommandSender sender, String[] args) {
		for (ServiceHook hook : registeredServiceHooks.values()){
			if (hook.delegateFind(sender, args)) return;
		}
		sender.sendMessage(ChatColor.RED + "[ContextManager] Nothing found.");
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	final void onPlayerJoin(PlayerJoinEvent event){
		updatePlayerInfos(event.getPlayer());
	}
	
	final void onPlayerChangedWorld(PlayerChangedWorldEvent event){
		updatePlayerInfos(event.getPlayer());
	}

	/**
	 * Update infos about one player.
	 * @param player
	 */
	private final void updatePlayerInfos(final Player player) {
		final String playerName = player.getName();
		final PlayerData data = getPlayerData(playerName, true);
		
		// Set permissions:
		data.permAnnounce = player.hasPermission("contextmanager.chat.announce");
		data.permMuted = Utils.hasPermission(player, "contextmanager.bypass.mute");
		
		// Set formats:
		data.normalFormat = getNormalFormat(playerName, settings.msgCol, false);
		data.announceFormat = getNormalFormat(playerName, settings.broadCastCol, true);
		data.partyFormat = getPartyFormat(playerName, settings.partyMsgCol);
	}
	
	/**
	 * Check all online players infos.
	 */
	private final void updatePlayerInfos() {
		for  (final Player player : Bukkit.getOnlinePlayers()){
			// TODO: Something faster for bigger servers ?
			//       (Might be every 1, 2 ticks one name , once through check min delay, fill set of names again)
			updatePlayerInfos(player);
		}
	}
	
	/**
	 * Called in the main thread.
	 * @param player
	 * @param split Split words as with a tell command split by spaces, first element is the command label (ignored).
	 */
	private void processTell(Player player, String[] split) {
		if (split.length <= 1) return;
		// if (!player.isOnline()) return; // TODO: necessary or not ?
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
		if (other != null && !settings.ignoreCanSee && !player.canSee(other) && !Utils.hasPermission(player, "contextmanager.bypass.tell")){
			if (otherData == null || !otherData.recipients.contains(lcName)) other = null;
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


	/**
	 * Args excludes the command label.
	 * @param player
	 * @param args
	 */
	public void onTell(Player player, String[] args) {
		// TODO improve this.
		processTell(player, CMCommand.inflate(args, "tellplayer"));
	}

	public void onAnnounce(Player player, String[] args, boolean global) {
		String playerName = player.getName();
		final PlayerData data = getPlayerData(playerName);
		ContextType type;
		if (data.channel == null) type = ContextType.DEFAULT;
		else type = ContextType.CHANNEL;
		String format = data.announceFormat;
		String sendMsg = format.replace("%1$s", playerName).replace("%2$s", Utils.join(Arrays.asList(args), " "));
		if (global) Bukkit.getServer().broadcastMessage(sendMsg);
		else{
			final Player[] players = Bukkit.getOnlinePlayers();
			final Set<Player> recipients = new HashSet<Player>(players.length);
			for (int i = 0; i < players.length; i ++){
				recipients.add(players[i]);
			}
			adjustRecipients(player, recipients, true);
			for ( Player other : recipients){
				other.sendMessage(sendMsg);
			}
			System.out.println(sendMsg);
		}
		addToHistory(new HistoryElement(type, playerName, null, sendMsg, true));
	}



}
