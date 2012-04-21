package me.asofold.bukkit.contextmanager;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.ChatColor;

public class PlayerData {
	/**
	 * Lower case name.
	 */
	String lcName;
	
	public PlayerData(String lcName){
		this.lcName = lcName;
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
	}

	public void resetChannel() {
		if (channel != null) ContextManager.tryMessage(lcName, ChatColor.DARK_GRAY+ContextManager.plgLabel+" Leaving channel: "+channel);
		channel = null;
	}
	
	/**
	 * Add the recipients, i.e. check if +- is used in front !
	 * @param args
	 * @param startIndex
	 */
	public void addRecipients(String[] args, int startIndex) {
		// TODO Auto-generated method stub
		
	}
	
	
	/**
	 * 
	 */
	public void addIgnore(){
		
	}
	
	/**
	 * Shortcut method to check if the other player is heard by this one.<br>
	 * NOTE: this does not check broadcast and party chat.
	 * @param other
	 * @return
	 */
	public final boolean canHear(final PlayerData other){
		// TODO: maybe make more efficient ways ...
		if (ignored.contains(other.lcName)) return false; // allow to ignore oneself.
		if (other == this) return true; // important check.
		if (!other.recipients.isEmpty() && !other.recipients.contains(lcName)) return false;
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
		// Get format string suffix for channel / recipients
		// TODO
		return "";
	}

	
	
}
