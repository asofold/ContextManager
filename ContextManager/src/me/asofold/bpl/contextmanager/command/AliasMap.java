package me.asofold.bpl.contextmanager.command;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
	
	/**
	 * 
	 * @param arg Must be lower case
	 * @param completions Result 
	 * @param filter Only these can be added.
	 */
	public void fillInTabCompletions(final String arg, final Collection<String> completions, final Set<String> filter){
		for (final String alias : commandAliases.keySet()){
			if (alias.startsWith(arg)){
				final String x = commandAliases.get(alias);
				if (filter.contains(x)) completions.add(x);
			}
		}
	}
}
