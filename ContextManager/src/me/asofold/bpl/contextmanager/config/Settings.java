package me.asofold.bpl.contextmanager.config;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import me.asofold.bpl.contextmanager.core.CMCore;
import me.asofold.bpl.contextmanager.plshared.Messaging;

import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.MemoryConfiguration;


public class Settings {
	public String msgCol = ChatColor.WHITE.toString();
	public String partyNameCol = ChatColor.GRAY.toString();
	public String partyBracketCol = ChatColor.GREEN.toString();
	public String partyMsgCol = ChatColor.GRAY.toString();
	public String broadCastCol = ChatColor.YELLOW.toString();
	
	public Set<String> mutePreventCommands = new HashSet<String>();
	
	public ChannelSettings channels = new ChannelSettings();
	
	public int histSize = 100;
	
	public boolean shortcutAnnounce = true;
	
	public boolean shortcutTell = true;
	
	/**
	 * Can only be changed with restart / reload.
	 */
	public boolean mcMMOChat = true;
	public boolean ignoreCanSee = false;
	
	public Settings(){
		// Empty constructor.
	}
	
	public static MemoryConfiguration getDefaultSettings(){
		MemoryConfiguration cfg = new MemoryConfiguration();
		cfg.set("chat.color.normal", "&f");
		cfg.set("chat.color.announce", "&e");
		cfg.set("chat.color.party.brackets", "&a");
		cfg.set("chat.color.party.name", "&7");
		cfg.set("chat.color.party.message", "&7");
		cfg.set("chat.shortcut.announce", true);
		cfg.set("chat.shortcut.tell", true);
		cfg.set("mute.prevent-commands", new LinkedList<String>());
		cfg.set("contexts.channels.names", new LinkedList<String>());
		cfg.set("history.size", 100);
		cfg.set("channels.default-channel-name", "default");
		cfg.set("channels.fetch-delay", 2500L);
		cfg.set("events.mcmmo.party", true);
		cfg.set("ignore-cansee", false);
//		List<String> load = new LinkedList<String>();
//		for ( String plg : new String[]{
//				"PermissionsEx", "mcMMO"
//		}){
//			load.add(plg);
//		}
		return cfg;
	}
	
	public void applySettings(Configuration cfg, CMCore core) {
		broadCastCol = Messaging.withChatColors(cfg.getString("chat.color.announce"));
		msgCol = Messaging.withChatColors(cfg.getString("chat.color.normal"));
		partyBracketCol = Messaging.withChatColors(cfg.getString("chat.color.party.brackets"));
		partyNameCol = Messaging.withChatColors(cfg.getString("chat.color.party.name"));
		partyMsgCol = Messaging.withChatColors(cfg.getString("chat.color.party.message"));
		shortcutAnnounce = cfg.getBoolean("chat.shortcut.announce");
		shortcutTell = cfg.getBoolean("chat.shortcut.tell");
		mcMMOChat = cfg.getBoolean("events.mcmmo.chat", true);
		// other
		histSize = cfg.getInt("history.size");
		// channels
		channels.applyConfig(cfg, core);
		// mute / commands
		ignoreCanSee = cfg.getBoolean("ignore-cansee", false);
		mutePreventCommands.clear();
		List<String> cmds = cfg.getStringList("mute.prevent-commands");
		if (cmds!= null){
			for (String cmd : cmds){
				cmd = cmd.trim().toLowerCase();
				if (!cmd.isEmpty()) mutePreventCommands.add(cmd);
			}
		}
	}
	
	
}
