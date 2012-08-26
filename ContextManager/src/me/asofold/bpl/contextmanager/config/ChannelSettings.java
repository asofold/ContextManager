package me.asofold.bpl.contextmanager.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.asofold.bpl.contextmanager.core.CMCore;
import me.asofold.bpl.contextmanager.core.PlayerData;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;

/**
 * Just to split off the default channel handling.
 * @author mc_dev
 *
 */
public class ChannelSettings {
	
	/**
	 * TODO: Bit of a hack design.
	 */
	private static String defaultChannelName = "global";
	
	/**
	 * Lower case -> correct case.
	 */
	private final Map<String, String> channels = new LinkedHashMap<String, String>();
	
	private final ArrayList<String> channelsOrdered = new ArrayList<String>();
	
	private long delayChannelsString = 2500;
	
	private long tsSetChannelsString = 0;
	
	String[] channelsString = new String[]{ChatColor.YELLOW + "[ContextManager] Available channels: ", ChatColor.GRAY+"(0)" + ChatColor.YELLOW + getDefaultChannelDisplayName()};

	public ChannelSettings(){
		channelsOrdered.add(getDefaultChannelName());
	}
	
	public String[] getChannesString(CMCore core) {
		if (System.currentTimeMillis()- tsSetChannelsString > delayChannelsString){
			setChannelsString(core);
		}
		return channelsString;
	}
	
	public void setChannelsString(CMCore core){
		tsSetChannelsString = System.currentTimeMillis();
		// TODO: add number of users + one line per channel
		// TODO: generate on load settings.
		
		Map<String, Integer> userCount = getChannelUserCount(core);
		
		String[] out = new String[channelsOrdered.size()+1];
		out[0] = ChatColor.YELLOW + "[ContextManager" +
				"] Available channels: ";
		out[1] = getChannelListEntry(0, getDefaultChannelDisplayName(), userCount.get(ChannelSettings.getDefaultChannelName()));
		for (int i = 1; i< channelsOrdered.size(); i++){
			String ch = channelsOrdered.get(i);
			out[i+1] = getChannelListEntry(i, ch, userCount.get(ch));
		}
		channelsString =  out;
	}
	
	public  Map<String, Integer> getChannelUserCount(CMCore core) {
		Map<String, Integer> counts = new HashMap<String, Integer>();
		for ( Player player : Bukkit.getServer().getOnlinePlayers()){
			PlayerData data = core.getPlayerData(player.getName());
			String ch;
			if (!data.recipients.isEmpty()) continue; // regard as if not there
			if (data.channel == null) ch = ChannelSettings.getDefaultChannelName();
			else ch = data.channel;
			Integer c = counts.get(ch);
			if ( c == null){
				counts.put(ch, 1);
			}
			else counts.put(ch,  c + 1);
		}
		return counts;
	}

	public String getChannelListEntry(int n, String name, Integer users){
		if (users == null) users = 0;
		String out = ChatColor.GRAY+"("+n+") "+ChatColor.YELLOW+name;
		if (users == 0) return out;
		out += ChatColor.GRAY+" - "+users+ " player";
		if (users>1) out += "s";
		return out;
	}
		

	public String getDefaultChannelDisplayName() {
		if (ChannelSettings.getDefaultChannelName().isEmpty()) return "default";
		else return ChannelSettings.getDefaultChannelName();
	}
	
	public String getAvailableChannel(String name) {
		if (name == null) return null;
		String chan = channels.get(name.trim().toLowerCase());
		if (chan == null){
			try{
				int i = Integer.parseInt(name);
				if (i>=0 && i<channelsOrdered.size()){
					if (i == 0) return ChannelSettings.getDefaultChannelName();
					chan = channelsOrdered.get(i);
				}
			} catch (NumberFormatException e){		
			}
		}
		if (name.equalsIgnoreCase("global") || (name.equalsIgnoreCase("default") || name.equalsIgnoreCase(ChannelSettings.getDefaultChannelName()))) return ChannelSettings.getDefaultChannelName();
		else return chan;
	}

	public void applyConfig(Configuration cfg, CMCore core) {
		ChannelSettings.setDefaultChannelName(cfg.getString("channels.default-channel-name", "default").trim());
	    channels.clear();
		channelsOrdered.clear();
		channelsOrdered.add(ChannelSettings.getDefaultChannelName());
		List<String> ch = cfg.getStringList("contexts.channels.names");
		if (ch != null){
			for (String c : ch){
				c = c.trim();
				channels.put(c.trim().toLowerCase(), c);
				channelsOrdered.add(c);
			}
		}
		delayChannelsString = cfg.getLong("channels.fetch-delay", 2500L);
		setChannelsString(core);
	}

	public static String getDefaultChannelName() {
		return defaultChannelName;
	}

	public static void setDefaultChannelName(String defaultChannelName) {
		ChannelSettings.defaultChannelName = defaultChannelName;
	}
}
