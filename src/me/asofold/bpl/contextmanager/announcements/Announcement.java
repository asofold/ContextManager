package me.asofold.bpl.contextmanager.announcements;

import java.util.Collection;
import java.util.LinkedHashSet;

import me.asofold.bpl.contextmanager.config.compatlayer.CompatConfig;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public class Announcement {
	
	public boolean useTag = true;
	
	protected final Collection<String> haveAnyPerm = new LinkedHashSet<String>();
	protected final Collection<String> notHaveAnyPerm = new LinkedHashSet<String>();
	protected final Collection<String> haveAnyGroup = new LinkedHashSet<String>();
	protected final Collection<String> notHaveAnyGroup = new LinkedHashSet<String>();
	
	public String[] texts = new String[0];
	
	public long delay = 240000L; // 4 minutes.
	
	public Announcement next = null;
	
	protected void setContent(Collection<String> store, Collection<String> cfgContent){
		store.clear();
		if (cfgContent != null){
			store.addAll(cfgContent);
		}
	}
	
	public void fromConfig(CompatConfig cfg, String prefix){
		useTag = cfg.getBoolean(prefix + "use-tag", true);
		setContent(haveAnyPerm, cfg.getStringList(prefix + "haveany-perm", null));
		setContent(notHaveAnyPerm, cfg.getStringList(prefix + "nothaveany-perm", null));
		setContent(haveAnyGroup, cfg.getStringList(prefix + "haveany-group", null));
		setContent(notHaveAnyGroup, cfg.getStringList(prefix + "nothaveany-group", null));
		setTexts(cfg.getStringList(prefix + "texts", null));
		delay = cfg.getLong(prefix + "delay", 240L) * 1000L; // 4 minutes in seconds.
	}
	
	public void setTexts(Collection<String> texts) {
		if (texts == null){
			this.texts = new String[0];
			return;
		}
		this.texts = new String[texts.size()];
		texts.toArray(this.texts);
		for (int i = 0; i < this.texts.length; i++){
			this.texts[i] = ChatColor.translateAlternateColorCodes('&', this.texts[i]);
		}
	}

	public boolean applies(final Player player){
		if (!haveAnyPerm.isEmpty()){
			boolean applies = true;
			applies = false;
			for (final String perm :haveAnyPerm){
				if (player.hasPermission(perm)){
					applies = true;
					break;
				}
			}
			if (!applies) return false;
		}
		if (!notHaveAnyPerm.isEmpty()){
			for (final String perm :notHaveAnyPerm){
				if (player.hasPermission(perm)){
					return false;
				}
			}
		}
		// Top level groups (not inheritance).
		final boolean hg = !haveAnyGroup.isEmpty();
		final boolean nhg = !notHaveAnyGroup.isEmpty();
		if (hg || nhg){
			// Get groups !
			final PermissionGroup[] groups = PermissionsEx.getUser(player).getGroups();
			boolean applies = haveAnyGroup.isEmpty();
			for (int i = 0; i < groups.length; i++){
				final String gn = groups[i].getName();
				if (haveAnyGroup.contains(gn)) applies = true;
				if (notHaveAnyGroup.contains(gn)) return false;
			}
			if (!applies) return false;
		}
		return true;
	}
	
}
