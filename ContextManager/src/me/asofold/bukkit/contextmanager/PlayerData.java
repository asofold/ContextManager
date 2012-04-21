package me.asofold.bukkit.contextmanager;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.ChatColor;

public class PlayerData {
	
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
	 * Shortcut method to check if the other player is hard by this one.<br>
	 * NOTE: this does not check broadcast and party chat.
	 * @param other
	 * @return
	 */
	public boolean canHear(PlayerData other){
		// TODO
		return true;
	}
	
	public String getExtraFormat(){
		// Get format string suffix for channel / recipients
		// TODO
		return "";
	}

	
	
}
