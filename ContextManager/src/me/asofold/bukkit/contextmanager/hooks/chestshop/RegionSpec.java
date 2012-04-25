package me.asofold.bukkit.contextmanager.hooks.chestshop;

import java.util.HashSet;
import java.util.Set;

import asofold.pluginlib.shared.blocks.FBlockPos;

public class RegionSpec {
	final String worldName;
	final String regionName;
	
	final int hash;
	
	final Set<FBlockPos> shops = new HashSet<FBlockPos>();
	
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

	
}
