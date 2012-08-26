package me.asofold.bpl.contextmanager.plshared.players;

import java.util.ArrayList;
import java.util.Collections;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Auxiliary for sorting by lower-case name (!).
 * @author mc_dev
 *
 */
public class SortablePlayer implements Comparable<SortablePlayer>{
	Player online = null;
	OfflinePlayer offline = null;
	String lcName = null;
	String name = null;
	public SortablePlayer(Player player){
		this.setOnline(player);
	}
	public SortablePlayer(OfflinePlayer player){
		this.setOffline(player);
	}
	public OfflinePlayer getOffline() {
		return offline;
	}
	public void setOffline(OfflinePlayer player) {
		this.name = player.getName();
		this.lcName = name.toLowerCase();
		this.offline = player;
	}
	public Player getOnline() {
		return online;
	}
	public void setOnline(Player player) {
		this.name = player.getName();
		this.lcName = name.toLowerCase();
		this.online = player;
	}
	public String getName(){
		return name;
	}
	public String getLCName(){
		return lcName;
	}
	@Override
	public int compareTo(SortablePlayer other) {
		// TODO Auto-generated method stub
		return this.lcName.compareTo(other.lcName);
	}
	public static ArrayList<SortablePlayer> getSortedPlayers(Object[] players){
		if ( players == null ) return new ArrayList<SortablePlayer>();
		ArrayList<SortablePlayer> out = new ArrayList<SortablePlayer>(players.length);
		for ( Object obj : players ){
			if ( obj == null); // ignore
			else if ( obj instanceof Player ) out.add(new SortablePlayer((Player)obj));
			else if (obj instanceof OfflinePlayer) out.add(new SortablePlayer((OfflinePlayer)obj));
			else if (obj instanceof SortablePlayer) out.add( (SortablePlayer) obj);
		}
		Collections.sort(out);
		return out;
	}
}
