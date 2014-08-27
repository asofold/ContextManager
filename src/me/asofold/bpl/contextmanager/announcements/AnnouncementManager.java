package me.asofold.bpl.contextmanager.announcements;

import java.util.HashMap;
import java.util.Map;

import me.asofold.bpl.contextmanager.config.compatlayer.CompatConfig;

import org.bukkit.entity.Player;


public class AnnouncementManager {
	
	public final  AnnouncementList joinList = new AnnouncementList(false);
	
	public final  AnnouncementList repeatList = new AnnouncementList(false);
	
	protected final Map<String, Runnable> tasks = new HashMap<String, Runnable>(40);
	
	// TODO: Arbitrary channels? Ids?
	
	public void fromConfig(CompatConfig cfg, String prefix){
		joinList.fromConfig(cfg, prefix + "join-list");
		repeatList.fromConfig(cfg, prefix + "repeat-list");
	}
	
	/**
	 * Should be lowest priority.
	 * @param player
	 */
	public void onPlayerJoin(final Player player){
		
	}
	
	public void onPlayerLeave(final Player player){
		
	}
}
