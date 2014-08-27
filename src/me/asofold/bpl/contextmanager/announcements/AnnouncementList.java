package me.asofold.bpl.contextmanager.announcements;

import java.util.ArrayList;
import java.util.List;

import me.asofold.bpl.contextmanager.config.compatlayer.CompatConfig;

/**
 * Just a list of announcements. Mainly eases config reading and links them.
 * @author mc_dev
 *
 */
public class AnnouncementList {
	
	protected List<Announcement> announcements = new ArrayList<Announcement>(20);
	
	public final boolean repeat;
	
	public AnnouncementList(){
		this(true);
	}
	
	public AnnouncementList(boolean repeat){
		this.repeat = repeat;
	}
	
	public void fromConfig(CompatConfig cfg, String prefix){
		announcements.clear();
		List<String> keys = cfg.getStringKeys(prefix);
		// TODO: Sort ?
		// TODO: Expand by tag (!).
		for (String key : keys){
			Announcement announcement = new Announcement();
			announcement.fromConfig(cfg, prefix + key + ".");
			announcements.add(announcement);
		}
		for (int i = 1; i < announcements.size(); i++){
			announcements.get(i - 1).next = announcements.get(i);
		}
//		if (repeat){
//			announcements.get(announcements.size() - 1).next = announcements.get(0);
//		}
	}
	
	/**
	 * Use to force tasks to end processing this list.
	 */
	public void clear(){
		for (Announcement announcement : announcements){
			announcement.next = null;
		}
		announcements.clear();
	}
	
	public boolean isEmpty(){
		return announcements.isEmpty();
	}
	
	public Announcement getFirst(){
		return isEmpty() ? null : announcements.get(0);
	}
}
