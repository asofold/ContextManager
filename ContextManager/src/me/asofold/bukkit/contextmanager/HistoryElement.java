package me.asofold.bukkit.contextmanager;

import org.bukkit.ChatColor;

import asofold.pluginlib.shared.Utils;

public class HistoryElement {
	public final long ts;
	public final ContextType type;
	public final String name;
	public final String details;
	public final String message;
	public final boolean isAnnounce;
	public HistoryElement(ContextType type, String name, String details, String message, boolean isAnnounce){
		this.type = type;
		this.name = name;
		this.details = details;
		this.message = message;
		this.isAnnounce = isAnnounce;
		ts = System.currentTimeMillis();
	}
	
	public String toString(){
		String bc = (isAnnounce?ChatColor.YELLOW:ChatColor.GRAY).toString();
		String mc = (isAnnounce?ChatColor.YELLOW:ChatColor.WHITE).toString();
		StringBuilder b = new StringBuilder(120);
		b.append(ChatColor.AQUA + "[");
		b.append(Utils.timeStr(ts));
		b.append("] " +bc);
		b.append(type.toString());
		b.append(" "+ChatColor.GREEN);
		b.append(name);
		if (!details.isEmpty()){
			b.append(ChatColor.GRAY+"(");
			b.append(details);
			b.append(ChatColor.GRAY+")");
		}
		b.append(" "+mc);
		b.append(message);
		return b.toString();
	}
}
