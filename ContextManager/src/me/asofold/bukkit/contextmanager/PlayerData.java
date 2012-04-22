package me.asofold.bukkit.contextmanager;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.ChatColor;

public class PlayerData {
	/**
	 * Lower case name.
	 */
	String lcName;
	
	String extraFormat = "";
	
	public PlayerData(String lcName){
		this.lcName = lcName;
		setExtraFormat();
	}
	
	/**
	 * simplistic by now.
	 */
	public String channel = null;
	
	public Set<String> recipients = new HashSet<String>();
	
//	/**
//	 * 
//	 */
//	public boolean muted = false;
	
	/**
	 * Players to ignore.
	 */
	public Set<String> ignored = new HashSet<String>();
	

	public void resetAll(){
		resetContexts();
		resetIgnored(); //
	}

	public void resetContexts() {
		resetChannel();
		resetRecipients();
	}
	
	public void resetIgnored() {
		ignored.clear();
	}

	public void resetRecipients(){
		if (!recipients.isEmpty()) ContextManager.tryMessage(lcName, ChatColor.DARK_GRAY+ContextManager.plgLabel+" Resetting recipients.");
		recipients.clear();
		setExtraFormat();
	}

	public void resetChannel() {
		if (channel != null) ContextManager.tryMessage(lcName, ChatColor.DARK_GRAY+ContextManager.plgLabel+" Leaving channel: "+channel);
		channel = null;
		setExtraFormat();
	}
	
	/**
	 * Add the recipients, i.e. check if +- is used in front !
	 * @param args
	 * @param startIndex
	 */
	public void addRecipients(String[] args, int startIndex) {
		// TODO Auto-generated method stub
		// use -* to remove all
		for (int i = startIndex; i<args.length; i++){
			String n = args[i].trim().toLowerCase();
			if (n.isEmpty()) continue;
			if (n.equals("-*")){
				recipients.clear();
			}
			else if (n.startsWith("-")) recipients.remove(n.substring(1));
			else recipients.add(n);
		}
		setExtraFormat(); // also resets format
	}
	
	
	public void setChannel(String channel){
		this.channel = channel;
		setExtraFormat(); // also resets format.
	}
	

	
	/**
	 * Shortcut method to check if the other player is heard by this one.<br>
	 * NOTE: this does not check broadcast and party chat.
	 * @param other
	 * @param isAnnounce 
	 * @return
	 */
	public final boolean canHear(final PlayerData other, boolean isAnnounce){
		// TODO: maybe make more efficient ways ...
		if (!isAnnounce && ignored.contains(other.lcName)) return false; // allow to ignore oneself.
		if (other == this) return true; // important check.
		if (!isAnnounce){
			boolean orne = !other.recipients.isEmpty(); 
			boolean rne = !recipients.isEmpty();
			if (orne && !other.recipients.contains(lcName)) return false;
			if (rne && !recipients.contains(other.lcName)) return false;
			if (orne && rne) return true; // always allow to hear
		}
		if (channel == null){
			if (other.channel != null) return false;
		} else{
			if (other.channel == null) return false;
			else if (!channel.equals(other.channel)) return false;
		}
		// TODO: more aspects.
		return true;
	}
	
	public String getExtraFormat(){
		if (!recipients.isEmpty()) return ChatColor.DARK_GRAY + "@"+ ChatColor.DARK_PURPLE + ContextManager.join(recipients, ",");
		if (channel!=null) return ChatColor.DARK_GRAY + "@" + channel;
		if (!ContextManager.defaultChannelName.isEmpty()) return ChatColor.DARK_GRAY + "@"+ContextManager.defaultChannelName;
		else return "";
	}

	public void setExtraFormat() {
		extraFormat = getExtraFormat();
	}
	
}
