package me.asofold.bpl.contextmanager.hooks.chestshop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.asofold.bpl.contextmanager.plshared.Utils;
import me.asofold.bpl.contextmanager.plshared.blocks.FBlockPos;

import org.bukkit.ChatColor;


public class RegionSpec {
	final String worldName;
	final String regionName;
	
	final int hash;
	
	final Set<FBlockPos> shops = new HashSet<FBlockPos>();
	
	private final List<String> itemStrings = new ArrayList<String>(20);
	
	long tsSetItemString = 0;
	
	public RegionSpec(String worldName, String regionName){
		this.worldName = worldName;
		this.regionName = regionName;
		hash = worldName.hashCode() ^ regionName.hashCode();
		itemStrings.add("<not available>");
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
	
	public List<String> getItemStrings(final Map<FBlockPos, ShopSpec> specs){
		if (System.currentTimeMillis() - tsSetItemString >10000) setItemString(specs);
		return itemStrings;
	}
	
	public String getFullItemString(final Map<FBlockPos, ShopSpec> specs){
		return Utils.join(getItemStrings(specs), ChatColor.DARK_GRAY + " | ");
	}

	private final void setItemString(final Map<FBlockPos, ShopSpec> specs) {
		tsSetItemString = System.currentTimeMillis();
		itemStrings.clear();
		for (final FBlockPos pos : shops){
			final ShopSpec spec = specs.get(pos);
			if (spec == null) continue; // overly ...
			itemStrings.add(spec.toString());
		}
		Collections.sort(itemStrings, String.CASE_INSENSITIVE_ORDER);
//		itemString = Utils.join(items, ChatColor.DARK_GRAY + " | ");
	}

	/**
	 * 
	 * @param blockMap
	 * @param prefix Must have: trim/lower-case.
	 * @return
	 */
	public String getItemStrings(Map<FBlockPos, ShopSpec> specs, String prefix)
	{
		if (itemStrings.isEmpty()) return null;
		if (prefix == null || prefix.isEmpty()) return getFullItemString(specs);
		final List<String> out = new ArrayList<String>(itemStrings.size());
		final List<String> itemStrings = getItemStrings(specs); 
		for (int i = 0 ; i < itemStrings.size(); i++){
			final String ref = itemStrings.get(i);
			if (ChatColor.stripColor(ref.toLowerCase()).startsWith(prefix)) out.add(ref);
		}
		if (out.isEmpty()) return null;
		else return Utils.join(out, ChatColor.DARK_GRAY + " | ");
	}
	
}
