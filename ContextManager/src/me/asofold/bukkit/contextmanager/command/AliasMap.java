package me.asofold.bukkit.contextmanager.command;

import java.util.HashMap;
import java.util.Map;

public class AliasMap {
	Map<String, String> commandAliases = new HashMap<String, String>();
	
	/**
	 * 
	 * @param aliasDef First String is the standard name to map to.
	 */
	public AliasMap(String[][] aliasDef){
		for ( String[] ref : aliasDef){
			String label = ref[0];
			for ( String n : ref){
				commandAliases.put(n, label);
			}
		}
	}
	
	/**
	 * Get lower case version, possibly mapped from an abreviation.
	 * @param input
	 * @return
	 */
	public String getMappedCommandLabel(String input){
		input = input.trim().toLowerCase();
		String out = commandAliases.get(input);
		if (out == null) return input;
		else return out;
	}
}
