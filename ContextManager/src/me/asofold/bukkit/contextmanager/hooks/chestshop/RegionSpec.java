package me.asofold.bukkit.contextmanager.hooks.chestshop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;

import asofold.pluginlib.shared.Utils;
import asofold.pluginlib.shared.blocks.FBlockPos;

public class RegionSpec {
	final String worldName;
	final String regionName;
	
	final int hash;
	
	final Set<FBlockPos> shops = new HashSet<FBlockPos>();
	
	private String itemString = "<not available>";
	
	long tsSetItemString = 0;
	
	public RegionSpec(String worldName, String regionName){
		this.worldName = worldName;
		this.regionName = regionName;
		hash = worldName.hashCode() ^ regionName.hashCode();
	}
	
	@Override
	public final int hashCode(){
		return hash;
	}

	@Override
	public final boolean equals(final Object obj) {
		if (!(obj instanceof RegionSpec)) return false;
		final RegionSpec other = (RegionSpec) obj;
		return regionName.equals(other.regionName) && worldName.equals(other.worldName);
	}
	
	public String getItemString(Map<FBlockPos, ShopSpec> specs){
		if (System.currentTimeMillis() - tsSetItemString >10000) setItemString(specs);
		return itemString;
	}

	private final void setItemString(final Map<FBlockPos, ShopSpec> specs) {
		tsSetItemString = System.currentTimeMillis();
		final List<String> items = new ArrayList<String>(20);
		for (final FBlockPos pos : shops){
			final ShopSpec spec = specs.get(pos);
			if (spec == null) continue; // overly ...
			items.add(spec.toString());
		}
		Collections.sort(items);
		itemString = Utils.join(items, ChatColor.DARK_GRAY + " | ");
	}
	
}
